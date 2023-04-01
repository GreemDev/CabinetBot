package net.greemdev.cabinet.lib.util.parse

import net.greemdev.cabinet.lib.util.newCustomParser
import net.greemdev.cabinet.lib.util.string
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.floor

enum class ZalgoCharacters {
    Up,
    Middle,
    Down;
}

enum class ZalgoIntensity {
    Low,
    Medium,
    High;
}

private fun rand(max: Int) = floor(ThreadLocalRandom.current().nextDouble() * max).toInt()

fun generateZalgo(content: String, intensity: ZalgoIntensity, characters: Array<ZalgoCharacters>) = newCustomParser<String, String> {
    string {
        content.filter { it !in ZalgoChars }.forEach {
            val (up, mid, down) = when (intensity) {
                ZalgoIntensity.Low -> Triple(rand(8), rand(2), rand(8))
                ZalgoIntensity.Medium -> Triple(rand(16) / 2 + 1, rand(6) / 2, rand(16) / 2 + 1)
                ZalgoIntensity.High -> Triple(rand(64) / 4 + 3, rand(16) / 4 + 1, rand(64) / 4 + 3)
            }

            +it
            if (ZalgoCharacters.Up in characters)
                repeat(up) {
                    +ZalgoChars.up.randomElement()
                }
            if (ZalgoCharacters.Middle in characters)
                repeat(mid) {
                    +ZalgoChars.mid.randomElement()
                }
            if (ZalgoCharacters.Down in characters)
                repeat(down) {
                    +ZalgoChars.down.randomElement()
                }
        }
    }
}(content)

private fun<T> Array<T>.randomElement(): T = this[ThreadLocalRandom.current().nextInt(0, size + 1)]

private object ZalgoChars : Collection<Char> by ZalgoChars.combined() {
    private fun combined() = (up + mid + down).toList()

    val up = arrayOf(
        '\u030d', /*    ̍     */ '\u030e', /*     ̎     */ '\u0304', /*      ̄     */ '\u0305', /*     ̅     */
        '\u033f', /*    ̿     */ '\u0311', /*     ̑     */ '\u0306', /*      ̆     */ '\u0310', /*     ̐     */
        '\u0352', /*    ͒     */ '\u0357', /*     ͗     */ '\u0351', /*     ͑     */ '\u0307', /*      ̇     */
        '\u0308', /*     ̈     */ '\u030a', /*      ̊     */ '\u0342', /*     ͂     */ '\u0343', /*     ̓     */
        '\u0344', /*    ̈́     */ '\u034a', /*     ͊     */ '\u034b', /*     ͋     */ '\u034c', /*     ͌     */
        '\u0303', /*     ̃     */ '\u0302', /*      ̂     */ '\u030c', /*      ̌     */ '\u0350', /*     ͐     */
        '\u0300', /*     ̀     */ '\u0301', /*      ́     */ '\u030b', /*      ̋     */ '\u030f', /*      ̏     */
        '\u0312', /*     ̒     */ '\u0313', /*     ̓     */ '\u0314', /*     ̔     */ '\u033d', /*     ̽     */
        '\u0309', /*     ̉     */ '\u0363', /*     ͣ     */ '\u0364', /*     ͤ     */ '\u0365', /*     ͥ     */
        '\u0366', /*    ͦ     */ '\u0367', /*     ͧ     */ '\u0368', /*     ͨ     */ '\u0369', /*     ͩ     */
        '\u036a', /*    ͪ     */ '\u036b', /*     ͫ     */ '\u036c', /*     ͬ     */ '\u036d', /*     ͭ     */
        '\u036e', /*    ͮ     */ '\u036f', /*     ͯ     */ '\u033e', /*     ̾     */ '\u035b', /*     ͛     */
        '\u0346', /*    ͆     */ '\u031a'  /*     ̚     */
    )

    val mid = arrayOf(
        '\u0315', /*     ̕     */ '\u031b', /*     ̛     */ '\u0340', /*     ̀     */ '\u0341', /*     ́     */
        '\u0358', /*     ͘     */ '\u0321', /*     ̡    */ '\u0322', /*     ̢    */ '\u0327', /*     ̧     */
        '\u0328', /*      ̨     */ '\u0334', /*     ̴    */ '\u0335', /*     ̵    */ '\u0336', /*     ̶     */
        '\u034f', /*      ͏     */ '\u035c', /*     ͜    */ '\u035d', /*     ͝    */ '\u035e', /*    ͞     */
        '\u035f', /*     ͟     */ '\u0360', /*     ͠    */ '\u0362', /*     ͢    */ '\u0338', /*     ̸     */
        '\u0337', /*      ̷     */ '\u0361', /*     ͡    */ '\u0489'  /*     ҉_   */
    )

    val down = arrayOf(
        '\u0316', /*     ̖     */ '\u0317', /*     ̗     */ '\u0318', /*     ̘     */ '\u0319', /*     ̙     */
        '\u031c', /*     ̜     */ '\u031d', /*     ̝     */ '\u031e', /*     ̞     */ '\u031f', /*     ̟     */
        '\u0320', /*     ̠     */ '\u0324', /*     ̤     */ '\u0325', /*     ̥     */ '\u0326', /*     ̦     */
        '\u0329', /*     ̩     */ '\u032a', /*     ̪     */ '\u032b', /*     ̫     */ '\u032c', /*     ̬     */
        '\u032d', /*     ̭     */ '\u032e', /*     ̮     */ '\u032f', /*     ̯     */ '\u0330', /*     ̰     */
        '\u0331', /*     ̱     */ '\u0332', /*     ̲     */ '\u0333', /*     ̳     */ '\u0339', /*     ̹     */
        '\u033a', /*     ̺     */ '\u033b', /*     ̻     */ '\u033c', /*     ̼     */ '\u0345', /*     ͅ     */
        '\u0347', /*     ͇     */ '\u0348', /*     ͈     */ '\u0349', /*     ͉     */ '\u034d', /*     ͍     */
        '\u034e', /*     ͎     */ '\u0353', /*     ͓     */ '\u0354', /*     ͔     */ '\u0355', /*     ͕     */
        '\u0356', /*     ͖     */ '\u0359', /*     ͙     */ '\u035a', /*     ͚     */ '\u0323'  /*     ̣     */
    )
}