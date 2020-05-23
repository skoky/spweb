import kotlinx.coroutines.runBlocking
import spark.kotlin.Http
import spark.kotlin.ignite
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


data class Status(
        val workmode: String,
        val batteryVoltage: String,
        val inputPower: String,
        val chargingCurrent: String,
        val timestamp: String
)

var status = Status(workmode = "Starting", batteryVoltage = "-", inputPower = "-", timestamp = "-", chargingCurrent = "-")

fun main(args: Array<String>) {

    val http: Http = ignite()
    http.port(80)


    http.get("/") {
        get()
    }

    runBlocking { // launch a new coroutine in background and continue

        val buffer = ByteArray(1024)
        val packet = DatagramPacket(buffer, buffer.size)
        try {
            val socket = DatagramSocket(5555, InetAddress.getLocalHost());
            socket.broadcast = true
            println("Listening for UDP on ${socket.localPort}")
            while (true) {
                socket.receive(packet)
                val msg = String(buffer, 0, packet.length)
                status = parseStatus(msg)
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }

}

fun get(): String {

    val batMin = 47.5
    val batMax = 53.2
    val voltageRange1p = (batMax - batMin) / 100

    return try {

        val voltage = status.batteryVoltage.toFloat()
        val perc = ((voltage - batMin) / voltageRange1p).toInt()
        "${status.workmode}<br>" +
                "Solar power: ${status.inputPower}W<br>" +
                "Battery voltage: ${status.batteryVoltage}V&nbsp;&nbsp;${perc}%<br>" +
                "Battery charging: ${status.chargingCurrent}A<br>" +
                "At: ${status.timestamp}"
    } catch (e: Exception) {
        "${e.message}"
    }
}


fun parseStatus(data: String): Status {

    // SPV1;getWorkMode:%s;getBatteryVoltage:%s;getPvInputPower1:%s;at:%d;getChargingCurrent:%s
    val a = data.split(";")
    val mode = a[1].split(":")[1]
    val v = a[2].split(":")[1]
    val p = a[3].split(":")[1]
    val t = a[4].split(":")[1].trim().toLong()
    val d = LocalDateTime.ofInstant(Instant.ofEpochMilli(t), ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_DATE_TIME)
    val ck = a[5].split(":")[1]

    return Status(workmode = mode, batteryVoltage = v, inputPower = p, chargingCurrent = ck, timestamp = d.toString())
}
