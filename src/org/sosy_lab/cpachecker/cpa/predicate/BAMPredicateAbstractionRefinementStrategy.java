/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateReducer.ReducedPredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.RefineableRelevantPredicatesComputer;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.RelevantPredicatesComputer;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * This is an extension of {@link PredicateAbstractionRefinementStrategy} that takes care of
 * updating the BAM state.
 */
public class BAMPredicateAbstractionRefinementStrategy extends PredicateAbstractionRefinementStrategy {

  private final BAMPredicateCPA predicateCpa;
  private boolean secondRepeatedCEX = false;

  protected BAMPredicateAbstractionRefinementStrategy(
      final Configuration config,
      final LogManager logger,
      final BAMPredicateCPA predicateCpa,
      final Solver pSolver,
      final PredicateAbstractionManager pPredAbsMgr)
      throws InvalidConfigurationException {

    super(config, logger, pPredAbsMgr, pSolver);
    this.predicateCpa = predicateCpa;
  }

  @Override
  public boolean performRefinement(
      ARGReachedSet pReached,
      List<ARGState> abstractionStatesTrace,
      List<BooleanFormula> pInterpolants,
      boolean pRepeatedCounterexample)
      throws CPAException, InterruptedException {

    boolean furtherCEXTrackingNeeded = true;

    // overriding this method is needed, as, in principle, it is possible
    // -- to get two successive spurious counterexamples, which only differ in its abstractions
    // (with 'aggressive caching').
    // -- to have an imprecise predicate-reduce-operator, which can be refined.

    // use flags to wait for the second repeated CEX
    if (!pRepeatedCounterexample) {
      pRepeatedCounterexample = false;
      secondRepeatedCEX = false;
    } else if (pRepeatedCounterexample && !secondRepeatedCEX) {
      pRepeatedCounterexample = false;
      secondRepeatedCEX = true;
    }

    // in case of a (twice) repeated CEX,
    // we try to improve the reduce-operator by refining the relevantPredicatesComputer.
    else if (pRepeatedCounterexample && secondRepeatedCEX) {
      final RelevantPredicatesComputer relevantPredicatesComputer =
          predicateCpa.getRelevantPredicatesComputer();
      if (relevantPredicatesComputer instanceof RefineableRelevantPredicatesComputer) {
        // even abstractions agree; try refining relevant predicates reducer
        RelevantPredicatesComputer newRelevantPredicatesComputer =
            refineRelevantPredicatesComputer(
                abstractionStatesTrace,
                pReached,
                (RefineableRelevantPredicatesComputer) relevantPredicatesComputer);

        if (newRelevantPredicatesComputer.equals(relevantPredicatesComputer)) {
          // repeated CEX && relevantPredicatesComputer was refined && refinement does not produce
          // progress -> error
          // TODO if this happens, there might be a bug in the analysis!
          // throw new RefinementFailedException(Reason.RepeatedCounterexample, null);

        } else {
          // we have a better relevantPredicatesComputer, thus update it.
          logger.logf(
              Level.FINEST,
              "refining relevantPredicatesComputer from %s to %s",
              relevantPredicatesComputer,
              newRelevantPredicatesComputer);
          predicateCpa.setRelevantPredicatesComputer(newRelevantPredicatesComputer);

          // reset flags and continue
          pRepeatedCounterexample = false;
          secondRepeatedCEX = false;
          furtherCEXTrackingNeeded = false;
        }

        // } else {
        // throw new RefinementFailedException(Reason.RepeatedCounterexample, null);
      }
    }

    super.performRefinement(
        pReached, abstractionStatesTrace, pInterpolants, pRepeatedCounterexample);

    return furtherCEXTrackingNeeded;
  }

  /**
   * In case of repeated counter-example, we try to improve the (dynamic) predicate-reducer by
   * adding all predicates (that match the block's locations) as relevant predicates. This
   * overrides/improves the formula-based reducing of abstractions and precision, where
   * substring-matching against block-local variables is performed.
   *
   * @return the refined relevantPredicateComputer
   */
  private RelevantPredicatesComputer refineRelevantPredicatesComputer(
      List<ARGState> abstractionStatesTrace,
      ARGReachedSet pReached,
      RefineableRelevantPredicatesComputer relevantPredicatesComputer) {
    UnmodifiableReachedSet reached = pReached.asReachedSet();
    Precision oldPrecision = reached.getPrecision(reached.getLastState());
    PredicatePrecision oldPredicatePrecision =
        Precisions.extractPrecisionByType(oldPrecision, PredicatePrecision.class);

    // we have to use the old root-precision, because the direct precision might have been reduced.
    // the root-precision should contain "all" predicates generated in the last refinement.
    if (oldPredicatePrecision instanceof ReducedPredicatePrecision) {
      oldPredicatePrecision =
          ((ReducedPredicatePrecision) oldPredicatePrecision).getRootPredicatePrecision();
    }

    BlockPartitioning partitioning = predicateCpa.getPartitioning();
    Deque<Block> openBlocks = new ArrayDeque<>();
    openBlocks.push(partitioning.getMainBlock());
    for (ARGState pathElement : abstractionStatesTrace) {
      CFANode currentNode = AbstractStates.extractLocation(pathElement);
      Integer currentNodeInstance =
          getPredicateState(pathElement).getAbstractionLocationsOnPath().get(currentNode);

      Set<AbstractionPredicate> localPreds =
          oldPredicatePrecision.getPredicates(currentNode, currentNodeInstance);
      for (Block block : openBlocks) {
        relevantPredicatesComputer =
            relevantPredicatesComputer.considerPredicatesAsRelevant(block, localPreds);
      }

      // first pop, then push -- otherwise we loose blocks that are entered at block-exit-locations.
      while (!openBlocks.isEmpty() && openBlocks.peek().isReturnNode(currentNode)) {
        openBlocks.pop();
      }

      if (partitioning.isCallNode(currentNode)) {
        final Block calledBlock = partitioning.getBlockForCallNode(currentNode);
        relevantPredicatesComputer =
            relevantPredicatesComputer.considerPredicatesAsRelevant(calledBlock, localPreds);
        openBlocks.push(calledBlock);
      }
    }
    return relevantPredicatesComputer;
  }
}
