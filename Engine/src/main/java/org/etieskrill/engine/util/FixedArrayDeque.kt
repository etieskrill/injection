package org.etieskrill.engine.util;

/**
 * An implementation of [ArrayDeque] with a fixed size. If any element is added while the maximum capacity is reached,
 * the first element that was added from the perspective of the writing direction will be overridden, which is the
 * element at [tail](ArrayDeque.last) for [addFirst], and [head](ArrayDeque.first) for [addLast].
 *
 * @param E the type of elements in the collection
 */
class FixedArrayDeque<E>(numElements: Int) : Collection<E> {

    val capacity: Int = numElements
    override val size: Int get() = deque.size

    val deque = ArrayDeque<E>(numElements)

    val isFull get() = deque.size == capacity

    fun add(element: E) = addLast(element)

    fun addFirst(element: E) {
        if (deque.isNotEmpty() && deque.size >= capacity) {
            deque.removeLast()
        }
        deque.addFirst(element)
    }

    fun addLast(element: E) {
        if (deque.isNotEmpty() && deque.size >= capacity) {
            deque.removeFirst()
        }
        deque.addLast(element)
    }

    fun addAll(collection: Collection<E>) = collection.forEach(this::addLast)

    fun fill(element: E) = deque.fill(element)

    operator fun plusAssign(element: E) {
        deque.add(element)
    }

    override fun isEmpty() = deque.isEmpty()
    override fun contains(element: E) = deque.contains(element)
    override fun iterator() = deque.iterator()
    override fun containsAll(elements: Collection<E>) = deque.containsAll(elements)

}

@JvmName(name = "averageOfInt")
fun FixedArrayDeque<Int>.average() = deque.average()
@JvmName(name = "averageOfLong")
fun FixedArrayDeque<Long>.average() = deque.average()
@JvmName(name = "averageOfFloat")
fun FixedArrayDeque<Float>.average() = deque.average()
@JvmName(name = "averageOfDouble")
fun FixedArrayDeque<Double>.average() = deque.average()
