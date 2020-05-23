import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

internal class MainKtTest {

    val data = "SPV1;getWorkMode:Grid-tie with backup;getBatteryVoltage:47.6;power:10;at:1590093010655"

    @Test
    fun parseStatusTest() {
        val status = parseStatus(data)
        assertEquals("Grid-tie with backup", status.workmode)
        assertEquals("47.6", status.batteryVoltage)
        assertEquals("10", status.inputPower)
        assertEquals("2020-05-21T22:30:10.655", status.timestamp)
    }
}