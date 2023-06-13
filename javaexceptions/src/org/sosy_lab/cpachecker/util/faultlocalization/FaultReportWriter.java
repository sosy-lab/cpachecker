// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo.InfoType;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultReason;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.PotentialFix;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.RankInfo;

/**
 * Provides useful methods for converting objects in this package to a HTML format. Extend this
 * class and override the methods to create a different HTML-representation. Change the
 * FaultReportWriter in FaultLocalizationInfo by calling the method <code>replaceHtmlWriter</code>
 *
 * @see FaultLocalizationInfo#replaceHtmlWriter(FaultReportWriter)
 */
public class FaultReportWriter {

  protected Set<InfoType> hideTypes;

  public FaultReportWriter() {
    hideTypes = new HashSet<>();
  }

  public FaultReportWriter(InfoType... pHideTypes) {
    hideTypes = new HashSet<>();
    hideTypes(pHideTypes);
  }

  public void hideTypes(InfoType... types) {
    hideTypes.clear();
    hideTypes.addAll(Arrays.asList(types));
  }

  public String toHtml(FaultInfo info) {
    String description = info.getDescription();
    if (info.getType().equals(InfoType.RANK_INFO)) {
      double likelihood = info.getScore();
      String percent = "<strong>" + ((int) (likelihood * 10000)) / 100d + "%</strong>";
      return description + " (" + percent + ")";
    }
    return description;
  }

  public String toHtml(FaultContribution faultContribution) {
    return toHtml(
            faultContribution.getInfos(),
            Collections.singletonList(faultContribution.correspondingEdge()))
        + (faultContribution.getScore() > 0
            ? "<br><i>Score: " + (int) (faultContribution.getScore() * 100) + "</i>"
            : "");
  }

  public String toHtml(Fault fault) {
    // list of all edges in fault sorted by line number
    List<CFAEdge> edges =
        fault.stream()
            .map(FaultContribution::correspondingEdge)
            .sorted(Comparator.comparingInt(l -> l.getFileLocation().getStartingLineInOrigin()))
            .collect(ImmutableList.toImmutableList());
    return toHtml(fault.getInfos(), edges);
  }

  /**
   * Convert this object to a HTML string for the report.
   *
   * @param correspondingEdges the corresponding edges to the fault
   * @param infos the FaultInfos appended to a Fault(Contribution)
   * @return hmtl code of this instance
   */
  protected String toHtml(List<FaultInfo> infos, List<CFAEdge> correspondingEdges) {
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
      }
    }

    Map<Integer, String> distinctRelevantStatements = getDistinctStatements(correspondingEdges);
    String header =
        "Error suspected on line(s): <strong>"
            + listLineNumbersAndJoin(distinctRelevantStatements.keySet())
            + "</strong><br>";
    StringBuilder html = new StringBuilder();

    if (!distinctRelevantStatements.isEmpty()) {
      html.append(" Relevant lines:\n<ul class=\"fault-lines\">\n");
      distinctRelevantStatements.entrySet().stream()
          .sorted(Comparator.comparingInt(e -> e.getKey()))
          .forEach(
              e ->
                  html.append(
                      "<li>"
                          + "<span class=\"line-number\">"
                          + e.getKey()
                          + "</span>"
                          + "<span class=\"line-content\">"
                          + e.getValue()
                          + "</span>"
                          + "</li>"));
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

    if (!faultInfo.isEmpty() && !hideTypes.contains(InfoType.RANK_INFO)) {
      html.append(printList("The score is obtained by:", "", faultInfo, true)).append("<br>");
    }

    return header + "<br>" + html;
  }

  protected String printList(
      String headline, String htmlId, List<? extends FaultInfo> infos, boolean useOrderedList) {
    List<? extends FaultInfo> copy = ImmutableList.sortedCopyOf(infos);
    String listType = useOrderedList ? "ol" : "ul";
    String id = "";
    if (!htmlId.isEmpty()) {
      id = " id=\"" + htmlId + "\"";
    }
    StringBuilder out = new StringBuilder(headline + "<br><" + listType + id + ">");
    for (FaultInfo info : copy) {
      out.append("<li>").append(toHtml(info)).append("</li>");
    }
    out.append("</").append(listType).append(">");
    return out.toString();
  }

  protected Map<Integer, String> getDistinctStatements(List<CFAEdge> pEdges) {
    Map<Integer, String> statements = new HashMap<>();
    for (CFAEdge e : pEdges) {
      int codeLineNumber = e.getFileLocation().getStartingLineInOrigin();
      String description = e.getDescription();
      /*checkState(
      !statements.containsKey(codeLineNumber)
          || statements.get(codeLineNumber).equals(description));*/
      statements.merge(codeLineNumber, description, (s1, s2) -> s1 + ", " + s2);
    }
    return statements;
  }

  private String listLineNumbersAndJoin(Collection<Integer> lineNumbers) {
    List<String> sortedNumbers =
        lineNumbers.stream().sorted().map(String::valueOf).collect(ImmutableList.toImmutableList());

    if (sortedNumbers.size() <= 2) {
      return String.join(" and ", sortedNumbers);
    }
    int lastIndex = sortedNumbers.size() - 1;
    return String.join(", ", sortedNumbers.subList(0, lastIndex))
        + " and "
        + sortedNumbers.get(lastIndex);
  }
}
