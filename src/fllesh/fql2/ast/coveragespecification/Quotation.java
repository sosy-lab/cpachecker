package fllesh.fql2.ast.coveragespecification;

import fllesh.fql2.ast.pathpattern.PathPattern;

public class Quotation implements CoverageSpecification {

  private PathPattern mPathPattern;
  
  public Quotation(PathPattern pPathPattern) {
    mPathPattern = pPathPattern;
  }
  
  public PathPattern getPathPattern() {
    return mPathPattern;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "\"(" + mPathPattern.toString() + ")\"";
  }

}
