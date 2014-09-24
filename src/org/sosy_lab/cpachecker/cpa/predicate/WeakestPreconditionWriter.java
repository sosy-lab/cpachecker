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
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.getPredicateState;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;


/**
 * Write all path formulas that reach a specified location
 * (here we consider one path formula as a conjunction of block formulas)
 *
 * The formulas get stored in the SMTLib2 format.
 *
 * The parameter analysis.stopAfterError should be "false" in order
 * to not exclude certain paths of programs that violate one or more properties.
 */
@SuppressWarnings("unused")
@Options
public class WeakestPreconditionWriter {

  private final CFA cfa;
  private final LogManager logger;
  private final AbstractionManager absmgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final RegionManager rmgr;
  private final PathFormulaManager pmgr;

  public WeakestPreconditionWriter(CFA pCfa, Configuration pConfig, LogManager pLogger, ShutdownNotifier pNotifier,
      Solver pSolver, FormulaManagerFactory pFormulaManagerFactory, AbstractionManager pAbsmgr, PathFormulaManager pPathFormulaManager,
      FormulaManagerView pFormulaManager, RegionManager pRmgr)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    cfa = pCfa;
    logger = pLogger;
    absmgr = pAbsmgr;
    fmgr = pFormulaManager;
    bfmgr = pFormulaManager.getBooleanFormulaManager();
    rmgr = pRmgr;
    pmgr = pPathFormulaManager;

    InterpolationManager manager = new InterpolationManager(
        fmgr,
        pmgr,
        pSolver,
        pFormulaManagerFactory,
        pConfig,
        pNotifier,
        logger);

  }

  private BooleanFormula getFormulaBetween(CFANode pStart, CFANode pDestination, ReachedSet pReached) {

    //  PredicateAbstractState state = getPredicateState(e);

    return null;
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

  private static final Function<PredicateAbstractState, BooleanFormula> GET_BLOCK_FORMULA
  = new Function<PredicateAbstractState, BooleanFormula>() {
      @Override
      public BooleanFormula apply(PredicateAbstractState e) {
        assert e.isAbstractionState();
        return e.getAbstractionFormula().getBlockFormula().getFormula();
      }
    };


  protected List<BooleanFormula> getFormulasForPath(List<ARGState> path, ARGState initialState)
      throws CPATransferException, InterruptedException {

      return from(path)
          .transform(toState(PredicateAbstractState.class))
          .transform(GET_BLOCK_FORMULA)
          .toList();
  }

  public void errorTracesToWeakestPrecondition(@Nonnull Appendable pWriteTo, @Nonnull ReachedSet pReached) throws IOException, CPATransferException, InterruptedException {
    Preconditions.checkNotNull(pWriteTo);
    Preconditions.checkNotNull(pReached);

    // Extract the formulas of the states
    // and compute the disjunction of all paths that reach the entry location
    BooleanFormula pathsToError = bfmgr.makeBoolean(false);

    FluentIterable<AbstractState> targetStates = from(pReached).filter(AbstractStates.IS_TARGET_STATE);
    for (AbstractState s: targetStates) {
      final ARGState target = (ARGState) s;
      assert target.isTarget() : "Last element in reached is not a target state before refinement";

      final @Nullable ARGPath path = ARGUtils.getOnePathTo(target);

      // create path with all abstraction location elements (excluding the initial element)
      // the last element is the element corresponding to the error location
      final List<ARGState> abstractionStatesTrace = transformPath(path);
      List<BooleanFormula> formulas = getFormulasForPath(abstractionStatesTrace, path.getFirstState());

      pathsToError = bfmgr.or(pathsToError, bfmgr.and(formulas));
    }


    // Convert the formula to a weakest precondition
    // - Negation of the abstract paths to the error location
    Formula wp = fmgr.simplify(bfmgr.not(pathsToError));

    // - TODO: Reduce to predicates on either global variables or function parameters

    // Write the formula in the SMT-LIB2 format to the target stream
    pWriteTo.append(wp.toString());
  }

  public void writeWeakestPreconditionFromAbstractions(@Nonnull Appendable pWriteTo, @Nonnull ReachedSet pReached) throws IOException, CPATransferException, InterruptedException {
    Preconditions.checkNotNull(pWriteTo);
    Preconditions.checkNotNull(pReached);

    // Extract the formulas of the states
    // and compute the disjunction of all paths that reach the entry location
    BooleanFormula wp = bfmgr.makeBoolean(false);

    for (AbstractState state : pReached) {
      PredicateAbstractState predicateState = getPredicateState(state);
      if (!predicateState.isAbstractionState()) {
        continue;
      }

      BooleanFormula abs = predicateState.getAbstractionFormula().asFormula();
      if (!bfmgr.isTrue(abs)) {
        wp = bfmgr.or(wp, abs);
      }
    }

    // Convert the formula to a weakest precondition
    // - Negation of the abstract paths to the error location
    wp = fmgr.simplify( wp);

    // - TODO: Reduce to predicates on either global variables or function parameters

    // Write the formula in the SMT-LIB2 format to the target stream
    fmgr.dumpFormula(wp).appendTo(pWriteTo);
  }

  public void writeWeakestPrecondition(Path pWriteTo, ReachedSet pReached) throws IOException, CPATransferException, InterruptedException {

    try (Writer w = Files.openOutputFile(pWriteTo)) {
      errorTracesToWeakestPrecondition(w, pReached);
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
