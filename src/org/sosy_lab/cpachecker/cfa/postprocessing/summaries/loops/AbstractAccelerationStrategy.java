// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class AbstractAccelerationStrategy extends LoopStrategy {

  public AbstractAccelerationStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, StrategiesEnum.HAVOCSTRATEGY, pCFA);
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode beforeWhile) {
    List<CFAEdge> filteredOutgoingEdges =
        this.summaryFilter.getEdgesForStrategies(
            beforeWhile.getLeavingEdges(),
            new HashSet<>(Arrays.asList(StrategiesEnum.BASE, this.strategyEnum)));

    if (filteredOutgoingEdges.size() != 1) {
      return Optional.empty();
    }

    if (!filteredOutgoingEdges.get(0).getDescription().equals("while")) {
      return Optional.empty();
    }

    CFANode loopStartNode = filteredOutgoingEdges.get(0).getSuccessor();

    Optional<Loop> loopMaybe = summaryInformation.getLoop(loopStartNode);
    if (loopMaybe.isEmpty()) {
      return Optional.empty();
    }

    Loop loop = loopMaybe.orElseThrow();

    // Function calls may change global variables, or have assert statements, which cannot be
    // summarized correctly
    if (loop.containsUserDefinedFunctionCalls()) {
      return Optional.empty();
    }

    // Set<AVariableDeclaration> modifiedVariables = loop.getModifiedVariables();
    // PathConstraints constraints = calculateConstraints(loop, loopStartNode);

    return Optional.empty();
  }

  private PathConstraints calculateConstraints(Loop loop, CFANode loopStartNode) {
    return calculateConstraints(loop, loopStartNode, 0);
  }

  private PathConstraints calculateConstraints(Loop loop, CFANode loopStartNode, int counter) {
    int partitionCounter = counter;
    List<CFANode> waitlist = new ArrayList<>();
    waitlist.add(loopStartNode);
    Map<CFANode, PathConstraints> constraintMap = new HashMap<>();
    constraintMap.put(loopStartNode, new PathConstraints(partitionCounter++));
    while (!waitlist.isEmpty()) {
      sortWaitlist(waitlist, constraintMap);
      CFANode current = waitlist.remove(0);
      assert !current.isLoopStart() || current == loopStartNode; //  we deal with nested loops later
      PathConstraints currentConstraint = constraintMap.get(current);
      Set<CFAEdge> leaving = ImmutableSet.copyOf(current.getLeavingEdges());
      Set<CFAEdge> filteredLeaving = Sets.intersection(leaving, loop.getInnerLoopEdges());
      assert leaving.size() == filteredLeaving.size() || current == loopStartNode;
      int multiplicity = filteredLeaving.size();
      assert multiplicity <= 2 && multiplicity >= 1;
      List<Integer> availablePartitions = new ArrayList<>();
      Optional<Entry<ImmutableSet<Integer>, ImmutableSet<Integer>>> newConstraint =
          Optional.empty();
      if (multiplicity > 1) {
        ImmutableSet<Integer> basePartition = currentConstraint.currentPartition;
        for (int i = 0; i < multiplicity; i++) {
          availablePartitions.add(partitionCounter++);
        }
        newConstraint =
            Optional.of(Map.entry(basePartition, ImmutableSet.copyOf(availablePartitions)));
      }
      for (CFAEdge e : filteredLeaving) {
        CFANode successor = e.getSuccessor();
        PathConstraints newPathConstraint =
            currentConstraint.transfer(e, availablePartitions, newConstraint);
        if (constraintMap.containsKey(successor)) {
          PathConstraints other = constraintMap.get(successor);
          constraintMap.put(successor, newPathConstraint.merge(other));
        } else {
          constraintMap.put(successor, newPathConstraint);
        }
        if (successor != loopStartNode && !waitlist.contains(successor)) {
          waitlist.add(successor);
        }
      }
    }
    return null;
  }

  private void sortWaitlist(List<CFANode> waitlist, Map<CFANode, PathConstraints> constraintMap) {
    while (true) {
      CFANode firstState = waitlist.get(0);
      if (firstState.getNumEnteringEdges() == 1 || firstState.isLoopStart()) {
        break;
      } else {
        boolean OK = true;
        for (int i = 0; i < firstState.getNumEnteringEdges(); i++) {
          if (!constraintMap.containsKey(firstState.getEnteringEdge(i).getPredecessor())) {
            OK = false;
            break;
          }
        }
        if (!OK) {
          waitlist.add(waitlist.remove(0)); // move to end
        } else {
          break;
        }
      }
    }
  }

  /**
   * This is an abstract domain to track constraints about execution frequency of statements inside
   * a loop. The idea is to have a bunch of symbolic counters (identified by integers). This domain
   * consists of 3 components.
   *
   * <p>Each time we branch a counter a0 splits into two new ones a1 and a2, and we save the
   * constraint a0=a1+a2 by adding a0->{a1,a2} into the constraint set (first component).
   *
   * <p>We track the current symbolic counter in the second component. Upon merging, we look up
   * whether the current counters can be resolved via the constraints in the first component (e.g.
   * merging a1 and a2 will yield a0), otherwise we just store their sum, indicated by putting them
   * in the same set.
   *
   * <p>In the third component we track for each statement in the loop how often it is executed, by
   * assigning it to a set of symbolic counters. For example if the statement "l27: x = x+1" is
   * mapped to {a1,a2,a5}, this means it is executed a1+a2+a5 times.
   */
  class PathConstraints {
    private final ImmutableMap<ImmutableSet<Integer>, ImmutableSet<Integer>> partitionConstraints;
    private final ImmutableSet<Integer> currentPartition;
    private final ImmutableMap<CFAEdge, ImmutableSet<Integer>> edgeToPartition;

    public PathConstraints(int init) {
      this(ImmutableMap.of(), ImmutableSet.of(init), ImmutableMap.of());
    }

    public PathConstraints(PathConstraints p) {
      partitionConstraints = p.partitionConstraints;
      currentPartition = p.currentPartition;
      edgeToPartition = p.edgeToPartition;
    }

    public PathConstraints(
        ImmutableMap<ImmutableSet<Integer>, ImmutableSet<Integer>> pPartitionConstraints,
        ImmutableSet<Integer> pCurrentPartition,
        ImmutableMap<CFAEdge, ImmutableSet<Integer>> pStatementToPartition) {
      partitionConstraints = pPartitionConstraints;
      currentPartition = pCurrentPartition;
      edgeToPartition = pStatementToPartition;
    }

    public PathConstraints merge(PathConstraints other) {
      ImmutableSet<Integer> jointPartition =
          ImmutableSet.<Integer>builder()
              .addAll(this.currentPartition)
              .addAll(other.currentPartition)
              .build();
      for (ImmutableSet<Integer> key : this.partitionConstraints.keySet()) {
        if (this.partitionConstraints.get(key).equals(jointPartition)) {
          jointPartition = key;
          break;
        }
      }
      ImmutableMap<CFAEdge, ImmutableSet<Integer>> jointEdgeMap =
          mergeMaps(this.edgeToPartition, other.edgeToPartition);
      ImmutableMap<ImmutableSet<Integer>, ImmutableSet<Integer>> jointPartitionConstraints =
          mergeMaps(this.partitionConstraints, other.partitionConstraints);
      return new PathConstraints(jointPartitionConstraints, jointPartition, jointEdgeMap);
    }

    private <K, V> ImmutableMap<K, V> mergeMaps(ImmutableMap<K, V> a, ImmutableMap<K, V> b) {
      return ImmutableMap.<K, V>builder()
          .putAll(a)
          .putAll(Maps.difference(b, a).entriesOnlyOnLeft())
          .build();
    }

    /**
     * Calculate the successor path constraints when following a CFAEdge. In case we are at a
     * branching of the CFA, the provided list of partitions will be non empty and we remove and use
     * one element of that list as our new partition. Otherwise we reuse the current partition. The
     * returned PathConstraints object will then contain in the mapping an entry for the CFAEdge to
     * the (potentially new) partition.
     */
    public PathConstraints transfer(
        CFAEdge edge,
        List<Integer> partitions,
        Optional<Entry<ImmutableSet<Integer>, ImmutableSet<Integer>>> newConstraint) {
      ImmutableSet<Integer> partitionToUse;
      if (partitions.size() > 0) {
        partitionToUse = ImmutableSet.of(partitions.remove(0));
      } else {
        assert !newConstraint.isPresent();
        partitionToUse = this.currentPartition;
      }
      ImmutableMap<CFAEdge, ImmutableSet<Integer>> newEdgeToPartition =
          ImmutableMap.<CFAEdge, ImmutableSet<Integer>>builder()
              .putAll(this.edgeToPartition)
              .put(edge, partitionToUse)
              .build();
      ImmutableMap<ImmutableSet<Integer>, ImmutableSet<Integer>> newPartitionConstraints;
      if (newConstraint.isPresent()) {
        newPartitionConstraints =
            ImmutableMap.<ImmutableSet<Integer>, ImmutableSet<Integer>>builder()
                .putAll(this.partitionConstraints)
                .put(newConstraint.orElseThrow())
                .build();
      } else {
        newPartitionConstraints = this.partitionConstraints;
      }
      return new PathConstraints(newPartitionConstraints, partitionToUse, newEdgeToPartition);
    }
  }
}
