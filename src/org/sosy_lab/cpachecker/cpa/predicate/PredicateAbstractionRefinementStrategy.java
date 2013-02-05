/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import static com.google.common.base.Preconditions.*;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;

/**
 * This class provides the refinement strategy for the classical predicate
 * abstraction (adding the predicates from the interpolant to the precision
 * and removing the relevant parts of the ARG).
 */
@Options(prefix="cpa.predicate.refinement")
public class PredicateAbstractionRefinementStrategy extends RefinementStrategy {

  @Option(description="use only the atoms from the interpolants as predicates, "
    + "and not the whole interpolant")
  private boolean atomicPredicates = true;

  @Option(description="split each arithmetic equality into two inequalities when extracting predicates from interpolants")
  private boolean splitItpAtoms = false;

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

  protected final LogManager logger;
  private final AbstractionManager amgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;

  private class Stats implements Statistics {
    @Override
    public String getName() {
      return "Predicate Abstraction Refiner";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("  Predicate creation:                 " + predicateCreation);
      out.println("  Precision update:                   " + precisionUpdate);
      out.println("  ARG update:                         " + argUpdate);
      out.println();
      PredicateAbstractionRefinementStrategy.this.printStatistics(out);
      out.println("Number of refs with location-based cutoff:  " + numberOfRefinementsWithStrategy2);
    }
  }

  // statistics
  private int numberOfRefinementsWithStrategy2 = 0;

  private final Timer predicateCreation = new Timer();
  private final Timer precisionUpdate = new Timer();
  private final Timer argUpdate = new Timer();

  protected PredicateAbstractionRefinementStrategy(final Configuration config,
      final LogManager pLogger, final FormulaManagerView pFormulaManager,
      final AbstractionManager pAbstractionManager) throws CPAException, InvalidConfigurationException {
    super(pFormulaManager.getBooleanFormulaManager());

    config.inject(this, PredicateAbstractionRefinementStrategy.class);

    logger = pLogger;
    amgr = pAbstractionManager;
    fmgr = pFormulaManager;
    bfmgr = pFormulaManager.getBooleanFormulaManager();
  }

  private ListMultimap<CFANode, AbstractionPredicate> newPredicates;

  @Override
  public void startRefinementOfPath() {
    checkState(newPredicates == null);
    newPredicates = ArrayListMultimap.create();
  }

  @Override
  public boolean performRefinementForState(BooleanFormula pInterpolant, ARGState interpolationPoint) {
    checkState(newPredicates != null);
    checkArgument(!bfmgr.isTrue(pInterpolant));

    predicateCreation.start();
    Collection<AbstractionPredicate> localPreds = convertInterpolant(pInterpolant);
    CFANode loc = AbstractStates.extractLocation(interpolationPoint);

    newPredicates.putAll(loc, localPreds);
    predicateCreation.stop();

    return false;
  }

  /**
   * Get the predicates out of an interpolant.
   * @param interpolant The interpolant formula.
   * @return A set of predicates.
   */
  protected final Collection<AbstractionPredicate> convertInterpolant(BooleanFormula interpolant) {
    if (bfmgr.isTrue(interpolant)) {
      return Collections.<AbstractionPredicate>emptySet();
    }

    Collection<AbstractionPredicate> preds;

    if (bfmgr.isFalse(interpolant)) {
      preds = ImmutableSet.of(amgr.makeFalsePredicate());
    } else {
      preds = getAtomsAsPredicates(interpolant);
    }
    assert !preds.isEmpty();

    logger.log(Level.FINEST, "Got predicates", preds);

    return preds;
  }

  /**
   * Create predicates for all atoms in a formula.
   */
  private List<AbstractionPredicate> getAtomsAsPredicates(BooleanFormula f) {
    Collection<BooleanFormula> atoms;
    if (atomicPredicates) {
      atoms = fmgr.extractAtoms(f, splitItpAtoms, false);
    } else {
      atoms = Collections.singleton(fmgr.uninstantiate(f));
    }

    List<AbstractionPredicate> preds = new ArrayList<>(atoms.size());

    for (BooleanFormula atom : atoms) {
      preds.add(amgr.makePredicate(atom));
    }
    return preds;
  }

  @Override
  public void finishRefinementOfPath(ARGState pUnreachableState,
      List<ARGState> pAffectedStates, ARGReachedSet pReached,
      boolean pRepeatedCounterexample)
      throws CPAException {

    if (newPredicates.isEmpty() && pUnreachableState.isTarget()) {
      // The only reason why this might appear is that the very last block is
      // infeasible in itself, however, we check for such cases during strengthen,
      // so they shouldn't appear here.
      throw new RefinementFailedException(RefinementFailedException.Reason.InterpolationFailed, null);
    }

    newPredicates.put(extractLocation(pUnreachableState), amgr.makeFalsePredicate());
    pAffectedStates.add(pUnreachableState);

    // We have two different strategies for the refinement root: set it to
    // the first interpolation point or set it to highest location in the ARG
    // where the same CFANode appears.
    // Both work, so this is a heuristics question to get the best performance.
    // My benchmark showed, that at least for the benchmarks-lbe examples it is
    // best to use strategy one iff newPredicatesFound.

    // get previous precision
    UnmodifiableReachedSet reached = pReached.asReachedSet();
    PredicatePrecision targetStatePrecision = extractPredicatePrecision(reached.getPrecision(reached.getLastState()));

    ARGState refinementRoot = getRefinementRoot(pAffectedStates, targetStatePrecision, pRepeatedCounterexample);

    logger.log(Level.FINEST, "Removing everything below", refinementRoot, "from ARG.");

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
    precisionUpdate.start();
    PredicatePrecision basePrecision;
    if (keepAllPredicates) {
      basePrecision = findAllPredicatesFromSubgraph(refinementRoot, reached);
    } else {
      basePrecision = targetStatePrecision;
    }

    logger.log(Level.ALL, "Old predicate map is", basePrecision);
    logger.log(Level.ALL, "New predicates are", newPredicates);

    PredicatePrecision newPrecision;
    if (addPredicatesGlobally) {
      newPrecision = basePrecision.addGlobalPredicates(newPredicates.values());
    } else {
      newPrecision = basePrecision.addLocalPredicates(newPredicates);
    }

    logger.log(Level.ALL, "Predicate map now is", newPrecision);

    assert basePrecision.calculateDifferenceTo(newPrecision) == 0 : "We forgot predicates during refinement!";
    assert targetStatePrecision.calculateDifferenceTo(newPrecision) == 0 : "We forgot predicates during refinement!";

    precisionUpdate.stop();


    argUpdate.start();

    pReached.removeSubtree(refinementRoot, newPrecision);

    assert (refinementCount > 0) || reached.size() == 1;

    if (sharePredicates) {
      pReached.updatePrecisionGlobally(newPrecision);
    }

    argUpdate.stop();

    newPredicates = null;
  }

  protected final PredicatePrecision extractPredicatePrecision(Precision oldPrecision) throws IllegalStateException {
    PredicatePrecision oldPredicatePrecision = Precisions.extractPrecisionByType(oldPrecision, PredicatePrecision.class);
    if (oldPredicatePrecision == null) {
      throw new IllegalStateException("Could not find the PredicatePrecision for the error element");
    }
    return oldPredicatePrecision;
  }

  private ARGState getRefinementRoot(List<ARGState> pAffectedStates, PredicatePrecision targetStatePrecision,
      boolean pRepeatedCounterexample) throws RefinementFailedException {
    boolean newPredicatesFound = !targetStatePrecision.getLocalPredicates().entries().containsAll(newPredicates.entries());

    ARGState firstInterpolationPoint = pAffectedStates.get(0);
    if (!newPredicatesFound) {
      if (pRepeatedCounterexample) {
        throw new RefinementFailedException(RefinementFailedException.Reason.RepeatedCounterexample, null);
      }
      numberOfRefinementsWithStrategy2++;

      CFANode firstInterpolationPointLocation = AbstractStates.extractLocation(firstInterpolationPoint);

      logger.log(Level.FINEST, "Found spurious counterexample,",
          "trying strategy 2: remove everything below node", firstInterpolationPointLocation, "from ARG.");

      // find top-most element in path with location == firstInterpolationPointLocation,
      // this is not necessary equal to firstInterpolationPoint
      ARGState current = firstInterpolationPoint;
      while (!current.getParents().isEmpty()) {
        current = Iterables.get(current.getParents(), 0);

        if (extractStateByType(current, PredicateAbstractState.class).isAbstractionState()) {
          CFANode loc = AbstractStates.extractLocation(current);
          if (loc.equals(firstInterpolationPointLocation)) {
            firstInterpolationPoint = current;
          }
        }
      }
    }
    return firstInterpolationPoint;
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
  public Statistics getStatistics() {
    return new Stats();
  }
}
