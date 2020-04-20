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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.misc.MultiMap;
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
import org.sosy_lab.cpachecker.util.faultlocalization.FaultReason;
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
  private Solver solver;
  private BooleanFormulaManager bmgr;
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

  @Override
  public Set<Fault> run(FormulaContext context, TraceFormula tf)
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

    totalTime.start();
    List<BooleanFormula> allFormulas = new ArrayList<>();

    allFormulas.add(tf.getPreCondition());
    allFormulas.addAll(tf.getAtoms());
    allFormulas.add(tf.getPostCondition());
    CounterexampleTraceInfo counterexampleTraceInfo =
        interpolationManager.buildCounterexampleTrace(new BlockFormulas(allFormulas));
    List<BooleanFormula> interpolants = new ArrayList<>(counterexampleTraceInfo.getInterpolants());
    if(interpolants.size() > 0){
      interpolants.remove(0);
    }

    List<Interval> sortedIntervals = new ArrayList<>();
    for (int i = 0; i < interpolants.size(); i++) {
      // TODO edge for precondition is the first edge at the moment
      // TODO actualForm can be false (c.f. SingleUnsatCore)
      BooleanFormula interpolant = interpolants.get(i);
      sortedIntervals.add(
          new Interval(
              search(0, i, x -> !isErrInv(interpolant, x)),
              search(i, tf.traceSize(), x -> isErrInv(interpolant, x)) - 1,
              interpolant,
              tf.getAtom(i)));
    }

    sortedIntervals.sort(Comparator.comparingInt(i -> i.start));

    Interval maxInterval = sortedIntervals.get(0);
    int prevEnd = 0;
    List<InterpolantToEdge> interpolantsForPosition = new ArrayList<>();
    for (Interval currInterval : sortedIntervals) {
      if (currInterval.start > prevEnd) {
        interpolantsForPosition.add(
            new InterpolantToEdge(maxInterval.invariant, maxInterval.correspondingEdge));
        if (maxInterval.end < tf.traceSize()) {
          interpolantsForPosition.add(
              new InterpolantToEdge(tf.getAtom(maxInterval.end), tf.getAtom(maxInterval.end)));
        }
        prevEnd = maxInterval.end;
        maxInterval = currInterval;
      } else {
        if (currInterval.end > maxInterval.end) {
           maxInterval = currInterval;
        }
      }
    }

    /* All interpolants containing the post condition as interpolant must be removed.
       The post condition cannot be a suitable explanation of why the error happened. */

    MultiMap<Selector, BooleanFormula> allInterpolants = new MultiMap<>();
    Set<Fault> indicators = new HashSet<>();
    for (InterpolantToEdge interpolantToEdge : interpolantsForPosition) {
      Selector current = Selector.of(interpolantToEdge.edge).orElse(null);
      if(current != null){
        //Don't show pre-condition statements in the result set
        if(errorTrace.getPreconditionSymbols().isEmpty() || !current.getEdge().toString().contains("__VERIFIER_nondet")){
          allInterpolants.map(current, interpolantToEdge.interpolant);
          indicators.add(new Fault(current));
        }
      }
    }
    int sumInterpolants = allInterpolants.values().stream().mapToInt(v -> v.size()).sum();
    for (Entry<Selector, List<BooleanFormula>> selectorListEntry : allInterpolants.entrySet()) {
      String description = selectorListEntry.getValue()
          .stream()
          //ExpressionConverter is not 100% reliable but better to read
          .map(l -> context.getSolver().getFormulaManager().uninstantiate(l).toString())//ExpressionConverter.convert(l))
          .distinct()
          .collect(Collectors.joining(","));
      if (!description.isEmpty()) {
        selectorListEntry.getKey().addReason(FaultReason
            .justify("The error is described by the invariant(s): " + description,
                (double)selectorListEntry.getValue().size()/sumInterpolants));
      }
    }
    totalTime.stop();
    return indicators;
  }

  private int search(int low, int high, Function<Integer, Boolean> incLow) {
    searchCalls.inc();
    if (high < low) {
      return low;
    }
    int mid = (low + high) / 2;
    if (incLow.apply(mid)) {
      return search(mid + 1, high, incLow);
    } else {
      return search(low, mid - 1, incLow);
    }
  }

  private boolean isErrInv(BooleanFormula interpolant, int i) {
    FormulaManagerView fmgr = solver.getFormulaManager();
    int n = errorTrace.traceSize();

    BooleanFormula plainPostCondition = fmgr.uninstantiate(errorTrace.getPostCondition());
    BooleanFormula plainInterpolant = fmgr.uninstantiate(interpolant);

    BooleanFormula interpolant_i =
        fmgr.instantiate(plainInterpolant, errorTrace.getSsaMap(i));
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
        .put("Search calls", searchCalls);
  }

  @Override
  public @Nullable String getName() {
    return "Error invariants algorithm";
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
