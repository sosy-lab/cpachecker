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
import com.google.common.collect.Multisets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.coverage.measures.CoverageMeasureHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;

/**
 * Coverage collector which is called at the end of an analysis. The calculated coverage measures
 * here only depends on the reached set.
 */
public class ReachedSetCoverageCollector extends CoverageCollector {
  private final Multiset<CFANode> reachedLocations = LinkedHashMultiset.create();

  ReachedSetCoverageCollector(
      CoverageMeasureHandler pCoverageMeasureHandler,
      TimeDependentCoverageHandler pTimeDependentCoverageHandler,
      CFA cfa) {
    super(pCoverageMeasureHandler, pTimeDependentCoverageHandler, cfa);
  }

  public void collectFromReachedSet(
      UnmodifiableReachedSet reachedSet, ConfigurableProgramAnalysis cpa) {
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
    addReachedNodes(reached);
  }

  public void addReachedNodes(Iterable<AbstractState> reached) {
    Multiset<CFANode> locations = LinkedHashMultiset.create();
    for (AbstractState state : reached) {
      for (CFANode node : AbstractStates.extractLocations(state)) {
        locations.addAll(extractNodesFromAggregatedBasicBlock(node));
      }
    }
    reachedLocations.addAll(locations);
  }

  public Multiset<CFANode> getReachedLocations() {
    return Multisets.unmodifiableMultiset(reachedLocations);
  }

  /**
   * Gets a CFA node and extracts all its predecessors belonging to its aggregated basic block.
   *
   * @param locationNode CFA Node starting node regarding the extraction of the other nodes
   * @return a list of CFA nodes which builds together an aggregated basic block ending at the input
   *     node.
   */
  private List<CFANode> extractNodesFromAggregatedBasicBlock(CFANode locationNode) {
    List<CFAEdge> currentAggregationBlock = null;
    List<CFANode> cfaNodes = new ArrayList<>();
    CFANode currentNode = locationNode;
    cfaNodes.add(locationNode);
    do {
      if (currentNode.isLoopStart()
          || (currentNode.getNumEnteringEdges() != 1)
          || (currentAggregationBlock != null
              && !currentNode.equals(
                  currentAggregationBlock.get(currentAggregationBlock.size() - 1).getPredecessor()))
          || (currentNode.getEnteringEdge(0).getEdgeType() == CFAEdgeType.CallToReturnEdge)
          || (currentNode.getEnteringEdge(0).getEdgeType() == CFAEdgeType.AssumeEdge)) {
        currentAggregationBlock = null;
        if (cfaNodes.size() > 1) {
          cfaNodes.remove(cfaNodes.size() - 1);
        }
      } else {
        CFAEdge enteringEdge = currentNode.getEnteringEdge(0);
        if (currentAggregationBlock == null) {
          currentAggregationBlock = new ArrayList<>();
        }
        currentAggregationBlock.add(enteringEdge);
        currentNode = enteringEdge.getPredecessor();
        cfaNodes.add(currentNode);
      }
    } while (currentAggregationBlock != null);
    return cfaNodes;
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
}
