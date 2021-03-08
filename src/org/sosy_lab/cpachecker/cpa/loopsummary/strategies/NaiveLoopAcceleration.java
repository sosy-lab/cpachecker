// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary.strategies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class NaiveLoopAcceleration extends AbstractStrategy {

  public NaiveLoopAcceleration() {}

  private HashSet<String> getModifiedVariables(CFANode loopStartNode, Integer loopBranchIndex) {
    HashSet<String> modifiedVariables = new HashSet<>();
    ArrayList<CFANode> reachedNodes = new ArrayList<>();
    reachedNodes.add(loopStartNode.getLeavingEdge(loopBranchIndex).getSuccessor());
    while (!reachedNodes.isEmpty()) {
      ArrayList<CFANode> newReachableNodes = new ArrayList<>();
      for (CFANode s : reachedNodes) {
        if (s != loopStartNode) {
          for (int i = 0; i < s.getNumLeavingEdges(); i++) {
            if (s != loopStartNode) {
              CFAEdge edge = s.getLeavingEdge(i);
              if (edge instanceof CStatementEdge) {
                CStatement statement = ((CStatementEdge) edge).getStatement();
                CExpression leftSide = ((CExpressionAssignmentStatement) statement).getLeftHandSide();
                if (leftSide instanceof CIdExpression) { // TODO Generalize
                  modifiedVariables.add(((CIdExpression) leftSide).getName());
                }
              }
              newReachableNodes.add(edge.getSuccessor());
            }
          }
        }
      }
      reachedNodes = newReachableNodes;
    }
    return modifiedVariables;
  }

  private GhostCFA buildGhostCFA(
      HashSet<String> modifiedVariables, CFANode loopStartNode, Integer loopBranchIndex) {
    CFANode startNodeGhostCFA = CFANode.newDummyCFANode("STARTNODEGHOST");
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode("ENDNODEGHOST");
    CFANode currentNode = startNodeGhostCFA;
    CFANode newNode = CFANode.newDummyCFANode("LSNA");
    CFAEdge startConditionLoopCFAEdgeTrue =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopStartNode.getLeavingEdge(loopBranchIndex),
            true,
            currentNode,
            newNode);
    currentNode.addLeavingEdge(startConditionLoopCFAEdgeTrue);
    newNode.addEnteringEdge(startConditionLoopCFAEdgeTrue);
    currentNode = newNode;
    newNode = CFANode.newDummyCFANode("LSNA");
    for (String variableName : modifiedVariables) {
      CVariableDeclaration pc =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              true,
              CStorageClass.EXTERN,
              CNumericTypes.INT, // TODO Improve this
              variableName,
              variableName,
              variableName,
              null);
      CIdExpression leftHandSide = new CIdExpression(FileLocation.DUMMY, pc);
      CExpression rightHandSide =
          CIntegerLiteralExpression.createDummyLiteral(
              0, CNumericTypes.INT); // TODO Set Variables to nondet
      CExpressionAssignmentStatement cStatementEdge =
          new CExpressionAssignmentStatement(FileLocation.DUMMY, leftHandSide, rightHandSide);
      CFAEdge dummyEdge =
          new CStatementEdge(
              variableName + " = NONDET", cStatementEdge, FileLocation.DUMMY, currentNode, newNode);
      currentNode.addLeavingEdge(dummyEdge);
      newNode.addEnteringEdge(dummyEdge);
      currentNode = newNode;
      newNode = CFANode.newDummyCFANode("LSNA");
    }
    startConditionLoopCFAEdgeTrue =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopStartNode.getLeavingEdge(loopBranchIndex),
            true,
            currentNode,
            newNode);
    currentNode.addLeavingEdge(startConditionLoopCFAEdgeTrue);
    newNode.addEnteringEdge(startConditionLoopCFAEdgeTrue);
    currentNode =
        unrollLoopOnce(
            loopStartNode,
            loopBranchIndex,
            endNodeGhostCFA,
            newNode); // TODO Improve loop unrolling
    CFAEdge startConditionLoopCFAEdgeFalse =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopStartNode.getLeavingEdge(loopBranchIndex),
            true,
            currentNode,
            endNodeGhostCFA);
    currentNode.addLeavingEdge(startConditionLoopCFAEdgeFalse);
    endNodeGhostCFA.addEnteringEdge(startConditionLoopCFAEdgeFalse);
    return new GhostCFA(startNodeGhostCFA, endNodeGhostCFA);
  }

  @Override
  public Optional<Collection<? extends AbstractState>> summarizeLoopState(
      AbstractState pState, Precision pPrecision, TransferRelation pTransferRelation)
      throws CPATransferException, InterruptedException {
    CFANode loopStartNode = AbstractStates.extractLocation(pState);
    Integer loopBranchIndex;
    Optional<Integer> loopBranchIndexOptional = getLoopBranchIndex(loopStartNode);
    if (loopBranchIndexOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBranchIndex = loopBranchIndexOptional.get();
    }

    HashSet<String> modifiedVariables = getModifiedVariables(loopStartNode, loopBranchIndex);

    GhostCFA ghostCFA =
        buildGhostCFA(modifiedVariables, loopStartNode, loopBranchIndex);

    Collection<AbstractState> realStatesEndCollection =
        transverseGhostCFA(ghostCFA, pState, pPrecision, pTransferRelation);

    return Optional.of(realStatesEndCollection);
  }
}
