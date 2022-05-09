// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.coverage.AnalysisIndependentCoverageCPA;
import org.sosy_lab.cpachecker.cpa.coverage.PredicateCoverageCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class CoverageUtility {

  public static Multiset<CFANode> getReachedNodes(Iterable<AbstractState> reached) {
    Multiset<CFANode> locations = LinkedHashMultiset.create();
    for (AbstractState state : reached) {
      ARGState argState = AbstractStates.extractStateByType(state, ARGState.class);
      if (argState != null) {
        LocationState locState =
            AbstractStates.extractStateByType(argState.getWrappedState(), LocationState.class);
        if (locState != null) {
          CFANode locationNode = locState.getLocationNode();
          locations.addAll(extractBlockNodes(locationNode));
        }
      }
    }
    return locations;
  }

  private static List<CFANode> extractBlockNodes(CFANode locationNode) {
    List<CFAEdge> currentComboEdge = null;
    List<CFANode> nodes = new ArrayList<>();
    CFANode currentNode = locationNode;
    nodes.add(locationNode);
    do {
      if (currentNode.isLoopStart()
          || (currentNode.getNumEnteringEdges() != 1)
          || (currentNode.getNumLeavingEdges() != 1)
          || (currentComboEdge != null
              && !currentNode.equals(
                  currentComboEdge.get(currentComboEdge.size() - 1).getSuccessor()))
          || (currentNode.getLeavingEdge(0).getEdgeType() == CFAEdgeType.CallToReturnEdge)
          || (currentNode.getLeavingEdge(0).getEdgeType() == CFAEdgeType.AssumeEdge)) {
        currentComboEdge = null;
        if (nodes.size() > 1) {
          nodes.remove(nodes.size() - 1);
        }
      } else {
        CFAEdge leavingEdge = currentNode.getLeavingEdge(0);
        if (currentComboEdge == null) {
          currentComboEdge = new ArrayList<>();
        }
        currentComboEdge.add(leavingEdge);
        currentNode = leavingEdge.getSuccessor();
        nodes.add(currentNode);
      }
    } while (currentComboEdge != null);
    return nodes;
  }

  public static CoverageData extractTimeDependentCoverageData(ConfigurableProgramAnalysis cpa) {
    CoverageData coverageData = new CoverageData();
    boolean usedPredicateCoverageCPA = false;

    if (cpa instanceof ARGCPA) {
      ImmutableList<ConfigurableProgramAnalysis> cpas = ((ARGCPA) cpa).getWrappedCPAs();
      for (var compositeCPA : cpas) {
        if (compositeCPA instanceof CompositeCPA) {
          ImmutableList<ConfigurableProgramAnalysis> wrappedCPAs =
              ((CompositeCPA) compositeCPA).getWrappedCPAs();
          for (var wrappedCPA : wrappedCPAs) {
            if (wrappedCPA instanceof PredicateCoverageCPA) {
              usedPredicateCoverageCPA = true;
              coverageData = ((PredicateCoverageCPA) wrappedCPA).getCoverageData();
            }
          }
          for (var wrappedCPA : wrappedCPAs) {
            if (wrappedCPA instanceof AnalysisIndependentCoverageCPA) {
              CoverageData analysisIndependentCoverageData =
                  ((AnalysisIndependentCoverageCPA) wrappedCPA).getCoverageData();
              if (usedPredicateCoverageCPA) {
                coverageData
                    .getTDCGHandler()
                    .mergeData(analysisIndependentCoverageData.getTDCGHandler());
                coverageData
                    .getCoverageHandler()
                    .mergeData(analysisIndependentCoverageData.getCoverageHandler());
                coverageData.mergeInfosPerFile(analysisIndependentCoverageData);
              } else {
                coverageData = analysisIndependentCoverageData;
              }
            }
          }
        }
      }
    }
    return coverageData;
  }

  public static CoverageData getCoverageDataFromReachedSet(UnmodifiableReachedSet pReached) {
    CoverageData coverageData = new CoverageData();
    if (pReached instanceof PartitionedReachedSet) {
      ConfigurableProgramAnalysis cpa = ((PartitionedReachedSet) pReached).getCPA();
      coverageData = CoverageUtility.extractTimeDependentCoverageData(cpa);
    }
    return coverageData;
  }

  public static boolean coversLine(CFAEdge pEdge) {
    FileLocation loc = pEdge.getFileLocation();
    if (loc.getStartingLineNumber() == 0) {
      // dummy location
      return false;
    }
    if (pEdge instanceof ADeclarationEdge
        && (((ADeclarationEdge) pEdge).getDeclaration() instanceof AFunctionDeclaration)) {
      // Function declarations span the complete body, this is not desired.
      return false;
    }
    return true;
  }
}
