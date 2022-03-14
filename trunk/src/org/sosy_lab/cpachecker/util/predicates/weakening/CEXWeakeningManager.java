// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.weakening;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.visitors.DefaultBooleanFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

/** Perform weakening using counter-examples to induction. */
public class CEXWeakeningManager {

  /** Selection strategy for CEX-based weakening. */
  enum SELECTION_STRATEGY {
    /** Abstract all matching children. */
    ALL,

    /** Abstract the first matching child. */
    FIRST,

    /** Abstract a random matching child. */
    RANDOM,

    /** Follow the branch which eventually results in least abstractions [on the given model]. */
    LEAST_REMOVALS
  }

  private final BooleanFormulaManager bfmgr;
  private final Solver solver;
  private final InductiveWeakeningManager.InductiveWeakeningStatistics statistics;
  private final Random r = new Random(0);
  private final ShutdownNotifier shutdownNotifier;
  private final WeakeningOptions options;

  public CEXWeakeningManager(
      FormulaManagerView pFmgr,
      Solver pSolver,
      InductiveWeakeningManager.InductiveWeakeningStatistics pStatistics,
      WeakeningOptions pOptions,
      ShutdownNotifier pShutdownNotifier) {
    options = pOptions;
    solver = pSolver;
    statistics = pStatistics;
    bfmgr = pFmgr.getBooleanFormulaManager();
    shutdownNotifier = pShutdownNotifier;
  }

  /**
   * Apply a weakening based on counterexamples derived from solver models.
   *
   * @param selectors The selectors.
   * @return A subset of selectors after abstracting which the query becomes inductive.
   */
  public Set<BooleanFormula> performWeakening(
      final Set<BooleanFormula> selectors,
      BooleanFormula fromState,
      PathFormula transition,
      BooleanFormula toState)
      throws SolverException, InterruptedException {

    final Set<BooleanFormula> toAbstract = new HashSet<>();
    List<BooleanFormula> selectorConstraints = new ArrayList<>();
    for (BooleanFormula selector : selectors) {
      selectorConstraints.add(bfmgr.not(selector));
    }
    BooleanFormula query = bfmgr.and(fromState, transition.getFormula(), bfmgr.not(toState));

    int noIterations = 1;
    try (ProverEnvironment env = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      env.push(query);

      while (!env.isUnsatWithAssumptions(selectorConstraints)) {
        noIterations++;
        shutdownNotifier.shutdownIfNecessary();
        try (Model m = env.getModel()) {

          toAbstract.addAll(
              getSelectorsToAbstract(ImmutableSet.copyOf(toAbstract), m, selectors, toState, 0));
        }

        selectorConstraints.clear();
        for (BooleanFormula selector : selectors) {
          if (toAbstract.contains(selector)) {
            selectorConstraints.add(selector);
          } else {
            selectorConstraints.add(bfmgr.not(selector));
          }
        }
      }
    }
    statistics.iterationsNo.add(noIterations);
    return toAbstract;
  }

  private List<BooleanFormula> getSelectorsToAbstract(
      final ImmutableSet<BooleanFormula> toAbstract,
      final Model m,
      final Set<BooleanFormula> selectors,
      final BooleanFormula primed,
      final int depth) {
    final List<BooleanFormula> newToAbstract = new ArrayList<>();

    // Perform the required abstraction.
    bfmgr.visitRecursively(
        primed,
        new DefaultBooleanFormulaVisitor<TraversalProcess>() {

          @Override
          protected TraversalProcess visitDefault() {
            return TraversalProcess.CONTINUE;
          }

          @Override
          public TraversalProcess visitAnd(List<BooleanFormula> operands) {
            // Under negation, AND becomes OR.
            // Abstracting away all children which evaluate to _true_ is sufficient.
            Set<BooleanFormula> filtered = new HashSet<>();
            for (BooleanFormula op : operands) {
              if (shouldAbstract(op)) {
                filtered.add(op);
              }
            }
            return TraversalProcess.custom(filtered);
          }

          @Override
          public TraversalProcess visitOr(List<BooleanFormula> operands) {
            // Under negation, OR becomes AND.
            // ALL children of this node evaluate to true iff the node
            // evaluates to true.
            // Abstracting away any child is sufficient to break the satisfiability.
            Optional<BooleanFormula> selector = findSelector(operands);

            if (selector.isPresent()) {
              if (shouldAbstract(bfmgr.or(operands))) {
                handleAnnotatedLiteral(selector.orElseThrow());
              }
              return TraversalProcess.SKIP;
            } else {

              // N.B.: This branch is *never* hit if we use
              // conjunction-annotation mode.

              // OR- implies a difficult choice, unless a selector is present.
              return selectChildren(operands);
            }
          }

          private void handleAnnotatedLiteral(BooleanFormula selector) {
            // Don't-care or evaluates-to-false.
            if (!toAbstract.contains(selector)) {
              newToAbstract.add(selector);
            }
          }

          private boolean shouldAbstract(BooleanFormula f) {
            Boolean out = m.evaluate(bfmgr.not(f));
            return out != null && out;
          }

          private Optional<BooleanFormula> findSelector(List<BooleanFormula> orOperands) {
            for (BooleanFormula operand : orOperands) {
              if (selectors.contains(operand)) {
                return Optional.of(operand);
              }
            }
            return Optional.empty();
          }

          private TraversalProcess selectChildren(List<BooleanFormula> operands) {
            switch (options.getRemovalSelectionStrategy()) {
              case ALL:
                return TraversalProcess.CONTINUE;
              case FIRST:
                BooleanFormula selected = operands.iterator().next();
                return TraversalProcess.custom(selected);
              case RANDOM:
                int rand = r.nextInt(operands.size());
                return TraversalProcess.custom(operands.get(rand));
              case LEAST_REMOVALS:
                if (depth >= options.getLeastRemovalsDepthLimit()) {
                  return TraversalProcess.custom(operands.iterator().next());
                }
                BooleanFormula out =
                    Collections.min(
                        operands, Comparator.comparingInt((f) -> recursivelyCallSelf(f).size()));
                return TraversalProcess.custom(out);
              default:
                throw new UnsupportedOperationException("Unexpected strategy");
            }
          }

          private List<BooleanFormula> recursivelyCallSelf(BooleanFormula f) {

            // Doing recursion while doing recursion :P
            // Use NullLogManager to avoid log pollution.
            return getSelectorsToAbstract(toAbstract, m, selectors, f, depth + 1);
          }
        });

    return newToAbstract;
  }
}
