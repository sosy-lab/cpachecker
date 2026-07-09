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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class CombinePredicatePrecisionOperator implements CombinePrecisionOperator {

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;

  public CombinePredicatePrecisionOperator(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();
  }

  private boolean containsImportantVariable(
      AbstractionPredicate p, Set<String> importantVariables) {
    BooleanFormula symbolicAtom = p.getSymbolicAtom();
    if (bfmgr.isTrue(symbolicAtom) || bfmgr.isFalse(symbolicAtom)) {
      return true;
    }
    Set<String> containedVariables = fmgr.extractVariables(symbolicAtom).keySet();
    return !Collections.disjoint(importantVariables, containedVariables);
  }

  private <K> ImmutableListMultimap<K, AbstractionPredicate> toFilteredMultimap(
      Collection<Entry<K, AbstractionPredicate>> entries, Set<String> importantVariables) {
    return entries.stream()
        .filter(entry -> containsImportantVariable(entry.getValue(), importantVariables))
        .collect(ImmutableListMultimap.toImmutableListMultimap(Entry::getKey, Entry::getValue));
  }

  /** Create a new precision that is the union of all given precisions. */
  private Precision toFilteredUnion(
      Collection<Precision> precisions, Set<String> importantVariables) {
    if (precisions.isEmpty()) {
      return PredicatePrecision.empty();
    }
    if (precisions.size() == 1) {
      return Iterables.getOnlyElement(precisions);
    }

    PredicatePrecision union = PredicatePrecision.unionOf(precisions);

    return new PredicatePrecision(
        toFilteredMultimap(union.getLocationInstancePredicates().entries(), importantVariables),
        toFilteredMultimap(union.getLocalPredicates().entries(), importantVariables),
        toFilteredMultimap(union.getFunctionPredicates().entries(), importantVariables),
        from(union.getGlobalPredicates())
            .filter(p -> containsImportantVariable(p, importantVariables))
            .toSet());
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
    boolean isAtLeastOneVariableInAllPrecisions =
        count.values().stream().mapToInt(Integer::intValue).max().orElse(0) == precisions.size();
    if (isAtLeastOneVariableInAllPrecisions) {
      for (var entry : ImmutableList.copyOf(count.entrySet())) {
        String variableName = entry.getKey();
        int numberOfPrecisionsVariableOccursIn = entry.getValue();
        if (numberOfPrecisionsVariableOccursIn < precisions.size()) {
          count.remove(variableName);
        }
      }
    }
    Set<String> variablesInAllPrecisions = count.keySet();
    return toFilteredUnion(precisions, variablesInAllPrecisions);
  }
}
