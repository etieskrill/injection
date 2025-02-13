import io.github.etieskrill.injection.extension.shaderreflection.ShaderReflectionGenerator
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class ShaderReflectionGeneratorTest {

    private val fixture = ShaderReflectionGenerator(mockk())

    val preprocessorTestValues = mapOf(
        """
            #define A a
            A
        """ to """
            a
        """,
        """
            #pragma A B
            A
            B
        """ to """
            #pragma A B
            A
            B
        """,
        """
            #define A
            A
        """ to "",
        """
            #define A
            a
        """ to "a",
        """
            #define A 5
            uniform vec3[A];
        """ to "uniform vec3[5];",
        readResource("shaders/Animation.vert") to readResource("shaders/AnimationPreprocessedDefine.vert"),
    )

    @Test
    fun `Should process all '#define' macros`() {
        preprocessorTestValues.forEach { (value, expected) ->
            assertEquals(expected.trim(), fixture.resolveDefineDirectives(value.trim()).trim())
        }
    }

    @Test
    fun `Removes all preprocessor macros`() {
        for ((value, expected) in mapOf(
            "#version 330 core" to "",
            "" to "",
            """
                #version 330 core
                #define A a
            """ to "",
            "#pragma stage vert" to "",
            """
                #pragma stage vert
                #pragma stage frag
            """ to "",
            """
                struct Sleep {
                    int on;
                    float that;
                    bool thang;
                };
                
                #ifdef VERTEX_SHADER
                uniform vec3 somebody;
                uniform int once;
                #endif
                
                #ifdef FRAGMENT_SHADER
                uniform ivec4 told;
                uniform bool me;
                #endif
            """ to """
                struct Sleep {
                    int on;
                    float that;
                    bool thang;
                };
                
                
                uniform vec3 somebody;
                uniform int once;
                
                
                
                uniform ivec4 told;
                uniform bool me; 
            """,
            readResource("shaders/Animation.vert") to readResource("shaders/AnimationPreprocessed.vert"),
        )) {
            assertEquals(expected.trim(), fixture.resolvePreprocessorDirectives(value.trim()).trim())
        }
    }

    @Test
    fun `Removes all comments`() {
        for ((value, expected) in mapOf(
            "//" to "",
            "//  " to "",
            "//asdf" to "",
            "  //asdf" to "",
            " // asdf " to "",
            "/**/" to "",
            "/* */" to "",
            "/* asdf*/" to "",
            "/*asdf */" to "",
            "/* asdf */" to "",
            "/* asdf   */" to "",
            "/*\n\n*/" to "",
            readResource("shaders/Animation.vert") to readResource("shaders/AnimationNoComments.vert"),
        )) {
            assertEquals(expected.trim(), fixture.removeComments(value.trim()).trim().trimTrailing())
        }
    }

    @Test
    fun simplifyGlslSource() {
        for ((value, expected) in mapOf(
            "layout (location = 0) in vec3 position;" to "layout(location=0)in vec3 position;",
            "uniform  mat4   model;" to "uniform mat4 model;",
            "   attribute   vec3  normal;" to "attribute vec3 normal;",
            "vec4  pos  =   vec4 ( 0 . 0 ,  1 . 0 , 0.0 , 1.0 );" to "vec4 pos=vec4(0.0,1.0,0.0,1.0);",
            "if   ( a  >  b )  {  return  a ;  }" to "if(a>b){return a;}",
            """ layout (location = 1)
                attribute vec3 normal;""" to """layout(location=1)attribute vec3 normal; """,
            """ uniform mat4 
                model;
           
                uniform mat3 normal;""" to """uniform mat4 model;uniform mat3 normal; """,
            """ void main ( ) {
                    gl_Position = combined * model * vec4 ( a_Position , 1.0 ) ;
                } """ to """void main(){gl_Position=combined*model*vec4(a_Position,1.0);}""",
            readResource("shaders/Animation.vert") to readResource("shaders/AnimationSimplified.vert"),
        )) {
            assertEquals(expected.trim(), fixture.simplifyGlslSource(value.trim()).trim().trimTrailing())
        }
    }

    private fun readResource(path: String): String = ClassLoader.getSystemResource(path).readText()
    private fun String.trimTrailing(): String =
        lines().joinToString(separator = System.lineSeparator()) { it.trimEnd() }

}
