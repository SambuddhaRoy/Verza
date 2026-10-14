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
        assertEquals(
            listOf("lb_exploration" to "explore-new", "lb_jams" to "jams-new", "lb_daily" to "daily"),
            got.map { it.first.id to it.second },
        )
    }

    @Test fun newestYearOfEachYearlyListWins() {
        val got = pickWeekly(
            listOf(
                pl("top-discoveries-of-2023", "2024-01-02T05:00:00Z", "2099-01-01T00:00:00Z", "disc-2023"),
                pl("top-discoveries-of-2025", "2026-01-02T05:00:00Z", "2099-01-01T00:00:00Z", "disc-2025"),
                pl("top-discoveries-of-2024", "2025-01-02T05:00:00Z", "2099-01-01T00:00:00Z", "disc-2024"),
                pl("top-missed-recordings-of-2025", "2026-01-02T05:00:00Z", "2099-01-01T00:00:00Z", "miss-2025"),
                pl("some-future-kind", "2026-10-01T05:00:00Z", "2099-01-01T00:00:00Z", "future"),
            ),
            now,
        )
        assertEquals(
            listOf(
                Triple("lb_discoveries", "Top Discoveries of 2025", "disc-2025"),
                Triple("lb_missed", "Top Missed Songs of 2025", "miss-2025"),
            ),
            got.map { Triple(it.first.id, it.first.title, it.second) },
        )
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
