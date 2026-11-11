# Runs the Nicotine+ core (pynicotine 3.3.11, unmodified) headless, for lossless downloads.
# Trimmed from Nicotine+ for Android's bridge.py: no chat, no shares UI, no uploads UI.
# Kotlin -> Python: call(name, json_args), queued onto the core's main loop.
# Python -> Kotlin: listener.onEvent(name, json_payload), from the core's main loop and the tagger thread.

import json
import os
import queue
import threading
import time

listener = None
state = {"connecting": False, "status": None, "error": None, "leaving": False}
searches = {}  # core search token -> Kotlin search id
hints = {}  # download folder -> tags Verza already knows
MAX_RESULTS = 500
# ponytail: shares and uploads stay on because the core needs them to answer peers; nothing is shared.
# buddies too: shares and uploads look every peer up in the buddy list (empty here) when it browses us
# or asks for a file, and without it that raised, which stopped the whole core.
COMPONENTS = {
    "error_handler", "portmapper", "network_thread", "shares", "users", "notifications",
    "network_filter", "statistics", "search", "downloads", "uploads", "buddies", "pluginhandler"
}


def emit(name, payload=None):
    listener.onEvent(name, json.dumps(payload))


def run(kotlin_listener, data_dir, download_dir, user_agent):
    global listener
    listener = kotlin_listener

    import tagger
    tagger.USER_AGENT = user_agent

    from pynicotine.config import config
    # Set paths explicitly: the defaults point at a desktop install's profile on some platforms
    config.set_config_file(os.path.join(data_dir, "config", "config"))
    config.set_data_folder(data_dir)

    from pynicotine.core import core
    from pynicotine.events import events
    from pynicotine.shares import Shares
    from pynicotine.downloads import Downloads

    _patch_scanner(Shares)
    _patch_downloaded(Downloads)

    core.init_components(enabled_components=COMPONENTS)

    transfers = config.sections["transfers"]
    transfers["downloaddir"] = download_dir
    transfers["incompletedir"] = os.path.join(download_dir, ".incomplete")

    for name, callback in (
        ("server-login", _on_login),
        ("server-disconnect", _on_disconnect),
        ("invalid-password", lambda *_a: _on_login_error("Wrong password for this username.")),
        ("invalid-username", lambda *_a: _on_login_error("That username isn't allowed. Use letters, numbers and spaces.")),
        ("file-search-response", _on_search_response),
    ):
        events.connect(name, callback)

    core.start()
    if config.sections["server"]["login"] and config.sections["server"]["passw"]:
        _connect()

    events.schedule(delay=1, callback=_tick, repeat=True)

    while events.process_thread_events():
        time.sleep(0.1)


def call(name, args_json):
    from pynicotine.events import events
    events.invoke_main_thread(globals()["cmd_" + name], **json.loads(args_json))


def _connect():
    from pynicotine.core import core
    state["connecting"] = True
    state["error"] = None
    core.connect()


def cmd_login(user, password):
    from pynicotine.config import config
    from pynicotine.core import core
    from pynicotine.events import events
    from pynicotine.slskmessages import UserStatus

    config.sections["server"]["login"] = user
    config.sections["server"]["passw"] = password
    config.write_configuration()

    if core.users.login_status != UserStatus.OFFLINE:
        state["leaving"] = True
        core.disconnect()
        # ponytail: reconnect after the network thread processes the disconnect
        events.schedule(delay=1, callback=_connect)
        return
    _connect()


def cmd_logout():
    from pynicotine.config import config
    from pynicotine.core import core
    config.sections["server"]["login"] = ""
    config.sections["server"]["passw"] = ""
    config.write_configuration()
    state["connecting"] = False
    state["leaving"] = True
    state["error"] = None
    core.disconnect()


def cmd_reconnect():
    _connect()


def cmd_search(id, text):
    from pynicotine.core import core
    core.search.do_search(text, "global")
    searches[core.search.token] = id


def cmd_stop_search(id):
    from pynicotine.core import core
    for token, sid in list(searches.items()):
        if sid == id:
            del searches[token]
            core.search.remove_search(token)


def cmd_download(user, path, size, folder, tags):
    from pynicotine.core import core
    hints[os.path.normpath(folder)] = tags
    core.downloads.enqueue_download(user, path, folder_path=folder, size=size)


def cmd_remove(user, path):
    from pynicotine.core import core
    transfer = core.downloads.transfers.get(user + path)
    if transfer is not None:
        hints.pop(os.path.normpath(transfer.folder_path), None)
        core.downloads.clear_downloads([transfer])


# Core events -> Kotlin

def _on_login(msg):
    state["connecting"] = False
    state["leaving"] = False
    if not msg.success:
        _on_login_error(f"Login failed: {msg.reason}")


def _on_disconnect(msg):
    from pynicotine.config import config
    if msg.manual_disconnect:
        # The core won't retry. Unless we asked for it, the server kicked us: usually the same
        # account signing in elsewhere, and retrying would just kick that client off in turn.
        state["connecting"] = False
        if not state["leaving"] and state["error"] is None:
            state["error"] = "Signed in somewhere else. Verza signs in again on your next download."
        state["leaving"] = False
        return
    # The core retries on its own unless we have no password
    state["connecting"] = bool(config.sections["server"]["passw"]) and state["error"] is None


def _on_login_error(text):
    state["connecting"] = False
    state["error"] = text


def _on_search_response(msg):
    from pynicotine.core import core
    from pynicotine.slskmessages import FileListMessage

    sid = searches.get(msg.token)
    search = core.search.searches.get(msg.token)
    if sid is None or search is None:
        return

    files = []
    for _code, path, size, ext, attrs, *_unused in msg.list:
        lower = path.lower()
        if not lower.endswith(".flac"):
            continue
        if any(w in lower for w in search.excluded_words) or not all(w in lower for w in search.included_words):
            continue
        _bitrate, length, _vbr, rate, bits = FileListMessage.parse_file_attributes(attrs)
        files.append({"path": path, "size": size, "secs": length or 0, "rate": rate or 0, "bits": bits or 0})

    if files:
        emit("results", {"id": sid, "user": msg.username, "slots": bool(msg.freeulslots),
                         "speed": msg.ulspeed, "queue": msg.inqueue, "files": files[:MAX_RESULTS]})


def _tick():
    from pynicotine.config import config
    from pynicotine.core import core
    from pynicotine.slskmessages import UserStatus

    online = core.users.login_status != UserStatus.OFFLINE
    status = {
        "state": "online" if online else ("connecting" if state["connecting"] else "offline"),
        "user": config.sections["server"]["login"],
        "error": state["error"],
    }
    if status != state["status"]:
        state["status"] = status
        emit("status", status)

    if core.downloads.transfers:
        emit("transfers", [{
            "key": t.username + t.virtual_path,
            "size": t.size or 0,
            "pos": t.current_byte_offset or 0,
            "status": t.status,
        } for t in core.downloads.transfers.values()])


# Android has no multiprocessing, so the share scanner runs in a thread instead of a process

def _patch_scanner(shares_cls):
    from pynicotine.config import config
    from pynicotine.shares import Scanner

    class ScannerThread(threading.Thread):
        def __init__(self, target, q):
            super().__init__(target=target, daemon=True, name="ShareScanner")
            self.q = q

        def is_alive(self):
            # Keep the reader loop going until it has drained everything the scanner queued
            return super().is_alive() or not self.q.empty()

    def build(self, share_groups=None, init=False, rescan=True, rebuild=False):
        q = queue.Queue()
        scanner = Scanner(
            q, share_groups, self.share_db_paths, init, rescan, rebuild,
            reveal_buddy_shares=config.sections["transfers"]["reveal_buddy_shares"],
            reveal_trusted_shares=config.sections["transfers"]["reveal_trusted_shares"]
        )
        return ScannerThread(scanner.run, q), q

    shares_cls._build_scanner_process = build


# Finished files get their tags and cover filled in, then Kotlin moves them into the library

def _patch_downloaded(downloads_cls):
    import tagger
    original = downloads_cls._file_downloaded_actions

    def wrapped(self, username, file_path):
        folder = os.path.dirname(file_path)
        # The folder is Verza's own per-song folder, so it is the root: its name is no album
        tagger.enqueue(file_path, folder, lambda path, _note: emit("downloaded", {"path": path}),
                       hints.pop(os.path.normpath(folder), None))
        return original(self, username, file_path)

    downloads_cls._file_downloaded_actions = wrapped
