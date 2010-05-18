package org.sosy_lab.cpachecker.fllesh.fql2.normalization;

import org.sosy_lab.cpachecker.fllesh.fql2.ast.FQLSpecification;

public interface FQLSpecificationRewriter {

  public FQLSpecification rewrite(FQLSpecification pSpecification);
  
}
