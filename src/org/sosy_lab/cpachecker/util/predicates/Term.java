package org.sosy_lab.cpachecker.util.predicates;

public interface Term {

  public <T> T accept(TermVisitor<T> pVisitor);
  
}
