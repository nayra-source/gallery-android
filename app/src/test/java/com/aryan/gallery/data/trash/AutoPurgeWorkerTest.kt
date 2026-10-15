package com.aryan.gallery.data.trash

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.Executor

import com.aryan.gallery.config.NotificationConfig

class AutoPurgeWorkerTest {

    private val context: Context = mock()
    private val workerParams: WorkerParameters = mock()
    private val trashRepository: TrashRepository = mock()

    private lateinit var contextCompatMock: MockedStatic<ContextCompat>

    @Before
    fun setUp() {
        contextCompatMock = mockStatic(ContextCompat::class.java)
        contextCompatMock.`when`<Int> { ContextCompat.checkSelfPermission(any(), any()) }
            .thenReturn(PackageManager.PERMISSION_DENIED)

        val directExecutor = Executor { command -> command.run() }
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val taskExecutor = object : TaskExecutor {
            override fun getMainThreadExecutor(): Executor = directExecutor
            override fun getSerialTaskExecutor(): androidx.work.impl.utils.taskexecutor.SerialExecutor =
                object : androidx.work.impl.utils.taskexecutor.SerialExecutor {
                    override fun execute(command: Runnable) { command.run() }
                    override fun hasPendingTasks(): Boolean = false
                }
            override fun getCoroutineScope(): kotlinx.coroutines.CoroutineScope = testScope
        }
        whenever(workerParams.taskExecutor).thenReturn(taskExecutor)
        whenever(workerParams.backgroundExecutor).thenReturn(directExecutor)
    }

    @After
    fun tearDown() {
        contextCompatMock.close()
    }

    @Test
    fun `doWork succeeds safely when notification permission is denied`() = runTest {
        val sampleEntity = TrashEntity(
            mediaId = 100L,
            displayName = "photo.jpg",
            uriString = "content://media/external/images/media/100",
            mediaType = "IMAGE",
            trashedAt = System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000,
            purgeAt = System.currentTimeMillis() - 1L * 24 * 60 * 60 * 1000
        )

        whenever(trashRepository.getExpiredTrashItems()).thenReturn(listOf(sampleEntity))

        val worker = AutoPurgeWorker(context, workerParams, trashRepository, NotificationConfig())
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }
}
