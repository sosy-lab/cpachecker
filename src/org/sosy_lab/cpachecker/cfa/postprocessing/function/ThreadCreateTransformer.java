/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadCreateStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadJoinStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

@Options
public class ThreadCreateTransformer {

  @Option(
      secure = true,
      name = "cfa.threads.threadCreate",
      description = "A name of thread_create function")
  private String threadCreate = "pthread_create";

  @Option(
      secure = true,
      name = "cfa.threads.threadSelfCreate",
      description = "A name of thread_join function")
  private String threadCreateN = "pthread_create_N";

  @Option(
      secure = true,
      name = "cfa.threads.threadJoin",
      description = "A name of thread_create_N function")
  private String threadJoin = "pthread_join";

  @Option(
      secure = true,
      name = "cfa.threads.threadSelfJoin",
      description = "A name of thread_join_N function")
  private String threadJoinN = "pthread_join_N";

  public static class ThreadFinder implements CFATraversal.CFAVisitor {

    private final String tCreate;
    private final String tCreateN;
    private final String tJoin;
    private final String tJoinN;

    ThreadFinder(String create, String createN, String join, String joinN) {
      tCreate = create;
      tCreateN = createN;
      tJoin = join;
      tJoinN = joinN;
    }

    Map<CFAEdge, CFunctionCallExpression> threadCreates = new HashMap<>();
    Map<CFAEdge, CFunctionCallExpression> threadJoins = new HashMap<>();

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {
      if (pEdge instanceof CStatementEdge) {
        CStatement statement = ((CStatementEdge)pEdge).getStatement();
        if (statement instanceof CAssignment) {
          CRightHandSide rhs = ((CAssignment)statement).getRightHandSide();
          if (rhs instanceof CFunctionCallExpression) {
            CFunctionCallExpression exp = ((CFunctionCallExpression)rhs);
            checkFunctionExpression(pEdge, exp);
          }
        } else if (statement instanceof CFunctionCallStatement) {
          CFunctionCallExpression exp = ((CFunctionCallStatement)statement).getFunctionCallExpression();
          checkFunctionExpression(pEdge, exp);
        }
      }
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitNode(CFANode pNode) {
      return TraversalProcess.CONTINUE;
    }

    private void checkFunctionExpression(CFAEdge edge, CFunctionCallExpression exp) {
      String fName = exp.getFunctionNameExpression().toString();
      if (fName.equals(tCreate) || fName.equals(tCreateN)) {
        threadCreates.put(edge, exp);
      } else if (fName.equals(tJoin) || fName.equals(tJoinN)) {
        threadJoins.put(edge, exp);
      }
    }
  }

  private final LogManager logger;

  public ThreadCreateTransformer(LogManager log, Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    logger = log;
  }

  public void transform(CFA cfa) throws InvalidConfigurationException, CParserException {
    ThreadFinder threadVisitor = new ThreadFinder(threadCreate, threadCreateN, threadJoin, threadJoinN);

    for (FunctionEntryNode functionStartNode : cfa.getAllFunctionHeads()) {
      CFATraversal.dfs().traverseOnce(functionStartNode, threadVisitor);
    }

    //We need to repeat this loop several times, because we traverse that part cfa, which is reachable from main
    for (Entry<CFAEdge, CFunctionCallExpression> entry : threadVisitor.threadCreates.entrySet()) {
      CFAEdge edge = entry.getKey();
      CFunctionCallExpression fCall = entry.getValue();

      String fName = fCall.getFunctionNameExpression().toString();
      List<CExpression> args = fCall.getParameterExpressions();
      if (args.size() < 3) {
        throw new InvalidConfigurationException("More arguments expected: " + fCall);
      }

      int magicNum = fName.startsWith("ldv_") ? 0 : 1;
      CIdExpression varName = getThreadVariableName(fCall);
      CExpression newThreadFunction = args.get(1 + magicNum);
      CIdExpression newThreadNameExpression = getFunctionName(newThreadFunction);
      List<CExpression> pParameters = Lists.newArrayList(args.get(2 + magicNum));
      String newThreadName = newThreadNameExpression.getName();
      CFunctionEntryNode entryNode = (CFunctionEntryNode) cfa.getFunctionHead(newThreadName);
      if (entryNode == null) {
        throw new InvalidConfigurationException("Can not find the body of function " + newThreadName + "(), full line: " + edge);
      }

      CFunctionDeclaration pDeclaration = entryNode.getFunctionDefinition();
      FileLocation pFileLocation = edge.getFileLocation();

      CFunctionCallExpression pFunctionCallExpression = new CFunctionCallExpression(pFileLocation, pDeclaration.getType().getReturnType(), newThreadNameExpression, pParameters, pDeclaration);
      boolean isSelfParallel = !fName.equals(threadCreate);
      CFunctionCallStatement pFunctionCall = new CThreadCreateStatement(pFileLocation, pFunctionCallExpression, isSelfParallel, varName.getName());

      if (edge instanceof CStatementEdge) {
        CStatement stmnt = ((CStatementEdge) edge).getStatement();
        if (stmnt instanceof CFunctionCallAssignmentStatement) {
          /* We should replace r = pthread_create(f) into
           *   - r = __VERIFIER_nondet_int();
           *   - [r == 0]
           *   - f()
           */
          String pRawStatement = edge.getRawStatement();

          CFANode pPredecessor = edge.getPredecessor();
          CFANode pSuccessor = edge.getSuccessor();
          CFANode firstNode = new CFANode(pPredecessor.getFunctionName());
          CFANode secondNode = new CFANode(pPredecessor.getFunctionName());
          ((MutableCFA) cfa).addNode(firstNode);
          ((MutableCFA) cfa).addNode(secondNode);

          CFACreationUtils.removeEdgeFromNodes(edge);

          CStatement assign = prepareRandomAssignment((CFunctionCallAssignmentStatement) stmnt);
          CStatementEdge randAssign = new CStatementEdge(pRawStatement, assign, pFileLocation, pPredecessor, firstNode);

          CExpression assumption = prepareAssumption((CFunctionCallAssignmentStatement) stmnt, cfa);
          CAssumeEdge trueEdge = new CAssumeEdge(pRawStatement, pFileLocation, firstNode, secondNode, assumption, true);
          CAssumeEdge falseEdge = new CAssumeEdge(pRawStatement, pFileLocation, firstNode, pSuccessor, assumption, false);

          CStatementEdge callEdge = new CStatementEdge(pRawStatement, pFunctionCall, pFileLocation, secondNode, pSuccessor);

          CFACreationUtils.addEdgeUnconditionallyToCFA(callEdge);
          CFACreationUtils.addEdgeUnconditionallyToCFA(randAssign);
          CFACreationUtils.addEdgeUnconditionallyToCFA(trueEdge);
          CFACreationUtils.addEdgeUnconditionallyToCFA(falseEdge);

          logger.log(Level.FINE, "Replace " + edge + " with " + callEdge);
        } else {
          replaceEdgeWith(edge, pFunctionCall);
        }

      } else {
        replaceEdgeWith(edge, pFunctionCall);
      }
    }

    for (Entry<CFAEdge, CFunctionCallExpression> entry : threadVisitor.threadJoins.entrySet()) {
      CFAEdge edge = entry.getKey();
      CFunctionCallExpression fCall = entry.getValue();
      CIdExpression varName = getThreadVariableName(fCall);
      FileLocation pFileLocation = edge.getFileLocation();

      String fName = fCall.getFunctionNameExpression().toString();
      boolean isSelfParallel = !fName.equals(threadJoin);
      CFunctionCallStatement pFunctionCall = new CThreadJoinStatement(pFileLocation, fCall, isSelfParallel, varName.getName());

      replaceEdgeWith(edge, pFunctionCall);
    }
  }

  private void replaceEdgeWith(CFAEdge edge, CFunctionCallStatement fCall) {
    CFANode pPredecessor = edge.getPredecessor();
    CFANode pSuccessor = edge.getSuccessor();
    FileLocation pFileLocation = edge.getFileLocation();
    String pRawStatement = edge.getRawStatement();

    CFACreationUtils.removeEdgeFromNodes(edge);

    CStatementEdge callEdge = new CStatementEdge(pRawStatement, fCall, pFileLocation, pPredecessor, pSuccessor);

    logger.log(Level.FINE, "Replace " + edge + " with " + callEdge);
    CFACreationUtils.addEdgeUnconditionallyToCFA(callEdge);

  }

  private CStatement prepareRandomAssignment(CFunctionCallAssignmentStatement stmnt) {
    FileLocation pFileLocation = stmnt.getFileLocation();
    CFunctionCallExpression fCall = stmnt.getFunctionCallExpression();
    CLeftHandSide left = stmnt.getLeftHandSide();

    CFunctionDeclaration tmpDecl =
        new CFunctionDeclaration(
            pFileLocation,
            new CFunctionType(left.getExpressionType(), Collections.emptyList(), false),
            "__VERIFIER_nondet_int",
            Collections.emptyList());
    CIdExpression tmpFuncName = new CIdExpression(pFileLocation, tmpDecl);

    CFunctionCallExpression nondetFunc =
        new CFunctionCallExpression(
            pFileLocation,
            left.getExpressionType(),
            tmpFuncName,
            Collections.emptyList(),
            tmpDecl);

    return new CFunctionCallAssignmentStatement(pFileLocation, left, nondetFunc);
  }

  private CExpression prepareAssumption(CFunctionCallAssignmentStatement stmnt, CFA cfa)
      throws InvalidConfigurationException {
    CLeftHandSide left = stmnt.getLeftHandSide();

    CBinaryExpressionBuilder bBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);

    try {
      return bBuilder.buildBinaryExpression(left, CIntegerLiteralExpression.ZERO, BinaryOperator.EQUALS);
    } catch (UnrecognizedCCodeException e) {
      throw new InvalidConfigurationException("Cannot proceed: ", e);
    }
  }


  private CIdExpression getFunctionName(CExpression fName) {
    if (fName instanceof CIdExpression) {
      return (CIdExpression)fName;
    } else if (fName instanceof CUnaryExpression) {
      return getFunctionName(((CUnaryExpression)fName).getOperand());
    } else if (fName instanceof CCastExpression) {
      return getFunctionName(((CCastExpression) fName).getOperand());
    } else {
      assert false : "Unsupported expression in ldv_thread_create: " + fName;
      return null;
    }
  }

  private CIdExpression getThreadVariableName(CFunctionCallExpression fCall) throws CParserException {
    CExpression var = fCall.getParameterExpressions().get(0);

    while (!(var instanceof CIdExpression)) {
      if (var instanceof CUnaryExpression) {
        //&t
        var = ((CUnaryExpression)var).getOperand();
      } else if (var instanceof CCastExpression) {
        //(void *(*)(void * ))(& ldv_factory_scenario_4)
        var = ((CCastExpression) var).getOperand();
      } else {
        throw new CParserException("Unsupported parameter expression " + var);
      }
    }
    return (CIdExpression) var;
  }
}
