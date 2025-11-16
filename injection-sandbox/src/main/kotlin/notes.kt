import io.github.oshai.kotlinlogging.KotlinLogging
import org.etieskrill.engine.application.App
import org.etieskrill.engine.graphics.Batch
import org.etieskrill.engine.graphics.camera.OrthographicCamera
import org.etieskrill.engine.scene.Node.ScaleMode
import org.etieskrill.engine.scene.Scene
import org.etieskrill.engine.scene.container.HBox
import org.etieskrill.engine.scene.container.VBox
import org.etieskrill.engine.scene.element.Button
import org.etieskrill.engine.scene.element.Checkbox
import org.etieskrill.engine.scene.element.Label
import org.etieskrill.engine.scene.element.TextField
import org.etieskrill.engine.util.ResourceReader
import org.etieskrill.engine.window.window
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryUtil.*
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
            scaleMode = ScaleMode.GROW
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
                Label("Wrapping").apply { margin = Vector4f(10f) },
                Checkbox {
                    logger.info { "Switch wrapping" }
                }.apply { margin = Vector4f(10f) }
            ),
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

    override fun dispose() {
        fileTypeBuffer.rewind()
        while (fileTypeBuffer.hasRemaining()) {
            val pointer = fileTypeBuffer.get()
            if (pointer != 0L) nmemFree(pointer)
        }
        fileTypeBuffer.free()
    }

}
