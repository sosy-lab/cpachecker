package org.sosy_lab.cpachecker.fllesh.ecp.reduced;

import org.sosy_lab.cpachecker.fllesh.ecp.reduced.ASTVisitor;

public interface Pattern {

  public <T> T accept(ASTVisitor<T> pVisitor);
  
}
