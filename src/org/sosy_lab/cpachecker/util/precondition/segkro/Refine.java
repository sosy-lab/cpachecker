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
package org.sosy_lab.cpachecker.util.precondition.segkro;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.precondition.PreconditionHelper;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.InterpolationWithCandidates;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.PreconditionRefiner;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class Refine implements PreconditionRefiner {

  private final ExtractNewPreds enp;
  private final InterpolationWithCandidates ipc;
  private final FormulaManagerView mgrv;
  private final BooleanFormulaManagerView bmgr;
  private final PathFormulaManager pmgrFwd;
  private final PreconditionHelper helper;
  private final AbstractionManager amgr;

  public Refine(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa,
      Solver pSolver, AbstractionManager pAmgr, ExtractNewPreds pExtractNewPreds, InterpolationWithCandidates pMinCorePrio)
          throws InvalidConfigurationException {

    amgr = pAmgr;
    enp = pExtractNewPreds;
    ipc = pMinCorePrio;
    mgrv = pSolver.getFormulaManager();
    bmgr = mgrv.getBooleanFormulaManager();
    pmgrFwd = new PathFormulaManagerImpl(
        mgrv, pConfig, pLogger, pShutdownNotifier,
        pCfa, AnalysisDirection.FORWARD);
    helper = new PreconditionHelper(mgrv, pConfig, pLogger, pShutdownNotifier, pCfa);
  }

  private Collection<BooleanFormula> literals(BooleanFormula pF) {
    return mgrv.extractLiterals(pF, false, false);
  }

  private BooleanFormula interpolate(BooleanFormula pF, BooleanFormula pCounterF) throws SolverException, InterruptedException {
    List<BooleanFormula> p = enp.extractNewPreds(pF);
    BooleanFormula f = bmgr.and(pF, bmgr.and(p));
    return ipc.getInterpolant(f, pCounterF, p);
  }


  private BooleanFormula subst(BooleanFormula pF) {
    return pF;
  }

  private List<BooleanFormula> subst(List<BooleanFormula> pFormulas) {
    ArrayList<BooleanFormula> result = Lists.newArrayList();
    for (BooleanFormula f: pFormulas) {
      result.add(subst(f));
    }
    return result;
  }


  private List<BooleanFormula> predsFromTrace(ARGPath pPath, BooleanFormula pPrecond, Optional<CFANode> pEntryWpLocation) throws SolverException, InterruptedException, CPATransferException {
    List<BooleanFormula> result = Lists.newArrayList();

    // TODO: It might be possible to use this code to also derive the predicate for the first sate.

    // Get additional predicates from the states along the trace
    //    (or the WPs along the trace)...

    BooleanFormula beforeTransCond = pPrecond; // FIXME: The paper might be wrong here... or hard to understand... (we should start with the negation)

    boolean skippedUntilEntryWpLocation = !pEntryWpLocation.isPresent();

    for (CFAEdge transition: pPath.asEdgesList()) {
      if (!skippedUntilEntryWpLocation) {
        if (transition.getPredecessor().equals(pEntryWpLocation.get())) {
          skippedUntilEntryWpLocation = true;
        } else {
          continue;
        }
      }

      beforeTransCond = interpolateX(pPath, transition, beforeTransCond);

      result.addAll(literals(beforeTransCond));
    }

    return result;
  }

  private BooleanFormula interpolateX(final ARGPath pPath, final CFAEdge transition, final BooleanFormula beforeTransCond) throws CPATransferException, SolverException, InterruptedException {
    // 1. Compute the two formulas (A/B) that are needed to compute a Craig interpolant
    //      afterTransCond === varphi_{k+1}
    // Formula A
    BooleanFormula afterTransCond = helper.getPreconditionOfPath(pPath, Optional.of(transition.getSuccessor()));
    List<BooleanFormula> p = enp.extractNewPreds(afterTransCond);
    afterTransCond = bmgr.and(afterTransCond, bmgr.and(p));

    // Formula B
    BooleanFormula counterAfterTransCond = computeCounterCondition(transition, bmgr.not(beforeTransCond));

    // Compute an interpolant; use a set of candidate predicates.
    //    The candidates for the interpolant are taken from Formula A (since that formula should get over-approximated)
    return ipc.getInterpolant(afterTransCond, counterAfterTransCond, p);
    // TODO: Substitution X/X` and back
  }

  /**
   *
   * @param pTransition           The transition to encode
   * @param pCounterStatePrecond  An uninstanciated formula that describes a precondition
   * @return
   * @throws SolverException
   */
  @VisibleForTesting
  private BooleanFormula computeCounterCondition(CFAEdge pTransition, BooleanFormula pCounterStatePrecond)
      throws CPATransferException, InterruptedException, SolverException {

    final PathFormula pf = pmgrFwd.makeAnd(
        pmgrFwd.makeEmptyPathFormula(),
        pCounterStatePrecond);

    final PathFormula transferPf = pmgrFwd.makeAnd(pf, pTransition);

    return helper.uninstanciatePathFormula(transferPf);
  }

  @Override
  public PredicatePrecision refine(ARGPath pTraceToViolation, ARGPath pTraceToValidTermination, Optional<CFANode> pWpLocation)
      throws SolverException, InterruptedException, CPATransferException {

    // Compute the WP for both traces
    BooleanFormula pcViolation = helper.getPreconditionOfPath(pTraceToViolation, pWpLocation);
    BooleanFormula pcValid = helper.getPreconditionOfPath(pTraceToValidTermination, pWpLocation);

    // "Enrich" the WPs with more general predicates
    pcViolation = interpolate(pcViolation, pcValid);
    pcValid = interpolate(pcValid, pcViolation);

    // Now we have an initial set of useful predicates; add them to the corresponding list.
    List<BooleanFormula> preds = Lists.newArrayList();
    preds.addAll(literals(pcViolation));
    preds.addAll(literals(pcValid));

    // Get additional predicates from the states along the trace
    //    (or the WPs along the trace)...
    //
    // -- along the trace to the violating state...
    preds.addAll(predsFromTrace(pTraceToViolation, pcViolation, pWpLocation));
    // -- along the trace to the termination state...
    preds.addAll(predsFromTrace(pTraceToValidTermination, pcValid, pWpLocation));

    return predicatesAsGlobalPrecision(preds);
  }

  private PredicatePrecision predicatesAsGlobalPrecision(Collection<BooleanFormula> pPreds) {
    Multimap<Pair<CFANode, Integer>, AbstractionPredicate> locationInstancePredicates = HashMultimap.create();
    Multimap<CFANode, AbstractionPredicate> localPredicates = HashMultimap.create();
    Multimap<String, AbstractionPredicate> functionPredicates = HashMultimap.create();
    Collection<AbstractionPredicate> globalPredicates = Lists.newArrayList();

    for (BooleanFormula f: pPreds) {
      AbstractionPredicate ap = amgr.makePredicate(f);
      globalPredicates.add(ap);
    }

    return new PredicatePrecision(
        locationInstancePredicates,
        localPredicates,
        functionPredicates,
        globalPredicates);
  }

}
