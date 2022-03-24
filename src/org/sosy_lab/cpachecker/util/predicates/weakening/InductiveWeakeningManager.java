// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.weakening;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Throwables;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.Tactic;

/**
 * Finds inductive weakening of formulas (originally: formula slicing). This class operates on
 * formulas, and should be orthogonal to CFA- and CPA-specific concepts.
 */
public class InductiveWeakeningManager implements StatisticsProvider {

  /** Possible weakening strategies. */
  public enum WEAKENING_STRATEGY {

    /** Remove all atoms containing the literals mentioned in the transition relation. */
    SYNTACTIC,

    /** Abstract away all literals, try to un-abstract them one by one. */
    DESTRUCTIVE,

    /** Select literals to abstract based on the counterexamples-to-induction. */
    CEX
  }

  private final WeakeningOptions options;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;

  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final LogManager logger;

  private final InductiveWeakeningStatistics statistics;
  private final SyntacticWeakeningManager syntacticWeakeningManager;
  private final DestructiveWeakeningManager destructiveWeakeningManager;
  private final CEXWeakeningManager cexWeakeningManager;
  private final Solver solver;

  private static final String SELECTOR_VAR_TEMPLATE = "_FS_SEL_VAR_";

  public InductiveWeakeningManager(
      WeakeningOptions pOptions,
      Solver pSolver,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {

    options = pOptions;
    statistics = new InductiveWeakeningStatistics();
    fmgr = pSolver.getFormulaManager();
    logger = pLogger;
    bfmgr = fmgr.getBooleanFormulaManager();
    syntacticWeakeningManager = new SyntacticWeakeningManager(fmgr);
    destructiveWeakeningManager =
        new DestructiveWeakeningManager(pSolver, fmgr, options, statistics);
    solver = pSolver;
    cexWeakeningManager =
        new CEXWeakeningManager(fmgr, pSolver, statistics, options, pShutdownNotifier);
  }

  /**
   * This method supports different states of <i>from</i> and <i>to</i> state lemmas. Only the
   * lemmas associated with the <i>to</i> state can be dropped.
   *
   * @param fromStateLemmas Uninstantiated lemmas associated with the <i>from</i> state.
   * @param transition Transition from <i>fromState</i> to <i>toState</i>. Has to start at {@code
   *     startingSSA}.
   * @param toStateLemmas Uninstantiated lemmas associated with the <i>to</i> state.
   * @return Subset of {@code toStateLemmas} to which everything in {@code fromStateLemmas} maps.
   */
  public Set<BooleanFormula> findInductiveWeakeningForRCNF(
      SSAMap startingSSA,
      Set<BooleanFormula> fromStateLemmas,
      final PathFormula transition,
      Set<BooleanFormula> toStateLemmas)
      throws SolverException, InterruptedException {

    BooleanFormula fromStateLemmasInstantiated =
        fromStateLemmas.stream()
            .map(f -> fmgr.instantiate(f, startingSSA))
            .collect(bfmgr.toConjunction());

    // Mapping from selectors to the items they annotate.
    final BiMap<BooleanFormula, BooleanFormula> selectionInfo = annotateConjunctions(toStateLemmas);
    BooleanFormula toStateLemmasAnnotated =
        Collections3.zipMapEntries(
                selectionInfo,
                (selector, f) -> bfmgr.or(selector, fmgr.instantiate(f, transition.getSsa())))
            .collect(bfmgr.toConjunction());

    final Set<BooleanFormula> toAbstract =
        findSelectorsToAbstract(
            selectionInfo,
            fromStateLemmasInstantiated,
            transition,
            toStateLemmasAnnotated,
            startingSSA,
            fromStateLemmas);

    ImmutableSet<BooleanFormula> out =
        from(toStateLemmas)
            .filter(lemma -> !toAbstract.contains(selectionInfo.inverse().get(lemma)))
            .toSet();
    assert checkAllMapsTo(
        fromStateLemmas, startingSSA, out, transition.getSsa(), transition.getFormula());
    return out;
  }

  /**
   * Find weakening of {@code lemmas} with respect to {@code transition}. This method assumes to-
   * and from- lemmas are the same, and drops both at the same time.
   *
   * @param lemmas Set of uninstantiated lemmas.
   * @return inductive subset of {@code lemmas}
   */
  public Set<BooleanFormula> findInductiveWeakeningForRCNF(
      final SSAMap startingSSA, final PathFormula transition, Set<BooleanFormula> lemmas)
      throws SolverException, InterruptedException {

    // Mapping from selectors to the items they annotate.
    final BiMap<BooleanFormula, BooleanFormula> selectionInfo = annotateConjunctions(lemmas);

    BooleanFormula fromStateLemmasAnnotated =
        Collections3.zipMapEntries(
                selectionInfo,
                (selector, f) -> bfmgr.or(selector, fmgr.instantiate(f, startingSSA)))
            .collect(bfmgr.toConjunction());

    BooleanFormula toStateLemmasAnnotated =
        Collections3.zipMapEntries(
                selectionInfo,
                (selector, f) -> bfmgr.or(selector, fmgr.instantiate(f, transition.getSsa())))
            .collect(bfmgr.toConjunction());

    final Set<BooleanFormula> toAbstract =
        findSelectorsToAbstract(
            selectionInfo,
            fromStateLemmasAnnotated,
            transition,
            toStateLemmasAnnotated,
            startingSSA,
            lemmas);

    ImmutableSet<BooleanFormula> out =
        from(lemmas)
            .filter(lemma -> !toAbstract.contains(selectionInfo.inverse().get(lemma)))
            .toSet();
    assert checkAllMapsTo(out, startingSSA, out, transition.getSsa(), transition.getFormula());

    return out;
  }

  /** Sanity checking on output, whether it is indeed inductive. */
  private boolean checkAllMapsTo(
      Set<BooleanFormula> from,
      SSAMap startSSA,
      Set<BooleanFormula> to,
      SSAMap finishSSA,
      BooleanFormula transition)
      throws SolverException, InterruptedException {
    return solver.isUnsat(
        bfmgr.and(
            fmgr.instantiate(bfmgr.and(from), startSSA),
            transition,
            fmgr.instantiate(bfmgr.not(bfmgr.and(to)), finishSSA)));
  }

  /**
   * @param selectionVarsInfo Mapping from the selectors to the (uninstantiated) formulas they
   *     annotate.
   * @param fromState Instantiated formula representing the state before the transition.
   * @param transition Transition under which inductiveness should hold.
   * @param toState Instantiated formula representing the state after the transition.
   * @param fromSSA SSAMap associated with the {@code fromState}.
   * @param pFromStateLemmas Uninstantiated lemmas describing the from- state.
   * @return Set of selectors which should be abstracted. Subset of {@code selectionVarsInfo} keys.
   */
  private Set<BooleanFormula> findSelectorsToAbstract(
      Map<BooleanFormula, BooleanFormula> selectionVarsInfo,
      BooleanFormula fromState,
      PathFormula transition,
      BooleanFormula toState,
      SSAMap fromSSA,
      Set<BooleanFormula> pFromStateLemmas)
      throws SolverException, InterruptedException {
    switch (options.getWeakeningStrategy()) {
      case SYNTACTIC:
        return syntacticWeakeningManager.performWeakening(
            fromSSA, selectionVarsInfo, transition.getSsa(), pFromStateLemmas);

      case DESTRUCTIVE:
        return destructiveWeakeningManager.performWeakening(
            selectionVarsInfo, fromState, transition, toState, fromSSA, pFromStateLemmas);

      case CEX:
        return cexWeakeningManager.performWeakening(
            selectionVarsInfo.keySet(), fromState, transition, toState);
      default:
        throw new UnsupportedOperationException("Unexpected enum value");
    }
  }

  private static final class TemporaryException extends RuntimeException {

    private static final long serialVersionUID = -7046164286357019183L;

    TemporaryException(InterruptedException e) {
      super(e);
    }

    TemporaryException(SolverException e) {
      super(e);
    }

    AssertionError unwrap() throws InterruptedException, SolverException {
      Throwables.propagateIfPossible(getCause(), InterruptedException.class, SolverException.class);
      throw new AssertionError(this);
    }
  }

  public BooleanFormula removeRedundancies(BooleanFormula input)
      throws InterruptedException, SolverException {
    // Assume the formula to be a conjunction over disjunctions.
    BooleanFormula nnf = fmgr.applyTactic(input, Tactic.NNF);

    try {
      return bfmgr.transformRecursively(
          nnf,
          new BooleanFormulaTransformationVisitor(fmgr) {
            @Override
            public BooleanFormula visitAnd(List<BooleanFormula> processedOperands) {
              try {
                return bfmgr.and(simplifyArgs(processedOperands));
              } catch (InterruptedException e) {
                throw new TemporaryException(e);
              } catch (SolverException e) {
                throw new TemporaryException(e);
              }
            }

            @Override
            public BooleanFormula visitOr(List<BooleanFormula> processedOperands) {
              try {
                return bfmgr.or(simplifyArgs(processedOperands));
              } catch (InterruptedException e) {
                throw new TemporaryException(e);
              } catch (SolverException e) {
                throw new TemporaryException(e);
              }
            }
          });
    } catch (TemporaryException e) {
      throw e.unwrap();
    }
  }

  private List<BooleanFormula> simplifyArgs(List<BooleanFormula> args)
      throws SolverException, InterruptedException {
    boolean changed = true;
    while (changed) {
      changed = false;
      for (int i = 0; i < args.size(); i++) {
        BooleanFormula f = args.get(i);
        List<BooleanFormula> others = others(args, i);
        if (solver.isUnsat(bfmgr.not(bfmgr.implication(bfmgr.and(others), f)))) {
          args = others;
          changed = true;
        }
      }
    }
    return args;
  }

  private <T> List<T> others(List<T> l, int i) {
    List<T> others = new ArrayList<>(l);
    others.remove(i);
    return others;
  }

  BiMap<BooleanFormula, BooleanFormula> annotateConjunctions(Collection<BooleanFormula> pInput) {

    ImmutableBiMap.Builder<BooleanFormula, BooleanFormula> result = ImmutableBiMap.builder();
    int i = -1;
    for (BooleanFormula f : pInput) {
      BooleanFormula selector = bfmgr.makeVariable(SELECTOR_VAR_TEMPLATE + ++i);
      result.put(selector, f);
    }

    return result.buildOrThrow();
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
  }

  public static class InductiveWeakeningStatistics implements Statistics {

    /** Number of iterations required for convergence. */
    final Multiset<Integer> iterationsNo = HashMultiset.create();

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      out.printf(
          "Histogram of number of iterations required for convergence: " + "%s %n", iterationsNo);
    }

    @Override
    public String getName() {
      return "Inductive Weakening";
    }
  }
}
