import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MainKtTest {

    private val data = "SPV1;getWorkMode:Grid-tie with backup;getBatteryVoltage:47.6;getPvInputPower1:10;at:1590093010655;getChargingCurrent:14.8"

    @Test
    fun parseStatusTest() {
        val status = parseStatus(data)
        assertEquals("Grid-tie with backup", status.workMode)
        assertEquals("47.6", status.batteryVoltage)
        assertEquals("10", status.inputPower)
        assertEquals("704", status.batteryPowerCalc)
        assertEquals("2020-05-21T22:30:10.655", status.timestamp)
    }


    @Test
    fun parseStatusTestNone() {
        val status = parseStatus("")
        assertEquals("Starting", status.workMode)
    }

    @Test
    fun parseStatusTestNull() {
        val status = parseStatus(null)
        assertEquals("Starting", status.workMode)
    }

    @Test
    fun parseStatusTestWrongData() {
        val status = parseStatus("no data")
        assertEquals("Parsing error", status.workMode)
    }

}