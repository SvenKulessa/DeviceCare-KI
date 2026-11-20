package com.example.core.stackdump

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** Offline import of an explicitly supplied text stream; never inspects another process. */
object StackDumpContract {
  const val VERSION = "devicecare-stackdump/1"
  const val MAX_IMPORT_BYTES = 4 * 1024 * 1024
  const val MAX_LINES = 100_000
  const val MAX_LINE_CHARS = 16_384
  const val MAX_REPORTED_GO_IDS = 64
}

enum class StackEvidenceStatus { VERIFIED, SUSPECTED, NOT_PROVEN }

data class StackEvidence(
  val code: String,
  val status: StackEvidenceStatus,
  val occurrences: Int,
)

data class StackDumpAssessment(
  val contractVersion: String = StackDumpContract.VERSION,
  /** Captured JVM headers are not a complete OS thread census. */
  val jvmHeaders: Int,
  val jvmStates: Map<String, Int>,
  val genericJvmThreadHeaders: Int,
  /** Parsed Go headers; NOT an active goroutine count. */
  val goHeaders: Int,
  val goIdsSample: List<Long>,
  val goIdsSampleTruncated: Boolean,
  val goStates: Map<String, Int>,
  /** Stack frame appearances, NOT flows, packets or unique connections. */
  val networkFrameOccurrences: Map<String, Int>,
  val evidence: List<StackEvidence>,
) {
  val liveGoroutineCount: Int? = null
}

/**
 * Caller owns/closes input. No persistence, networking or Android APIs.
 * Fixed error codes avoid leaking source data through validation errors.
 */
object OfflineStackDumpAnalyzer {
  private val jvmHeader =
    Regex("""^Thread:\s+(.+?)\s+\[id=[0-9]+\s+state=([A-Z_]+)\b""")
  private val genericThread = Regex("""Thread-[0-9]+""")
  private val goHeader = Regex("""^goroutine\s+([0-9]+)\s+\[([^\]]+)\]:""")

  fun import(input: InputStream): StackDumpAssessment {
    val data = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var emptyReads = 0
    while (true) {
      val count = input.read(buffer)
      if (count == -1) break
      if (count == 0) {
        require(++emptyReads <= 3) { "STACK_IMPORT_STALLED" }
        continue
      }
      emptyReads = 0
      require(data.size() <= StackDumpContract.MAX_IMPORT_BYTES - count) { "STACK_IMPORT_TOO_LARGE" }
      data.write(buffer, 0, count)
    }
    val content =
      try {
        StandardCharsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(data.toByteArray())).toString()
      } catch (_: CharacterCodingException) {
        throw IllegalArgumentException("STACK_IMPORT_INVALID_UTF8")
      }
    require('\u0000' !in content) { "STACK_IMPORT_BINARY_CONTENT" }

    var section = ""
    var lines = 0
    var jvmCount = 0
    var genericCount = 0
    var goCount = 0
    val jvmStates = linkedMapOf<String, Int>()
    val goStates = linkedMapOf<String, Int>()
    val goIds = ArrayList<Long>()
    val frames = linkedMapOf<String, Int>()
    content.lineSequence().forEach { line ->
      lines++
      require(lines <= StackDumpContract.MAX_LINES) { "STACK_IMPORT_TOO_MANY_LINES" }
      require(line.length <= StackDumpContract.MAX_LINE_CHARS) { "STACK_IMPORT_LINE_TOO_LONG" }
      when (line.trim()) {
        "=== JVM STACK ===" -> section = "JVM"
        "=== GO STACK ===" -> section = "GO"
      }
      if (section == "JVM") {
        val match = jvmHeader.find(line)
        if (match != null) {
          jvmCount++
          if (genericThread.matches(match.groupValues[1])) genericCount++
          val state = match.groupValues[2].takeIf {
            it in setOf("RUNNABLE", "WAITING", "TIMED_WAITING", "BLOCKED", "NEW", "TERMINATED")
          } ?: "OTHER"
          jvmStates[state] = (jvmStates[state] ?: 0) + 1
        }
      }
      if (section == "GO") {
        val match = goHeader.find(line)
        if (match != null) {
          goCount++
          val id = match.groupValues[1].toLongOrNull()
          if (id != null && goIds.size < StackDumpContract.MAX_REPORTED_GO_IDS) goIds.add(id)
          val state = match.groupValues[2].substringBefore(',').trim().let {
            when (it) {
              "running", "runnable", "select", "chan receive", "IO wait",
              "syscall", "sync.WaitGroup.Wait", "sleep" -> it
              else -> "other"
            }
          }
          goStates[state] = (goStates[state] ?: 0) + 1
        }
      }
      if (section.isNotEmpty()) {
        val category = when {
          "intra/dnsx/" in line -> "DNS"
          "netstack/udp.go" in line || "gonet.(*UDPConn)" in line -> "UDP"
          "netstack/tcp.go" in line || "gonet.(*TCPConn)" in line -> "TCP"
          "tun2socks" in line || "tunnel/tunnel.go" in line -> "VPN_TUN"
          "SocketInputStream.socketRead" in line -> "SOCKET_IO"
          else -> null
        }
        if (category != null) frames[category] = (frames[category] ?: 0) + 1
      }
    }
    val evidence = mutableListOf<StackEvidence>()
    if (jvmCount > 0) evidence.add(StackEvidence("JVM_HEADERS_PARSED", StackEvidenceStatus.VERIFIED, jvmCount))
    if (goCount > 0) evidence.add(StackEvidence("GO_HEADERS_PARSED_NOT_LIVE_COUNT", StackEvidenceStatus.VERIFIED, goCount))
    if (genericCount > 0) evidence.add(StackEvidence("GENERIC_JVM_THREAD_HEADERS", StackEvidenceStatus.VERIFIED, genericCount))
    if (frames.isNotEmpty()) evidence.add(StackEvidence("NETWORK_FRAMES_OBSERVED", StackEvidenceStatus.VERIFIED, frames.values.sum()))
    if (genericCount >= 64) evidence.add(StackEvidence("HIGH_GENERIC_THREAD_HEADER_COUNT", StackEvidenceStatus.SUSPECTED, genericCount))
    if (jvmCount == 0 && goCount == 0) {
      evidence.add(StackEvidence("NO_RECOGNIZED_HEADERS", StackEvidenceStatus.NOT_PROVEN, 0))
    }
    evidence.add(StackEvidence("RESOURCE_LEAK_NOT_ESTABLISHED", StackEvidenceStatus.NOT_PROVEN, 0))
    evidence.add(StackEvidence("MALWARE_NOT_ESTABLISHED", StackEvidenceStatus.NOT_PROVEN, 0))
    return StackDumpAssessment(
      jvmHeaders = jvmCount,
      jvmStates = jvmStates.toMap(),
      genericJvmThreadHeaders = genericCount,
      goHeaders = goCount,
      goIdsSample = goIds.toList(),
      goIdsSampleTruncated = goCount > goIds.size,
      goStates = goStates.toMap(),
      networkFrameOccurrences = frames.toMap(),
      evidence = evidence.toList(),
    )
  }
}
