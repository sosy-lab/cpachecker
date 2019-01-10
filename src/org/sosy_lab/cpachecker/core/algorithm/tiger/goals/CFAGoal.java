/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.algorithm.tiger.goals;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.ThreeValuedAnswer;
import org.sosy_lab.cpachecker.cpa.multigoal.CFAEdgesGoal;

public class CFAGoal extends Goal {

  CFAEdgesGoal cfaEdgesGoal;

  public CFAGoal(List<CFAEdge> pEdges) {
    cfaEdgesGoal = new CFAEdgesGoal(pEdges);
  }

  public CFAGoal(CFAEdge pEdge) {
    cfaEdgesGoal = new CFAEdgesGoal(Arrays.asList(pEdge));
  }

  @Override
  public String getName() {
    Iterator<CFAEdge> iter = cfaEdgesGoal.getEdges().iterator();
    StringBuilder builder = new StringBuilder();
    while (iter.hasNext()) {
      builder.append(iter.next().toString());
      if (iter.hasNext()) {
        builder.append("->");
      }
    }
    return builder.toString();
  }

  @Override
  public ThreeValuedAnswer getsCoveredByPath(List<CFAEdge> pPath) {
    return cfaEdgesGoal.coveredByPath(pPath) ? ThreeValuedAnswer.ACCEPT : ThreeValuedAnswer.REJECT;
  }

  public CFAEdgesGoal getCFAEdgesGoal() {
    return cfaEdgesGoal;
  }

}
