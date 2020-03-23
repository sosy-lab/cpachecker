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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FaultLocalizationReason<I extends FaultLocalizationOutput> {

  private String description;
  private double likelihood;

  // set this to true if likelihood of this reason should not be weighted.
  private boolean hintOnly;

  // The ranking does not support ranking subsets. Sometimes an error may be caused by multiple
  // lines.
  // To indicate this, place all related objects in this list.
  // Just add the subset to this list.
  // Only write the FaultLocalizationOutput object containing this Reason object in the list.
  /*
  e.g.: Ranking the subset  1) [S1, S2, S3]
                            2) [S1, S3]
                            3) [S2, S4]

  should result in the following list after the ranking algorithm is used:
  RankingList: 1)  S1 (related: S2, S3)    Alternatively: 1) S2 (related: S1, S3)
                      (related: S3)                       2) S3 (related: S1)
               2)  S2 (related: S4)                       3) S4 (related: S2)
   */
  private List<I> related;

  public FaultLocalizationReason(String pDescription) {
    related = new ArrayList<>();
    description = pDescription;
  }

  public String getDescription() {
    return description;
  }

  public List<I> getRelated() {
    return related;
  }

  public double getLikelihood() {
    return likelihood;
  }

  public void setLikelihood(double pLikelihood) {
    likelihood = pLikelihood;
  }

  public void setRelated(List<I> pRelated) {
    related = pRelated;
  }

  public void setDescription(String pDescription) {
    description = pDescription;
  }

  @Override
  public String toString() {
    String percent = ((int) (likelihood * 10000)) / 100d + "%";
    if (related.isEmpty()) {
      return description + " (" + percent + ")";
    }
    return description
        + " (related to lines: "
        + related.stream()
            .map(l -> "" + l.correspondingEdge().getFileLocation().getStartingLineInOrigin())
            .distinct()
            .sorted(Comparator.comparingInt(Integer::parseInt))
            .collect(Collectors.joining(","))
        + " with likelihood of "
        + percent
        + ")";
  }

  public static <I extends FaultLocalizationOutput> FaultLocalizationReason<I> of(
      Set<I> causes, FaultLocalizationExplanation reason) {
    return new FaultLocalizationReason<>(reason.explanationFor(causes));
  }

  public static <I extends FaultLocalizationOutput> FaultLocalizationReason<I> of(
      I cause, FaultLocalizationExplanation reason) {
    return of(Collections.singleton(cause), reason);
  }

  public static <I extends FaultLocalizationOutput> FaultLocalizationReason<I> defaultExplanationOf(
      Set<I> cause) {
    return new FaultLocalizationReason<>(
        FaultLocalizationHeuristicImpl.explainLocationWithoutContext().explanationFor(cause));
  }

  public static <I extends FaultLocalizationOutput> FaultLocalizationReason<I> defaultExplanationOf(
      I cause) {
    return defaultExplanationOf(Collections.singleton(cause));
  }
}
