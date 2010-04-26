package org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern;

public interface PathPattern {

  public <T> T accept(ASTVisitor<T> pVisitor);
  
}
