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

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interpolation.AbstractInterpolationBasedRefiner;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * This class provides the refinement strategy for the classical predicate
 * abstraction (adding the predicates from the interpolant to the precision
 * and removing the relevant parts of the ARG).
 */
@Options(prefix="cpa.predicate.refinement")
public class PredicateRefiner extends AbstractInterpolationBasedRefiner<Collection<AbstractionPredicate>> implements StatisticsProvider {

  @Option(description="refinement will add all discovered predicates "
          + "to all the locations in the abstract trace")
  private boolean addPredicatesGlobally = false;

  @Option(description="During refinement, keep predicates from all removed parts " +
                      "of the ARG. Otherwise, only predicates from the error path " +
                      "are kept.")
  private boolean keepAllPredicates = false;

  @Option(description="Do a complete restart (clearing the reached set) " +
                      "after N refinements. 0 to disable, 1 for always.")
  @IntegerOption(min=0)
  private int restartAfterRefinements = 0;

  @Option(description="During refinement, add all new predicates to the precisions " +
                      "of all abstract states in the reached set.")
  private boolean sharePredicates = false;

  private int refinementCount = 0; // this is modulo restartAfterRefinements

  private class Stats implements Statistics {
    @Override
    public String getName() {
      return "Predicate Abstraction Refiner";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      PredicateRefiner.this.printStatistics(out, pResult, pReached);
      out.println("  Precision update:               " + precisionUpdate);
      out.println("  ARG update:                     " + argUpdate);
    }
  }

  private final Timer precisionUpdate = new Timer();
  private final Timer argUpdate = new Timer();

  public static PredicateRefiner create(ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    PredicateCPA predicateCpa = CPAs.retrieveCPA(pCpa, PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(PredicateRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    LogManager logger = predicateCpa.getLogger();

    PredicateRefinementManager manager = new PredicateRefinementManager(predicateCpa.getFormulaManager(),
                                          predicateCpa.getPathFormulaManager(),
                                          predicateCpa.getSolver(),
                                          predicateCpa.getAbstractionManager(),
                                          predicateCpa.getFormulaManagerFactory(),
                                          predicateCpa.getConfiguration(),
                                          logger);

    return new PredicateRefiner(predicateCpa.getConfiguration(), logger, pCpa, manager);
  }

  protected PredicateRefiner(final Configuration config, final LogManager logger,
      final ConfigurableProgramAnalysis pCpa,
      final PredicateRefinementManager pInterpolationManager) throws CPAException, InvalidConfigurationException {

    super(config, logger, pCpa, pInterpolationManager);

    config.inject(this, PredicateRefiner.class);
  }

  @Override
  protected final List<ARGState> transformPath(Path pPath) {
    List<ARGState> result = from(pPath)
      .skip(1)
      .transform(Pair.<ARGState>getProjectionToFirst())
      .filter(Predicates.compose(PredicateAbstractState.FILTER_ABSTRACTION_STATES,
                                 toState(PredicateAbstractState.class)))
      .toImmutableList();

    assert from(result).allMatch(new Predicate<ARGState>() {
      @Override
      public boolean apply(@Nullable ARGState pInput) {
        boolean correct = pInput.getParents().size() <= 1;
        assert correct : "PredicateRefiner expects abstraction states to have only one parent, but this state has more:" + pInput;
        return correct;
      }
    });

    assert pPath.getLast().getFirst() == result.get(result.size()-1);
    return result;
  }

  private static final Function<PredicateAbstractState, Formula> GET_BLOCK_FORMULA
                = new Function<PredicateAbstractState, Formula>() {
                    @Override
                    public Formula apply(PredicateAbstractState e) {
                      assert e.isAbstractionState();
                      return e.getAbstractionFormula().getBlockFormula();
                    }
                  };

  @Override
  protected List<Formula> getFormulasForPath(List<ARGState> path, ARGState initialState) throws CPATransferException {
    return from(path)
        .transform(toState(PredicateAbstractState.class))
        .transform(GET_BLOCK_FORMULA)
        .toImmutableList();
  }

  @Override
  protected void performRefinement(ARGReachedSet pReached,
      List<ARGState> pPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> pCounterexample,
      boolean pRepeatedCounterexample) throws CPAException {

    precisionUpdate.start();

    // get previous precision
    UnmodifiableReachedSet reached = pReached.asReachedSet();
    PredicatePrecision targetStatePrecision = extractPredicatePrecision(reached.getPrecision(reached.getLastState()));

    // collect predicates from refinement and find refinement root
    Pair<ARGState, Multimap<CFANode, AbstractionPredicate>> refinementResult =
            performRefinement(targetStatePrecision, pPath, pCounterexample, pRepeatedCounterexample);

    ARGState refinementRoot = refinementResult.getFirst();
    Multimap<CFANode, AbstractionPredicate> newPredicates = refinementResult.getSecond();

    // check whether we should restart
    refinementCount++;
    if (restartAfterRefinements > 0 && refinementCount >= restartAfterRefinements) {
      ARGState root = (ARGState)reached.getFirstState();
      // we have to use the child as the refinementRoot
      assert root.getChildren().size() == 1 : "ARG root should have exactly one child";
      refinementRoot = Iterables.getLast(root.getChildren());

      logger.log(Level.FINEST, "Restarting analysis after",refinementCount,"refinements by clearing the ARG.");
      refinementCount = 0;
    }

    // now create new precision
    PredicatePrecision basePrecision;
    if (keepAllPredicates) {
      basePrecision = findAllPredicatesFromSubgraph(refinementRoot, reached);
    } else {
      basePrecision = targetStatePrecision;
    }

    PredicatePrecision newPrecision;
    if (addPredicatesGlobally) {
      newPrecision = basePrecision.addGlobalPredicates(newPredicates.values());
    } else {
      newPrecision = basePrecision.addLocalPredicates(newPredicates);
    }

    logger.log(Level.ALL, "Predicate map now is", newPrecision);

    precisionUpdate.stop();


    argUpdate.start();

    pReached.removeSubtree(refinementRoot, newPrecision);

    assert (refinementCount > 0) || reached.size() == 1;

    if (sharePredicates) {
      pReached.updatePrecisionGlobally(newPrecision);
    }

    argUpdate.stop();
  }

  protected PredicatePrecision extractPredicatePrecision(Precision oldPrecision) throws IllegalStateException {
    PredicatePrecision oldPredicatePrecision = Precisions.extractPrecisionByType(oldPrecision, PredicatePrecision.class);
    if (oldPredicatePrecision == null) {
      throw new IllegalStateException("Could not find the PredicatePrecision for the error element");
    }
    return oldPredicatePrecision;
  }

  private Pair<ARGState, Multimap<CFANode, AbstractionPredicate>> performRefinement(
      PredicatePrecision oldPrecision, List<ARGState> pPath,
      CounterexampleTraceInfo<Collection<AbstractionPredicate>> pInfo,
      boolean pRepeatedCounterexample) throws CPAException {

    List<Collection<AbstractionPredicate>> newPreds = pInfo.getPredicatesForRefinement();

    // target state is not really an interpolation point, exclude it
    List<ARGState> interpolationPoints = pPath.subList(0, pPath.size()-1);
    assert interpolationPoints.size() == newPreds.size();

    boolean predicatesFound = false;
    boolean newPredicatesFound = false;
    ARGState firstInterpolationPoint = null;

    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
    pmapBuilder.putAll(oldPrecision.getLocalPredicates());

    // iterate through interpolationPoints and find first point with new predicates, from there we have to cut the ARG
    // also build new precision
    int i = 0;
    for (ARGState interpolationPoint : interpolationPoints) {
      Collection<AbstractionPredicate> localPreds = newPreds.get(i++);

      if (localPreds.size() > 0) {
        // found predicates
        predicatesFound = true;
        CFANode loc = AbstractStates.extractLocation(interpolationPoint);

        if (firstInterpolationPoint == null) {
          firstInterpolationPoint = interpolationPoint;
        }

        if (!oldPrecision.getPredicates(loc).containsAll(localPreds)) {
          // new predicates for this location

          System.out.println(localPreds);

          newPredicatesFound = true;
          pmapBuilder.putAll(loc, localPreds);
        }

      }
    }
    if (!predicatesFound) {
      // The only reason why this might appear is that the very last block is
      // infeasible in itself, however, we check for such cases during strengthen,
      // so they shouldn't appear here.
      throw new RefinementFailedException(RefinementFailedException.Reason.InterpolationFailed, null);
    }
    assert firstInterpolationPoint != null;

    // We have two different strategies for the refinement root: set it to
    // the firstInterpolationPoint or set it to highest location in the ARG
    // where the same CFANode appears.
    // Both work, so this is a heuristics question to get the best performance.
    // My benchmark showed, that at least for the benchmarks-lbe examples it is
    // best to use strategy one iff newPredicatesFound.

    ARGState refinementRoot = null;
    if (newPredicatesFound) {
      refinementRoot = firstInterpolationPoint;

      logger.log(Level.FINEST, "Found spurious counterexample,",
          "trying strategy 1: remove everything below", refinementRoot, "from ARG.");

    } else {
      if (pRepeatedCounterexample) {
        throw new RefinementFailedException(RefinementFailedException.Reason.RepeatedCounterexample, null);
      }

      CFANode firstInterpolationPointLocation = AbstractStates.extractLocation(firstInterpolationPoint);

      logger.log(Level.FINEST, "Found spurious counterexample,",
          "trying strategy 2: remove everything below node", firstInterpolationPointLocation, "from ARG.");

      // find first element in path with location == firstInterpolationPointLocation,
      // this is not necessary equal to firstInterpolationPoint
      for (ARGState abstractionPoint : pPath) {
        CFANode loc = AbstractStates.extractLocation(abstractionPoint);
        if (loc.equals(firstInterpolationPointLocation)) {
          refinementRoot = abstractionPoint;
          break;
        }
      }
      if (refinementRoot == null) {
        throw new CPAException("Inconsistent ARG, did not find element for " + firstInterpolationPointLocation);
      }
    }
    return Pair.of(refinementRoot, (Multimap<CFANode, AbstractionPredicate>)pmapBuilder.build());
  }

  /**
   * Collect all precisions in the subgraph below refinementRoot and merge
   * their predicates.
   * @return a new precision with all these predicates.
   */
  private PredicatePrecision findAllPredicatesFromSubgraph(
      ARGState refinementRoot, UnmodifiableReachedSet reached) {

    PredicatePrecision newPrecision = PredicatePrecision.empty();

    // find all distinct precisions to merge them
    Set<Precision> precisions = Sets.newIdentityHashSet();
    for (ARGState state : refinementRoot.getSubgraph()) {
      if (!state.isCovered()) {
        // covered states are not in reached set
        precisions.add(reached.getPrecision(state));
      }
    }

    for (Precision prec : precisions) {
      newPrecision = newPrecision.mergeWith(extractPredicatePrecision(prec));
    }
    return newPrecision;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
  }
}
