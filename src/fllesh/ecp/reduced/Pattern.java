package fllesh.ecp.reduced;

import fllesh.ecp.reduced.ASTVisitor;

public interface Pattern {

  public <T> T accept(ASTVisitor<T> pVisitor);
  
}
