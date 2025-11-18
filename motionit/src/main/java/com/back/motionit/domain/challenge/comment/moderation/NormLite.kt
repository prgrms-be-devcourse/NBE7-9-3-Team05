package com.back.motionit.domain.challenge.comment.moderation

import java.text.Normalizer
import java.util.*

object NormLite {

    // 초성 자모(ᄀ, ᄂ ...) → 호환 자모(ㄱ, ㄴ ...)
    private val JAMO_TO_COMPAT: Map<Int, Char> = mapOf(
        0x1100 to 'ㄱ', // ᄀ -> ㄱ
        0x1101 to 'ㄲ', // ᄁ -> ㄲ
        0x1102 to 'ㄴ', // ᄂ -> ㄴ
        0x1103 to 'ㄷ', // ᄃ -> ㄷ
        0x1104 to 'ㄸ', // ᄄ -> ㄸ
        0x1105 to 'ㄹ', // ᄅ -> ㄹ
        0x1106 to 'ㅁ', // ᄆ -> ㅁ
        0x1107 to 'ㅂ', // ᄇ -> ㅂ
        0x1108 to 'ㅃ', // ᄈ -> ㅃ
        0x1109 to 'ㅅ', // ᄉ -> ㅅ
        0x110A to 'ㅆ', // ᄊ -> ㅆ
        0x110B to 'ㅇ', // ᄋ -> ㅇ
        0x110C to 'ㅈ', // ᄌ -> ㅈ
        0x110D to 'ㅉ', // ᄍ -> ㅉ
        0x110E to 'ㅊ', // ᄎ -> ㅊ
        0x110F to 'ㅋ', // ᄏ -> ㅋ
        0x1110 to 'ㅌ', // ᄐ -> ㅌ
        0x1111 to 'ㅍ', // ᄑ -> ㅍ
        0x1112 to 'ㅎ', // ᄒ -> ㅎ
    )

    private val ZERO_WIDTH_REGEX = Regex("[\\u200B-\\u200D\\uFEFF]")
    private val BETWEEN_HANGUL_MASK_REGEX = Regex("(?<=[가-힣])[1lI|]+(?=[가-힣])")
    private val SEPARATORS_REGEX = Regex("[._\\-/,\\s]+")
    private val REPEAT_CHAR_REGEX = Regex("(.)\\1{3,}")

    private fun toCompatJamo(word: String): String {
        val sb = StringBuilder(word.length)
        var i = 0
        while (i < word.length) {
            val cp = word.codePointAt(i)
            i += Character.charCount(cp)
            val mapped = JAMO_TO_COMPAT[cp]
            if (mapped != null) {
                sb.append(mapped)
            } else {
                sb.appendCodePoint(cp)
            }
        }
        return sb.toString()
    }

    @JvmStatic
    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) {
            return ""
        }

        var norm = Normalizer
            .normalize(raw, Normalizer.Form.NFKC)
            .lowercase(Locale.getDefault())

        // 제로폭 문자 제거
        norm = ZERO_WIDTH_REGEX.replace(norm, "")

        norm = toCompatJamo(norm)

        norm = BETWEEN_HANGUL_MASK_REGEX.replace(norm, "")

        norm = SEPARATORS_REGEX.replace(norm, "")

        norm = REPEAT_CHAR_REGEX.replace(norm, "$1$1")

        return norm.trim()
    }
}


