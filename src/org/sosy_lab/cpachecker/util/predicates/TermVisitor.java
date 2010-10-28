package org.sosy_lab.cpachecker.util.predicates;

public interface TermVisitor<T> {

  public T visit(Constant pConstant);
  public T visit(Variable pVariable);
  
}
