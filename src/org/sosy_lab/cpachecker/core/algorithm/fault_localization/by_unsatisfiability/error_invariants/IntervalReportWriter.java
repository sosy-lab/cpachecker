// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants.ErrorInvariantsAlgorithm.Interval;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultReportWriter;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo.InfoType;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultReason;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.PotentialFix;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.RankInfo;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaToCVisitor;

public class IntervalReportWriter extends FaultReportWriter {

  private final FormulaManagerView formulaManager;
  private final FormulaToCVisitor visitor;

  public IntervalReportWriter(FormulaManagerView pFormulaManager) {
    formulaManager = pFormulaManager;
    visitor = new FormulaToCVisitor(formulaManager);
  }

  @Override
  public String toHtml(Fault pFault) {
    if (pFault instanceof Interval) {
      List<CFAEdge> edges =
          pFault.stream()
              .map(FaultContribution::correspondingEdge)
              .sorted(Comparator.comparingInt(l -> l.getFileLocation().getStartingLineInOrigin()))
              .collect(ImmutableList.toImmutableList());
      return intervalToHtml((Interval) pFault, pFault.getInfos(), edges);
    } else {
      return super.toHtml(pFault);
    }
  }

  public String intervalToHtml(
      Interval interval, List<FaultInfo> infos, List<CFAEdge> correspondingEdges) {
    List<FaultReason> faultReasons = new ArrayList<>();
    List<RankInfo> faultInfo = new ArrayList<>();
    List<PotentialFix> faultFix = new ArrayList<>();

    // Sorted insert
    for (FaultInfo info : infos) {
      switch (info.getType()) {
        case FIX:
          faultFix.add((PotentialFix) info);
          break;
        case REASON:
          faultReasons.add((FaultReason) info);
          break;
        case RANK_INFO:
          faultInfo.add((RankInfo) info);
          break;
        default:
          throw new AssertionError("Unknown InfoType");
      }
    }
    // every second entry symbolizes an interval (i.e. index/2 equals the current number of
    // intervals)
    int index = interval.getIntendedIndex() / 2;

    formulaManager.visit(interval.getInvariant(), visitor);

    Map<Integer, String> distinctRelevantStatements = getDistinctStatements(correspondingEdges);
    String header =
        "Interpolant <strong>"
            + index
            + "</strong>:<br>"
            + " <textarea readonly class=\"interval-scrollbox\">"
            + visitor.getString()
            + "</textarea><br>";
    StringBuilder html = new StringBuilder();

    if (!distinctRelevantStatements.isEmpty()) {
      html.append(" Relevant lines:\n<ul class=\"fault-lines\">\n");
      distinctRelevantStatements.entrySet().stream()
          .sorted(Comparator.comparingInt(e -> e.getKey()))
          .forEach(
              e ->
                  html.append("<li>" + "<span class=\"line-number\">")
                      .append(e.getKey())
                      .append("</span>")
                      .append("<span class=\"line-content\">")
                      .append(e.getValue())
                      .append("</span>")
                      .append("</li>"));
      html.append("</ul>\n");
    } else {
      header = "Additional Information for:<br>" + header;
    }

    if (!faultReasons.isEmpty() && !hideTypes.contains(InfoType.REASON)) {
      html.append(
              printList(
                  "Detected <strong>"
                      + faultReasons.size()
                      + "</strong> possible reason"
                      + (faultReasons.size() == 1 ? ":" : "s:"),
                  "",
                  faultReasons,
                  true))
          .append("<br>");
    }

    if (!faultFix.isEmpty() && !hideTypes.contains(InfoType.FIX)) {
      html.append(
              printList(
                  "Found <strong>"
                      + faultFix.size()
                      + "</strong> possible bug-fix"
                      + (faultFix.size() == 1 ? ":" : "es:"),
                  "fix-list",
                  faultFix,
                  false))
          .append("<br>");
    }

    if (!faultInfo.isEmpty() && !hideTypes.contains(InfoType.RANK_INFO)) {
      html.append(printList("The score is obtained by:", "", faultInfo, true)).append("<br>");
    }

    return header + "<br>" + html;
  }
}
