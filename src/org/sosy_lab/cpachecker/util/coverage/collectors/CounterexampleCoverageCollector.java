// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.collectors;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Coverage collector which is used by the CEXExporter. The calculated coverage measures depends on
 * a given counter example path. Therefore, it is only usable after the main analysis is done.
 */
public class CounterexampleCoverageCollector extends CoverageCollector {
  public static CounterexampleCoverageCollector collectCoveredEdges(ARGPath cexPath) {
    CounterexampleCoverageCollector coverageCollector = new CounterexampleCoverageCollector();
    PathIterator pathIterator = cexPath.fullPathIterator();
    while (pathIterator.hasNext()) {
      CFAEdge edge = pathIterator.getOutgoingEdge();
      // Considering covered up until (but not including) when the
      // AssumptionAutomaton state is __FALSE.
      if (coverageCollector.isOutsideAssumptionAutomaton(pathIterator.getNextAbstractState())) {
        break;
      }
      coverageCollector.addVisitedEdge(edge);
      pathIterator.advance();
    }
    return coverageCollector;
  }

  private boolean isOutsideAssumptionAutomaton(ARGState s) {
    boolean foundAssumptionAutomaton = false;
    for (AutomatonState aState : AbstractStates.asIterable(s).filter(AutomatonState.class)) {
      if (aState.getOwningAutomatonName().equals("AssumptionAutomaton")) {
        foundAssumptionAutomaton = true;
        if (aState.getInternalStateName().equals("__FALSE")) {
          return true;
        }
      }
    }
    checkArgument(
        foundAssumptionAutomaton,
        "This method should only be called when an "
            + "Assumption Automaton is used as part of the specification.");
    return false;
  }
}
