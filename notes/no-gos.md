# Lombok

- Only use annotations specified in the documentation of
  the ["official plugin"](https://kotlinlang.org/docs/lombok.html#supported-annotations),
  i.e.:
  > Basics, like:
  > - Getter, Setter
  > - Builder
  > - Data
  > - ToString
  >
  > But no experimental features or extensions like:
  > - Slf4j
  >
  > Some features like `@Accessors` are okay anyway?

  The `engine` module uses the `io.freefair.lombok` plugin, which autoconfigures _Lombok_ for both **Java** and
  **Kotlin**, but follows the above guidelines.
