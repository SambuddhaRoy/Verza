package com.verza.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What Android Auto, Assistant and any other browser see when they look inside Verza.
 *
 * The session lives in :player and the library lives in :app's Room database, and :player cannot
 * depend on :app. So the app publishes a finished tree here and the session serves it, the same
 * arrangement [NowPlayingBridge] and [PlayerSettings] already use for the notification's like button
 * and the playback options.
 *
 * A snapshot rather than a query interface on purpose. A browser asks for children on a binder call
 * that it expects to return promptly, and a car is the worst possible place to discover that a
 * database read went slow. The whole tree is a few hundred rows of text, so it is cheap to hold.
 */
object MediaBrowseTree {

    /** One entry. [playableIds] is empty for a folder, and populated for something you can play. */
    data class Node(
        val id: String,
        val title: String,
        val subtitle: String = "",
        val artworkUri: String? = null,
        val browsable: Boolean = false,
        val children: List<Node> = emptyList(),
    )

    private val _root = MutableStateFlow<List<Node>>(emptyList())
    val root: StateFlow<List<Node>> = _root.asStateFlow()

    fun publish(folders: List<Node>) {
        _root.value = folders
    }

    /** Depth-first lookup. The tree is two levels deep, so this never walks far. */
    fun find(id: String): Node? {
        for (folder in _root.value) {
            if (folder.id == id) return folder
            folder.children.firstOrNull { it.id == id }?.let { return it }
        }
        return null
    }

    /** Every playable id under [parentId], in order, for "play this folder". */
    fun playableUnder(parentId: String): List<Node> =
        find(parentId)?.children?.filter { !it.browsable }.orEmpty()

    const val ROOT_ID = "verza_root"
}
