package org.sosy_lab.cpachecker.fllesh.fql2.normalization.coveragespecification;

import org.sosy_lab.cpachecker.fllesh.fql2.ast.coveragespecification.CoverageSpecification;

public interface CoverageSpecificationRewriter {

  public CoverageSpecification rewrite(CoverageSpecification pSpecification);
  
}
