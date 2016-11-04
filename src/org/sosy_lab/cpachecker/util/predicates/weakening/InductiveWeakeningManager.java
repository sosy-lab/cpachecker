/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.weakening;


import static org.sosy_lab.cpachecker.util.predicates.weakening.InductiveWeakeningManager.WEAKENING_STRATEGY.CEX;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds inductive weakening of formulas (originally: formula slicing).
 * This class operates on formulas, and should be orthogonal to
 * CFA- and CPA-specific concepts.
 */
@Options(prefix="cpa.slicing")
public class InductiveWeakeningManager implements StatisticsProvider {

  @Option(description="Inductive weakening strategy", secure=true)
  private WEAKENING_STRATEGY weakeningStrategy = CEX;

  /**
   * Possible weakening strategies.
   */
  public enum WEAKENING_STRATEGY {

    /**
     * Remove all atoms containing the literals mentioned in the transition relation.
     */
    SYNTACTIC,

    /**
     * Abstract away all literals, try to un-abstract them one by one.
     */
    DESTRUCTIVE,

    /**
     * Select literals to abstract based on the counterexamples-to-induction.
     */
    CEX
  }

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
      Configuration config,
      Solver pSolver,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {
    config.inject(this);

    statistics = new InductiveWeakeningStatistics();
    fmgr = pSolver.getFormulaManager();
    logger = pLogger;
    bfmgr = fmgr.getBooleanFormulaManager();
    syntacticWeakeningManager = new SyntacticWeakeningManager(fmgr);
    destructiveWeakeningManager = new DestructiveWeakeningManager(pSolver,
        fmgr, config, statistics);
    solver = pSolver;
    cexWeakeningManager = new CEXWeakeningManager(
        fmgr, pSolver, statistics, config, pShutdownNotifier);
  }

  /**
   * This method supports different states of <i>from</i> and <i>to</i> state
   * lemmas. Only the lemmas associated with the <i>to</i> state can be dropped.
   *
   * @param fromStateLemmas Uninstantiated lemmas associated with the
   *                        <i>from</i> state.
   * @param transition Transition from <i>fromState</i> to <i>toState</i>.
   *                   Has to start at {@code startingSSA}.
   * @param toStateLemmas Uninstantiated lemmas associated with the
   *                        <i>to</i> state.
   *
   * @return Subset of {@code toStateLemmas} to which everything in
   * {@code fromStateLemmas} maps.
   */
  public Set<BooleanFormula> findInductiveWeakeningForRCNF(
      SSAMap startingSSA,
      Set<BooleanFormula> fromStateLemmas,
      final PathFormula transition,
      Set<BooleanFormula> toStateLemmas
     )
      throws SolverException, InterruptedException {

    // Mapping from selectors to the items they annotate.
    final BiMap<BooleanFormula, BooleanFormula> selectionInfo = HashBiMap.create();

    List<BooleanFormula> fromStateLemmasInstantiated =
        fmgr.instantiate(fromStateLemmas, startingSSA);

    List<BooleanFormula> toStateLemmasInstantiated =
        fmgr.instantiate(toStateLemmas, transition.getSsa());
    BooleanFormula toStateLemmasAnnotated = annotateConjunctions(
        toStateLemmasInstantiated, selectionInfo
    );

    final Set<BooleanFormula> toAbstract = findSelectorsToAbstract(
        selectionInfo,
        bfmgr.and(fromStateLemmasInstantiated),
        transition,
        toStateLemmasAnnotated,
        startingSSA,
        fromStateLemmas);

    Set<BooleanFormula> out =
        Sets.filter(toStateLemmas,
            lemma -> (!toAbstract.contains(selectionInfo.inverse().get(
                fmgr.instantiate(lemma, transition.getSsa())
            ))));
    assert checkAllMapsTo(fromStateLemmas, startingSSA, out, transition
        .getSsa(), transition.getFormula());
    return out;
  }

  /**
   * Find weakening of {@code lemmas} with respect to {@code transition}.
   * This method assumes to- and from- lemmas are the same, and drops both at
   * the same time.
   *
   * @param lemmas Set of uninstantiated lemmas.
   * @return inductive subset of {@code lemmas}
   */
  public Set<BooleanFormula> findInductiveWeakeningForRCNF(
      final SSAMap startingSSA,
      final PathFormula transition,
      Set<BooleanFormula> lemmas
  )
      throws SolverException, InterruptedException {

    // Mapping from selectors to the items they annotate.
    final BiMap<BooleanFormula, BooleanFormula> selectionInfo = HashBiMap.create();

    List<BooleanFormula> fromStateLemmasInstantiated = fmgr.instantiate(lemmas, startingSSA);
    BooleanFormula fromStateLemmasAnnotated = annotateConjunctions(
        fromStateLemmasInstantiated, selectionInfo
    );
    BooleanFormula toStateLemmasAnnotated = fmgr.instantiate(
        fromStateLemmasAnnotated, transition.getSsa());

    final Set<BooleanFormula> toAbstract = findSelectorsToAbstract(
        selectionInfo,
        fromStateLemmasAnnotated,
        transition,
        toStateLemmasAnnotated,
        startingSSA, lemmas);

    Set<BooleanFormula> out =
        Sets.filter(lemmas,
            lemma -> (!toAbstract.contains(selectionInfo.inverse().get(
                fmgr.instantiate(lemma, startingSSA)
            ))));
    assert checkAllMapsTo(out, startingSSA, out, transition.getSsa(),
        transition.getFormula());

    return out;
  }

  /**
   * Sanity checking on output, whether it is indeed inductive.
   */
  private boolean checkAllMapsTo(
      Set<BooleanFormula> from,
      SSAMap startSSA,
      Set<BooleanFormula> to,
      SSAMap finishSSA,
      BooleanFormula transition
  ) throws SolverException, InterruptedException {
    return solver.isUnsat(bfmgr.and(
            fmgr.instantiate(bfmgr.and(from), startSSA),
            transition,
            fmgr.instantiate(bfmgr.not(bfmgr.and(to)), finishSSA)
        ));
  }

  /**
   *
   * @param selectionVarsInfo Mapping from the selectors to the already
   *                          instantiated formulas they annotate.
   * @param fromState Instantiated formula representing the state before the
   *                  transition.
   * @param transition Transition under which inductiveness should hold.
   * @param toState Instantiated formula representing the state after the
   *                transition.
   * @param fromSSA SSAMap associated with the {@code fromState}.
   * @param pFromStateLemmas Uninstantiated lemmas describing the from- state.
   * @return Set of selectors which should be abstracted.
   *         Subset of {@code selectionVarsInfo} keys.
   */
  private Set<BooleanFormula> findSelectorsToAbstract(
      Map<BooleanFormula, BooleanFormula> selectionVarsInfo,
      BooleanFormula fromState,
      PathFormula transition,
      BooleanFormula toState,
      SSAMap fromSSA,
      Set<BooleanFormula> pFromStateLemmas) throws SolverException, InterruptedException {
    switch (weakeningStrategy) {
      case SYNTACTIC:
        return syntacticWeakeningManager.performWeakening(
                fromSSA, selectionVarsInfo, transition, pFromStateLemmas);

      case DESTRUCTIVE:
        return destructiveWeakeningManager.performWeakening(
            selectionVarsInfo,
            fromState,
            transition,
            toState,
            fromSSA,
            pFromStateLemmas);

      case CEX:
        return cexWeakeningManager.performWeakening(
            selectionVarsInfo,
            fromState,
            transition,
            toState);
      default:
        throw new UnsupportedOperationException("Unexpected enum value");
    }
  }

  public BooleanFormula removeRedundancies(BooleanFormula input)
      throws InterruptedException {
    // Assume the formula to be a conjunction over disjunctions.
    BooleanFormula nnf = fmgr.applyTactic(input, Tactic.NNF);

    return bfmgr.transformRecursively(nnf, new BooleanFormulaTransformationVisitor(fmgr) {
          @Override
          public BooleanFormula visitAnd(List<BooleanFormula> processedOperands) {
            try {
              return bfmgr.and(simplifyArgs(processedOperands));
            } catch (SolverException|InterruptedException pE) {
              throw new UnsupportedOperationException("Error while "
                  + "simplifying", pE); }
          }

          @Override
          public BooleanFormula visitOr(List<BooleanFormula> processedOperands) {
            try {
              return bfmgr.or(simplifyArgs(processedOperands));
            } catch (SolverException|InterruptedException pE) {
              throw new UnsupportedOperationException("Error while "
                  + "simplifying", pE); }
          }
        });
  }

  private List<BooleanFormula> simplifyArgs(
      List<BooleanFormula> args)
      throws SolverException, InterruptedException {
    boolean changed = true;
    while (changed) {
      changed = false;
      for (int i = 0; i < args.size(); i++) {
        BooleanFormula f = args.get(i);
        List<BooleanFormula> others = others(args, i);
        if (solver.isUnsat(
            bfmgr.not(
                bfmgr.implication(bfmgr.and(others), f)
            )
        )) {
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

  BooleanFormula annotateConjunctions(
      Collection<BooleanFormula> pInput,
      final Map<BooleanFormula, BooleanFormula> pSelectionVarsInfoToFill) {

    Set<BooleanFormula> annotated = new HashSet<>(pInput.size());
    int i = -1;
    for (BooleanFormula f : pInput) {
      BooleanFormula selector = bfmgr.makeVariable(SELECTOR_VAR_TEMPLATE + ++i);
      pSelectionVarsInfoToFill.put(selector, f);
      annotated.add(bfmgr.or(selector, f));
    }
    return bfmgr.and(annotated);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
  }

  public static class InductiveWeakeningStatistics implements Statistics {

    /**
     * Number of iterations required for convergence.
     */
    final Multiset<Integer> iterationsNo = HashMultiset.create();

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      out.printf("Histogram of number of iterations required for convergence: "
          + "%s %n", iterationsNo);
    }

    @Override
    public String getName() {
      return "Inductive Weakening";
    }
  }
}
