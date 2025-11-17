package com.back.motionit.domain.challenge.comment.moderation

import java.text.Normalizer
import java.util.*

object NormLite {
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

    private fun toCompatJamo(word: String): String {
        val sb = StringBuilder()
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
        if (raw == null) {
            return ""
        }
        var norm = Normalizer.normalize(raw, Normalizer.Form.NFKC).lowercase(Locale.getDefault())

        norm = norm.replace("[\\u200B-\\u200D\\uFEFF]".toRegex(), "")

        //초성 자모(ᄉ,ᄇ 등)를 호환 자모(ㅅ,ㅂ)로 되돌리기
        norm = toCompatJamo(norm)

        norm = norm.replace("(?<=[가-힣])[1lI|]+(?=[가-힣])".toRegex(), "")

        norm = norm.replace("[._\\-/,\\s]+".toRegex(), "")

        norm = norm.replace("(.)\\1{3,}".toRegex(), "$1$1")

        return norm.trim ()
    }
}
