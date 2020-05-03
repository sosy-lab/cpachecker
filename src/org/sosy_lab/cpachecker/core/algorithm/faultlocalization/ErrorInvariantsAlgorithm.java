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
import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.Selector;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.TraceFormula;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class ErrorInvariantsAlgorithm implements FaultLocalizationAlgorithmInterface, Statistics {

  private ShutdownNotifier shutdownNotifier;
  private Configuration config;
  private LogManager logger;
  private TraceFormula errorTrace;
  private FormulaContext formulaContext;
  //private CFA cfa;
  //private boolean useImproved;

  private StatTimer totalTime = new StatTimer(StatKind.SUM, "Total time for ErrInv");
  private StatCounter searchCalls = new StatCounter("Search calls");
  private StatCounter solverCalls = new StatCounter("Solver calls");

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

  private List<BooleanFormula> getInterpolants()
      throws CPAException, InterruptedException, InvalidConfigurationException {
    InterpolationManager interpolationManager =
        new InterpolationManager(
            formulaContext.getManager(),
            formulaContext.getSolver(),
            Optional.empty(),
            Optional.empty(),
            config,
            shutdownNotifier,
            logger);

    List<BooleanFormula> allFormulas = new ArrayList<>();
    allFormulas.add(errorTrace.getPreCondition());
    allFormulas.addAll(errorTrace.getAtoms());
    allFormulas.add(errorTrace.getPostCondition());
    CounterexampleTraceInfo counterexampleTraceInfo =
        interpolationManager.buildCounterexampleTrace(new BlockFormulas(allFormulas));
    List<BooleanFormula> interpolants = new ArrayList<>(counterexampleTraceInfo.getInterpolants());

    if (interpolants.size() > 0 && interpolants.size() == errorTrace.traceSize() + 1) {
      interpolants.remove(0);
    }

    return interpolants;
  }

  @Override
  public Set<Fault> run(FormulaContext context, TraceFormula tf)
      throws CPAException, InterruptedException, SolverException, VerifyException,
          InvalidConfigurationException {
    formulaContext = context;
    errorTrace = tf;

    totalTime.start();

    ImmutableList<BooleanFormula> interpolants = ImmutableList.copyOf(getInterpolants());
    PriorityQueue<Interval> sortedIntervals = new PriorityQueue<>();
    for (int i = 0; i < interpolants.size(); i++) {
      // TODO actualForm can evaluate to false (c.f. SingleUnsatCore)
      BooleanFormula interpolant = interpolants.get(i);
      Interval current =
          new Interval(
              search(0, i, x -> !isErrInv(interpolant, x)),
              search(i, tf.traceSize(), x -> isErrInv(interpolant, x)) - 1,
              interpolant,
              tf.getSelectors().get(i));
      sortedIntervals.add(current);
    }

    Interval maxInterval = sortedIntervals.peek();
    int prevEnd = 0;
    List<InterpolantToSelector> interpolantsForPosition = new ArrayList<>();
    for (Interval currInterval : sortedIntervals) {
      if (currInterval.start > prevEnd) {
        interpolantsForPosition.add(
            new InterpolantToSelector(maxInterval.invariant, maxInterval.selector));
        if (maxInterval.end < tf.traceSize()) {
          interpolantsForPosition.add(
              new InterpolantToSelector(
                  tf.getAtom(maxInterval.end), tf.getSelectors().get(maxInterval.end)));
        }
        prevEnd = maxInterval.end;
        maxInterval = currInterval;
      } else {
        if (currInterval.end > maxInterval.end) {
          maxInterval = currInterval;
        }
      }
    }

    // Create Faults, process information and append it to the created Faults
    Map<Fault, String> descriptions = new HashMap<>();
    for (InterpolantToSelector interpolantToSelector : interpolantsForPosition) {
      Selector current = interpolantToSelector.selector;
      // Don't show pre-condition statements in the result set
      if (errorTrace.getPreconditionSymbols().isEmpty()
          || !current.getEdge().toString().contains("__VERIFIER_nondet")) {
        Fault f = new Fault(current);
        descriptions.merge(
            f, interpolantToSelector.invariant.toString(), (fault, desc) -> fault + ", " + desc);
      }
    }

    totalTime.stop();
    descriptions.forEach(
        (fault, desc) ->
            fault.addInfo(FaultInfo.justify("Described by the interpolant(s): " + desc)));
    return descriptions.keySet();
  }

  private int search(int low, int high, Function<Integer, Boolean> incLow) {
    searchCalls.inc();
    if (high < low) {
      return low;
    }
    solverCalls.inc();
    int mid = (low + high) / 2;
    if (incLow.apply(mid)) {
      return search(mid + 1, high, incLow);
    } else {
      return search(low, mid - 1, incLow);
    }
  }

  private boolean isErrInv(BooleanFormula interpolant, int i) {
    Solver solver = formulaContext.getSolver();
    FormulaManagerView fmgr = solver.getFormulaManager();
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    int n = errorTrace.traceSize();

    BooleanFormula plainPostCondition = fmgr.uninstantiate(errorTrace.getPostCondition());
    BooleanFormula plainInterpolant = fmgr.uninstantiate(interpolant);

    BooleanFormula interpolant_i = fmgr.instantiate(plainInterpolant, errorTrace.getSsaMap(i));
    BooleanFormula postCondition_i =
        fmgr.instantiate(plainPostCondition, errorTrace.getSsaMap(n - i));

    BooleanFormula firstFormula =
        bmgr.implication(
            bmgr.and(errorTrace.getPreCondition(), errorTrace.slice(i)), interpolant_i);
    BooleanFormula secondFormula =
        bmgr.implication(
            bmgr.and(interpolant, errorTrace.slice(i, n), postCondition_i), bmgr.makeFalse());
    try {
      return solver.isUnsat(bmgr.not(firstFormula)) && solver.isUnsat(bmgr.not(secondFormula));
    } catch (SolverException | InterruptedException pE) {
      throw new AssertionError("first and second formula have to be solvable for the solver");
    }
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter w0 = StatisticsWriter.writingStatisticsTo(out);
    w0.put("Total time", totalTime)
        .put("Search calls", searchCalls).put("Solver calls", solverCalls);
  }

  @Override
  public @Nullable String getName() {
    return "Error invariants algorithm";
  }

  private static class InterpolantToSelector {
    private Selector selector;
    private BooleanFormula invariant;

    public InterpolantToSelector(BooleanFormula pInvariant, Selector pSelector) {
      selector = pSelector;
      invariant = pInvariant;
    }
  }

  private static class Interval implements Comparable<Interval> {

    private int start;
    private int end;
    private BooleanFormula invariant;
    private Selector selector;

    public Interval(
        int pStart, int pEnd, BooleanFormula pInvariant, Selector pSelector) {
      start = pStart;
      end = pEnd;
      invariant = pInvariant;
      selector = pSelector;
    }

    //Enables usage of PriorityQueue
    @Override
    public int compareTo(Interval pInterval) {
      return Integer.compare(start, pInterval.start);
    }
  }
}
