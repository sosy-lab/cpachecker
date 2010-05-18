package org.sosy_lab.cpachecker.fllesh.fql2.normalization;

import org.sosy_lab.cpachecker.fllesh.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.PathPattern;
import org.sosy_lab.cpachecker.fllesh.fql2.normalization.coveragespecification.CoverageSpecificationRewriter;
import org.sosy_lab.cpachecker.fllesh.fql2.normalization.pathpattern.PathPatternRewriter;

public class CompositeFQLSpecificationRewriter implements FQLSpecificationRewriter {

  private CoverageSpecificationRewriter mCoverRewriter;
  private PathPatternRewriter mPassingRewriter;
  
  public CompositeFQLSpecificationRewriter(CoverageSpecificationRewriter pCoverRewriter, PathPatternRewriter pPassingRewriter) {
    mCoverRewriter = pCoverRewriter;
    mPassingRewriter = pPassingRewriter;
  }
  
  @Override
  public FQLSpecification rewrite(FQLSpecification pSpecification) {
    CoverageSpecification lCover = pSpecification.getCoverageSpecification();
    PathPattern lPassing = pSpecification.getPathPattern();
    
    CoverageSpecification lNewCover = mCoverRewriter.rewrite(lCover);
    PathPattern lNewPassing = mPassingRewriter.rewrite(lPassing);
    
    if (lNewCover.equals(lCover) && lNewPassing.equals(lPassing)) {
      return pSpecification;
    }
    else {
      return new FQLSpecification(lNewCover, lNewPassing);
    }
  }

}
