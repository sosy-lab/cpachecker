// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import com.google.common.collect.Lists;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadCreateStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadJoinStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
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
      description = "A name of thread_create_N function")
  private String threadCreateN = "pthread_create_N";

  @Option(
      secure = true,
      name = "cfa.threads.threadJoin",
      description = "A name of thread_join function")
  private String threadJoin = "pthread_join";

  @Option(
      secure = true,
      name = "cfa.threads.threadSelfJoin",
      description = "A name of thread_join_N function")
  private String threadJoinN = "pthread_join_N";

  private class ThreadFinder implements CFATraversal.CFAVisitor {

    Map<CFAEdge, CFunctionCallExpression> threadCreates = new HashMap<>();
    Map<CFAEdge, CFunctionCallExpression> threadJoins = new HashMap<>();

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {
      if (pEdge instanceof CStatementEdge) {
        CStatement statement = ((CStatementEdge) pEdge).getStatement();
        if (statement instanceof CAssignment) {
          CRightHandSide rhs = ((CAssignment) statement).getRightHandSide();
          if (rhs instanceof CFunctionCallExpression) {
            CFunctionCallExpression exp = ((CFunctionCallExpression) rhs);
            checkFunctionExpression(pEdge, exp);
          }
        } else if (statement instanceof CFunctionCallStatement) {
          CFunctionCallExpression exp =
              ((CFunctionCallStatement) statement).getFunctionCallExpression();
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
      if (fName.equals(threadCreate) || fName.equals(threadCreateN)) {
        threadCreates.put(edge, exp);
      } else if (fName.equals(threadJoin) || fName.equals(threadJoinN)) {
        threadJoins.put(edge, exp);
      }
    }
  }

  private final LogManager logger;

  public ThreadCreateTransformer(LogManager log, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = log;
  }

  public void transform(CFA cfa) {
    ThreadFinder threadVisitor = new ThreadFinder();

    for (FunctionEntryNode functionStartNode : cfa.getAllFunctionHeads()) {
      CFATraversal.dfs().traverseOnce(functionStartNode, threadVisitor);
    }

    // We need to repeat this loop several times, because we traverse that part cfa, which is
    // reachable from main
    for (Entry<CFAEdge, CFunctionCallExpression> entry : threadVisitor.threadCreates.entrySet()) {
      CFAEdge edge = entry.getKey();
      CFunctionCallExpression fCall = entry.getValue();

      String fName = fCall.getFunctionNameExpression().toString();
      List<CExpression> args = fCall.getParameterExpressions();
      if (args.size() != 4) {
        throw new UnsupportedOperationException("More arguments expected: " + fCall);
      }

      CIdExpression varName = getThreadVariableName(fCall);
      CExpression calledFunction = args.get(2);
      CIdExpression functionNameExpression = getFunctionName(calledFunction);
      List<CExpression> functionParameters = Lists.newArrayList(args.get(3));
      String newThreadName = functionNameExpression.getName();
      CFunctionEntryNode entryNode = (CFunctionEntryNode) cfa.getFunctionHead(newThreadName);
      if (entryNode == null) {
        throw new UnsupportedOperationException(
            "Can not find the body of function " + newThreadName + "(), full line: " + edge);
      }

      CFunctionDeclaration functionDeclaration = entryNode.getFunctionDefinition();
      FileLocation pFileLocation = edge.getFileLocation();

      CFunctionCallExpression pFunctionCallExpression =
          new CFunctionCallExpression(
              pFileLocation,
              functionDeclaration.getType().getReturnType(),
              functionNameExpression,
              functionParameters,
              functionDeclaration);

      boolean isSelfParallel = !fName.equals(threadCreate);
      CFunctionCallStatement pFunctionCall =
          new CThreadCreateStatement(
              pFileLocation, pFunctionCallExpression, isSelfParallel, varName.getName());

      if (edge instanceof CStatementEdge) {
        CStatement stmnt = ((CStatementEdge) edge).getStatement();
        if (stmnt instanceof CFunctionCallAssignmentStatement) {
          /* We should replace r = pthread_create(f) into
           *   - r = TMP;
           *   - [r == 0]
           *   - f()
           */
          String pRawStatement = edge.getRawStatement();

          CFANode pPredecessor = edge.getPredecessor();
          CFANode pSuccessor = edge.getSuccessor();
          CFANode firstNode = new CFANode(pPredecessor.getFunction());
          CFANode secondNode = new CFANode(pPredecessor.getFunction());
          ((MutableCFA) cfa).addNode(firstNode);
          ((MutableCFA) cfa).addNode(secondNode);

          CFACreationUtils.removeEdgeFromNodes(edge);

          CStatement assign = prepareRandomAssignment((CFunctionCallAssignmentStatement) stmnt);
          CStatementEdge randAssign =
              new CStatementEdge(pRawStatement, assign, pFileLocation, pPredecessor, firstNode);

          CExpression assumption = prepareAssumption((CFunctionCallAssignmentStatement) stmnt, cfa);
          CAssumeEdge trueEdge =
              new CAssumeEdge(
                  pRawStatement, pFileLocation, firstNode, secondNode, assumption, true);
          CAssumeEdge falseEdge =
              new CAssumeEdge(
                  pRawStatement, pFileLocation, firstNode, pSuccessor, assumption, false);

          CStatementEdge callEdge =
              new CStatementEdge(
                  pRawStatement, pFunctionCall, pFileLocation, secondNode, pSuccessor);

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
      CFunctionCallStatement pFunctionCall =
          new CThreadJoinStatement(pFileLocation, fCall, isSelfParallel, varName.getName());

      replaceEdgeWith(edge, pFunctionCall);
    }
  }

  private void replaceEdgeWith(CFAEdge edge, CFunctionCallStatement fCall) {
    CFANode pPredecessor = edge.getPredecessor();
    CFANode pSuccessor = edge.getSuccessor();
    FileLocation pFileLocation = edge.getFileLocation();
    String pRawStatement = edge.getRawStatement();

    CFACreationUtils.removeEdgeFromNodes(edge);

    CStatementEdge callEdge =
        new CStatementEdge(pRawStatement, fCall, pFileLocation, pPredecessor, pSuccessor);

    logger.log(Level.FINE, "Replace " + edge + " with " + callEdge);
    CFACreationUtils.addEdgeUnconditionallyToCFA(callEdge);
  }

  private int tmpVarCounter = 0;

  private CStatement prepareRandomAssignment(CFunctionCallAssignmentStatement stmnt) {
    FileLocation pFileLocation = stmnt.getFileLocation();
    CFunctionCallExpression fCall = stmnt.getFunctionCallExpression();
    CLeftHandSide left = stmnt.getLeftHandSide();

    String tmpName = "CPA_TMP_" + tmpVarCounter++;
    CType retType = fCall.getDeclaration().getType().getReturnType();
    CSimpleDeclaration decl =
        new CVariableDeclaration(
            pFileLocation, false, CStorageClass.AUTO, retType, tmpName, tmpName, tmpName, null);
    CIdExpression tmp = new CIdExpression(pFileLocation, decl);

    return new CExpressionAssignmentStatement(pFileLocation, left, tmp);
  }

  private CExpression prepareAssumption(CFunctionCallAssignmentStatement stmnt, CFA cfa) {
    CLeftHandSide left = stmnt.getLeftHandSide();

    CBinaryExpressionBuilder bBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);

    try {
      return bBuilder.buildBinaryExpression(
          left, CIntegerLiteralExpression.ZERO, BinaryOperator.EQUALS);
    } catch (UnrecognizedCodeException e) {
      throw new UnsupportedOperationException("Cannot proceed: ", e);
    }
  }

  private CIdExpression getFunctionName(CExpression fName) {
    if (fName instanceof CIdExpression) {
      return (CIdExpression) fName;
    } else if (fName instanceof CUnaryExpression) {
      return getFunctionName(((CUnaryExpression) fName).getOperand());
    } else if (fName instanceof CCastExpression) {
      return getFunctionName(((CCastExpression) fName).getOperand());
    } else {
      throw new UnsupportedOperationException("Unsupported expression in pthread_create: " + fName);
    }
  }

  private CIdExpression getThreadVariableName(CFunctionCallExpression fCall) {
    CExpression var = fCall.getParameterExpressions().get(0);

    while (!(var instanceof CIdExpression)) {
      if (var instanceof CUnaryExpression) {
        // &t
        var = ((CUnaryExpression) var).getOperand();
      } else if (var instanceof CCastExpression) {
        // (void *(*)(void * ))(& ldv_factory_scenario_4)
        var = ((CCastExpression) var).getOperand();
      } else {
        throw new UnsupportedOperationException("Unsupported parameter expression " + var);
      }
    }
    return (CIdExpression) var;
  }
}
