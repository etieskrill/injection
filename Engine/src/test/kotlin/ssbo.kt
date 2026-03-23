import org.etieskrill.engine.graphics.gl.StorageBufferObject
import org.etieskrill.engine.graphics.gl.VertexArrayAccessor
import org.joml.Vector2f
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL46C.*
import org.lwjgl.opengl.GLCapabilities
import kotlin.test.Test
import kotlin.test.fail

class SSBOTest {

    data class Colour(
        val redGreen: Vector2f,
        val blue: Float,
        val alpha: Float
    )

    object ColourAccessor : VertexArrayAccessor<Colour>() {
        override fun registerFields() {
            addField<Vector2f> { vertex, buffer -> vertex.redGreen.get(buffer) }
            addField<Float> { vertex, buffer -> buffer.putFloat(vertex.blue) }
            addField<Float> { vertex, buffer -> buffer.putFloat(vertex.alpha) }
        }

        private inline fun <reified T> addField(mapper: FieldMapper<Colour>) = addField(T::class.java, mapper)
    }

    @Test
    fun ssbo() {
        glfwInit()

        val window = glfwCreateWindow(600, 500, "Test", 0L, 0L)
        glfwMakeContextCurrent(window)

        glfwSetKeyCallback(window) { window, key, scancode, action, mods ->
            if (action != GLFW_PRESS && mods == GLFW_MOD_CONTROL && (key == GLFW_KEY_W || key == GLFW_KEY_ESCAPE))
                glfwSetWindowShouldClose(window, true)
        }

        GLCapabilities.initialize()
        GL.createCapabilities()

        assert(glGetError() == GL_NO_ERROR)

//        val data = BufferUtils.createFloatBuffer(8)
//        Vector4f(0f, 0f, 1f, 1f).get(data).position(4)
//        data.put(1f).put(0f).put(0f).put(1f)
//            .flip()

//        val ssbo = glCreateBuffers()
//        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo)
//        glBufferData(GL_SHADER_STORAGE_BUFFER, data.remaining() * Float.SIZE_BYTES.toLong(), GL_STATIC_DRAW)
//        glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0L, data)
//        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, ssbo, 1)

        val ssbo = StorageBufferObject(2, ColourAccessor)
        assert(glGetError() == GL_NO_ERROR)
        ssbo.setData(listOf(Colour(Vector2f(0f, 0f), 1f, 1f), Colour(Vector2f(1f, 0f), 0f, 1f)))
        assert(glGetError() == GL_NO_ERROR)
        ssbo.bind(0)
        assert(glGetError() == GL_NO_ERROR)

        val vertexShader = glCreateShader(GL_VERTEX_SHADER)
        glShaderSource(vertexShader, """
            #version 430 core
            
            const vec2 vertices[4] = vec2[](vec2(-1, -1), vec2(1, -1), vec2(-1, 1), vec2(1, 1));
            
            void main() {
                gl_Position = vec4(vertices[gl_VertexID], 0, 1);
            }
        """.trimIndent())
        glCompileShader(vertexShader)
        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) != GL_TRUE) {
            println(glGetShaderInfoLog(vertexShader))
            fail()
        }

        assert(glGetError() == GL_NO_ERROR)

        val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(fragmentShader, """
            #version 430 core
            
            struct Colour {
                vec2 redGreen;
                float blue;
                float alpha;
            };
            
            layout (std430, binding = 0) readonly buffer SSBO {
                Colour colour[];
            };
            
            void main() {
                vec4 fragColour = vec4(0);
                
                for (int i = 0; i < colour.length(); i++) {
                    Colour c = colour[i];
                    fragColour += vec4(c.redGreen, c.blue, c.alpha);
                }
                
                gl_FragColor = fragColour;
            }
        """.trimIndent())
        glCompileShader(fragmentShader)
        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) != GL_TRUE) {
            println(glGetShaderInfoLog(fragmentShader))
            fail()
        }

        assert(glGetError() == GL_NO_ERROR)

        val program = glCreateProgram()
        glAttachShader(program, vertexShader)
        glAttachShader(program, fragmentShader)
        glLinkProgram(program)

        if (glGetProgrami(program, GL_LINK_STATUS) != GL_TRUE) {
            println(glGetProgramInfoLog(program))
            fail()
        }

        assert(glGetError() == GL_NO_ERROR)

        glUseProgram(program)

        assert(glGetError() == GL_NO_ERROR)

        val dummyVAO = glCreateVertexArrays()
        glBindVertexArray(dummyVAO)

        glDisable(GL_CULL_FACE)

        assert(glGetError() == GL_NO_ERROR)

        while (!glfwWindowShouldClose(window)) {
            glClearColor(0f, 0f, 0f, 1f)
            glClear(GL_COLOR_BUFFER_BIT)

            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)

            glfwSwapBuffers(window)
            glfwPollEvents()

            Thread.sleep(10)
        }

        glfwTerminate()
    }
}