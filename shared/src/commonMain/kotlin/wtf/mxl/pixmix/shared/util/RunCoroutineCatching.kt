package wtf.mxl.pixmix.shared.util

import kotlinx.coroutines.CancellationException

/**
 * Like [runCatching] but never swallows [CancellationException]. Coroutine cancellation
 * must propagate up the call stack — wrapping it in `Result.failure` surfaces "job
 * cancelled" as if it were a user-visible error and breaks structured concurrency.
 */
inline fun <T> runCoroutineCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (c: CancellationException) {
    throw c
} catch (t: Throwable) {
    Result.failure(t)
}
