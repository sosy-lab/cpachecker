// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultScoring;

public class FaultLocalizationInfoWithTraceFormula extends FaultLocalizationInfo {

  private final TraceFormula traceFormula;

  /**
   * @param pFaults set of faults obtained by a fault localization algorithm
   * @param pScoring how to calculate the scores of each fault
   * @param pTraceFormula calculated trace formula for the counterexample {@code pParent}
   * @param pParent the counterexample info of the target state
   */
  public FaultLocalizationInfoWithTraceFormula(
      Set<Fault> pFaults,
      FaultScoring pScoring,
      TraceFormula pTraceFormula,
      CounterexampleInfo pParent,
      boolean pSortIntended) {
    super(correctlySortFaults(pFaults, pScoring, pSortIntended), pParent);
    traceFormula = pTraceFormula;
  }

  private static List<Fault> correctlySortFaults(
      Set<Fault> pFaults, FaultScoring pScoring, boolean pSortIntended) {
    List<Fault> ranked = FaultRankingUtils.rank(pScoring, pFaults);
    if (pSortIntended) {
      return ImmutableList.sortedCopyOf(Comparator.comparingInt(Fault::getIntendedIndex), pFaults);
    }
    return ranked;
  }

  public TraceFormula getTraceFormula() {
    return traceFormula;
  }
}
