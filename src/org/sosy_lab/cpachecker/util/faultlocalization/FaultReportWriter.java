package org.sosy_lab.cpachecker.util.faultlocalization;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * Provides useful methods for converting objects in this package to a HTML format.
 * Extend this class and override the methods to create a different HTML-representation.
 * Change the FaultReportWrite in FaultLocalizationInfo by calling the method <code>replaceHtmlWriter</code>
 */
public class FaultReportWriter {

  public String toHtml(FaultReason reason) {
    double likelihood = reason.getLikelihood();
    boolean hintOnly = reason.isHint();
    String description = reason.getDescription();

    String percent = "<strong>" + ((int) (likelihood * 10000)) / 100d + "%</strong>";
    if(hintOnly){
      return description;
    }
    return description + " (" + percent + ")";
  }

  public String toHtml(FaultContribution faultContribution) {
    return toHtml(faultContribution.getReasons(), Collections.singletonList(faultContribution.correspondingEdge())) + "<br><i>Score: " + (int)(faultContribution.getScore()*100)+"</i>";
  }

  public String toHtml(Fault fault) {
    // list of all edges in fault sorted by line number
    List<CFAEdge> edges = fault
        .stream()
        .map(FaultContribution::correspondingEdge)
        .sorted(Comparator.comparingInt(l -> l.getFileLocation().getStartingLineInOrigin()))
        .collect(Collectors.toList());
    return toHtml(fault.getReasons(), edges);
  }

  /**
   * Convert this object to a HTML string for the report.
   * @return hmtl code of this instance
   */
  private String toHtml(List<FaultReason> reasons, List<CFAEdge> correspondingEdges){
    Comparator<FaultReason> sortReasons =
        Comparator.comparingInt(c -> c.isHint() ? 0 : 1);
    sortReasons = sortReasons.thenComparing((c1,c2) -> Double.compare(c2.getLikelihood(), c1.getLikelihood()));
    reasons.sort(sortReasons);
    int numberReasons = reasons.stream().filter(r -> !r.isHint()).mapToInt(r -> 1).sum();

    String header = "Error suspected on line(s): <strong>" + listDistinctLineNumbersAndJoin(correspondingEdges)
        + "</strong><br>";
    String reasonsString = "Detected <strong>" + numberReasons + "</strong> possible reason(s):<br>";
    StringBuilder html = new StringBuilder();

    //This works because reasons is sorted by ReasonType first.
    int numberHints = reasons.size()-numberReasons;
    html.append("<ul id=\"hint-list\">");
    for(int i = 0; i < numberHints; i++){
      html.append("<li>").append(toHtml(reasons.get(i))).append("</li>");
    }
    html.append("</ul>");

    html.append("<ol>");
    for (int i = numberHints; i < reasons.size(); i++){
        html.append("<li>").append(toHtml(reasons.get(i))).append("</li>");
    }
    html.append("</ol>");


    return header + "\n" + reasonsString + "\n" + html;
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
