// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The single list of known analysis bugs that cause a concurrency integration test to be
 * <b>skipped</b> instead of run.
 *
 * <p>Every entry is a real defect: the analysis returns a definite but <em>wrong</em> verdict — it
 * claims a program with a violation is safe, or a safe program unsafe. None of them is merely
 * imprecision (an analysis that honestly answers UNKNOWN is sound, and the tests assert that
 * outcome instead of skipping it).
 *
 * <p><b>To file one of these:</b> comment out or delete its entry below. The corresponding tests
 * then run for real, CI turns red, and the failure output is the reproducer for the issue report.
 * Deleting an entry is the only edit needed; the test classes read this list and need no change.
 *
 * <p>Consumed by {@code ConcurrencyReachabilityTest}, {@code DataRaceAnalysisTest}, {@code
 * ConcurrencyOverflowTest} and {@code PORCPAOverflowTest}. The programs live in {@code
 * test/programs/por/}.
 */
public final class KnownConcurrencyIssues {

  private KnownConcurrencyIssues() {}

  /**
   * One known bug: every combination of {@link #configs} × {@link #programs} is skipped, with
   * {@link #description} reported as the reason.
   */
  private record KnownIssue(
      String description, ImmutableSet<String> configs, ImmutableSet<String> programs) {}

  // ===========================================================================================
  // THE LIST. Comment out an entry to make its tests run (and fail), so the issue can be filed.
  // ===========================================================================================
  private static final ImmutableList<KnownIssue> ISSUES =
      ImmutableList.of(

          // -------------------------------------------------------------------------------------
          // (1) valueAnalysis-concurrency: incorrect FALSE on a safe program.
          // The value analysis never learns `p == &shared`, so `*p = 1` leaves `shared` unknown and
          // it proposes a spurious counterexample. That is legitimate imprecision -- but this config
          // sets `analysis.checkCounterexamples = false`, so nothing refutes the path and it is
          // reported as a violation. (The POR configs hit the same imprecision and answer UNKNOWN,
          // because their counterexample check rejects the path.)
          new KnownIssue(
              "valueAnalysis-concurrency reports FALSE on a safe program: it has no counterexample"
                  + " check (analysis.checkCounterexamples = false), so its spurious counterexample"
                  + " becomes an incorrect-FALSE",
              ImmutableSet.of("config/valueAnalysis-concurrency.properties"),
              ImmutableSet.of("pointer_write_safe.c")),

          // -------------------------------------------------------------------------------------
          // (2) bddAnalysis-concurrency: incorrect FALSE on safe programs.
          // The BDD domain cannot model a pointer or a float, so the later assume stays open and the
          // error looks reachable. It is sound on the plain-integer baselines, so this is a domain
          // limit that is not being reported honestly (UNKNOWN) but as a violation.
          new KnownIssue(
              "bddAnalysis-concurrency reports FALSE on a safe program: the BDD domain cannot model"
                  + " the pointer / float, and reports the resulting open branch as a violation"
                  + " rather than as UNKNOWN",
              ImmutableSet.of("config/bddAnalysis-concurrency.properties"),
              ImmutableSet.of("pointer_write_safe.c", "atomic_float_safe.c")),

          // -------------------------------------------------------------------------------------
          // (3) dataRaceAnalysis: misses a real data race.
          // In `_Atomic float *p` the _Atomic qualifier binds to the POINTEE, so `p` itself is an
          // ordinary non-atomic pointer and the two concurrent writes to `p` race. The ordering-
          // consistency analysis reports FALSE here, correctly.
          new KnownIssue(
              "dataRaceAnalysis reports TRUE (race-free) on a program with a real race: in `_Atomic"
                  + " float *p` the qualifier binds to the pointee, so writes to the pointer `p`"
                  + " itself are unsynchronised",
              ImmutableSet.of("config/dataRaceAnalysis.properties"),
              ImmutableSet.of("atomic_float_ptr_unsafe.c")),

          // -------------------------------------------------------------------------------------
          // (4) predicateAnalysis-concurrency--overflow: misses every overflow reachable after a
          // thread is created. The boundary is exactly a pthread_create -- an overflow BEFORE any
          // create is still found -- and it is governed by cfa.useCFACloningForMultiThreadedPrograms:
          // with that option off, the very same programs are correctly reported FALSE. The SV-COMP
          // portfolio (svcomp27--overflow) enables the option, so its ConcurrencySafety-NoOverflows
          // results are affected too. Not root-caused; it is NOT the liveness filter
          // (overflow.useLiveness is already false and no expression is skipped for liveness).
          new KnownIssue(
              "predicateAnalysis-concurrency--overflow reports TRUE on programs that overflow: it"
                  + " misses any overflow reachable after a pthread_create. Governed by"
                  + " cfa.useCFACloningForMultiThreadedPrograms -- with that option off the same"
                  + " programs are correctly FALSE. Also affects the svcomp27--overflow portfolio",
              ImmutableSet.of("config/predicateAnalysis-concurrency--overflow.properties"),
              ImmutableSet.of(
                  "overflow_unsafe.c",
                  "overflow_after_join_unsafe.c",
                  "overflow_stale_lookahead_unsafe.c")),

          // -------------------------------------------------------------------------------------
          // (5) POR + OverflowCPA: misses an overflow because the overflow lookahead goes stale
          // across an interleaving. OverflowCPA never checks the edge it is given -- to constrain
          // `y + 1` it needs y BEFORE the increment, so it looks ahead from the previous edge and
          // parks a flag. Under POR the next edge to run may belong to another thread, so the flag
          // goes stale. The interleaving that would refresh it is pruned by POR's reduction, because
          // the edge doing the lookahead reads only a local and POR's independence relation does not
          // know that OverflowCPA reads the NEXT edge's variables. Changing the program's guard from
          // `if (local == 0)` to `if (y >= 0)` makes POR find the overflow.
          // sequentialization-concurrency--overflow reports this program FALSE, which is what
          // establishes that it really does overflow.
          new KnownIssue(
              "por-*-overflow reports TRUE on a program that overflows: OverflowCPA's one-edge"
                  + " lookahead goes stale across an interleaving, and POR's independence relation"
                  + " does not know the lookahead reads the next edge's variables, so it prunes the"
                  + " order that would refresh it",
              ImmutableSet.of(
                  "config/por-value-overflow.properties", "config/por-pred-overflow.properties"),
              ImmutableSet.of("overflow_stale_lookahead_unsafe.c")));

  // ===========================================================================================

  /**
   * The reason this (config, program) pair is a known bug, or {@code null} if it is not — in which
   * case the test must run normally.
   */
  public static @Nullable String reasonFor(String pConfig, String pProgram) {
    for (KnownIssue issue : ISSUES) {
      if (issue.configs().contains(pConfig) && issue.programs().contains(pProgram)) {
        return issue.description();
      }
    }
    return null;
  }
}
