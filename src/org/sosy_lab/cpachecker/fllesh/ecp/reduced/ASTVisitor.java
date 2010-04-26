package org.sosy_lab.cpachecker.fllesh.ecp.reduced;

public interface ASTVisitor<T> {

  public T visit(Atom pAtom);
  public T visit(Concatenation pConcatenation);
  public T visit(Repetition pRepetition);
  public T visit(Union pUnion);
  
}
