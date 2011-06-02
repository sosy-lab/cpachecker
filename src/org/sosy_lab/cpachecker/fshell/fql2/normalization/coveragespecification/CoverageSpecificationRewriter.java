package org.sosy_lab.cpachecker.fshell.fql2.normalization.coveragespecification;

import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.CoverageSpecification;

public interface CoverageSpecificationRewriter {

  public CoverageSpecification rewrite(CoverageSpecification pSpecification);

}
