package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast;

public interface FQLNode {

  public <T> T accept(ASTVisitor<T> pVisitor);
  
}
