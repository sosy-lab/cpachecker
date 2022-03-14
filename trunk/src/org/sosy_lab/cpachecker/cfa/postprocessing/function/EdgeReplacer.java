// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import java.util.Collection;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

@Options
public abstract class EdgeReplacer {

  @Option(
      secure = true,
      name = "analysis.functionPointerEdgesForUnknownPointer",
      description = "Create edge for skipping a function pointer call if its value is unknown.")
  protected boolean createUndefinedFunctionCall = true;

  private final MutableCFA cfa;
  private final LogManager logger;
  private int instrumentedFunctions;

  protected EdgeReplacer(MutableCFA pCfa, Configuration config, LogManager pLogger)
      throws InvalidConfigurationException {
    cfa = pCfa;
    logger = pLogger;
    config.inject(this, EdgeReplacer.class);
    instrumentedFunctions = 0;
  }

  private CFANode newCFANode(final AFunctionDeclaration pFunction) {
    assert cfa != null;
    CFANode nextNode = new CFANode(pFunction);
    cfa.addNode(nextNode);
    return nextNode;
  }

  int getNumberOfInstrumenetedFunctions() {
    return instrumentedFunctions;
  }

  /**
   * This method adds 2 edges to the cfa: 1. trueEdge from rootNode to thenNode and 2. falseEdge
   * from rootNode to elseNode.
   */
  private void addConditionEdges(
      CExpression nameExp,
      CUnaryExpression amper,
      CFANode rootNode,
      CFANode thenNode,
      CFANode elseNode,
      FileLocation fileLocation) {

    final CBinaryExpressionBuilder binExprBuilder =
        new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
    CBinaryExpression condition =
        binExprBuilder.buildBinaryExpressionUnchecked(nameExp, amper, BinaryOperator.EQUALS);

    // edge connecting condition with thenNode
    final CAssumeEdge trueEdge =
        new CAssumeEdge(condition.toASTString(), fileLocation, rootNode, thenNode, condition, true);
    CFACreationUtils.addEdgeToCFA(trueEdge, logger);

    // edge connecting condition with elseNode
    final CAssumeEdge falseEdge =
        new CAssumeEdge(
            "!(" + condition.toASTString() + ")",
            fileLocation,
            rootNode,
            elseNode,
            condition,
            false);
    CFACreationUtils.addEdgeToCFA(falseEdge, logger);
  }

  private CFunctionCall createRegularCall(
      CFunctionCall functionCall, CFunctionCallExpression newCallExpr) {
    if (functionCall instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement asgn = (CFunctionCallAssignmentStatement) functionCall;
      return new CFunctionCallAssignmentStatement(
          functionCall.getFileLocation(), asgn.getLeftHandSide(), newCallExpr);
    } else if (functionCall instanceof CFunctionCallStatement) {
      return new CFunctionCallStatement(functionCall.getFileLocation(), newCallExpr);
    } else {
      throw new AssertionError("Unknown CFunctionCall subclass.");
    }
  }

  @SuppressWarnings("unused")
  protected boolean shouldBeInstrumented(CFunctionCall functionCall) {
    return true;
  }

  protected abstract CFunctionCallExpression createNewCallExpression(
      CFunctionCallExpression oldCallExpr,
      CExpression nameExp,
      FunctionEntryNode fNode,
      CIdExpression func);

  protected abstract AbstractCFAEdge createSummaryEdge(
      CStatementEdge statement, CFANode rootNode, CFANode end);

  public void instrument(
      CStatementEdge statement, Collection<CFunctionEntryNode> funcs, CExpression nameExp) {
    CFunctionCall functionCall = (CFunctionCall) statement.getStatement();

    if (funcs.isEmpty() || !shouldBeInstrumented(functionCall)) {
      return;
    }
    instrumentedFunctions++;

    CFunctionCallExpression oldCallExpr = functionCall.getFunctionCallExpression();
    FileLocation fileLocation = statement.getFileLocation();
    CFANode start = statement.getPredecessor();
    CFANode end = statement.getSuccessor();

    CFACreationUtils.removeEdgeFromNodes(statement);

    CFANode rootNode = start;
    for (FunctionEntryNode fNode : funcs) {
      CFANode thenNode = newCFANode(start.getFunction());
      CFANode elseNode = newCFANode(start.getFunction());

      CIdExpression func =
          new CIdExpression(
              nameExp.getFileLocation(),
              (CType) fNode.getFunctionDefinition().getType(),
              fNode.getFunctionName(),
              (CSimpleDeclaration) fNode.getFunctionDefinition());
      CUnaryExpression amper =
          new CUnaryExpression(
              nameExp.getFileLocation(),
              new CPointerType(false, false, func.getExpressionType()),
              func,
              CUnaryExpression.UnaryOperator.AMPER);
      CFANode retNode = newCFANode(start.getFunction());

      addConditionEdges(nameExp, amper, rootNode, thenNode, elseNode, fileLocation);

      String pRawStatement =
          "pointer call(" + fNode.getFunctionName() + ") " + statement.getRawStatement();
      CFunctionCallExpression newCallExpr =
          createNewCallExpression(oldCallExpr, nameExp, fNode, func);
      CFunctionCall regularCall = createRegularCall(functionCall, newCallExpr);

      CStatementEdge callEdge =
          new CStatementEdge(pRawStatement, regularCall, fileLocation, thenNode, retNode);
      CFACreationUtils.addEdgeUnconditionallyToCFA(callEdge);

      BlankEdge be = new BlankEdge("skip " + statement, fileLocation, retNode, end, "skip");
      CFACreationUtils.addEdgeUnconditionallyToCFA(be);

      rootNode = elseNode;
    }

    CFAEdge ae;
    if (createUndefinedFunctionCall) {
      ae = createSummaryEdge(statement, rootNode, end);
    } else {
      CFANode term = new CFATerminationNode(rootNode.getFunction());
      cfa.addNode(term);
      ae = new BlankEdge("blank pointer call", fileLocation, rootNode, term, "blank pointer call");
    }
    CFACreationUtils.addEdgeUnconditionallyToCFA(ae);
  }
}
