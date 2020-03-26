/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization;

import com.google.common.base.VerifyException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicator;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.common.ErrorIndicatorSet;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.Selector;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.TraceFormula;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class ErrorInvariantsAlgorithm implements FaultLocalizationAlgorithmInterface {

  private ShutdownNotifier shutdownNotifier;
  private Configuration config;
  private LogManager logger;
  private TraceFormula errorTrace;
  private Solver solver;
  private BooleanFormulaManager bmgr;
  //private CFA cfa;
  //private boolean useImproved;

  public ErrorInvariantsAlgorithm(
      ShutdownNotifier pShutdownNotifier,
      Configuration pConfiguration,
      LogManager pLogger
      //CFA pCfa,
      //boolean pImproved
      ) {
    shutdownNotifier = pShutdownNotifier;
    config = pConfiguration;
    logger = pLogger;
    //cfa = pCfa;
    //useImproved = pImproved;
  }

  @Override
  public ErrorIndicatorSet<Selector> run(FormulaContext context, TraceFormula tf)
      throws CPAException, InterruptedException, SolverException, VerifyException,
          InvalidConfigurationException {
    solver = context.getSolver();
    bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    errorTrace = tf;
    InterpolationManager interpolationManager =
        new InterpolationManager(
            context.getManager(),
            solver,
            Optional.empty(),
            Optional.empty(),
            config,
            shutdownNotifier,
            logger);

    List<BooleanFormula> allFormulas = new ArrayList<>();

    allFormulas.add(tf.getPreCondition());
    allFormulas.addAll(tf.getAtoms());
    allFormulas.add(tf.getPostCondition());
    CounterexampleTraceInfo counterexampleTraceInfo =
        interpolationManager.buildCounterexampleTrace(new BlockFormulas(allFormulas));
    List<BooleanFormula> interpolants = counterexampleTraceInfo.getInterpolants();

    List<Interval> sortedIntervals = new ArrayList<>();
    for (int i = 0; i < interpolants.size(); i++) {
      // TODO edge for precondition
      // TODO actualForm can be false (c.f. SingleUnsatCore)
      BooleanFormula interpolant = interpolants.get(i);
      sortedIntervals.add(
          new Interval(
              search(0, i, x -> !isErrInv(interpolant, x)),
              search(i, tf.traceSize(), x -> isErrInv(interpolant, x)) - 1,
              interpolant,
              i == 0 ? tf.getAtom(0) : tf.getAtom(i - 1)));
    }
    // FormulaManagerView
    sortedIntervals.sort(Comparator.comparingInt(l -> l.start));

    Interval maxInterval = sortedIntervals.get(0);
    int prevEnd = 0;
    List<InterpolantToEdge> interpolantsForPosition = new ArrayList<>();
    for (Interval currInterval : sortedIntervals) {
      if (currInterval.start > prevEnd) {
        interpolantsForPosition.add(
            new InterpolantToEdge(maxInterval.invariant, maxInterval.correspondingEdge));
        if (maxInterval.end < tf.traceSize())
          interpolantsForPosition.add(
              new InterpolantToEdge(tf.getAtom(maxInterval.end), tf.getAtom(maxInterval.end)));
        prevEnd = maxInterval.end;
        maxInterval = currInterval;
      } else if (currInterval.end > maxInterval.end) maxInterval = currInterval;
    }
    // TODO null edge
    // All interpolants containing the post condition as interpolant must be removed. The post
    // condition cannot be a suitable explanation of why the error happens.
    return new ErrorIndicatorSet<>(
        interpolantsForPosition.stream()
            .filter(l -> !l.interpolant.equals(errorTrace.getPostCondition()))
            .map(
                l ->
                    new ErrorIndicator<>(Collections.singleton(
                        Selector.of(l.edge).orElse(Selector.makeSelector(context, l.edge, null)))))
            .collect(Collectors.toSet()));
  }

  private int search(int low, int high, Function<Integer, Boolean> incLow) {
    if (high < low) return low;
    int mid = (low + high) / 2;
    if (incLow.apply(mid)) return search(mid + 1, high, incLow);
    else return search(low, mid - 1, incLow);
  }

  private boolean isErrInv(BooleanFormula interpolant, int i) {
    FormulaManagerView fmgr = solver.getFormulaManager();
    int n = errorTrace.traceSize();

    List<SSAMap> consecutiveFormulaMaps = errorTrace.getSsaMaps();

    BooleanFormula plainPostCondition = fmgr.uninstantiate(errorTrace.getPostCondition());
    BooleanFormula plainInterpolant = fmgr.uninstantiate(interpolant);

    BooleanFormula interpolant_i =
        fmgr.instantiate(plainInterpolant, consecutiveFormulaMaps.get(i));
    BooleanFormula postCondition_i =
        fmgr.instantiate(plainPostCondition, consecutiveFormulaMaps.get(n - i));

    BooleanFormula firstFormula =
        bmgr.implication(
            bmgr.and(errorTrace.getPreCondition(), errorTrace.slice(i)), interpolant_i);
    BooleanFormula secondFormula =
        bmgr.implication(
            bmgr.and(plainInterpolant, errorTrace.slice(i, n), postCondition_i), bmgr.makeFalse());
    try {
      return solver.isUnsat(bmgr.not(firstFormula)) && solver.isUnsat(bmgr.not(secondFormula));
    } catch (SolverException | InterruptedException pE) {
      throw new AssertionError("first and second formula have to be solvable for the solver");
    }
  }

  private static class InterpolantToEdge {
    private BooleanFormula edge;
    private BooleanFormula interpolant;

    public InterpolantToEdge(BooleanFormula pInterpolant, BooleanFormula pEdge) {
      edge = pEdge;
      interpolant = pInterpolant;
    }
  }

  private static class Interval {

    private int start;
    private int end;
    private BooleanFormula invariant;
    private BooleanFormula correspondingEdge;

    public Interval(
        int pStart, int pEnd, BooleanFormula pInvariant, BooleanFormula pCorrespondingEdge) {
      start = pStart;
      end = pEnd;
      invariant = pInvariant;
      correspondingEdge = pCorrespondingEdge;
    }
  }
}
