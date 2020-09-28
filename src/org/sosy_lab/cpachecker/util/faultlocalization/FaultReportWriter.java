// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo.InfoType;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultReason;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.Hint;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.PotentialFix;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.RankInfo;

/**
 * Provides useful methods for converting objects in this package to a HTML format.
 * Extend this class and override the methods to create a different HTML-representation.
 * Change the FaultReportWriter in FaultLocalizationInfo by calling the method <code>replaceHtmlWriter</code>
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

  public void hideTypes(InfoType... types){
    hideTypes.clear();
    hideTypes.addAll(Arrays.asList(types));
  }

  public String toHtml(FaultInfo info) {
    String description = info.getDescription();
    if(info.getType().equals(InfoType.RANK_INFO)){
      double likelihood = info.getScore();
      String percent = "<strong>" + ((int) (likelihood * 10000)) / 100d + "%</strong>";
      return description + " (" + percent + ")";
    }
    return description;
  }

  public String toHtml(FaultContribution faultContribution) {
    return toHtml(faultContribution.getInfos(),
        Collections.singletonList(faultContribution.correspondingEdge())) +
        (faultContribution.getScore() > 0 ? "<br><i>Score: " + (int)(faultContribution.getScore()*100)+"</i>" : "");
  }

  public String toHtml(Fault fault) {
    // list of all edges in fault sorted by line number
    List<CFAEdge> edges = fault
        .stream()
        .map(FaultContribution::correspondingEdge)
        .sorted(Comparator.comparingInt(l -> l.getFileLocation().getStartingLineInOrigin()))
        .collect(Collectors.toList());
    return toHtml(fault.getInfos(), edges);
  }

  /**
   * Convert this object to a HTML string for the report.
   * @param correspondingEdges the corresponding edges to the fault
   * @param infos the FaultInfos appended to a Fault(Contribution)
   * @return hmtl code of this instance
   */
  protected String toHtml(List<FaultInfo> infos, List<CFAEdge> correspondingEdges){
    List<FaultReason> faultReasons = new ArrayList<>();
    List<RankInfo> faultInfo = new ArrayList<>();
    List<PotentialFix> faultFix = new ArrayList<>();
    List<Hint> faultHint = new ArrayList<>();

    //Sorted insert
    for (FaultInfo info : infos) {
      switch(info.getType()){
        case FIX:
          faultFix.add((PotentialFix) info);
          break;
        case HINT:
          faultHint.add((Hint)info);
          break;
        case REASON:
          faultReasons.add((FaultReason) info);
          break;
        case RANK_INFO:
          faultInfo.add((RankInfo) info);
          break;
      }
    }

    String header = "Error suspected on line(s): <strong>" + listDistinctLineNumbersAndJoin(correspondingEdges)
        + "</strong><br>";
    StringBuilder html = new StringBuilder();

    if (!correspondingEdges.isEmpty()) {
      html.append(" Relevant lines:\n<ul class=\"fault-lines\">\n");
      correspondingEdges.stream()
          .sorted(Comparator.comparingInt(e -> e.getFileLocation().getStartingLineInOrigin()))
          .forEach(
              e ->
                  html.append(
                      "<li>"
                          + "<span class=\"line-number\">"
                          + e.getFileLocation().getStartingLineInOrigin()
                          + "</span>"
                          + "<span class=\"line-content\">"
                          + e.getDescription()
                          + "</span>"
                          + "</li>"));
      html.append("</ul>\n");
    } else {
      header = "Additional Information";
    }

    if (!faultReasons.isEmpty() && !hideTypes.contains(InfoType.REASON)) {
      html.append(printList("Detected <strong>" +
              faultReasons.size() + "</strong> possible reason" + (faultReasons.size() == 1? ":":"s:"), "",
          faultReasons, true))
          .append("<br>");
    }

    if (!faultFix.isEmpty() && !hideTypes.contains(InfoType.FIX)) {
      html.append(printList("Found <strong>" + faultFix.size() + "</strong> possible bug-fix" + (faultFix.size() == 1?":":"es:"), "fix-list",
          faultFix, false))
          .append("<br>");
    }

    if (!faultHint.isEmpty() && !hideTypes.contains(InfoType.HINT)) {
      String headline = faultHint.size() == 1? "hint is available:" : "hints are available:";
      html.append(
          printList(
              "<strong>" + faultHint.size() + "</strong> " + headline,
              "hint-list",
              faultHint,
              false))
          .append("<br>");
    }

    if (!faultInfo.isEmpty()  && !hideTypes.contains(InfoType.RANK_INFO)) {
      html.append(printList("The score is obtained by:", "", faultInfo, true))
          .append("<br>");
    }

    return header + "<br>" + html;
  }

  protected String printList(
      String headline,
      String htmlId,
      List<? extends FaultInfo> infos,
      boolean useOrderedList){
    List<? extends FaultInfo> copy = new ArrayList<>(infos);
    Collections.sort(copy);
    String listType = useOrderedList? "ol":"ul";
    String id = "";
    if(!htmlId.isEmpty()){
      id = " id=\"" + htmlId + "\"";
    }
    StringBuilder out = new StringBuilder(headline + "<br><"  + listType + id + ">");
    for (FaultInfo info : copy) {
      out.append("<li>").append(toHtml(info)).append("</li>");
    }
    out.append("</").append(listType).append(">");
    return out.toString();
  }

  protected String listDistinctLineNumbersAndJoin(List<CFAEdge> edges){
    return edges
        .stream()
        .mapToInt(l -> l.getFileLocation().getStartingLineInOrigin())
        .sorted()
        .distinct()
        .mapToObj(Integer::toString)
        .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
          int lastIndex = list.size() - 1;
          if (lastIndex < 1) {
            return String.join("", list);
          }
          if (lastIndex == 1) {
            return String.join(" and ", list);
          }
          return String.join(" and ",
              String.join(", ", list.subList(0, lastIndex)),
              list.get(lastIndex));
        }));
  }

}
