// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.underapproximating;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
import org.sosy_lab.cpachecker.cfa.ast.utils.Utils;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.AbstractStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class NondetVariableAssignmentStrategy extends AbstractStrategy {

  public NondetVariableAssignmentStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      CFA pCfa) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, pCfa);
  }

  @Override
  public Optional<GhostCFA> summarize(CFANode nondetValueStartNode) {

    // Check that the current element to be underapproximated is a nondet function call
    if (nondetValueStartNode.getNumLeavingEdges() != 1) {
      return Optional.empty();
    }

    CFAEdge leavingEdge = nondetValueStartNode.getLeavingEdge(0);

    if (!(leavingEdge instanceof CStatementEdge)) {
      return Optional.empty();
    }

    if (!(((CStatementEdge) leavingEdge).getStatement()
        instanceof CFunctionCallAssignmentStatement)) {
      return Optional.empty();
    }

    CFunctionCallAssignmentStatement statement = (CFunctionCallAssignmentStatement) ((CStatementEdge) leavingEdge).getStatement();

    CLeftHandSide variable = statement.getLeftHandSide();
    CFunctionCallExpression functionCall = statement.getRightHandSide();

    if (!Utils.isNondetCall(functionCall)) {
      return Optional.empty();
    }

    // Start generating the parameterized GhostCFA
    CFANode startNodeGhostCFA = CFANode.newDummyCFANode(nondetValueStartNode.getFunctionName());
    CFANode stopNodeGhostCFA = CFANode.newDummyCFANode(nondetValueStartNode.getFunctionName());

    CType variableType;
    if (variable instanceof CIdExpression) {
      variableType = ((CIdExpression) variable).getExpressionType();
    } else if (variable instanceof CArraySubscriptExpression) {
      variableType = ((CArraySubscriptExpression) variable).getExpressionType();
    } else {
      return Optional.empty();
    }

    // Assign variable to some value
    AExpressionFactory expressionFactory = new AExpressionFactory();
    CExpressionAssignmentStatement randomValueAssignmentToVariable =
        (CExpressionAssignmentStatement) expressionFactory.from(0, variableType).assignTo(variable);

    List<AExpression> parametersGhostCFA = new ArrayList<>();
    parametersGhostCFA.add(expressionFactory.from(0, variableType).build());

    CFAEdge dummyEdge =
        new CStatementEdge(
            randomValueAssignmentToVariable.toString(),
            randomValueAssignmentToVariable,
            FileLocation.DUMMY,
            startNodeGhostCFA,
            stopNodeGhostCFA);
    CFACreationUtils.addEdgeUnconditionallyToCFA(dummyEdge);

    return Optional.of(
        new GhostCFA(
            startNodeGhostCFA,
            stopNodeGhostCFA,
            nondetValueStartNode,
            nondetValueStartNode.getLeavingEdge(0).getSuccessor(),
            StrategiesEnum.NONDETVARIABLEASSIGNMENTSTRATEGY,
            parametersGhostCFA,
            StrategyQualifier.Underapproximating));
  }
}
