import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.application.App
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.scene.component.Button
import org.etieskrill.engine.scene.component.Checkbox
import org.etieskrill.engine.scene.component.container.HBox
import org.etieskrill.engine.scene.component.Label
import org.etieskrill.engine.scene.component.TextField
import org.etieskrill.engine.scene.component.container.VBox
import org.etieskrill.engine.util.ResourceReader
import org.etieskrill.engine.window.window
import org.joml.Vector2f
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.system.Platform
import org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog
import org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_saveFileDialog
import java.io.File

private val logger = KotlinLogging.logger {}

fun main() {
    Noteify.run()
}

object Noteify : App(window {
    resizeable = true
}) {

    private var openFile: String? = null
    private var changed = false
    private var title = ""

    private val userHome = when (Platform.get()) {
        Platform.WINDOWS -> "${System.getProperty("user.home")}\\Documents\\"
        Platform.LINUX -> "${System.getProperty("user.home")}/"
        else -> throw TODO("Unsupported platform: ${Platform.get()}")
    }

    private val fileTypes = listOf("txt")
    private val fileTypeBuffer = BufferUtils.createPointerBuffer(fileTypes.size)
    private val FILTER_NAME = "Text File (${fileTypes.joinToString(", ") { "*.$it" }})"

    init {
        fileTypes.forEach {
            fileTypeBuffer.put(memAddress(memUTF8("*.$it")))
        }
        fileTypeBuffer.flip()
    }

    init {
        val textField = TextField(pacer).apply {
            changeCallback = { changed = true }
            size = Vector2f(200f)
        }

        val root = VBox(
            HBox(
                Button(Label("Save")) {
                    save(false, textField.textEditor.toString())
                    changed = false
                    logger.info { "Saved at" }
                }.apply { margin = Vector4f(10f) },
                Button(Label("Save as")) {
                    save(true, textField.textEditor.toString())
                    changed = false
                    logger.info { "Saved as $openFile" }
                }.apply { margin = Vector4f(10f) },
                Button(Label("Open")) {
                    openFile = tinyfd_openFileDialog(
                        "Select text file", openFile ?: userHome,
                        fileTypeBuffer, FILTER_NAME, false
                    ) ?: return@Button logger.info { "User canceled open file dialog" }

                    val content = ResourceReader.getResource(openFile)!!
                    textField.textEditor.text = content

                    changed = false

                    logger.info { "Opened file: $openFile" }
                }.apply { margin = Vector4f(10f) },
                Label("Wrapping"),
                Checkbox {
                    logger.info { "Switch wrapping" }
                }
            ).apply { size = Vector2f(600f, 150f) },
            textField
        )

        window.scene = Scene(
            Batch(renderer, window.currentSize),
            root,
            OrthographicCamera(window.currentSize)
        )
    }

    private fun save(saveAs: Boolean, text: String) {
        if (saveAs || openFile == null) {
            openFile = tinyfd_saveFileDialog(
                if (saveAs) "Save file as" else "Save file", openFile ?: userHome,
                fileTypeBuffer, FILTER_NAME
            ) ?: return logger.info { "User canceled save file dialog" }
        }

        File(openFile!!).writeText(text)
        window.title = "Noteify - $openFile"
    }

    override fun loop(delta: Double) {
        var title = ""
        if (changed) title += "*"
        title += "Noteify"
        if (openFile != null) title += " - $openFile"

        if (this.title != title) {
            window.title = title
            this.title = title
        }
    }

}
