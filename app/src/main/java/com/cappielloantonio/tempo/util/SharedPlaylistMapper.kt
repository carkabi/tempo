package com.cappielloantonio.tempo.util

import com.cappielloantonio.tempo.repository.tropikeau.models.SharedPlaylistItem
import com.cappielloantonio.tempo.subsonic.models.Child

object SharedPlaylistMapper {
    @JvmStatic
    fun toChild(item: SharedPlaylistItem): Child {
        return Child(
            id = item.trackId,
            isDir = false,
            title = item.title,
            album = item.album,
            artist = item.artist,
            coverArtId = item.coverArtId,
            duration = item.duration,
            type = "music"
        )
    }
    @JvmStatic
    fun toChildren(items: List<SharedPlaylistItem>?): ArrayList<Child> {
        val result = ArrayList<Child>()
        items?.forEach { item ->
            if (!item.trackId.isNullOrBlank()) {
                result.add(toChild(item))
            }
        }
        return result
    }
}
