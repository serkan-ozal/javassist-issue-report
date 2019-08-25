package ozal.serkan.javassist.issue.report

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author serkan
 */
class Runner {

    private val context = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher()

    fun run(): String {
        System.out.println("Will run")
        val result = runBlocking {
            try {
                withTimeout(TimeUnit.MINUTES.toMillis(1)) {
                    async(context) {
                        System.out.println("Running in async context ...")
                        "OK"
                    }.await()
                }
            } catch (t: Throwable) {
                System.err.println("Finished with exception ${t}")
                "FAIL"
            }
        }
        System.out.println("Result = ${result}")
        return result
    }

}
