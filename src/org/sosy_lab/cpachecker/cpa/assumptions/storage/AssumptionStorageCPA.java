// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

/**
 * CPA used to capture the assumptions that ought to be dumped.
 *
 * <p>Note that once the CPA algorithm has finished running, a call to dumpInvariants() is needed to
 * process the reachable states and produce the actual invariants.
 */
public class AssumptionStorageCPA
    implements ConfigurableProgramAnalysis, ProofChecker, AutoCloseable {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(AssumptionStorageCPA.class);
  }

  private final AssumptionStorageTransferRelation transferRelation;
  private final FormulaManagerView formulaManager;
  private final AssumptionStorageState topState;

  private final Solver solver;

  private AssumptionStorageCPA(
      Configuration config, LogManager logger, ShutdownNotifier pShutdownNotifier, CFA cfa)
      throws InvalidConfigurationException {
    solver = Solver.create(config, logger, pShutdownNotifier);
    formulaManager = solver.getFormulaManager();
    FormulaEncodingOptions options = new FormulaEncodingOptions(config);
    CtoFormulaTypeHandler typeHandler = new CtoFormulaTypeHandler(logger, cfa.getMachineModel());
    CtoFormulaConverter converter =
        new CtoFormulaConverter(
            options,
            formulaManager,
            cfa.getMachineModel(),
            cfa.getVarClassification(),
            logger,
            pShutdownNotifier,
            typeHandler,
            AnalysisDirection.FORWARD);
    BooleanFormulaManagerView bfmgr = formulaManager.getBooleanFormulaManager();
    topState = new AssumptionStorageState(formulaManager, bfmgr.makeTrue(), bfmgr.makeTrue());
    transferRelation = new AssumptionStorageTransferRelation(converter, formulaManager, topState);
  }

  public FormulaManagerView getFormulaManager() {
    return formulaManager;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new AssumptionStorageDomain();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return topState;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return new AssumptionStorageStop();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return new AssumptionStoragePrecisionAdjustment(transferRelation);
  }

  @Override
  public boolean areAbstractSuccessors(
      AbstractState pState, CFAEdge pCfaEdge, Collection<? extends AbstractState> pSuccessors)
      throws CPATransferException, InterruptedException {
    // always assume is successor, only write and read states that have true assumptions, stop
    // formulae
    return true;
  }

  @Override
  public boolean isCoveredBy(AbstractState pState, AbstractState pOtherState)
      throws CPAException, InterruptedException {
    // always assume is covered, only write and read states that have true assumptions, stop
    // formulae
    return true;
  }

  @Override
  public void close() {
    solver.close();
  }
}
