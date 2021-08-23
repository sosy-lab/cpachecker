// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;

public class NaiveLoopAccelerationStrategy extends AbstractLoopStrategy {

  public NaiveLoopAccelerationStrategy(
      final LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependencyInterface pStrategyDependencies,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, pCFA);
  }

  private Optional<GhostCFA> buildGhostCFA(
      Set<String> pModifiedVariables, CFANode loopStartNode, Integer loopBranchIndex) {
    CFANode startNodeGhostCFA = CFANode.newDummyCFANode("STARTNODEGHOST");
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode("ENDNODEGHOST");
    CFANode currentNode = startNodeGhostCFA;
    CFANode newNode = CFANode.newDummyCFANode("LSNA");
    if (!(loopStartNode.getLeavingEdge(loopBranchIndex) instanceof CAssumeEdge)) {
      return Optional.empty();
    }
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
    for (String variableName : pModifiedVariables) {
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

    Optional<CFANode> loopUnrollingSuccess =
        unrollLoopOnce(loopStartNode, loopBranchIndex, currentNode, endNodeGhostCFA);
    if (loopUnrollingSuccess.isEmpty()) {
      return Optional.empty();
    } else {
      currentNode = loopUnrollingSuccess.orElseThrow();
    }


    CFAEdge startConditionLoopCFAEdgeFalse =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopStartNode.getLeavingEdge(loopBranchIndex),
            false,
            currentNode,
            endNodeGhostCFA);
    currentNode.addLeavingEdge(startConditionLoopCFAEdgeFalse);
    endNodeGhostCFA.addEnteringEdge(startConditionLoopCFAEdgeFalse);

    CFANode dummyNode = CFANode.newDummyCFANode("LSNA");
    startConditionLoopCFAEdgeTrue =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopStartNode.getLeavingEdge(loopBranchIndex),
            true,
            currentNode,
            dummyNode);
    currentNode.addLeavingEdge(startConditionLoopCFAEdgeTrue);
    dummyNode.addEnteringEdge(startConditionLoopCFAEdgeTrue);

    startConditionLoopCFAEdgeFalse =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopStartNode.getLeavingEdge(loopBranchIndex),
            false,
            startNodeGhostCFA,
            endNodeGhostCFA);
    startNodeGhostCFA.addLeavingEdge(startConditionLoopCFAEdgeFalse);
    endNodeGhostCFA.addEnteringEdge(startConditionLoopCFAEdgeFalse);

    CFANode afterLoopNode = loopStartNode.getLeavingEdge(1 - loopBranchIndex).getSuccessor();

    return Optional.of(
        new GhostCFA(
            startNodeGhostCFA,
            endNodeGhostCFA,
            loopStartNode,
            afterLoopNode,
            StrategiesEnum.NaiveLoopAcceleration));
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode loopStartNode) {

    if (loopStartNode.getNumLeavingEdges() != 1) {
      return Optional.empty();
    }

    if (!loopStartNode.getLeavingEdge(0).getDescription().equals("while")) {
      return Optional.empty();
    }

    CFANode loopStartNodeLocal = loopStartNode.getLeavingEdge(0).getSuccessor();

    Integer loopBranchIndex;
    Optional<Integer> loopBranchIndexOptional = getLoopBranchIndex(loopStartNodeLocal);
    if (loopBranchIndexOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBranchIndex = loopBranchIndexOptional.orElseThrow();
    }

    Set<String> modifiedVariables;
    Optional<Set<String>> modifiedVariablesSuccess =
        getModifiedVariables(loopStartNodeLocal, loopBranchIndex);
    if (modifiedVariablesSuccess.isEmpty()) {
      return Optional.empty();
    } else {
      modifiedVariables = modifiedVariablesSuccess.orElseThrow();
    }

    GhostCFA ghostCFA;
    Optional<GhostCFA> ghostCFASuccess =
        buildGhostCFA(modifiedVariables, loopStartNodeLocal, loopBranchIndex);
    if (ghostCFASuccess.isEmpty()) {
      return Optional.empty();
    } else {
      ghostCFA = ghostCFASuccess.orElseThrow();
    }

    return Optional.of(ghostCFA);
  }
}
