// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class CombinePredicatePrecisionOperator implements CombinePrecisionOperator {

  private final FormulaManagerView fmgr;

  public CombinePredicatePrecisionOperator(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  private boolean isRequired(AbstractionPredicate p, Set<String> importantVariables) {
    if (fmgr.getBooleanFormulaManager().isTrue(p.getSymbolicAtom())
        || fmgr.getBooleanFormulaManager().isFalse(p.getSymbolicAtom())) {
      return true;
    }
    Set<String> containedVariables = fmgr.extractVariables(p.getSymbolicAtom()).keySet();
    return !Sets.intersection(importantVariables, containedVariables).isEmpty();
  }

  private <K> ImmutableListMultimap<K, AbstractionPredicate> toMultimap(
      Collection<Entry<K, AbstractionPredicate>> entries, Set<String> importantVariables) {
    return entries.stream()
        .filter(entry -> isRequired(entry.getValue(), importantVariables))
        .collect(ImmutableListMultimap.toImmutableListMultimap(Entry::getKey, Entry::getValue));
  }

  /** Create a new precision that is the union of all given precisions. */
  private Precision unionOf(Collection<Precision> precisions, Set<String> removeOthers) {
    if (precisions.isEmpty()) {
      return PredicatePrecision.empty();
    }
    if (precisions.size() == 1) {
      return Iterables.getOnlyElement(precisions);
    }

    PredicatePrecision union = PredicatePrecision.unionOf(precisions);

    return new PredicatePrecision(
        toMultimap(union.getLocationInstancePredicates().entries(), removeOthers),
        toMultimap(union.getLocalPredicates().entries(), removeOthers),
        toMultimap(union.getFunctionPredicates().entries(), removeOthers),
        from(union.getGlobalPredicates()).filter(p -> isRequired(p, removeOthers)).toSet());
  }

  @Override
  public Precision combine(Collection<Precision> precisions) throws InterruptedException {
    Preconditions.checkArgument(precisions.stream().allMatch(PredicatePrecision.class::isInstance));
    Preconditions.checkArgument(
        !precisions.isEmpty(), "Cannot combine an empty collection of precisions");
    Map<String, Integer> count = new HashMap<>();
    for (Precision precision : precisions) {
      for (String variable :
          Precisions.extractPrecisionByType(precision, PredicatePrecision.class)
              .getVariables(fmgr)) {
        count.merge(variable, 1, Integer::sum);
      }
    }
    if (count.values().stream().mapToInt(Integer::intValue).max().orElse(0) == precisions.size()) {
      for (Entry<String, Integer> stringIntegerEntry : ImmutableList.copyOf(count.entrySet())) {
        if (stringIntegerEntry.getValue() < precisions.size()) {
          count.remove(stringIntegerEntry.getKey());
        }
      }
    }
    return unionOf(precisions, count.keySet());
  }
}
