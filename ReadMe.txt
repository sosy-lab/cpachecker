CPAchecker 2008-11-26

Required build environment:
- install eclipse >= 3.3
- get C/C++ develoment kit (platform and sdk) < 5.0 or remove the IMacroScanner
  argument and import from StubCodeReaderFactory.java (5.0 and later)
- add eclipse/plugins to your classpath or add many many .jars
- use 1.6 Java SDK or contact Michael Tautschnig <tautschnig@forsyte.de> to
  obtain patches to make it work and compile with 1.5 (may show degraded
  performance, though)

