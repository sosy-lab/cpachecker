package org.sosy_lab.cpachecker.cfa.model;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.ContextSwitch;

public class ContextSwitchSummaryEdge extends SummaryEdge {
  private ContextSwitch contextSwitch;
  
  public ContextSwitchSummaryEdge(String pRawStatement, FileLocation pFileLocation, CFANode pPredecessor, CFANode pSuccessor, ContextSwitch contextSwitch) {
    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    this.contextSwitch = contextSwitch;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.ContextSwitchSummaryEdge;
  }

  @Override
  public String getCode() {
    if(contextSwitch != null) {
      return contextSwitch.toString();
    }
    return "";
  }

}
