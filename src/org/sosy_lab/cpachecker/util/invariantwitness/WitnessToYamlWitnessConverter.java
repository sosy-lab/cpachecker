// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Edge;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

public class WitnessToYamlWitnessConverter {
  private final LogManager logger;
  private final boolean writeLocationInvariants;

  public WitnessToYamlWitnessConverter(LogManager pLogger) {
    this(pLogger, false);
  }

  public WitnessToYamlWitnessConverter(LogManager pLogger, boolean pWriteLocationInvariants) {
    logger = pLogger;
    writeLocationInvariants = pWriteLocationInvariants;
  }

  public ImmutableList<InvariantWitness> convertProofWitness(Witness pWitness) {
    Preconditions.checkState(pWitness.getWitnessType().equals(WitnessType.CORRECTNESS_WITNESS));
    ImmutableSet.Builder<InvariantWitness> builder = ImmutableSet.builder();
    for (String invexpstate : pWitness.getInvariantExportStates()) {
      ExpressionTree<Object> invariantExpression = pWitness.getStateInvariant(invexpstate);

      // True invariants do not add any information in order to proof the program
      if (invariantExpression.equals(ExpressionTrees.getTrue())) {
        continue;
      }

      boolean isLoopHead =
          pWitness.getEnteringEdges().get(invexpstate).stream()
              .anyMatch(
                  x ->
                      "true".equalsIgnoreCase(x.getLabel().getMapping().get(KeyDef.ENTERLOOPHEAD)));
      if (isLoopHead) {
        builder.addAll(handleLoopInvariant(invariantExpression, invexpstate, pWitness));
      } else if (writeLocationInvariants) {
        builder.addAll(handleLocationInvariant(invariantExpression, invexpstate, pWitness));
      }
    }

    return builder.build().asList();
  }

  private Set<InvariantWitness> handleLoopInvariant(
      ExpressionTree<Object> pInvariantExpression, String pInvexpstate, Witness pWitness) {
    // Loop Invariants should be at the loop head i.e. at the statement where the CFA
    // says there is a Loop Start node. This cannot be done through the witness,
    // we therefore project the witness away and work diorectly on the CFA.
    // The semantics are when a node is matched in the witness only when coming from the
    // incoming edge, therefore edges should be at the loop heads.
    ImmutableSet<CFANode> cfaNodes =
        FluentIterable.from(pWitness.getARGStatesFor(pInvexpstate))
            .transform(x -> AbstractStates.asIterable(x).filter(LocationState.class))
            .stream()
            .flatMap(x -> x.stream())
            .map(x -> x.getLocationNode())
            .collect(ImmutableSet.toImmutableSet());

    Set<InvariantWitness> invariants = new HashSet<>();

    CFA cfa = pWitness.getCfa();
    if (cfa.getLoopStructure().isEmpty()) {
      logger.log(
          Level.WARNING,
          "Could not export the Loop Invariant, since Loop Structures have been disabled in the"
              + " CFA!");
      return invariants;
    }

    Set<Loop> allPossibleLoops = new HashSet<>();
    for (CFANode node : cfaNodes) {
      // Since we now that the CFANode we have is very close to the actual Loop head node
      // we need to find the Loop which is the smallest possible, but still contains the CFANode
      // in question. Since this will be the for which the invariant should hold
      int minimalLoopSize = Integer.MAX_VALUE;
      Optional<Loop> tightestFittingLoop = Optional.empty();
      for (Loop loop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
        // The node can also be present in the leaving edges, since the invariant should also be
        // valid if we are not even executing the loop once
        if ((loop.getLoopNodes().contains(node)
                || FluentIterable.from(loop.getOutgoingEdges())
                    .transform(e -> e.getSuccessor())
                    .anyMatch(n -> n == node))
            && loop.getLoopNodes().size() < minimalLoopSize) {
          tightestFittingLoop = Optional.of(loop);
          minimalLoopSize = loop.getLoopNodes().size();
        }
      }
      if (tightestFittingLoop.isPresent()) {
        allPossibleLoops.add(tightestFittingLoop.orElseThrow());
      }
    }

    for (Loop loop : allPossibleLoops) {
      // For loops the edges leaving the loop heads are the ones usually containing either
      // a blank edge or the loop boundary condition. Therefore they contain the line where
      // the loop head is actually present
      ImmutableSet<CFAEdge> leavingEdges =
          FluentIterable.from(loop.getLoopHeads()).transform(CFAUtils::leavingEdges).stream()
              .flatMap(x -> x.stream())
              .collect(ImmutableSet.toImmutableSet());

      for (CFAEdge edge : leavingEdges) {
        FileLocation loc = edge.getFileLocation();
        if (loc == FileLocation.DUMMY || loc == FileLocation.MULTIPLE_FILES) {
          continue;
        }

        invariants.add(
            new InvariantWitness(
                pInvariantExpression, edge.getFileLocation(), edge.getPredecessor()));
      }
    }

    return invariants;
  }

  private Set<InvariantWitness> handleLocationInvariant(
      ExpressionTree<Object> pInvariantExpression, String pInvexpstate, Witness pWitness) {
    // To handle location invariants, we need to discover which statement they come from
    ImmutableSet<CFAEdge> enteringEdges;
    Set<InvariantWitness> invariants = new HashSet<>();

    List<Edge> enteringEdgeWitness = (List<Edge>) pWitness.getEnteringEdges().get(pInvexpstate);
    for (Edge e : enteringEdgeWitness) {
      if (e.getLabel().getMapping().containsKey(KeyDef.CONTROLCASE)) {
        // If they come from only a single branch of a if statement, then using the Witness
        // to discover where they come from is hard, therefore we need to use the CFA
        ImmutableSet<CFANode> cfaNodesCandidates =
            FluentIterable.from(pWitness.getARGStatesFor(pInvexpstate))
                .transform(x -> AbstractStates.asIterable(x).filter(LocationState.class))
                .stream()
                .flatMap(x -> x.stream())
                .map(x -> x.getLocationNode())
                .collect(ImmutableSet.toImmutableSet());

        // Remove all nodes which can be reached from the other nodes, since
        // we want the earliest possible position for the invariant. This needs
        // to be done since the mapping between Witness and CFA is wrong
        // in multiple cases and contains superflows nodes/edges
        Set<CFANode> cfaNodes = new HashSet<>();
        for (CFANode n : cfaNodesCandidates) {
          if (!cfaNodesCandidates.stream()
              .map(CFAUtils::leavingEdges)
              .flatMap(x -> x.stream())
              .map(x -> x.getSuccessor())
              .anyMatch(x -> x == n)) {
            cfaNodes.add(n);
          }
        }

        enteringEdges =
            FluentIterable.from(cfaNodes).transform(CFAUtils::enteringEdges).stream()
                .flatMap(x -> x.stream())
                .collect(ImmutableSet.toImmutableSet());

      } else {
        // If they do not come from if statements and are merely present, then we need to use
        // the GraphML format
        enteringEdges = ImmutableSet.copyOf(pWitness.getCFAEdgeFor(e));
      }

      if (enteringEdges.size() != 1) {
        logger.logf(
            Level.WARNING,
            "Expected one CFA entering edge matching the location invariant in the witness, but"
                + " identified %d!",
            enteringEdges.size());
      }

      for (CFAEdge edge : enteringEdges) {
        FileLocation loc = edge.getFileLocation();
        if (loc == FileLocation.DUMMY || loc == FileLocation.MULTIPLE_FILES) {
          continue;
        }

        invariants.add(new InvariantWitness(pInvariantExpression, loc, edge.getSuccessor()));
      }
    }

    return invariants;
  }
}
