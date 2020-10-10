// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants;

import com.google.common.base.Splitter;
import com.google.common.base.VerifyException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.MapsDifference;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.FaultLocalizerWithTraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.Selector;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pretty_print.BooleanFormulaParser;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * The ErrorInvariantsAlgorithm tries to abstract the trace formula by creating an alternating
 * sequence of actual locations and summarizing interpolants. Based on the work of Ermis Evren,
 * Martin Schäf, and Thomas Wies: "Error invariants." International Symposium on Formal Methods.
 * Springer, Berlin, Heidelberg, 2012
 */
public class ErrorInvariantsAlgorithm implements FaultLocalizerWithTraceFormula, Statistics {

  private ShutdownNotifier shutdownNotifier;
  private Configuration config;
  private LogManager logger;
  private TraceFormula errorTrace;
  private FormulaContext formulaContext;
  private List<SSAMap> maps;

  // Memorize already processed interpolants to minimize solver calls
  private Multimap<BooleanFormula, Integer> memorize;

  private StatTimer totalTime = new StatTimer(StatKind.SUM, "Total time for ErrInv");
  private StatCounter searchCalls = new StatCounter("Number of search calls");
  private StatCounter solverCalls = new StatCounter("Numer of solver calls");
  private StatCounter memoizationCalls =
      new StatCounter("Number of interpolant-interval cache hits");

  /**
   * Calculate an alternating sequence of edges and summarizing interpolants of the program to make
   * error detection more accessible.
   *
   * @param pShutdownNotifier the shutdown notifier
   * @param pConfiguration the run configurations
   * @param pLogger the logger
   */
  public ErrorInvariantsAlgorithm(
      ShutdownNotifier pShutdownNotifier, Configuration pConfiguration, LogManager pLogger) {
    shutdownNotifier = pShutdownNotifier;
    config = pConfiguration;
    logger = pLogger;
    memorize = ArrayListMultimap.create();
  }

  /**
   * Calculate interpolants
   *
   * @return all interpolants for each position
   */
  private List<BooleanFormula> getInterpolants()
      throws CPAException, InterruptedException, InvalidConfigurationException {
    solverCalls.inc();
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
    allFormulas.add(errorTrace.getPrecondition());
    allFormulas.addAll(errorTrace.getEntries().toAtomList());
    allFormulas.add(errorTrace.getPostcondition());
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
    maps = tf.getEntries().toSSAMapList();
    totalTime.start();

    List<BooleanFormula> interpolants = getInterpolants();
    Set<Interval> allIntervals = new HashSet<>();

    // calculate interval boundaries for each interpolant
    for (int i = 0; i < interpolants.size(); i++) {
      BooleanFormula interpolant = interpolants.get(i);
      Interval current =
          new Interval(
              search(0, i, interpolant, true),
              search(i, tf.traceSize(), interpolant, false) - 1,
              interpolant);
      allIntervals.add(current);
    }

    // sort the intervals and calculate abstrace error trace
    List<Interval> sortedIntervals =
        allIntervals.stream()
            .sorted(Comparator.comparingInt(interval -> interval.start))
            .collect(Collectors.toList());
    List<Selector> selectors = errorTrace.getEntries().toSelectorList();
    Interval maxInterval = sortedIntervals.get(0);
    int prevEnd = 0;
    List<AbstractTraceElement> abstractTrace = new ArrayList<>();
    for (Interval currInterval : sortedIntervals) {
      if (currInterval.start > prevEnd) {
        abstractTrace.add(maxInterval);
        if (maxInterval.end < tf.traceSize()) {
          abstractTrace.add(selectors.get(maxInterval.end));
        }
        prevEnd = maxInterval.end;
        maxInterval = currInterval;
      } else {
        if (currInterval.end > maxInterval.end) {
          maxInterval = currInterval;
        }
      }
    }
    totalTime.stop();
    // transform error trace to report format
    return createFaults(abstractTrace);
  }

  /**
   * Transforms an abstract error trace to faults. An abstract error trace is an alternating
   * sequence of intervals and selectors.
   *
   * @param abstractTrace Abstract trace of a traceformula
   * @return faults for the report
   */
  private Set<Fault> createFaults(List<AbstractTraceElement> abstractTrace) {
    // Stores description of last interval
    List<Selector> allSelectors = errorTrace.getEntries().toSelectorList();
    Selector prev = allSelectors.get(0);
    Set<Fault> faults = new HashSet<>();
    FormulaManagerView fmgr = formulaContext.getSolver().getFormulaManager();
    for (int i = 0; i < abstractTrace.size(); i++) {
      AbstractTraceElement errorInvariant = abstractTrace.get(i);
      if (errorInvariant instanceof Selector) {
        prev = (Selector) errorInvariant;
        Fault singleton = new Fault(prev);
        singleton.setIntendedIndex(i);
        faults.add(singleton);
        continue;
      }
      if (errorInvariant instanceof Interval) {
        Interval curr = (Interval) errorInvariant;
        Selector next;
        if (i + 1 < abstractTrace.size()) {
          next = (Selector) abstractTrace.get(i + 1);
        } else {
          next = allSelectors.get(allSelectors.size() - 1);
        }
        Set<FaultContribution> contributions = new HashSet<>();
        for (int j = allSelectors.indexOf(prev); j < allSelectors.indexOf(next); j++) {
          contributions.add(allSelectors.get(j));
        }
        if (curr.isEmpty()) {
          contributions.add(prev);
        }
        curr.replaceErrorSet(contributions);
        String description = extractRelevantInformation(fmgr, curr);
        curr.addInfo(FaultInfo.justify("The describing interpolant: " + description));
        curr.addInfo(FaultInfo.hint("This interpolant sums up the meaning of the marked edges."));
        curr.setIntendedIndex(i);
        faults.add(curr);
        continue;
      }
    }

    String abstractErrorTrace =
        abstractTrace.stream().map(e -> " - " + e).collect(Collectors.joining("\n"));
    logger.log(Level.INFO, "Abstract error trace:\n" + abstractErrorTrace);
    return faults;
  }

  /**
   * Extracts the fault-relevant information from the given formula. Since the original trace
   * formulas are often too detailed for a concise description of the fault, this method reduces the
   * displayed information to the relevant one.
   *
   * @param fmgr formula manager to instantiate and uninstantiate formulas
   * @param interval interval to extract information from
   * @return relevant information
   */
  private String extractRelevantInformation(FormulaManagerView fmgr, Interval interval) {
    BooleanFormulaManager bmgr = fmgr.getBooleanFormulaManager();
    List<String> helpfulFormulas = new ArrayList<>();
    Set<BooleanFormula> conjunctions = bmgr.toConjunctionArgs(interval.invariant, true);
    for (BooleanFormula f : conjunctions) {
      if (f.toString().contains("_ADDRESS_OF")) {
        List<String> findName = Splitter.on("__ADDRESS_OF_").splitToList(f.toString());
        if (findName.size() > 1) {
          List<String> extractName = Splitter.on("@").splitToList(findName.get(1));
          if (!extractName.isEmpty()) {
            helpfulFormulas.add("(`values_of` " + extractName.get(0) + ")");
            continue;
          }
        }
      }
      helpfulFormulas.add(BooleanFormulaParser.parse(fmgr.uninstantiate(f)).toString());
    }
    // return "<ul><li>"  + helpfulFormulas.stream().distinct().map(s -> s.replaceAll("@",
    // "")).collect(Collectors.joining(" </li><li> ")) + "</li></ul>";
    return helpfulFormulas.stream()
        .distinct()
        .map(s -> s.replaceAll("@", ""))
        .collect(Collectors.joining(" ∧ "));
  }

  /**
   * Perform a binary search to find the limits of an inductive interpolant
   *
   * @param low start point
   * @param high end point
   * @param interpolant interpolant which will guide the search direction
   * @param negate whether the return value of the error invariants check should be negated.
   * @return the maximal or minimal bound of an inductive interval
   */
  private int search(int low, int high, BooleanFormula interpolant, boolean negate)
      throws SolverException, InterruptedException {
    searchCalls.inc();
    if (high < low) {
      return low;
    }
    int mid = (low + high) / 2;
    // shortcut for if (negate) then !isErrInv else isErrInv
    if (isErrInv(interpolant, mid) ^ negate) {
      return search(mid + 1, high, interpolant, negate);
    } else {
      return search(low, mid - 1, interpolant, negate);
    }
  }

  /**
   * Return if interpolant is inductive on position i.
   *
   * @param interpolant A interpolant
   * @param slicePosition where to slice the trace formula
   * @return true if interpolant is inductive at i, false else
   */
  private boolean isErrInv(BooleanFormula interpolant, int slicePosition)
      throws SolverException, InterruptedException {
    Solver solver = formulaContext.getSolver();
    FormulaManagerView fmgr = solver.getFormulaManager();
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    int n = errorTrace.traceSize();
    BooleanFormula plainInterpolant = fmgr.uninstantiate(interpolant);

    // Memoization
    if (memorize.containsKey(plainInterpolant)) {
      memoizationCalls.inc();
      if (memorize.get(plainInterpolant).contains(-slicePosition - 1)) {
        return false;
      }
      if (memorize.get(plainInterpolant).contains(slicePosition + 1)) {
        return true;
      }
    }

    // shift the interpolant to the correct time stamp
    SSAMap shift =
        SSAMap.merge(
            maps.get(slicePosition),
            maps.get(0),
            MapsDifference.collectMapsDifferenceTo(new ArrayList<>()));
    BooleanFormula shiftedInterpolant = fmgr.instantiate(plainInterpolant, shift);

    BooleanFormula firstFormula =
        bmgr.implication(
            bmgr.and(errorTrace.getPrecondition(), errorTrace.slice(slicePosition)),
            shiftedInterpolant);
    BooleanFormula secondFormula =
        bmgr.and(
            shiftedInterpolant, errorTrace.slice(slicePosition, n), errorTrace.getPostcondition());

    // isUnsat
    solverCalls.inc();
    boolean isValid = isValid(firstFormula) && solver.isUnsat(secondFormula);
    memorize.put(plainInterpolant, isValid ? slicePosition + 1 : -slicePosition - 1);
    return isValid;
  }

  /**
   * Calculates if a formula is a tautology. A formula is a tautology if the negation is
   * unsatisfiable.
   *
   * @param formula check if formula is a tautology
   * @return true if formula is a tautology, false else
   */
  private boolean isValid(BooleanFormula formula) throws SolverException, InterruptedException {
    solverCalls.inc();
    BooleanFormulaManager bmgr =
        formulaContext.getSolver().getFormulaManager().getBooleanFormulaManager();
    return formulaContext.getSolver().isUnsat(bmgr.not(formula));
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out)
        .put(totalTime)
        .put(searchCalls)
        .put(solverCalls)
        .put(memoizationCalls);
  }

  @Override
  public @Nullable String getName() {
    return getClass().getSimpleName();
  }

  /** Stores the interpolant for a selector and its boundaries */
  public static class Interval extends Fault implements AbstractTraceElement {

    private int start;
    private int end;
    private BooleanFormula invariant;

    public Interval(int pStart, int pEnd, BooleanFormula pInvariant) {
      start = pStart;
      end = pEnd;
      invariant = pInvariant;
    }

    @Override
    public String toString() {
      return "Interval [" + start + ";" + end + "]: " + BooleanFormulaParser.parse(invariant);
    }

    @Override
    public boolean equals(Object q) {
      if (q instanceof Interval) {
        Interval compare = (Interval) q;
        return compare.start == start
            && compare.end == end
            && invariant.equals(compare.invariant)
            && super.equals(q);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(invariant, start, end, super.hashCode());
    }
  }
}
