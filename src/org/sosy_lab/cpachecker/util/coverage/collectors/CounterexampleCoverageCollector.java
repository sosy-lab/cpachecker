// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.collectors;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.LinkedHashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.coverage.data.FileCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;

public class CounterexampleCoverageCollector extends CoverageCollector {
  /* ##### Constructors ##### */
  CounterexampleCoverageCollector(
      Map<String, FileCoverageStatistics> pInfosPerFile,
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler,
      CFA cfa) {
    super(pInfosPerFile, pCoverageMeasureHandler, pTimeDependentCoverageHandler, cfa);
  }

  CounterexampleCoverageCollector() {
    super(new LinkedHashMap<>(), new CoverageMeasureHandler(), new TimeDependentCoverageHandler());
  }

  /* ##### Static Methods ##### */
  public static Map<String, FileCoverageStatistics> from(ARGPath targetPath) {
    CounterexampleCoverageCollector coverageCollector = new CounterexampleCoverageCollector();
    coverageCollector.collectCoveredEdges(targetPath);
    return coverageCollector.infosPerFile;
  }

  /* ##### Helper Methods ##### */
  private void collectCoveredEdges(ARGPath cexPath) {
    PathIterator pathIterator = cexPath.fullPathIterator();
    while (pathIterator.hasNext()) {
      CFAEdge edge = pathIterator.getOutgoingEdge();

      // Considering covered up until (but not including) when the
      // AssumptionAutomaton state is __FALSE.
      if (isOutsideAssumptionAutomaton(pathIterator.getNextAbstractState())) {
        break;
      }
      addVisitedEdge(edge);
      pathIterator.advance();
    }
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
