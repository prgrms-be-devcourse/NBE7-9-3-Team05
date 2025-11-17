package com.back.motionit.domain.challenge.comment.moderation;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public class NormLite {

	private static final Map<Integer, Character> JAMO_TO_COMPAT = new HashMap<>();

	static {
		JAMO_TO_COMPAT.put(0x1100, 'ㄱ'); // ᄀ -> ㄱ
		JAMO_TO_COMPAT.put(0x1101, 'ㄲ'); // ᄁ -> ㄲ
		JAMO_TO_COMPAT.put(0x1102, 'ㄴ'); // ᄂ -> ㄴ
		JAMO_TO_COMPAT.put(0x1103, 'ㄷ'); // ᄃ -> ㄷ
		JAMO_TO_COMPAT.put(0x1104, 'ㄸ'); // ᄄ -> ㄸ
		JAMO_TO_COMPAT.put(0x1105, 'ㄹ'); // ᄅ -> ㄹ
		JAMO_TO_COMPAT.put(0x1106, 'ㅁ'); // ᄆ -> ㅁ
		JAMO_TO_COMPAT.put(0x1107, 'ㅂ'); // ᄇ -> ㅂ
		JAMO_TO_COMPAT.put(0x1108, 'ㅃ'); // ᄈ -> ㅃ
		JAMO_TO_COMPAT.put(0x1109, 'ㅅ'); // ᄉ -> ㅅ
		JAMO_TO_COMPAT.put(0x110A, 'ㅆ'); // ᄊ -> ㅆ
		JAMO_TO_COMPAT.put(0x110B, 'ㅇ'); // ᄋ -> ㅇ
		JAMO_TO_COMPAT.put(0x110C, 'ㅈ'); // ᄌ -> ㅈ
		JAMO_TO_COMPAT.put(0x110D, 'ㅉ'); // ᄍ -> ㅉ
		JAMO_TO_COMPAT.put(0x110E, 'ㅊ'); // ᄎ -> ㅊ
		JAMO_TO_COMPAT.put(0x110F, 'ㅋ'); // ᄏ -> ㅋ
		JAMO_TO_COMPAT.put(0x1110, 'ㅌ'); // ᄐ -> ㅌ
		JAMO_TO_COMPAT.put(0x1111, 'ㅍ'); // ᄑ -> ㅍ
		JAMO_TO_COMPAT.put(0x1112, 'ㅎ'); // ᄒ -> ㅎ
	}

	private static String toCompatJamo(String word) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < word.length(); ) {
			int cp = word.codePointAt(i);
			i += Character.charCount(cp);
			Character mapped = JAMO_TO_COMPAT.get(cp);
			if (mapped != null) {
				sb.append(mapped);
			} else {
				sb.appendCodePoint(cp);
			}
		}
		return sb.toString();
	}

	public static String normalize(String raw) {
		if (raw == null) {
			return "";
		}
		String norm = Normalizer.normalize(raw, Normalizer.Form.NFKC).toLowerCase();

		norm = norm.replaceAll("[\\u200B-\\u200D\\uFEFF]", "");

		//초성 자모(ᄉ,ᄇ 등)를 호환 자모(ㅅ,ㅂ)로 되돌리기
		norm = toCompatJamo(norm);

		norm = norm.replaceAll("(?<=[가-힣])[1lI|]+(?=[가-힣])", "");

		norm = norm.replaceAll("[._\\-/,\\s]+", "");

		norm = norm.replaceAll("(.)\\1{3,}", "$1$1");

		return norm.trim();
	}
}
