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
package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;

/**
 * CPA used to capture the assumptions that ought to be dumped.
 *
 * Note that once the CPA algorithm has finished running, a call
 * to dumpInvariants() is needed to process the reachable states
 * and produce the actual invariants.
 */
public class AssumptionStorageCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(AssumptionStorageCPA.class);
  }

  private final AbstractDomain abstractDomain;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;
  private final FormulaManagerView formulaManager;
  private final AssumptionStorageState topState;

  private AssumptionStorageCPA(Configuration config, LogManager logger, ShutdownNotifier pShutdownNotifier, CFA cfa) throws InvalidConfigurationException {
    formulaManager = new FormulaManagerView(new FormulaManagerFactory(config, logger, pShutdownNotifier).getFormulaManager(), config, logger);
    CtoFormulaTypeHandler typeHandler = new CtoFormulaTypeHandler(logger, cfa.getMachineModel(), formulaManager);
    CtoFormulaConverter converter = new CtoFormulaConverter(new FormulaEncodingOptions(config), formulaManager, cfa.getMachineModel(), cfa.getVarClassification(), logger, typeHandler);
    abstractDomain = new AssumptionStorageDomain(formulaManager);
    stopOperator = new AssumptionStorageStop();
    BooleanFormulaManagerView bfmgr = formulaManager.getBooleanFormulaManager();
    topState = new AssumptionStorageState(formulaManager, bfmgr.makeBoolean(true), bfmgr.makeBoolean(true));
    transferRelation = new AssumptionStorageTransferRelation(converter, formulaManager, topState);
  }

  public FormulaManagerView getFormulaManager() {
    return formulaManager;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public AbstractState getInitialState(CFANode node) {
    return topState;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return SingletonPrecision.getInstance();
  }
}
