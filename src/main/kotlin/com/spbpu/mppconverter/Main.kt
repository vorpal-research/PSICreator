import com.spbpu.mppconverter.kootstrap.PSICreator
import com.spbpu.mppconverter.util.debugPrint

fun main() {
    val psi = PSICreator().getPSIForFile("test/HelloWorld.kt")
    psi.debugPrint()
}