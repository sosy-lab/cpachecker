package org.sosy_lab.cpachecker.fshell.fql2.normalization.pathpattern;

import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.PathPattern;

public interface PathPatternRewriter {

  public PathPattern rewrite(PathPattern pPattern);

}
