// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Objects;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * The effect of a block prefix on backwards callstack transfer.
 *
 * <p>Unlike a path, this value ignores edges on which backwards callstack transfer is the identity.
 * Equal values can therefore accompany a disjunction of predicate path formulas. Calls, returns,
 * summary statements, and statements that can report unsupported calls remain in the effect.
 *
 * <p>In a program without recursion, a call that the block entered itself and the matching return
 * are the identity for backwards transfer as well: the return pushes the callee with the call node
 * of the call site, and the call pops it again, so the only thing the pair can do is report a
 * recursion. {@link #append(CFAEdge, boolean)} cancels such pairs if asked to, and it also leaves
 * out statements that call functions, which the forward transfer already checks for unsupported
 * functions. Otherwise paths that call different functions on the way to the same location would
 * keep different effects, and the reached set, which is partitioned by the callstack state, would
 * never merge them.
 *
 * <p>Equality is deliberately a sufficient, not a necessary, test for equal transformers. No
 * bounded history or comparison of just the current stack may replace it. CFA edge identity is
 * meaningful within one analysis; effects are reset at block entries, not sent between workers.
 */
final class DssCallstackEffect {

  static final DssCallstackEffect EMPTY = new DssCallstackEffect(PersistentLinkedList.of());

  private final PersistentLinkedList<CFAEdge> reversedEdges;

  private DssCallstackEffect(PersistentLinkedList<CFAEdge> pReversedEdges) {
    reversedEdges = pReversedEdges;
  }

  DssCallstackEffect append(CFAEdge pEdge) {
    return append(pEdge, false);
  }

  /**
   * The effect of this prefix followed by {@code pEdge}.
   *
   * @param pCancelMatchedCalls whether a return may cancel the call it matches and statements
   *     calling functions may be left out, which is only allowed in a program without recursion
   */
  DssCallstackEffect append(CFAEdge pEdge, boolean pCancelMatchedCalls) {
    if (pCancelMatchedCalls) {
      if (pEdge instanceof FunctionReturnEdge returnEdge
          && !reversedEdges.isEmpty()
          && reversedEdges.head() instanceof FunctionCallEdge callEdge
          && callEdge.getSummaryEdge().equals(returnEdge.getSummaryEdge())) {
        return new DssCallstackEffect(reversedEdges.tail());
      }
      if (pEdge instanceof AStatementEdge && !(pEdge instanceof CFunctionSummaryStatementEdge)) {
        return this;
      }
    }
    if (pEdge instanceof FunctionCallEdge
        || pEdge instanceof FunctionReturnEdge
        || pEdge instanceof CFunctionSummaryStatementEdge
        || (pEdge instanceof AStatementEdge statement
            && statement.getStatement() instanceof AFunctionCall)) {
      return new DssCallstackEffect(reversedEdges.with(pEdge));
    }
    return this;
  }

  boolean accepts(
      CallstackState pAtBlockEnd,
      CallstackTransferRelationBackwards pBackwards,
      Precision pPrecision)
      throws CPATransferException {
    AbstractState current = DssCallstackState.unwrap(pAtBlockEnd);
    for (CFAEdge edge : reversedEdges) {
      Collection<? extends AbstractState> predecessors =
          pBackwards.getAbstractSuccessorsForEdge(current, pPrecision, edge);
      if (predecessors.isEmpty()) {
        return false;
      }
      current = Iterables.getOnlyElement(predecessors);
    }
    return true;
  }

  @Override
  public boolean equals(Object pOther) {
    return this == pOther
        || (pOther instanceof DssCallstackEffect other
            && reversedEdges.equals(other.reversedEdges));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(reversedEdges);
  }
}
