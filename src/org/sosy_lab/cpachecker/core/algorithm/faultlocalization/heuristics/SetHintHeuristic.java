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

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicator;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicatorSet;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationOutput;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationReason;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.FaultLocalizationSetHeuristic;

public class SetHintHeuristic<I extends FaultLocalizationOutput> implements
                                                                 FaultLocalizationSetHeuristic<I> {
  private int maxNumberOfHints;

  /**
   * Create hints for the first pMaxNumberOfHints sets in the ErrorIndicatorSet
   * @param pMaxNumberOfHints number of hints to be printed. Passing -1 leads to hints for all elements in the set.
   */
  public SetHintHeuristic(int pMaxNumberOfHints){
    maxNumberOfHints=pMaxNumberOfHints;
  }
  @Override
  public Map<ErrorIndicator<I>, Integer> rankSubsets(
      ErrorIndicatorSet<I> errorIndicators) {
    Map<ErrorIndicator<I>, Integer> errorIndicatorIntegerMap = new HashMap<>();
    for (ErrorIndicator<I> errorIndicator : errorIndicators) {
      int current = 0;
      for (I i : errorIndicator) {
        errorIndicator.addReason(FaultLocalizationReason.hintFor(new ErrorIndicator<>(i)));
        current++;
        if(current == maxNumberOfHints){
          break;
        }
      }
      errorIndicatorIntegerMap.put(errorIndicator, 1);
    }
    return errorIndicatorIntegerMap;
  }
}
