package org.etieskrill.engine.graphics.model

import org.etieskrill.engine.graphics.model.loader.loadToVAO
import org.joml.Vector2f
import org.joml.Vector3f

fun rectangle(position: Vector2f, size: Vector2f): Model {
    return rectangle(position.x(), position.y(), size.x(), size.y())
}

fun rectangle(x: Float, y: Float, width: Float, height: Float, material: Material? = null): Model {
    val x = if (width < 0) width else x
    val y = if (height < 0) height else y

    val vertices = listOf(
        Vertex(Vector3f(x, y, 0f), textureCoords = Vector2f(0f)),
        Vertex(Vector3f(x, y + height, 0f), textureCoords = Vector2f(0f, 1f)),
        Vertex(Vector3f(x + width, y, 0f), textureCoords = Vector2f(1f, 0f)),
        Vertex(Vector3f(x + width, y + height, 0f), textureCoords = Vector2f(1f, 1f))
    )

    val indices = listOf(0, 2, 1, 3, 1, 2)

    val mat = material ?: PhongMaterial()

    return Model(
        "internal_model_factory:quad",
        Node(
            "root",
            meshes = listOf(loadToVAO(vertices, indices, mat))
        )
    )
}

/**
 * @param x        horizontal position of circle sector centre
 * @param y        vertical position of circle sector centre
 * @param radius   circle sector radius
 * @param start    circle sector start point in degrees
 * @param end      circle sector end point in degrees
 * @param segments number of segments to subdivide the circle sector into
 * @return an indexed memory model of the circle sector
 */
fun circleSect(x: Float, y: Float, radius: Float, start: Float, end: Float, segments: Int): Model {
    check(radius > 0) { "Radius must be greater than zero" }
    check(segments > 2) { "Circle sector must have more than two segments" }

    TODO()

//    FloatBuffer vertices = BufferUtils.createFloatBuffer(3 * (segments + 2));
//    vertices.put(x).put(y).put(0f);
//
//    ShortBuffer indices = BufferUtils.createShortBuffer(segments + 2);
//    indices.put((short) 0);
//
//    for (short i = 0; i <= segments; i++) {
//        float subAngle = i * ((end - start) / segments) + start;
//
//        float subX = (float) (radius * Math.cos(Math.toRadians(subAngle))) + x;
//        float subY = (float) (radius * Math.sin(Math.toRadians(subAngle))) + y;
//
//        vertices.put(subX).put(subY).put(0f);
//        indices.put((short) (i + 1));
//    }
//
//    vertices.flip();
//    float[] vertices_a = new float[vertices.capacity()];
//    vertices.get(vertices_a);
//
//    float[] colours = new float[(int) (vertices_a.length / 0.75)];
//    Arrays.fill(colours, 1f);
//
//    float[] textures = new float[(int) (vertices_a.length / 1.5)];
//    Arrays.fill(textures, 0f);
//
//    indices.flip();
//    short[] indices_a = new short[indices.capacity()];
//    indices.get(indices_a);
//
//    //return loader.loadToVAO(vertices_a, colours, textures, indices_a, GL11C.GL_TRIANGLE_FAN);
//    throw new UnsupportedOperationException("Currently under major reconstruction");
}

fun circle(x: Float, y: Float, radius: Float, segments: Int): Model {
    //return circleSect(x, y, radius, 0, 360, segments);
    TODO("Currently under major reconstruction")
}

fun roundedRect(x: Float, y: Float, width: Float, height: Float, rounding: Float, segments: Int): Model {
    TODO()

//    if (rounding < 0) throw new IllegalArgumentException("corner rounding cannot be smaller than zero");
//    if (2 * rounding > width || 2 * rounding > height)
//        throw new IllegalArgumentException("rounding cannot exceed total width or height");
//
////        Model models = Model.of();
//
//    float xTopLeft = x + rounding, yTopLeft = y + height - rounding;
//    float xTopRight = x + width - rounding, yTopRight = y + height - rounding;
//    float xBottomLeft = x + rounding, yBottomLeft = y + rounding;
//    float xBottomRight = x + width - rounding, yBottomRight = y + rounding;
//
////        models.add(rectangle(x + rounding, y, width - 2 * rounding, height));
////        models.add(rectangle(x, y + rounding, rounding, height - 2 * rounding));
////        models.add(rectangle(x + width - rounding, y + rounding, rounding, height - 2 * rounding));
////        models.add(circleSect(xTopLeft, yTopLeft, rounding, 90, 180, segments));
////        models.add(circleSect(xTopRight, yTopRight, rounding, 0, 90, segments));
////        models.add(circleSect(xBottomLeft, yBottomLeft, rounding, 180, 270, segments));
////        models.add(circleSect(xBottomRight, yBottomRight, rounding, 270, 360, segments));
//
//    throw new UnsupportedOperationException("Currently under major reconstruction");
}

fun box(size: Vector3f): Model {
    TODO()
//    val transform = Transform(scale = size)
//    return EngineModelLoader.load("internal-model-factory:box") {
//        loadModel("box.obj", ModelLoaderOptions())
//    }
}

fun box(size: Vector3f, material: Material): Model {
    TODO()
//    var transform = new Transform();
//    transform.setScale(size);
//    Model baseBox = EngineModelLoader.INSTANCE.load("internal-model-factory:box", () -> {
//        var builder = new Model.Builder("box.obj");
//        builder.setInitialTransform(transform); //FIXME there is no way this works as intended
//        builder.setCulling(false);
//        return builder.build();
//    });
//    baseBox.getNodes().get(2).getMeshes().get(0).setMaterial(material);
//    return new Model(baseBox);
}

fun quadBox(size: Vector3f) {
    TODO()
//    var transform = new Transform();
//    transform.setScale(size);
//    return EngineModelLoader.INSTANCE.load("internal-model-loader:quad-box", () -> {
//        var builder = new Model.Builder("quad-box.obj");
//        builder.setInitialTransform(transform); //FIXME neither does this
//        return builder.build();
//    });
}

//TODO probs fibonacci or subdivision, test with phong, gouraud and flat shading
fun sphere(radius: Float, subdivisions: Int): Model {
    TODO("Currently under major reconstruction")
}
