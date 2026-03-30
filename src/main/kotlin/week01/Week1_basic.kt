package week01

// val = 한 번 정하면 변경 불가 -> CS 개념 제목처럼 안 바뀌는 데이터
// var = 나중에 바꿀 수 있음 -> 학습 점수, 진도율처럼 바뀌는 데이터

fun main() {
    // === SECTION 1: val vs var ===
    val conceptTitle = "스택 (Stack)"
    val conceptCategory = "자료구조"

    var studyScore = 0
    var isLearned = false

    println("개념: $conceptTitle | 카테고리: $conceptCategory")
    println("점수: $studyScore | $isLearned")

    studyScore = 85;    // var는 변경 가능
    isLearned = true

    // === SECTION 2: String 템플릿 ===
    val title = "큐 (Queue)"
    val learnedCount = 12
    val totalCount = 50

    // $변수명 으로 바로 삽입
    println("현재 학습 중: $title")

    // ${표현식} 으로 계산도 가능
    println("진도: $learnedCount / $totalCount")
    println("달성률: ${learnedCount * 100 / totalCount}%")
    println("남은 개념: ${totalCount - learnedCount}개")

    // === SECTION 3: when 표현식 (퀴즈 정답 판단)
    val userAnswer = 2
    val correctAnswer = 2

    // when: Java의 switch보다 훨씬 간결하고 강력함
    val result = when (userAnswer) {
        correctAnswer -> "정답입니다!"
        else -> "틀렸습니다. 정답은 ${correctAnswer}번이에요."
    }

    // 점수에 따른 등급 판정
    val quizScore = 78
    val grade = when {
        quizScore >= 90 -> "A - 완벽해요!"
        quizScore >= 80 -> "B - 잘했어요!"
        quizScore >= 70 -> "C - 한 번 더 복습!"
        else -> "D - 기초부터 다시!"
    }

    // 카테고리별 아이콘 매핑
//    val icon = when (category) {
//        "자료구조" -> "🗂️"
//        "알고리즘" -> "⚙️"
//        "OS"       -> "🖥️"
//        "네트워크" -> "🌐"
//        else       -> "📌"
//    }

    // === SECTION 4 ===
    val csTopics = listOf("스택", "큐", "연결 리스트", "트리", "그래프")

    // 기본 순회
    for (topic in csTopics) {
        println("  • $topic")
    }

    // 인덱스와 함께 (번호 표시)
    for ((index, topic) in csTopics.withIndex()) {
        println("${index + 1}. $topic")
    }

    // Kotlin 스타일 - 실제 앱에서 가장 많이 활용되는 패턴
    csTopics.forEach { topic ->
        println("$topic 학습 완료")
    }

    // 조건 필터링
    val filtered = csTopics.filter { it.length <= 2}
    filtered.forEach { println("-> $it") }


}
