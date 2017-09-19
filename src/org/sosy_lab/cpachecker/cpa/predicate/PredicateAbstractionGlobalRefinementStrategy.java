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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Refinement strategy for a global predicate abstraction refinement. Global means
 * that we do not have only one error path in the reached set but multiple, and
 * they all need to be refined.
 */
@Options(prefix = "cpa.predicate")
class PredicateAbstractionGlobalRefinementStrategy extends GlobalRefinementStrategy {

  @Option(
    secure = true,
    name = "refinement.sharePredicates",
    description =
        "During refinement, add all new predicates to the precisions "
            + "of all abstract states in the reached set."
  )
  private boolean sharePredicates = false;

  @Option(
    secure = true,
    name = "refinement.global.restartAfterRefinement",
    description = "Do a complete restart (clearing the reached set) after the refinement"
  )
  private boolean restartAfterRefinement = false;

  private boolean atomicPredicates = false;

  protected final LogManager logger;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final PredicateAbstractionManager predAbsMgr;

  private StatTimer predicateCreation = new StatTimer(StatKind.SUM, "Predicate creation");
  private StatTimer precisionUpdate = new StatTimer(StatKind.SUM, "Precision update");
  private StatTimer argUpdate = new StatTimer(StatKind.SUM, "ARG update");

  private ListMultimap<CFANode, AbstractionPredicate> newPredicates;
  private ARGReachedSet reached;
  private ARGState refinementRoot;

  protected PredicateAbstractionGlobalRefinementStrategy(
      final Configuration config,
      final LogManager pLogger,
      final PredicateAbstractionManager pPredAbsMgr,
      final Solver pSolver)
      throws InvalidConfigurationException {
    super(pSolver);

    config.inject(this, PredicateAbstractionGlobalRefinementStrategy.class);

    logger = pLogger;
    fmgr = pSolver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    predAbsMgr = pPredAbsMgr;
  }

  @Override
  protected void startRefinementOfPath() {
    // do nothing here we only need a global start refinement
  }

  @Override
  public void initializeGlobalRefinement() {
    checkState(newPredicates == null);
    // needs to be a fully deterministic data structure,
    // thus a Multimap based on a LinkedHashMap
    // (we iterate over the keys)
    newPredicates = MultimapBuilder.linkedHashKeys().arrayListValues().build();
  }

  @Override
  public boolean performRefinement(
      ARGReachedSet pReached,
      List<ARGState> pAbstractionStatesTrace,
      List<BooleanFormula> pInterpolants,
      boolean pRepeatedCounterexample)
      throws CPAException, InterruptedException {
    Preconditions.checkState(
        newPredicates != null,
        "#initializeGlobalRefinement has to be called before performing a refinement.");

    if (reached == null) {
      reached = pReached;
    } else if (reached != pReached) {
      throw new IllegalStateException("During global refinement, reached set may not be changed.");
    }

    return super.performRefinement(
        reached, pAbstractionStatesTrace, pInterpolants, pRepeatedCounterexample);
  }

  @Override
  public void updatePrecisionAndARG() throws InterruptedException {
    PredicatePrecision newPrecision = computeNewPrecision();

    updateARG(newPrecision, refinementRoot);

    // reset reached set to null, for next global refinement
    reached = null;
    refinementRoot = null;
    newPredicates = null;
  }

  @Override
  public void resetGlobalRefinement() {
    // reset reached set to null, for next global refinement
    reached = null;
    refinementRoot = null;
    newPredicates = null;
  }

  protected void updateARG(PredicatePrecision pNewPrecision, ARGState pRefinementRoot)
      throws InterruptedException {

    argUpdate.start();

    List<Precision> precisions = new ArrayList<>(2);
    List<Predicate<? super Precision>> precisionTypes = new ArrayList<>(2);

    precisions.add(pNewPrecision);
    precisionTypes.add(Predicates.instanceOf(PredicatePrecision.class));

    UnmodifiableReachedSet unmodifiableReached = reached.asReachedSet();

    if (isValuePrecisionAvailable(pRefinementRoot)) {
      precisions.add(mergeAllValuePrecisionsFromSubgraph(pRefinementRoot, unmodifiableReached));
      precisionTypes.add(VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));
    }

    reached.removeSubtree(pRefinementRoot, precisions, precisionTypes);

    if (sharePredicates) {
      reached.updatePrecisionGlobally(
          pNewPrecision, Predicates.instanceOf(PredicatePrecision.class));
    }

    argUpdate.stop();
  }

  private boolean isValuePrecisionAvailable(ARGState root) {
    if (!reached.asReachedSet().contains(root)) {
      return false;
    }
    return Precisions.extractPrecisionByType(
            reached.asReachedSet().getPrecision(root), VariableTrackingPrecision.class)
        != null;
  }

  private VariableTrackingPrecision mergeAllValuePrecisionsFromSubgraph(
      ARGState refinementRoot, UnmodifiableReachedSet reached) {

    VariableTrackingPrecision rootPrecision =
        Precisions.extractPrecisionByType(
            reached.getPrecision(refinementRoot), VariableTrackingPrecision.class);

    // find all distinct precisions to merge them
    Set<Precision> precisions = Sets.newIdentityHashSet();
    for (ARGState state : refinementRoot.getSubgraph()) {
      if (!state.isCovered()) {
        // covered states are not in reached set
        precisions.add(reached.getPrecision(state));
      }
    }

    for (Precision prec : precisions) {
      rootPrecision =
          rootPrecision.join(
              Precisions.extractPrecisionByType(prec, VariableTrackingPrecision.class));
    }

    return rootPrecision;
  }

  @Override
  protected boolean performRefinementForState(BooleanFormula pInterpolant, ARGState pState)
      throws InterruptedException, SolverException {
    checkState(newPredicates != null);
    checkArgument(!bfmgr.isTrue(pInterpolant));

    predicateCreation.start();
    newPredicates.putAll(extractLocation(pState), convertInterpolant(pInterpolant));
    predicateCreation.stop();

    return false;
  }

  private final PredicatePrecision computeNewPrecision() {
    // get previous precision
    UnmodifiableReachedSet unmodifiableReached = reached.asReachedSet();

    logger.log(Level.FINEST, "Removing everything below", refinementRoot, "from ARG.");

    // now create new precision
    precisionUpdate.start();
    PredicatePrecision basePrecision =
        findAllPredicatesFromSubgraph(refinementRoot, unmodifiableReached);

    logger.log(Level.ALL, "Old predicate map is", basePrecision);
    logger.log(Level.ALL, "New predicates are", newPredicates);

    PredicatePrecision newPrecision = basePrecision.addLocalPredicates(newPredicates.entries());

    logger.log(Level.ALL, "Predicate map now is", newPrecision);

    assert basePrecision.calculateDifferenceTo(newPrecision) == 0
        : "We forgot predicates during refinement!";

    precisionUpdate.stop();

    return newPrecision;
  }

  /**
   * Collect all precisions in the subgraph below refinementRoot and merge
   * their predicates.
   * @return a new precision with all these predicates.
   */
  private PredicatePrecision findAllPredicatesFromSubgraph(
      ARGState refinementRoot, UnmodifiableReachedSet reached) {
    return PredicatePrecision.unionOf(
        from(refinementRoot.getSubgraph())
            .filter(not(ARGState::isCovered))
            .transform(reached::getPrecision));
  }

  /**
   * Get the predicates out of an interpolant.
   * @param pInterpolant The interpolant formula.
   * @return A set of predicates.
   */
  private final Collection<AbstractionPredicate> convertInterpolant(
      final BooleanFormula pInterpolant) {

    BooleanFormula interpolant = pInterpolant;

    if (bfmgr.isTrue(interpolant)) {
      return Collections.<AbstractionPredicate>emptySet();
    }

    Collection<AbstractionPredicate> preds;

    if (atomicPredicates) {
      preds = predAbsMgr.getPredicatesForAtomsOf(interpolant);

    } else {
      preds = ImmutableList.of(predAbsMgr.getPredicateFor(interpolant));
    }

    assert !preds.isEmpty()
        : "Interpolant without relevant predicates: "
            + pInterpolant
            + "; simplified to "
            + interpolant;

    logger.log(Level.FINEST, "Got predicates", preds);

    return preds;
  }

  /**
   * After a path was strengthened, we need to take care of the coverage relation.
   * We also remove the infeasible part from the ARG,
   * and re-establish the coverage invariant (i.e., that states on the path
   * are either covered or cannot be covered).
   */
  @Override
  protected void finishRefinementOfPath(
      ARGState infeasiblePartOfART,
      List<ARGState> changedElements,
      ARGReachedSet pReached,
      boolean pRepeatedCounterexample)
      throws CPAException, InterruptedException {
    // only thing to do here is adding the false predicate for unreacheable states
    newPredicates.put(extractLocation(infeasiblePartOfART), predAbsMgr.makeFalsePredicate());
    changedElements.add(infeasiblePartOfART);

    if (restartAfterRefinement) {
      refinementRoot = (ARGState) reached.asReachedSet().getFirstState();

    } else if (refinementRoot == null) {
        refinementRoot = changedElements.get(0);

        // search parent of both refinement roots and use this as the new
        // refinement root
    } else {
      PathIterator firstPath = ARGUtils.getOnePathTo(refinementRoot).pathIterator();
      PathIterator secondPath = ARGUtils.getOnePathTo(changedElements.get(0)).pathIterator();

      // TODO should they be equal or identical?
      while (firstPath.getAbstractState().equals(secondPath.getAbstractState())) {
        refinementRoot = firstPath.getAbstractState();

        if (firstPath.hasNext() && secondPath.hasNext()) {
          firstPath.advance();
          secondPath.advance();
        } else {
          break;
        }
      }
    }
  }

  @Override
  public Statistics getStatistics() {
    // TODO Auto-generated method stub
    return null;
  }
}
