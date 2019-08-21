The files in this directory are examples as small as possible that make errors
in CPAchecker visible. Once the errors are fixed, they serve as regression
checks.

- modulo.cil.c: MathsatSymbolicFormulaManager did not handle op_modulo up to
  r438, causing an indefinite recursion.
- global_vars.cil.c: buildAbstraction skips DeclarationEdge, even though it
  would get handled in MathsatSymbolicFormulaManager; as such, the information
  about global variables was inconsistent because buildCounterExample does _not_
  skip those edges. The code is built from a stripped-down version of encode.c
  from the inn news server.
- path_duplication.cil.c: performRefinement of ExplicitTransferRelation stops
  after seeing seemingly the same path more than two times. The root cause of
  this problem is still being investigated. The code is built from
  kbfiltr_simpl1.cil.c by removing all the pieces of the code that did not
  affect the relevant path.

