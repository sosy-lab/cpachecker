package org.sosy_lab.cpachecker.fllesh.fql2.ast.terms;

public interface TermVisitor<T> {

  public T visit(Constant pConstant);
  public T visit(Variable pVariable);
  
}
