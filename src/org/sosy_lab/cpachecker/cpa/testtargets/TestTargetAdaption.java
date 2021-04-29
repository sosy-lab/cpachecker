// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.util.CFAUtils;

public enum TestTargetAdaption {
  NONE {
    @Override
    public Set<CFAEdge> adaptTestTargets(final Set<CFAEdge> targets, final CFA pCfa) {
      return targets;
    }
  },
  COVERED_NEXT_EDGE {
    @Override
    public Set<CFAEdge> adaptTestTargets(final Set<CFAEdge> targets, final CFA pCfa) {
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
  BASIC_ESSENTIAL_EDGE {
    @Override
    public Set<CFAEdge> adaptTestTargets(final Set<CFAEdge> targets, final CFA pCfa) {
      // basic heuristic that follows paths with forced predecessor/successors and removes
      // unnecessary test targets
      return new TestTargetMinimizerBasicEssential().reduceTargets(targets);
    }
  },
  ESSENTIAL_EDGE_ORIGINAL {
    @Override
    public Set<CFAEdge> adaptTestTargets(final Set<CFAEdge> targets, final CFA pCfa) {
      // advanced heuristic that minimizes the control flow graph to eliminate as many test targets
      // as possible
      return new TestTargetMinimizerEssential().reduceTargets(targets, pCfa, true);
    }
  },
  ESSENTIAL_EDGE {
    @Override
    public Set<CFAEdge> adaptTestTargets(final Set<CFAEdge> targets, final CFA pCfa) {
      // advanced heuristic that minimizes the control flow graph to eliminate as many test targets
      // as possible
      return new TestTargetMinimizerEssential().reduceTargets(targets, pCfa, false);
    }
  },
  TESTCOMP {
    @Override
    public Set<CFAEdge> adaptTestTargets(final Set<CFAEdge> targets, final CFA pCfa) {
      // currently only simple heuristic
      Set<CFAEdge> newGoals;
      if (targets.size() < 1000) {
        newGoals = COVERED_NEXT_EDGE.adaptTestTargets(targets, pCfa);
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

  public abstract Set<CFAEdge> adaptTestTargets(final Set<CFAEdge> targets, final CFA pCfa);
}
