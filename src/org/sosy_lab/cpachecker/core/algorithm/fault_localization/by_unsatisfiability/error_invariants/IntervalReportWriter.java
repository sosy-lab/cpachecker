// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants.ErrorInvariantsAlgorithm.Interval;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultReportWriter;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo.InfoType;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultReason;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.Hint;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.PotentialFix;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.RankInfo;

public class IntervalReportWriter extends FaultReportWriter {

  @Override
  public String toHtml(Fault pFault) {
    if (pFault instanceof Interval) {
      List<CFAEdge> edges =
          pFault.stream()
              .map(FaultContribution::correspondingEdge)
              .sorted(Comparator.comparingInt(l -> l.getFileLocation().getStartingLineInOrigin()))
              .collect(Collectors.toList());
      return intervalToHtml(pFault.getInfos(), edges);
    } else {
      return super.toHtml(pFault);
    }
  }

  public String intervalToHtml(List<FaultInfo> infos, List<CFAEdge> correspondingEdges) {
    List<FaultReason> faultReasons = new ArrayList<>();
    List<RankInfo> faultInfo = new ArrayList<>();
    List<PotentialFix> faultFix = new ArrayList<>();
    List<Hint> faultHint = new ArrayList<>();

    // Sorted insert
    for (FaultInfo info : infos) {
      switch (info.getType()) {
        case FIX:
          faultFix.add((PotentialFix) info);
          break;
        case HINT:
          faultHint.add((Hint) info);
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

    String header =
        "Interpolant describing line(s): <strong>"
            + listDistinctLineNumbersAndJoin(correspondingEdges)
            + "</strong><br>";
    StringBuilder html = new StringBuilder();

    if (!correspondingEdges.isEmpty()) {
      html.append(" Relevant lines:\n<ul class=\"fault-lines\">\n");
      correspondingEdges.stream()
          .sorted(Comparator.comparingInt(e -> e.getFileLocation().getStartingLineInOrigin()))
          .forEach(
              e ->
                  html.append("<li>" + "<span class=\"line-number\">")
                      .append(e.getFileLocation().getStartingLineInOrigin())
                      .append("</span>")
                      .append("<span class=\"line-content\">")
                      .append(e.getDescription())
                      .append("</span>")
                      .append("</li>"));
      html.append("</ul>\n");
    } else {
      header = "Additional Information";
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

    if (!faultHint.isEmpty() && !hideTypes.contains(InfoType.HINT)) {
      String headline = faultHint.size() == 1 ? "hint is available:" : "hints are available:";
      html.append(
              printList(
                  "<strong>" + faultHint.size() + "</strong> " + headline,
                  "hint-list",
                  faultHint,
                  false))
          .append("<br>");
    }

    if (!faultInfo.isEmpty() && !hideTypes.contains(InfoType.RANK_INFO)) {
      html.append(printList("The score is obtained by:", "", faultInfo, true)).append("<br>");
    }

    return header + "<br>" + html;
  }
}
