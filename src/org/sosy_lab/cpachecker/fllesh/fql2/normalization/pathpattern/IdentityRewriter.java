package org.sosy_lab.cpachecker.fllesh.fql2.normalization.pathpattern;

import org.sosy_lab.cpachecker.fllesh.fql2.ast.pathpattern.PathPattern;

public class IdentityRewriter implements PathPatternRewriter {

  private static IdentityRewriter mInstance = new IdentityRewriter();
  
  public static IdentityRewriter getInstance() {
    return mInstance;
  }
  
  private IdentityRewriter() {
    
  }
  
  @Override
  public PathPattern rewrite(PathPattern pPattern) {
    return pPattern;
  }

}
