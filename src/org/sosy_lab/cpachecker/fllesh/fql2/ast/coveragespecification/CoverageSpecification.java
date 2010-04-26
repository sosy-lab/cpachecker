package org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification;

public interface CoverageSpecification {

  public <T> T accept(ASTVisitor<T> pVisitor);
  
}
