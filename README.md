This repository contains tools for generating gnarly tests of a FIRRTL compiler which is not the [Scala FIRRTL Compiler (SFC)](https://github.com/chipsalliance/firrtl).
Tools in this repository generate representative FIRRTL circuits and their SFC output.
It is up to the user to integrate these tests into their FIRRTL compiler to compare their output vs. the SFC.


Requirements:

- [`scala-cli`](https://scala-cli.virtuslab.org/)

How to use this:

Run one of the programs to generate examples into a `build/` directory.
E.g., `scala-cli Invalid.scala`.
