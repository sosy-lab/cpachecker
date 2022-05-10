// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.collectors;

import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.coverage.data.FileCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageData;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageType;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageUtility;

/**
 * Coverage collector which is called used by the PredicateCoverageCPA. The calculated coverage
 * measures depends on data which is collected during the predicate analysis. Since this class
 * collects data during the analysis it is possible to calculate a temporal coverage value based on
 * partially available data. Therefore, the data collected in this class is also suitable to be used
 * for a TDCG.
 */
public class PredicateAnalysisCoverageCollector extends CoverageCollector {
  /* ##### Constructors ##### */
  PredicateAnalysisCoverageCollector(
      Map<String, FileCoverageStatistics> pInfosPerFile,
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler,
      CFA cfa) {
    super(pInfosPerFile, pCoverageMeasureHandler, pTimeDependentCoverageHandler, cfa);
    timeDependentCoverageHandler.initPredicateAnalysisTDCG();
    coverageMeasureHandler.initPredicateAnalysisMeasures();
  }

  public void resetPredicateRelevantVariablesNodes() {
    for (FileCoverageStatistics info : infosPerFile.values()) {
      info.predicateStatistics.resetPredicateRelevantVariablesNodes();
    }
  }

  /* ##### Add Methods ##### */
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

  /* ##### Getter Methods ##### */
  public double getTempPredicateConsideredCoverage(CFA cfa) {
    return getTempCoverage(cfa, TimeDependentCoverageType.PredicateConsideredLocations);
  }

  public double getTempAbstractStateCoveredNodesCoverage(CFA cfa) {
    return getTempCoverage(cfa, TimeDependentCoverageType.AbstractStateCoveredNodes);
  }

  public double getTempPredicateRelevantVariablesCoverage(CFA cfa) {
    return getTempCoverage(cfa, TimeDependentCoverageType.PredicateRelevantVariables);
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
