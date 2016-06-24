/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
