package org.sosy_lab.cpachecker.cpa.abm;

import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class RecursiveAnalysisFailedException extends CPATransferException {

  private static final long serialVersionUID = 3822584071233172171L;

  private int depth;
  
  public RecursiveAnalysisFailedException(CPAException e) {
    super(createMessage(e));
    
    if (e instanceof RecursiveAnalysisFailedException) {
      RecursiveAnalysisFailedException recursiveException = (RecursiveAnalysisFailedException)e;
      initCause(recursiveException.getCause());
      depth = recursiveException.depth + 1;
    } else {
      initCause(e);
      depth = 1;
    }
  }
  
  private static String createMessage(CPAException e) {
    if (e instanceof RecursiveAnalysisFailedException) {
      RecursiveAnalysisFailedException r = (RecursiveAnalysisFailedException)e;
      return "Error in recursive analysis at depth " + r.depth + ": " + r.getCause().getMessage(); 
    } else {
      return "Error in recursive analysis at depth 1: " + e.getMessage();
    }
  }
  
  @Override
  public CPAException getCause() {
    return (CPAException)super.getCause();
  }
}
