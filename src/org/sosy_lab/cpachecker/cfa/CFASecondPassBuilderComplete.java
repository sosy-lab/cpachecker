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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
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
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodCallEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodEntryNode;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.IAFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.JParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * This class is responsible for creating call edges.
 * Additionally to super class it creates
 * 1. In case of function pointer call (option functionPointerCalls)
 *  it creates calls to each potential function matching some criteria (defined by functionPointerCalls)
 * 2. Summary call statement edges (option summaryEdges).
 *  If functionPointerCalls is on it creates summary edges for each potential regular call
 *
 */
@Options
public class CFASecondPassBuilderComplete extends CFASecondPassBuilder {

  @Option(name="analysis.functionPointerCalls",
      description="create all potential function pointer call edges")
  private boolean fptrCallEdges = true;

  @Option(name="analysis.summaryEdges",
      description="create summary call statement edges")
  private boolean summaryEdges = true;

  private enum FunctionSet {
    ALL, //all defined functions considered (Warning: some CPAs require at least EQ_PARAM_SIZES)
    EQ_PARAM_SIZES //all functions with matching number of parameters considered
  }

  private FunctionSet functionSet = FunctionSet.EQ_PARAM_SIZES;

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

    if(isRegularCall(f)) {
      createCallAndReturnEdges(statement, functionCall);
    } else {
      if(language == Language.C && fptrCallEdges) {
        CExpression nameExp = (CExpression)f.getFunctionNameExpression();
        Collection<FunctionEntryNode> funcs = getFunctionSet(f, functionCall);
        CFANode start = statement.getPredecessor();
        CFANode end = statement.getSuccessor();
        // delete old edge
        CFACreationUtils.removeEdgeFromNodes(statement);

        CFANode rootNode = start;
        CFANode elseNode = null;
        for(FunctionEntryNode fNode : funcs) {
          CFANode thenNode = newCFANode(start.getLineNumber(), start.getFunctionName());
          elseNode = newCFANode(start.getLineNumber(), start.getFunctionName());
          CIdExpression func = new CIdExpression(nameExp.getFileLocation(),
              nameExp.getExpressionType(),
              fNode.getFunctionName(),
              (CSimpleDeclaration)fNode.getFunctionDefinition());
          CUnaryExpression amper = new CUnaryExpression(nameExp.getFileLocation(),
              nameExp.getExpressionType(), func, CUnaryExpression.UnaryOperator.AMPER);
          CBinaryExpression condition = new CBinaryExpression(f.getFileLocation(),
              CNumericTypes.INT, nameExp, amper, BinaryOperator.EQUALS);

          addConditionEdges(condition, rootNode, thenNode, elseNode, start.getLineNumber());


          CFANode retNode = newCFANode(start.getLineNumber(), start.getFunctionName());
          //create special summary edge
          //thenNode-->retNode
          FunctionSummaryEdge calltoReturnEdge = createSpecialSummaryEdge(statement.getLineNumber(),
              "pointer call(" + fNode.getFunctionName() + ") " + statement.getRawStatement(),
              thenNode, retNode, functionCall);
          createCallAndSummaryStatementEdge(calltoReturnEdge, statement, functionCall, fNode, false);

          //retNode-->end
          BlankEdge be = new BlankEdge("skip", statement.getLineNumber(), retNode, end, "skip");
          CFACreationUtils.addEdgeUnconditionallyToCFA(be);

          rootNode = elseNode;
        }

        //rootNode --> end
        if(summaryEdges) {
          //create summary statement edge without function name
          createUndefinedSummaryStatementEdge(rootNode, end, statement, functionCall);
        } else {
          //no way to skip the function call
          //remove last edge to elseNode
          for (CFAEdge edge : CFAUtils.enteringEdges(elseNode)) {
            CFACreationUtils.removeEdgeFromNodes(edge);
          }
          cfa.removeNode(elseNode);
        }

      }
    }
  }

  private void createUndefinedSummaryStatementEdge(CFANode predecessorNode, CFANode successorNode, AStatementEdge statement,
      AFunctionCall functionCall) {
    CFunctionSummaryStatementEdge summaryStatementEdge =
        new CFunctionSummaryStatementEdge("undefined call " + statement.getRawStatement(),
        (CStatement)statement.getStatement(), statement.getLineNumber(), predecessorNode, successorNode,
        (CFunctionCall) functionCall, null);

    predecessorNode.addLeavingEdge(summaryStatementEdge);
    successorNode.addEnteringEdge(summaryStatementEdge);
  }

  //TODO: replace function call by pointer expression with regular call by name (in functionCall and edge.getStatement())
  private void createCallAndSummaryStatementEdge(FunctionSummaryEdge calltoReturnEdge,
      AStatementEdge edge,
      AFunctionCall functionCall, FunctionEntryNode fDefNode, boolean removeUnreachable) {

    CFANode predecessorNode = calltoReturnEdge.getPredecessor();
    CFANode successorNode = calltoReturnEdge.getSuccessor();
    String functionName = fDefNode.getFunctionName();
    int lineNumber = edge.getLineNumber();
    FunctionExitNode fExitNode = fDefNode.getExitNode();

    FunctionCallEdge callEdge = null;

    // create new edges
    if(language == Language.C) {

      if(summaryEdges) {
        CFunctionSummaryStatementEdge summaryStatementEdge =
            new CFunctionSummaryStatementEdge(edge.getRawStatement(),
                (CStatement)edge.getStatement(), edge.getLineNumber(),
                predecessorNode, successorNode, (CFunctionCall) functionCall, functionName);

        predecessorNode.addLeavingEdge(summaryStatementEdge);
        successorNode.addEnteringEdge(summaryStatementEdge);
      }

      callEdge = new CFunctionCallEdge(edge.getRawStatement(),
          lineNumber, predecessorNode,
          (CFunctionEntryNode) fDefNode, (CFunctionCall) functionCall,  (CFunctionSummaryEdge) calltoReturnEdge);

    } else if(language == Language.JAVA){

      callEdge = new JMethodCallEdge(edge.getRawStatement(),
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

      if(language == Language.C) {
        returnEdge = new CFunctionReturnEdge(lineNumber, fExitNode, successorNode, (CFunctionSummaryEdge) calltoReturnEdge);
      } else if (language == Language.JAVA) {
        returnEdge = new JMethodReturnEdge(lineNumber, fExitNode, successorNode, (JMethodSummaryEdge) calltoReturnEdge);
      }

      fExitNode.addLeavingEdge(returnEdge);
      successorNode.addEnteringEdge(returnEdge);
    }
  }

  /**
   * @category helper
   */
  private CFANode newCFANode(final int filelocStart, final String functionName) {
    assert cfa != null;
    CFANode nextNode = new CFANode(filelocStart, functionName);
    cfa.addNode(nextNode);
    return nextNode;
  }

  private boolean isRegularCall(AFunctionCallExpression f) {
    if (f.getDeclaration() == null) {
      // There might be a function pointer shadowing a function,
      // so we need to check this explicitly here.
      return false;
    }
    String name = f.getFunctionNameExpression().toASTString();
    return cfa.getAllFunctionNames().contains(name);
  }

  private FunctionSummaryEdge createSpecialSummaryEdge(int lineNumber, String pRawStatement,
      CFANode predecessorNode, CFANode successorNode, AFunctionCall functionCall) {
    FunctionSummaryEdge calltoReturnEdge = null;
    // create new edges
    if(language == Language.C) {
      calltoReturnEdge = new CFunctionSummaryEdge(pRawStatement, lineNumber,
          predecessorNode, successorNode, (CFunctionCall) functionCall);
    } else if(language == Language.JAVA) {
      calltoReturnEdge = new JMethodSummaryEdge(pRawStatement,
          lineNumber, predecessorNode, successorNode, (JMethodOrConstructorInvocation) functionCall);
    }
    predecessorNode.addLeavingSummaryEdge(calltoReturnEdge);
    successorNode.addEnteringSummaryEdge(calltoReturnEdge);

    return calltoReturnEdge;
  }

  /** This method adds 2 edges to the cfa:
   * 1. trueEdge from rootNode to thenNode and
   * 2. falseEdge from rootNode to elseNode.
   * @category conditions
   */
  private void addConditionEdges(CExpression condition, CFANode rootNode,
      CFANode thenNode, CFANode elseNode, int filelocStart) {
    // edge connecting condition with thenNode
    final CAssumeEdge trueEdge = new CAssumeEdge(condition.toASTString(),
        filelocStart, rootNode, thenNode, condition, true);
    CFACreationUtils.addEdgeToCFA(trueEdge, logger);

    // edge connecting condition with elseNode
    final CAssumeEdge falseEdge = new CAssumeEdge("!(" + condition.toASTString() + ")",
        filelocStart, rootNode, elseNode, condition, false);
    CFACreationUtils.addEdgeToCFA(falseEdge, logger);
  }

  /**
   * inserts call, return and summary edges from a node to its successor node.
   * @param edge The function call edge.
   * @param functionCall If the call was an assignment from the function call
   * this keeps only the function call expression, e.g. if statement is a = call(b);
   * then functionCall is call(b).
   * @throws ParserException
   */
  protected void createCallAndReturnEdges(AStatementEdge edge, AFunctionCall functionCall) throws ParserException {

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

    if(!checkParamSizes(functionCallExpression, fDefNode)) {
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

    FunctionSummaryEdge calltoReturnEdge = createSpecialSummaryEdge(edge.getLineNumber(), edge.getRawStatement(),
        predecessorNode, successorNode, functionCall);
    createCallAndSummaryStatementEdge(calltoReturnEdge, edge, functionCall, fDefNode, true);
  }

  private Collection<FunctionEntryNode> getFunctionSet(
      AFunctionCallExpression functionCallExpression, AFunctionCall expr) {
    if(functionSet == FunctionSet.ALL) {
      return cfa.getAllFunctionHeads();
    } else {
      //if(functionSet == FunctionSet.EQ_PARAM_SIZES)
      Collection<FunctionEntryNode> col = cfa.getAllFunctionHeads();
      Collection<FunctionEntryNode> res = new ArrayList<>();
      for(FunctionEntryNode f : col) {
        if(checkParamSizes(expr.getFunctionCallExpression(), f)) {
          res.add(f);
        }
      }
      return res;
    }
  }

  private boolean checkParamSizes(AFunctionCallExpression functionCallExpression,
      FunctionEntryNode fDefNode) {
    //get the parameter expression
    List<? extends IAExpression> parameters = functionCallExpression.getParameterExpressions();

    // check if the number of function parameters are right
    IAFunctionType functionType = fDefNode.getFunctionDefinition().getType();
    int declaredParameters = functionType.getParameters().size();
    int actualParameters = parameters.size();

    return (functionType.takesVarArgs() && declaredParameters <= actualParameters) || (declaredParameters == actualParameters);
  }

}
