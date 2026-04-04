package week04

/**
 * 1. Null Safety
 * - email, age가 nullable인데 toResponse() 에서 Elvis(?:)와 let으로 안전하게 처리
 *
 * 2. sealed class 예외 계층
 * - AppException 을 상속한 커스텀 예외들이 when 문에서 타입별로 분기되는 패턴
 *
 * 3. Result + runCatching - 예외를 직접 try-catch 하지 않고 result로 감싸서
 *  toApiResponse() 에서 한 번에 처리하는 패턴
 */

// ========================
// 1. Custom Exception 계층
// ========================

sealed class AppException(message: String, cause: Throwable? = null) : Exception(message, cause)

class NotFoundException(resource: String, id: Any) : AppException("$resource not found: id=$id")
class ValidationException(field: String, reason: String) : AppException("Validation failed [$field]: $reason")
class DatabaseException(operation: String, cause: Throwable) : AppException("DB error during $operation", cause)

// ========================
// 2. 도메인 모델
// ========================

data class User(
    val id: Long,
    val name: String,
    val email: String?,      // nullable - 이메일 null 처리 (이메일 없으면 안 됨)
    val age: Int?           // nullable - 나이 미입력 가능
)

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,      // non-null - 응답에는 반드시 포함
    val ageGroup: String
)

// ========================
// 3. Repository (DB 시뮬레이션)
// ========================

object UserRepository {
    private val db = mapOf(
        1L to User(1L, "김철수", "chulsoo@example.com", 28),
        2L to User(2L, "이영희", null, null),   // 이메일/나이 없음
        3L to User(3L, "박민준", "minjun@example.com", 17)
    )

    fun findById(id: Long): User? = db[id]      // 없으면 null 반환

    fun findAll(): List<User> = db.values.toList()
}

// ========================
// 4. Null Safety 처리 패턴
// ========================

fun User.toResponse(): UserResponse {
    val resolvedEmail = email                   // null 가능
        ?: "no-email@unknown.com"               // Elvis 연산자로 기본값 처리

    val resolvedAge = age ?: 0
    val ageGroup = when {
        resolvedAge < 20 -> "10대"
        resolvedAge < 30 -> "20대"
        resolvedAge < 40 -> "30대"
        else             -> "40대 이상"
    };

    return UserResponse(id, name, resolvedEmail, ageGroup)
}

// ========================
// 5. Result 타입으로 예외처리
// ========================

fun getUser(id: Long): Result<UserResponse> = runCatching {
    val user = UserRepository.findById(id)
        ?: throw NotFoundException("User", id)

    validateUser(user)      // 검증 실패 시 예외 던짐
    user.toResponse()
}

fun validateUser(user: User) {
    if (user.name.isBlank()) {
        throw ValidationException("name", "이름은 비어있을 수 없습니다")
    }
    user.age?.let { age ->
        if (age < 0 || age > 150) {
            throw ValidationException("age", "나이가 유효하지 않습니다: $age")
        }
    }
}

// ========================
// 6. 에러 응답 처리
// ========================

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

fun <T> Result<T>.toApiResponse(): ApiResponse<T> = fold(
    onSuccess = { ApiResponse(success = true, data = it) },
    onFailure = { ex ->
        val message = when (ex) {
            is NotFoundException -> "리소스를 찾을 수 없습니다: ${ex.message}"
            is ValidationException -> "입력값 오류: ${ex.message}"
            is DatabaseException -> "서버 오류가 발생했습니다."
            else                 -> "알 수 없는 오류: ${ex.message}"
        }
        ApiResponse(success = false, error = message)
    }
)

// ========================
// 7. 전체 흐름 실행
// ========================

fun main() {
    val testIds = listOf(1L, 2L, 3L, 999L)

    println("=== 유저 조회 API 시뮬레이션 ===\n")

    testIds.forEach { id ->
        val response = getUser(id).toApiResponse()
        println("ID: $id -> $response")
    }

    println("\n=== 전체 유저 목록 ===\n")

    UserRepository.findAll()
        .map { it.toResponse() }
        .forEach { println(it) }
}













