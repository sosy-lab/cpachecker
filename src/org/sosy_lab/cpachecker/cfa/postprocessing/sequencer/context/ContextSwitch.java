package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context;

import java.util.Collection;
import java.util.HashSet;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.ContextSwitchSummaryEdge;


public class ContextSwitch {
  /**
   * Context switch numbering start at 1. ContextSwitch number 0 is reserved for
   * the function start for every thread
   */
  private int contextSwitchNumber;
  private AThread thread;
  
  private CFANode jumpInNode;
  private Collection<CFAEdge> contextStatementCause = new HashSet<CFAEdge>();
  
  public ContextSwitch(int contextSwitchNumber, AThread thread, CFAEdge switchPoint) {
//    Preconditions.checkArgument(PThreadUtils.isContextSwitchPoint(switchPoint));
    
    
    this.thread = thread;
    this.jumpInNode = switchPoint.getSuccessor();
    this.contextSwitchNumber = contextSwitchNumber;
    this.contextStatementCause.add(switchPoint);
  }

  public AThread getThread() {
    return thread;
  }
  
  public int getContextSwitchNumber() {
    return contextSwitchNumber;
  }

  public CFANode getContextSwtichNode() {
    CFAEdge edge = jumpInNode.getEnteringSummaryEdge();
    if(edge instanceof ContextSwitchSummaryEdge) {
      return edge.getPredecessor();
    }
    return null;
  }
  
  public void setContextSwitchNode(CFANode contextSwitchNode) {
    assert !getContextSwitchReturnNode().equals(contextSwitchNode) 
        : "Inconsistent state. Cannot set the contextswitch node to the node where the thread jumps back";
    this.jumpInNode = contextSwitchNode;
  }

  public CFANode getContextSwitchReturnNode() {
    return jumpInNode;
  }
  
  public Collection<CFAEdge> getContextStatementCause() {
    return contextStatementCause;
  }
  
  public void addContextStatementCause(CFAEdge contextStatementCause) {
    assert canNewEdgeTriggerContextSwitch(contextStatementCause);
    assert contextStatementCause.getSuccessor().equals(jumpInNode);
    this.contextStatementCause.add(contextStatementCause);
  }
  
  private boolean canNewEdgeTriggerContextSwitch(CFAEdge contextStatementCause) {
    CFANode a = contextStatementCause.getPredecessor();
    CFANode b = contextStatementCause.getSuccessor();
    for(CFAEdge edge : this.contextStatementCause) {
      CFANode predecessor = edge.getPredecessor();
      CFANode successor = edge.getSuccessor();
      if(a.equals(predecessor)) {
        return false;
      }
      if(!b.equals(successor)) {
        return false;
      }
    }
    return true;
  }
  
  //TODO what is with function cloner!
//  public void addContextStatementCause(CFAEdge contextStatementCause) {
//    this.contextStatementCause.add(contextStatementCause);
//  }

  @Override
  public String toString() {
    String rep = "(" + thread + " pc=" + contextSwitchNumber + ")";

    return rep;
  }

  
  // FIXME find better solution for this
  public void replaceContextSwitchCause(CFAEdge toReplace, CFAEdge replace) {
    assert contextStatementCause.contains(toReplace);
    assert !contextStatementCause.contains(replace);
    contextStatementCause.remove(toReplace);
    contextStatementCause.add(replace);
  }

}
