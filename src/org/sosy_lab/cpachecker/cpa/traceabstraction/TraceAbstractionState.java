// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

public class TraceAbstractionState implements AbstractState, Graphable, Serializable {

  private static final long serialVersionUID = -3454798281550882095L;

  static TraceAbstractionState createInitState() {
    return new TraceAbstractionState(ImmutableSet.of());
  }

  /** These are the predicates that hold in this state */
  private final ImmutableSetMultimap<String, AbstractionPredicate> functionPredicates;

  private TraceAbstractionState(Multimap<String, AbstractionPredicate> pFunctionPredicates) {
    this(pFunctionPredicates.entries());
  }

  private TraceAbstractionState(
      Iterable<Map.Entry<String, AbstractionPredicate>> pFunctionPredicates) {
    // TODO: code is duplicated from PredicatePrecision; this needs to be refactored accordingly
    Multimap<String, AbstractionPredicate> predMap =
        MultimapBuilder.treeKeys().arrayListValues().build();
    for (Map.Entry<String, AbstractionPredicate> entry : pFunctionPredicates) {
      predMap.put(entry.getKey(), entry.getValue());
    }
    functionPredicates = ImmutableSetMultimap.copyOf(predMap);
  }

  ImmutableSetMultimap<String, AbstractionPredicate> getFunctionPredicates() {
    return functionPredicates;
  }

  /**
   * Creates a new TraceAbstractionState that contains the union of predicates that already hold in
   * this state as well as the newly given predicates.
   */
  public TraceAbstractionState unionOf(
      Iterable<Map.Entry<String, AbstractionPredicate>> newPredicates) {
    if (Iterables.isEmpty(newPredicates)) {
      return this;
    }
    return new TraceAbstractionState(
        Iterables.concat(getFunctionPredicates().entries(), newPredicates));
  }

  @Override
  public int hashCode() {
    return Objects.hash(functionPredicates);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TraceAbstractionState)) {
      return false;
    }
    TraceAbstractionState other = (TraceAbstractionState) obj;
    return Objects.equals(functionPredicates, other.functionPredicates);
  }

  @Override
  public String toString() {
    if (functionPredicates.isEmpty()) {
      return "_empty_preds_";
    }

    return FluentIterable.from(functionPredicates.entries())
        .transform(x -> x.getKey() + "=" + x.getValue())
        .join(Joiner.on("; "));
  }

  @Override
  public String toDOTLabel() {
    // TODO: remove graphable interface eventually
    // (currently used for debugging)
    if (functionPredicates.isEmpty()) {
      return "_empty_state_";
    }

    return FluentIterable.from(functionPredicates.entries())
        .transform(x -> x.getKey() + "=" + x.getValue())
        .join(Joiner.on("; "));
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
}
