package org.sosy_lab.cpachecker.cfa.model;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.ControlCodeBuilder;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.ContextSwitch;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils.CFASequenceBuilder;

public class ContextSwitchEdge extends AbstractCFAEdge {
  private ContextSwitch contextSwitch;
  private boolean toScheduler;
  
  public ContextSwitchEdge(ContextSwitch contextSwitch, String pRawStatement, FileLocation pFileLocation, CFANode pPredecessor, CFANode pSuccessor, boolean toScheduler) {
    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    this.contextSwitch = contextSwitch;
    this.toScheduler = toScheduler;
    assert checkEdgeConsistency() : "Predecessor "+pPredecessor+" or successor "+pSuccessor+" edges doesn't fit to the context switch point " + contextSwitch.getContextSwtichNode() +"..."+contextSwitch.getContextSwitchReturnNode();
  }
  
  private boolean checkEdgeConsistency() {
    if(this.getPredecessor().equals(CFASequenceBuilder.DUMMY_NODE) || this.getSuccessor().equals(CFASequenceBuilder.DUMMY_NODE)) {
      return true;
    }
    CFANode csNode = contextSwitch.getContextSwtichNode();
    CFANode csReturnNode = contextSwitch.getContextSwitchReturnNode();
    if(csReturnNode == null) {
      return contextSwitch.getContextSwtichNode().equals(this.getPredecessor());
    }
    assert !toScheduler || this.getSuccessor().getFunctionName().equals(ControlCodeBuilder.THREAD_SIMULATION_FUNCTION_NAME);
    assert toScheduler || this.getPredecessor().getFunctionName().equals(ControlCodeBuilder.THREAD_SIMULATION_FUNCTION_NAME);
    return csNode.equals(this.getPredecessor()) || csReturnNode.equals(getSuccessor());
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.ContextSwtichEdge;
  }

  public boolean isToScheduler() {
    return toScheduler;
  }

  @Override
  public String getCode() {
    return toString();
  }
  

  public ContextSwitch getContextSwitch() {
    return contextSwitch;
  }

  @Override
  public String getDescription() {
    if (contextSwitch == null) {
      return "null";
    }

    CFANode jumpIn = contextSwitch.getContextSwtichNode();
    String edgeDescription = "context switch ";
    if (jumpIn.equals(getPredecessor())) {
      edgeDescription += contextSwitch.toString() + " -> " + "deligator";
    } else {
      assert getSuccessor().equals(contextSwitch.getContextSwitchReturnNode());
      edgeDescription += "deligator" + " -> " + contextSwitch.toString();
    }
    return edgeDescription;

  }

}
