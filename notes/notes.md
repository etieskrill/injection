# TODOs and hints to potential TODOs

- set GLFW_DECORATED window hint to false for borderless
- read up on [GLFW Window Hints](https://www.glfw.org/docs/latest/window.html#window_hints)
- the following are stored in a bound vao:
  - Calls to `glEnableVertexAttribArray` or `glDisableVertexAttribArray`
  - Vertex attribute configurations via `glVertexAttribPointer`
  - Vertex buffer objects associated with vertex attributes by calls to `glVertexAttribPointer`
  - the _last_ ebo bound with `glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ...)`
- vaos can bind multiple different vbos
- wireframes can be drawn using `glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)` and reset again with 
  `glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)`, read details at 
  [Khronos](https://registry.khronos.org/OpenGL-Refpages/gl4/html/glPolygonMode.xhtml)
- a variable `uniform` is global to a shader program, and as such can be accessed by eny of it's 
  attached shaders.


# Other Resources

- [Me like](https://en.wikipedia.org/wiki/Swizzling_(computer_graphics))