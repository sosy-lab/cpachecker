package org.sosy_lab.cpachecker.fshell.fql2.ast;

import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.ast.filter.Identity;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.PathPattern;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.Repetition;
import org.sosy_lab.cpachecker.fshell.fql2.parser.FQLParser;

public class FQLSpecification {
  private CoverageSpecification mCoverageSpecification;
  private PathPattern mPathPattern;
  
  private static PathPattern mDefaultPassingClause = new Repetition(new Edges(Identity.getInstance()));
  
  public static PathPattern getDefaultPassingClause() {
    return mDefaultPassingClause;
  }
  
  public FQLSpecification(CoverageSpecification pCoverageSpecification, PathPattern pPathPattern) {
    mCoverageSpecification = pCoverageSpecification;
    mPathPattern = pPathPattern;
  }
  
  public FQLSpecification(CoverageSpecification pCoverageSpecification) {
    mCoverageSpecification = pCoverageSpecification;
    mPathPattern = null;
  }
  
  public CoverageSpecification getCoverageSpecification() {
    return mCoverageSpecification;
  }
  
  public boolean hasPassingClause() {
    return (mPathPattern != null);
  }
  
  public PathPattern getPathPattern() {
    if (!hasPassingClause()) {
      throw new UnsupportedOperationException();
    }
    
    return mPathPattern;
  }
  
  @Override
  public String toString() {
    if (hasPassingClause()) {
      return "COVER " + mCoverageSpecification.toString() + " PASSING " + mPathPattern.toString();
    }
    else {
      return "COVER " + mCoverageSpecification.toString();
    }
  }
  
  public static FQLSpecification parse(String pFQLSpecificationString) throws Exception {
    FQLParser lParser = new FQLParser(pFQLSpecificationString);

    Object pParseResult;

    try {
      pParseResult = lParser.parse().value;
    }
    catch (Exception e) {
      System.out.println(pFQLSpecificationString);

      throw e;
    }

    assert(pParseResult instanceof FQLSpecification);

    return (FQLSpecification)pParseResult;
  }
  
}
