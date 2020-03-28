/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common;

import com.google.common.collect.ForwardingSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ErrorIndicator<I extends FaultLocalizationOutput> extends ForwardingSet<I> {

  private Set<I> errorSet;
  private List<FaultLocalizationReason> reasons;
  private Function<List<FaultLocalizationReason>, Double> evaluationFunction = a -> a.stream().filter(c -> !c.isHint()).mapToDouble(b -> b.getLikelihood()).average().orElse(0) * 100;;

  /**
   * Calculates the score by the provided evaluation function.
   * @return calculated score
   */
  public double calculateScore() {return evaluationFunction.apply(reasons);}

  public List<Integer> sortedLineNumbers(){
    List<Integer> lines = new ArrayList<>();
    for(I elem: errorSet){
      lines.add(elem.correspondingEdge().getFileLocation().getStartingLineInOrigin());
    }
    Collections.sort(lines);
    return lines;
  }

  /**
   * Error Indicators indicate a subset of all edges that most likely contain an error.
   * @param pErrorSet set to forward
   */
  public ErrorIndicator(Set<I> pErrorSet){
    errorSet = pErrorSet;
    reasons = new ArrayList<>();
  }

  public ErrorIndicator(){
    errorSet = new HashSet<>();
    reasons = new ArrayList<>();
  }

  /**
   * Creates a mutable set with only one member
   * @param singleton a indicator that is transformed into a singleton set
   */
  public ErrorIndicator(I singleton){
    errorSet = new HashSet<>(Collections.singleton(singleton));
    reasons = new ArrayList<>();
  }

  /**
   * Add a reason to explain why this set indicates a error.
   * Appending reasons in heuristics is the recommended way.
   * @param reason Fix, Hint or Reason why this might be an error or why a heuristic did add a core to this set.
   */
  public void addReason(FaultLocalizationReason reason){
    reasons.add(reason);
  }

  public List<FaultLocalizationReason> getReasons() {
    return reasons;
  }

  public void setEvaluationFunction(Function<List<FaultLocalizationReason>, Double> evaluation){
    evaluationFunction = evaluation;
  }

  /**
   * HTML representation of a reason for the report.
   * Extend this class and override this method to change the representation.
   * @param reason the reason that is converted to html code
   * @return html code of reason
   */
  protected String reasonToHtml(FaultLocalizationReason reason) {
    double likelihood = reason.getLikelihood();
    boolean hintOnly = reason.isHint();
    String description = reason.getDescription();

    String percent = "<strong>" + ((int) (likelihood * 10000)) / 100d + "%</strong>";
    if(hintOnly){
      return description;
    }
    return description + " (" + percent + ")";
  }

  /**
   * Convert this object to a HTML string for the report.
   * @return hmtl code of this instance
   */
  public String toHtml(){
    Comparator<FaultLocalizationReason> sortReasons =
        Comparator.comparingInt(l -> l.isHint() ? 0 : 1);
    sortReasons = sortReasons.thenComparingDouble(b -> 1/b.getLikelihood());
    reasons.sort(sortReasons);
    int numberReasons = reasons.stream().filter(l -> !l.isHint()).mapToInt(l -> 1).sum();

    String header = "Error suspected on line(s): <strong>" + errorSet
        .stream()
        .mapToInt(l -> l.correspondingEdge().getFileLocation().getStartingLineInOrigin())
        .sorted()
        .distinct()
        .mapToObj(l -> (Integer)l + "")
        .collect(Collectors.collectingAndThen(Collectors.toList(), lineCollector()))
        + "</strong><br>";
    String reasonsString = "Detected <strong>" + numberReasons + "</strong> possible reason(s):<br>";
    StringBuilder html = new StringBuilder();
    html.append("<ul id=\"hint-list\">");
    for (var reason : this.reasons){
      if(reason.isHint())
        html.append("<li>").append(reasonToHtml(reason)).append("</li>");
      else break;
    }
    html.append("</ul>");

    html.append("<ol>");
    for (var reason : this.reasons){
      if(!reason.isHint())
        html.append("<li>").append(reasonToHtml(reason)).append("</li>");
    }
    html.append("</ol>");


    return header + "\n" + reasonsString + "\n" + html;
  }

  @Override
  public String toString(){
    String header = "Error suspected on line(s): " + errorSet.stream().map(l -> l.correspondingEdge().getFileLocation().getStartingLineInOrigin()+"").collect(Collectors.collectingAndThen(Collectors.toList(), lineCollector()));
    String reasonString = "Detected " + this.reasons.size() + " possible reason(s): ";
    StringBuilder body = new StringBuilder();
    for (int i = 0; i < this.reasons.size(); i++) {
      body.append("  ").append(i + 1).append(") ").append(this.reasons.get(i).toString())
          .append("\n");
    }
    return header + "\n" + reasonString + "\n" + body;
  }

  /**
   * Transforms lists like [1,2,7,3] to "1,2,7 and 3"
   * @return joined string
   */
  private Function<List<String>, String> lineCollector() {
    return list -> {
      int lastIndex = list.size() - 1;
      if (lastIndex < 1) return String.join("", list);
      if (lastIndex == 1) return String.join(" and ", list);
      return String.join(" and ",
          String.join(", ", list.subList(0, lastIndex)),
          list.get(lastIndex));
    };
  }

  @Override
  protected Set<I> delegate() {
    return errorSet;
  }
}
