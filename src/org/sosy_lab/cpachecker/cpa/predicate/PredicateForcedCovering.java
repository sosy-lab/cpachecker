/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static org.sosy_lab.cpachecker.util.AbstractStates.extractElementByType;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.SymbolicRegionManager.SymbolicRegion;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.interpolation.UninstantiatingInterpolationManager;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * An implementation of {@link ForcedCovering} which works with
 * {@link PredicateAbstractState}s and tries to strengthen them the
 * necessary amount by using interpolation.
 */
public class PredicateForcedCovering implements ForcedCovering, StatisticsProvider {

  private final class FCStatistics implements Statistics {

    private int attemptedForcedCoverings = 0;
    private int successfulForcedCoverings = 0;
    private int wasAlreadyCovered = 0;

    @Override
    public String getName() {
      return "Predicate Forced Covering";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("Attempted forced coverings:             " + attemptedForcedCoverings);
      if (attemptedForcedCoverings > 0) {
        out.println("Successful forced coverings:            " + successfulForcedCoverings + " (" + toPercent(successfulForcedCoverings, attemptedForcedCoverings) + ")");
      }
      out.println("No of times elment was already covered: " + wasAlreadyCovered);
    }

    private String toPercent(double val, double full) {
      return String.format("%1.0f", val/full*100) + "%";
    }
  }

  private final FCStatistics stats = new FCStatistics();
  private final LogManager logger;

  private final ForcedCoveringStopOperator stop;

  private final FormulaManager fmgr;
  private final InterpolationManager<Formula> imgr;
  private final Solver solver;

  public PredicateForcedCovering(Configuration config, LogManager pLogger,
      ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    logger = pLogger;

    if (pCpa.getStopOperator() instanceof ForcedCoveringStopOperator) {
      stop = (ForcedCoveringStopOperator) pCpa.getStopOperator();
    } else {
      throw new InvalidConfigurationException(PredicateForcedCovering.class.getSimpleName() + " needs a CPA with support for forced coverings");
    }

    PredicateCPA predicateCpa = ((WrapperCPA)pCpa).retrieveWrappedCpa(PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(PredicateForcedCovering.class.getSimpleName() + " needs a PredicateCPA");
    }

    imgr = new UninstantiatingInterpolationManager(predicateCpa.getFormulaManager(),
                                                   predicateCpa.getPathFormulaManager(),
                                                   predicateCpa.getSolver(),
                                                   predicateCpa.getFormulaManagerFactory(),
                                                   config, pLogger);
    fmgr = predicateCpa.getFormulaManager();
    solver = predicateCpa.getSolver();
  }

  @Override
  public boolean tryForcedCovering(AbstractState pElement, Precision pPrecision, ReachedSet pReached)
      throws CPAException, InterruptedException {
    ARGState argElement = (ARGState)pElement;
    if (argElement.isCovered()) {
      return false;
    }

    if (pReached.getReached(pElement).size() <= 1) {
      return false;
    }

    PredicateAbstractState predicateElement = getPredicateElement(pElement);
    if (!(predicateElement.getAbstractionFormula().asRegion() instanceof SymbolicRegion)) {
      throw new CPAException("Cannot use PredicateForcedCovering with non-symbolic abstractions");
    }
    if (!predicateElement.isAbstractionElement()) {
      return false;
    }

    logger.log(Level.FINER, "Starting interpolation-based forced covering.");
    logger.log(Level.ALL, "Attempting to force-cover", argElement);

    ARGReachedSet arg = new ARGReachedSet(pReached);

    List<ARGState> parentList = ImmutableList.copyOf(getParentAbstractionElements(argElement)).reverse();
    Set<ARGState> parentSet = ImmutableSet.copyOf(parentList);

    for (AbstractState reachedElement : pReached.getReached(pElement)) {
      if (pElement == reachedElement) {
        continue;
      }

      if (stop.stop(argElement, Collections.singleton(reachedElement), pPrecision)
          || argElement.isCovered()) {
        stats.wasAlreadyCovered++;
        logger.log(Level.FINER, "Element was covered by another element without strengthening");
        return true;
      }

      if (stop.isForcedCoveringPossible(pElement, reachedElement, pPrecision)) {
        stats.attemptedForcedCoverings++;
        logger.log(Level.ALL, "Candidate for forced-covering is", reachedElement);

        List<ARGState> reachedParentList = ImmutableList.copyOf(getParentAbstractionElements((ARGState)reachedElement)).reverse();
        ARGState commonParent = Iterables.find(
            reachedParentList,
            Predicates.in(parentSet));
        assert commonParent != null : "Elements do not have common parent, but are in the same reached set";


        List<ARGState> path = new ArrayList<ARGState>();
        for (ARGState pathElement : parentList) {
          if (pathElement == commonParent) {
            break;
          }
          path.add(pathElement);
        }
        path = Lists.reverse(path);

        assert path.get(path.size()-1) == argElement : "Path does not end in the current element";

        // path is now the list of abstraction elements from the common ancestor
        // of reachedElement and argElement (excluding) to argElement (including):
        // path = ]commonParent; argElement]

        // create list of formulas:
        // 1) state formula of commonParent instantiated with indices of commonParent
        // 2) block formulas from commonParent to argElement
        // 3) negated state formula of reachedElement instantiated with indices of argElement
        List<Formula> formulas = new ArrayList<Formula>(path.size()+2);
        {
          formulas.add(getPredicateElement(commonParent).getAbstractionFormula().asInstantiatedFormula());

          for (AbstractState pathElement : path) {
            formulas.add(getPredicateElement(pathElement).getAbstractionFormula().getBlockFormula());
          }

          SSAMap ssaMap = getPredicateElement(argElement).getPathFormula().getSsa().withDefault(1);
          Formula stateFormula = getPredicateElement(reachedElement).getAbstractionFormula().asFormula();
          assert !stateFormula.isTrue() : "Existing element with state true would cover anyway, no forced covering needed";
          formulas.add(fmgr.makeNot(fmgr.instantiate(stateFormula, ssaMap)));
        }

        path.add(0, commonParent); // now path is [commonParent; argElement] (including x and v)
        assert formulas.size() == path.size() + 1;

        CounterexampleTraceInfo<Formula> interpolantInfo = imgr.buildCounterexampleTrace(formulas, Collections.<ARGState>emptySet());

        if (!interpolantInfo.isSpurious()) {
          logger.log(Level.FINER, "Forced covering unsuccessful.");
          continue; // forced covering not possible
        }


        stats.successfulForcedCoverings++;
        logger.log(Level.FINER, "Forced covering successful.");

        List<Formula> interpolants = interpolantInfo.getPredicatesForRefinement();
        assert interpolants.size() == formulas.size() - 1 : "Number of interpolants is wrong";
        assert interpolants.size() == path.size();

        for (Pair<Formula, ARGState> interpolationPoint : Pair.zipList(interpolants, path)) {
          Formula itp = interpolationPoint.getFirst();
          ARGState element = interpolationPoint.getSecond();

          if (itp.isTrue()) {
            continue;
          }

          PredicateAbstractState predElement = getPredicateElement(element);
          AbstractionFormula af = predElement.getAbstractionFormula();
          if (!solver.implies(af.asFormula(), itp)) {

            Formula newFormula = fmgr.makeAnd(itp, af.asFormula());
            Formula instantiatedNewFormula = fmgr.instantiate(newFormula, predElement.getPathFormula().getSsa());
            AbstractionFormula newAF = new AbstractionFormula(new SymbolicRegion(newFormula), newFormula, instantiatedNewFormula, af.getBlockFormula());
            predElement.setAbstraction(newAF);

            arg.removeCoverageOf(element);
          }
        }

        // For debugging, run stop operator on this element.
        // However, ARGStopSep may return false although it is covered,
        // thus the second check.
        assert stop.stop(argElement, Collections.singleton(reachedElement), pPrecision)
            || argElement.isCovered()
            : "Forced covering did not cover element\n" + argElement + "\nwith\n" + reachedElement;

        if (!argElement.isCovered()) {
          argElement.setCovered((ARGState)reachedElement);
        } else {
          assert argElement.getCoveringElement() == reachedElement;
        }

        return true;
      }
    }

    return false;
  }

  private Iterable<ARGState> getParentAbstractionElements(ARGState argElement) {
    Path pathToRoot = ARGUtils.getOnePathTo(argElement);

    return Iterables.filter(
            Iterables.transform(pathToRoot, Pair.<ARGState>getProjectionToFirst()),
        Predicates.compose(
            PredicateAbstractState.FILTER_ABSTRACTION_ELEMENTS,
            AbstractStates.extractElementByTypeFunction(PredicateAbstractState.class)));
  }

  private static PredicateAbstractState getPredicateElement(AbstractState pElement) {
    return extractElementByType(pElement, PredicateAbstractState.class);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
