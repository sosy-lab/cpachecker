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
package org.sosy_lab.cpachecker.cpa.abe;

import com.google.common.base.Function;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Helper CPA for configurable program analyses based on SMT
 * and adjustable block encoding.
 * Meant to be used as a field inside another CPA.
 */
@Options(prefix="cpa.abe")
public final class ABECPA
    <
        A extends ABEAbstractedState<A>,
        P extends Precision
    >
    extends SingleEdgeTransferRelation
    implements ConfigurableProgramAnalysis,
               AbstractDomain,
               PrecisionAdjustment,
               MergeOperator {

  @Option(secure=true,
      description="Cache formulas produced by path formula manager")
  private boolean useCachingPathFormulaManager = true;

  private final ABEWrappingManager<A, P> manager;

  public ABECPA(
      Configuration pConfiguration,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCFA,
      ABEManager<A, P> clientManager,
      Solver pSolver
  ) throws InvalidConfigurationException {
    pConfiguration.inject(this, ABECPA.class);

    FormulaManagerView formulaManager = pSolver.getFormulaManager();
    PathFormulaManager pathFormulaManager = new PathFormulaManagerImpl(
        formulaManager, pConfiguration, pLogger, pShutdownNotifier, pCFA,
        AnalysisDirection.FORWARD);

    if (useCachingPathFormulaManager) {
      pathFormulaManager = new CachingPathFormulaManager(pathFormulaManager);
    }
    manager = new ABEWrappingManager<>(clientManager, pathFormulaManager,
        formulaManager, pCFA, pLogger, pSolver, pConfiguration);
  }

  @Override
  public AbstractState join(
      AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    throw new UnsupportedOperationException("Join operator not supported.");
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean isLessOrEqual(
      AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    return manager.isLessOrEqual((ABEState<A>) state1, (ABEState<A>) state2);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return this;
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(this);
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return this;
  }

  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return manager.getInitialState(node, partition);
  }

  @Override
  public Precision getInitialPrecision(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return manager.getInitialPrecision(node, partition);
  }

  @Override
  @SuppressWarnings("unchecked")
  public AbstractState merge(
      AbstractState state1, AbstractState state2, Precision precision)
      throws CPAException, InterruptedException {
    return manager.merge(
        (ABEState<A>) state1,
        (ABEState<A>) state2
    );
  }

  @Override
  @SuppressWarnings("unchecked")
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState) throws CPAException, InterruptedException {
    return manager.prec(
        (ABEState<A>) state,
        (P) precision,
        states,
        fullState
    );
  }

  @Override
  @SuppressWarnings("unchecked")
  public Optional<? extends AbstractState> strengthen(
      AbstractState pState, Precision pPrecision,
      List<AbstractState> otherStates)
      throws CPAException, InterruptedException {
    return manager.strengthen(
        (ABEState<A>) pState,
        (P) pPrecision, otherStates);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    return manager.getAbstractSuccessorsForEdge(
        (ABEState<A>) state,
        cfaEdge
    );
  }
}
