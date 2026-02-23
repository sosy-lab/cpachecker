// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class TailRecursionEliminationProgramTransformation extends ProgramTransformation{
  public ProgramTransformationBehaviour behaviour = ProgramTransformationBehaviour.PRECISE;

  @Override
  public Optional<ProgramTransformationInformation> canBeApplied(CFA pCFA) {
    return Optional.empty();
  }

  @Override
  public SubCFA transform(CFA pCFA, ProgramTransformationInformation pInfo) {
    // TODO WIP!
    ArrayList<CFANode> nodes = new ArrayList<>();
    ArrayList<CFAEdge> edges = new ArrayList<>();
    boolean isFinished = false;
    ArrayList<Integer> visitedNodes = new ArrayList<>();

    ImmutableList<? extends AParameterDeclaration> parameters = pInfo.entryNode().getFunction().getParameters();
    int parameterNum = parameters.size();

    // Create new start node and set it as LoopStart
    nodes.add(CFANode.newDummyCFANode(pInfo.entryNode().getFunctionName()));
    nodes.getFirst().setLoopStart();
    visitedNodes.add(pInfo.entryNode().getNodeNumber());

    // iterate through nodes and copy them + adding edges to the previous one
    CFANode currentNode = pInfo.entryNode();
    CFANode previousNewNode = nodes.getLast();
    while (!isFinished) {
      FluentIterable<CFAEdge> leavingEdges = currentNode.getLeavingEdges();
      switch (leavingEdges.size()) {
        // case 0, we reached the last edge in this function
        case 0:
          break;
        // case 1, add equivalent edge to edges
        case 1:
          nodes.add(CFANode.newDummyCFANode());
          CFAEdgeType edgeType = leavingEdges.get(0).getEdgeType();
          break;
        case 2:
          break;
        default:
          break;
      }
    }

    // add n extra nodes, where n is the number of function parameters

    // connect the last extra node to the initial node to create a loop


    return new SubCFA(
        pInfo.entryNode(),
        pInfo.exitNode(),
        nodes.getFirst(),
        nodes.getLast(),
        ProgramTransformationEnum.TAIL_RECURSION_ELIMINATION,
        ProgramTransformationBehaviour.PRECISE,
        ImmutableSet.of(),
        ImmutableSet.of()
    );
  }

  private void copyFromCFA(CFA pCFA, CFANode pCurrentNode, CFANode pExitNode, ArrayList<CFANode> pNodes, ArrayList<CFAEdge> pEdges, ArrayList<Integer> pVisitedNodes) {
    // TODO WIP!

    // for each leaving edge successor node call copyFromCFA
    FluentIterable<CFAEdge> leavingEdges = pCurrentNode.getLeavingEdges();
    if (leavingEdges.isEmpty()) {
      return;
    }
    for(CFAEdge edge : leavingEdges) {
      int nextNodeNumber = edge.getSuccessor().getNodeNumber();
      if (!pVisitedNodes.contains(nextNodeNumber)) {
        pNodes.add(CFANode.newDummyCFANode());
        pVisitedNodes.add(nextNodeNumber);
      }
      // TODO add new edge
      copyFromCFA(pCFA, edge.getSuccessor(), pExitNode, pNodes, pEdges, pVisitedNodes);
    }
  }
}
