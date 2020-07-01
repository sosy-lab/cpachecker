/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.testtargets;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.util.CFAUtils;

public enum TestTargetAdaption {
  NONE {
    @Override
    public Set<CFAEdge> adaptTestTargets(final Set<CFAEdge> targets) {
      return targets;
    }
  },
  COVERED_NEXT_EDGE {
    @Override
    public Set<CFAEdge> adaptTestTargets(final Set<CFAEdge> targets) {
      // currently only simple heuristic
      Set<CFAEdge> newGoals;
      newGoals = new HashSet<>(targets);
      boolean allSuccessorsGoals;
      for (CFAEdge target : targets) {
        if (target.getSuccessor().getNumEnteringEdges() == 1) {
          allSuccessorsGoals = true;
          for (CFAEdge leaving : CFAUtils.leavingEdges(target.getSuccessor())) {
            if (!targets.contains(leaving)) {
              allSuccessorsGoals = false;
              break;
            }
          }
          if (allSuccessorsGoals) {
            newGoals.remove(target);
          }
        }
      }
      return newGoals;
    }
  },
  TESTCOMP {
    @Override
    public Set<CFAEdge> adaptTestTargets(final Set<CFAEdge> targets) {
      // currently only simple heuristic
      Set<CFAEdge> newGoals;
      if (targets.size() < 1000) {
        newGoals = COVERED_NEXT_EDGE.adaptTestTargets(targets);
      } else {
        newGoals = new HashSet<>();
        for (CFAEdge target : targets) {
          if (target.getEdgeType() == CFAEdgeType.AssumeEdge) {
            for (CFAEdge leaving : CFAUtils.leavingEdges(target.getSuccessor())) {
              if (!(leaving.getEdgeType() == CFAEdgeType.AssumeEdge)) {
                newGoals.add(leaving);
              }
            }
          } else {
            newGoals.add(target);
          }
        }
      }
      return newGoals;
    }
  };

  public abstract Set<CFAEdge> adaptTestTargets(final Set<CFAEdge> targets);
}
