package org.sosy_lab.cpachecker.fllesh.fql2.normalization.pathpattern;

import org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.PathPattern;

public interface PathPatternRewriter {

  public PathPattern rewrite(PathPattern pPattern);
  
}
