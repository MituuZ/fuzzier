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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ResultBatcherTest {
    @Test
    fun `add should return null when batch size not reached`() {
        val batcher = ResultBatcher<String>(batchSize = 3)
        assertNull(batcher.add("item1"))
        assertNull(batcher.add("item2"))
    }

    @Test
    fun `add should return batch when batch size reached`() {
        val batcher = ResultBatcher<String>(batchSize = 3)
        batcher.add("item1")
        batcher.add("item2")
        val result = batcher.add("item3")

        assertEquals(listOf("item1", "item2", "item3"), result)
    }

    @Test
    fun `add should clear batch after returning`() {
        val batcher = ResultBatcher<String>(batchSize = 2)
        batcher.add("item1")
        val result = batcher.add("item2")

        assertNotNull(result)
        assertFalse(batcher.hasRemaining())
    }

    @Test
    fun `getRemaining should return items that have not been batched`() {
        val batcher = ResultBatcher<String>(batchSize = 3)
        batcher.add("item1")
        batcher.add("item2")

        val remaining = batcher.getRemaining()
        assertEquals(listOf("item1", "item2"), remaining)
    }

    @Test
    fun `getRemaining should clear internal batch`() {
        val batcher = ResultBatcher<String>(batchSize = 3)
        batcher.add("item1")
        batcher.getRemaining()

        assertFalse(batcher.hasRemaining())
    }

    @Test
    fun `getRemaining should return empty list when no items`() {
        val batcher = ResultBatcher<String>(batchSize = 3)
        assertTrue(batcher.getRemaining().isEmpty())
    }

    @Test
    fun `getCount should track total items added`() {
        val batcher = ResultBatcher<String>(batchSize = 2)
        batcher.add("item1")
        assertEquals(1, batcher.getCount())

        batcher.add("item2")
        assertEquals(2, batcher.getCount())

        batcher.add("item3")
        assertEquals(3, batcher.getCount())
    }

    @Test
    fun `hasRemaining should return true when items in batch`() {
        val batcher = ResultBatcher<String>(batchSize = 3)
        batcher.add("item1")
        assertTrue(batcher.hasRemaining())
    }

    @Test
    fun `hasRemaining should return false when batch is empty`() {
        val batcher = ResultBatcher<String>(batchSize = 3)
        assertFalse(batcher.hasRemaining())
    }

    @Test
    fun `hasRemaining should return false after batch is returned`() {
        val batcher = ResultBatcher<String>(batchSize = 2)
        batcher.add("item1")
        batcher.add("item2")
        assertFalse(batcher.hasRemaining())
    }

    @Test
    fun `should handle default batch size of 20`() {
        val batcher = ResultBatcher<String>()

        for (i in 1..19) {
            assertNull(batcher.add("item$i"))
        }

        val result = batcher.add("item20")
        assertNotNull(result)
        assertEquals(20, result?.size)
    }

    @Test
    fun `should work with different types`() {
        val batcher = ResultBatcher<Int>(batchSize = 2)
        batcher.add(1)
        val result = batcher.add(2)

        assertEquals(listOf(1, 2), result)
    }

    @Test
    fun `multiple batches should work correctly`() {
        val batcher = ResultBatcher<String>(batchSize = 2)

        val batch1 = batcher.add("item1")
        assertNull(batch1)

        val batch2 = batcher.add("item2")
        assertEquals(listOf("item1", "item2"), batch2)

        val batch3 = batcher.add("item3")
        assertNull(batch3)

        val batch4 = batcher.add("item4")
        assertEquals(listOf("item3", "item4"), batch4)

        assertEquals(4, batcher.getCount())
    }
}
