// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import static org.sosy_lab.cpachecker.cpa.oc.EventType.READ;
import static org.sosy_lab.cpachecker.cpa.oc.EventType.WRITE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.NoException;

final class EdgeCloner {
  private static final String ONLY_C_SUPPORTED = "only C supported";
  private static final Map<EdgeIdentifier, EdgeCloner> cache = new HashMap<>();
  private static final Map<CFAEdge, EdgeCloner> reverseCache = new HashMap<>();

  static CFAEdge clone(final CFAEdge pCFAEdge, final int pid, final AbstractState pState) {
    final EdgeCloner edgeCloner = cache
        .computeIfAbsent(new EdgeIdentifier(pState, pid, pCFAEdge), pair -> new EdgeCloner(pid, pCFAEdge));
    reverseCache.put(edgeCloner.mappedEdge, edgeCloner);
    return edgeCloner.mappedEdge;
  }

  record EdgeIdentifier(AbstractState state, int pid, CFAEdge pCFAEdge) {
  }

  static List<MemoryEvent> getAccesses(final CFAEdge pCFAEdge) {
    final var edgeCloner = reverseCache.get(pCFAEdge);
    return edgeCloner.getMemoryEvents();
  }

  private CFAEdge mappedEdge;
  private final AstCloner astCloner;

  private EdgeCloner(final int idx, final CFAEdge pCFAEdge) {
    this.astCloner = new AstCloner(idx, new ArrayList<>());
    this.mappedEdge = cloneEdgeDirect(pCFAEdge);
  }

  private CFAEdge cloneEdgeDirect(final CFAEdge edge) {
    final FileLocation loc = edge.getFileLocation();
    final CFANode start = edge.getPredecessor();
    final CFANode end = edge.getSuccessor();
    final String rawStatement = edge.getRawStatement();
    return switch (edge.getEdgeType()) {
      case BlankEdge -> cloneBlankEdge(edge);
      case AssumeEdge -> cloneAssumeEdge(edge, rawStatement, loc, start, end);
      case StatementEdge -> cloneStatementEdge(edge, rawStatement, loc, start, end);
      case DeclarationEdge -> cloneDeclarationEdge(edge, rawStatement, loc, start, end);
      case ReturnStatementEdge -> cloneReturnStatementEdge(edge, rawStatement, loc, start, end);
      case FunctionCallEdge -> cloneFunctionCallEdge(edge, rawStatement, loc, start, end);
      case FunctionReturnEdge -> cloneFunctionReturnEdge(edge, loc, start, end);
      case CallToReturnEdge -> cloneCallToReturnEdge(edge, rawStatement, loc, start, end);
    };
  }

  private CFAEdge cloneBlankEdge(final CFAEdge edge) {
    return edge;
  }

  private CFAEdge cloneAssumeEdge(
      final CFAEdge edge, final String rawStatement, final FileLocation loc, final CFANode start, final CFANode end) {
    if (edge instanceof CAssumeEdge pCAssumeEdge) {
      final var newAst = astCloner.cloneAstRightSide(pCAssumeEdge.getExpression());
      if (newAst.equals(pCAssumeEdge.getExpression())) {
        return edge;
      } else {
        return new CAssumeEdge(
            rawStatement,
            loc,
            start,
            end,
            newAst,
            pCAssumeEdge.getTruthAssumption(),
            pCAssumeEdge.isSwapped(),
            pCAssumeEdge.isArtificialIntermediate());
      }
    }
    throw new AssertionError(ONLY_C_SUPPORTED);
  }

  private CFAEdge cloneStatementEdge(
      final CFAEdge edge, final String rawStatement, final FileLocation loc, final CFANode start, final CFANode end) {
    if (edge instanceof CFunctionSummaryStatementEdge pCFunctionSummaryStatementEdge) {
      final var newStatement = astCloner.cloneAst(pCFunctionSummaryStatementEdge.getStatement());
      final var newFuncCall = astCloner.cloneAst(pCFunctionSummaryStatementEdge.getFunctionCall());
      if (newStatement.equals(pCFunctionSummaryStatementEdge.getStatement())
          && newFuncCall.equals(pCFunctionSummaryStatementEdge.getFunctionCall())) {
        return edge;
      } else {
        return new CFunctionSummaryStatementEdge(
            rawStatement,
            newStatement,
            loc,
            start,
            end,
            newFuncCall,
            pCFunctionSummaryStatementEdge.getFunctionName());
      }
    } else if (edge instanceof CStatementEdge pCStatementEdge) {
      final var newStatement = astCloner.cloneAst(pCStatementEdge.getStatement());
      if (newStatement.equals(pCStatementEdge.getStatement())) {
        return edge;
      } else {
        return new CStatementEdge(rawStatement, newStatement, loc, start, end);
      }
    }
    throw new AssertionError(ONLY_C_SUPPORTED);
  }

  private CFAEdge cloneDeclarationEdge(
      final CFAEdge edge, final String rawStatement, final FileLocation loc, final CFANode start, final CFANode end) {
    if (edge instanceof CDeclarationEdge pCDeclarationEdge) {
      final var newDeclaration = astCloner.cloneAstLeftSide(pCDeclarationEdge.getDeclaration());
      if (newDeclaration.equals(pCDeclarationEdge.getDeclaration())) {
        return edge;
      } else {
        return new CDeclarationEdge(rawStatement, loc, start, end, newDeclaration);
      }
    }
    throw new AssertionError(ONLY_C_SUPPORTED);
  }

  private CFAEdge cloneReturnStatementEdge(
      final CFAEdge edge, final String rawStatement, final FileLocation loc, final CFANode start, final CFANode end) {
    assert end instanceof FunctionExitNode
        : "Expected FunctionExitNode: " + end + ", " + end.getClass();
    if (edge instanceof CReturnStatementEdge pCReturnStatementEdge) {
      final var newStatement = astCloner.cloneAst(pCReturnStatementEdge.getReturnStatement());
      if (newStatement.equals(pCReturnStatementEdge.getReturnStatement())) {
        return edge;
      } else {
        return new CReturnStatementEdge(rawStatement, newStatement, loc, start, (FunctionExitNode) end);
      }
    }
    throw new AssertionError(ONLY_C_SUPPORTED);
  }

  private CFAEdge cloneFunctionCallEdge(
      final CFAEdge edge, final String rawStatement, final FileLocation loc, final CFANode start, final CFANode end) {
    assert end instanceof CFunctionEntryNode
        : "Expected FunctionExitNode: " + end + ", " + end.getClass();
    if (edge instanceof CFunctionCallEdge pCFunctionCallEdge) {
      final var newAst = astCloner.cloneAst((CFunctionCall) pCFunctionCallEdge.getRawAST().orElseThrow());
      if (newAst.equals(pCFunctionCallEdge.getRawAST().orElseThrow())) {
        return edge;
      } else {
        return new CFunctionCallEdge(
            rawStatement,
            loc,
            start,
            (CFunctionEntryNode) end,
            newAst,
            pCFunctionCallEdge.getSummaryEdge());
      }
    }
    throw new AssertionError();
  }

  private CFAEdge cloneFunctionReturnEdge(
      final CFAEdge edge, final FileLocation loc, final CFANode start, final CFANode end) {
    if (edge instanceof CFunctionReturnEdge pCFunctionReturnEdge) {
      final var newEdge = (CFunctionSummaryEdge) cloneEdgeDirect(pCFunctionReturnEdge.getSummaryEdge());
      if (newEdge.equals(pCFunctionReturnEdge.getSummaryEdge())) {
        return edge;
      } else {
        return new CFunctionReturnEdge(loc, (FunctionExitNode) start, end, newEdge);
      }
    }
    throw new AssertionError(ONLY_C_SUPPORTED);
  }

  private CFAEdge cloneCallToReturnEdge(
      final CFAEdge edge, final String rawStatement, final FileLocation loc, final CFANode start, final CFANode end) {
    if (edge instanceof CFunctionSummaryEdge pCFunctionSummaryEdge) {
      final var newExpr = astCloner.cloneAst(pCFunctionSummaryEdge.getExpression());
      if (newExpr.equals(pCFunctionSummaryEdge.getExpression())) {
        return edge;
      } else {
        return new CFunctionSummaryEdge(
            rawStatement, loc, start, end, newExpr, pCFunctionSummaryEdge.getFunctionEntry());
      }
    }
    throw new AssertionError(ONLY_C_SUPPORTED);
  }

  public List<MemoryEvent> getMemoryEvents() {
    return astCloner.getMemoryEvents();
  }
}
