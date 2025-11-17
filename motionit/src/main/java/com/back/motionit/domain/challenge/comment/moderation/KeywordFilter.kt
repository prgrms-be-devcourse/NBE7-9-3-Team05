package com.back.motionit.domain.challenge.comment.moderation

object KeywordFilter {

    enum class Decision {
        ALLOW, WARN, BLOCK
    }
    // 오탐 방지
    private val WHITELIST: List<String> = listOf(
        "시발점", "병신도", "꺼져가", "꺼져감", "보지말", "보지않", "보지못", "보지마", "자지말",
        "자지않", "자지못", "자지마", "자지러", "2018년",
        "Scunthorpe"

    )

    private val BLOCK: List<String> = listOf(
        "씨발", "ㅆㅣ발", "씹팔", "좆", "존나", "병신", "븅신", "ㅄ",
        "지랄", "지럴", "개새끼", "개새", "개자식", "개지랄", "꺼져",
        "꺼져라", "꺼져버려", "뒤져", "뒤져라", "죽여", "죽일놈", "죽어라", "18새끼",
        "18년", "18놈", "18것", "십팔", "시팔", "씨팔", "씹년", "씹놈", "씹새끼", "쌍년",
        "쌍놈", "썅", "좆같", "좆나", "좆밥", "좆", "미친년", "미친놈",
        "쓰레기같은", "창녀", "창년", "강간", "느금마", "자지", "보지", "씨발", "씨발년", "새끼", "병신", "좆밥", "1찍", "2찍", "좆물", "걸레년", "년놈",
        "딸딸이", "빨갱이", "토착왜구", "수구꼴통", "문빠", "벌레새끼", "일베충", "펨코충", "죽여버려", "목잘라", "찌른다", "칼부림",
        "게이새끼", "게이년", "병신", "메갈", "시발", "시바라", "씨바", "시바", "찔러", "등신", "시발",
        "motherfucker", "nigger", "faggot", "cunt", "slut", "whore", "dick", "pussy", "retard"
    )

    private val WARN: List<String> = listOf(
        "ㅅㅂ", "ㅆㅂ", "ㅈㄴ", "ㅈ나", "ㅈㄹ", "ㅁㅊ", "개극혐",
        "극혐", "정신병자", "돌았냐", "돌았네", "ㅂㅅ", "ㅗ",
        "ㄲㅈ", "ㅉㅉ", "ㅆㄹㄱ", "ㅅㄲ", "ㅈㄹㄴ", "ㄱㅅㄲ", "ㄱㅅㄴ", "ㄴㄱㅈ", "ㄷㅈ",
        "ㄷㅊ", "ㅅㅂㄹㅁ", "ㅆㅂㄴ", "ㅂㅅ같", "ㅈㄴ웃김", "병맛", "급식충", "틀딱", "꼰대",
        "멍청", "저능", "쪼다", "개씹", "미친놈", "또라이", "돌아이", "애미", "애비", "영포티", "영써티", "호구", "꼴리",
        "대깨문", "윤찌끄레기", "문슬람", "국짐", "보수꼰대", "패버려", "죽인다", "남혐", "여혐", "장애있", "틀딱",
        "bitch", "asshole", "shit", "fuck", "crap", "loser", "dumb", "idiot"
    )

    @JvmStatic
    fun decide(raw: String): Decision {
        var norm = NormLite.normalize(raw)

        for (w in WHITELIST) {
            norm = norm.replace(w, "")
        }

        for (b in BLOCK) {
            if (norm.contains(b)) {
                return Decision.BLOCK
            }
        }

        for (w in WARN) {
            if (norm.contains(w)) {
                return Decision.WARN
            }
        }
        return Decision.ALLOW
    }

}
