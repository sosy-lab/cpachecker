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

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;

import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

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

  @Option(secure=true, description="If an abstraction is computed during refinement, "
      + "use only the interpolant as input, not the concrete block.")
  private boolean abstractInterpolantOnly = false;

  @Option(secure=true, description="Actually compute an abstraction, "
      + "otherwise just convert the interpolants to BDDs as they are.")
  private boolean doAbstractionComputation = false;

  final Timer abstractionTime = new Timer();
  final Timer itpCheckTime  = new Timer();

  private final FormulaManagerView fmgr;
  private final PredicateAbstractionManager predAbsMgr;

  ImpactUtility(Configuration config,
      FormulaManagerView pFmgr, PredicateAbstractionManager pPredAbsMgr)
          throws InvalidConfigurationException {
    config.inject(this);

    if (!doAbstractionComputation && abstractInterpolantOnly) {
      throw new InvalidConfigurationException(
          "Setting cpa.predicate.refinement.abstractInterpolantOnly=true " +
          "is not possible without cpa.predicate.refinement.doAbstractionComputation=true.");
    }

    fmgr = pFmgr;
    predAbsMgr = pPredAbsMgr;
  }

  boolean requiresPreviousBlockAbstraction() {
    // If we compute an abstraction of the current block,
    // we do need the abstraction from the start of the block.
    return doAbstractionComputation && !abstractInterpolantOnly;
  }

  /**
   * Strengthen a state given a (non-trivial) interpolant
   * by conjunctively adding the interpolant to the state's state formula.
   *
   * @param itp The interpolant.
   * @param s The state.
   * @param lastAbstraction The abstraction that was computed at the beginning
   *         of the current block (so it is not the abstraction of s,
   *         but the abstraction of the last predecessor of s that
   *         is an abstraction state).
   *         This may be null if {@link #requiresPreviousBlockAbstraction()} returns false.
   * @return True if the state was actually changed.
   */
  boolean strengthenStateWithInterpolant(final BooleanFormula itp,
      final ARGState s, final AbstractionFormula lastAbstraction)
          throws SolverException, InterruptedException {
    checkState(!requiresPreviousBlockAbstraction()
        || lastAbstraction != null);

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

    CFANode location = AbstractStates.extractLocation(s);
    Optional<CallstackStateEqualsWrapper>
        callstackInfo = AbstractStates.extractOptionalCallstackWraper(s);

    // Compute an abstraction with the new predicates.
    abstractionTime.start();
    AbstractionFormula newAbstraction;
    if (!doAbstractionComputation) {
      // Only create a region from itp without abstraction computation.
      newAbstraction = predAbsMgr.asAbstraction(fmgr.uninstantiate(itp), blockFormula);

    } else if (abstractInterpolantOnly) {
      // Compute an abstraction of "itp" using the predicates from "itp".
      Collection<AbstractionPredicate> preds = predAbsMgr.getPredicatesForAtomsOf(itp);
      newAbstraction = predAbsMgr.buildAbstraction(
          location, callstackInfo, itp, blockFormula, preds);

    } else {
      // Compute an abstraction of "lastAbstraction & blockFormula" using the predicates from "itp".
      Collection<AbstractionPredicate> preds = predAbsMgr.getPredicatesForAtomsOf(itp);
      newAbstraction = predAbsMgr.buildAbstraction(
          location, callstackInfo, lastAbstraction, blockFormula, preds);
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
