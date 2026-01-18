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

class SearchMatcher {
    fun matchesLine(line: String, searchString: String, caseMode: CaseMode): Boolean {
        return if (caseMode == CaseMode.INSENSITIVE) {
            line.contains(searchString, ignoreCase = true)
        } else {
            line.contains(searchString)
        }
    }

    fun parseSearchWords(searchString: String): List<String> {
        return searchString.trim().split(" ")
    }

    fun extractFirstCompleteWord(searchString: String): String? {
        val trimmedSearch = searchString.trim()
        val words = parseSearchWords(trimmedSearch)

        if (words.size > 1 && words[1].isNotEmpty() && trimmedSearch.count { it == ' ' } >= 2) {
            return words[1]
        }

        return null
    }
}
