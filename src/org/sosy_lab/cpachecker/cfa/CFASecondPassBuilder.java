/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
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
import org.sosy_lab.cpachecker.cfa.model.java.JMethodCallEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodEntryNode;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.IAFunctionType;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.JParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * This class takes several CFAs (each for a single function) and combines them
 * into one CFA by inserting the necessary function call and return edges.
 */
public class CFASecondPassBuilder {

  protected final MutableCFA cfa;
  protected final Language language;
  protected final LogManager logger;
  /**
   * Class constructor.
   * @param map List of all CFA's in the program.
   */
  public CFASecondPassBuilder(MutableCFA pCfa,  Language pLanguage, LogManager pLogger) {
    cfa = pCfa;
    language = pLanguage;
    logger = pLogger;
  }

  /**
   * Inserts call edges and return edges (@see {@link #insertCallEdges(CFANode)}
   * in all functions.
   * @param functionName  The function where to start processing.
   * @throws ParserException
   */
  public void insertCallEdgesRecursively() throws ParserException {
    for (FunctionEntryNode functionStartNode : cfa.getAllFunctionHeads()) {
      insertCallEdges(functionStartNode);
    }
  }

  /**
   * Traverses a CFA with the specified function name and insert call edges
   * and return edges from the call site and to the return site of the function
   * call.
   * @param initialNode CFANode where to start processing
   * @throws ParserException
   */
  private void insertCallEdges(FunctionEntryNode initialNode) throws ParserException {
    // we use a worklist algorithm
    Deque<CFANode> workList = new ArrayDeque<>();
    Set<CFANode> processed = new HashSet<>();

    workList.addLast(initialNode);

    while (!workList.isEmpty()) {
      CFANode node = workList.pollFirst();
      if (!processed.add(node)) {
        // already handled
        continue;
      }

      for (CFAEdge edge : leavingEdges(node).toImmutableList()) {
        if (edge instanceof AStatementEdge) {
          AStatementEdge statement = (AStatementEdge)edge;
          IAStatement expr = statement.getStatement();
          buildCallEdges(expr, statement);
        }

        // if successor node is not on a different CFA, add it to the worklist
        CFANode successorNode = edge.getSuccessor();
        if (node.getFunctionName().equals(successorNode.getFunctionName())) {
          workList.add(successorNode);
        }
      }
    }
  }

  protected void buildCallEdges(IAStatement expr, AStatementEdge statement) throws ParserException {
    // if statement is of the form x = call(a,b); or call(a,b);
    if (shouldCreateCallEdges(expr)) {
      createCallAndReturnEdges(statement, (AFunctionCall)expr);
    }
  }

  private boolean shouldCreateCallEdges(IAStatement s) {
    if (!(s instanceof AFunctionCall)) {
      return false;
    }
    AFunctionCallExpression f = ((AFunctionCall)s).getFunctionCallExpression();
    if(!isRegularCall(f)) {
      return false;
    }
    return isDefined(f);
  }

  public static boolean isRegularCall(AFunctionCallExpression f) {
    if (f.getDeclaration() == null) {
      // There might be a function pointer shadowing a function,
      // so we need to check this explicitly here.
      String name = f.getFunctionNameExpression().toASTString();
      if(name.startsWith("__builtin_")) {
        //logger.log(Level.INFO, "Ignoring builtin function " + name);
        return true;
      }
      return false;
    }
    return true;
  }

  protected boolean isDefined(AFunctionCallExpression f) {
    String name = f.getFunctionNameExpression().toASTString();
    return cfa.getAllFunctionNames().contains(name);
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
    int lineNumber = edge.getLineNumber();
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
      }
    }

    // delete old edge
    CFACreationUtils.removeEdgeFromNodes(edge);


    FunctionSummaryEdge calltoReturnEdge = null;
    FunctionCallEdge callEdge = null;

    // create new edges
    if(language == Language.C) {

      calltoReturnEdge = new CFunctionSummaryEdge(edge.getRawStatement(),
          lineNumber, predecessorNode, successorNode, (CFunctionCall) functionCall);

      callEdge = new CFunctionCallEdge(edge.getRawStatement(),
          lineNumber, predecessorNode,
          (CFunctionEntryNode) fDefNode, (CFunctionCall) functionCall,  (CFunctionSummaryEdge) calltoReturnEdge);

    } else if(language == Language.JAVA){

      calltoReturnEdge = new JMethodSummaryEdge(edge.getRawStatement(),
          lineNumber, predecessorNode, successorNode, (JMethodOrConstructorInvocation) functionCall);

      callEdge = new JMethodCallEdge(edge.getRawStatement(),
          lineNumber, predecessorNode,
          (JMethodEntryNode)fDefNode, (JMethodOrConstructorInvocation) functionCall, (JMethodSummaryEdge) calltoReturnEdge);
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

      FunctionReturnEdge returnEdge = null;

      if(language == Language.C) {
        returnEdge = new CFunctionReturnEdge(lineNumber, fExitNode, successorNode, (CFunctionSummaryEdge) calltoReturnEdge);
      } else if (language == Language.JAVA) {
        returnEdge = new JMethodReturnEdge(lineNumber, fExitNode, successorNode, (JMethodSummaryEdge) calltoReturnEdge);
      }

      fExitNode.addLeavingEdge(returnEdge);
      successorNode.addEnteringEdge(returnEdge);
    }
  }

  public void collectDataRecursively() {}

  protected final boolean checkParamSizes(AFunctionCallExpression functionCallExpression,
      IAFunctionType functionType) {
    //get the parameter expression
    List<? extends IAExpression> parameters = functionCallExpression.getParameterExpressions();

    // check if the number of function parameters are right
    int declaredParameters = functionType.getParameters().size();
    int actualParameters = parameters.size();

    return (functionType.takesVarArgs() && declaredParameters <= actualParameters) || (declaredParameters == actualParameters);
  }
}