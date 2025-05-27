import org.joml.Matrix4f
import kotlin.test.Test

private typealias PoolIndex = Int

private const val CHUNK_SIZE = 100

class MemoryAllocationTests {

    @Test
    fun naiveAllocation() {
        var totalAllocated = 0L

        val list = ArrayDeque<Matrix4f>(CHUNK_SIZE)
        repeatForSeconds(5f) {
            repeat(CHUNK_SIZE) {
                list += Matrix4f()
                totalAllocated++
            }
            list.clear()
        }

        println("Temporary list: ${list.size}")
        println("Total number of allocated matrices: $totalAllocated")
    }

    @Test
    fun pooledAllocation() {
        var totalAllocated = 0L

        val pool = mutableListOf<Matrix4f>()
        val freeIndices = ArrayDeque<PoolIndex>()

        class Pool(val allocations: MutableList<PoolIndex> = ArrayDeque(CHUNK_SIZE)) {
            fun matrix4f(): Matrix4f {
                if (freeIndices.isEmpty()) {
                    val matrix = Matrix4f()
                    pool += matrix
                    allocations += pool.indexOf(matrix)
                    return matrix.identity()
                }

                val index = freeIndices.removeFirst()
                //so to recap, the basic java implementation of the array list has a default capacity of 10, after
                // which, if a new element needs to grow the list, the capacity is INCREMENTED BY ONE, and better yet,
                // each #grow operation copies the entire backing fucking array into an array one larger in size. this
                // is of course just how the ArrayList works, but an increment of one? let's suffice it to say that this
                // is pretty much the most non-ideal case for this application.
                allocations += index
                return pool[index]
            }

            fun free() {
                freeIndices += allocations
                //allocations.clear() //optional as object is discarded
            }
        }

        fun pooled(block: (Pool) -> Unit) {
            val pool = Pool()
            block(pool)
            pool.free()
        }

        repeatForSeconds(5f) {
            pooled { pool ->
                repeat(CHUNK_SIZE) {
                    pool.matrix4f()
                    totalAllocated++
                }
            }
        }

        println("Pool size: ${pool.size}")
        println("Total number of allocated matrices: $totalAllocated")
    }

}

private fun repeatForSeconds(seconds: Float, block: () -> Unit) {
    val start = System.nanoTime()
    while (System.nanoTime() - start < 1_000_000_000 * seconds) {
        block()
    }
}

private fun useMatrices(matrices: List<Matrix4f>) {
    matrices.chunked(100).forEach { chunk ->
        chunk.forEachIndexed { i, matrix ->
            when {
                i % 4 == 0 -> matrix.translate(i.toFloat(), 2f * i, 3f * i)
//                i % 4 == 1 -> matrix.rotate()
            }
        }
    }
}
