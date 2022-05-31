// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.error_invariants;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Joiner;
import com.google.common.base.VerifyException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.MapsDifference;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.FaultLocalizerWithTraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace.ConjunctionTraceInterpreter;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace.Trace.TraceAtom;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.trace.TraceInterpreter;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
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

/**
 * The ErrorInvariantsAlgorithm tries to abstract the trace formula by creating an alternating
 * sequence of actual locations and summarizing interpolants. Based on the work of Ermis Evren,
 * Martin Schäf, and Thomas Wies: "Error invariants." International Symposium on Formal Methods.
 * Springer, Berlin, Heidelberg, 2012
 */
public class ErrorInvariantsAlgorithm implements FaultLocalizerWithTraceFormula, Statistics {

  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final LogManager logger;
  private TraceFormula errorTrace;
  private ImmutableList<SSAMap> maps;

  // Memorize already processed interpolants to minimize solver calls
  private final Multimap<BooleanFormula, Integer> memorize;

  private final StatTimer totalTime = new StatTimer(StatKind.SUM, "Total time for ErrInv");
  private final StatCounter searchCalls = new StatCounter("Number of search calls");
  private final StatCounter solverCalls = new StatCounter("Number of solver calls");
  private final StatCounter memoizationCalls =
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
  private List<BooleanFormula> getInterpolants(FormulaContext formulaContext)
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
    allFormulas.add(errorTrace.getPrecondition().getPrecondition());
    allFormulas.addAll(errorTrace.getTrace().toFormulaList());
    allFormulas.add(
        formulaContext
            .getSolver()
            .getFormulaManager()
            .getBooleanFormulaManager()
            .not(errorTrace.getPostCondition().getPostCondition()));
    List<BooleanFormula> interpolants = interpolationManager.interpolate(allFormulas).orElseThrow();
    return transformedImmutableListCopy(
        interpolants,
        element -> formulaContext.getSolver().getFormulaManager().uninstantiate(element));
  }

  @Override
  public Set<Fault> run(FormulaContext context, TraceFormula tf)
      throws CPAException, InterruptedException, SolverException, VerifyException,
          InvalidConfigurationException {
    errorTrace = tf;
    maps =
        ImmutableList.<SSAMap>builder()
            .add(tf.getTrace().getInitialSsaMap())
            .addAll(tf.getTrace().toSSAMapList())
            .build();
    totalTime.start();

    List<BooleanFormula> interpolants = getInterpolants(context);
    List<Interval> sortedIntervals = new ArrayList<>();

    // calculate interval boundaries for each interpolant
    for (int i = 0; i < interpolants.size(); i++) {
      BooleanFormula interpolant = interpolants.get(i);
      Interval current =
          new Interval(
              search(context.getSolver(), 0, i, interpolant, true),
              search(context.getSolver(), i, tf.getTrace().size(), interpolant, false) - 1,
              interpolant);
      sortedIntervals.add(current);
    }

    // sort the intervals and calculate abstract error trace
    sortedIntervals.sort(Comparator.comparingInt(Interval::getStart));
    ImmutableList<TraceAtom> selectors = ImmutableList.copyOf(errorTrace.getTrace());
    Interval maxInterval = sortedIntervals.get(0);
    int prevEnd = 0;
    List<AbstractTraceElement> abstractTrace = new ArrayList<>();
    for (Interval currInterval : sortedIntervals) {
      if (currInterval.getStart() > prevEnd) {
        abstractTrace.add(maxInterval);
        if (maxInterval.getEnd() < tf.getTrace().size()) {
          abstractTrace.add(selectors.get(maxInterval.getEnd()));
        }
        prevEnd = maxInterval.getEnd();
        maxInterval = currInterval;
      } else {
        if (currInterval.getEnd() > maxInterval.getEnd()) {
          maxInterval = currInterval;
        }
      }
    }
    totalTime.stop();

    abstractTrace =
        summarize(
            abstractTrace, context.getSolver().getFormulaManager().getBooleanFormulaManager());

    // transform error trace to report format
    return createFaults(abstractTrace);
  }

  /* TODO: evaluate max intervals instead of max inductive interpolants (code summary)
   Interval current = new Interval(searchMin(i, interpolant), searchMax(i, interpolant), interpolant);
   private int searchMax(int pos, BooleanFormula pInterpolant) throws SolverException, InterruptedException {
     for (int i = errorTrace.getTrace().size() - 1; i >= 0; i--) {
       if (isErrInv(pInterpolant, i)) {
         return Math.max(i, pos);
       }
     }
     return pos;
   }

   private int searchMin(int pos, BooleanFormula pInterpolant) throws SolverException, InterruptedException {
     for (int i = 0; i < errorTrace.getTrace().size(); i++) {
       if (isErrInv(pInterpolant, i)) {
         return Math.min(i, pos);
       }
     }
     return pos;
   }
  */

  /**
   * Summarize the abstract trace by summarizing interpolants which are followed by the same
   * selector
   *
   * @param abstractTrace the extended list
   * @param bmgr the boolean formula manager
   * @return summarized list
   */
  private List<AbstractTraceElement> summarize(
      List<AbstractTraceElement> abstractTrace, BooleanFormulaManager bmgr) {
    if (abstractTrace.size() < 2) {
      return new ArrayList<>(abstractTrace);
    }

    /*
    Example:           will be transformed to
    Interpolant 0      Interpolant 0 && Interpolant 1
    Selector 0         Selector 0
    Interpolant 1
    Selector 0
    Interpolant 2      Interpolant 2
    Selector 1         Selector 1
     */

    List<AbstractTraceElement> summarizedList = new ArrayList<>();
    TraceAtom lastSelector = null;

    for (AbstractTraceElement abstractTraceElement : abstractTrace) {
      if (abstractTraceElement instanceof TraceAtom) {
        if (abstractTraceElement.equals(lastSelector)) {
          Interval toMerge = (Interval) summarizedList.remove(summarizedList.size() - 3);
          Interval lastInterval = (Interval) summarizedList.remove(summarizedList.size() - 1);
          Interval merged = Interval.merge(toMerge, lastInterval, bmgr);
          summarizedList.add(summarizedList.size() - 1, merged);
        } else {
          summarizedList.add(abstractTraceElement);
          lastSelector = (TraceAtom) abstractTraceElement;
        }
      } else {
        summarizedList.add(abstractTraceElement);
      }
    }

    return summarizedList;
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
    ImmutableList<TraceAtom> allSelectors = ImmutableList.copyOf(errorTrace.getTrace());
    TraceAtom prev = allSelectors.get(0);
    Set<Fault> faults = new HashSet<>();
    for (int i = 0; i < abstractTrace.size(); i++) {
      AbstractTraceElement errorInvariant = abstractTrace.get(i);
      if (errorInvariant instanceof TraceAtom) {
        prev = (TraceAtom) errorInvariant;
        Fault singleton = new Fault(prev);
        singleton.setIntendedIndex(i);
        faults.add(singleton);
      } else if (errorInvariant instanceof Interval) {
        Interval curr = (Interval) errorInvariant;
        // curr.invariant =
        // formulaContext.getSolver().getFormulaManager().uninstantiate(curr.invariant);
        TraceAtom next;
        if (i + 1 < abstractTrace.size()) {
          next = (TraceAtom) abstractTrace.get(i + 1);
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
        curr.setIntendedIndex(i);
        // precondition has an own entry in FLInfo -> exclude it from here
        if (i != 0 || !curr.invariant.equals(errorTrace.getPrecondition().getPrecondition())) {
          faults.add(curr);
        }
      }
    }

    logger.log(
        Level.ALL,
        "Abstract error trace:",
        Appenders.forIterable(Joiner.on("\n - "), abstractTrace));
    logger.log(
        Level.FINEST,
        "tfresult=",
        FluentIterable.from(abstractTrace)
            .filter(tr -> !(tr instanceof Interval))
            .transform(
                fc ->
                    ((TraceAtom) fc)
                        .correspondingEdge()
                        .getFileLocation()
                        .getStartingLineInOrigin()));

    return faults;
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
  private int search(Solver solver, int low, int high, BooleanFormula interpolant, boolean negate)
      throws SolverException, InterruptedException {
    searchCalls.inc();
    if (high < low) {
      return low;
    }
    int mid = (low + high) / 2;
    // shortcut for if (negate) then !isErrInv else isErrInv
    if (isErrInv(solver, interpolant, mid) ^ negate) {
      return search(solver, mid + 1, high, interpolant, negate);
    } else {
      return search(solver, low, mid - 1, interpolant, negate);
    }
  }

  /**
   * Return if interpolant is inductive on position i.
   *
   * @param interpolant A interpolant
   * @param slicePosition where to slice the trace formula
   * @return true if interpolant is inductive at i, false else
   */
  private boolean isErrInv(Solver solver, BooleanFormula interpolant, int slicePosition)
      throws SolverException, InterruptedException {
    FormulaManagerView fmgr = solver.getFormulaManager();
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    int n = errorTrace.getTrace().size();
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

    TraceInterpreter interpreter =
        new ConjunctionTraceInterpreter(solver.getFormulaManager().getBooleanFormulaManager());

    BooleanFormula firstFormula =
        bmgr.implication(
            bmgr.and(
                errorTrace.getPrecondition().getPrecondition(),
                interpreter.interpret(errorTrace.getTrace().slice(slicePosition))),
            shiftedInterpolant);
    BooleanFormula secondFormula =
        bmgr.and(
            shiftedInterpolant,
            interpreter.interpret(errorTrace.getTrace().slice(slicePosition, n)),
            bmgr.not(errorTrace.getPostCondition().getPostCondition()));

    // isUnsat
    solverCalls.inc();
    boolean isValid = isValid(solver, firstFormula) && solver.isUnsat(secondFormula);
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
  private boolean isValid(Solver solver, BooleanFormula formula)
      throws SolverException, InterruptedException {
    solverCalls.inc();
    BooleanFormulaManager bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    return solver.isUnsat(bmgr.not(formula));
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

    private final int start;
    private final int end;
    private final BooleanFormula invariant;

    public Interval(int pStart, int pEnd, BooleanFormula pInvariant) {
      start = pStart;
      end = pEnd;
      invariant = pInvariant;
    }

    public static Interval merge(
        final Interval pFirst, final Interval pSecond, final BooleanFormulaManager pBmgr) {
      int newStart = Integer.min(pFirst.start, pSecond.start);
      int newEnd = Integer.max(pFirst.end, pSecond.end);
      return new Interval(
          newStart, newEnd, pBmgr.and(pFirst.getInvariant(), pSecond.getInvariant()));
    }

    public int getEnd() {
      return end;
    }

    public int getStart() {
      return start;
    }

    @Override
    public String toString() {
      return "Interval [" + start + ";" + end + "]: " + invariant;
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

    public BooleanFormula getInvariant() {
      return invariant;
    }

    @Override
    public int hashCode() {
      return Objects.hash(invariant, start, end, super.hashCode());
    }
  }
}
