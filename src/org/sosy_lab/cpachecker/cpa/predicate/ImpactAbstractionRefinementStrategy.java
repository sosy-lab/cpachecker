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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * Refinement strategy similar to {@link ImpactRefinementStrategy},
 * however it computes an abstraction before strengthening states with interpolants.
 * This enables the use of BDDs similar to predicate abstraction.
 */
@Options(prefix="cpa.predicate.refinement")
class ImpactAbstractionRefinementStrategy extends RefinementStrategy {

  @Option(description="split each arithmetic equality into two inequalities when extracting predicates from interpolants")
  private boolean splitItpAtoms = false;

  private class Stats implements Statistics {

    private final Timer abstraction = new Timer();
    private final Timer itpCheck  = new Timer();
    private final Timer coverTime = new Timer();
    private final Timer argUpdate = new Timer();

    @Override
    public String getName() {
      return "Impact+Abstraction Refiner";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("  Computing abstraction of itp:       " + abstraction);
      out.println("  Checking whether itp is new:        " + itpCheck);
      out.println("  Coverage checks:                    " + coverTime);
      out.println("  ARG update:                         " + argUpdate);
      out.println();
      ImpactAbstractionRefinementStrategy.this.printStatistics(out);
    }
  }

  private final Stats stats = new Stats();

  private final AbstractionManager amgr;
  private final FormulaManagerView fmgr;
  private final PredicateAbstractionManager predAbsMgr;

  private AbstractionFormula lastAbstraction = null;

  protected ImpactAbstractionRefinementStrategy(final Configuration config, final LogManager logger,
      final FormulaManagerView pFmgr, final AbstractionManager pAmgr,
      final PredicateAbstractionManager pPredAbsMgr) throws InvalidConfigurationException, CPAException {
    super(pFmgr.getBooleanFormulaManager());

    amgr = pAmgr;
    fmgr = pFmgr;
    predAbsMgr = pPredAbsMgr;
  }

  @Override
  protected void startRefinementOfPath() {
    checkState(lastAbstraction == null);
    lastAbstraction = predAbsMgr.makeTrueAbstractionFormula(null);
  }

  /**
   * For each interpolant, we strengthen the corresponding state by
   * computing an abstraction of the interpolant
   * conjunctively adding the interpolant to its state formula.
   */
  @Override
  protected boolean performRefinementForState(BooleanFormula itp, ARGState s) {
    checkArgument(!fmgr.getBooleanFormulaManager().isTrue(itp));
    checkArgument(!fmgr.getBooleanFormulaManager().isFalse(itp));
    checkState(lastAbstraction != null);

    // Extract predicates from interpolants.
    Collection<BooleanFormula> atoms = fmgr.extractAtoms(itp, splitItpAtoms, false);
    List<AbstractionPredicate> preds = new ArrayList<>(atoms.size());
    for (BooleanFormula atom : atoms) {
      preds.add(amgr.makePredicate(atom));
    }

    PredicateAbstractState predicateState = AbstractStates.extractStateByType(s, PredicateAbstractState.class);
    AbstractionFormula oldAbs = predicateState.getAbstractionFormula();

    // lastAbstraction is the one from the previous block.
    // oldAbs is the abstraction from the current location that was computed before.

    // Compute an abstraction with the new predicates.
    stats.abstraction.start();
    AbstractionFormula newAbs = predAbsMgr.buildAbstraction(lastAbstraction, oldAbs.getBlockFormula(), preds);
    stats.abstraction.stop();

    stats.itpCheck.start();
    boolean isNewItp = !predAbsMgr.checkCoverage(oldAbs, newAbs);
    stats.itpCheck.stop();

    if (isNewItp) {
      // newAbs is not entailed by oldAbs,
      // we need to strengthen the element
      AbstractionFormula result = predAbsMgr.makeAnd(oldAbs, newAbs);
      predicateState.setAbstraction(result);
      lastAbstraction = result;

    } else {
      // prepare for next call
      lastAbstraction = oldAbs;
    }
    return !isNewItp;
  }

  /**
   * After a path was strengthened, we need to take care of the coverage relation.
   * We also remove the infeasible part from the ARG,
   * and re-establish the coverage invariant (i.e., that states on the path
   * are either covered or cannot be covered).
   */
  @Override
  protected void finishRefinementOfPath(ARGState infeasiblePartOfART,
      List<ARGState> changedElements, ARGReachedSet pReached,
      boolean pRepeatedCounterexample)
      throws CPAException {
    checkState(lastAbstraction != null);
    lastAbstraction = null;

    stats.argUpdate.start();
    for (ARGState w : changedElements) {
      pReached.removeCoverageOf(w);
    }

    pReached.removeInfeasiblePartofARG(infeasiblePartOfART);
    stats.argUpdate.stop();

    // optimization: instead of closing all ancestors of v,
    // close only those that were strengthened during refine
    stats.coverTime.start();
    try {
      for (ARGState w : changedElements) {
        if (pReached.tryToCover(w)) {
          break; // all further elements are covered anyway
        }
      }
    } finally {
      stats.coverTime.stop();
    }
  }

  @Override
  public Statistics getStatistics() {
    return stats;
  }
}
