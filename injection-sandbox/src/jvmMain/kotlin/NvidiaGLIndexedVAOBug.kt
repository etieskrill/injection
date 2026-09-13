import org.etieskrill.engine.graphics.gl.GLUtils.checkErrorThrowing
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11C.*
import org.lwjgl.opengl.GL15C.*
import org.lwjgl.opengl.GL20C.*
import org.lwjgl.opengl.GL30C.*

// GPU: Nvidia RTX 4070
// Nvidia driver version: 610.88

fun main() {
    NvidiaGLIndexedVAOBug()
}

class NvidiaGLIndexedVAOBug {

    init {
        glfwInit()
    }

    val window = glfwCreateWindow(200, 200, "Test", 0L, 0L)

    init {
        glfwMakeContextCurrent(window)
        GL.createCapabilities()
    }

    init {
        checkErrorThrowing()

        val vertexArray = glGenVertexArrays()

        val vertexBuffer = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer)
        glBufferData(GL_ARRAY_BUFFER, 3L * 2 * Float.SIZE_BYTES, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        val indexBuffer = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, 3L * Int.SIZE_BYTES, GL_STATIC_DRAW)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        glBindVertexArray(vertexArray)

        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer) //does NOT need to be explicitly rebound for vao
        glBufferSubData(GL_ARRAY_BUFFER, 0L, floatArrayOf(0.0f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f))

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer) //DOES need to be explicitly rebound for vao
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0L, intArrayOf(0, 1, 2))

        glEnableVertexAttribArray(0) //may be placed anywhere between binding of vao and attrib pointer call
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.SIZE_BYTES, 0L) //MUST be placed after vbo and ebo are bound to vao

        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        while (!glfwWindowShouldClose(window)) {
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            glDrawBuffers(GL_BACK)

            glBindVertexArray(vertexArray)
            glDrawElements(GL_TRIANGLES, 3, GL_UNSIGNED_INT, 0L)
            glBindVertexArray(0)

            glfwSwapBuffers(window)
            glfwPollEvents()
            Thread.sleep(16)
        }
    }

}
