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
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.heuristics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicatorSet;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristic;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationHeuristicImpl;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationOutput;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationReason;

public class ErrorLocationNearestHeuristic<I extends FaultLocalizationOutput>
    implements FaultLocalizationHeuristic<I> {

  private int errorLocation;

  /**
   * Sorts the result set by absolute distance to the error location based on the linenumber
   *
   * @param pErrorLocation the error location
   */
  public ErrorLocationNearestHeuristic(CFAEdge pErrorLocation) {
    errorLocation = pErrorLocation.getLineNumber();
  }

  @Override
  public List<I> rank(ErrorIndicatorSet<I> result) {
    List<I> sort =
        new ArrayList<>(FaultLocalizationHeuristicImpl.condenseErrorIndicatorSet(result));
    sort.sort(
        Comparator.comparingInt(
            a -> Math.abs(errorLocation - a.correspondingEdge().getLineNumber())));
    for (I l : sort) {
      FaultLocalizationReason<I> reason =
          new FaultLocalizationReason<>(
              "Distance to error location: "
                  + Math.abs(errorLocation - l.correspondingEdge().getLineNumber())
                  + " line(s).");
      reason.setLikelihood(
          BigDecimal.valueOf(2)
              .pow(sort.size() - 1 - sort.indexOf(l))
              .divide(
                  BigDecimal.valueOf(2).pow(sort.size()).subtract(BigDecimal.ONE),
                  10,
                  RoundingMode.HALF_UP)
              .doubleValue());
      l.addReason(reason);
    }

    return sort;
  }
}
