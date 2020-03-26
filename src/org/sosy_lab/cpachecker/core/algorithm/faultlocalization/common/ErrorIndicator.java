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
  private List<FaultLocalizationReason> reasonList;
  private Function<List<FaultLocalizationReason>, Double> evaluationFunction = a -> a.stream().filter(c -> !c.isHintOnly()).mapToDouble(b -> b.getLikelihood()).average().orElse(0) * 100;;

  public double calculateScore() {return evaluationFunction.apply(reasonList);}

  public List<Integer> sortedLineNumbers(){
    List<Integer> lines = new ArrayList<>();
    for(I elem: errorSet){
      lines.add(elem.correspondingEdge().getFileLocation().getStartingLineInOrigin());
    }
    Collections.sort(lines);
    return lines;
  }

  public ErrorIndicator(Set<I> pErrorSet){
    errorSet = pErrorSet;
    reasonList = new ArrayList<>();
  }

  public ErrorIndicator(){
    errorSet = new HashSet<>();
    reasonList = new ArrayList<>();
  }

  public void addReason(FaultLocalizationReason reason){
    reasonList.add(reason);
  }

  public List<FaultLocalizationReason> getReasonList() {
    return reasonList;
  }

  public void setEvaluationFunction(Function<List<FaultLocalizationReason>, Double> evaluation){
    evaluationFunction = evaluation;
  }

  protected String reasonToHtml(FaultLocalizationReason reason) {
    double likelihood = reason.getLikelihood();
    boolean hintOnly = reason.isHintOnly();
    String description = reason.getDescription();

    String percent = "<strong>" + ((int) (likelihood * 10000)) / 100d + "%</strong>";
    if(hintOnly){
      return description;
    }
    return description + " (" + percent + ")";
  }

  public String toHtml(){
    Comparator<FaultLocalizationReason> sortReasons =
        Comparator.comparingInt(l -> l.isHintOnly()?0:1);
    sortReasons = sortReasons.thenComparingDouble(b -> 1/b.getLikelihood());
    reasonList.sort(sortReasons);
    int numberReasons = reasonList.stream().filter(l -> !l.isHintOnly()).mapToInt(l -> 1).sum();

    String header = "Error suspected on line(s): <strong>" + errorSet
        .stream()
        .mapToInt(l -> l.correspondingEdge().getFileLocation().getStartingLineInOrigin())
        .sorted()
        .distinct()
        .mapToObj(l -> (Integer)l + "")
        .collect(Collectors.collectingAndThen(Collectors.toList(), lineCollector()))
        + "</strong><br>";
    String reasons = "Detected <strong>" + numberReasons + "</strong> possible reason(s):<br>";
    StringBuilder html = new StringBuilder();
    html.append("<ul id=\"hint-list\">");
    for (var reason : reasonList){
      if(reason.isHintOnly())
        html.append("<li>").append(reasonToHtml(reason)).append("</li>");
      else break;
    }
    html.append("</ul>");

    html.append("<ol>");
    for (var reason : reasonList){
      if(!reason.isHintOnly())
        html.append("<li>").append(reasonToHtml(reason)).append("</li>");
    }
    html.append("</ol>");


    return header + "\n" + reasons + "\n" + html;
  }

  @Override
  public String toString(){
    String header = "Error suspected on line(s): " + errorSet.stream().map(l -> l.correspondingEdge().getFileLocation().getStartingLineInOrigin()+"").collect(Collectors.collectingAndThen(Collectors.toList(), lineCollector()));
    String reasons = "Detected " + reasonList.size() + " possible reason(s): ";
    StringBuilder body = new StringBuilder();
    for (int i = 0; i < reasonList.size(); i++) {
      body.append("  ").append(i + 1).append(") ").append(reasonList.get(i).toString())
          .append("\n");
    }
    return header + "\n" + reasons + "\n" + body;
  }

  /**
   * Transforms lists like [1,2,7,3] to "1,2,7 and 3"
   * @return
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
