// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
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
 * Balanced calls are replaced by recursion guards, not erased: backwards return transfer checks the
 * number of frames of the called function before pushing, and the matching call then pops that
 * frame. The guard retains that check while repeated balanced calls need only one guard.
 *
 * <p>Equality is deliberately a sufficient, not a necessary, test for equal transformers. No
 * bounded history or comparison of just the current stack may replace it. CFA edge identity is
 * meaningful within one analysis; effects are reset at block entries, not sent between workers.
 */
final class DssCallstackEffect {

  static final DssCallstackEffect EMPTY = new DssCallstackEffect(PersistentLinkedList.of());

  private sealed interface Operation permits Edge, Guards {}

  private record Edge(CFAEdge edge) implements Operation {
    @Override
    public boolean equals(Object pOther) {
      // CFAEdge.equals ignores the edge's statement and type.
      return pOther instanceof Edge other && edge == other.edge;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(edge);
    }
  }

  /**
   * A balanced section leaves the input stack unchanged. For each function, remember the maximum
   * number of its frames temporarily pushed during backwards replay. Counts for other functions do
   * not affect CallstackTransferRelation.hasRecursion. Samples are only used to execute the
   * existing recursion checks (including skip/unsupported handling), not for equality.
   */
  private static final class Guards implements Operation {
    private final ImmutableMap<String, Integer> depths;
    private final ImmutableMap<String, FunctionReturnEdge> samples;

    Guards(Map<String, Integer> pDepths, Map<String, FunctionReturnEdge> pSamples) {
      depths = ImmutableMap.copyOf(pDepths);
      samples = ImmutableMap.copyOf(pSamples);
    }

    @Override
    public boolean equals(Object pOther) {
      return pOther instanceof Guards other && depths.equals(other.depths);
    }

    @Override
    public int hashCode() {
      return depths.hashCode();
    }
  }

  private final PersistentLinkedList<Operation> reversedEdges;

  private DssCallstackEffect(PersistentLinkedList<Operation> pReversedEdges) {
    reversedEdges = pReversedEdges;
  }

  DssCallstackEffect append(CFAEdge pEdge) {
    if (pEdge instanceof FunctionReturnEdge returnEdge) {
      PersistentLinkedList<Operation> rest = reversedEdges;
      Map<String, Integer> depths = new LinkedHashMap<>();
      Map<String, FunctionReturnEdge> samples = new LinkedHashMap<>();
      if (!rest.isEmpty() && rest.head() instanceof Guards inner) {
        depths.putAll(inner.depths);
        samples.putAll(inner.samples);
        rest = rest.tail();
      }
      if (!rest.isEmpty()
          && rest.head() instanceof Edge recorded
          && recorded.edge() instanceof FunctionCallEdge call
          && call.getSummaryEdge() == returnEdge.getSummaryEdge()
          && call.getPredecessor().getLeavingEdges().contains(call)) {
        String function = call.getSuccessor().getFunctionName();
        depths.merge(function, 1, Integer::sum);
        samples.put(function, returnEdge);
        rest = rest.tail();
        // Sequential balanced sections conjoin guards, so only the larger depth is relevant.
        if (!rest.isEmpty() && rest.head() instanceof Guards previous) {
          previous.depths.forEach((name, depth) -> depths.merge(name, depth, Math::max));
          previous.samples.forEach(samples::putIfAbsent);
          rest = rest.tail();
        }
        return new DssCallstackEffect(rest.with(new Guards(depths, samples)));
      }
    }
    if (pEdge instanceof FunctionCallEdge
        || pEdge instanceof FunctionReturnEdge
        || pEdge instanceof CFunctionSummaryStatementEdge
        || (pEdge instanceof AStatementEdge statement
            && statement.getStatement() instanceof AFunctionCall)) {
      return new DssCallstackEffect(reversedEdges.with(new Edge(pEdge)));
    }
    return this;
  }

  boolean accepts(
      CallstackState pAtBlockEnd,
      CallstackTransferRelationBackwards pBackwards,
      Precision pPrecision)
      throws CPATransferException {
    AbstractState current = DssCallstackState.unwrap(pAtBlockEnd);
    for (Operation operation : reversedEdges) {
      if (operation instanceof Edge edge) {
        Collection<? extends AbstractState> predecessors =
            pBackwards.getAbstractSuccessorsForEdge(current, pPrecision, edge.edge());
        if (predecessors.isEmpty()) {
          return false;
        }
        current = Iterables.getOnlyElement(predecessors);
      } else if (operation instanceof Guards guards) {
        for (Map.Entry<String, Integer> guard : guards.depths.entrySet()) {
          AbstractState temporary = current;
          for (int depth = 0; depth < guard.getValue(); depth++) {
            Collection<? extends AbstractState> predecessors =
                pBackwards.getAbstractSuccessorsForEdge(
                    temporary, pPrecision, guards.samples.get(guard.getKey()));
            if (predecessors.isEmpty()) {
              return false;
            }
            temporary = Iterables.getOnlyElement(predecessors);
          }
        }
      }
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
