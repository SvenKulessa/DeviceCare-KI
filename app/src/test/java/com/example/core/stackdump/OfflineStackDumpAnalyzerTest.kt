package com.example.core.stackdump

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineStackDumpAnalyzerTest {
  private fun parse(text: String) =
    OfflineStackDumpAnalyzer.import(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))

  @Test fun jvmStatesAndGenericNamesAreCountedWithoutExposingNames() {
    val report = parse(
      """
      === JVM STACK ===
      Thread: Thread-356  [id=580  state=RUNNABLE  daemon=false]
      Thread: main  [id=2  state=WAITING  daemon=false]
      Thread: Thread-7  [id=11  state=TIMED_WAITING  daemon=false]
      Thread: user@example.com  [id=12  state=BLOCKED  daemon=false]
      """.trimIndent()
    )
    assertEquals(4, report.jvmHeaders)
    assertEquals(2, report.genericJvmThreadHeaders)
    assertEquals(1, report.jvmStates["RUNNABLE"])
    assertEquals(1, report.jvmStates["WAITING"])
    assertEquals(1, report.jvmStates["TIMED_WAITING"])
    assertEquals(1, report.jvmStates["BLOCKED"])
    assertFalse(report.toString().contains("user@example.com"))
  }

  @Test fun goIdsAreNotLiveCounts() {
    val report = parse(
      """
      === GO STACK ===
      goroutine 189506 [select, 1 minutes]:
      goroutine 172979 [IO wait, 8 minutes]:
      goroutine 84 [running, locked to thread]:
      """.trimIndent()
    )
    assertEquals(3, report.goHeaders)
    assertEquals(listOf(189506L, 172979L, 84L), report.goIdsSample)
    assertEquals(1, report.goStates["IO wait"])
    assertNull(report.liveGoroutineCount)
    assertTrue(report.evidence.any { it.code == "RESOURCE_LEAK_NOT_ESTABLISHED" && it.status == StackEvidenceStatus.NOT_PROVEN })
  }

  @Test fun networkFrameAppearancesAreNotConnectionCounts() {
    val report = parse(
      """
      === JVM STACK ===
      at java.net.SocketInputStream.socketRead0(Native Method)
      === GO STACK ===
      github.com/celzero/firestack/intra/dnsx/transport.go:1136
      github.com/celzero/firestack/intra/netstack/udp.go:336
      github.com/celzero/firestack/intra/netstack/tcp.go:336
      github.com/celzero/firestack/intra/tun2socks.go:446
      """.trimIndent()
    )
    assertEquals(1, report.networkFrameOccurrences["DNS"])
    assertEquals(1, report.networkFrameOccurrences["UDP"])
    assertEquals(1, report.networkFrameOccurrences["TCP"])
    assertEquals(1, report.networkFrameOccurrences["VPN_TUN"])
    assertEquals(1, report.networkFrameOccurrences["SOCKET_IO"])
    assertEquals(0, report.goHeaders)
  }

  @Test fun manyHeadersAreSuspectedButNotProvenLeaks() {
    val data = buildString {
      appendLine("=== JVM STACK ===")
      repeat(64) { appendLine("Thread: Thread-$it [id=$it state=RUNNABLE daemon=false]") }
    }
    val report = parse(data)
    assertTrue(report.evidence.any { it.code == "HIGH_GENERIC_THREAD_HEADER_COUNT" && it.status == StackEvidenceStatus.SUSPECTED })
    assertTrue(report.evidence.any { it.code == "RESOURCE_LEAK_NOT_ESTABLISHED" && it.status == StackEvidenceStatus.NOT_PROVEN })
  }

  @Test fun sampledIdsHaveHardCapWithoutInflatingLiveCounts() {
    val data = buildString {
      appendLine("=== GO STACK ===")
      repeat(80) { appendLine("goroutine " + (170000 + it) + " [select]:") }
    }
    val report = parse(data)
    assertEquals(80, report.goHeaders)
    assertEquals(64, report.goIdsSample.size)
    assertTrue(report.goIdsSampleTruncated)
    assertNull(report.liveGoroutineCount)
  }

  @Test fun rejectsOversizeInvalidUtf8BinaryAndLongLine() {
    assertEquals("STACK_IMPORT_TOO_LARGE",
      assertThrows(IllegalArgumentException::class.java) {
        OfflineStackDumpAnalyzer.import(ByteArrayInputStream(ByteArray(StackDumpContract.MAX_IMPORT_BYTES + 1)))
      }.message)
    assertEquals("STACK_IMPORT_INVALID_UTF8",
      assertThrows(IllegalArgumentException::class.java) {
        OfflineStackDumpAnalyzer.import(ByteArrayInputStream(byteArrayOf(0xC3.toByte(), 0x28)))
      }.message)
    assertEquals("STACK_IMPORT_BINARY_CONTENT",
      assertThrows(IllegalArgumentException::class.java) { parse("=== JVM STACK ===\u0000") }.message)
    assertEquals("STACK_IMPORT_LINE_TOO_LONG",
      assertThrows(IllegalArgumentException::class.java) {
        parse("=== GO STACK ===\n" + "z".repeat(StackDumpContract.MAX_LINE_CHARS + 1))
      }.message)
  }

  @Test fun unmarkedTextFailsClosedAndNeverPretendsMalwareProof() {
    val report = parse("Thread: Thread-1 [id=1 state=RUNNABLE]\ngoroutine 123 [running]:")
    assertEquals(0, report.jvmHeaders)
    assertEquals(0, report.goHeaders)
    assertTrue(report.evidence.any { it.code == "NO_RECOGNIZED_HEADERS" && it.status == StackEvidenceStatus.NOT_PROVEN })
    assertTrue(report.evidence.any { it.code == "MALWARE_NOT_ESTABLISHED" && it.status == StackEvidenceStatus.NOT_PROVEN })
    assertEquals(StackDumpContract.VERSION, report.contractVersion)
  }
}
