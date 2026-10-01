package com.verza.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Every install that existed before the design setting has no stored value. It must come up in the
 * design it was already using, Material; anything else would restyle the whole app under someone
 * the day they update. The same goes for a stored value this version does not know, such as one
 * written by a later build and read after a downgrade.
 */
class DesignSchemeTest {

    @Test
    fun `no stored design means Material`() {
        assertEquals(DesignScheme.MATERIAL, DesignScheme.fromName(null))
    }

    @Test
    fun `a stored design this version does not know means Material`() {
        assertEquals(DesignScheme.MATERIAL, DesignScheme.fromName("HOLOGRAM"))
        assertEquals(DesignScheme.MATERIAL, DesignScheme.fromName(""))
    }

    @Test
    fun `a stored design round-trips by name`() {
        DesignScheme.entries.forEach { assertEquals(it, DesignScheme.fromName(it.name)) }
    }
}
