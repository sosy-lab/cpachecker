// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class CombinePredicatePrecisionOperator implements CombinePrecisionOperator {

  private final FormulaManagerView fmgr;

  public CombinePredicatePrecisionOperator(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
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
    return PredicatePrecision.unionOf(precisions, count.keySet());
  }
}
