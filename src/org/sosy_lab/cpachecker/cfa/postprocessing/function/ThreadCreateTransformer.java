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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadCreateStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;


public class ThreadCreateTransformer {

  private final static String CREATE = "ldv_thread_create";
  private final static String CREATE_SELF_PARALLEL = "ldv_thread_create_N";

  public static class ThreadFinder implements CFATraversal.CFAVisitor {

    Map<CFAEdge, CFunctionCallExpression> threadCreates = new HashMap<>();

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
      if (fName.equals(CREATE) || fName.equals(CREATE_SELF_PARALLEL)) {
        threadCreates.put(edge, exp);
      }
    }
  }

  private final LogManager logger;

  public ThreadCreateTransformer(LogManager log) {
    logger = log;
  }

  public void transform(CFA cfa) {
    ThreadFinder threadVisitor = new ThreadFinder();
    for (FunctionEntryNode functionStartNode : cfa.getAllFunctionHeads()) {
      CFATraversal.dfs().traverseOnce(functionStartNode, threadVisitor);
    }

    //We need to repeat this loop several times, because we traverse that part cfa, which is reachable from main
    for (CFAEdge edge : threadVisitor.threadCreates.keySet()) {

      CFunctionCallExpression fCall = threadVisitor.threadCreates.get(edge);
      List<CExpression> args = fCall.getParameterExpressions();
      CExpression newThreadFunction = args.get(1);
      List<CExpression> pParameters = Lists.newArrayList(args.get(2));
      CIdExpression newThreadNameExpression = getFunctionName(newThreadFunction);
      String newThreadName = newThreadNameExpression.getName();
      CFunctionEntryNode entryNode = (CFunctionEntryNode) cfa.getFunctionHead(newThreadName);
      CFunctionDeclaration pDeclaration = entryNode.getFunctionDefinition();
      CFANode pPredecessor = edge.getPredecessor();
      CFANode pSuccessor = edge.getSuccessor();
      FileLocation pFileLocation = edge.getFileLocation();
      String pRawStatement = edge.getRawStatement();

      CFACreationUtils.removeEdgeFromNodes(edge);

      CFunctionCallExpression pFunctionCallExpression = new CFunctionCallExpression(pFileLocation, pDeclaration.getType().getReturnType(), newThreadNameExpression, pParameters, pDeclaration);
      boolean isSelfParallel;
      String fName = fCall.getFunctionNameExpression().toString();
      if (fName.equals(CREATE)) {
        isSelfParallel = false;
      } else {
        assert (fName.equals(CREATE_SELF_PARALLEL)) : "Unsupported thread create function " + fName;
        isSelfParallel = true;
      }
      CFunctionCallStatement pFunctionCall = new CThreadCreateStatement(pFileLocation, pFunctionCallExpression, isSelfParallel);

      CStatementEdge callEdge = new CStatementEdge(pRawStatement, pFunctionCall, pFileLocation, pPredecessor, pSuccessor);

      CFACreationUtils.addEdgeUnconditionallyToCFA(callEdge);

    }
  }


  private CIdExpression getFunctionName(CExpression fName) {
    if (fName instanceof CIdExpression) {
      return (CIdExpression)fName;
    } else if (fName instanceof CUnaryExpression) {
      return getFunctionName(((CUnaryExpression)fName).getOperand());
    } else if (fName instanceof CCastExpression) {
      return getFunctionName(((CCastExpression)fName).getOperand());
    } else {
      assert false : "Unsupported expression in ldv_thread_create: " + fName;
      return null;
    }
  }
}
