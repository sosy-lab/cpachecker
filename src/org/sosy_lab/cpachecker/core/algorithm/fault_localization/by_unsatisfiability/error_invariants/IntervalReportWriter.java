// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants;

import com.google.common.base.Splitter;
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
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.PotentialFix;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.RankInfo;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print.BooleanFormulaParser;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print.FormulaNode;

public class IntervalReportWriter extends FaultReportWriter {

  @Override
  public String toHtml(Fault pFault) {
    if (pFault instanceof Interval) {
      List<CFAEdge> edges =
          pFault.stream()
              .map(FaultContribution::correspondingEdge)
              .sorted(Comparator.comparingInt(l -> l.getFileLocation().getStartingLineInOrigin()))
              .collect(Collectors.toList());
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

    String header = "Interpolant <strong>" + index + "</strong>:<br>"
        + " <textarea readonly class=\"interval-scrollbox\">"
        + extractRelevantInformation(interval)
        + "</textarea><br>";
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

  /**
   * Extracts the fault-relevant information from the given formula. Since the original trace
   * formulas are often too detailed for a concise description of the fault, this method reduces the
   * displayed information to the relevant one.
   *
   * @param interval interval to extract information from
   * @return relevant information
   */
  private String extractRelevantInformation(Interval interval) {

    FormulaNode root = BooleanFormulaParser.parse(interval.getInvariant());
    List<FormulaNode> conjunctions = BooleanFormulaParser.toConjunctionArgs(root);
    List<String> helpfulFormulas = new ArrayList<>();

    for (FormulaNode f : conjunctions) {
      if (f.toString().contains("_ADDRESS_OF")) {
        List<String> findName = Splitter.on("__ADDRESS_OF_").splitToList(f.toString());
        if (findName.size() > 1) {
          List<String> extractName = Splitter.on("@").splitToList(findName.get(1));
          if (!extractName.isEmpty()) {
            helpfulFormulas.add("(values of " + extractName.get(0) + ")");
            continue;
          }
        }
      }
      helpfulFormulas.add(f.toString());
    }
    // return "<ul><li>"  + helpfulFormulas.stream().distinct().map(s -> s.replaceAll("@",
    // "")).collect(Collectors.joining(" </li><li> ")) + "</li></ul>";
    return helpfulFormulas.stream()
        .distinct()
        .map(s -> s.replaceAll("@", ""))
        .collect(Collectors.joining(" ∧ "));
  }
}
