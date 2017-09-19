/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.toPercent;

import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCovering;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * An implementation of {@link ForcedCovering} which works with
 * {@link PredicateAbstractState}s and tries to strengthen them the
 * necessary amount by using interpolation.
 */
public class PredicateForcedCovering implements ForcedCovering, StatisticsProvider {

  private static final class FCStatistics implements Statistics {

    private int attemptedForcedCoverings = 0;
    private int successfulForcedCoverings = 0;
    private int wasAlreadyCovered = 0;

    @Override
    public String getName() {
      return "Predicate Forced Covering";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {
      out.println("Attempted forced coverings:             " + attemptedForcedCoverings);
      if (attemptedForcedCoverings > 0) {
        out.println("Successful forced coverings:            " + successfulForcedCoverings + " (" + toPercent(successfulForcedCoverings, attemptedForcedCoverings) + ")");
      }
      out.println("No of times elment was already covered: " + wasAlreadyCovered);
    }
  }

  private final FCStatistics stats = new FCStatistics();
  private final LogManager logger;
  private final ARGCPA argCpa;

  private final ForcedCoveringStopOperator stop;

  private final FormulaManagerView fmgr;
  private final InterpolationManager imgr;
  private final PredicateAbstractionManager predAbsMgr;
  private final ImpactUtility impact;

  public PredicateForcedCovering(Configuration config, LogManager pLogger,
      ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    logger = pLogger;

    if (!(pCpa instanceof ARGCPA)) {
      throw new InvalidConfigurationException(PredicateForcedCovering.class.getSimpleName() + " needs an ARGCPA");
    }
    argCpa = (ARGCPA)pCpa;
    stop = argCpa.getStopOperator();

    PredicateCPA predicateCpa = ((WrapperCPA)pCpa).retrieveWrappedCpa(PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(PredicateForcedCovering.class.getSimpleName() + " needs a PredicateCPA");
    }

    imgr = new InterpolationManager(
                                                   predicateCpa.getPathFormulaManager(),
                                                   predicateCpa.getSolver(),
                                                   predicateCpa.getCfa().getLoopStructure(),
                                                   predicateCpa.getCfa().getVarClassification(),
                                                   config,
                                                   predicateCpa.getShutdownNotifier(),
                                                   pLogger);
    fmgr = predicateCpa.getSolver().getFormulaManager();
    predAbsMgr = predicateCpa.getPredicateManager();
    impact = new ImpactUtility(config, fmgr, predAbsMgr);
  }

  @Override
  public boolean tryForcedCovering(final AbstractState pState,
      final Precision pPrecision, final ReachedSet pReached)
      throws CPAException, InterruptedException {
    final ARGState argState = (ARGState)pState;
    if (argState.isCovered()) {
      return false;
    }

    if (pReached.getReached(pState).size() <= 1) {
      return false;
    }

    final PredicateAbstractState predicateElement = getPredicateState(pState);
    if (!predicateElement.isAbstractionState()) {
      return false;
    }

    BooleanFormulaManager bfmgr = fmgr.getBooleanFormulaManager();
    logger.log(Level.FINER, "Starting interpolation-based forced covering.");
    logger.log(Level.ALL, "Attempting to force-cover", argState);

    ARGReachedSet arg = new ARGReachedSet(pReached, argCpa);

    List<ARGState> parentList = getAbstractionPathTo(argState);
    for (final AbstractState coveringCandidate : pReached.getReached(pState)) {
      if (pState == coveringCandidate) {
        continue;
      }

      if (stop.stop(argState, Collections.singleton(coveringCandidate), pPrecision)
          || argState.isCovered()) {
        stats.wasAlreadyCovered++;
        logger.log(Level.FINER, "State was covered by another state without strengthening");
        return true;
      }

      if (stop.isForcedCoveringPossible(pState, coveringCandidate, pPrecision)) {
        stats.attemptedForcedCoverings++;
        logger.log(Level.ALL, "Candidate for forced-covering is", coveringCandidate);

        // A) find common parent of argState and coveringCandidate
        List<ARGState> candidateParentList = getAbstractionPathTo((ARGState)coveringCandidate);
        final int commonParentIdx =
            findLengthOfCommonPrefix(parentList, candidateParentList) - 1;

        assert commonParentIdx >= 0 : "States do not have common parent, but are in the same reached set";
        assert parentList.get(commonParentIdx).equals(candidateParentList.get(commonParentIdx)) : "Common prefix does not end with same element";

        final ARGState commonParent = parentList.get(commonParentIdx);
        final List<ARGState> path = parentList.subList(commonParentIdx+1, parentList.size());

        // path is now the list of abstraction elements from the common ancestor
        // of coveringCandidate and argState (excluding) to argState (including):
        // path = ]commonParent; argState]

        // B) create list of formulas:
        // 1) state formula of commonParent instantiated with indices of commonParent
        // 2) block formulas between commonParent and argState
        // 3) negated state formula of reachedState instantiated with indices of argState
        List<BooleanFormula> formulas = new ArrayList<>(path.size()+1);
        {
          formulas.add(getPredicateState(commonParent).getAbstractionFormula().asInstantiatedFormula());

          for (AbstractState pathElement : path) {
            formulas.add(getPredicateState(pathElement).getAbstractionFormula().getBlockFormula().getFormula());
          }

          SSAMap ssaMap = getPredicateState(argState).getPathFormula().getSsa().withDefault(1);
          BooleanFormula stateFormula = getPredicateState(coveringCandidate).getAbstractionFormula().asFormula();
          assert !bfmgr.isTrue(stateFormula) : "Existing state with abstraction true would cover anyway, no forced covering needed";
          formulas.add(bfmgr.not(fmgr.instantiate(stateFormula, ssaMap)));
        }
        assert formulas.size() == path.size() + 2;

        // C) Compute interpolants
        CounterexampleTraceInfo interpolantInfo = imgr.buildCounterexampleTrace(formulas);

        if (!interpolantInfo.isSpurious()) {
          logger.log(Level.FINER, "Forced covering unsuccessful.");
          continue; // forced covering not possible
        }


        stats.successfulForcedCoverings++;
        logger.log(Level.FINER, "Forced covering successful.");

        List<BooleanFormula> interpolants = interpolantInfo.getInterpolants();
        assert interpolants.size() == formulas.size() - 1 : "Number of interpolants is wrong";

        // As the first interpolant is implied by the first formula,
        // which is the state formula of commonParent,
        // we do not need to strengthen the commonParent with the first interpolant.
        interpolants = interpolants.subList(1, interpolants.size());

        // This is always the abstraction of the abstraction state before the "current" one.
        AbstractionFormula lastAbstraction = getPredicateState(commonParent).getAbstractionFormula();

        // D) update ARG
        for (Pair<BooleanFormula, ARGState> interpolationPoint : Pair.zipList(interpolants, path)) {
          BooleanFormula itp = interpolationPoint.getFirst();
          ARGState element = interpolationPoint.getSecond();

          boolean stateChanged;
          try {
            stateChanged = impact.strengthenStateWithInterpolant(
                                                  itp, element, lastAbstraction);
          } catch (SolverException e) {
            throw new CPAException("Solver failure", e);
          }
          if (stateChanged) {
            arg.removeCoverageOf(element);
          }

          lastAbstraction = getPredicateState(element).getAbstractionFormula();
        }

        // For debugging, run stop operator on this element.
        // However, ARGStopSep may return false although it is covered,
        // thus the second check.
        assert stop.stop(argState, Collections.singleton(coveringCandidate), pPrecision)
            || argState.isCovered()
            : "Forced covering did not cover element\n" + argState + "\nwith\n" + coveringCandidate;

        if (!argState.isCovered()) {
          argState.setCovered((ARGState)coveringCandidate);
        } else {
          assert argState.getCoveringState() == coveringCandidate;
        }

        return true;
      }
    }

    return false;
  }

  /**
   * Return a list with all abstraction states on the path from the ARG root
   * to the given element.
   */
  private ImmutableList<ARGState> getAbstractionPathTo(ARGState argState) {
    ARGPath pathFromRoot = ARGUtils.getOnePathTo(argState);

    return from(pathFromRoot.asStatesList())
        .filter(PredicateAbstractState.CONTAINS_ABSTRACTION_STATE)
        .toList();
  }

  /**
   * Given two Iterables, return the length of the common prefix of both Iterables.
   * If there are no common elements in the beginning of the Iterables,
   * 0 is returned.
   * If one of the Iterables is fully contained in the other,
   * the length of the shorter element is returned.
   * @param i1 An Iterable.
   * @param i2 An Iterable.
   * @return A non-negative int giving a length.
   */
  private static int findLengthOfCommonPrefix(Iterable<?> i1, Iterable<?> i2) {
    int i = 0;
    Iterator<?> it1 = i1.iterator();
    Iterator<?> it2 = i2.iterator();
    while (it1.hasNext() && it2.hasNext()) {
      if (!Objects.equals(it1.next(), it2.next())) {
        break;
      }
      i++;
    }
    return i;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
