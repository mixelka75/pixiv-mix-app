package wtf.mxl.pixmix.shared.util

/**
 * Map raw exception messages into something a user can act on. Network exception
 * class names differ across JVM (UnknownHostException, ConnectException, …) and
 * Skiko/web — we match on substrings so the same code works everywhere.
 */
fun Throwable.userMessage(): String {
    val raw = (message ?: this::class.simpleName ?: "Unknown error").lowercase()
    return when {
        "unknownhost" in raw || "unable to resolve" in raw || "name or service not known" in raw ->
            "Нет соединения с сервером"
        "timeout" in raw || "timed out" in raw -> "Тайм-аут запроса"
        "refused" in raw -> "Сервер недоступен"
        "ssl" in raw || "certificate" in raw -> "Проблема с сертификатом"
        "401" in raw || "unauthorized" in raw -> "Сессия истекла — нужно войти заново"
        "403" in raw -> "Доступ запрещён (возможно, нужен прокси)"
        "429" in raw -> "Слишком много запросов — попробуйте позже"
        "5" in raw && (" 5" in raw || "/5" in raw) -> "Сервер pixiv недоступен"
        else -> message ?: "Неизвестная ошибка"
    }
}
