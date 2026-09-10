// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;

/**
 * The rules for the propositions that are given by a specification automaton, i.e., for
 * unreach-call: the automaton decides where a violation is, so the rules do not need to know
 * anything about the error function of the task.
 *
 * <p>Both rules argue with the locations at which an automaton of the specification can report a
 * violation. This argument only holds for an automaton that matches the syntax of an edge, not for
 * one that asks another CPA about the state of the program ({@code CHECK(...)}), because such an
 * automaton can report a violation anywhere and reports none while no other CPA is present. The
 * rules are therefore restricted to the propositions for which CPAchecker itself provides the
 * automaton, and all of these match the syntax of an edge.
 */
final class ReachabilityRules {

  private ReachabilityRules() {}

  private static final ImmutableSet<Property> PROPOSITIONS =
      ImmutableSet.of(
          CommonVerificationProperty.REACHABILITY,
          CommonVerificationProperty.REACHABILITY_ERROR,
          CommonVerificationProperty.REACHABILITY_LABEL,
          CommonVerificationProperty.ASSERT);

  static ImmutableList<TrivialRule> rules() {
    return ImmutableList.of(
        new TrivialRule(
            "no-reachable-target-location",
            "A specification that is given by an automaton holds if the automaton cannot match: no"
                + " location that an execution of the program can reach is a target location of"
                + " the automaton.",
            PROPOSITIONS,
            ReachabilityRules::checkNoReachableTargetLocation),
        new TrivialRule(
            "violation-on-every-execution",
            "Every execution of the program executes the same sequence of edges as long as every"
                + " location on it has exactly one possible successor. If a target location is on"
                + " that sequence, every execution violates the specification.",
            PROPOSITIONS,
            ReachabilityRules::checkViolationOnEveryExecution));
  }

  private static Optional<RuleVerdict> checkNoReachableTargetLocation(ProgramFacts pFacts) {
    if (pFacts.usesFunctionAddress()) {
      // A function without a body could call a function of the program that reports a violation,
      // and the computation of the target locations does not know about such a call.
      return Optional.empty();
    }
    Optional<ImmutableSet<CFANode>> targetLocations = pFacts.targetLocations();
    if (targetLocations.isEmpty()
        || !Sets.intersection(targetLocations.orElseThrow(), pFacts.reachableNodes()).isEmpty()) {
      return Optional.empty();
    }
    return RuleVerdict.proven(
        "none of the "
            + targetLocations.orElseThrow().size()
            + " target locations of the specification automata is among the "
            + pFacts.reachableNodes().size()
            + " locations that an execution of the program ("
            + pFacts.cfa().getNumberOfFunctions()
            + " functions) can reach");
  }

  private static Optional<RuleVerdict> checkViolationOnEveryExecution(ProgramFacts pFacts) {
    Optional<ImmutableSet<CFANode>> targetLocations = pFacts.targetLocations();
    if (targetLocations.isEmpty() || targetLocations.orElseThrow().isEmpty()) {
      return Optional.empty();
    }
    ImmutableList<CFAEdge> chain = pFacts.chain().edges();
    for (int i = 0; i < chain.size(); i++) {
      CFAEdge edge = chain.get(i);
      if (targetLocations.orElseThrow().contains(edge.getSuccessor())) {
        return RuleVerdict.refuted(
            "every execution reaches the violation at "
                + edge.getFileLocation()
                + " (\""
                + edge.getDescription()
                + "\") after "
                + i
                + " edges that have no alternative",
            edge);
      }
    }
    return Optional.empty();
  }
}
