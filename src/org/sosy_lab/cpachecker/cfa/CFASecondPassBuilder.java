/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodCallEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodEntryNode;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.IAFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.JParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFATraversal;

/**
 * This class takes several CFAs (each for a single function) and combines them
 * into one CFA by inserting the necessary function call and return edges.
 */
@Options
public class CFASecondPassBuilder {

  @Option(
    secure = true,
    name = "analysis.summaryEdges",
    description = "create summary call statement edges"
  )
  private boolean summaryEdges = false;

  @Option(
    secure = true,
    name = "analysis.stubs",
    description = "create functin call edges for both the function and the stub"
  )
  private boolean stubEdges = false;

  @Option(
    secure = true,
    name = "analysis.stubPostfix",
    description = "specify postfix to find the stub for a function e.g. f --> f_stub"
  )
  private String stubPostfix = "___stub";

  @Option(
    secure = true,
    name = "cfa.assumeFunctions",
    description = "Which functions should be interpreted as encoding assumptions"
  )
  private Set<String> assumeFunctions = ImmutableSet.of("__VERIFIER_assume");

  protected final MutableCFA cfa;
  protected final Language language;
  protected final LogManager logger;

  public CFASecondPassBuilder(final MutableCFA pCfa, final Language pLanguage, final LogManager pLogger,
                              final Configuration config) throws InvalidConfigurationException {
    cfa = pCfa;
    language = pLanguage;
    logger = pLogger;
    config.inject(this);
  }

  /**
   * Inserts call edges and return edges (@see {@link #insertCallEdges(AStatementEdge)} in all functions.
   */
  public void insertCallEdgesRecursively() throws ParserException {

    // 1.Step: get all function calls
    final FunctionCallCollector visitor = new FunctionCallCollector();
    for (final FunctionEntryNode entryNode : cfa.getAllFunctionHeads()) {
      CFATraversal.dfs().traverseOnce(entryNode, visitor);
      // No need for Traversal.ignoreFunctionCalls(), because there are no functioncall-edges.
      // They are created in the next loop.
    }

    // 2.Step: replace functionCalls with functioncall- and return-edges
    for (final AStatementEdge functionCall: visitor.getFunctionCalls()) {
      // it could be that the current function call was already removed
      // due to being unreachable (endless loop in front)
      // therefore we have to check that a predecessor exists before
      // inserting the new call edges
      if (functionCall.getPredecessor().getNumEnteringEdges() != 0) {
        insertCallEdges(functionCall);
      }
    }
  }

  /**
   * Inserts call edges and return edges from the call site and to the return site of the function
   * call.
   */
  private void insertCallEdges(final AStatementEdge statementEdge) throws ParserException {
    final AFunctionCall call = (AFunctionCall) statementEdge.getStatement();
    if (shouldCreateCallEdges(call) && language.equals(Language.C)) {
      final CFunctionCall cCall = (CFunctionCall) call;
      final CFunctionCallExpression callExpression = cCall.getFunctionCallExpression();
      final String funcName = callExpression.getDeclaration().getName();
      final FunctionExitNode funcExit = cfa.getFunctionHead(funcName).getExitNode();
      // don't approximate knowingly non-terminating functions
      // this should not be unsound, but may result in significantly different CFA
      if (stubEdges && stubPostfix != null && funcExit.getNumEnteringEdges() != 0) {
        final String stubName = callExpression.getDeclaration().getName() + stubPostfix;
        final CFunctionEntryNode stubEntry = (CFunctionEntryNode) cfa.getFunctionHead(stubName);
        if (stubEntry != null
            && checkParamSizes(callExpression, stubEntry.getFunctionDefinition().getType())) {
          createCallAndReturnEdgesWithStub((CStatementEdge) statementEdge, cCall, stubEntry);
        } else {
          createCallAndReturnEdges(statementEdge, call);
        }
      } else {
        createCallAndReturnEdges(statementEdge, call);
      }
    } else {
      replaceBuiltinFunction(statementEdge, call);
    }
  }

  /** returns True, iff the called function has a body (and a CFA). */
  private boolean shouldCreateCallEdges(final AFunctionCall call) {
    final ADeclaration functionDecl = call.getFunctionCallExpression().getDeclaration();

    // If we have a function declaration, it is a normal call to this function,
    // and neither a call to an undefined function nor a function pointer call.
    return (functionDecl != null) && cfa.getAllFunctionNames().contains(functionDecl.getName());
  }

  private void createCallAndReturnEdgesWithStub(
      final CStatementEdge funcEdge,
      final CFunctionCall funcCall,
      final CFunctionEntryNode stubEntry)
      throws ParserException {

    final CFANode predecessorNode = funcEdge.getPredecessor();
    assert predecessorNode.getLeavingSummaryEdge() == null;

    final CFANode directSuccessorNode = funcEdge.getSuccessor();
    final CFANode successorNode;

    if (directSuccessorNode.getEnteringSummaryEdge() == null) {
      successorNode = directSuccessorNode;
    } else {
      // Control flow merging directly after two function calls.
      // Our CFA structure currently does not support this,
      // so insert a dummy node and a blank edge.
      CFANode tmp = new CFANode(directSuccessorNode.getFunctionName());
      cfa.addNode(tmp);
      CFAEdge tmpEdge = new BlankEdge("", FileLocation.DUMMY, tmp, directSuccessorNode, "");
      CFACreationUtils.addEdgeUnconditionallyToCFA(tmpEdge);
      successorNode = tmp;
    }

    final CFunctionCallExpression funcCallExpression = funcCall.getFunctionCallExpression();
    final String functionName = funcCallExpression.getDeclaration().getName();
    final FileLocation fileLocation = funcEdge.getFileLocation();
    final CFunctionEntryNode funcEntry = (CFunctionEntryNode) cfa.getFunctionHead(functionName);
    final FunctionExitNode funcExit = funcEntry.getExitNode();

    final CFunctionDeclaration stubDeclaration = stubEntry.getFunctionDefinition();
    final CExpression stubFunctionNameExpression = new CIdExpression(fileLocation, stubDeclaration);
    final CFunctionCallExpression stubCallExpression =
        new CFunctionCallExpression(
            fileLocation,
            funcCallExpression.getExpressionType(),
            stubFunctionNameExpression,
            funcCallExpression.getParameterExpressions(),
            stubDeclaration);
    final CFunctionCall stubCall =
        funcCall instanceof CFunctionCallAssignmentStatement
            ? new CFunctionCallAssignmentStatement(
                fileLocation,
                ((CFunctionCallAssignmentStatement) funcCall).getLeftHandSide(),
                stubCallExpression)
            : new CFunctionCallStatement(fileLocation, stubCallExpression);
    // the following variable is just for symmetry, could as well be removed
    final CStatementEdge stubEdge =
        new CStatementEdge(
            funcEdge.getRawStatement(),
            stubCall,
            fileLocation,
            predecessorNode,
            directSuccessorNode);
    final FunctionExitNode stubExit = stubEntry.getExitNode();

    // get the parameter expression
    // check if the number of function parameters are right
    if (!checkParamSizes(funcCallExpression, funcEntry.getFunctionDefinition().getType())) {
      int declaredParameters = funcEntry.getFunctionDefinition().getType().getParameters().size();
      int actualParameters = funcCallExpression.getParameterExpressions().size();
      throw new CParserException(
          "Function "
              + functionName
              + " takes "
              + declaredParameters
              + " parameter(s) but is called with "
              + actualParameters
              + " parameter(s)",
          funcEdge);
    }

    // delete old edge
    CFACreationUtils.removeEdgeFromNodes(funcEdge);

    // create two intermediate nodes and assumption edges for calls to func & stub
    final CFANode beforeFuncCall = new CFANode(directSuccessorNode.getFunctionName());
    final CFANode beforeStubCall = new CFANode(directSuccessorNode.getFunctionName());
    cfa.addNode(beforeFuncCall);
    cfa.addNode(beforeStubCall);
    final CIntegerLiteralExpression constOne =
        new CIntegerLiteralExpression(fileLocation, CNumericTypes.INT, BigInteger.ONE);
    final CIntegerLiteralExpression constZero =
        new CIntegerLiteralExpression(fileLocation, CNumericTypes.INT, BigInteger.ZERO);
    final CBinaryExpression trueExpr =
        new CBinaryExpression(
            fileLocation,
            CNumericTypes.BOOL,
            CNumericTypes.INT,
            constZero,
            constZero,
            BinaryOperator.EQUALS);
    final CBinaryExpression falseExpr =
        new CBinaryExpression(
            fileLocation,
            CNumericTypes.BOOL,
            CNumericTypes.INT,
            constZero,
            constOne,
            BinaryOperator.EQUALS);
    final CFAEdge assumeFuncEdge =
        new CAssumeEdge("*unroll*", fileLocation, predecessorNode, beforeFuncCall, trueExpr, true);
    final CFAEdge assumeStubEdge =
        new CAssumeEdge("*stub*", fileLocation, predecessorNode, beforeStubCall, falseExpr, false);
    CFACreationUtils.addEdgeUnconditionallyToCFA(assumeFuncEdge);
    CFACreationUtils.addEdgeUnconditionallyToCFA(assumeStubEdge);

    final CFunctionSummaryEdge funcSummaryEdge =
        new CFunctionSummaryEdge(
            funcEdge.getRawStatement(),
            fileLocation,
            beforeFuncCall,
            successorNode,
            funcCall,
            funcEntry);
    beforeFuncCall.addLeavingSummaryEdge(funcSummaryEdge);
    successorNode.addEnteringSummaryEdge(funcSummaryEdge);

    if (summaryEdges) {
      final CFunctionSummaryStatementEdge funcSummaryStatementEdge =
          new CFunctionSummaryStatementEdge(
              funcEdge.getRawStatement(),
              funcCall,
              fileLocation,
              beforeFuncCall,
              successorNode,
              funcCall,
              funcEntry.getFunctionName());
      beforeFuncCall.addLeavingEdge(funcSummaryStatementEdge);
      successorNode.addEnteringEdge(funcSummaryStatementEdge);
    }

    final CFANode stubNode = new CFANode(directSuccessorNode.getFunctionName());
    cfa.addNode(stubNode);
    final CFunctionSummaryEdge stubSummaryEdge =
        new CFunctionSummaryEdge(
            stubEdge.getRawStatement(),
            fileLocation,
            beforeStubCall,
            stubNode,
            stubCall,
            stubEntry);
    beforeStubCall.addLeavingSummaryEdge(stubSummaryEdge);
    CFAEdge stubDummyEdge = new BlankEdge("*dummy*", fileLocation, stubNode, successorNode, "");
    CFACreationUtils.addEdgeUnconditionallyToCFA(stubDummyEdge);
    stubNode.addEnteringSummaryEdge(stubSummaryEdge);

    final CFunctionCallEdge funcCallEdge =
        new CFunctionCallEdge(
            funcEdge.getRawStatement(),
            fileLocation,
            beforeFuncCall,
            funcEntry,
            funcCall,
            funcSummaryEdge);
    beforeFuncCall.addLeavingEdge(funcCallEdge);
    funcEntry.addEnteringEdge(funcCallEdge);

    final CFunctionCallEdge stubCallEdge =
        new CFunctionCallEdge(
            stubEdge.getRawStatement(),
            fileLocation,
            beforeStubCall,
            stubEntry,
            stubCall,
            stubSummaryEdge);
    beforeStubCall.addLeavingEdge(stubCallEdge);
    stubEntry.addEnteringEdge(stubCallEdge);

    final CFunctionReturnEdge funcReturnEdge =
        new CFunctionReturnEdge(fileLocation, funcExit, successorNode, funcSummaryEdge);
    funcExit.addLeavingEdge(funcReturnEdge);
    successorNode.addEnteringEdge(funcReturnEdge);

    final CFunctionReturnEdge stubReturnEdge =
        new CFunctionReturnEdge(fileLocation, stubExit, stubNode, stubSummaryEdge);
    stubExit.addLeavingEdge(stubReturnEdge);
    stubNode.addEnteringEdge(stubReturnEdge);
  }

  private void createCallAndReturnEdges(AStatementEdge edge, AFunctionCall functionCall)
      throws ParserException {

    CFANode predecessorNode = edge.getPredecessor();
    assert predecessorNode.getLeavingSummaryEdge() == null;

    CFANode successorNode = edge.getSuccessor();

    if (successorNode.getEnteringSummaryEdge() != null) {
      // Control flow merging directly after two function calls.
      // Our CFA structure currently does not support this,
      // so insert a dummy node and a blank edge.
      CFANode tmp = new CFANode(successorNode.getFunctionName());
      cfa.addNode(tmp);
      CFAEdge tmpEdge = new BlankEdge("", FileLocation.DUMMY, tmp, successorNode, "");
      CFACreationUtils.addEdgeUnconditionallyToCFA(tmpEdge);
      successorNode = tmp;
    }

    AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
    String functionName = functionCallExpression.getDeclaration().getName();
    FileLocation fileLocation = edge.getFileLocation();
    FunctionEntryNode fDefNode = cfa.getFunctionHead(functionName);
    FunctionExitNode fExitNode = fDefNode.getExitNode();

    //get the parameter expression
    // check if the number of function parameters are right
    if (!checkParamSizes(functionCallExpression, fDefNode.getFunctionDefinition().getType())) {
      int declaredParameters = fDefNode.getFunctionDefinition().getType().getParameters().size();
      int actualParameters = functionCallExpression.getParameterExpressions().size();

      switch (language) {
      case JAVA:
        throw new JParserException("Function " + functionName + " takes "
            + declaredParameters + " parameter(s) but is called with "
            + actualParameters + " parameter(s)", edge);

      case C:
        throw new CParserException("Method " + functionName + " takes "
            + declaredParameters + " parameter(s) but is called with "
            + actualParameters + " parameter(s)", edge);

      default:
        throw new AssertionError("Unhandled language " + language);
      }
    }

    // delete old edge
    CFACreationUtils.removeEdgeFromNodes(edge);


    FunctionSummaryEdge calltoReturnEdge = null;
    FunctionCallEdge callEdge = null;

    // create new edges

    switch (language) {
    case C:
      if (summaryEdges) {
        CFunctionSummaryStatementEdge summaryStatementEdge =
            new CFunctionSummaryStatementEdge(edge.getRawStatement(),
                ((CFunctionCall)functionCall), fileLocation,
                predecessorNode, successorNode, (CFunctionCall)functionCall, fDefNode.getFunctionName());

        predecessorNode.addLeavingEdge(summaryStatementEdge);
        successorNode.addEnteringEdge(summaryStatementEdge);
      }

      calltoReturnEdge = new CFunctionSummaryEdge(edge.getRawStatement(),
          fileLocation, predecessorNode, successorNode,
          (CFunctionCall)functionCall, (CFunctionEntryNode)fDefNode);

      callEdge = new CFunctionCallEdge(edge.getRawStatement(),
          fileLocation, predecessorNode,
          (CFunctionEntryNode) fDefNode, (CFunctionCall) functionCall,  (CFunctionSummaryEdge) calltoReturnEdge);
      break;

    case JAVA:
      calltoReturnEdge = new JMethodSummaryEdge(edge.getRawStatement(),
          fileLocation, predecessorNode, successorNode,
          (JMethodOrConstructorInvocation)functionCall, (JMethodEntryNode)fDefNode);

      callEdge = new JMethodCallEdge(edge.getRawStatement(),
          fileLocation, predecessorNode,
          (JMethodEntryNode)fDefNode, (JMethodOrConstructorInvocation) functionCall, (JMethodSummaryEdge) calltoReturnEdge);
      break;

    default:
      throw new AssertionError();
    }

    predecessorNode.addLeavingSummaryEdge(calltoReturnEdge);
    successorNode.addEnteringSummaryEdge(calltoReturnEdge);

    predecessorNode.addLeavingEdge(callEdge);
    fDefNode.addEnteringEdge(callEdge);


    if (fExitNode.getNumEnteringEdges() == 0) {
      // exit node of called functions is not reachable, i.e. this function never returns
      // no need to add return edges, instead we can remove the part after this function call

      CFACreationUtils.removeChainOfNodesFromCFA(successorNode);

    } else {

      FunctionReturnEdge returnEdge;

      switch (language) {
      case C:
        returnEdge = new CFunctionReturnEdge(fileLocation, fExitNode, successorNode, (CFunctionSummaryEdge) calltoReturnEdge);
        break;
      case JAVA:
        returnEdge = new JMethodReturnEdge(fileLocation, fExitNode, successorNode, (JMethodSummaryEdge) calltoReturnEdge);
        break;
      default:
        throw new AssertionError();
      }

      fExitNode.addLeavingEdge(returnEdge);
      successorNode.addEnteringEdge(returnEdge);
    }
  }

  private boolean checkParamSizes(AFunctionCallExpression functionCallExpression,
      IAFunctionType functionType) {
    //get the parameter expression
    List<? extends AExpression> parameters = functionCallExpression.getParameterExpressions();

    // check if the number of function parameters are right
    int declaredParameters = functionType.getParameters().size();
    int actualParameters = parameters.size();

    return (functionType.takesVarArgs() && declaredParameters <= actualParameters) || (declaredParameters == actualParameters);
  }

  private void replaceBuiltinFunction(AStatementEdge edge, AFunctionCall call) {
    if (!(edge instanceof CStatementEdge)) {
      return;
    }

    AFunctionCallExpression f = call.getFunctionCallExpression();
    if (f.getDeclaration() == null) {
      return;
    }
    String name = f.getDeclaration().getName();

    if (!assumeFunctions.contains(name)) {
      return;
    }

    if (f.getParameterExpressions().size() != 1) {
      logger.logf(Level.WARNING, "Ignoring call to %s with illegal number of parameters (%s).",
          name, f.getParameterExpressions().size());
      return;
    }

    if (call instanceof AFunctionCallAssignmentStatement) {
      logger.logf(Level.WARNING, "Ignoring non-void call to %s.", name);
      return;
    }

    CExpression assumeExp = (CExpression)f.getParameterExpressions().get(0);

    if (!(assumeExp instanceof CBinaryExpression && ((CBinaryExpression)assumeExp).getOperator().isLogicalOperator())) {
      assumeExp = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger)
                      .buildBinaryExpressionUnchecked(assumeExp,
                                                      CIntegerLiteralExpression.ZERO,
                                                      BinaryOperator.NOT_EQUALS);
    }

    AssumeEdge trueEdge = new CAssumeEdge(edge.getRawStatement(), edge.getFileLocation(),
        edge.getPredecessor(), edge.getSuccessor(), assumeExp, true);

    CFANode elseNode = new CFATerminationNode(edge.getPredecessor().getFunctionName());
    AssumeEdge falseEdge = new CAssumeEdge(edge.getRawStatement(), edge.getFileLocation(),
        edge.getPredecessor(), elseNode, assumeExp, false);

    CFACreationUtils.removeEdgeFromNodes(edge);
    cfa.addNode(elseNode);
    CFACreationUtils.addEdgeUnconditionallyToCFA(trueEdge);
    CFACreationUtils.addEdgeUnconditionallyToCFA(falseEdge);
  }
}
