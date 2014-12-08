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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.precondition.PreconditionHelper;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.InterpolationWithCandidates;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

public class Refine {

  private final ExtractNewPreds enp;
  private final InterpolationWithCandidates ipc;
  private final FormulaManager mgr;
  private final FormulaManagerView mgrv;
  private final BooleanFormulaManagerView bmgr;
  private final PathFormulaManager pmgrFwd;
  private final PreconditionHelper helper;

  public Refine(Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdown, CFA pCfa,
      ExtractNewPreds pEnp, InterpolationWithCandidates pIpc, FormulaManager pMgr, FormulaManagerView pMgrv)
          throws InvalidConfigurationException {

    enp = pEnp;
    ipc = pIpc;
    mgr = pMgr;
    mgrv = pMgrv;
    bmgr = mgrv.getBooleanFormulaManager();
    pmgrFwd = new PathFormulaManagerImpl(
        pMgrv, pConfig, pLogger, pShutdown,
        pCfa, AnalysisDirection.FORWARD);
    helper = new PreconditionHelper(pMgrv);
  }

  private Collection<BooleanFormula> atoms(BooleanFormula pF) {
    return mgrv.extractAtoms(pF, false, false);
  }

  private BooleanFormula interpolate(BooleanFormula pF, BooleanFormula pCounterF) throws SolverException, InterruptedException {
    List<BooleanFormula> p = enp.extractNewPreds(pF);
    BooleanFormula f = bmgr.and(pF, bmgr.and(p));
    return ipc.getInterpolant(f, pCounterF, p);
  }


  private BooleanFormula subst(BooleanFormula pF) {
    return null;
  }

  private List<BooleanFormula> subst(List<BooleanFormula> pFormulas) {
    ArrayList<BooleanFormula> result = Lists.newArrayList();
    for (BooleanFormula f: pFormulas) {
      result.add(subst(f));
    }
    return result;
  }


  private List<BooleanFormula> predsFromTrace(ARGPath pPath, BooleanFormula pPrecond) throws SolverException, InterruptedException, CPATransferException {
    List<BooleanFormula> result = Lists.newArrayList();

    // TODO: It might be possible to use this code to also derive the predicate for the first sate.

    // Get additional predicates from the states along the trace
    //    (or the WPs along the trace)...

    final PathIterator it = pPath.pathIterator();
    BooleanFormula counterStatePrecond = pPrecond; // FIXME: The paper might be wrong here... or hard to understand... (we should start with the negation)

    while (it.hasNext()) {
      it.advance();
      final CFAEdge transition = it.getIncomingEdge(); // TODO: Depends on the direction of the analysis. Check this
      final ARGState state = it.getAbstractState();

      BooleanFormula statePrecond = helper.getPathStatePrecondition(pPath, state);
      List<BooleanFormula> p = enp.extractNewPreds(statePrecond);
      statePrecond = bmgr.and(statePrecond, bmgr.and(p));

      counterStatePrecond = computeCounterPrecondition(transition, counterStatePrecond);
      pPrecond = ipc.getInterpolant(subst(statePrecond), counterStatePrecond, subst(p));

      result.addAll(atoms(pPrecond));
    }

    return result;
  }

  /**
   *
   * @param pTransition           The transition to encode
   * @param pCounterStatePrecond  An uninstanciated formula that describes a precondition
   * @return
   * @throws SolverException
   */
  @VisibleForTesting
  private BooleanFormula computeCounterPrecondition(CFAEdge pTransition, BooleanFormula pCounterStatePrecond)
      throws CPATransferException, InterruptedException, SolverException {

    final PathFormula pf = pmgrFwd.makeAnd(
        pmgrFwd.makeEmptyPathFormula(),
        pCounterStatePrecond);

    final PathFormula transferPf = pmgrFwd.makeAnd(pf, pTransition);

    return helper.uninstanciatePathFormula(transferPf);
  }

  public Collection<BooleanFormula> refine(ARGPath pTraceToViolation, ARGPath pTraceToValidTermination) throws SolverException, InterruptedException, CPATransferException {
    // Compute the WP for both traces
    BooleanFormula pcViolation = helper.getPathPrecond(pTraceToViolation);
    BooleanFormula pcValid = helper.getPathPrecond(pTraceToValidTermination);

    // "Enrich" the WPs with more general predicates
    pcViolation = interpolate(pcViolation, pcValid);
    pcValid = interpolate(pcValid, pcViolation);

    // Now we have an initial set of useful predicates; add them to the corresponding list.
    List<BooleanFormula> preds = Lists.newArrayList();
    preds.addAll(atoms(pcViolation));
    preds.addAll(atoms(pcValid));

    // Get additional predicates from the states along the trace
    //    (or the WPs along the trace)...
    //
    // -- along the trace to the violating state...
    preds.addAll(predsFromTrace(pTraceToViolation, pcViolation));
    // -- along the trace to the termination state...
    preds.addAll(predsFromTrace(pTraceToValidTermination, pcValid));

    return preds;
  }

}
