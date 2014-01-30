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
package org.sosy_lab.cpachecker.util;

import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpressionCollectingVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;


public class TokenCollector {

  private static Map<String, Set<Integer>> variableRelatedTokens = Maps.newHashMap();

  private static void collectLine (final SortedSet<Integer> target, final FileLocation loc, boolean overApproximateTokens) {
    if (loc != null) {
      if (overApproximateTokens) {
        int lowerBound = loc.getStartingLineNumber();
        int upperBound = loc.getEndingLineNumber();
        if (target.size() > 0) {
          lowerBound = Math.min(lowerBound, target.first());
          upperBound = Math.max(upperBound, target.last());
        }
        for (int line=lowerBound; line<=upperBound; line++) {
          target.add(line);
        }
      } else {
        target.add(loc.getStartingLineNumber());
      }
    }
  }

  private static class TokenCollectingVisitor implements CStatementVisitor<Void, RuntimeException>,
      CInitializerVisitor<Void, RuntimeException>, CExpressionVisitor<Void, RuntimeException> {

    public final TreeSet<Integer> result = Sets.newTreeSet();

    private final boolean overApproximateTokens;

    public TokenCollectingVisitor(boolean overApproximateTokens) {
      this.overApproximateTokens = overApproximateTokens;
    }

    private void addFromLoc (final FileLocation loc) {
      collectLine(result, loc, overApproximateTokens);
    }

    @Override
    public Void visit(CArraySubscriptExpression pE) throws RuntimeException {
      addFromLoc(pE.getFileLocation());
      pE.getArrayExpression().accept(this);
      pE.getSubscriptExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(CBinaryExpression pE) throws RuntimeException {
      addFromLoc(pE.getFileLocation());
      pE.getOperand1().accept(this);
      pE.getOperand2().accept(this);
      return null;
    }

    @Override
    public Void visit(CCastExpression pE) throws RuntimeException {
      addFromLoc(pE.getFileLocation());
      pE.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CComplexCastExpression pE) throws RuntimeException {
      addFromLoc(pE.getFileLocation());
      pE.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CFieldReference pE) throws RuntimeException {
      addFromLoc(pE.getFileLocation());
      pE.getFieldOwner().accept(this);
      return null;
    }

    @Override
    public Void visit(CPointerExpression pE) throws RuntimeException {
      addFromLoc(pE.getFileLocation());
      pE.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CUnaryExpression pE) throws RuntimeException {
      addFromLoc(pE.getFileLocation());
      pE.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CInitializerExpression pE) throws RuntimeException {
      addFromLoc(pE.getFileLocation());
      pE.getExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(CInitializerList pI) throws RuntimeException {
      addFromLoc(pI.getFileLocation());
      for (CInitializer i: pI.getInitializers()) {
        addFromLoc(i.getFileLocation());
        i.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CDesignatedInitializer pI) throws RuntimeException {
      addFromLoc(pI.getFileLocation());
      return null;
    }

    @Override
    public Void visit(CExpressionAssignmentStatement pS) throws RuntimeException {
      addFromLoc(pS.getFileLocation());
      pS.getLeftHandSide().accept(this);
      pS.getRightHandSide().accept(this);
      return null;
    }

    @Override
    public Void visit(CExpressionStatement pS) throws RuntimeException {
      addFromLoc(pS.getFileLocation());
      pS.getExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(CFunctionCallAssignmentStatement pS) throws RuntimeException {
      addFromLoc(pS.getFileLocation());
      pS.getLeftHandSide().accept(this);
      pS.getRightHandSide().getFunctionNameExpression().accept(this);
      for (CExpression expr : pS.getRightHandSide().getParameterExpressions()) {
        expr.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CFunctionCallStatement pS) throws RuntimeException {
      addFromLoc(pS.getFileLocation());
      return null;
    }

    @Override
    public Void visit(CIdExpression pE) throws RuntimeException {
      addFromLoc(pE.getFileLocation());
      return null;
    }

    @Override
    public Void visit(CCharLiteralExpression pE) throws RuntimeException {
      addFromLoc(pE.getFileLocation());
      return null;
    }

    @Override
    public Void visit(CFloatLiteralExpression pE) throws RuntimeException {
      if (pE.getFileLocation() != null) {
        addFromLoc(pE.getFileLocation());
      }
      return null;
    }

    @Override
    public Void visit(CIntegerLiteralExpression pE) throws RuntimeException {
      if (pE.getFileLocation() != null) {
        addFromLoc(pE.getFileLocation());
      }
      return null;
    }

    @Override
    public Void visit(CStringLiteralExpression pE) throws RuntimeException {
      addFromLoc(pE.getFileLocation());
      return null;
    }

    @Override
    public Void visit(CTypeIdExpression pE) throws RuntimeException {
      addFromLoc(pE.getFileLocation());
      return null;
    }

    @Override
    public Void visit(CTypeIdInitializerExpression pE) throws RuntimeException {
      addFromLoc(pE.getFileLocation());
      return null;
    }

    @Override
    public Void visit(CImaginaryLiteralExpression pE) throws RuntimeException {
      addFromLoc(pE.getFileLocation());
      return null;
    }

    public Set<Integer> collectTokensFrom(CExpression astNode) {
      astNode.accept(this);
      return result;
    }

    public Set<Integer> collectTokensFrom(CStatement astNode) {
      astNode.accept(this);
      return result;
    }

    public Set<Integer> collectTokensFrom(CInitializer astNode) {
      astNode.accept(this);
      return result;
    }

  }

  public static Set<Integer> collectTokensFrom(CAstNode astNode, boolean overApproximateTokens) {
    TokenCollectingVisitor visitor = new TokenCollectingVisitor(overApproximateTokens);

    if (astNode instanceof CStatement) {
      return visitor.collectTokensFrom((CStatement) astNode);
    } else if (astNode instanceof CExpression) {
      return visitor.collectTokensFrom((CExpression) astNode);
    } else if (astNode instanceof CInitializer) {
      return visitor.collectTokensFrom((CInitializer) astNode);
    }

    return Collections.emptySet();
  }

  public static synchronized Set<Integer> getTokensFromCFAEdge(CFAEdge pEdge, boolean overApproximateTokens) {
    final TreeSet<Integer> result = Sets.newTreeSet();
    final Deque<CFAEdge> edges = Queues.newArrayDeque();
    final Deque<CAstNode> astNodes = Queues.newArrayDeque();

    if (overApproximateTokens) {
      Set<String> variables = getEdgeVariableNames(pEdge);
      for (String variable: variables) {
        if (variable.contains("__CPA")) {
          Set<Integer> tokens = variableRelatedTokens.get(variable);
          if (tokens != null) {
            result.addAll(tokens);
          } else {
            result.addAll(Collections.<Integer>emptySet());
          }
        }
      }
    }

    edges.add(pEdge);

    while (!edges.isEmpty()) {
      CFAEdge edge = edges.pop();
      CFANode startNode = edge.getPredecessor();
      CFANode endNode = edge.getSuccessor();

      if (overApproximateTokens) {
        result.add(edge.getLineNumber());
      }

      switch (edge.getEdgeType()) {
      case MultiEdge:
        edges.addAll(((MultiEdge) edge).getEdges());
      break;
      case AssumeEdge:
        if (overApproximateTokens) {
          result.add(endNode.getLineNumber());

          // Assumes of a while loop should also include the while token
          for (CFAEdge e: CFAUtils.enteringEdges(startNode)) {
            if (e instanceof BlankEdge) {
              result.add(e.getLineNumber());
            }
          }
        }
        CAssumeEdge assumeEdge = ((CAssumeEdge) edge);
        astNodes.add(assumeEdge.getExpression());
      break;
      case CallToReturnEdge:
        CFunctionSummaryEdge fnSumEdge = (CFunctionSummaryEdge) edge;
        result.add(fnSumEdge.getLineNumber());
        result.addAll(collectTokensFrom(fnSumEdge.getExpression(), overApproximateTokens));
        result.addAll(collectTokensFrom(fnSumEdge.getExpression().getFunctionCallExpression().getFunctionNameExpression(), overApproximateTokens));
        collectLine(result, fnSumEdge.getExpression().getFileLocation(), overApproximateTokens);
        collectLine(result, fnSumEdge.getExpression().getFunctionCallExpression().getFileLocation(), overApproximateTokens);
        astNodes.addAll(fnSumEdge.getExpression().getFunctionCallExpression().getParameterExpressions());
      break;
      case DeclarationEdge:
        CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
        collectLine(result, decl.getFileLocation(), overApproximateTokens);
        if (decl instanceof CVariableDeclaration) {
          CVariableDeclaration varDecl = (CVariableDeclaration) decl;
          if (varDecl.getInitializer() != null) {
            result.addAll(collectTokensFrom(varDecl.getInitializer(), overApproximateTokens));
          }
        }
      break;
      case FunctionCallEdge:
        if (edge.getPredecessor().getLeavingSummaryEdge() != null) {
          edges.add(edge.getPredecessor().getLeavingSummaryEdge());
        }
        result.add(((CFunctionCallEdge) edge).getLineNumber());
        astNodes.addAll(((CFunctionCallEdge) edge).getArguments());
      break ;
      case FunctionReturnEdge:
        result.add(((CFunctionReturnEdge) edge).getLineNumber());
      break;
      case ReturnStatementEdge:
        CExpression expr = ((CReturnStatementEdge) edge).getExpression();
        if (expr != null) {
          result.add(((CReturnStatementEdge) edge).getLineNumber());
        }
      break;
      case StatementEdge:
        result.addAll(collectTokensFrom(((CStatementEdge) edge).getStatement(), overApproximateTokens));
      break;
      }

      while(!astNodes.isEmpty()) {
        CAstNode node = astNodes.pop();
        result.addAll(collectTokensFrom(node, overApproximateTokens));
      }
    }

    return result;
  }

  public static synchronized void getKnownToEdge(CFAEdge edge) {
    Set<String> variables = getEdgeVariableNames(edge);
    Set<Integer> tokens = getTokensFromCFAEdge(edge, true);

    // Store for each variable the related tokens
    for (String variable: variables) {
      Set<Integer> variableTokens = variableRelatedTokens.get(variable);
      if (variableTokens == null) {
        variableTokens = Sets.newTreeSet();
        variableRelatedTokens.put(variable, variableTokens);
      }
      variableTokens.addAll(tokens);
    }
  }

  public static Set<String> getEdgeVariableNames(CFAEdge subject) {
    CIdExpressionCollectingVisitor visitor = new CIdExpressionCollectingVisitor();

    final Set<String> result = Sets.newHashSet();
    final Set<CIdExpression> idExs = Sets.newHashSet();
    final Deque<CFAEdge> edges = Queues.newArrayDeque();

    edges.add(subject);

    while (!edges.isEmpty()) {
      CFAEdge edge = edges.pop();

      switch (edge.getEdgeType()) {
      case MultiEdge:
        edges.addAll(((MultiEdge) edge).getEdges());
      break;
      case AssumeEdge:
        CAssumeEdge assumeEdge = ((CAssumeEdge) edge);
        idExs.addAll(assumeEdge.getExpression().accept(visitor));
      break;
      case CallToReturnEdge:
        CFunctionSummaryEdge fnSumEdge = (CFunctionSummaryEdge) edge;
        idExs.addAll(fnSumEdge.getExpression().accept(visitor));
      break;
      case DeclarationEdge:
        CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
        if (decl instanceof CVariableDeclaration) {
          CVariableDeclaration varDecl = (CVariableDeclaration) decl;
          result.add(varDecl.getQualifiedName());
          if (varDecl.getInitializer() != null) {
            idExs.addAll(varDecl.getInitializer().accept(visitor));
          }
        }
      break;
      case FunctionCallEdge:
        CFunctionCallEdge callEdge = (CFunctionCallEdge) edge;
        for (CExpression e: callEdge.getArguments()) {
          idExs.addAll(e.accept(visitor));
        }
      break ;
      case FunctionReturnEdge:
      break;
      case ReturnStatementEdge:
        CExpression expr = ((CReturnStatementEdge) edge).getExpression();
        if (expr != null) {
          idExs.addAll(expr.accept(visitor));
        }
      break;
      case StatementEdge:
        idExs.addAll(((CStatementEdge) edge).getStatement().accept(visitor));
      break;
      }
    }

    for (CIdExpression e: idExs) {
      result.add(VariableClassification.getScopedName(subject, e));
    }

    return result;
  }


}
