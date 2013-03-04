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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
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
import org.sosy_lab.cpachecker.cfa.types.Type;
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

  @Option(name="analysis.createUndefinedFunctionCall",
      description="create undefined function call summary statement edge")
  private boolean createUndefinedFunctionCall = true;

  private enum FunctionSet {
    ALL, //all defined functions considered (Warning: some CPAs require at least EQ_PARAM_SIZES)
    EQ_PARAM_SIZES, //all functions with matching number of parameters considered
    EQ_PARAM_TYPES, //all functions with matching number and types of parameters considered
    USED_IN_CODE //includes only functions which address is taken in the code
  }

  private FunctionSet functionSet = FunctionSet.USED_IN_CODE;

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
      if(isDefined(f)) {
        createCallAndReturnEdges(statement, functionCall);
      } else {
        logger.log(Level.FINEST, "Call to undefined function " + f);
        //TODO: decide: should we create summary edge?
      }
    } else {
      logger.log(Level.FINEST, "Function pointer call " + f);
      if(language == Language.C && fptrCallEdges) {
        CExpression nameExp = (CExpression)f.getFunctionNameExpression();
        Collection<FunctionEntryNode> funcs = getFunctionSet(f, functionCall);
        CFANode start = statement.getPredecessor();
        CFANode end = statement.getSuccessor();
        // delete old edge
        CFACreationUtils.removeEdgeFromNodes(statement);
        logger.log(Level.FINEST, "Size of matching function set is " + funcs.size());

        CFANode rootNode = start;
        CFANode elseNode = null;
        for(FunctionEntryNode fNode : funcs) {
          logger.log(Level.FINEST, "Matching function: " + fNode.getFunctionName());
          CFANode thenNode = newCFANode(start.getLineNumber(), start.getFunctionName());
          elseNode = newCFANode(start.getLineNumber(), start.getFunctionName());
          CIdExpression func = createIdExpression(nameExp, fNode);
          CUnaryExpression amper = new CUnaryExpression(nameExp.getFileLocation(),
              nameExp.getExpressionType(), func, CUnaryExpression.UnaryOperator.AMPER);
          CBinaryExpression condition = new CBinaryExpression(f.getFileLocation(),
              CNumericTypes.INT, nameExp, amper, BinaryOperator.EQUALS);

          addConditionEdges(condition, rootNode, thenNode, elseNode, start.getLineNumber());


          CFANode retNode = newCFANode(start.getLineNumber(), start.getFunctionName());
          //create special summary edge
          //thenNode-->retNode
          String pRawStatement = "pointer call(" + fNode.getFunctionName() + ") " + statement.getRawStatement();

          //replace function call by pointer expression with regular call by name (in functionCall and edge.getStatement())
          CFunctionCall regularCall;
          if(functionSet != FunctionSet.EQ_PARAM_TYPES) {
            regularCall = (CFunctionCall)functionCall;
          } else {
            regularCall = createRegularCall((CFunctionCall)functionCall, fNode);
          }

          FunctionSummaryEdge calltoReturnEdge = createSpecialSummaryEdge(statement.getLineNumber(),
              pRawStatement,
              thenNode, retNode, regularCall);

          createCallAndSummaryStatementEdge(calltoReturnEdge, statement.getLineNumber(), pRawStatement, regularCall, fNode, false);

          //retNode-->end
          BlankEdge be = new BlankEdge("skip", statement.getLineNumber(), retNode, end, "skip");
          CFACreationUtils.addEdgeUnconditionallyToCFA(be);

          rootNode = elseNode;
        }

        if(elseNode==null) {
          logger.log(Level.INFO, "Functions matching function pointer call " + nameExp + " were not found");
          createUndefinedSummaryStatementEdge(rootNode, end, statement, functionCall);
        } else {
          //rootNode --> end
          if(createUndefinedFunctionCall) {
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
  }

  private Set<String> addressedFunctions;

  @Override
  public void collectDataRecursively() {
    if(language == Language.C && fptrCallEdges) {
      if(functionSet == FunctionSet.USED_IN_CODE) {
        addressedFunctions = new HashSet<>();
        logger.log(Level.FINEST, "Collect function pointer variables");
        for (FunctionEntryNode functionStartNode : cfa.getAllFunctionHeads()) {
          Set<String> vars = FunctionPointerVariablesCollector.collectVars(functionStartNode);
          logger.log(Level.FINEST, "Function pointer variables size = " + vars.size());
          logger.log(Level.FINEST, "Function pointer variables = " + vars);
          addressedFunctions.addAll(vars);
        }
      }
    }
  }

  private CIdExpression createIdExpression(CExpression nameExp,
      FunctionEntryNode fNode) {
    return new CIdExpression(nameExp.getFileLocation(),
        nameExp.getExpressionType(),
        fNode.getFunctionName(),
        (CSimpleDeclaration)fNode.getFunctionDefinition());
  }

  private CFunctionCall createRegularCall(CFunctionCall functionCall, FunctionEntryNode fNode) {
    if(functionCall instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement asgn = (CFunctionCallAssignmentStatement)functionCall;
      CFunctionCallExpression e = asgn.getRightHandSide();
      CFunctionCallExpression newExpr = new CFunctionCallExpression(e.getFileLocation(), e.getExpressionType(),
          createIdExpression(e.getFunctionNameExpression(), fNode),
          e.getParameterExpressions(), (CFunctionDeclaration)fNode.getFunctionDefinition());
      return new CFunctionCallAssignmentStatement(asgn.getFileLocation(), asgn.getLeftHandSide(), newExpr);
    } else if(functionCall instanceof CFunctionCallStatement) {
      CFunctionCallStatement asgn = (CFunctionCallStatement)functionCall;
      CFunctionCallExpression e = asgn.getFunctionCallExpression();
      CFunctionCallExpression newExpr = new CFunctionCallExpression(e.getFileLocation(), e.getExpressionType(),
          createIdExpression(e.getFunctionNameExpression(), fNode),
          e.getParameterExpressions(), (CFunctionDeclaration)fNode.getFunctionDefinition());
      return new CFunctionCallStatement(asgn.getFileLocation(), newExpr);
    } else {
      return functionCall;
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
    if(language == Language.C) {

      if(summaryEdges) {
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

    } else if(language == Language.JAVA){

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

    if(!checkParamSizes(functionCallExpression, fDefNode.getFunctionDefinition().getType())) {
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

  private Collection<FunctionEntryNode> getFunctionSet(
      AFunctionCallExpression functionCallExpression, AFunctionCall expr) {
    if(functionSet == FunctionSet.ALL) {
      return cfa.getAllFunctionHeads();
    } else {
      Collection<FunctionEntryNode> col = cfa.getAllFunctionHeads();
      Collection<FunctionEntryNode> res = new ArrayList<>();
      if(functionSet == FunctionSet.EQ_PARAM_SIZES) {
        for(FunctionEntryNode f : col) {
          if(checkParamSizes(expr.getFunctionCallExpression(), f.getFunctionDefinition().getType())) {
            res.add(f);
          }
        }
        return res;
      } else {
        for(FunctionEntryNode f : col) {
          if(checkParamSizesAndTypes(expr.getFunctionCallExpression(), f.getFunctionDefinition().getType())) {
            if(functionSet == FunctionSet.EQ_PARAM_TYPES) {
              res.add(f);
            } else {
              //functionSet == FunctionSet.USED_IN_CODE
              if(addressedFunctions.contains(f.getFunctionName())) {
                res.add(f);
              }
            }
          }
        }
        return res;
      }
    }
  }

  private boolean checkParamSizesAndTypes(
      AFunctionCallExpression functionCallExpression, IAFunctionType functionType) {

    if(!checkParamSizes(functionCallExpression, functionType))
      return false;

    // check if the number of function parameters are right
    int declaredParameters = functionType.getParameters().size();

    Type declRet = functionType.getReturnType();
    Type actRet = functionCallExpression.getExpressionType();
    boolean b = declRet.equals(actRet);
    if(!b) {
      //logger.log(Level.FINEST, "declRet=" + declRet + ", actRet=" + actRet);
      return false;
    }
    List<? extends Type> declParams = functionType.getParameters();
    List<? extends IAExpression> exprParams = functionCallExpression.getParameterExpressions();
    for(int i=0; i<declaredParameters; i++) {
      Type dt = declParams.get(i);
      Type et = exprParams.get(i).getExpressionType();
      assert dt!=null;
      if(!dt.equals(et)) {
        return false;
      }
      logger.log(Level.FINEST, "declType=" + functionType);
      logger.log(Level.FINEST, "param[" + i + "] decl=" + dt + ", expr=" + et);
    }

    return true;
  }
}
