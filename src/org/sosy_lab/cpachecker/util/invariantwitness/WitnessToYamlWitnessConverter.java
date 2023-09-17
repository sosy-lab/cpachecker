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
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
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
                  pEdge ->
                      "true"
                          .equalsIgnoreCase(
                              pEdge.getLabel().getMapping().get(KeyDef.ENTERLOOPHEAD)));

      // Simplify the invariant such that reading it in afterwards is easier
      invariantExpression = ExpressionTrees.simplify(invariantExpression);

      // Remove CPAchecker internal values from the invariant expression
      invariantExpression = ExpressionTrees.removeCPAcheckerInternals(invariantExpression);

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
            .transformAndConcat(AbstractStates::asIterable)
            .filter(LocationState.class)
            .transform(pLocState -> pLocState.getLocationNode())
            .toSet();

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
          FluentIterable.from(loop.getLoopHeads())
              .transformAndConcat(CFAUtils::leavingEdges)
              .toSet();

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
      // We ignore all invariants which depend on the internal of CPAchecker to be useful
      if (FluentIterable.from(pWitness.getCFAEdgeFor(e))
          .filter(AssumeEdge.class)
          .anyMatch(pEdge -> pEdge.getExpression().toString().matches(".*__CPAchecker_TMP.*"))) {
        logger.logf(
            Level.WARNING,
            "Ignoring invariant at node %s with edge %s in the Witness due to the edge which enters"
                + " the state in the witness containing a dependency on CPAchecker internal"
                + " datastructures!",
            pInvexpstate,
            e.toString());
        continue;
      }

      // We ignore all invariants which are inside a loop which already contains a loop invariant
      // and are not used in a lot of places in the ARG
      if (FluentIterable.from(pWitness.getEnteringEdges().get(e.getSource()))
              .allMatch(
                  pEdge ->
                      "true"
                          .equalsIgnoreCase(
                              pEdge.getLabel().getMapping().get(KeyDef.ENTERLOOPHEAD)))
          && ("condition-true".equalsIgnoreCase(e.getLabel().getMapping().get(KeyDef.CONTROLCASE))
              || "condition-false"
                  .equalsIgnoreCase(e.getLabel().getMapping().get(KeyDef.CONTROLCASE)))
          && FluentIterable.from(pWitness.getLeavingEdges().get(e.getTarget()))
              .allMatch(
                  pEdge ->
                      "true"
                          .equalsIgnoreCase(
                              pEdge.getLabel().getMapping().get(KeyDef.ENTERLOOPHEAD)))
          && FluentIterable.from(pWitness.getARGStatesFor(pInvexpstate))
                  .transformAndConcat(AbstractStates::asIterable)
                  .filter(LocationState.class)
                  .transform(pLocState -> pLocState.getLocationNode())
                  .size()
              <= 3) {
        logger.logf(
            Level.WARNING,
            "Ignoring invariant at node %s with edge %s in the Witness due to the edge which enters"
                + " it being a weaker invariant than the one provided at the loop head"
                + " datastructures!",
            pInvexpstate,
            e.toString());
        continue;
      }

      // We handle entering functions the same way we handle entering and if branch
      if (e.getLabel().getMapping().containsKey(KeyDef.CONTROLCASE)
          || e.getLabel().getMapping().containsKey(KeyDef.FUNCTIONENTRY)) {
        // If they come from only a single branch of a if statement, then using the Witness
        // to discover where they come from is hard, therefore we need to use the CFA
        ImmutableSet<CFANode> cfaNodesCandidates =
            FluentIterable.from(pWitness.getARGStatesFor(pInvexpstate))
                .transformAndConcat(AbstractStates::asIterable)
                .filter(LocationState.class)
                .transform(pLocState -> pLocState.getLocationNode())
                .toSet();

        // We need to differentiate between nodes which call a function and those which do not,
        // since the normal matching returns the last possible node where the invariant is valid
        // which is already inside the function and therefore not valid
        if (cfaNodesCandidates.stream().anyMatch(x -> x instanceof FunctionEntryNode)) {
          // When we call a function we want the edge which enters the function
          enteringEdges =
              cfaNodesCandidates.stream()
                  .map(CFAUtils::leavingEdges)
                  .flatMap(pEdge -> pEdge.stream())
                  .filter(pEdge -> pEdge instanceof FunctionCallEdge)
                  .collect(ImmutableSet.toImmutableSet());
        } else {
          if (pWitness.getLeavingEdges().get(pInvexpstate).stream()
              .anyMatch(x -> x.getLabel().getMapping().containsKey(KeyDef.CONTROLCASE))) {
            // If the leaving edges are control edges we want all nodes which do not contain
            // any AssumeEdge leaving it. Since these are probably the ones which match
            // the leaving edges of the state
            cfaNodesCandidates =
                cfaNodesCandidates.stream()
                    .filter(
                        pNode ->
                            CFAUtils.enteringEdges(pNode).stream()
                                .noneMatch(pEdge -> pEdge instanceof CAssumeEdge))
                    .collect(ImmutableSet.toImmutableSet());
          }

          // Get the last possible node in which the invariant is valid.
          // This needs to be done, because sometimes declarations or other
          // things are needed to express the invariant, but also match the
          // Witness state
          // TODO: The actual fix would be to find the first state where all the variables used in
          // the invariant have been declared
          // The last node cannot be a functionExitNode, since this would mean that the invariant
          // overapproximates a return statement, which does nothing. This doesn't make sense
          // semantically.
          cfaNodesCandidates =
              cfaNodesCandidates.stream()
                  .filter(pNode -> !(pNode instanceof FunctionExitNode))
                  .collect(ImmutableSet.toImmutableSet());

          Set<CFANode> cfaNodes = new HashSet<>();
          for (CFANode node : cfaNodesCandidates) {
            if (cfaNodesCandidates.stream()
                .map(CFAUtils::enteringEdges)
                .flatMap(pEdge -> pEdge.stream())
                .map(pEdge -> pEdge.getPredecessor())
                .noneMatch(pNode -> pNode == node)) {
              cfaNodes.add(node);
            }
          }

          enteringEdges =
              FluentIterable.from(cfaNodes).transformAndConcat(CFAUtils::enteringEdges).toSet();
        }
      } else {
        // If they do not come from if statements and are merely present, then we need to use
        // the GraphML format
        // This case may or may not be obsolete currently. But since there is no clear analysis of
        // what is present in the GraphML witnesses and is mostly done through heuristics, this case
        // will remain until there is a clear indication that it cannot be reached or that it
        // generates errors.
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
        CFANode node;
        if (edge instanceof FunctionCallEdge) {
          node = edge.getPredecessor();
        } else {
          node = edge.getSuccessor();
        }

        FileLocation loc = edge.getFileLocation();
        if (loc == FileLocation.DUMMY || loc == FileLocation.MULTIPLE_FILES) {
          continue;
        }

        invariants.add(new InvariantWitness(pInvariantExpression, loc, node));
      }
    }

    return invariants;
  }
}
