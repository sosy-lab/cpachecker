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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation;

/** The rules for the properties about concurrency. */
final class ConcurrencyRules {

  private ConcurrencyRules() {}

  private static final ImmutableSet<Property> PROPOSITIONS =
      ImmutableSet.of(CommonVerificationProperty.DATA_RACE, CommonVerificationProperty.DEADLOCK);

  /**
   * The functions that create a thread. {@code pthread_create_N} is the name that the CFA
   * construction uses after it has replaced the thread operations (cf. option {@code
   * cfa.threads.threadCreate}).
   */
  private static final ImmutableSet<String> THREAD_CREATION_FUNCTIONS =
      ImmutableSet.of(
          ThreadingTransferRelation.THREAD_START,
          ThreadingTransferRelation.THREAD_START + "_N",
          "thrd_create");

  static ImmutableList<TrivialRule> rules() {
    return ImmutableList.of(
        new TrivialRule(
            "single-threaded",
            "A program that creates no thread has only one thread of execution, so no two accesses"
                + " to a memory location are concurrent and no two threads can wait for each"
                + " other.",
            PROPOSITIONS,
            ConcurrencyRules::checkSingleThreaded));
  }

  private static Optional<RuleVerdict> checkSingleThreaded(ProgramFacts pFacts) {
    if (!pFacts.unknownFunctions().isEmpty()) {
      // A function without a body could create a thread.
      return Optional.empty();
    }
    for (CFAEdge edge : pFacts.reachableEdges()) {
      String called = ProgramFacts.nameOfCallWithoutBody(edge);
      if (called != null && THREAD_CREATION_FUNCTIONS.contains(called)) {
        return Optional.empty();
      }
      for (CAstNode node : ProgramFacts.astNodes(edge)) {
        if (node instanceof CFunctionCallExpression call
            && call.getDeclaration() != null
            && THREAD_CREATION_FUNCTIONS.contains(call.getDeclaration().getName())) {
          // The program brings its own definition of a function that creates a thread.
          return Optional.empty();
        }
      }
    }
    return RuleVerdict.proven(
        "no reachable edge of the program ("
            + pFacts.reachableEdges().size()
            + " edges) creates a thread");
  }
}
