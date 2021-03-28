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
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class NaiveLoopAcceleration extends AbstractStrategy {

  public NaiveLoopAcceleration(final LogManager pLogger) {
    super(pLogger);
  }


  private Optional<HashSet<String>> getModifiedVariables(
      CFANode loopStartNode, Integer loopBranchIndex) {
    HashSet<String> modifiedVariables = new HashSet<>();
    ArrayList<CFANode> reachedNodes = new ArrayList<>();
    reachedNodes.add(loopStartNode.getLeavingEdge(loopBranchIndex).getSuccessor());
    Collection<CFANode> seenNodes = new HashSet<>();
    while (!reachedNodes.isEmpty()) {
      ArrayList<CFANode> newReachableNodes = new ArrayList<>();
      for (CFANode s : reachedNodes) {
        seenNodes.add(s);
        if (s != loopStartNode) {
          for (int i = 0; i < s.getNumLeavingEdges(); i++) {
            if (s != loopStartNode) {
              CFAEdge edge = s.getLeavingEdge(i);
              if (edge instanceof CStatementEdge) {
                CStatement statement = ((CStatementEdge) edge).getStatement();
                CExpression leftSide;
                if (statement instanceof CFunctionCallAssignmentStatement) {
                  leftSide = ((CFunctionCallAssignmentStatement) statement).getLeftHandSide();
                } else if (statement instanceof CExpressionAssignmentStatement) {
                  leftSide =
                      ((CExpressionAssignmentStatement) statement).getLeftHandSide();
                } else {
                  logger.log(
                      Level.INFO,
                      "Unknown Statement in Naive Loop Accel of type: "
                          + statement.getClass()
                          + " \n Statetement has form: "
                          + statement);
                  return Optional.empty();
                }
                if (leftSide instanceof CIdExpression) { // TODO Generalize
                  modifiedVariables.add(((CIdExpression) leftSide).getName());
                }
              }
              if (!seenNodes.contains(edge.getSuccessor())) {
                newReachableNodes.add(edge.getSuccessor());
              }
            }
          }
        }
      }
      reachedNodes = newReachableNodes;
    }
    return Optional.of(modifiedVariables);
  }

  private Optional<GhostCFA> buildGhostCFA(
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
      CFunctionCallExpression rightHandSide =
          new CFunctionCallExpression(
              FileLocation.DUMMY,
              CNumericTypes.INT,
              new CIdExpression(
                  FileLocation.DUMMY,
                  new CFunctionDeclaration(
                      FileLocation.DUMMY,
                      new CFunctionTypeWithNames(
                          CNumericTypes.INT, new ArrayList<CParameterDeclaration>(), false),
                      "__VERIFIER_nondet_int",
                      new ArrayList<CParameterDeclaration>())),
              new ArrayList<CExpression>(),
              new CFunctionDeclaration(
                  FileLocation.DUMMY,
                  new CFunctionTypeWithNames(
                      CNumericTypes.INT, new ArrayList<CParameterDeclaration>(), false),
                  "__VERIFIER_nondet_int",
                  "__VERIFIER_nondet_int",
                  new ArrayList<CParameterDeclaration>())); // TODO Improve this
      CFunctionCallAssignmentStatement cStatementEdge =
          new CFunctionCallAssignmentStatement(FileLocation.DUMMY, leftHandSide, rightHandSide);
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
    Optional<CFANode> loopUnrollingSuccess =
        unrollLoopOnce(loopStartNode, loopBranchIndex, endNodeGhostCFA, newNode);
    if (loopUnrollingSuccess.isEmpty()) {
      return Optional.empty();
    } else {
      currentNode = loopUnrollingSuccess.get();
    }
    CFAEdge startConditionLoopCFAEdgeFalse =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopStartNode.getLeavingEdge(loopBranchIndex),
            false,
            currentNode,
            endNodeGhostCFA);
    currentNode.addLeavingEdge(startConditionLoopCFAEdgeFalse);
    endNodeGhostCFA.addEnteringEdge(startConditionLoopCFAEdgeFalse);
    startConditionLoopCFAEdgeFalse =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopStartNode.getLeavingEdge(loopBranchIndex),
            false,
            startNodeGhostCFA,
            endNodeGhostCFA);
    startNodeGhostCFA.addLeavingEdge(startConditionLoopCFAEdgeFalse);
    endNodeGhostCFA.addEnteringEdge(startConditionLoopCFAEdgeFalse);
    return Optional.of(new GhostCFA(startNodeGhostCFA, endNodeGhostCFA));
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

    HashSet<String> modifiedVariables;
    Optional<HashSet<String>> modifiedVariablesSuccess =
        getModifiedVariables(loopStartNode, loopBranchIndex);
    if (modifiedVariablesSuccess.isEmpty()) {
      return Optional.empty();
    } else {
      modifiedVariables = modifiedVariablesSuccess.get();
    }

    GhostCFA ghostCFA;
    Optional<GhostCFA> ghostCFASuccess = buildGhostCFA(modifiedVariables, loopStartNode, loopBranchIndex);
    if (ghostCFASuccess.isEmpty()) {
      return Optional.empty();
    } else {
      ghostCFA = ghostCFASuccess.get();
    }

    Collection<AbstractState> realStatesEndCollection =
        transverseGhostCFA(ghostCFA, pState, pPrecision, pTransferRelation, loopBranchIndex);

    return Optional.of(realStatesEndCollection);
  }
}
