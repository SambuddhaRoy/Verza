# Fills in missing tags and embeds cover art in downloaded music, so any music player shows the right
# title, artist, album, track number and cover. Tags the file already has are never changed.
# Missing ones come from MusicBrainz, or from the folder and file name when MusicBrainz has no match.
# Covers come from an image in the album folder, else the Cover Art Archive.

import base64
import difflib
import json
import os
import queue
import re
import threading
import time
import urllib.error
import urllib.parse
import urllib.request

import mutagen
from mutagen.flac import FLAC, Picture
from mutagen.id3 import APIC
from mutagen.mp4 import MP4, MP4Cover

AUDIO = {".mp3", ".flac", ".m4a", ".mp4", ".ogg", ".oga", ".opus"}
NEEDED = ("title", "artist", "album", "tracknumber")
FILLED = NEEDED + ("albumartist", "discnumber", "date", "musicbrainz_trackid", "musicbrainz_albumid")
MAX_FOLDER_IMAGE = 1_500_000  # bigger scans would be copied into every track; use the 500 px archive copy instead
USER_AGENT = "NicotinePlusAndroid/0.3.0"

_jobs = queue.Queue()
_worker = None
_cache = {}  # MusicBrainz and cover lookups, keyed by URL
_last_mb = 0.0


def enqueue(path, root, done, hints=None):
    """Tags path on a background thread (lookups are rate limited), then calls done(path, note)."""
    global _worker
    _jobs.put((path, root, done, hints))
    if _worker is None:
        _worker = threading.Thread(target=_run, daemon=True, name="Tagger")
        _worker.start()


def pending():
    return _jobs.qsize()


def _run():
    while True:
        path, root, done, hints = _jobs.get()
        try:
            note = tag_file(path, root, hints)
        except Exception as error:  # a broken file or a network hiccup must not stop the queue
            note = f"Couldn't tag {os.path.basename(path)}: {error}"
        done(path, note)


def tag_file(path, root=None, hints=None):
    """Returns a note saying what changed, or None when nothing did.

    hints: title/artist/album the caller already knows, trusted over the file name.
    """
    if os.path.splitext(path)[1].lower() not in AUDIO:
        return None
    f = mutagen.File(path, easy=True)
    if f is None:
        return None
    if f.tags is None:
        f.add_tags()
    have = {k: _first(f.tags, k) for k in FILLED}
    has_cover = _has_cover(path)
    if all(have[k] for k in NEEDED) and has_cover:
        return None

    guess = guess_from_path(path, root)
    guess.update({k: v for k, v in (hints or {}).items() if v})
    query = {k: have[k] or guess.get(k, "") for k in ("title", "artist", "album", "tracknumber")}
    release, track = _match(query)
    found = _from_musicbrainz(release, track) if track else guess

    added = [k for k, v in found.items() if v and k in FILLED and not have[k]]
    for k in added:
        f.tags[k] = str(found[k])
    if added:
        f.save()

    if not has_cover:
        cover = _cover(path, root, release)
        if cover:
            _embed(path, *cover)
            added.append("cover")

    names = [k for k in added if not k.startswith("musicbrainz")]
    if not names:
        return None
    source = "" if names == ["cover"] else " from MusicBrainz" if track else " from the file name"
    return f"Tagged {os.path.basename(path)}{source}: {', '.join(names)}"


def _first(tags, key):
    try:
        value = tags.get(key)
    except (KeyError, ValueError):
        return ""
    return str(value[0]).strip() if value else ""


# Guessing from "Artist - Album (1997) [FLAC]/02 - Title.flac"

def _clean(s):
    return re.sub(r"\s*[\[({][^\])}]*[\])}]", "", s).replace("_", " ").strip(" .-")


def guess_from_path(path, root=None):
    folder = os.path.dirname(path)
    stem = os.path.splitext(os.path.basename(path))[0]
    out = {}

    m = re.match(r"^(?:(\d)[-.])?(\d{1,3})(?:\s*[-._)]\s*|\s+)(.+)$", stem)
    if m:
        out["tracknumber"] = str(int(m.group(2)))
        if m.group(1):
            out["discnumber"] = m.group(1)
        stem = m.group(3)
    parts = [p.strip() for p in _clean(stem).split(" - ")]
    if len(parts) > 1:
        out["artist"], out["title"] = parts[0], " - ".join(parts[1:])
    else:
        out["title"] = parts[0]

    if root is None or os.path.normpath(folder) != os.path.normpath(root):
        name = os.path.basename(folder)
        year = re.search(r"\b(19|20)\d\d\b", name)
        if year:
            out["date"] = year.group(0)
        parts = [p.strip() for p in _clean(name).split(" - ")]
        parts = [p for p in parts if not re.fullmatch(r"(19|20)\d\d", p)]
        if len(parts) > 1:
            out.setdefault("artist", parts[0])
            out["album"] = " - ".join(parts[1:])
        elif parts and parts[0]:
            out["album"] = parts[0]
    return out


# MusicBrainz

def _get(url, limited=True):
    global _last_mb
    if url in _cache:
        return _cache[url]
    if limited:  # MusicBrainz allows one request per second
        time.sleep(max(0.0, _last_mb + 1.1 - time.monotonic()))
        _last_mb = time.monotonic()
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                body = response.read()
            break
        except urllib.error.HTTPError as error:
            if error.code == 503 and attempt < 2:  # MusicBrainz is busy or throttling us
                time.sleep(5 * (attempt + 1))
                continue
            if error.code != 404:
                raise
            body = None
            break
    _cache[url] = body
    return body


def _mb(path, **query):
    body = _get(f"https://musicbrainz.org/ws/2/{path}?fmt=json&" + urllib.parse.urlencode(query))
    return json.loads(body) if body else None


def _lucene(s):
    return re.sub(r'([+\-&|!(){}\[\]^"~*?:\\/])', r"\\\1", s)


def _norm(s):
    return re.sub(r"[^a-z0-9]+", "", s.lower())


def _similar(a, b):
    return difflib.SequenceMatcher(None, _norm(a), _norm(b)).ratio()


def _release(release_id):
    return _mb(f"release/{release_id}", inc="recordings+artist-credits+release-groups")


def _rank(release):
    """Sort key that puts the original studio album first, ahead of reissues, compilations and bootlegs."""
    group = release.get("release-group", {})
    return (release.get("status") != "Official", group.get("primary-type") != "Album",
            bool(group.get("secondary-types")), release.get("date") or "9999", len(release.get("media", [])))


def _match(q):
    """Finds the MusicBrainz release and track for a file, or (None, None).

    Needs the album. ponytail: a title alone isn't looked up, because MusicBrainz search ranks dozens of
    compilation and DJ-mix copies of a hit level with the original album; a wrong album is worse than none.
    """
    if not (q["artist"] and q["album"]):
        return None, None
    found = _mb("release/", limit=10, query=f'release:"{_lucene(q["album"])}" AND artist:"{_lucene(q["artist"])}"')
    # Below 90 a match is more often wrong than right
    candidates = sorted((r for r in (found or {}).get("releases", []) if r.get("score", 0) >= 90), key=_rank)
    for summary in candidates[:3]:
        release = _release(summary["id"])
        track = _track_in([dict(t, disc=m.get("position", 1)) for m in release.get("media", []) for t in m.get("tracks", [])], q)
        if track:
            return release, track
    return None, None


def _track_in(tracks, q):
    number, title = q["tracknumber"].split("/")[0], q["title"]
    if number.isdigit():
        by_number = [t for t in tracks if t.get("position") == int(number)]
        # With several discs, or a title that disagrees, the number alone isn't enough
        for t in by_number:
            if not title or _similar(title, t["title"]) >= 0.6:
                return t
    if title:
        best = max(tracks, key=lambda t: _similar(title, t["title"]), default=None)
        if best and _similar(title, best["title"]) >= 0.75:
            return best
    return None


def _credit(credits):
    return "".join(c.get("name", "") + c.get("joinphrase", "") for c in credits or [])


def _from_musicbrainz(release, track):
    discs = len(release.get("media", []))
    return {
        "title": track["title"],
        "artist": _credit(track.get("artist-credit")),
        "album": release["title"],
        "albumartist": _credit(release.get("artist-credit")),
        "tracknumber": str(track["position"]),
        "discnumber": str(track["disc"]) if discs > 1 else "",
        "date": release.get("release-group", {}).get("first-release-date") or release.get("date", ""),
        "musicbrainz_trackid": track.get("recording", {}).get("id", ""),
        "musicbrainz_albumid": release["id"],
    }


# Cover art

def _has_cover(path):
    f = mutagen.File(path)
    if isinstance(f, FLAC):
        return bool(f.pictures)
    tags = f.tags
    if tags is None:
        return False
    if hasattr(tags, "getall"):  # ID3
        return bool(tags.getall("APIC"))
    if isinstance(f, MP4):
        return "covr" in tags
    return "metadata_block_picture" in tags  # Ogg Vorbis / Opus


def _cover(path, root, release):
    """(bytes, mime) from the album folder or the Cover Art Archive, or None."""
    folder = os.path.dirname(path)
    # Loose single-file downloads share the root folder, so an image there belongs to no album in particular
    if root is None or os.path.normpath(folder) != os.path.normpath(root):
        images = [n for n in os.listdir(folder) if os.path.splitext(n)[1].lower() in (".jpg", ".jpeg", ".png")]
        images.sort(key=lambda n: (not re.match(r"(cover|folder|front|albumart)", n.lower()), n))
        for name in images[:1]:
            file = os.path.join(folder, name)
            if os.path.getsize(file) <= MAX_FOLDER_IMAGE:
                with open(file, "rb") as image:
                    return image.read(), "image/png" if name.lower().endswith(".png") else "image/jpeg"
    if release:
        data = _get(f"https://coverartarchive.org/release/{release['id']}/front-500", limited=False)
        group = release.get("release-group", {}).get("id")
        if data is None and group:
            data = _get(f"https://coverartarchive.org/release-group/{group}/front-500", limited=False)
        if data:
            return data, "image/jpeg"
    return None


def _embed(path, data, mime):
    f = mutagen.File(path)
    if isinstance(f, FLAC):
        f.add_picture(_picture(data, mime))
    elif isinstance(f, MP4):
        kind = MP4Cover.FORMAT_PNG if mime == "image/png" else MP4Cover.FORMAT_JPEG
        f.tags["covr"] = [MP4Cover(data, imageformat=kind)]
    elif hasattr(f.tags, "getall"):
        f.tags.add(APIC(encoding=3, mime=mime, type=3, desc="Cover", data=data))
    else:
        f.tags["metadata_block_picture"] = [base64.b64encode(_picture(data, mime).write()).decode()]
    f.save()


def _picture(data, mime):
    picture = Picture()
    picture.type = 3  # front cover
    picture.mime = mime
    picture.data = data
    return picture
