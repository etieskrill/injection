import Tile.*
import java.math.BigInteger
import kotlin.math.pow

enum class Tile { EMPTY, HORSE, WALL }

data class Grid(
    val name: String,
    val size: Pair<Int, Int>,
    val budget: Int,
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
val tileKey = tileMap.map { it.value to it.key }.toMap()

val grids = ClassLoader
    .getSystemResource("grids.txt")
    .readText()
    .lines().filter { !it.startsWith("#") }.joinToString("\n")
    .split("---")
    .filter { it.isNotBlank() }
    .map {
        val lines = it.split('\n')
        val params = lines[0].split(' ').filter { it.isNotBlank() }
        val size = params[0].split("x").let { it[0].toInt() to it[1].toInt() }
        val budget = params[1].toInt()
        val name = params.subList(2, params.size).joinToString(" ")
        var hasHorse = false
        val tiles = lines.subList(1, lines.size - 1).map {
            it.mapNotNull {
                if (tileMap[it] == HORSE) {
                    check(!hasHorse) { "Each grid must have exactly one horse: $name has none" }
                    hasHorse = true
                }
                tileMap[it]
            }.toTypedArray() + Array(size.second - it.length) { EMPTY }
        }.toTypedArray()
        check(hasHorse) { "Each grid must have exactly one horse: $name has none" }

        Grid(name, size, budget, tiles)
    }

fun calculateScore(grid: Array<Array<Tile>>): Int? {
    if (grid.isEmpty()) throw IllegalArgumentException("Grid must not be empty")

    var horseX = 0;
    var horseY = 0
    grid.forEachIndexed { rowIndex, row ->
        row
            .forEachIndexed { colIndex, tile ->
                if (tile == HORSE) {
                    horseX = rowIndex; horseY = colIndex
                }
            }
    }
    val stack = mutableListOf<Pair<Int, Int>>(horseX to horseY)

    val visited = Array(grid.size) { Array(grid[0].size) { false } }
    visited[horseX][horseY] = true

    var score = 1 //horse counts as empty tile

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
//            println("Escaped")
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

fun gosperComb(n: Int, k: Int): Sequence<Long> {
    check(n <= 63)
    check(k <= n)

    var comb = (1L shl k) - 1L
    var first = true
    return generateSequence {
        if (first) {
            first = false
            return@generateSequence comb
        }

        val x = comb and -comb
        val y = comb + x
        comb = (((comb and y.inv()) / x) shr 1) or y

        return@generateSequence comb.takeIf { comb < (1L shl n) }
    }
}

fun findBestEnclosure(grid: Grid): Pair<Int, Array<Array<Tile>>> {
    check(grid.budget > 0) { "Cannot enclose without walls" } //even if already enclosed

    val numVariables = grid.grid.sumOf { it.count { it == EMPTY } }

    //partition search space for 63 bit gosper pattern

    var maxScore = 0
    var maxGrid: Array<Array<Tile>>? = null

    for (i in gosperComb(numVariables, grid.budget)) {
        val copy = grid.grid.copyOf()
        for ((index, row) in grid.grid.withIndex()) copy[index] = row.copyOf()

        var lookupIndex = 0
        val emptyLookup = Array<Pair<Int, Int>>(numVariables) { -1 to -1 }
        copy.forEachIndexed { y, row ->
            row.forEachIndexed { x, column ->
                if (copy[y][x] == EMPTY) emptyLookup[lookupIndex++] = y to x
            }
        }
        check(lookupIndex == emptyLookup.size)
        emptyLookup.forEachIndexed { index, (y, x) ->
            if (i shr index and 1L == 1L) {
                check(grid.grid[y][x] == EMPTY)
                copy[y][x] = WALL
            }
        }

        val score = calculateScore(copy)
        if (score != null && score > maxScore) {
            maxScore = score
            maxGrid = copy
        }
//        if () println("\r$i / ${(1L shl numVariables) - 1L}")
    }

    if (maxGrid == null) error("Horse cannot be enclosed with given number of walls")

    return maxScore to maxGrid
}

fun main() {
    val numVariables = grids[2].grid.sumOf { it.count { it == EMPTY } }
    println("num variables: 2^$numVariables")

    val (maxScore, maxGrid) = findBestEnclosure(grids[2])

    println(buildString {
        appendLine("Best score: $maxScore")
        for (row in maxGrid) {
            for (column in row) {
                append("${tileKey[column]} ")
            }
            appendLine()
        }
    })
}
