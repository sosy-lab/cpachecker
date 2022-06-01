// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.collectors;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;
import org.sosy_lab.cpachecker.util.coverage.util.CoverageUtility;

/**
 * Coverage collector which is called used by the CoverageCPA. The calculated coverage measures
 * depends on data which is collected during an analysis. Since this class collects data during the
 * analysis it is possible to calculate a temporal coverage value based on partially available data.
 * Therefore, the data collected in this class is also suitable to be used for a TDCG.
 */
public class AnalysisIndependentCoverageCollector extends CoverageCollector {
  private final Multiset<CFANode> visitedLocations = LinkedHashMultiset.create();
  private final Set<String> visitedVariables = new HashSet<>();
  private final Set<String> allVariables = new HashSet<>();
  private final CFA cfa;

  AnalysisIndependentCoverageCollector(
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler,
      CFA pCfa) {
    super(pCoverageMeasureHandler, pTimeDependentCoverageHandler, pCfa);
    cfa = pCfa;
    addInitialNodesForMeasures();
  }

  public void addInitialNodesForMeasures() {
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

  public void addAllProgramVariables() {
    for (CFANode node : cfa.getAllNodes()) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        Optional<String> variable = getNewVariableFromCFAEdge(edge);
        if (variable.isPresent()) {
          allVariables.add(variable.orElseThrow());
        }
      }
    }
  }

  public void addVisitedVariables(final CFAEdge pEdge) {
    if (!CoverageUtility.coversLine(pEdge)) {
      return;
    }
    Optional<String> variable = getNewVariableFromCFAEdge(pEdge);
    if (variable.isPresent()) {
      visitedVariables.add(variable.orElseThrow());
    }
  }

  public Set<String> getAllVariables() {
    return allVariables;
  }

  public Set<String> getVisitedVariables() {
    return visitedVariables;
  }

  private Optional<String> getNewVariableFromCFAEdge(CFAEdge edge) {
    if (edge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      CDeclarationEdge declarationEdge = (CDeclarationEdge) edge;
      CDeclaration dec = declarationEdge.getDeclaration();
      String variableName = dec.getQualifiedName();
      if (variableName != null
          && !variableName.contains("__CPAchecker_TMP_")
          && variableName.contains("::")) {
        return Optional.of(dec.getQualifiedName());
      }
    }
    return Optional.empty();
  }
}
