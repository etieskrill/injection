import Tile.*

enum class Tile { EMPTY, HORSE, WALL }

data class Grid(
    val name: String,
    val size: Pair<Int, Int>,
    val grid: Array<Array<Tile>>
) {
    override fun toString(): String {
        return "Grid(name='$name', size=$size, grid=${grid.contentDeepToString()})"
    }
}

val tileMap = mapOf(
    ' ' to EMPTY,
    'H' to HORSE,
    'W' to WALL
)

val grids = ClassLoader
    .getSystemResource("grids.txt")
    .readText()
    .lines().filter { !it.startsWith("#") }.joinToString("")
    .split("---")
    .filter { it.isNotBlank() }
    .map {
        val lines = it.split('\n')
        val params = lines[0].split(' ').filter { it.isNotBlank() }
        val name = params.subList(1, params.size - 1).joinToString("")
        val size = params[0].split("x").let { it[0].toInt() to it[1].toInt() }
        Grid(
            name, size,
            lines.subList(1, lines.size - 1).map {
                it.mapNotNull { tileMap[it] }.toTypedArray() + Array(size.second - it.length) { EMPTY }
            }.toTypedArray()
        )
    }

//
//val grid = arrayOf(
//    arrayOf(EMPTY, EMPTY, WALL, WALL, EMPTY),
//    arrayOf(WALL, EMPTY, EMPTY, EMPTY, EMPTY),
//    arrayOf(EMPTY, EMPTY, HORSE, WALL, WALL),
//    arrayOf(EMPTY, WALL, EMPTY, EMPTY, EMPTY),
//    arrayOf(EMPTY, WALL, WALL, EMPTY, EMPTY)
//)

val grid = arrayOf(
    arrayOf(EMPTY, WALL, WALL, WALL, EMPTY),
    arrayOf(WALL, EMPTY, EMPTY, WALL, EMPTY),
    arrayOf(WALL, EMPTY, HORSE, WALL, WALL),
    arrayOf(EMPTY, WALL, EMPTY, WALL, EMPTY),
    arrayOf(EMPTY, WALL, WALL, WALL, EMPTY)
)

fun calculateScore(grid: Array<Array<Tile>>): Int? {
    if (grid.isEmpty()) throw IllegalArgumentException("Grid must not be empty")

    val stack = mutableListOf<Pair<Int, Int>>(
        grid.size / 2 to grid[0].size / 2
    )

    val visited = Array(grid.size) { Array(grid[0].size) { false } }
    visited[grid.size / 2][grid[0].size / 2] = true

    var score = 1 //TODO does horse count as square?

    var iter = 0
    while (stack.isNotEmpty()) {
        if (iter++ > 100000) {
            println("Exceeded maximum iterations")
            return null
        }

        val (x, y) = stack.removeLast()

        val left = addTile(x - 1, y, grid, stack, visited)
        val right = addTile(x + 1, y, grid, stack, visited)
        val up = addTile(x, y + 1, grid, stack, visited)
        val down = addTile(x, y - 1, grid, stack, visited)

        if (left == -1 || right == -1 || up == -1 || down == -1) {
            println("Escaped")
            return null
        }

        score += left + right + up + down
    }

    return score
}

fun addTile(
    x: Int,
    y: Int,
    grid: Array<Array<Tile>>,
    stack: MutableList<Pair<Int, Int>>,
    visited: Array<Array<Boolean>>
): Int {
    if (x !in 0..<grid[0].size || y !in 0..<grid.size) return -1
    if (visited[y][x]) return 0

    visited[y][x] = true

    when (grid[y][x]) {
        WALL, HORSE -> return 0
        EMPTY -> {
            stack.add(x to y)
            return 1
        }
    }
}

fun main() {
//    println(calculateScore(grid))
    println(grids)
}
