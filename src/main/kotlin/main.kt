import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Status(
        val workMode: String?,
        val batteryVoltage: String,
        val inputPower: String,
        val chargingCurrent: String,
        val timestamp: String,
        val batteryPowerCalc: String,
        val batterPerc: Int
)

fun emptyStatus() = Status(workMode = "Starting", batteryVoltage = "-", inputPower = "-",
        timestamp = "-", chargingCurrent = "-", batteryPowerCalc = "-", batterPerc = 0)

var status = emptyStatus()

fun main(args: Array<String>) {

    embeddedServer(Netty, 80) {
        routing {
            get("/") {
                updatePerc()
                call.respondHtml {
                    head {
                        meta {
                            httpEquiv = "refresh"
                            content = "3"
                        }
                    }
                    body {
                        if (status.workMode.isNullOrBlank()) {
                            p {
                                +"No data"
                            }
                        } else {
                            p{
                                +"Work mode: "
                                status.workMode?.let { +it }
                            }
                            p {
                                +"Solar power: "
                                +status.inputPower
                                +"W"
                            }
                            p {
                                +"Battery voltage: "
                                +status.batteryVoltage
                                +" -> "
                                +status.batterPerc.toString()
                                +"%"
                            }
                            p{
                                +"Battery charging: "
                                +status.chargingCurrent
                                +"A"
                            }
                            p{
                                +"Battery power: "
                                +status.batteryPowerCalc
                                +"W"
                            }
                            p {
                                +"At: "
                                +status.timestamp
                            }
                        }
                    }
                }
            }
        }
        println("Web server started")
    }.start(wait = false)

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
                if (msg.isNotBlank()) status = parseStatus(msg)
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }

}

fun updatePerc() {

    val batMin = 47.5
    val batMax = 53.2
    val voltageRange1p = (batMax - batMin) / 100

    try {

        val voltage = status.batteryVoltage.toFloat()
        val perc = ((voltage - batMin) / voltageRange1p).toInt()
        status = status.copy(batterPerc= perc)
    } catch (e: Exception) {
        status = status.copy(workMode= null)
    }
}


fun parseStatus(data: String?): Status {

    data?.let { it ->
        if (it.isEmpty()) {
            return emptyStatus()
        }


        // SPV1;getWorkMode:%s;getBatteryVoltage:%s;getPvInputPower1:%s;at:%d;getChargingCurrent:%s
        val a = it.split(";")
        if (a.size != 6) return emptyStatus().copy(workMode = "Parsing error")

        val mode = a[1].split(":")[1]
        val v = a[2].split(":")[1]
        val p = a[3].split(":")[1]
        val t = a[4].split(":")[1].trim().toLong()
        val d = LocalDateTime.ofInstant(Instant.ofEpochMilli(t), ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_DATE_TIME)
        val ck = a[5].split(":")[1]

        val batteryPower = (v.toFloat() * ck.toFloat()).toInt().toString()

        return Status(workMode = mode, batteryVoltage = v, inputPower = p,
                chargingCurrent = ck, timestamp = d.toString(), batteryPowerCalc = batteryPower, batterPerc = 0)
    }
    return emptyStatus()
}
