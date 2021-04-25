// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary.strategies.extrapolation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.GhostCFA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class PolynomialExtrapolationStrategy extends AbstractExtrapolationStrategy {
  // See:
  // http://evoq-eval.siam.org/Portals/0/Publications/SIURO/Vol1_Issue1/A_Simple_Expression_for_Multivariate.pdf?ver=2018-03-30-130233-050

  final int multinomialDegree;

  public PolynomialExtrapolationStrategy(
      final LogManager pLogger, ShutdownNotifier pShutdownNotifier, int strategyIndex) {
    super(pLogger, pShutdownNotifier, strategyIndex);
    this.multinomialDegree = 4;
  }

  @SuppressWarnings("unused")
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
      loopBranchIndex = loopBranchIndexOptional.orElseThrow();
    }

    /*Set<String> modifiedVariableNames;
    Optional<Set<String>> modifiedVariablesSuccess =
        getModifiedVariables(loopStartNode, loopBranchIndex);
    if (modifiedVariablesSuccess.isEmpty()) {
      return Optional.empty();
    } else {
      modifiedVariableNames = modifiedVariablesSuccess.get();
    }
    */

    Set<String> allVariables;
    Optional<Set<String>> allVariablesSuccess = getAllVariables(loopStartNode, loopBranchIndex);
    if (allVariablesSuccess.isEmpty()) {
      return Optional.empty();
    } else {
      allVariables = allVariablesSuccess.orElseThrow();
    }

    if (allVariables.size() > 10 || this.multinomialDegree > 10) {
      // In this case the n + m choose m is to big
      return Optional.empty();
    }

    int amntDataPoints =
        binomialCoefficient(allVariables.size() + this.multinomialDegree, this.multinomialDegree);

    Map<String, Integer> intialValuesVariables = getInitialValuesVariables(pState);

    List<Map<String, Integer>> dataPoints;
    Optional<List<Map<String, Integer>>> dataPointsOptional =
        getDataPointsForVariables(
            intialValuesVariables, amntDataPoints, loopStartNode, loopBranchIndex);
    if (dataPointsOptional.isEmpty()) {
      return Optional.empty();
    } else {
      dataPoints = dataPointsOptional.orElseThrow();
    }

    GhostCFA ghostCFA;
    Optional<GhostCFA> ghostCFASuccess = summaryCFA(dataPoints, pState, loopBranchIndex);
    if (ghostCFASuccess.isEmpty()) {
      return Optional.empty();
    } else {
      ghostCFA = ghostCFASuccess.orElseThrow();
    }

    Collection<AbstractState> realStatesEndCollection =
        transverseGhostCFA(ghostCFA, pState, pPrecision, pTransferRelation, loopBranchIndex);

    return Optional.of(realStatesEndCollection);
  }

  @SuppressWarnings("unused")
  private Optional<GhostCFA> summaryCFA(
      List<Map<String, Integer>> pDataPoints, AbstractState pState, Integer pLoopBranchIndex) {
    // TODO Auto-generated method stub
    return Optional.empty();
  }

  private Map<String, Integer> getInitialValuesVariables(
      @SuppressWarnings("unused") AbstractState pState) {
    // TODO transverse the CFA in inverse order to get the intial values of the node if they exist,
    // only consider the first value found as correct and only consider it correct if before it
    // there were no branchings. Alternatively expect a value CPA and extract the value of the
    // Variables from there, for this the abstractState would be needed
    Map<String, Integer> intialValuesVariables = new HashMap<>();
    return intialValuesVariables;
  }

  private int binomialCoefficient(int n, int k) {
    int ret = 1;
    for (int i = 0; i < k; i++) {
      ret = ret * (n - i);
    }
    // Done like this to avoid rounding errors when dividing
    for (int i = 2; i <= k; i++) {
      ret = ret / i;
    }
    return ret;
  }

  @SuppressWarnings("unused")
  public Optional<Set<String>> getAllVariables(
      final CFANode pLoopStartNode, Integer pLoopBranchIndex) {
    // For now it is assumed that all variables which occur in a loop are unknown
    Set<String> allVariables = new HashSet<>();
    List<CFANode> reachedNodes = new ArrayList<>();
    reachedNodes.add(pLoopStartNode.getLeavingEdge(pLoopBranchIndex).getSuccessor());
    Collection<CFANode> seenNodes = new HashSet<>();
    while (!reachedNodes.isEmpty()) {
      List<CFANode> newReachableNodes = new ArrayList<>();
      for (CFANode s : reachedNodes) {
        seenNodes.add(s);
        if (s != pLoopStartNode) {
          for (int i = 0; i < s.getNumLeavingEdges(); i++) {
            if (s != pLoopStartNode) {
              CFAEdge edge = s.getLeavingEdge(i);
              if (edge instanceof CStatementEdge) {
                CStatement statement = ((CStatementEdge) edge).getStatement();
                if (statement instanceof CExpressionAssignmentStatement) {
                  allVariables.addAll(
                      getVariablesExpression(
                          ((CExpressionAssignmentStatement) statement).getLeftHandSide()));
                  allVariables.addAll(
                      getVariablesExpression(
                          ((CExpressionAssignmentStatement) statement).getRightHandSide()));
                }
              } else if (edge instanceof BlankEdge || edge instanceof CDeclarationEdge) {
                continue;
              } else {
                return Optional.empty();
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
    return Optional.of(allVariables);
  }

  private Set<String> getVariablesExpression(CExpression expression) {
    Set<String> allVariables = new HashSet<>();
    if (expression instanceof CIdExpression) {
      allVariables.add(((CIdExpression) expression).getName());
    } else if (expression instanceof CBinaryExpression) {
      allVariables.addAll(getVariablesExpression(((CBinaryExpression) expression).getOperand1()));
      allVariables.addAll(getVariablesExpression(((CBinaryExpression) expression).getOperand2()));
    }
    return allVariables;
  }

  @SuppressWarnings("unused")
  public Optional<List<Map<String, Integer>>> getDataPointsForVariables(
      Map<String, Integer> intialValuesVariables,
      final int amntDataPoints,
      final CFANode pLoopStartNode,
      final int pLoopBranchIndex) {
    List<Map<String, Integer>> dataPoints = new ArrayList<>();
    // TODO Calculate the Data Points for each Loop Iteration
    // If the amount of loop unrollings is less than amntDataPoints
    // the empty optional should be returned
    return Optional.of(dataPoints);
  }
}
