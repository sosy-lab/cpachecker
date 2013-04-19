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
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

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
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * Refinement strategy similar based on the general idea of McMillan's Impact
 * algorithm: do not create a precision and remove parts from ARG,
 * but instead directly strengthen the states in the ARG using interpolants.
 * For the details on how to use the interpolants, there are three different
 * strategies that are explained in the description for
 * {@link #performRefinementForState(BooleanFormula, ARGState)}.
 *
 * This class can be used both with and without BDDs.
 */
@Options(prefix="cpa.predicate.refinement")
class ImpactRefinementStrategy extends RefinementStrategy {

  @Option(description="split each arithmetic equality into two inequalities when extracting predicates from interpolants")
  private boolean splitItpAtoms = false;

  @Option(description="If an abstraction is computed during refinement, "
      + "use only the interpolant as input, not the concrete block.")
  private boolean abstractInterpolantOnly = false;

  @Option(description="Actually compute an abstraction, "
      + "otherwise just convert the interpolants to BDDs as they are.")
  private boolean doAbstractionComputation = false;

  private class Stats implements Statistics {

    private final Timer abstraction = new Timer();
    private final Timer itpCheck  = new Timer();
    private final Timer coverTime = new Timer();
    private final Timer argUpdate = new Timer();

    @Override
    public String getName() {
      return "Impact Refiner";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("  Computing abstraction of itp:       " + abstraction);
      out.println("  Checking whether itp is new:        " + itpCheck);
      out.println("  Coverage checks:                    " + coverTime);
      out.println("  ARG update:                         " + argUpdate);
      out.println();
      out.println("Number of abstractions during refinements:  " + abstraction.getNumberOfIntervals());
      ImpactRefinementStrategy.this.printStatistics(out);
    }
  }

  private final Stats stats = new Stats();

  private final AbstractionManager amgr;
  private final FormulaManagerView fmgr;
  private final PredicateAbstractionManager predAbsMgr;

  // During the refinement of a single path,
  // a reference to the abstraction of the last state we have seen
  // (we sometimes needs this to refer to the previous block).
  private AbstractionFormula lastAbstraction = null;

  protected ImpactRefinementStrategy(final Configuration config, final LogManager logger,
      final FormulaManagerView pFmgr,
      final Solver pSolver,
      final PredicateAbstractionManager pPredAbsMgr,
      final AbstractionManager pAmgr)
          throws InvalidConfigurationException, CPAException {
    super(pFmgr.getBooleanFormulaManager(), pSolver);
    config.inject(this, ImpactRefinementStrategy.class);

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
   * conjunctively adding the interpolant to its state formula.
   *
   * There are several strategies for handling the interpolant:
   * 1) Do nothing, just convert it into a Region
   * 2) Compute an abstraction of the interpolant.
   * 3) Compute an abstraction of the preceding block using the predicates
   *    from the interpolant just like predicate abstraction would do.
   *
   * All strategies can in principle be applied both with and without using BDDs,
   * although some combinations will not make sense (especially 2) and 3)
   * should be used only with BDDs).
   * Strategy 1 without BDDs is exactly what Impact does and is very cheap.
   * Strategy 3 with BDDs is similar to predicate abstraction,
   * but the abstractions are computed during refinement instead of during the
   * forward analysis.
   *
   * Note that the decision whether to use BDDs or not is not in the scope
   * of this class.
   */
  @Override
  protected boolean performRefinementForState(BooleanFormula itp,
      ARGState s) {
    checkArgument(!fmgr.getBooleanFormulaManager().isTrue(itp));
    checkArgument(!fmgr.getBooleanFormulaManager().isFalse(itp));
    checkState(lastAbstraction != null);

    // Extract predicates from interpolants.
    Collection<BooleanFormula> atoms = fmgr.extractAtoms(itp, splitItpAtoms, false);
    List<AbstractionPredicate> preds = new ArrayList<>(atoms.size());
    for (BooleanFormula atom : atoms) {
      preds.add(amgr.makePredicate(atom));
    }

    PredicateAbstractState predicateState = extractStateByType(s, PredicateAbstractState.class);

    // lastAbstraction is the abstraction that was computed at the end
    // of the previous block in the last call to this method.

    // existingAbstraction is the abstraction from the current abstract state
    // that was computed before.
    final AbstractionFormula existingAbstraction = predicateState.getAbstractionFormula();

    // blockFormula is the concrete formula representing the current block.
    PathFormula blockFormula = existingAbstraction.getBlockFormula();

    // Compute an abstraction with the new predicates.
    stats.abstraction.start();
    AbstractionFormula newAbstraction;
    if (!doAbstractionComputation) {
      // Only create a BDD from itp without abstraction computation.
      newAbstraction = predAbsMgr.buildAbstraction(fmgr.uninstantiate(itp), blockFormula);

    } else if (abstractInterpolantOnly) {
      // Compute an abstraction of "itp"
      newAbstraction = predAbsMgr.buildAbstraction(itp, blockFormula, preds);

    } else {
      // Compute an abstraction of "lastAbstraction & blockFormula"
      newAbstraction = predAbsMgr.buildAbstraction(lastAbstraction, blockFormula, preds);
    }
    stats.abstraction.stop();

    stats.itpCheck.start();
    boolean isNewItp = !predAbsMgr.checkCoverage(existingAbstraction, newAbstraction);
    stats.itpCheck.stop();

    if (isNewItp) {
      // newAbs is not entailed by oldAbs,
      // we need to strengthen the element
      newAbstraction = predAbsMgr.makeAnd(existingAbstraction, newAbstraction);
      predicateState.setAbstraction(newAbstraction);
      lastAbstraction = newAbstraction;

    } else {
      // prepare for next call
      lastAbstraction = existingAbstraction;
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
