package org.omnilex.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchNormalizerTest {
    @Test fun `normalizes case and wildcard`() {
        assertEquals("b%nk", SearchNormalizer.normalize(" B*NK "))
    }

    @Test fun `soundex encodes known word`() {
        assertEquals("R160", SearchNormalizer.soundex("river"))
    }
}
