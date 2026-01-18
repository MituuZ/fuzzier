/*
 *  MIT License
 *
 *  Copyright (c) 2025 Mitja Leino
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.mituuz.fuzzier.grep.backend

import com.mituuz.fuzzier.entities.CaseMode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SearchMatcherTest {
    private val matcher = SearchMatcher()

    @Test
    fun `matchesLine should return true for exact match with case sensitive`() {
        assertTrue(matcher.matchesLine("hello world", "hello", CaseMode.SENSITIVE))
    }

    @Test
    fun `matchesLine should return false for case mismatch with case sensitive`() {
        assertFalse(matcher.matchesLine("hello world", "HELLO", CaseMode.SENSITIVE))
    }

    @Test
    fun `matchesLine should return true for case mismatch with case insensitive`() {
        assertTrue(matcher.matchesLine("hello world", "HELLO", CaseMode.INSENSITIVE))
    }

    @Test
    fun `matchesLine should return true for lowercase match with case insensitive`() {
        assertTrue(matcher.matchesLine("HELLO WORLD", "hello", CaseMode.INSENSITIVE))
    }

    @Test
    fun `matchesLine should return false when substring not found`() {
        assertFalse(matcher.matchesLine("hello world", "xyz", CaseMode.SENSITIVE))
    }

    @Test
    fun `matchesLine should handle empty string`() {
        assertTrue(matcher.matchesLine("hello world", "", CaseMode.SENSITIVE))
    }

    @Test
    fun `parseSearchWords should split by spaces`() {
        val result = matcher.parseSearchWords("hello world test")
        assertEquals(listOf("hello", "world", "test"), result)
    }

    @Test
    fun `parseSearchWords should trim input`() {
        val result = matcher.parseSearchWords("  hello world  ")
        assertEquals(listOf("hello", "world"), result)
    }

    @Test
    fun `parseSearchWords should handle single word`() {
        val result = matcher.parseSearchWords("hello")
        assertEquals(listOf("hello"), result)
    }

    @Test
    fun `parseSearchWords should handle empty string`() {
        val result = matcher.parseSearchWords("")
        assertEquals(listOf(""), result)
    }

    @Test
    fun `extractFirstCompleteWord should return second word when enough spaces`() {
        val result = matcher.extractFirstCompleteWord("hello world test")
        assertEquals("world", result)
    }

    @Test
    fun `extractFirstCompleteWord should return null when less than 2 spaces`() {
        val result = matcher.extractFirstCompleteWord("hello world")
        assertNull(result)
    }

    @Test
    fun `extractFirstCompleteWord should return null when only one word`() {
        val result = matcher.extractFirstCompleteWord("hello")
        assertNull(result)
    }

    @Test
    fun `extractFirstCompleteWord should return null when second word is empty`() {
        val result = matcher.extractFirstCompleteWord("hello  test")
        assertNull(result)
    }

    @Test
    fun `extractFirstCompleteWord should handle multiple spaces`() {
        val result = matcher.extractFirstCompleteWord("hello world test foo")
        assertEquals("world", result)
    }

    @Test
    fun `extractFirstCompleteWord should trim input`() {
        val result = matcher.extractFirstCompleteWord("  hello world test  ")
        assertEquals("world", result)
    }
}
