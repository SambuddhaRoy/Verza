package com.verza.data

import com.verza.data.ListenBrainzRepository.Companion.pickWeekly
import com.verza.data.ListenBrainzRepository.Summary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * Which ListenBrainz playlists become Home mixes. ListenBrainz keeps every past week in the list and
 * stops making new ones for someone who stops listening, so the newest live one of each kind must
 * win, an expired one must never be shown as this week's, and Daily Jams is not one of ours.
 */
class ListenBrainzTest {
    private val now = t("2026-10-10T12:00:00Z")
    private fun t(s: String) = Instant.parse(s).toEpochMilli()
    private fun pl(patch: String, date: String, expires: String, id: String) = Summary(id, patch, t(date), t(expires))

    @Test fun newestLiveOfEachKindWins() {
        val got = pickWeekly(
            listOf(
                pl("weekly-jams", "2026-09-28T05:00:00Z", "2026-10-12T05:00:00Z", "jams-old"),
                pl("weekly-exploration", "2026-10-05T05:00:00Z", "2026-10-19T05:00:00Z", "explore-new"),
                pl("weekly-jams", "2026-10-05T05:00:00Z", "2026-10-19T05:00:00Z", "jams-new"),
                pl("daily-jams", "2026-10-10T05:00:00Z", "2026-10-11T05:00:00Z", "daily"),
            ),
            now,
        )
        assertEquals(listOf("lb_exploration" to "explore-new", "lb_jams" to "jams-new"), got.map { it.first.id to it.second })
    }

    @Test fun expiredWeeksAreLeftOut() {
        val got = pickWeekly(
            listOf(
                pl("weekly-jams", "2026-08-24T05:00:00Z", "2026-09-07T05:00:00Z", "aug"),
                pl("weekly-exploration", "2026-08-24T05:00:00Z", "2026-09-07T05:00:00Z", "aug2"),
            ),
            now,
        )
        assertEquals(emptyList<Any>(), got)
    }

    @Test fun noPlaylistsIsNotAnError() {
        assertEquals(emptyList<Any>(), pickWeekly(emptyList(), now))
    }
}
