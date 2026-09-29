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
import org.sosy_lab.common.collect.PersistentList;
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
 * summary statements, and statements that can report unsupported calls remain in the effect. In
 * particular, cancelling a call and a return would lose the backwards recursion check.
 *
 * <p>Equality is deliberately a sufficient, not a necessary, test for equal transformers. No
 * bounded history or comparison of just the current stack may replace it. CFA edge identity is
 * meaningful within one analysis; effects are reset at block entries, not sent between workers.
 */
final class DssCallstackEffect {

  static final DssCallstackEffect EMPTY = new DssCallstackEffect(PersistentLinkedList.of());

  private final PersistentList<CFAEdge> reversedEdges;

  private DssCallstackEffect(PersistentList<CFAEdge> pReversedEdges) {
    reversedEdges = pReversedEdges;
  }

  DssCallstackEffect append(CFAEdge pEdge) {
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
