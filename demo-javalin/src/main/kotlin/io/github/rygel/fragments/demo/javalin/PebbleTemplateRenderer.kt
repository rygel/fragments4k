package io.github.rygel.fragments.demo.javalin

import io.github.rygel.fragments.javalin.TemplateRenderer
import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.loader.ClasspathLoader
import java.io.StringWriter

class PebbleTemplateRenderer : TemplateRenderer {
    private val engine =
        PebbleEngine
            .Builder()
            .loader(ClasspathLoader())
            .strictVariables(false)
            .build()

    override fun render(
        template: String,
        viewModel: Any,
    ): String {
        val compiled = engine.getTemplate("templates/$template.pebble")
        val writer = StringWriter()
        compiled.evaluate(writer, mapOf("viewModel" to viewModel))
        return writer.toString()
    }
}
