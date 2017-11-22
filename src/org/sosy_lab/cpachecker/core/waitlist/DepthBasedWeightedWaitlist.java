/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.waitlist;

import java.util.Comparator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class DepthBasedWeightedWaitlist extends WeightedRandomWaitlist {

  public DepthBasedWeightedWaitlist() {
    super(new BranchingComparator());
  }

  private static class BranchingComparator implements Comparator<AbstractState> {

    @Override
    public int compare(AbstractState o1, AbstractState o2) {
      ARGState s1 = (ARGState) o1;
      ARGState s2 = (ARGState) o2;
      return s1.getBranchingCount() - s2.getBranchingCount();
    }
  }
}
