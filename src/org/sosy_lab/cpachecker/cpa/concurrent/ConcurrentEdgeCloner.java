// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.function.UnaryOperator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
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

/**
 * Edge cloner for POR. Most methods delegate to {@link ConcurrentCfaCloner}, which clones the
 * entire CFA (nodes and edges) per thread ID. {@link #cloneSingleEdge} instead rebuilds one edge in
 * isolation, keeping its original endpoint nodes and optionally renaming every global-variable
 * access to a fresh name via a {@link GlobalAccessRenamer} ("concurrent SSA"); the result is not
 * wired into the CFA.
 */
public final class ConcurrentEdgeCloner {

  private ConcurrentEdgeCloner() {}

  /**
   * Supplies a fresh qualified name for each single access to a global variable. When such a
   * renamer is passed to the POR cloners, every occurrence of a global variable in a cloned AST is
   * renamed to the value returned by {@link #freshName}, turning each access into a distinct
   * symbol. Without a renamer, global variables keep their original names.
   */
  public interface GlobalAccessRenamer {

    /**
     * Returns the fresh qualified name to use for one access to the given global variable.
     *
     * @param pDeclaration the original declaration of the accessed global variable
     * @param pIsWrite whether the access writes the variable (otherwise it reads it)
     */
    String freshName(CVariableDeclaration pDeclaration, boolean pIsWrite);

    /**
     * Offers to replace a whole address-of expression (called with the ORIGINAL, un-cloned
     * expression before its operand is cloned), given a sub-cloner that clones an rvalue
     * sub-expression with the same renaming — so a compound operand such as {@code &p->field} or
     * {@code &a[i]} can have its pointer/index sub-parts cloned while the object base and byte
     * offset are resolved separately and combined into a single address. Returning null keeps the
     * default cloning.
     */
    @SuppressWarnings("unused")
    default @Nullable CExpression replaceAddressOf(
        CUnaryExpression pOriginalAddressOf, UnaryOperator<CExpression> pSubCloner) {
      return null;
    }

    /**
     * Whether accesses to this <b>local</b> variable should be routed through {@link #freshName}
     * (the same path globals take) instead of the plain per-thread rename. A renamer that models an
     * aliasing regime returns {@code true} for an address-taken local, so that a pointer — or
     * another thread the local's address escaped to — reaches the same memory as the local's own
     * accesses. Globals always go through {@link #freshName}; this only concerns locals.
     */
    @SuppressWarnings("unused")
    default boolean treatsLocalAsRegion(CVariableDeclaration pLocal) {
      return false;
    }

    /**
     * Offers to replace one aliased memory access (pointer dereference, array subscript, or field
     * reference), given the ORIGINAL access expression and a sub-cloner that clones an rvalue
     * sub-expression with the same renaming (so the renamer can decide, per sub-part, whether to
     * clone it or treat it as an object base/offset). Returning null keeps the default cloning.
     */
    @SuppressWarnings("unused")
    default @Nullable CIdExpression replaceAliasedAccess(
        CExpression pOriginalAccess, boolean pIsWrite, UnaryOperator<CExpression> pSubCloner) {
      return null;
    }

    /** Thrown by renamer callbacks for accesses the caller cannot support. */
    final class UnsupportedAccessException extends RuntimeException {
      private static final long serialVersionUID = 1L;

      public UnsupportedAccessException(String pMessage) {
        super(pMessage);
      }
    }
  }

  /**
   * Returns the cloned edge for the given original CFA edge and thread ID. The first call for a
   * given thread ID triggers a full CFA clone. If the edge is already a cloned edge, it is returned
   * as-is.
   */
  static CFAEdge clone(final CFAEdge pCFAEdge, final int threadId, final CFA pCfa) {
    ConcurrentCfaCloner cfaCloner = ConcurrentCfaCloner.getOrCreate(threadId, pCfa);
    return cfaCloner.getClonedEdge(pCFAEdge);
  }

  /**
   * Returns the cloned CFA node corresponding to the given node for the given thread ID. If the
   * node is already a cloned node, it is returned as-is.
   */
  static CFANode getClonedNode(final CFANode pNode, final int threadId, final CFA pCfa) {
    ConcurrentCfaCloner cfaCloner = ConcurrentCfaCloner.getOrCreate(threadId, pCfa);
    return cfaCloner.getClonedNode(pNode);
  }

  /**
   * Rebuilds a single edge in isolation: the resulting edge has cloned/renamed ASTs but keeps the
   * ORIGINAL predecessor and successor {@link CFANode}s of {@code pEdge}. The result is not wired
   * into the CFA in any way (no {@code addLeavingEdge}/{@code addEnteringEdge} calls) and is meant
   * to be fed directly to a path-formula converter.
   *
   * <p>Local variables and parameters are renamed with the usual {@code T{pThreadId}_} prefix.
   * Every single access to a global variable is additionally renamed via {@code pRenamer}, turning
   * it into a distinct symbol ("concurrent SSA").
   */
  public static CFAEdge cloneSingleEdge(
      final CFAEdge pEdge, final int pThreadId, final GlobalAccessRenamer pRenamer) {
    checkNotNull(pRenamer);

    if (pEdge instanceof BlankEdge) {
      return pEdge;
    }

    ConcurrentAstCloner renamerCloner = new ConcurrentAstCloner(pThreadId, pRenamer);

    return switch (pEdge) {
      case CAssumeEdge cEdge -> cloneSingleAssumeEdge(cEdge, renamerCloner);
      case CFunctionSummaryStatementEdge cEdge ->
          cloneSingleSummaryStatementEdge(cEdge, renamerCloner);
      case CStatementEdge cEdge -> cloneSingleStatementEdge(cEdge, renamerCloner);
      case CDeclarationEdge cEdge -> cloneSingleDeclarationEdge(cEdge, renamerCloner);
      case CReturnStatementEdge cEdge -> cloneSingleReturnStatementEdge(cEdge, renamerCloner);
      case CFunctionCallEdge cEdge -> cloneSingleFunctionCallEdge(cEdge, pThreadId, renamerCloner);
      case CFunctionReturnEdge cEdge ->
          cloneSingleFunctionReturnEdge(cEdge, pThreadId, renamerCloner);
      case CFunctionSummaryEdge cEdge -> cloneSingleFunctionSummaryEdge(cEdge, renamerCloner);
      default -> throw new AssertionError("unhandled edge " + pEdge + " of " + pEdge.getClass());
    };
  }

  private static CFAEdge cloneSingleAssumeEdge(CAssumeEdge pEdge, ConcurrentAstCloner pCloner) {
    return new CAssumeEdge(
        pEdge.getRawStatement(),
        pEdge.getFileLocation(),
        pEdge.getPredecessor(),
        pEdge.getSuccessor(),
        pCloner.cloneAstRightSide(pEdge.getExpression()),
        pEdge.getTruthAssumption(),
        pEdge.isSwapped(),
        pEdge.isArtificialIntermediate());
  }

  private static CFAEdge cloneSingleSummaryStatementEdge(
      CFunctionSummaryStatementEdge pEdge, ConcurrentAstCloner pCloner) {
    return new CFunctionSummaryStatementEdge(
        pEdge.getRawStatement(),
        pCloner.cloneAst(pEdge.getStatement()),
        pEdge.getFileLocation(),
        pEdge.getPredecessor(),
        pEdge.getSuccessor(),
        pCloner.cloneAst(pEdge.getFunctionCall()),
        pEdge.getFunctionName());
  }

  private static CFAEdge cloneSingleStatementEdge(
      CStatementEdge pEdge, ConcurrentAstCloner pCloner) {
    return new CStatementEdge(
        pEdge.getRawStatement(),
        pCloner.cloneAst(pEdge.getStatement()),
        pEdge.getFileLocation(),
        pEdge.getPredecessor(),
        pEdge.getSuccessor());
  }

  private static CFAEdge cloneSingleDeclarationEdge(
      CDeclarationEdge pEdge, ConcurrentAstCloner pCloner) {
    return new CDeclarationEdge(
        pEdge.getRawStatement(),
        pEdge.getFileLocation(),
        pEdge.getPredecessor(),
        pEdge.getSuccessor(),
        pCloner.cloneAst(pEdge.getDeclaration()));
  }

  private static CFAEdge cloneSingleReturnStatementEdge(
      CReturnStatementEdge pEdge, ConcurrentAstCloner pCloner) {
    CFANode succ = pEdge.getSuccessor();
    if (!(succ instanceof FunctionExitNode exitNode)) {
      throw new AssertionError("Expected FunctionExitNode successor: " + succ);
    }
    return new CReturnStatementEdge(
        pEdge.getRawStatement(),
        pCloner.cloneAst(pEdge.getReturnStatement()),
        pEdge.getFileLocation(),
        pEdge.getPredecessor(),
        exitNode);
  }

  /**
   * The call edge's formula only encodes parameter passing: the arguments are reads, renamed via
   * {@code pRenamerCloner}, but a possible assignment left-hand side is not written on this edge
   * (that happens on the return edge instead), so it only gets the plain thread-ID renaming from a
   * fresh, renamer-less cloner.
   */
  private static CFAEdge cloneSingleFunctionCallEdge(
      CFunctionCallEdge pEdge, int pThreadId, ConcurrentAstCloner pRenamerCloner) {
    CFANode succ = pEdge.getSuccessor();
    if (!(succ instanceof CFunctionEntryNode entryNode)) {
      throw new AssertionError("Expected CFunctionEntryNode successor: " + succ);
    }
    ConcurrentAstCloner plainCloner = new ConcurrentAstCloner(pThreadId);
    CFunctionCall clonedCall =
        cloneFunctionCallStatement(pEdge.getFunctionCall(), pRenamerCloner, plainCloner);

    // The path-formula converter takes the callee's formal parameters (and return variable) from
    // the entry node, so it must see the same T-renamed declarations as the callee's body edges.
    CFunctionEntryNode clonedEntry = cloneEntryForFormulas(entryNode, plainCloner);
    CFunctionSummaryEdge origSummary = pEdge.getSummaryEdge();
    CFunctionSummaryEdge clonedSummary =
        new CFunctionSummaryEdge(
            origSummary.getRawStatement(),
            origSummary.getFileLocation(),
            origSummary.getPredecessor(),
            origSummary.getSuccessor(),
            clonedCall,
            clonedEntry);

    return new CFunctionCallEdge(
        pEdge.getRawStatement(),
        pEdge.getFileLocation(),
        pEdge.getPredecessor(),
        clonedEntry,
        clonedCall,
        clonedSummary);
  }

  /**
   * Throwaway clone of a function-entry node with T-renamed parameter and return-variable
   * declarations, matching the renaming of the callee's body edges; only ever read by the
   * path-formula converter and never wired into the CFA.
   */
  private static CFunctionEntryNode cloneEntryForFormulas(
      CFunctionEntryNode pEntry, ConcurrentAstCloner pCloner) {
    CFunctionDeclaration clonedDeclaration = pCloner.cloneAst(pEntry.getFunctionDefinition());
    Optional<CVariableDeclaration> clonedReturnVariable =
        pEntry.getReturnVariable().map(pCloner::cloneAstLeftSide);
    return new CFunctionEntryNode(
        pEntry.getFileLocation(), clonedDeclaration, null, clonedReturnVariable);
  }

  /**
   * Mirror image of {@link #cloneSingleFunctionCallEdge}: the return edge's formula only encodes
   * {@code lhs := retval}, so the assignment left-hand side is the write, renamed via {@code
   * pRenamerCloner}, while the call/arguments part (not evaluated again on this edge) only gets the
   * plain thread-ID renaming.
   */
  private static CFAEdge cloneSingleFunctionReturnEdge(
      CFunctionReturnEdge pEdge, int pThreadId, ConcurrentAstCloner pRenamerCloner) {
    CFANode pred = pEdge.getPredecessor();
    if (!(pred instanceof FunctionExitNode exitNode)) {
      throw new AssertionError("Expected FunctionExitNode predecessor: " + pred);
    }
    ConcurrentAstCloner plainCloner = new ConcurrentAstCloner(pThreadId);
    CFunctionSummaryEdge origSummary = pEdge.getSummaryEdge();
    CFunctionCall clonedCall =
        cloneFunctionCallStatement(origSummary.getExpression(), plainCloner, pRenamerCloner);

    CFunctionSummaryEdge clonedSummary =
        new CFunctionSummaryEdge(
            origSummary.getRawStatement(),
            origSummary.getFileLocation(),
            origSummary.getPredecessor(),
            origSummary.getSuccessor(),
            clonedCall,
            cloneEntryForFormulas(origSummary.getFunctionEntry(), plainCloner));

    return new CFunctionReturnEdge(
        pEdge.getFileLocation(), exitNode, pEdge.getSuccessor(), clonedSummary);
  }

  /**
   * A stand-alone call-to-return edge (as opposed to one paired with a {@link CFunctionCallEdge} or
   * {@link CFunctionReturnEdge} above) fully encodes {@code lhs := f(args)} by itself, so both the
   * arguments and the assignment left-hand side are renamed via {@code pRenamerCloner}.
   */
  private static CFAEdge cloneSingleFunctionSummaryEdge(
      CFunctionSummaryEdge pEdge, ConcurrentAstCloner pRenamerCloner) {
    CFunctionCall clonedCall =
        cloneFunctionCallStatement(pEdge.getExpression(), pRenamerCloner, pRenamerCloner);
    return new CFunctionSummaryEdge(
        pEdge.getRawStatement(),
        pEdge.getFileLocation(),
        pEdge.getPredecessor(),
        pEdge.getSuccessor(),
        clonedCall,
        pEdge.getFunctionEntry());
  }

  /**
   * Clones a function-call statement using two (possibly identical) cloners: {@code pArgsCloner}
   * for the call's function-name and argument expressions (always reads), and {@code pLhsCloner}
   * for a possible assignment left-hand side (a write).
   */
  private static CFunctionCall cloneFunctionCallStatement(
      CFunctionCall pCall, ConcurrentAstCloner pArgsCloner, ConcurrentAstCloner pLhsCloner) {
    CFunctionCallExpression clonedExpr =
        pArgsCloner.cloneAstRightSide(pCall.getFunctionCallExpression());
    if (pCall instanceof CFunctionCallAssignmentStatement assignment) {
      return new CFunctionCallAssignmentStatement(
          assignment.getFileLocation(),
          pLhsCloner.cloneAstLeftSide(assignment.getLeftHandSide()),
          clonedExpr);
    } else if (pCall instanceof CFunctionCallStatement stmt) {
      return new CFunctionCallStatement(stmt.getFileLocation(), clonedExpr);
    }
    throw new AssertionError("unhandled CFunctionCall " + pCall + " of " + pCall.getClass());
  }
}
