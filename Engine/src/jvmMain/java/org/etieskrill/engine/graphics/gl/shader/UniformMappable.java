package org.etieskrill.engine.graphics.gl.shader;

import org.jetbrains.annotations.NotNull;

public interface UniformMappable {

    boolean map(@NotNull ShaderProgram.UniformMapper mapper);

}
