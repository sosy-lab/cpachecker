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

import static org.sosy_lab.cpachecker.util.AbstractStates.*;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.InvariantsManager.InvariantGenerationStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.InvariantsManager.InvariantUsageStrategy;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.cwriter.LoopCollectingEdgeVisitor;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.solver.api.BooleanFormula;

import com.google.common.collect.Lists;

/**
 * This class provides a basic refiner implementation for predicate analysis.
 * When a counterexample is found, it creates a path for it and checks it for
 * feasibility, getting the interpolants if possible.
 *
 * It does not define any strategy for using the interpolants to update the
 * abstraction, this is left to an instance of {@link RefinementStrategy}.
 *
 * It does, however, produce a nice error path in case of a feasible counterexample.
 */
@Options(prefix="cpa.predicate.refinement")
public class PredicateCPARefinerWithInvariants extends PredicateCPARefiner {

  @Option(secure=true, description="Timelimit for invariant generation which may be"
                                 + " used during refinement.\n"
                                 + "(Use seconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.NANOSECONDS,
                  defaultUserUnit=TimeUnit.SECONDS,
                  min=0)
  private TimeSpan timeForInvariantGeneration = TimeSpan.ofNanos(0);

  @Option(secure=true, description="For differing errorpaths, the loop for which"
      + " invariants should be generated may still be the same, with this option"
      + " you can set the maximal amount of invariant generation runs per loop."
      + " 0 means no upper limit given.")
  private int maxInvariantGenerationsPerLoop = 2;

  @Option(secure=true, description="Invariants that are not strong enough to"
      + " refute the counterexample can be ignored with this option."
      + " (Weak invariants will lead to repeated counterexamples, thus taking"
      + " time which could be used for the rest of the analysis, however, the"
      + " found invariants may also be better for loops as interpolation.)")
  private boolean useStrongInvariantsOnly = true;

  @Option(secure=true, description="use only atoms from generated invariants"
                                 + "as predicates, and not the whole invariant")
  private boolean atomicInvariants = false;

  @Option(
    secure = true,
    description =
        "Which strategy should be used for generating"
            + " invariants, a comma separated list can be specified. In case one strategy fails,"
            + " the next one is used."
  )
  private List<InvariantGenerationStrategy> invariantGenerationStrategy =
      Lists.newArrayList(InvariantGenerationStrategy.PF_INDUCTIVE_WEAKENING);

  @Option(
    secure = true,
    description = "Where should the generated invariants (if there are some) be used?"
  )
  private InvariantUsageStrategy invariantUsageStrategy =
      InvariantUsageStrategy.ABSTRACTION_FORMULA;

  private final InvariantsManager invariantsManager;
  private final LoopStructure loopStructure;
  private final Map<Loop, Integer> loopOccurrences = new HashMap<>();
  private boolean wereInvariantsGenerated = false;

  private final Configuration config;
  private final Stats stats = new Stats();

  // the previously analyzed counterexample to detect repeated counterexamples
  private List<CFANode> lastErrorPath = null;

  // we get the configuration out of the pCPA object and do not need another one
  @SuppressWarnings("options")
  public PredicateCPARefinerWithInvariants(
      final ConfigurableProgramAnalysis pCpa,
      final InterpolationManager pInterpolationManager,
      final PathChecker pPathChecker,
      final PrefixProvider pPrefixProvider,
      final RefinementStrategy pStrategy)
      throws InvalidConfigurationException {

    super(pCpa, pInterpolationManager, pPathChecker, pPrefixProvider, pStrategy);
    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);

    config = predicateCpa.getConfiguration();
    config.inject(this, PredicateCPARefinerWithInvariants.class);

    invariantsManager = predicateCpa.getInvariantsManager();
    loopStructure = predicateCpa.getCfa().getLoopStructure().get();
  }

  @Override
  public final CounterexampleInfo performRefinement(final ARGReachedSet pReached, final ARGPath allStatesTrace) throws CPAException, InterruptedException {

    // no invariants should be generated, we can do an interpolating refinement immediately
    switch (invariantUsageStrategy) {
      case NONE:
        return super.performRefinement(pReached, allStatesTrace);
      case REFINEMENT:
      case ABSTRACTION_FORMULA:
      case COMBINATION:
        return performRefinement0(pReached, allStatesTrace);
      default:
        throw new AssertionError("Unhandled case statement");
    }
  }

  private CounterexampleInfo performRefinement0(
      final ARGReachedSet pReached, final ARGPath allStatesTrace)
      throws CPAException, InterruptedException {

    final List<CFANode> errorPath = Lists.transform(allStatesTrace.asStatesList(), AbstractStates.EXTRACT_LOCATION);
    final boolean repeatedCounterexample = errorPath.equals(lastErrorPath);
    lastErrorPath = errorPath;

    Set<Loop> loopsInPath;

    // check if invariants can be used at all
    if ((loopsInPath = canInvariantsBeUsed(allStatesTrace, repeatedCounterexample)).isEmpty()) {
      return super.performRefinement(pReached, allStatesTrace);
    }


    Set<ARGState> elementsOnPath = extractElementsOnPath(allStatesTrace);

    // No branches/merges in path, it is precise.
    // We don't need to care about creating extra predicates for branching etc.
    boolean branchingOccurred = true;
    if (elementsOnPath.size() == allStatesTrace.size()) {
      elementsOnPath = Collections.emptySet();
      branchingOccurred = false;
    }

    // create path with all abstraction location elements (excluding the initial element)
    // the last element is the element corresponding to the error location
    final List<ARGState> abstractionStatesTrace = transformPath(allStatesTrace);
    totalPathLength.setNextValue(abstractionStatesTrace.size());

    logger.log(Level.ALL, "Abstraction trace is", abstractionStatesTrace);

    // create list of formulas on path
    final List<BooleanFormula> formulas = createFormulasOnPath(allStatesTrace, abstractionStatesTrace);

    CounterexampleTraceInfo counterexample =
        formulaManager.buildCounterexampleTrace(
            formulas,
            Lists.<AbstractState>newArrayList(abstractionStatesTrace),
            elementsOnPath,
            false);

    // if error is spurious refine
    if (counterexample.isSpurious()) {
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");


      List<Pair<PathFormula, CFANode>> argForPathFormulaBasedGeneration = new ArrayList<>();
      for (ARGState state : abstractionStatesTrace) {
        CFANode node = extractLocation(state);
        if (loopStructure.getAllLoopHeads().contains(node)) {
          PredicateAbstractState predState =
              extractStateByType(state, PredicateAbstractState.class);
          PathFormula pathFormula = predState.getPathFormula();
          argForPathFormulaBasedGeneration.add(Pair.of(pathFormula, node));
        }
      }

      Triple<ARGPath, List<ARGState>, Set<Loop>> argForErrorPathBasedGeneration =
          Triple.of(allStatesTrace, abstractionStatesTrace, loopsInPath);

      List<BooleanFormula> precisionIncrement = null;
      for (InvariantGenerationStrategy invGenStrategy : invariantGenerationStrategy) {
        invariantsManager.findInvariants(
            invariantUsageStrategy,
            invGenStrategy,
            argForPathFormulaBasedGeneration,
            argForErrorPathBasedGeneration);
        precisionIncrement = invariantsManager.getInvariantsForRefinement();
        if (!precisionIncrement.isEmpty()) {
          break;
        }
      }

      // fall-back to interpolation
      if (precisionIncrement.isEmpty()) {
        precisionIncrement =
            formulaManager
                .buildCounterexampleTrace(
                    formulas,
                    Lists.<AbstractState>newArrayList(abstractionStatesTrace),
                    elementsOnPath,
                    true)
                .getInterpolants();
      } else {
        stats.succInvariantRefinements.setNextValue(1);
        wereInvariantsGenerated = true;
      }

      if (strategy instanceof PredicateAbstractionRefinementStrategy) {
        ((PredicateAbstractionRefinementStrategy)strategy).setUseAtomicPredicates(atomicInvariants);
      }

      strategy.performRefinement(pReached, abstractionStatesTrace, precisionIncrement, repeatedCounterexample);

      return CounterexampleInfo.spurious();

    } else {
      // we have a real error
      logger.log(Level.FINEST, "Error trace is not spurious");
      // we need interpolants for creating a precise error path
      counterexample = formulaManager.buildCounterexampleTrace(formulas,
          Lists.<AbstractState>newArrayList(abstractionStatesTrace),
          elementsOnPath,
          true);
      CounterexampleInfo cex = handleRealError(allStatesTrace, branchingOccurred, counterexample);

      return cex;
    }
  }

  /**
   * An empty set signalizes that invariants cannot be used.
   */
  private Set<Loop> canInvariantsBeUsed(
      final ARGPath allStatesTrace, final boolean repeatedCounterexample) {
    // nothing was computed up to now, so just call refinement of
    // our super class if we have a repeated counter example
    // or we don't even need a precision increment
    if (repeatedCounterexample || !strategy.needsInterpolants()) {
      if (repeatedCounterexample && useStrongInvariantsOnly && wereInvariantsGenerated) {
        logger.log(
            Level.WARNING,
            "Repeated Countereample although generated invariants were strong"
                + " enough to refute it. Falling back to interpolation.");
      }

      // only interpolation or invariant-based refinements should be counted
      // as repeated error paths
      if (!strategy.needsInterpolants()) {
        lastErrorPath = null;
      }
      wereInvariantsGenerated = false;
      return Collections.emptySet();
    }

    // get the relevant loops in the ARGPath and the number of occurrences of
    // the most often found one
    Set<Loop> loopsInPath = getRelevantLoops(allStatesTrace);
    int maxFoundLoop = getMaxCountOfOccuredLoop(loopsInPath);

    // no loops found, use normal interpolation refinement
    if (maxFoundLoop > maxInvariantGenerationsPerLoop || loopsInPath.isEmpty()) {
      wereInvariantsGenerated = false;
      return Collections.emptySet();
    }
    return loopsInPath;
  }

  /**
   * Returns the maximal number of occurences of one of the loops given in the
   * parameter. This method takes loops found in earlier refinements into account.
   */
  private int getMaxCountOfOccuredLoop(Set<Loop> loopsInPath) {
    int maxFoundLoop = 0;
    for (Loop loop : loopsInPath) {
      if (loopOccurrences.containsKey(loop)) {
        int tmpFoundLoop = loopOccurrences.get(loop) + 1;
        if (tmpFoundLoop > maxFoundLoop) {
          maxFoundLoop = tmpFoundLoop;
        }
        loopOccurrences.put(loop, tmpFoundLoop);
      } else {
        loopOccurrences.put(loop, 1);
        if (maxFoundLoop == 0) {
          maxFoundLoop = 1;
        }
      }
    }
    return maxFoundLoop;
  }

  /**
   * This method returns the set of loops which are relevant for the given
   * ARGPath.
   */
  private Set<Loop> getRelevantLoops(final ARGPath allStatesTrace) {
    PathIterator pathIt = allStatesTrace.pathIterator();
    LoopCollectingEdgeVisitor loopFinder = null;

    try {
      loopFinder = new LoopCollectingEdgeVisitor(cfa.getLoopStructure().get(), config);
    } catch (InvalidConfigurationException e1) {
      // this will never happen, but for the case it does, we just return
      // the empty set, therefore the refinement will be done without invariant
      // generation definitely and only with interpolation / static refinement
      return Collections.emptySet();
    }

    while(pathIt.hasNext()) {
      loopFinder.visit(pathIt.getAbstractState(), pathIt.getOutgoingEdge(), null);
      pathIt.advance();
    }

    return loopFinder.getRelevantLoops().keySet();
  }

  private class Stats extends AbstractStatistics {

    private final StatInt succInvariantRefinements =
        new StatInt(StatKind.COUNT, "Number of successful invariants refinements");
    private final StatInt totalInductiveRefinements =
        new StatInt(StatKind.COUNT, "Number of invariant refinements with k-induction");
    private final StatInt succInductiveRefinements =
        new StatInt(StatKind.COUNT, "Number of successful refinements with k-induction");

    @Override
    public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
      StatisticsWriter w0 = writingStatisticsTo(out);

      int numberOfRefinements = totalRefinement.getUpdateCount();
      if (numberOfRefinements > 0) {
        w0.beginLevel().put(succInvariantRefinements);
        w0.beginLevel().put(totalInductiveRefinements);
        w0.beginLevel().beginLevel().put(succInductiveRefinements);
      }
    }

    @Override
    public String getName() {
      return strategy.getStatistics().getName() + " with Invariants";
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    pStatsCollection.add(stats);
  }
}
