// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.simplerules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingTransferRelation;

/** The rules for the properties about concurrency. */
final class ConcurrencyRules {

  private ConcurrencyRules() {}

  private static final ImmutableSet<Property> PROPOSITIONS =
      ImmutableSet.of(CommonVerificationProperty.DATA_RACE, CommonVerificationProperty.DEADLOCK);

  /**
   * The calls after which a program may have more than one thread. {@link
   * ThreadingTransferRelation#FUNCTIONS_REQUIRING_THREADING} is the set of calls whose semantics
   * only an analysis with the ThreadingCPA models ({@code pthread_create}, {@code pthread_once} and
   * {@code pthread_barrier_wait}), i.e. the calls that make a program concurrent for CPAchecker.
   * Two names are added to it: {@code pthread_create_N} is what the CFA construction puts in place
   * of {@code pthread_create} (cf. option {@code cfa.threads.threadCreate}), and {@code
   * thrd_create} creates a thread in the threads of C11 (ISO/IEC 9899:2011, 7.26.5.1), which no
   * analysis of CPAchecker models.
   *
   * <p>A call of a function without a body already makes this rule abstain, so this set is what
   * catches the calls that do have one: the definition that the CFA construction inserts for a
   * replaced thread operation, and a definition that the program brings along itself.
   */
  private static final ImmutableSet<String> CONCURRENCY_FUNCTIONS =
      ImmutableSet.<String>builder()
          .addAll(ThreadingTransferRelation.FUNCTIONS_REQUIRING_THREADING)
          .add(ThreadingTransferRelation.THREAD_START + "_N")
          .add("thrd_create")
          .build();

  static ImmutableList<SimpleRule> rules() {
    return ImmutableList.of(
        new SimpleRule(
            "single-threaded",
            "A program that creates no thread has only one thread of execution, so no two accesses"
                + " to a memory location are concurrent and no two threads can wait for each"
                + " other.",
            PROPOSITIONS,
            ConcurrencyRules::checkSingleThreaded));
  }

  private static RuleVerdict checkSingleThreaded(ProgramFacts pFacts) {
    if (!pFacts.unknownFunctions().isEmpty()) {
      // A function without a body could create a thread.
      return RuleVerdict.abstained();
    }
    if (pFacts.callsAnyOf(CONCURRENCY_FUNCTIONS)) {
      return RuleVerdict.abstained();
    }
    return RuleVerdict.proven(
        "no reachable edge of the program ("
            + pFacts.reachableEdges().size()
            + " edges) calls a function that could make the program concurrent");
  }
}
