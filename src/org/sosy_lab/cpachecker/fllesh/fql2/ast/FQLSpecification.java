package org.sosy_lab.cpachecker.fllesh.fql2.ast;

import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.PathPattern;

public class FQLSpecification {
  private CoverageSpecification mCoverageSpecification;
  private PathPattern mPathPattern;
  
  public FQLSpecification(CoverageSpecification pCoverageSpecification, PathPattern pPathPattern) {
    mCoverageSpecification = pCoverageSpecification;
    mPathPattern = pPathPattern;
  }
  
  public CoverageSpecification getCoverageSpecification() {
    return mCoverageSpecification;
  }
  
  public PathPattern getPathPattern() {
    return mPathPattern;
  }
  
  @Override
  public String toString() {
    return "COVER " + mCoverageSpecification.toString() + " PASSING " + mPathPattern.toString(); 
  }
  
}
