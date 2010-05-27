package org.sosy_lab.cpachecker.fllesh.fql2.ast.terms;

public interface Term {

  public <T> T accept(TermVisitor<T> pVisitor);
  
}
