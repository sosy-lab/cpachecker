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
package org.sosy_lab.cpachecker.core.algorithm.precondition;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import java.util.List;

import javax.annotation.Nonnull;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateVariableElimination;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

public final class PreconditionHelper {

  private final FormulaManagerView mgrv;
  private final BooleanFormulaManagerView bmgr;

  public PreconditionHelper(FormulaManagerView pMgrv) {
    this.mgrv = pMgrv;
    this.bmgr = pMgrv.getBooleanFormulaManager();
  }

  static final Function<PredicateAbstractState, PathFormula> GET_BLOCK_FORMULA
    = new Function<PredicateAbstractState, PathFormula>() {
      @Override
      public PathFormula apply(PredicateAbstractState e) {
        assert e.isAbstractionState();
        return e.getAbstractionFormula().getBlockFormula();
      }
    };

  static List<ARGState> transformPath(ARGPath pPath) {
    List<ARGState> result = from(pPath.asStatesList())
      .skip(1)
      .filter(Predicates.compose(PredicateAbstractState.FILTER_ABSTRACTION_STATES,
                                 toState(PredicateAbstractState.class)))
      .toList();

    assert from(result).allMatch(new Predicate<ARGState>() {
      @Override
      public boolean apply(ARGState pInput) {
        boolean correct = pInput.getParents().size() <= 1;
        assert correct : "PreconditionWriter expects abstraction states to have only one parent, but this state has more:" + pInput;
        return correct;
      }
    });

    assert pPath.getLastState() == result.get(result.size()-1);
    return result;
  }

  public BooleanFormula uninstanciatePathFormula(PathFormula pPf)
      throws SolverException, InterruptedException {

    return mgrv.uninstantiate(
        PredicateVariableElimination.eliminateDeadVariables(mgrv, pPf.getFormula(), pPf.getSsa()));
  }

  public BooleanFormula getPathPrecond(ARGPath pPath) {
    ImmutableList<PathFormula> r = from(pPath.asStatesList())
        .transform(toState(PredicateAbstractState.class))
        .transform(GET_BLOCK_FORMULA)
        .toList();
    return null;
  }

  public List<BooleanFormula> getPrecondsAlongPath(ARGPath pPath) {
    ImmutableList<PathFormula> r = from(pPath.asStatesList())
        .transform(toState(PredicateAbstractState.class))
        .transform(GET_BLOCK_FORMULA)
        .toList();
    return null;
  }


  public BooleanFormula getPreconditionOfPath(@Nonnull ARGPath pPath) {
    return null;
  }

  public BooleanFormula getPathStatePrecondition(ARGPath pPath, ARGState pStateInPath) {
    return null;
  }

  public BooleanFormula getPreconditionFromReached(@Nonnull ReachedSet pReached, PreconditionPartition pPartition) {
    Preconditions.checkNotNull(pReached);

    BooleanFormula conjunctiveWp = bmgr.makeBoolean(true);

    // Also for backwards analysis can exist multiple target states (for the same CFA location)
    FluentIterable<AbstractState> targetStates = from(pReached).filter(AbstractStates.IS_TARGET_STATE);
    for (AbstractState s: targetStates) {
      final ARGState target = (ARGState) s;
      final ARGPath pathToTarget = ARGUtils.getOnePathTo(target);
      assert pathToTarget != null : "The abstract target-state must be on an abstract path!";

      // create path with all abstraction location elements (excluding the initial element)
      // the last element is the element corresponding to the target location (which is the entry location of a backwards analysis)
      final List<ARGState> abstractionStatesTrace = transformPath(pathToTarget);
      assert abstractionStatesTrace.size() > 1;
      PredicateAbstractState stateWithAbstraction = AbstractStates.extractStateByType(
          abstractionStatesTrace.get(abstractionStatesTrace.size()-2),
          PredicateAbstractState.class);

      // The last abstraction state before the target location contains the negation of the WP
      conjunctiveWp = mgrv.makeAnd(conjunctiveWp, mgrv.makeNot(mgrv.uninstantiate(stateWithAbstraction.getAbstractionFormula().asFormula())));
    }

    // The WP is the negation of targetAbstraction
    BooleanFormula wp = mgrv.simplify(conjunctiveWp);

    return wp;
  }

}
