package week02

// data class: 데이터를 담는 전용 클래스
// toString, equals , copy가 자동 생성됨

data class CsConcept(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val difficulty: Int,
    val isLearned: Boolean = false  // 기본값
)

data class QuizQuestion(
    val id: Int,
    val conceptId: Int,
    val question: String,
    val options: List<String>,       // 4지선다 보기
    val correctIndex: Int           // 정답 인덱스 0~3
)

data class UserProgress(
    val conceptId: Int,
    val isLearned: Boolean,
    val quizScore: Int? = null,     // 퀴즈 미응시면 null
    val lastStudiedAt: String? = null
)

fun main() {
    val allConcepts = listOf(
        CsConcept(1, "스택", "LIFO 구조", "자료구조", 1, true),
        CsConcept(2, "큐", "FIFO 구조", "자료구조", 1, true),
        CsConcept(3, "프로세스", "독립 실행 단위", "OS", 2),
        CsConcept(4, "TCP", "신뢰성 있는 프로토콜", "네트워크", 2),
    )

    // 카테고리 필터
    val dsOnly = allConcepts.filter { it.category == "자료구조" }

    // 학습 완료된 것만
    val learned = allConcepts.filter { it.isLearned }

    // 제목만 추출
    val titles = allConcepts.map { it.title }

    // 카테고리별 그룹핑
    val grouped = allConcepts.groupBy { it.category }
    grouped.forEach { (cat, list) ->
        println("$cat: ${list.size}개")
    }

    // 진도율 계산
    val rate = learned.size * 100 / allConcepts.size
    println("학습 진도: $rate%")

    /**
     * Int? = null이 될 수 있는 Int
     * Int = 절대 null이 될 수 없는 Int
     */
    var quizScore: Int? = null      // 퀴즈 안 품
    quizScore = 85                  // 퀴즈 풀고 나서 값 할당

    // ?: 엘비스 연산자 - null이면 기본값 사용
    val display = quizScore ?: "미응시"
    println("점수: $display")

    // ?. 안전한 접근 - null이면 그냥 null 반환 (앱 안 꺼짐)
    val lastDate: String? = null
    println("점수: $display")

    // let - null이 아닐 때만 블록 실행
    val score: Int? = 78
    score?.let {
        // score가 null이 아닐 때만 실행됨
        val grade = when {
            it >= 90 -> "A"
            it >= 80 -> "B"
            it >= 70 -> "C"
            else     -> "D"
        }
        println("$it 점 -> 등급: $grade")
    } ?: println("퀴즈를 먼저 풀어주세요")

}













