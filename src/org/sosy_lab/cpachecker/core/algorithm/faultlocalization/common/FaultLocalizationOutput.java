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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public abstract class FaultLocalizationOutput {

  /**
   * the heuristics give scores and add reasons on why this object leads to an error here the score
   * of the whole object is calculated automatically as the average value of all assigned scores.
   * make sure that the heuristics are balanced. Giving a high score to an object may corrupt the
   * ranking because the calculated average will be high to. it is possible to change the score
   * evaluation algorithm.
   */
  protected List<FaultLocalizationReason<? extends FaultLocalizationOutput>>
      faultLocalizationReasons = new ArrayList<>();

  public double getScore() {
    return evaluateScore();
  }

  public void addReason(
      FaultLocalizationReason<? extends FaultLocalizationOutput> pFaultLocalizationReason) {
    faultLocalizationReasons.add(pFaultLocalizationReason);
  }

  // Override this method to change the default score evaluation
  protected double evaluateScore() {
    return faultLocalizationReasons.stream()
            .mapToDouble(FaultLocalizationReason::getLikelihood)
            .average()
            .orElse(0)
        * 100;
  }

  public List<FaultLocalizationReason<? extends FaultLocalizationOutput>>
      getFaultLocalizationReasons() {
    return faultLocalizationReasons;
  }

  public static FaultLocalizationOutput of(CFAEdge pEdge) {
    return new FaultLocalizationOutput() {
      @Override
      public CFAEdge correspondingEdge() {
        return pEdge;
      }

      @Override
      public boolean equals(Object q) {
        if (q instanceof FaultLocalizationOutput)
          return pEdge.equals(((FaultLocalizationOutput) q).correspondingEdge());
        return false;
      }

      @Override
      public int hashCode() {
        return pEdge.hashCode();
      }
    };
  }

  protected String reasonToHtml(FaultLocalizationReason<? extends FaultLocalizationOutput> reason) {
    double likelihood = reason.getLikelihood();
    String description = reason.getDescription();
    List<? extends FaultLocalizationOutput> related = reason.getRelated();

    String percent = ((int) (likelihood * 10000)) / 100d + "%";
    if (related.isEmpty()) {
      return description + " (<strong>" + percent + "</strong>)";
    }
    return description
        + " (strict relation to lines: "
        + related.stream()
            .map(l -> "" + l.correspondingEdge().getFileLocation().getStartingLineInOrigin())
            .distinct()
            .sorted(Comparator.comparingInt(Integer::parseInt))
            .collect(Collectors.joining(","))
        + " with likelihood of "
        + percent
        + ")";
  }

  public String htmlRepresentation() {
    StringBuilder html =
        new StringBuilder(
            "Error suspected on <strong>line "
                + correspondingEdge().getFileLocation().getStartingLineInOrigin()
                + "</strong>.<br>");
    int reasons = faultLocalizationReasons.size();
    html.append("Detected <strong>")
        .append(reasons)
        .append("</strong> possible reason")
        .append(reasons == 1 ? "" : "s")
        .append(":<br>");
    html.append("<ol>");
    for (var reason : faultLocalizationReasons)
      html.append("<li>").append(reasonToHtml(reason)).append("</li>");
    html.append("</ol>");
    return html.toString();
  }

  /**
   * String representation of this class. This method is forwarded to toString method. Classes that
   * extend this class may want to override the toString method this is why an extra method was
   * created. To change the output int the Report (Counterexample.x.html) just override this
   * method.) Override this method to change the given output to user. (Click on rank in
   * ReportManager)
   *
   * @return String representation of this object
   */
  public String textRepresentation() {
    int reasons = faultLocalizationReasons.size();
    StringBuilder toString =
        new StringBuilder()
            .append("Error suspected on line ")
            .append(correspondingEdge().getFileLocation().getStartingLineInOrigin())
            .append(". (Score: ")
            .append((int) getScore())
            .append(")\n")
            .append("Detected ")
            .append(reasons)
            .append(" possible reason(s):\n");
    for (int i = 0; i < reasons; i++) {
      toString
          .append("  ")
          .append(i + 1)
          .append(") ")
          .append(faultLocalizationReasons.get(i).toString())
          .append("\n");
    }
    return toString.toString();
  }

  public boolean hasReasons() {
    return !faultLocalizationReasons.isEmpty();
  }

  @Override
  public String toString() {
    return textRepresentation();
  }

  public abstract CFAEdge correspondingEdge();

  @Override
  public abstract boolean equals(Object q);

  @Override
  public abstract int hashCode();
}
