package net.greemdev.cabinet.lib.util

import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import io.ktor.utils.io.concurrent.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

fun Runtime.addSuspendShutdownHook(func: suspend CoroutineScope.() -> Unit) =
    addShutdownHook(thread(start = false) { runBlocking(block = func) })

context(CoroutineScope)
fun <T> Collection<T>.launchForEach(func: suspend CoroutineScope.(T) -> Unit) {
    forEach {
        launch { func(it) }
    }
}

fun <T> Collection<T>.runForEach(func: suspend CoroutineScope.(T) -> Unit) {
    forEach {
        runBlocking { func(it) }
    }
}

fun Job.invokeOnFailure(block: (Throwable) -> Unit) = invokeOnCompletion {
    if (it != null && it !is CancellationException)
        block(it)
}

fun Job.invokeOnSuccess(block: () -> Unit) = invokeOnCompletion {
    if (it == null)
        block()
}

fun blockWhile(predicate: () -> Boolean) {
    while (predicate()) {
        //
    }
}

fun blockUntil(predicate: () -> Boolean) {
    while (!predicate()) {
        //
    }
}

suspend infix fun <T> Deferred<T>.thenTake(block: suspend (T) -> Unit) = thenRun(block)
suspend infix fun <T> Deferred<T>.then(block: suspend (T) -> T): T = block(await())
suspend infix fun <T, R> Deferred<T>.thenRun(block: suspend (T) -> R): R = block(await())

inline fun CoroutineScope.buildJob(crossinline block: AsyncJobBuilder.() -> Unit) =
    taking(object : AsyncJobBuilder(this) {}, block)

inline fun CoroutineScope.wrapJob(job: Job, crossinline block: AsyncJobBuilder.() -> Unit) = buildJob(block) executing job

abstract class AsyncJobBuilder(private val scope: CoroutineScope) {
    private var onSuccess: () -> Unit = {}
    private var onFailure: (Throwable) -> Unit = {}
    private var onCancel: (CancellationException) -> Unit = {}

    fun whenDone(block: () -> Unit) {
        onSuccess = block
    }

    fun whenError(block: (Throwable) -> Unit) {
        onFailure = block
    }

    fun whenCancelled(block: (CancellationException) -> Unit) {
        onCancel = block
    }

    infix fun executing(block: suspend CoroutineScope.() -> Unit) = executing(scope.launch(block = block))

    infix fun executing(job: Job) = job.apply {
        invokeOnCompletion {
            when (it) {
                null -> onSuccess()
                is CancellationException -> onCancel(it)
                else -> onFailure(it)
            }
        }
    }
}

private val coroutineLog by slf4j { "Coroutines" }

private val pool = Executors.newScheduledThreadPool(ForkJoinPool.getCommonPoolParallelism().coerceAtLeast(2)) {
    thread(
        start = false,
        name = "Volte-Work-Thread",
        isDaemon = true,
        block = it::run
    )
}

val dispatcher by invoking { pool.asCoroutineDispatcher() }
val supervisor by lazy { SupervisorJob() }
val coroutineExceptionHandler = CoroutineExceptionHandler { _, t ->
    if (t is Error) {
        supervisor.cancel()
        throw t
    }
    if (t !is CancellationException)
        coroutineLog.error(t) { "Exception in coroutine" }
}

val scheduler = Scheduler()

val scope by lazy {
    CoroutineScope(dispatcher + supervisor + coroutineExceptionHandler)
}

fun<T> coroutines(block: CoroutineScope.() -> T): T = scope.block()

fun<T> transactionAsync(
    context: CoroutineDispatcher? = null,
    db: Database? = null,
    transactionIsolation: Int? = null,
    block: suspend Transaction.() -> T
): Deferred<T> = scope.transactionAsync(context, db, transactionIsolation, block)

fun<T> CoroutineScope.transactionAsync(
    context: CoroutineDispatcher? = null,
    db: Database? = null,
    transactionIsolation: Int? = null,
    block: suspend Transaction.() -> T
): Deferred<T> = async { newSuspendedTransaction(context, db, transactionIsolation, block) }
