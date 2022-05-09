// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.collectors;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.coverage.data.FileCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.data.FilePredicateCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageData;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageType;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageUtility;

public class PredicateAnalysisCoverageCollector extends CoverageCollector {

  PredicateAnalysisCoverageCollector(
      Map<String, FileCoverageStatistics> pInfosPerFile,
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler,
      CFA cfa) {
    super(pInfosPerFile, pCoverageMeasureHandler, pTimeDependentCoverageHandler, cfa);
    timeDependentCoverageHandler.initPredicateAnalysisTDCG();
    coverageMeasureHandler.initPredicateAnalysisMeasures();
  }

  PredicateAnalysisCoverageCollector(
      Map<String, FileCoverageStatistics> pInfosPerFile,
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler) {
    super(pInfosPerFile, pCoverageMeasureHandler, pTimeDependentCoverageHandler);
  }

  public void resetPredicateRelevantVariablesNodes() {
    for (FileCoverageStatistics info : infosPerFile.values()) {
      info.predicateStatistics.resetPredicateRelevantVariablesNodes();
    }
  }

  public double getTempPredicateConsideredCoverage(CFA cfa) {
    return getTempCoverage(cfa, TimeDependentCoverageType.PredicateConsideredLocations);
  }

  public double getTempAbstractStateCoveredNodesCoverage(CFA cfa) {
    return getTempCoverage(cfa, TimeDependentCoverageType.AbstractStateCoveredNodes);
  }

  public double getTempPredicateRelevantVariablesCoverage(CFA cfa) {
    return getTempCoverage(cfa, TimeDependentCoverageType.PredicateRelevantVariables);
  }

  public Map<String, FilePredicateCoverageStatistics> getPredicateStatistics() {
    Map<String, FilePredicateCoverageStatistics> predicateInfosPerFile = new LinkedHashMap<>();
    for (Entry<String, FileCoverageStatistics> fileInfo : infosPerFile.entrySet()) {
      predicateInfosPerFile.put(fileInfo.getKey(), fileInfo.getValue().predicateStatistics);
    }
    return predicateInfosPerFile;
  }

  public void setPredicateStatistics(
      Map<String, FilePredicateCoverageStatistics> predicateInfosPerFile) {
    for (Entry<String, FileCoverageStatistics> fileInfo : infosPerFile.entrySet()) {
      fileInfo.getValue().predicateStatistics = predicateInfosPerFile.get(fileInfo.getKey());
    }
  }

  public void addPredicateConsideredNode(final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    FileCoverageStatistics collector = getCollector(pEdge);
    collector.predicateStatistics.addPredicateConsideredNode(pEdge.getSuccessor());
  }

  public void addPredicateRelevantVariablesNodes(final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    FileCoverageStatistics collector = getCollector(pEdge);
    collector.predicateStatistics.addPredicateRelevantVariablesNodes(pEdge.getSuccessor());
  }

  public void addAbstractStateCoveredNodes(final Set<CFANode> nodes, final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    FileCoverageStatistics collector = getCollector(pEdge);
    collector.addAbstractStateCoveredNodes(nodes);
  }

  public void addInitialNodesForTDCG(
      CFA cfa, TimeDependentCoverageData tdcgData, TimeDependentCoverageType type) {
    for (var node : cfa.getAllNodes()) {
      if (node.getNodeNumber() == 1) {
        CFANode candidateNode = node;
        if (getCollectorForInitNode(candidateNode).isEmpty()) {
          return;
        }
        FileCoverageStatistics collector = getCollectorForInitNode(candidateNode).orElseThrow();
        do {
          switch (type) {
            case PredicateConsideredLocations:
              collector.predicateStatistics.addPredicateConsideredNode(candidateNode);
              tdcgData.addTimeStamp(getTempPredicateConsideredCoverage(cfa));
              break;
            case PredicateRelevantVariables:
              collector.predicateStatistics.addPredicateRelevantVariablesNodes(candidateNode);
              tdcgData.addTimeStamp(getTempPredicateRelevantVariablesCoverage(cfa));
              break;
            default:
              break;
          }
          candidateNode = candidateNode.getLeavingEdge(0).getSuccessor();
        } while (candidateNode.getNumLeavingEdges() == 1);
        break;
      }
    }
  }

  double getTempCoverage(CFA cfa, TimeDependentCoverageType type) {
    int numTotalNodes = cfa.getAllNodes().size();
    int numRelevantNodes = 0;
    for (FileCoverageStatistics info : infosPerFile.values()) {
      switch (type) {
        case PredicateConsideredLocations:
          numRelevantNodes += info.predicateStatistics.allPredicateConsideredNodes.size();
          break;
        case PredicateRelevantVariables:
          numRelevantNodes += info.predicateStatistics.allPredicateRelevantVariablesNodes.size();
          break;
        case AbstractStateCoveredNodes:
          numRelevantNodes += info.allAbstractStateCoveredNodes.size();
          break;
        default:
          break;
      }
    }
    return numRelevantNodes / (double) numTotalNodes;
  }
}
