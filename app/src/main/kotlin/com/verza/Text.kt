package com.verza

/** "1 track", "12 tracks". Every count used to say "tracks", so a one-song playlist read "1 tracks". */
fun tracksLabel(n: Int): String = if (n == 1) "1 track" else "$n tracks"
