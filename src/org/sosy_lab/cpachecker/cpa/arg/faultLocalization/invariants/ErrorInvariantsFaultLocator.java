/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg.faultLocalization.invariants;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.faultLocalization.FaultLocator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.InterpolatingProverEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * {@link FaultLocator} based on Error Invariants.
 * Error Invariants were proposed by Ermis, Schaef, Wies in Error invariants, FM 2012.
 */
public class ErrorInvariantsFaultLocator implements FaultLocator {

  private final FormulaManagerView manager;
  private final CtoFormulaConverter converter;

  private final InterpolationManager interpolator;

  private final LogManager logger;

  public ErrorInvariantsFaultLocator(
      final FormulaManagerView pManager,
      final CtoFormulaConverter pConverter,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final InterpolationManager pInterpolator
  ) throws InvalidConfigurationException {
    manager = pManager;
    converter = pConverter;
    interpolator = pInterpolator;

    logger = pLogger;
  }

  @Override
  public void performLocalization(final CounterexampleInfo pInfo, final ARGPath pErrorPath)
      throws CPAException, InterruptedException, SolverException {
    SSAMapBuilder ssa = SSAMap.emptySSAMap().builder();
    BooleanFormula postCondition = getPostCondition(pErrorPath, ssa);
    locateAll(pInfo, pErrorPath, postCondition, ssa);
  }

  private BooleanFormula getPostCondition(final ARGPath pErrorPath, final SSAMapBuilder pSsa)
      throws CPATransferException, InterruptedException {
    List<CFAEdge> path = pErrorPath.getFullPath();

    int lastAssumeIdx = path.size();
    CFAEdge lastAssumeEdge;
    do {
      lastAssumeIdx--;
      lastAssumeEdge = path.get(lastAssumeIdx);
    }
    while (!lastAssumeEdge.getEdgeType().equals(CFAEdgeType.AssumeEdge));

    BooleanFormula lastAssumption = converter.makePredicate(
            ((CAssumeEdge) lastAssumeEdge).getExpression(),
            lastAssumeEdge,
            "main",
            pSsa);
    return manager.makeNot(lastAssumption);
  }

  private void locateAll(
      final CounterexampleInfo pInfo,
      final ARGPath pErrorPath,
      final BooleanFormula pPostCondition,
      final SSAMapBuilder pSsa
  ) throws SolverException, InterruptedException, CPAException {
    Optional<List<CFAEdge>> newEdges = Optional.of(pErrorPath.getFullPath());
    List<CFAEdge> oldEdges;
    do {
      oldEdges = newEdges.get();
      newEdges = locate(pInfo, oldEdges, pPostCondition, pSsa);
    } while (newEdges.isPresent());
  }

  private Optional<List<CFAEdge>> locate(
      final CounterexampleInfo pInfo,
      final List<CFAEdge> pErrorPathEdges,
      final BooleanFormula pPostCondition,
      final SSAMapBuilder pSsa
  ) throws InterruptedException, CPAException {
    List<BooleanFormula> pathFormula = getPathFormula(pErrorPathEdges, pSsa);
    pathFormula.add(manager.instantiate(pPostCondition, pSsa.build()));

    CounterexampleTraceInfo traceInfo = interpolator.buildCounterexampleTrace(pathFormula);

    if (traceInfo.isSpurious()) {
      List<BooleanFormula> interpolants = traceInfo.getInterpolants();
      List<Integer> relevantOpIndices = getFaultRelevantIndices(interpolants);
      List<CFAEdge> cleanedErrorPath = cleanErrorPath(pErrorPathEdges, relevantOpIndices);

      return Optional.of(cleanedErrorPath);
    } else {
      return Optional.empty();
    }
  }

  private List<BooleanFormula> getPathFormula(final List<CFAEdge> pErrorPath, final SSAMapBuilder
      pSsa)
      throws InterruptedException, CPATransferException {
    List<BooleanFormula> formulas = new ArrayList<>(pErrorPath.size());
    for (CFAEdge e : pErrorPath) {
      Formula f = converter.makeTerm(e, pSsa);
      if (f instanceof BooleanFormula) {
        formulas.add((BooleanFormula) f);
      } else {
        logger.log(Level.WARNING, "Edge ", e, " ignored in fault localization.");
      }
    }

    return formulas;
  }

  private List<CFAEdge> cleanErrorPath(
      final List<CFAEdge> pErrorPathEdges,
      final List<Integer> pRelevantOpIndices
  ) {
    List<CFAEdge> cleanedErrorPath = new ArrayList<>(pErrorPathEdges);
    for (int idx : pRelevantOpIndices) {
      CFAEdge faultyEdge = cleanedErrorPath.get(idx);
      cleanedErrorPath.set(
          idx,
          BlankEdge.buildNoopEdge(faultyEdge.getPredecessor(), faultyEdge.getSuccessor()));
    }
    return cleanedErrorPath;
  }

  private List<Integer> getFaultRelevantIndices(final List<BooleanFormula> pInterpolants) {
    BooleanFormula previousInterpolant = pInterpolants.get(0);
    assert manager.getBooleanFormulaManager().isTrue(previousInterpolant);
    List<Integer> relevantOpIndices = new ArrayList<>(5);
    int currentOpIndex = 0;
    for (BooleanFormula i : pInterpolants) {
      if (!previousInterpolant.equals(i)) {
        relevantOpIndices.add(currentOpIndex);
        previousInterpolant = i;
      }
      currentOpIndex++;
    }

    return relevantOpIndices;
  }

  private <T> List<BooleanFormula> interpolate(
      final CounterexampleInfo pInfo,
      final List<T> pItpStack,
      final List<CFAEdge> pEdges,
      final InterpolatingProverEnvironment<T> pItpProver
  ) throws SolverException, InterruptedException {
    List<BooleanFormula> interpolants = new ArrayList<>();

    int prefixEnd = 0;
    for (CFAEdge e : pEdges) {
      prefixEnd++;
      interpolants.add(pItpProver.getInterpolant(pItpStack.subList(0, prefixEnd)));
    }

    return interpolants;
  }
}
