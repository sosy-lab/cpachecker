// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.collectors;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
  private final Set<CFANode> predicateConsideredLocations = new LinkedHashSet<>();
  private final Set<CFANode> predicateRelevantVariablesLocations = new LinkedHashSet<>();
  private final Multiset<String> variableNames = HashMultiset.create();
  private final Multiset<String> relevantVariableNames = HashMultiset.create();
  private int previousPredicateRelevantVariablesLocationsSize = 0;

  PredicateAnalysisCoverageCollector(
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler,
      CFA cfa) {
    super(pCoverageMeasureHandler, pTimeDependentCoverageHandler, cfa);
    timeDependentCoverageHandler.initPredicateAnalysisTDCG();
    coverageMeasureHandler.addAllPredicateAnalysisMeasuresTypes();
  }

  public void addPredicateConsideredNode(final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    predicateConsideredLocations.add(pEdge.getSuccessor());
  }

  public void addPredicateRelevantVariablesNodes(final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    predicateRelevantVariablesLocations.add(pEdge.getSuccessor());
  }

  public void addAbstractionVariables(Set<String> pVariableNames, final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    variableNames.addAll(pVariableNames);
  }

  public void addRelevantAbstractionVariables(Set<String> pVariableNames, final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    relevantVariableNames.addAll(pVariableNames);
  }

  public void addInitialNodesForTDCG(
      CFA cfa, TimeDependentCoverageData tdcgData, TimeDependentCoverageType type) {
    for (CFANode node : cfa.getAllNodes()) {
      if (node.getNodeNumber() == 1) {
        CFANode candidateNode = node;
        do {
          switch (type) {
            case PredicateConsideredLocations:
              predicateConsideredLocations.add(candidateNode);
              tdcgData.addTimestamp(getTempPredicateConsideredCoverage(cfa));
              break;
            case PredicateRelevantVariables:
              predicateRelevantVariablesLocations.add(candidateNode);
              tdcgData.addTimestamp(getTempPredicateRelevantVariablesCoverage(cfa));
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

  public void resetPredicateRelevantVariablesNodes() {
    previousPredicateRelevantVariablesLocationsSize = predicateRelevantVariablesLocations.size();
    predicateRelevantVariablesLocations.clear();
  }

  public double getTempPredicateConsideredCoverage(CFA cfa) {
    return getTempCoverage(cfa, TimeDependentCoverageType.PredicateConsideredLocations);
  }

  public double getTempPredicateRelevantVariablesCoverage(CFA cfa) {
    return getTempCoverage(cfa, TimeDependentCoverageType.PredicateRelevantVariables);
  }

  public Set<CFANode> getPredicateConsideredLocations() {
    return predicateConsideredLocations;
  }

  public Set<CFANode> getPredicateRelevantConsideredLocations() {
    return predicateRelevantVariablesLocations;
  }

  public int getPredicateRelevantConsideredLocationsCount() {
    return Math.max(
        predicateRelevantVariablesLocations.size(),
        previousPredicateRelevantVariablesLocationsSize);
  }

  public Multiset<String> getVariableNames() {
    return variableNames;
  }

  public Multiset<String> getRelevantVariableNames() {
    return relevantVariableNames;
  }

  private double getTempCoverage(CFA cfa, TimeDependentCoverageType type) {
    int numTotalNodes = cfa.getAllNodes().size();
    int numRelevantNodes = 0;
    switch (type) {
      case PredicateConsideredLocations:
        numRelevantNodes = predicateConsideredLocations.size();
        break;
      case PredicateRelevantVariables:
        numRelevantNodes = predicateRelevantVariablesLocations.size();
        break;
      default:
        break;
    }
    return numRelevantNodes / (double) numTotalNodes;
  }
}
