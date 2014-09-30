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

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;

@Options
public class WeakestPreconditionWriter {

  private final FormulaManagerView fmgr;

  public WeakestPreconditionWriter(CFA pCfa, Configuration pConfig, LogManager pLogger, FormulaManagerView pFormulaManager)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    fmgr = pFormulaManager;
  }

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
        assert correct : "PredicateCPARefiner expects abstraction states to have only one parent, but this state has more:" + pInput;
        return correct;
      }
    });

    assert pPath.getLastState() == result.get(result.size()-1);
    return result;
  }

  public void writeWeakestPreconditionFromAbstractions(@Nonnull Appendable pWriteTo, @Nonnull ReachedSet pReached) throws IOException, CPATransferException, InterruptedException {
    Preconditions.checkNotNull(pWriteTo);
    Preconditions.checkNotNull(pReached);

    BooleanFormula targetAbstraction = fmgr.getBooleanFormulaManager().makeBoolean(true);

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
      targetAbstraction = fmgr.makeAnd(targetAbstraction, fmgr.uninstantiate(stateWithAbstraction.getAbstractionFormula().asFormula()));
    }

    // The WP is the negation of targetAbstraction
    BooleanFormula wp = fmgr.simplify(fmgr.makeNot(targetAbstraction));

    // Write the formula in the SMT-LIB2 format to the target stream
    pWriteTo.append(wp.toString());
  }


  public void writeWeakestPrecondition(Path pWriteTo, ReachedSet pReached) throws IOException, CPATransferException, InterruptedException {

    try (Writer w = Files.openOutputFile(pWriteTo)) {
      writeWeakestPreconditionFromAbstractions(w, pReached);
    }
  }

  public void writeWeakestPrecondition(Path pWriteTo, ReachedSet pReached, @Nonnull LogManager pCatchExceptionsTo) {
    Preconditions.checkNotNull(pCatchExceptionsTo);

    try {
      writeWeakestPrecondition(pWriteTo, pReached);

    } catch (Exception e) {
      pCatchExceptionsTo.logException(Level.WARNING, e, "Writing reaching paths failed!");
    }
  }

}
