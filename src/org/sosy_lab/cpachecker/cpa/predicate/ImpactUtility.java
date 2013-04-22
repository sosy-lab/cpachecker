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

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

/**
 * This class implements the core of all the approaches that are similar
 * to McMillan's Lazy Abstraction with Interpolants ("Impact") approach:
 * the updating of an ARG state given a suitable interpolant for that state.
 * It does not do any more than that, i.e.,
 * it does NOT update the ARG covering relation ship,
 * it does NOT re-add states to the waitlist etc.
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
 * of this class (it just uses the given {@link RegionManager}).
 */
@Options(prefix="cpa.predicate.refinement")
final class ImpactUtility {

  @Option(description="split each arithmetic equality into two inequalities when extracting predicates from interpolants")
  private boolean splitItpAtoms = false;

  @Option(description="If an abstraction is computed during refinement, "
      + "use only the interpolant as input, not the concrete block.")
  private boolean abstractInterpolantOnly = false;

  @Option(description="Actually compute an abstraction, "
      + "otherwise just convert the interpolants to BDDs as they are.")
  private boolean doAbstractionComputation = false;

  final Timer abstractionTime = new Timer();
  final Timer itpCheckTime  = new Timer();

  private final AbstractionManager amgr;
  private final FormulaManagerView fmgr;
  private final PredicateAbstractionManager predAbsMgr;

  ImpactUtility(Configuration config, AbstractionManager pAmgr,
      FormulaManagerView pFmgr, PredicateAbstractionManager pPredAbsMgr)
          throws InvalidConfigurationException {
    config.inject(this);

    if (!doAbstractionComputation && abstractInterpolantOnly) {
      throw new InvalidConfigurationException(
          "Setting cpa.predicate.refinement.abstractInterpolantOnly=true " +
          "is not possible without cpa.predicate.refinement.doAbstractionComputation=true.");
    }

    amgr = pAmgr;
    fmgr = pFmgr;
    predAbsMgr = pPredAbsMgr;
  }

  /**
   * Strengthen a state given a (non-trivial) interpolant
   * by conjunctively adding the interpolant to the state's state formula.
   *
   * @param interpolant The interpolant.
   * @param state The state.
   * @return True if the state was actually changed.
   */
  boolean strengthenStateWithInterpolant(final BooleanFormula itp,
      final ARGState s, final AbstractionFormula lastAbstraction) {
    checkState(lastAbstraction != null);

    if (fmgr.getBooleanFormulaManager().isTrue(itp)) {
      return false;
    }

    final PredicateAbstractState predicateState = getPredicateState(s);

    // lastAbstraction is the abstraction that was computed at the end
    // of the previous block in the last call to this method.

    // existingAbstraction is the abstraction from the current abstract state
    // that was computed before.
    final AbstractionFormula existingAbstraction = predicateState.getAbstractionFormula();

    // blockFormula is the concrete formula representing the current block.
    final PathFormula blockFormula = existingAbstraction.getBlockFormula();

    if (itp.equals(existingAbstraction.asInstantiatedFormula())) {
      return false;
    }

    // Compute an abstraction with the new predicates.
    abstractionTime.start();
    AbstractionFormula newAbstraction;
    if (!doAbstractionComputation) {
      // Only create a region from itp without abstraction computation.
      newAbstraction = predAbsMgr.buildAbstraction(fmgr.uninstantiate(itp), blockFormula);

    } else {
      // Extract predicates from interpolants.
      Collection<BooleanFormula> atoms = fmgr.extractAtoms(itp, splitItpAtoms, false);
      List<AbstractionPredicate> preds = new ArrayList<>(atoms.size());
      for (BooleanFormula atom : atoms) {
        preds.add(amgr.makePredicate(atom));
      }
      if (fmgr.getBooleanFormulaManager().isFalse(itp)) {
        preds.add(amgr.makeFalsePredicate());
      }

      if (abstractInterpolantOnly) {
        // Compute an abstraction of "itp"
        newAbstraction = predAbsMgr.buildAbstraction(itp, blockFormula, preds);

      } else {
        // Compute an abstraction of "lastAbstraction & blockFormula"
        newAbstraction = predAbsMgr.buildAbstraction(lastAbstraction, blockFormula, preds);
      }
    }
    abstractionTime.stop();

    itpCheckTime.start();
    boolean isNewItp = !predAbsMgr.checkCoverage(existingAbstraction, newAbstraction);
    itpCheckTime.stop();

    if (isNewItp) {
      // newAbs is not entailed by oldAbs,
      // we need to strengthen the element
      newAbstraction = predAbsMgr.makeAnd(existingAbstraction, newAbstraction);
      predicateState.setAbstraction(newAbstraction);
    }
    return isNewItp;
  }
}
