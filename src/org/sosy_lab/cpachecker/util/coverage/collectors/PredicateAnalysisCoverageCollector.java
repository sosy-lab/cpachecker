// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.collectors;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageType;

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
  private final Set<String> predicateAbstractionVariables = new HashSet<>();
  private final CFA cfa;
  private ImmutableSet<CFANode> oldPredicateRelevantVariablesLocations = ImmutableSet.of();

  PredicateAnalysisCoverageCollector(
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler,
      CFA pCfa) {
    super(pCoverageMeasureHandler, pTimeDependentCoverageHandler, pCfa);
    cfa = pCfa;
  }

  public void addPredicateConsideredLocation(CFAEdge pEdge) {
    addCoverageLocation(predicateConsideredLocations, pEdge);
  }

  public void addPredicateRelevantVariablesLocation(CFAEdge pEdge) {
    addCoverageLocation(predicateRelevantVariablesLocations, pEdge);
  }

  public void addRelevantAbstractionVariables(Set<String> pVariableNames) {
    predicateAbstractionVariables.addAll(pVariableNames);
  }

  public void resetPredicateRelevantVariablesLocations() {
    oldPredicateRelevantVariablesLocations =
        ImmutableSet.copyOf(predicateRelevantVariablesLocations);
    predicateRelevantVariablesLocations.clear();
  }

  public double getTempPredicateConsideredCoverage() {
    return getTempCoverage(TimeDependentCoverageType.PredicateConsideredLocations);
  }

  public double getTempPredicateRelevantVariablesCoverage() {
    return getTempCoverage(TimeDependentCoverageType.PredicateRelevantVariables);
  }

  public double getTempPredicateAbstractionVariablesCoverage() {
    if (getAllVariables().isEmpty()) {
      addAllProgramVariables();
    }
    return getPredicateAbstractionVariables().size() / (double) getAllVariables().size();
  }

  public Set<CFANode> getPredicateConsideredLocations() {
    return Collections.unmodifiableSet(predicateConsideredLocations);
  }

  public Set<CFANode> getPredicateRelevantConsideredLocations() {
    if (predicateRelevantVariablesLocations.size()
        > oldPredicateRelevantVariablesLocations.size()) {
      return Collections.unmodifiableSet(predicateRelevantVariablesLocations);
    } else {
      return Collections.unmodifiableSet(oldPredicateRelevantVariablesLocations);
    }
  }

  public int getPredicateRelevantConsideredLocationsCount() {
    return getPredicateRelevantConsideredLocations().size();
  }

  public Set<String> getPredicateAbstractionVariables() {
    return Collections.unmodifiableSet(predicateAbstractionVariables);
  }

  private void addCoverageLocation(Set<CFANode> pLocations, CFAEdge pEdge) {
    if (pLocations.isEmpty()) {
      pLocations.add(pEdge.getPredecessor());
    }
    if (pLocations.contains(pEdge.getPredecessor())
        || pEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
      pLocations.add(pEdge.getSuccessor());
    }
  }

  private double getTempCoverage(TimeDependentCoverageType type) {
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
