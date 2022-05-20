// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.ImmutableSet;
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

  @Option(secure=true, name="analysis.summaryEdges",
      description="create summary call statement edges")
  private boolean summaryEdges = false;

  @Option(secure=true, name="cfa.assumeFunctions",
      description="Which functions should be interpreted as encoding assumptions")
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
   * Inserts call edges and return edges from the call site and to the return site of the function call.
   */
  private void insertCallEdges(final AStatementEdge statementEdge) throws ParserException {
    final AFunctionCall call = (AFunctionCall)statementEdge.getStatement();
    if (shouldCreateCallEdges(call)) {
      createCallAndReturnEdges(statementEdge, call);
    } else {
      replaceBuiltinFunction(statementEdge, call);
    }
  }

  /** returns True, iff the called function has a body (and a CFA). */
  private boolean shouldCreateCallEdges(final AFunctionCall call) {
    final ADeclaration functionDecl = call.getFunctionCallExpression().getDeclaration();

    // If we have a function declaration, it is a normal call to this function,
    // and neither a call to an undefined function nor a function pointer call.
    return (functionDecl != null)
            && cfa.getAllFunctionNames().contains(functionDecl.getName());
  }

  /**
   * inserts call, return and summary edges from a node to its successor node.
   * @param edge The function call edge.
   * @param functionCall If the call was an assignment from the function call
   * this keeps only the function call expression, e.g. if statement is a = call(b);
   * then functionCall is call(b).
   */
  private void createCallAndReturnEdges(AStatementEdge edge, AFunctionCall functionCall) throws ParserException {

    CFANode predecessorNode = edge.getPredecessor();
    assert predecessorNode.getLeavingSummaryEdge() == null;

    CFANode successorNode = edge.getSuccessor();

    if (successorNode.getEnteringSummaryEdge() != null) {
      // Control flow merging directly after two function calls.
      // Our CFA structure currently does not support this,
      // so insert a dummy node and a blank edge.
      CFANode tmp = new CFANode(successorNode.getFunction());
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

    CFANode elseNode = new CFATerminationNode(edge.getPredecessor().getFunction());
    AssumeEdge falseEdge = new CAssumeEdge(edge.getRawStatement(), edge.getFileLocation(),
        edge.getPredecessor(), elseNode, assumeExp, false);

    CFACreationUtils.removeEdgeFromNodes(edge);
    cfa.addNode(elseNode);
    CFACreationUtils.addEdgeUnconditionallyToCFA(trueEdge);
    CFACreationUtils.addEdgeUnconditionallyToCFA(falseEdge);
  }
}
