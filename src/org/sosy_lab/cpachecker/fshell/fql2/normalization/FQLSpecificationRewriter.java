package org.sosy_lab.cpachecker.fshell.fql2.normalization;

import org.sosy_lab.cpachecker.fshell.fql2.ast.FQLSpecification;

public interface FQLSpecificationRewriter {

  public FQLSpecification rewrite(FQLSpecification pSpecification);
  
}
