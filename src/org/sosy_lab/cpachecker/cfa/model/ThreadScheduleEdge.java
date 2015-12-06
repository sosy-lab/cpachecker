package org.sosy_lab.cpachecker.cfa.model;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.AThread;


public class ThreadScheduleEdge extends AbstractCFAEdge {
  private AThread thread;

  public ThreadScheduleEdge(CFANode pPredecessor, CFANode pSuccessor, AThread thread) {
    super("", FileLocation.DUMMY, pPredecessor, pSuccessor);
    
    this.thread = thread;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.ThreadScheduleEdge;
  }

  @Override
  public String getCode() {
    return thread.toString();
  }
  
  public AThread getThreadContext() {
    return thread;
  }

}
