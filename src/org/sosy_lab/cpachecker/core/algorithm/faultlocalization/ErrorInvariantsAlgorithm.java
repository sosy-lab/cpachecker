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

import com.google.common.base.Splitter;
import com.google.common.base.VerifyException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.MapsDifference;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.AbstractTraceElement;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
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
  //(commented out, needed for flowsensitive traceformula)private CFA cfa;
  //private boolean useImproved;

  //Memorize already processed interpolants to minimize solver calls
  private Map<MemorizeInterpolant, Boolean> memorize = new HashMap<>();

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

  /**
   * Calculate interpolants
   * @return all interpolants for each position
   */
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
    return counterexampleTraceInfo.getInterpolants();
  }

  @Override
  public Set<Fault> run(FormulaContext context, TraceFormula tf)
      throws CPAException, InterruptedException, SolverException, VerifyException,
          InvalidConfigurationException {
    formulaContext = context;
    errorTrace = tf;

    totalTime.start();

    List<BooleanFormula> interpolants = getInterpolants();
    Set<Interval> allIntervals = new HashSet<>();

    // calculate interval boundaries for each interpolant
    for (int i = 0; i < interpolants.size(); i++) {
      // TODO actualForm can evaluate to false (c.f. SingleUnsatCore)
      BooleanFormula interpolant = interpolants.get(i);
      Interval current =
          new Interval(
              search(0, i, x -> !isErrInv(interpolant, x)),
              search(i, tf.traceSize(), x -> isErrInv(interpolant, x)) - 1,
              interpolant);
      allIntervals.add(current);
    }

    //sort the intervals and calculate abstrace error trace
    List<Interval> sortedIntervals = allIntervals.stream().sorted().collect(Collectors.toList());
    Interval maxInterval = sortedIntervals.get(0);
    int prevEnd = 0;
    List<AbstractTraceElement> abstractTrace = new ArrayList<>();
    for(Interval currInterval: sortedIntervals) {
      if (currInterval.start > prevEnd) {
        abstractTrace.add(maxInterval);
        if (maxInterval.end < tf.traceSize()) {
          abstractTrace.add(errorTrace.getSelectors().get(maxInterval.end));
        }
        prevEnd = maxInterval.end;
        maxInterval = currInterval;
      } else {
        if (currInterval.end > maxInterval.end) {
          maxInterval = currInterval;
        }
      }
    }

    //transform error trace into report format
    return createFaults(abstractTrace);
  }

  /**
   * Transforms an abstract error trace to faults.
   * An abstract error trace looks (in the best case) like this:
   * Interval[0;4] invariant1
   * Selector 5
   * Interval [6:10] invariant2
   * ...
   * @param abstractTrace Abstract trace of a traceformula
   * @return faults for the report
   */
  private Set<Fault> createFaults(List<AbstractTraceElement> abstractTrace){
    //Stores description of last interval
    String description = "";

    //Stores the last created fault (so we can add the next description)
    Fault lastCreatedFault = null;
    Set<String> variablesToTrack = new HashSet<>();
    Set<Fault> faults = new HashSet<>();
    FormulaManagerView fmgr = formulaContext.getSolver().getFormulaManager();
    BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
    for (AbstractTraceElement errorInvariant : abstractTrace) {
      if (errorInvariant instanceof Selector) {
        //Create fault and append the description of the previous and the next interval
        Fault f = new Fault((Selector)errorInvariant);
        f.addInfo(FaultInfo.justify("So far, the following is responsible for the error: " + description));
        //f.addInfo(FaultInfo.hint("Track the variables: " + String.join(", ", variablesToTrack)));
        lastCreatedFault = f;
        faults.add(f);
      }
      if (errorInvariant instanceof Interval){
        Interval interval = (Interval)errorInvariant;
        BooleanFormula invariant = fmgr.uninstantiate(interval.invariant);
        variablesToTrack.addAll(fmgr.extractVariables(invariant).keySet());
        if (variablesToTrack.removeIf(p -> p.contains("__VERIFIER_nondet"))) {
          variablesToTrack.add("user input");
        }
        //Replace long unreadable formulas with their actual meaning if possible
        description = extractRelevantInformation(fmgr, interval);
        //description = description/*.replaceAll("__VERIFIER_nondet_[a-zA-Z0-9]+!", "CPA_user_input_")*/.replaceAll("@", "");
        if(lastCreatedFault != null){
          lastCreatedFault.addInfo(FaultInfo.hint("From now on, the following is responsible for the error: " + description));
        }
      }
    }
    // if there is only one interpolant the algorithm failed to abstract the error trace
    // we can only state that the post-condition holds in every location (no gain of information)
    if (abstractTrace.size() == 1) {
      if(abstractTrace.get(0) instanceof Interval){
        CFAEdge lastEdge = errorTrace.getEdges().get(errorTrace.getEdges().size()-1);
        Fault f = new Fault(Selector.makeSelector(formulaContext, bmgr.makeTrue(), lastEdge));
        f.addInfo(FaultInfo.justify("The whole program can be described by: " + description));
        f.addInfo(FaultInfo.hint("NOTE: The algorithm did not find a suitable abstraction."));
        faults.add(f);
      }
    }
    String abstractErrorTrace = abstractTrace.stream().map(e -> "-" + e).collect(Collectors.joining("\n"));
    logger.log(Level.INFO, "Abstract error trace:\n"+abstractErrorTrace);
    return faults;
  }

  /**
   * Some interpolants (eg arrays) may have an unreadable format.
   * Since most of the users will be confused by the internal representation we reduce
   * the information to only the relevant one.
   * @param fmgr formula manager to instantiate and uninstantiate formulas
   * @param interval interval to extract information from
   * @return relevant information
   */
  private String extractRelevantInformation(FormulaManagerView fmgr, Interval interval) {
    BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
    List<String> helpfulFormulas = new ArrayList<>();
    Set<BooleanFormula> conjunctions = bmgr.toConjunctionArgs(interval.invariant, true);
    for(BooleanFormula f: conjunctions){
      if (!f.toString().contains("_ADDRESS_OF")){
        helpfulFormulas.add(formulaContext.getConverter().convert(fmgr.uninstantiate(f)).trim());
      } else {
        List<String> findName = Splitter.on("__ADDRESS_OF_").splitToList(f.toString());
        if (findName.size() > 1) {
          List<String> extractName = Splitter.on("@").splitToList(findName.get(1));
          if (!extractName.isEmpty()) {
            helpfulFormulas.add("\"values of " + extractName.get(0) + "\"");
            continue;
          }
        }
        helpfulFormulas.add(formulaContext.getConverter().convert(fmgr.uninstantiate(f)));
      }
    }
    //return "<ul><li>"  + helpfulFormulas.stream().distinct().map(s -> s.replaceAll("@", "")).collect(Collectors.joining(" </li><li> ")) + "</li></ul>";
    return helpfulFormulas.stream().distinct().map(s -> s.replaceAll("@", "")).collect(Collectors.joining(" and "));
  }

  /**
   * Perform a binary search to find the limits of an inductive interpolant
   * @param low start point
   * @param high end point
   * @param incLow function that indicates the search direction
   * @return the maximal or minimal bound of an inductive interval
   */
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


  /**
   * Return if interpolant is inductive on position i.
   * @param interpolant A interpolant
   * @param i position in the trace formula
   * @return true if interpolant is inductive at i, false else
   */
  private boolean isErrInv(BooleanFormula interpolant, int i) {
    Solver solver = formulaContext.getSolver();
    FormulaManagerView fmgr = solver.getFormulaManager();
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    int n = errorTrace.traceSize();
    BooleanFormula plainInterpolant = fmgr.uninstantiate(interpolant);
    MemorizeInterpolant currInterpolant = new MemorizeInterpolant(plainInterpolant, i);

    //Memoization
    if (memorize.containsKey(currInterpolant)) {
      return memorize.get(currInterpolant);
    }

    // shift the interpolant to the correct time stamp
    SSAMap shift =
        SSAMap.merge(
            errorTrace.getSsaMap(i),
            errorTrace.getSsaMap(0),
            MapsDifference.collectMapsDifferenceTo(new ArrayList<>()));
    BooleanFormula shiftedInterpolant = fmgr.instantiate(plainInterpolant, shift);

    solverCalls.inc();
    BooleanFormula firstFormula =
        bmgr.implication(
            bmgr.and(errorTrace.getPreCondition(), errorTrace.slice(i)), shiftedInterpolant);
    BooleanFormula secondFormula =
            bmgr.and(shiftedInterpolant, errorTrace.slice(i, n), errorTrace.getPostCondition());

    try {
      boolean isValid = isValid(firstFormula) && solver.isUnsat(secondFormula);
      memorize.put(currInterpolant, isValid);
      return isValid;
    } catch (SolverException | InterruptedException pE) {
      throw new AssertionError("first and second formula have to be solvable for the solver");
    }
  }

  /**
   * Calculates if a formula is a tautology.
   * A formula is a tautology if the negation is unsatisfiable.
   * @param formula check if formula is a tautology
   * @return true if formula is a tautology, false else
   */
  private boolean isValid(BooleanFormula formula) throws SolverException, InterruptedException {
    BooleanFormulaManager bmgr = formulaContext.getSolver().getFormulaManager().getBooleanFormulaManager();
    return formulaContext.getSolver().isUnsat(bmgr.not(formula));
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

  /**
   * Stores the interpolant for a selector and its boundaries
   */
  private static class Interval implements Comparable<Interval>, AbstractTraceElement {

    private int start;
    private int end;
    private BooleanFormula invariant;

    public Interval(
        int pStart, int pEnd, BooleanFormula pInvariant) {
      start = pStart;
      end = pEnd;
      invariant = pInvariant;
    }

    @Override
    public String toString() {
      return "Interval [" + start + ";" + end + "]: " + invariant;
    }

    @Override
    public boolean equals(Object q){
      if(q instanceof Interval){
        Interval compare = (Interval)q;
        return compare.start == start && compare.end == end && invariant.equals(compare.invariant);
      }
      return false;
    }

    @Override
    public int hashCode(){
      return Objects.hash(invariant, start, end);
    }

    @Override
    public int compareTo(Interval pInterval) {
      return Integer.compare(start, pInterval.start);
    }
  }

  /** Stores interpolants and positions that are already evaluateddd*/
  private static class MemorizeInterpolant {
    private BooleanFormula interpolant;
    private int position;

    private MemorizeInterpolant(BooleanFormula pInterpolant, int pPosition){
      interpolant = pInterpolant;
      position = pPosition;
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || !(pO instanceof MemorizeInterpolant)) {
        return false;
      }
      MemorizeInterpolant memorizeInterpolant = (MemorizeInterpolant) pO;
      return position == memorizeInterpolant.position &&
          Objects.equals(interpolant, memorizeInterpolant.interpolant);
    }

    @Override
    public int hashCode() {
      return Objects.hash(interpolant, position);
    }

    @Override
    public String toString() {
      return "MemorizeInterpolant{" +
          "interpolant=" + interpolant +
          ", position=" + position +
          '}';
    }
  }
}
