package space.kscience.snark.ktor

fun main() {
    SNARKServer(LocalDataHolder(), 9090).run()
}