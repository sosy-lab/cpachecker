/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.loopacceleration;

import java.util.ArrayList;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class LoopIOTransferRelation extends SingleEdgeTransferRelation {

  CFA cfa;
  final Collection<CFANode> allNodes;

  public LoopIOTransferRelation(CFA cfa) {
    this.cfa = cfa;
    allNodes = cfa.getAllNodes();
  }

  @Override
  public Collection<? extends AbstractState>
      getAbstractSuccessorsForEdge(AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
          throws CPATransferException, InterruptedException { // TODO Auto-generated method stub
    /**
     * if (allNodes == null) { throw new CPATransferException("CPA not properly initialized."); }
     *
     * ArrayList<LoopIOState> coll = new ArrayList<>();
     *
     * boolean loopstart = false;
     *
     * for (CFANode node : allNodes) {
     *
     * if (node.isLoopStart()) { coll.add(getInputsFromLoopNodes(getNodesInLoop(node))); } }
     *
     * for (CFANode node : allNodes) { for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) { if
     * (edge.getEdgeType().equals(CFAEdgeType.StatementEdge)) { for (LoopIOState ls : coll) { if
     * (ls.getEnd() < node.getNodeNumber()) { for (String inp : ls.getInput()) { if
     * (edge.getEdgeType().equals(CFAEdgeType.StatementEdge)) { if
     * (inp.contains(CFAEdgeUtils.getLeftHandVariable(edge)) &&
     * !ls.getOutput().contains(CFAEdgeUtils.getLeftHandVariable(edge))) {
     * ls.addToOutput(CFAEdgeUtils.getLeftHandVariable(edge)); } }
     *
     * } } } } } } return coll; }
     *
     * private LoopIOState getInputsFromLoopNodes(ArrayList<CFANode> loopNodes) { ArrayList<CFANode>
     * ln = loopNodes;
     *
     * LoopIOState ls = new LoopIOState();
     *
     * for (CFANode node : ln) { if (node.isLoopStart()) { ls.setName(node.getNodeNumber()); } else
     * { for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) { if
     * (edge.getDescription().contains("LOOPEND")) { ls.setEnd(edge.getLineNumber()); } else if
     * (edge.getEdgeType().equals(CFAEdgeType.StatementEdge)) {
     * ls.addToInput(CFAEdgeUtils.getLeftHandVariable(edge)); } } } }
     *
     * return ls; }
     *
     * private ArrayList<CFANode> getNodesInLoop(CFANode node) { boolean end = false; CFANode
     * startNode = node; ArrayList<CFANode> tempNode = new ArrayList<>(); tempNode.add(0, node);
     * ArrayList<CFANode> allNodesInLoop = new ArrayList<>();
     *
     * while (end != true) { CFANode oldNode = tempNode.get(0); allNodesInLoop.add(tempNode.get(0));
     * if (!CFAUtils.allSuccessorsOf(oldNode).contains(startNode)) { tempNode.addAll(0,
     * CFAUtils.allSuccessorsOf(oldNode).toList()); } else { ArrayList<CFANode> temp = new
     * ArrayList<>(); temp.addAll(CFAUtils.allSuccessorsOf(oldNode).toList());
     * temp.remove(startNode); tempNode.addAll(0, temp); } if
     * (oldNode.getLeavingEdge(0).getDescription().contains("LOOPEND")) { end = true; }
     * tempNode.remove(oldNode); } return allNodesInLoop;
     **/
    if (allNodes == null) {
      throw new CPATransferException("CPA not properly initialized.");
    }
    ArrayList<LoopIOState> coll = new ArrayList<>();
    coll.add(new LoopIOState());
    return coll;
  }

}
