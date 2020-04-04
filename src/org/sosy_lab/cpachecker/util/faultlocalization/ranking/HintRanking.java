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
package org.sosy_lab.cpachecker.util.faultlocalization.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRanking;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultReason;

public class HintRanking implements FaultRanking {
  private int maxNumberOfHints;

  /**
   * Create hints for the first pMaxNumberOfHints sets in the ErrorIndicatorSet
   * @param pMaxNumberOfHints number of hints to be printed. Passing -1 leads to hints for all elements in the set.
   */
  public HintRanking(int pMaxNumberOfHints){
    maxNumberOfHints=pMaxNumberOfHints;
  }

  @Override
  public List<Fault> rank(
      Set<Fault> result) {
    for (Fault faultLocalizationOutputs : result) {
      int hints = 0;
      for (FaultContribution faultContribution : faultLocalizationOutputs) {
        if(hints == maxNumberOfHints){
          break;
        }
        faultContribution.addReason(FaultReason.hintFor(new Fault(faultContribution)));
        hints++;
      }
    }
    return new ArrayList<>(result);
  }
}
