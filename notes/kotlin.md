Kotlin is really cool both on the front- and backend of things. Receivers, extensions,
delegates, dsl builders, the very rich standard library and amazing collection system,
open source compiler backend, and multiplatform, just to name a few of both categories.

That said, if i ever designed a language myself, i would probably take the Kotlin ecosystem
and the language as a base, and change the following:

- Function parameters are `var`
- Add compact bloody ternary expressions using `?`, like any respectable language has
- Expression ifs returning Unit, Nothing, or a nullable type, are not required to have an 
  else branch, implicitly returning null, or nothing in case of Unit/Nothing

And some things i would personally like, but have not put enough thought into to know
whether it would actually be possible:

- Invert the name/type ordering. Probably even revert to return types instead of `fun`.
  An exception to this may be lambdas.

Also, about some experimental changes:

- Dunno if context parameters are going to be like a drop-in replacement, or as powerful
  as context receivers are now, but i certainly would not remove them. Just have `override`
  functions not have to declare the context explicitly when the super function has one.
