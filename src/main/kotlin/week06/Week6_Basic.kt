package week06

import java.util.*;

// 1. 권한(Role)의 정의
enum class Role { ADMIN, USER, GUEST }

// 2. 사용자 정보를 담는 데이터 클래스
data class Member(
    val name: String,
    val role: Role,
    var cash: Int = 0
)

// 3. 로또 시스템 핵심 로직
class LottoSystem {
    private var winningNumbers: List<Int> = emptyList()

    // [Admin 권한] 당첨 번호 생성
    fun generateWinningNumbers(admin: Member) {
        if (admin.role != Role.ADMIN) {
            println("권한이 없습니다. 관리자만 당첨 번호를 생성할 수 있습니다.")
            return
        }
        winningNumbers = (1..45).shuffled().take(6).sorted()
        println("📢 [시스템] 이번 주 당첨 번호가 생성되었습니다: $winningNumbers")
    }

    // [Guest/User 공통] 당첨 번호 확인
    fun getWinningNumbers() {
        if (winningNumbers.isEmpty()) {
            println("아직 당첨 번호가 생성되지 않았습니다.")
        } else {
            println("현재 당첨 번호: $winningNumbers")
        }
    }

    // [User 권한] 로또 구매
    fun buyLotto(user: Member): List<Int>? {
        if (user.role != Role.USER) {
            println("구매 권한이 없습니다. 일반 사용자만 구매 가능합니다.")
            return null
        }
        if (user.cash < 1000) {
            println("잔액이 부족합니다. (1,000원 필요)")
            return null
        }

        user.cash -= 1000
        val ticket = (1..45).shuffled().take(6).sorted()
        println("🎟️ ${user.name} 님이 로또를 구매했습니다: $ticket (남은 잔액: ${user.cash}원)")
        return ticket
    }
}

// 4. 실행 및 테스트
fun main() {
    val system = LottoSystem()

    val admin = Member("운영자", Role.ADMIN)
    val user = Member("공실", Role.USER, 5000)
    val guest = Member("방문자", Role.GUEST)

    println("--- 1. 권한 테스트 ---")
    system.generateWinningNumbers(user)     // 거절됨
    system.generateWinningNumbers(admin)             // 성공

    println("\n--- 2. 구매 테스트 ---")
    val ticket1 = system.buyLotto(user)
    val ticket2 = system.buyLotto(guest)     // 거절됨

    println("\n--- 3. 당첨 확인 ---")
    system.getWinningNumbers()

    if (ticket1 != null) {
        val matchCount = ticket1.intersect(system.getWinningNumbersList().toSet()).size
        println("결과: ${user.name} 님의 번호 중 $matchCount 개가 일치합니다!")
    }

}

// 로직 보조를 위한 확장 함수 (내부 데이터 접근용)
fun LottoSystem.getWinningNumbersList(): List<Int> {
    // 실제 서비스라면 보안상 필터링이 필요하지만 실습을 위해 추가
    return (1..45).shuffled().take(6)      // 임시 반환
}

















