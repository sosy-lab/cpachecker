package org.sosy_lab.cpachecker.util.faultlocalization;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
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
 */
public class FaultReportWriter {

  public String toHtml(FaultInfo info) {
    double likelihood = info.getScore();
    String description = info.getDescription();

    String percent = "<strong>" + ((int) (likelihood * 10000)) / 100d + "%</strong>";
    if(info.getType().equals(InfoType.HINT) || info.getType().equals(InfoType.FIX)){
      return description;
    }
    return description + " (" + percent + ")";
  }

  public String toHtml(FaultContribution faultContribution) {
    return toHtml(faultContribution.getInfos(), Collections.singletonList(faultContribution.correspondingEdge())) + "<br><i>Score: " + (int)(faultContribution.getScore()*100)+"</i>";
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
   * @return hmtl code of this instance
   */
  private String toHtml(List<FaultInfo> infos, List<CFAEdge> correspondingEdges){
    PriorityQueue<FaultReason> faultReasons = new PriorityQueue<>();
    PriorityQueue<RankInfo> faultInfo = new PriorityQueue<>();
    PriorityQueue<PotentialFix> faultFix = new PriorityQueue<>();
    PriorityQueue<Hint> faultHint = new PriorityQueue<>();

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

    if (!faultReasons.isEmpty()) {
      html.append(printList("Detected <strong>" +
              faultInfo.size() + "</strong> possible reason(s):<br>", "",
          faultInfo, true))
          .append("<br>");
    }

    if (!faultHint.isEmpty()) {
      html.append(
              printList(
                  "<strong>" + faultHint.size() + "</strong> hints are available:<br>",
                  "hint-list",
                  faultHint,
                  false))
          .append("<br>");
    }

    if (!faultFix.isEmpty()) {
      html.append(printList("Found " + faultFix.size() + " possible bug-fixes:<br>", "fix-list",
          faultInfo, false))
          .append("<br>");
    }

    if (!faultInfo.isEmpty()) {
      html.append(printList("The score is obtained by:<br>", "", faultInfo, true))
          .append("<br>");
    }

    return header + "\n" + html;
  }

  private String printList(String headline, String htmlId, Collection<? extends FaultInfo> infos, boolean useOrderedList){
    String listType = useOrderedList? "ol":"ul";
    String id = "";
    if(!htmlId.equals("")){
      id = " id=\"" + htmlId + "\"";
    }
    StringBuilder out = new StringBuilder(headline + "<br><"  + listType + id + ">");
    for (FaultInfo info : infos) {
      out.append("<li>").append(toHtml(info)).append("</li>");
    }
    out.append("</").append(listType).append(">");
    return out.toString();
  }

  private String listDistinctLineNumbersAndJoin(List<CFAEdge> edges){
    return edges
        .stream()
        .mapToInt(l -> l.getFileLocation().getStartingLineInOrigin())
        .sorted()
        .distinct()
        .mapToObj(l -> (Integer)l + "")
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
