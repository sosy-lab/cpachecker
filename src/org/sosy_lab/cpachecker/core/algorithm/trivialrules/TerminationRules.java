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
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.trivialrules.ProgramFacts.ChainEnd;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/** The rules for the termination property. */
final class TerminationRules {

  private TerminationRules() {}

  private static final ImmutableSet<Property> PROPOSITIONS =
      ImmutableSet.of(CommonVerificationProperty.TERMINATION);

  /**
   * The option that replaces a loop of blank edges with the end of the program. It is not valid for
   * the termination property, because it turns a program that runs forever into one that ends.
   */
  private static final String REMOVE_TRIVIAL_LOOPS_OPTION = "cfa.removeTrivialLoops";

  static ImmutableList<TrivialRule> rules() {
    return ImmutableList.of(
        new TrivialRule(
            "no-reachable-loop",
            "An execution that runs forever has to repeat a location, which needs a loop or a"
                + " recursion. A program in which no execution reaches a loop or a recursive call"
                + " therefore terminates.",
            PROPOSITIONS,
            TerminationRules::checkNoReachableLoop),
        new TrivialRule(
            "endless-loop-on-every-execution",
            "Every execution of the program executes the same sequence of edges as long as every"
                + " location on it has exactly one possible successor. If that sequence reaches a"
                + " location twice, it repeats itself forever, so no execution terminates.",
            PROPOSITIONS,
            TerminationRules::checkEndlessLoopOnEveryExecution));
  }

  private static Optional<RuleVerdict> checkNoReachableLoop(ProgramFacts pFacts) {
    Optional<LoopStructure> loopStructure = pFacts.loopStructure();
    if (loopStructure.isEmpty()) {
      // Without the loop structure we do not know where the loops are.
      return Optional.empty();
    }
    if (pFacts.isOptionSetTo(REMOVE_TRIVIAL_LOOPS_OPTION, true)
        || pFacts.hasReplacedTrivialLoop()) {
      pFacts
          .logger()
          .log(
              java.util.logging.Level.INFO,
              "Not using the rule no-reachable-loop because the option",
              REMOVE_TRIVIAL_LOOPS_OPTION,
              "removed loops from the CFA that a nonterminating program can have.");
      return Optional.empty();
    }
    if (!pFacts.unknownFunctions().isEmpty()) {
      // A function without a body could run forever.
      return Optional.empty();
    }

    ImmutableSet<CFANode> reachable = pFacts.reachableNodes();
    for (Loop loop : loopStructure.orElseThrow().getAllLoops()) {
      if (!Sets.intersection(loop.getLoopNodes(), reachable).isEmpty()) {
        return Optional.empty();
      }
    }
    ImmutableSet<CFANode> recursion =
        ImmutableSet.copyOf(Sets.intersection(pFacts.recursionNodes(), reachable));
    if (!recursion.isEmpty()) {
      return Optional.empty();
    }

    return RuleVerdict.proven(
        "no execution of the program reaches one of the "
            + loopStructure.orElseThrow().getCount()
            + " loops of the CFA or a recursive call, so every execution reaches the end of the"
            + " program after at most "
            + pFacts.reachableEdges().size()
            + " steps");
  }

  /**
   * The last edge of the given sequence that a location of the program can be reported for. The
   * edges that close a loop in the CFA are blank edges without a location of their own.
   */
  private static CFAEdge lastEdgeWithLocation(ImmutableList<CFAEdge> pChain) {
    for (int i = pChain.size() - 1; i >= 0; i--) {
      if (pChain.get(i).getFileLocation().isRealLocation()) {
        return pChain.get(i);
      }
    }
    return Iterables.getLast(pChain);
  }

  private static Optional<RuleVerdict> checkEndlessLoopOnEveryExecution(ProgramFacts pFacts) {
    if (pFacts.chain().end() != ChainEnd.REPEATED_LOCATION) {
      return Optional.empty();
    }
    ImmutableList<CFAEdge> chain = pFacts.chain().edges();
    CFAEdge inTheLoop = lastEdgeWithLocation(chain);
    return RuleVerdict.refuted(
        "every execution reaches "
            + inTheLoop.getFileLocation()
            + " (\""
            + inTheLoop.getDescription()
            + "\") again after "
            + chain.size()
            + " edges that have no alternative, so it repeats itself forever",
        inTheLoop);
  }
}
