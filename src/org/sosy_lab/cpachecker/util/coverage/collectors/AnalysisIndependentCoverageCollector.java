// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.collectors;

import java.util.Map;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.coverage.data.FileCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureType;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;

public class AnalysisIndependentCoverageCollector extends CoverageCollector {

  AnalysisIndependentCoverageCollector(
      Map<String, FileCoverageStatistics> pInfosPerFile,
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler,
      CFA cfa) {
    super(pInfosPerFile, pCoverageMeasureHandler, pTimeDependentCoverageHandler, cfa);
    timeDependentCoverageHandler.initAnalysisIndependentTDCG();
    coverageMeasureHandler.initAnalysisIndependentMeasures();
    addInitialNodesForMeasures(cfa);
  }

  AnalysisIndependentCoverageCollector(
      Map<String, FileCoverageStatistics> pInfosPerFile,
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler) {
    super(pInfosPerFile, pCoverageMeasureHandler, pTimeDependentCoverageHandler);
  }

  public void addInitialNodesForMeasures(CFA cfa) {
    for (var node : cfa.getAllNodes()) {
      if (node.getNodeNumber() == 1) {
        CFANode candidateNode = node;
        if (getCollectorForInitNode(candidateNode).isEmpty()) {
          return;
        }
        FileCoverageStatistics collector = getCollectorForInitNode(candidateNode).orElseThrow();
        do {
          for (var type : coverageMeasureHandler.getAllTypes()) {
            if (type == CoverageMeasureType.VisitedLocations) {
              collector.visitedLocations.add(candidateNode.getNodeNumber());
            }
          }
          candidateNode = candidateNode.getLeavingEdge(0).getSuccessor();
        } while (candidateNode.getNumLeavingEdges() == 1);
        break;
      }
    }
  }

  public void addVisitedLocation(CFAEdge pEdge) {
    final FileLocation loc = pEdge.getFileLocation();
    final FileCoverageStatistics collector = getFileInfoTarget(loc, infosPerFile);
    collector.visitedLocations.add(pEdge.getSuccessor().getNodeNumber());
    if (!collector.visitedLocations.contains(pEdge.getPredecessor().getNodeNumber())) {
      collector.visitedLocations.add(pEdge.getPredecessor().getNodeNumber());
    }
  }

  public double getTempVisitedCoverage() {
    int numTotalLines = 0;
    int numVisitedLines = 0;
    for (FileCoverageStatistics info : infosPerFile.values()) {
      numTotalLines += info.allLines.size();
      numVisitedLines += info.visitedLines.entrySet().size();
    }
    double visitedLinesCoverage = 0.0;
    if (numTotalLines > 0) {
      visitedLinesCoverage = numVisitedLines / (double) numTotalLines;
    }
    return visitedLinesCoverage;
  }
}
