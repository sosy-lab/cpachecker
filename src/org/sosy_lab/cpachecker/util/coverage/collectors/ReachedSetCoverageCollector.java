// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.collectors;

import static com.google.common.base.Predicates.notNull;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.AbstractBAMCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.coverage.data.FileCoverageStatistics;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;

public class ReachedSetCoverageCollector extends CoverageCollector {

  ReachedSetCoverageCollector(
      Map<String, FileCoverageStatistics> pInfosPerFile,
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler,
      CFA cfa) {
    super(pInfosPerFile, pCoverageMeasureHandler, pTimeDependentCoverageHandler, cfa);
  }

  public void collectFromReachedSet(
      UnmodifiableReachedSet reachedSet, CFA cfa, ConfigurableProgramAnalysis cpa) {
    FluentIterable<AbstractState> reached = FluentIterable.from(reachedSet);
    // hack to get all reached states for BAM
    if (cpa instanceof AbstractBAMCPA) {
      Collection<ReachedSet> otherReachedSets =
          ((AbstractBAMCPA) cpa).getData().getCache().getAllCachedReachedStates();
      reached = reached.append(FluentIterable.concat(otherReachedSets));
    }

    // Add information about visited functions
    for (FunctionEntryNode entryNode :
        AbstractStates.extractLocations(reached)
            .filter(notNull())
            .filter(FunctionEntryNode.class)) {

      final FileLocation loc = entryNode.getFileLocation();
      if (loc.getStartingLineNumber() == 0) {
        // dummy location
        continue;
      }

      addVisitedFunction(entryNode);
    }

    collectVisitedEdges(reached);
    addExistingNodes(cfa);
    addReachedNodes(getReachedNodes(reached));
  }

  private void collectVisitedEdges(Iterable<AbstractState> reached) {
    Set<CFANode> reachedNodes =
        FluentIterable.from(reached)
            .transform(AbstractStates::extractLocation)
            .filter(notNull())
            .toSet();
    // Add information about visited locations

    for (AbstractState state : reached) {
      ARGState argState = AbstractStates.extractStateByType(state, ARGState.class);
      if (argState != null) {
        for (ARGState child : argState.getChildren()) {
          // Do not specially check child.isCovered, as the edge to covered state also should be
          // marked as covered edge
          List<CFAEdge> edges = argState.getEdgesToChild(child);
          if (edges.size() > 1) {
            for (CFAEdge innerEdge : edges) {
              addVisitedEdge(innerEdge);
            }

            // BAM produces paths with no edge connection thus the list will be empty
          } else if (!edges.isEmpty()) {
            addVisitedEdge(Iterables.getOnlyElement(edges));
          }
        }
      } else {
        // Simple kind of analysis
        // Cover all edges from reached nodes
        // It is less precise, but without ARG it is impossible to know what path we chose
        CFANode node = AbstractStates.extractLocation(state);
        for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
          if (reachedNodes.contains(edge.getSuccessor())) {
            addVisitedEdge(edge);
          }
        }
      }
    }
  }

  public Multiset<CFANode> getReachedNodes(Iterable<AbstractState> reached) {
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

  private List<CFANode> extractBlockNodes(CFANode locationNode) {
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
}
