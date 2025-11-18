package com.back.motionit.domain.challenge.comment.moderation

/**
 * BLOCK / WARN 다중 패턴 매칭용 Aho-Corasick 구현
 * - 패턴마다 KeywordFilter.Decision (BLOCK / WARN)을 저장
 * - search() 시 BLOCK이 하나라도 발견되면 BLOCK 우선
 */
class AhoCorasick {

    private class Node(
        val children: MutableMap<Char, Node> = mutableMapOf(),
        var fail: Node? = null,
        val outputs: MutableList<KeywordFilter.Decision> = mutableListOf(),
    )

    private val root = Node()


    fun addPattern(pattern: String, decision: KeywordFilter.Decision) {
        if (pattern.isBlank()) return

        var node = root
        for (ch in pattern) {
            node = node.children.getOrPut(ch) { Node() }
        }
        node.outputs += decision
    }


    fun buildFailureLinks() {
        val queue: ArrayDeque<Node> = ArrayDeque()


        root.children.values.forEach { child ->
            child.fail = root
            queue.add(child)
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            for ((ch, nextNode) in current.children) {
                var failNode = current.fail
                while (failNode != null && !failNode.children.containsKey(ch)) {
                    failNode = failNode.fail
                }

                nextNode.fail = when {
                    failNode == null -> root
                    else -> failNode.children[ch] ?: root
                }

                // 실패 링크가 가리키는 노드의 outputs도 현재 노드에 합쳐줌
                nextNode.outputs += nextNode.fail?.outputs ?: emptyList()

                queue.add(nextNode)
            }
        }
    }


    fun search(text: String): KeywordFilter.Decision? {
        var node = root
        var foundWarn = false

        for (ch in text) {
            while (node != root && !node.children.containsKey(ch)) {
                node = node.fail ?: root
            }
            node = node.children[ch] ?: root

            if (node.outputs.isNotEmpty()) {
                node.outputs.forEach { decision ->
                    when (decision) {
                        KeywordFilter.Decision.BLOCK -> return KeywordFilter.Decision.BLOCK
                        KeywordFilter.Decision.WARN -> foundWarn = true
                        else -> {}
                    }
                }
            }
        }

        return if (foundWarn) {
            KeywordFilter.Decision.WARN
        } else {
            null
        }
    }
}
