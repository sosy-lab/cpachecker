// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Edge cloner for POR that uses {@link PorAstCloner} (thread-ID-only variable renaming, no CSSA
 * indices).
 */
final class PorEdgeCloner {
  private static final String ONLY_C_SUPPORTED = "only C supported";
  private static final Map<EdgeIdentifier, PorEdgeCloner> cache = new HashMap<>();
  private static final Map<CFAEdge, EdgeIdentifier> reverseCache = new HashMap<>();

  record EdgeIdentifier(AbstractState state, int pid, CFAEdge pCFAEdge) {
  }

  static CFAEdge clone(final CFAEdge pCFAEdge, final int pid, final AbstractState pState) {
    final EdgeIdentifier identifier = new EdgeIdentifier(pState, pid, pCFAEdge);
    final PorEdgeCloner edgeCloner =
        cache.computeIfAbsent(identifier, pair -> new PorEdgeCloner(pid, pCFAEdge));
    reverseCache.put(edgeCloner.mappedEdge, identifier);
    return edgeCloner.mappedEdge;
  }

  static Integer getPid(final CFAEdge pCFAEdge) {
    final EdgeIdentifier identifier = reverseCache.get(pCFAEdge);
    if (identifier == null) {
      return null;
    }
    return identifier.pid;
  }


  private final PorAstCloner astCloner;
  private final CFAEdge mappedEdge;

  private PorEdgeCloner(final int idx, final CFAEdge pCFAEdge) {
    this.astCloner = new PorAstCloner(idx);
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
    return new BlankEdge(edge.getRawStatement(), edge.getFileLocation(), edge.getPredecessor(),
        edge.getSuccessor(), edge.getDescription());
  }

  private CFAEdge cloneAssumeEdge(
      final CFAEdge edge,
      final String rawStatement,
      final FileLocation loc,
      final CFANode start,
      final CFANode end) {
    if (edge instanceof CAssumeEdge pCAssumeEdge) {
      final var newAst = astCloner.cloneAstRightSide(pCAssumeEdge.getExpression());
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
    throw new AssertionError(ONLY_C_SUPPORTED);
  }

  private CFAEdge cloneStatementEdge(
      final CFAEdge edge,
      final String rawStatement,
      final FileLocation loc,
      final CFANode start,
      final CFANode end) {
    if (edge instanceof CFunctionSummaryStatementEdge pCFunctionSummaryStatementEdge) {
      final var newStatement = astCloner.cloneAst(pCFunctionSummaryStatementEdge.getStatement());
      final var newFuncCall = astCloner.cloneAst(pCFunctionSummaryStatementEdge.getFunctionCall());
      return new CFunctionSummaryStatementEdge(
          rawStatement,
          newStatement,
          loc,
          start,
          end,
          newFuncCall,
          pCFunctionSummaryStatementEdge.getFunctionName());
    } else if (edge instanceof CStatementEdge pCStatementEdge) {
      final var newStatement = astCloner.cloneAst(pCStatementEdge.getStatement());
      return new CStatementEdge(rawStatement, newStatement, loc, start, end);
    }
    throw new AssertionError(ONLY_C_SUPPORTED);
  }

  private CFAEdge cloneDeclarationEdge(
      final CFAEdge edge,
      final String rawStatement,
      final FileLocation loc,
      final CFANode start,
      final CFANode end) {
    if (edge instanceof CDeclarationEdge pCDeclarationEdge) {
      final var newDeclaration = astCloner.cloneAstLeftSide(pCDeclarationEdge.getDeclaration());
      return new CDeclarationEdge(rawStatement, loc, start, end, newDeclaration);
    }
    throw new AssertionError(ONLY_C_SUPPORTED);
  }

  private CFAEdge cloneReturnStatementEdge(
      final CFAEdge edge,
      final String rawStatement,
      final FileLocation loc,
      final CFANode start,
      final CFANode end) {
    assert end instanceof FunctionExitNode
        : "Expected FunctionExitNode: " + end + ", " + end.getClass();
    if (edge instanceof CReturnStatementEdge pCReturnStatementEdge) {
      final var newStatement = astCloner.cloneAst(pCReturnStatementEdge.getReturnStatement());
      return new CReturnStatementEdge(
          rawStatement, newStatement, loc, start, (FunctionExitNode) end);
    }
    throw new AssertionError(ONLY_C_SUPPORTED);
  }

  private CFAEdge cloneFunctionCallEdge(
      final CFAEdge edge,
      final String rawStatement,
      final FileLocation loc,
      final CFANode start,
      final CFANode end) {
    assert end instanceof CFunctionEntryNode
        : "Expected FunctionExitNode: " + end + ", " + end.getClass();
    if (edge instanceof CFunctionCallEdge pCFunctionCallEdge) {
      final var newAst =
          astCloner.cloneAst((CFunctionCall) pCFunctionCallEdge.getRawAST().orElseThrow());
      return new CFunctionCallEdge(
          rawStatement,
          loc,
          start,
          (CFunctionEntryNode) end,
          newAst,
          pCFunctionCallEdge.getSummaryEdge());
    }
    throw new AssertionError();
  }

  private CFAEdge cloneFunctionReturnEdge(
      final CFAEdge edge, final FileLocation loc, final CFANode start, final CFANode end) {
    if (edge instanceof CFunctionReturnEdge pCFunctionReturnEdge) {
      final var newEdge =
          (CFunctionSummaryEdge) cloneEdgeDirect(pCFunctionReturnEdge.getSummaryEdge());
      return new CFunctionReturnEdge(loc, (FunctionExitNode) start, end, newEdge);
    }
    throw new AssertionError(ONLY_C_SUPPORTED);
  }

  private CFAEdge cloneCallToReturnEdge(
      final CFAEdge edge,
      final String rawStatement,
      final FileLocation loc,
      final CFANode start,
      final CFANode end) {
    if (edge instanceof CFunctionSummaryEdge pCFunctionSummaryEdge) {
      final var newExpr = astCloner.cloneAst(pCFunctionSummaryEdge.getExpression());
      return new CFunctionSummaryEdge(
          rawStatement, loc, start, end, newExpr, pCFunctionSummaryEdge.getFunctionEntry());
    }
    throw new AssertionError(ONLY_C_SUPPORTED);
  }
}
