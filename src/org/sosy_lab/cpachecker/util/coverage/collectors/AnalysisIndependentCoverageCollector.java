// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureType;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;

/**
 * Coverage collector which is called used by the AnalysisIndependentCoverageCPA. The calculated
 * coverage measures depends on data which is collected during an analysis. Since this class
 * collects data during the analysis it is possible to calculate a temporal coverage value based on
 * partially available data. Therefore, the data collected in this class is also suitable to be used
 * for a TDCG.
 */
public class AnalysisIndependentCoverageCollector extends CoverageCollector {
  private final Multiset<CFANode> visitedLocations = LinkedHashMultiset.create();
  private static final ImmutableList<CoverageMeasureType> TYPES =
      ImmutableList.of(
          CoverageMeasureType.VISITED_LOCATIONS,
          CoverageMeasureType.CONSIDERED_LOCATIONS_HEAT_MAP,
          CoverageMeasureType.VISITED_LINES_HEAT_MAP);

  AnalysisIndependentCoverageCollector(
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler,
      CFA cfa) {
    super(pCoverageMeasureHandler, pTimeDependentCoverageHandler, cfa);
    timeDependentCoverageHandler.initAnalysisIndependentTDCG();
    addInitialNodesForMeasures(cfa);
  }

  public void collect(CoverageCollectorHandler coverageCollectorHandler) {
    collect(coverageCollectorHandler, TYPES);
  }

  public void addInitialNodesForMeasures(CFA cfa) {
    boolean isLoop = false;
    for (CFANode node : cfa.getAllNodes()) {
      if (node.getNodeNumber() == 1) {
        CFANode candidateNode = node;
        do {
          if (!visitedLocations.contains(candidateNode)) {
            visitedLocations.add(candidateNode);
          }
          CFANode currentNode = candidateNode;
          candidateNode = candidateNode.getLeavingEdge(0).getSuccessor();
          if (currentNode == candidateNode) {
            isLoop = true;
          }
        } while (candidateNode.getNumLeavingEdges() == 1 && !isLoop);
        break;
      }
    }
  }

  public void addVisitedLocation(CFAEdge pEdge) {
    visitedLocations.add(pEdge.getSuccessor());
    if (!visitedLocations.contains(pEdge.getPredecessor())) {
      visitedLocations.add(pEdge.getPredecessor());
    }
  }

  public Multiset<CFANode> getVisitedLocations() {
    return visitedLocations;
  }

  public double getTempVisitedCoverage() {
    int totalLines = getExistingLinesCount();
    if (totalLines > 0) {
      return getVisitedLinesCount() / (double) totalLines;
    }
    return 0.0;
  }
}
