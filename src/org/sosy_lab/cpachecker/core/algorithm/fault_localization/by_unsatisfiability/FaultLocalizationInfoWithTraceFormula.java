// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.JSON;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultScoring;

public class FaultLocalizationInfoWithTraceFormula extends FaultLocalizationInfo {

  private final TraceFormula traceFormula;

  /**
   * Fault localization algorithms will result in a set of sets of CFAEdges that are most likely to
   * fix a bug. Transforming it into a Set of Faults enables the possibility to attach reasons of
   * why this edge is in this set. After ranking the set of faults an instance of this class can be
   * created.
   *
   * <p>The class should be used to display information to the user.
   *
   * <p>Note that there is no need to create multiple instances of this object if more than one
   * ranking should be applied. FaultRankingUtils provides a method that concatenates multiple
   * rankings.
   *
   * <p>To see the result of FaultLocalizationInfo replace the CounterexampleInfo of the target
   * state by this or simply call {@link #apply()} on an instance of this class.
   *
   * @param pFaults set of faults obtained by a fault localization algorithm
   * @param pScoring how to calculate the scores of each fault
   * @param pParent the counterexample info of the target state
   */
  public FaultLocalizationInfoWithTraceFormula(
      Set<Fault> pFaults,
      FaultScoring pScoring,
      TraceFormula pTraceFormula,
      CounterexampleInfo pParent) {
    super(pFaults, pScoring, pParent);
    traceFormula = pTraceFormula;
  }

  public TraceFormula getTraceFormula() {
    return traceFormula;
  }

  @Override
  public void addSpecificInformationToHTMLReport(Writer writer) throws IOException {
    writer.write(",\n\"precondition\":");
    List<String> preconditionParts = new ArrayList<>();
    for (CFAEdge cfaEdge : traceFormula.getPrecondition().getEdgesForPrecondition()) {
      String input = cfaEdge.getCode().replaceAll(";", "");
      List<String> parts = Splitter.on(" ").limit(2).splitToList(input);
      assert !parts.isEmpty() : "Splitter split " + input + " into 0 parts.";
      if (parts.size() == 1) {
        preconditionParts.add(parts.get(0));
      } else {
        preconditionParts.add(parts.get(1));
      }
    }
    if (preconditionParts.isEmpty()) {
      preconditionParts.add("true");
    }
    JSON.writeJSONString(
        ImmutableMap.of("fl-precondition", Joiner.on(" && ").join(preconditionParts)), writer);
  }
}
