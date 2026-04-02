package week03

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import week02.CsConcept

// 가짜 DB (실제 앱에서는 Room DB가 이 역할을 함)
object FakeDatabase {

    private val concepts = mutableListOf(
        CsConcept(1, "스택", "LIFO 구조", "자료구조", 1),
        CsConcept(2, "큐",       "FIFO 구조",          "자료구조", 1),
        CsConcept(3, "프로세스", "독립 실행 단위",      "OS",       2),
        CsConcept(4, "TCP",      "신뢰성 있는 프로토콜", "네트워크", 2),
    )

    // suspend fun = 시간이 걸릴 수 있는 함수 (코루틴 안에서만 호출 가능)
    suspend fun getAllConcepts(): List<CsConcept> {
        delay(500)      // DB 읽기 시뮬레이션
        return concepts.toList();
    }

    suspend fun saveConcept(concept: CsConcept) {
        delay(400)      // DB 쓰기 시뮬레이션
        concepts.add(concept)
        println("저장 완료: ${concept.title}")
    }

}

fun main() = runBlocking {
    // runBlocking: main 함수에서 코루틴 진입점
    // 실제 앱에서는 viewModelScope.launch { } 사용

    // launch: 결과값 없는 백그라운드 작업
    val job = launch {
        FakeDatabase.saveConcept(
            CsConcept(5, "힙", "우선순위 큐", "자료구조", 3)
        )
    }
    job.join()      // 완료 대기

    // withContext: 특정 스레드에서 실행 후 결과 반환
    val concepts = withContext(Dispatchers.IO) {
        FakeDatabase.getAllConcepts()       // IO 스레드에서 DB 읽기
    }
    println("총 ${concepts.size}개 로드 완료")

    // async: 결과값 있는 백그라운드 작업 (병렬 실행)
    coroutineScope {
        val ds = async(Dispatchers.IO) {
            FakeDatabase.getAllConcepts().filter { it.category == "자료구조" }
        }
        val os = async(Dispatchers.IO) {
            FakeDatabase.getAllConcepts().filter { it.category == "OS" }
        }

        // 두 카테고리를 동시에 로딩 -> 순차 대비 빠름
        println("자료구조: ${ds.await().size}개")
        println("OS: ${os.await().size}개")
    }


}