TODO:

## General:
- move engine extensions to folder
- texture wrapping demo: drop down selector component
- ui builder dsl
- application segfaults on linux **AFTER** termination, so probably memory which was unduly freed, or not freed when it
  should have been?
- test on more architectures
- actual resource directory (+ lfs)

## Shader dsl:

- detect declaration usages by stage and if there is only one then move it there
- return list of statements (statements => block, declaration, statements) instead in order to push formatting further
  back
