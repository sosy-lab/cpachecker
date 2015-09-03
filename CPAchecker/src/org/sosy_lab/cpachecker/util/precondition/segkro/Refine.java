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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.precondition.PreconditionHelper;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathPosition;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class Refine implements PreconditionRefiner {

  private static enum FormulaMode { INSTANTIATED, UNINSTANTIATED }

  private final InterpolationWithCandidates ipc;
  private final BooleanFormulaManagerView bmgr;
  private final PathFormulaManager pmgrFwd;
  private final PreconditionHelper helper;
  private final FormulaManagerView mgrv;
  private final AbstractionManager amgr;
  private final ExtractNewPreds enp;

  public Refine(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      CFA pCfa, Solver pSolver, AbstractionManager pAmgr, ExtractNewPreds pExtractNewPreds,
      InterpolationWithCandidates pMinCorePrio)
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

  private Set<BooleanFormula> uninstantiatedLiterals(BooleanFormula pF) {
    return mgrv.uninstantiate(mgrv.extractLiterals(pF));
  }

  @VisibleForTesting
  BooleanFormula interpolate(
      final BooleanFormula pPreconditionA,
      final BooleanFormula pPreconditionB,
      final PathPosition pItpPosition)
          throws SolverException, InterruptedException {

    List<BooleanFormula> p = enp.extractNewPreds(pPreconditionA);
    BooleanFormula f = (p.size() == 0)
        ? pPreconditionA
        : bmgr.and(pPreconditionA, bmgr.and(p));

    return ipc.getInterpolant(f, pPreconditionB, p, pItpPosition.getLocation());
  }

  private PathFormula pre(
      final PathPosition pPreForPosition,
      final FormulaMode pMode)
          throws CPATransferException, SolverException, InterruptedException {

    final ARGPath pathToPosition = pPreForPosition.getPath();
    final PathFormula pf = helper.computePathformulaForArbitraryTrace(pathToPosition, pPreForPosition);
    final BooleanFormula f = mgrv.eliminateDeadVariables(pf.getFormula(), pf.getSsa());

    if (pMode == FormulaMode.UNINSTANTIATED) {
      return alterPf(pmgrFwd.makeEmptyPathFormula(), mgrv.uninstantiate(f));
    } else {
      return alterPf(pf, f);
    }
  }

  @VisibleForTesting
  static class ReversedEdge {

    private CFAEdge original;

    public ReversedEdge(CFAEdge pOriginal) {
      Preconditions.checkNotNull(pOriginal);
      this.original = pOriginal;
    }

    public CFAEdge getOriginal() {
      return original;
    }

    public CFAEdgeType getEdgeType() {
      return original.getEdgeType();
    }

    public CFANode getPredecessor() {
      return original.getSuccessor();
    }

    public CFANode getSuccessor() {
      return original.getPredecessor();
    }

    @Override
    public String toString() {
      return getPredecessor() + " -{" +
          original.getDescription().replaceAll("\n", " ") + "}-> " + getSuccessor();
    }
  }

  @VisibleForTesting
  List<ReversedEdge> getReversedTrace(ARGPath pTrace) {
    List<ReversedEdge> result = Lists.newArrayListWithCapacity(pTrace.asEdgesList().size());
    for (CFAEdge g: Lists.reverse(pTrace.asEdgesList())) {
      if (g != null) {
        result.add(new ReversedEdge(g));
      }
    }
    return result;
  }

  private Multimap<CFANode, BooleanFormula> predsFromTrace(
      final PathPosition pFinalWpPosition,
      final PathFormula pInstanciatedTracePrecond)
          throws SolverException, InterruptedException, CPATransferException {

    Preconditions.checkNotNull(pInstanciatedTracePrecond);
    Preconditions.checkNotNull(pFinalWpPosition);

    ImmutableMultimap.Builder<CFANode, BooleanFormula> result = ImmutableMultimap.builder();

    // TODO: It might be possible to use this code to also derive the predicate for the first sate.

    // Get additional predicates from the states along the trace
    //    (or the WPs along the trace)...

    PathFormula preAtK = pInstanciatedTracePrecond; // FIXME: The paper might be wrong here... or hard to understand... (we should start with the negation)

    PathIterator reverseIt = pFinalWpPosition.reverseIterator();

    while (reverseIt.hasNext()) {

      final CFAEdge t = reverseIt.getIncomingEdge();
      reverseIt.advance();
      final PathPosition nextPos = reverseIt.getPosition();

      if (t == null) {
        continue;
      }

      if (t.getEdgeType() != CFAEdgeType.BlankEdge) {

        //
        //           X                         X'
        //        varphi_k                 varphi_k+1
        //    ...--->o------------------------->o---...
        //         psi_E         t_k
        //

        // 1. Compute the two formulas (A/B) that are needed to compute a Craig interpolant
        //      afterTransCond === varphi_{k+1}

        // Formula A
        PathFormula preAtKp1 = pre(nextPos, FormulaMode.INSTANTIATED);

        if (bmgr.isTrue(preAtKp1.getFormula())) {
          continue;
        }

        final List<BooleanFormula> predsNew = enp.extractNewPreds(preAtKp1.getFormula());
        if (predsNew.size() > 0) {
          preAtKp1 = alterPf(preAtKp1, bmgr.and(preAtKp1.getFormula(), bmgr.and(predsNew)));
        }

        // Formula B
        final PathFormula transFromPreAtK = computeCounterCondition(t,
            alterPf(preAtK, bmgr.not(preAtK.getFormula())));

        // Compute an interpolant; use a set of candidate predicates.
        //    The candidates for the interpolant are taken from Formula A (since that formula should get over-approximated)

        final SSAMap instantiateWith = transFromPreAtK.getSsa();
        preAtK = alterPf(
            transFromPreAtK,
            ipc.getInterpolant(
                mgrv.instantiate(preAtKp1.getFormula(), instantiateWith),
                transFromPreAtK.getFormula(),
                mgrv.instantiate(predsNew, instantiateWith),
                t.getSuccessor()));

        result.putAll(t.getSuccessor(), uninstantiatedLiterals(preAtK.getFormula()));
      }
    }


    return result.build();
  }

  /**
   *
   * @param pTransition           The transition to encode
   * @param pCounterStatePrecond  An uninstanciated formula that describes a precondition
   * @return
   * @throws SolverException
   */
  private PathFormula computeCounterCondition(
      final CFAEdge pTransition,
      final PathFormula pCounterStatePrecond)
      throws CPATransferException, InterruptedException {

    return pmgrFwd.makeAnd(pCounterStatePrecond, pTransition);
  }

  private PathFormula alterPf(PathFormula pPf, BooleanFormula pF) {
    return new PathFormula(
        pF,
        pPf.getSsa(),
        PointerTargetSet.emptyPointerTargetSet(),
        1);
  }

  @Override
  public PredicatePrecision refine(
      final PathPosition pTraceFromViolation,
      final PathPosition pTraceFromValidTermination)
    throws SolverException, InterruptedException, CPATransferException {

    // Compute the precondition for both traces
    PathFormula pcViolation = pre(pTraceFromViolation, FormulaMode.INSTANTIATED);
    PathFormula pcValid = pre(pTraceFromValidTermination, FormulaMode.INSTANTIATED);

    // "Enrich" the preconditions with more general predicates
    // TODO: Is this part completely useless when using QE?
    pcViolation = alterPf(pcViolation,
        interpolate(pcViolation.getFormula(), pcValid.getFormula(), pTraceFromViolation));
    pcValid = alterPf(pcValid,
        interpolate(pcValid.getFormula(), pcViolation.getFormula(), pTraceFromValidTermination));

    // Now we have an initial set of useful predicates; add them to the corresponding list.
    Builder<BooleanFormula> globalPreds = ImmutableList.builder();
    ImmutableMultimap.Builder<CFANode, BooleanFormula> localPreds = ImmutableMultimap.builder();

    globalPreds.addAll(uninstantiatedLiterals(pcViolation.getFormula()));
    globalPreds.addAll(uninstantiatedLiterals(pcValid.getFormula()));

    // Get additional predicates from the states along the trace
    //    (or the WPs along the trace)...
    //
    // -- along the trace to the violating state...
    Multimap<CFANode, BooleanFormula> predsViolation = predsFromTrace(pTraceFromViolation, pcViolation);
    localPreds.putAll(predsViolation);
    // -- along the trace to the termination state...
    Multimap<CFANode, BooleanFormula> predsFromValid = predsFromTrace(pTraceFromValidTermination, pcValid);
    localPreds.putAll(predsFromValid);

    return predicatesAsGlobalPrecision(globalPreds.build(), localPreds.build());
  }

  private PredicatePrecision predicatesAsGlobalPrecision(
      final ImmutableList<BooleanFormula> pGlobalPreds,
      final ImmutableMultimap<CFANode, BooleanFormula> pLocalPreds) {

    Multimap<Pair<CFANode, Integer>, AbstractionPredicate> locationInstancePredicates = HashMultimap.create();
    Multimap<CFANode, AbstractionPredicate> localPredicates = HashMultimap.create();
    Multimap<String, AbstractionPredicate> functionPredicates = HashMultimap.create();
    Collection<AbstractionPredicate> globalPredicates = Lists.newArrayList();

    // TODO: Make the local predicates really local!!
    for (BooleanFormula f: pLocalPreds.values()) {
      AbstractionPredicate ap = amgr.makePredicate(f);
      globalPredicates.add(ap);
    }

    for (BooleanFormula f: pGlobalPreds) {
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
