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
import com.google.common.graph.Traverser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;

public class TailRecursionEliminationProgramTransformation extends ProgramTransformation{
  public ProgramTransformationBehaviour behaviour = ProgramTransformationBehaviour.PRECISE;

  @Override
  public Optional<SubCFA> transform(CFA pCFA, CFANode pNode) {

    // TODO check transformation conditions
    if (!(pNode instanceof FunctionEntryNode)) {
      return Optional.empty();
    }
    if (((FunctionEntryNode) pNode).getExitNode().isEmpty()) {
      return Optional.empty();
    }
    CFANode exitNode = ((FunctionEntryNode) pNode).getExitNode().get();
    String functionName = pNode.getFunctionName();

    FluentIterable<CFAEdge> enteringEdges = exitNode.getEnteringEdges();
    for(CFAEdge edge : enteringEdges) {
      CReturnStatement returnStatement = ((CReturnStatementEdge) edge).getReturnStatement();
      // TODO return expressions checken
      if (returnStatement.getReturnValue().isEmpty()) {
        return  Optional.empty();
      }
      CExpression returnExpression = returnStatement.getReturnValue().get();
    }

    // TODO perform transformation
    ArrayList<CFANode> nodes = new ArrayList<>();
    ArrayList<CFAEdge> edges = new ArrayList<>();
    HashMap<Integer, Integer> nodeMap = new HashMap<>();
    Traverser<CFANode> cfaNetworkTraverser = Traverser.forGraph(pCFA.asGraph());
    Iterable<CFANode> cfaNodeIterable = cfaNetworkTraverser.breadthFirst(pNode);
    ImmutableList<? extends AParameterDeclaration> parameters = pNode.getFunction().getParameters();
    int parameterNum = parameters.size();

    for(CFANode currentNode : cfaNodeIterable) {
      if (currentNode.getFunctionName().equals(functionName)) {
        if (currentNode.getNodeNumber() == exitNode.getNodeNumber()) {
          nodes.add(CFANode.newDummyCFANode(functionName));
          nodeMap.put(currentNode.getNodeNumber(), nodes.getLast().getNodeNumber());
          break;
        } else if (currentNode.getNodeNumber() == pNode.getNodeNumber()) {
          nodes.add(CFANode.newDummyCFANode(functionName));
          nodes.getLast().setLoopStart();
          nodeMap.put(currentNode.getNodeNumber(), nodes.getLast().getNodeNumber());
        }
      }
    }

    return Optional.of(new SubCFA(
        pNode,
        exitNode,
        nodes.getFirst(),
        nodes.getLast(),
        ProgramTransformationEnum.TAIL_RECURSION_ELIMINATION,
        ProgramTransformationBehaviour.PRECISE,
        ImmutableSet.copyOf(nodes),
        ImmutableSet.copyOf(edges)
    ));
  }
}
