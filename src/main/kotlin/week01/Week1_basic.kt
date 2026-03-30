package week01

// val = 한 번 정하면 변경 불가 -> CS 개념 제목처럼 안 바뀌는 데이터
// var = 나중에 바꿀 수 있음 -> 학습 점수, 진도율처럼 바뀌는 데이터

fun main() {
    val conceptTitle = "스택 (Stack)"
    val conceptCategory = "자료구조"

    var studyScore = 0
    var isLearned = false

    println("개념: $conceptTitle | 카테고리: $conceptCategory")
    println("점수: $studyScore | $isLearned")

    studyScore = 85;    // var는 변경 가능
    isLearned = true

}
