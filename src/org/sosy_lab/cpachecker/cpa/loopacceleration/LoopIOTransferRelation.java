// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.cpa.loopacceleration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class LoopIOTransferRelation extends SingleEdgeTransferRelation {

  final Collection<CFANode> allNodes;

  public LoopIOTransferRelation(CFA cfa) {
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
    List<LoopIOState> coll = new ArrayList<>();
    coll.add(new LoopIOState());
    return coll;
  }

}
