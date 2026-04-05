package week05

import java.time.LocalDateTime

/**
 * OTT 스트리밍 서비스 - 콘텐츠 재생 비즈니스 로직 실습
 * => "사용자가 영상을 재생하기까지" 전체 흐름을 코드로 경험하기
 */

// ============================================================
// 도메인 모델
// ============================================================

enum class SubscriptionTier { FREE, BASIC, PREMIUM }
enum class ContentGrade { ALL, TWELVE, FIFTEEN, ADULT }
enum class StreamQuality { SD, HD, FHD, UHD }

data class User(
    val id: Long,
    val name: String,
    val age: Int,
    val subscription: SubscriptionTier,
    val isVerified: Boolean                 // 성인 인증 여부
)

data class Content(
    val id: Long,
    val title: String,
    val grade: ContentGrade,
    val availableFrom: SubscriptionTier,    // 이 등급 이상만 시청 가능
    val maxQuality: StreamQuality
)

data class PlayToken(
    val userId: Long,
    val contentId: Long,
    val quality: StreamQuality,
    val expiresAt: LocalDateTime,
    val streamUrl: String
)

// ============================================================
// Custom Exception 계층
// ============================================================

sealed class StreamingException(message: String) : Exception(message)

class AuthException(reason: String) : StreamingException("인증 오류: $reason")
class AgeRestrictionException(grade: ContentGrade, userAge: Int)
    : StreamingException("${grade.name} 등급 콘텐츠는 ${userAge}세 이용 불가")
class SubscriptionException(required: SubscriptionTier, current: SubscriptionTier)
    : StreamingException("${required.name} 구독 필요 (현재: ${current.name})")
class ContentNotFoundException(id: Long) : StreamingException("콘텐츠 없음: id=$id")
class ConcurrentStreamException(max: Int) : StreamingException("동시 재생 초과: 최대 ${max}개")

// ============================================================
// Repository (DB 시뮬레이션)
// ============================================================

object UserRepository {
    private val users = mapOf(
        1L to User(1L, "김철수", 25, SubscriptionTier.PREMIUM, isVerified = true),
        2L to User(2L, "이영희", 19, SubscriptionTier.BASIC,   isVerified = false),
        3L to User(3L, "박민준", 15, SubscriptionTier.FREE,    isVerified = false),
        4L to User(4L, "최지원", 28, SubscriptionTier.BASIC,   isVerified = true)
    )

    fun findById(id: Long): User? = users[id]
}

object ContentRepository {
    private val contents = mapOf(
        101L to Content(101L, "킹덤 시즌3",      ContentGrade.FIFTEEN, SubscriptionTier.BASIC,   StreamQuality.FHD),
        102L to Content(102L, "무료 다큐멘터리", ContentGrade.ALL,     SubscriptionTier.FREE,    StreamQuality.HD),
        103L to Content(103L, "프리미엄 영화",   ContentGrade.ADULT,   SubscriptionTier.PREMIUM, StreamQuality.UHD),
        104L to Content(104L, "어린이 애니",     ContentGrade.ALL,     SubscriptionTier.FREE,    StreamQuality.HD)
    )

    fun findById(id: Long): Content? = contents[id]
}

// 동시 재생 세션 관리 (실제인 경우 Redis)
object SessionRepository {
    private val activeSessions = mutableMapOf<Long, Int>()      // userId -> 재생 중인 스트림 수
    private val maxStreams = mapOf(
        SubscriptionTier.FREE       to 1,
        SubscriptionTier.BASIC      to 2,
        SubscriptionTier.PREMIUM    to 4
    )

    fun getActiveCount(userId: Long): Int = activeSessions.getOrDefault(userId, 0)
    fun getMaxAllowed(tier: SubscriptionTier): Int = maxStreams[tier]!!
    fun addSession(userId: Long) { activeSessions[userId] = getActiveCount(userId) + 1 }
}

// ============================================================
// 비즈니스 로직 서비스
// ============================================================

// -- 인증 서비스 --
object AuthService {
    fun authenticate(userId: Long): Result<User> = runCatching {
        UserRepository.findById(userId)
            ?: throw AuthException("존재하지 않는 유저: $userId")
    }
}

// -- 콘텐츠 접근 검증 서비스 --
object AccessControlService {

    fun validate(user: User, content: Content): Result<Unit> = runCatching {
        checkAgeRestriction(user, content)
        checkSubscription(user, content)
        checkConcurrentStream(user)
    }

    private fun checkAgeRestriction(user: User, content: Content) {
        val minAge = when (content.grade) {
            ContentGrade.ALL -> 0
            ContentGrade.TWELVE -> 12
            ContentGrade.FIFTEEN -> 15
            ContentGrade.ADULT -> if (user.isVerified) 0 else throw AgeRestrictionException(
                content.grade,
                user.age
            )
        }
        if (user.age < minAge) throw AgeRestrictionException(content.grade, user.age)
    }

    private fun checkSubscription(user: User, content: Content) {
        val tierOrder =
            listOf(SubscriptionTier.FREE, SubscriptionTier.BASIC, SubscriptionTier.PREMIUM)
        val userLevel = tierOrder.indexOf(user.subscription)
        val requiredLevel = tierOrder.indexOf(content.availableFrom)
        if (userLevel < requiredLevel) throw SubscriptionException(
            content.availableFrom,
            user.subscription
        )
    }

    private fun checkConcurrentStream(user: User) {
        val active = SessionRepository.getActiveCount(user.id)
        val max = SessionRepository.getMaxAllowed(user.subscription)
        if (active >= max) throw ConcurrentStreamException(max)
    }
}

// -- 스트림 품질 결정 서비스 --
object QualityService {
    fun resolve(user: User, content: Content): StreamQuality {
        // FREE는 최대 HD, BASIC은 최대 FHD, PREMIUM은 제한 없음
        val tierMax = when (user.subscription) {
            SubscriptionTier.FREE   -> StreamQuality.HD
            SubscriptionTier.BASIC  -> StreamQuality.FHD
            SubscriptionTier.PREMIUM -> StreamQuality.UHD
        }
        val qualityOrder = listOf(StreamQuality.SD,
            StreamQuality.HD, StreamQuality.FHD, StreamQuality.UHD)
        val userMax = qualityOrder.indexOf(tierMax)
        val contentMax = qualityOrder.indexOf(content.maxQuality)
        return qualityOrder[minOf(userMax, contentMax)]     // 둘 중 낮은 품질로 결정
    }
}

// -- 토큰 발급 서비스
object TokenService {
    fun issue(user: User, content: Content, quality: StreamQuality): PlayToken {
        SessionRepository.addSession(user.id)
        return PlayToken(
            userId = user.id,
            contentId = content.id,
            quality = quality,
            expiresAt = LocalDateTime.now().plusHours(6),
            streamUrl = "https://stream.ott.com/content/\${content.id}?q=\${quality.name.lowercase()}&uid=\${user.id}"
        )
    }
}

// 파사드(Facade) - 전체 흐름 오케스트레이션
object StreamingService {

    fun play(userId: Long, contentId: Long): Result<PlayToken> = runCatching {

        // Step 1. 유저 인증
        val user = AuthService.authenticate(userId).getOrThrow()

        // Step 2. 콘텐츠 조회
        val content = ContentRepository.findById(contentId)
            ?: throw ContentNotFoundException(contentId)

        // Step 3. 접근 권한 검증 (나이 / 구독 / 동시재생)
        AccessControlService.validate(user, content).getOrThrow()

        // Step 4. 스트림 품질 결정
        val quality = QualityService.resolve(user, content)

        // Step 5. 재생 토큰 발급
        TokenService.issue(user, content, quality)
    }

}

// ============================================================
// API 응답 래퍼 & 에러 핸들링
// ============================================================

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val errorCode: String? = null,
    val message: String? = null
)

fun <T> Result<T>.toApiResponse(): ApiResponse<T> = fold(
    onSuccess = { ApiResponse(success = true, data = it) },
    onFailure = { ex ->
        val (code, msg) = when (ex) {
            is AuthException           -> "AUTH_ERROR"        to ex.message!!
            is AgeRestrictionException -> "AGE_RESTRICTED"    to ex.message!!
            is SubscriptionException   -> "SUBSCRIPTION_REQUIRED" to ex.message!!
            is ContentNotFoundException -> "NOT_FOUND"        to ex.message!!
            is ConcurrentStreamException -> "STREAM_LIMIT"   to ex.message!!
            else                       -> "INTERNAL_ERROR"   to "서버 오류가 발생했습니다"
        }
        ApiResponse(success = false, errorCode = code, message = msg)
    }
)

// ============================================================
// 시나리오 실행
// ============================================================

fun runScenario(label: String, userId: Long, contentId: Long) {
    println("\n▶ $label")
    println("  유저=$userId, 콘텐츠=$contentId")
    val response = StreamingService.play(userId, contentId).toApiResponse()
    if (response.success) {
        val token = response.data!!
        println("  ✅ 재생 성공!")
        println("     품질   : ${token.quality}")
        println("     URL    : ${token.streamUrl}")
        println("     만료   : ${token.expiresAt}")
    } else {
        println(" ❌ [\${response.errorCode}] \${response.message}")
    }
}

fun main() {
    println("========================================")
    println("  OTT 스트리밍 서비스 재생 흐름 시뮬레이션")
    println("========================================")

    // 시나리오 1: 정상 재생 (PREMIUM 유저 + 프리미엄 영화)
    runScenario("정상 재생", userId = 1L, contentId = 103L)

    // 시나리오 2: 구독 등급 부족 (BASIC 유저 → 프리미엄 영화)
    runScenario("구독 등급 부족", userId = 2L, contentId = 103L)

    // 시나리오 3: 나이 제한 (15세 유저 → 15세 이상 콘텐츠)
    runScenario("나이 제한", userId = 3L, contentId = 101L)

    // 시나리오 4: 성인 미인증 (BASIC 유저, 미인증 → 성인 콘텐츠)
    runScenario("성인 인증 필요", userId = 4L, contentId = 103L)

    // 시나리오 5: 콘텐츠 없음
    runScenario("존재하지 않는 콘텐츠", userId = 1L, contentId = 999L)

    // 시나리오 6: 무료 콘텐츠 (FREE 유저)
    runScenario("무료 유저 → 무료 콘텐츠", userId = 3L, contentId = 102L)

    // 시나리오 7: 품질 다운그레이드 확인 (BASIC 유저 → UHD 콘텐츠)
    runScenario("품질 자동 조정", userId = 4L, contentId = 103L)

}









