/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodCallEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodEntryNode;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodSummaryEdge;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.JParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * This class is responsible for creating call edges.
 * Additionally to super class it creates summary call statement edges.
 */
@Options
public class CFASecondPassBuilderComplete extends CFASecondPassBuilder {

  @Option(name="analysis.summaryEdges",
      description="create summary call statement edges")
  private boolean summaryEdges = false;

  public CFASecondPassBuilderComplete(MutableCFA pCfa, Language pLanguage, Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    super(pCfa, pLanguage, pLogger);
    config.inject(this);
  }

  @Override
  protected void buildCallEdges(IAStatement expr, AStatementEdge statement) throws ParserException {
    if (!(expr instanceof AFunctionCall)) {
      //this is not a call edge
      return;
    }
    AFunctionCall functionCall = (AFunctionCall)expr;
    AFunctionCallExpression f = functionCall.getFunctionCallExpression();

    if (isRegularCall(f)) {
      if (isDefined(f)) {
        createCallAndReturnEdges(statement, functionCall);
      }
    }
  }

  private void createCallAndSummaryStatementEdge(FunctionSummaryEdge calltoReturnEdge,
      //AStatementEdge edge,
      int lineNumber,
      String pRawStatement,
      CFunctionCall functionCall, FunctionEntryNode fDefNode, boolean removeUnreachable) {

    CFANode predecessorNode = calltoReturnEdge.getPredecessor();
    CFANode successorNode = calltoReturnEdge.getSuccessor();
    String functionName = fDefNode.getFunctionName();
    FunctionExitNode fExitNode = fDefNode.getExitNode();

    FunctionCallEdge callEdge = null;

    CStatement statement = functionCall.asStatement();

    // create new edges
    if (language == Language.C) {

      if (summaryEdges) {
        CFunctionSummaryStatementEdge summaryStatementEdge =
            new CFunctionSummaryStatementEdge(pRawStatement,
                statement, lineNumber,
                predecessorNode, successorNode, functionCall, functionName);

        predecessorNode.addLeavingEdge(summaryStatementEdge);
        successorNode.addEnteringEdge(summaryStatementEdge);
      }

      callEdge = new CFunctionCallEdge(pRawStatement,
          lineNumber, predecessorNode,
          (CFunctionEntryNode) fDefNode, functionCall,  (CFunctionSummaryEdge) calltoReturnEdge);

    } else if (language == Language.JAVA) {

      callEdge = new JMethodCallEdge(pRawStatement,
          lineNumber, predecessorNode,
          (JMethodEntryNode)fDefNode, (JMethodOrConstructorInvocation) functionCall, (JMethodSummaryEdge) calltoReturnEdge);
    }

    predecessorNode.addLeavingEdge(callEdge);
    fDefNode.addEnteringEdge(callEdge);

    if (removeUnreachable && fExitNode.getNumEnteringEdges() == 0) {
      // exit node of called functions is not reachable, i.e. this function never returns
      // no need to add return edges, instead we can remove the part after this function call

      CFACreationUtils.removeChainOfNodesFromCFA(successorNode);

    } else {

      FunctionReturnEdge returnEdge = null;

      if (language == Language.C) {
        returnEdge = new CFunctionReturnEdge(lineNumber, fExitNode, successorNode, (CFunctionSummaryEdge) calltoReturnEdge);
      } else if (language == Language.JAVA) {
        returnEdge = new JMethodReturnEdge(lineNumber, fExitNode, successorNode, (JMethodSummaryEdge) calltoReturnEdge);
      }

      fExitNode.addLeavingEdge(returnEdge);
      successorNode.addEnteringEdge(returnEdge);
    }
  }

  private FunctionSummaryEdge createSpecialSummaryEdge(int lineNumber, String pRawStatement,
      CFANode predecessorNode, CFANode successorNode, AFunctionCall functionCall) {
    FunctionSummaryEdge calltoReturnEdge = null;
    // create new edges
    if (language == Language.C) {
      calltoReturnEdge = new CFunctionSummaryEdge(pRawStatement, lineNumber,
          predecessorNode, successorNode, (CFunctionCall) functionCall);
    } else if (language == Language.JAVA) {
      calltoReturnEdge = new JMethodSummaryEdge(pRawStatement,
          lineNumber, predecessorNode, successorNode, (JMethodOrConstructorInvocation) functionCall);
    }
    predecessorNode.addLeavingSummaryEdge(calltoReturnEdge);
    successorNode.addEnteringSummaryEdge(calltoReturnEdge);

    return calltoReturnEdge;
  }

  /**
   * inserts call, return and summary edges from a node to its successor node.
   * @param edge The function call edge.
   * @param functionCall If the call was an assignment from the function call
   * this keeps only the function call expression, e.g. if statement is a = call(b);
   * then functionCall is call(b).
   * @throws ParserException
   */
  private void createCallAndReturnEdges(AStatementEdge edge, AFunctionCall functionCall) throws ParserException {

    CFANode predecessorNode = edge.getPredecessor();
    assert predecessorNode.getLeavingSummaryEdge() == null;

    CFANode successorNode = edge.getSuccessor();

    if (successorNode.getEnteringSummaryEdge() != null) {
      // Control flow merging directly after two function calls.
      // Our CFA structure currently does not support this,
      // so insert a dummy node and a blank edge.
      CFANode tmp = new CFANode(successorNode.getLineNumber(), successorNode.getFunctionName());
      cfa.addNode(tmp);
      CFAEdge tmpEdge = new BlankEdge("", successorNode.getLineNumber(), tmp, successorNode, "");
      CFACreationUtils.addEdgeUnconditionallyToCFA(tmpEdge);
      successorNode = tmp;
    }

    AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
    String functionName = functionCallExpression.getDeclaration().getName();
    FunctionEntryNode fDefNode = cfa.getFunctionHead(functionName);

    if (!checkParamSizes(functionCallExpression, fDefNode.getFunctionDefinition().getType())) {
      int actualParameters = functionCallExpression.getParameterExpressions().size();
      int declaredParameters = fDefNode.getFunctionDefinition().getType().getParameters().size();
      switch (language) {
      case JAVA:
        throw new JParserException("Function " + functionName + " takes "
            + declaredParameters + " parameter(s) but is called with "
            + actualParameters + " parameter(s)", edge);

      case C:
        throw new CParserException("Method " + functionName + " takes "
            + declaredParameters + " parameter(s) but is called with "
            + actualParameters + " parameter(s)", edge);
      }
    }

    // delete old edge
    CFACreationUtils.removeEdgeFromNodes(edge);

    FunctionSummaryEdge calltoReturnEdge =
        createSpecialSummaryEdge(edge.getLineNumber(), edge.getRawStatement(),
        predecessorNode, successorNode, functionCall);
    createCallAndSummaryStatementEdge(calltoReturnEdge,
        edge.getLineNumber(), edge.getRawStatement(),
        (CFunctionCall)functionCall, fDefNode, true);
  }
}
