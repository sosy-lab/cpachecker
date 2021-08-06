// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.util.Map;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

/**
 * This class manages all predicates that accrue during a refinement. For that it stores them for
 * later use and offers methods that additionally help in managing those.
 */
public class TraceAbstractionPredicatesStorage {

  // TODO: merge/refactor functionality of this class with PredicatePrecision.
  // For now predicates other than function predicates are ignored, however, they also need to be
  // considered eventually.

  private final Multimap<String, AbstractionPredicate> functionPredicates;

  TraceAbstractionPredicatesStorage() {
    functionPredicates = MultimapBuilder.linkedHashKeys().arrayListValues().build();
  }

  public void addFunctionPredicates(Multimap<String, AbstractionPredicate> pFunctionPredicates) {
    addFunctionPredicates(pFunctionPredicates.entries());
  }

  private void addFunctionPredicates(
      Iterable<Map.Entry<String, AbstractionPredicate>> pFunctionPredicates) {
    Multimap<String, AbstractionPredicate> predicates =
        MultimapBuilder.linkedHashKeys().arrayListValues().build();
    for (Map.Entry<String, AbstractionPredicate> entry : pFunctionPredicates) {
      predicates.put(entry.getKey(), entry.getValue());
    }
    functionPredicates.putAll(predicates);
  }

  public ImmutableMultimap<String, AbstractionPredicate> getPredicates() {
    return ImmutableSetMultimap.copyOf(functionPredicates);
  }
}
