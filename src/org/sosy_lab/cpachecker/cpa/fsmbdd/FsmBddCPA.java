/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.fsmbdd;

import net.sf.javabdd.BDDFactory;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.fsmbdd.interfaces.DomainIntervalProvider;

/**
 * CPA for the verification of finite state machines
 * using binary decision diagrams.
 */
@Options(prefix="cpa.fsmbdd")
public class FsmBddCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(FsmBddCPA.class);
  }

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private FsmTransferRelation transferRelation;
  private PrecisionAdjustment precisionAdjustment;
  private FsmPrecision precision;

  private final Configuration config;
  private final LogManager logger;

  private final BDDFactory bddFactory;
  private final DomainIntervalProvider domainIntervalProvider;

  private FsmBddCPA(Configuration pConfig, LogManager pLogger, CFA pCfa) throws InvalidConfigurationException {
    this.config = pConfig;
    this.logger = pLogger;

    pConfig.inject(this);

    //
    // Initialization of the BDD library (JavaBdd)
    //
    this.bddFactory = BDDFactory.init("java", 50000000, 2000000);
    this.bddFactory.setIncreaseFactor(1);
    this.bddFactory.setMaxIncrease(200000000);

    //
    // Initialization of the (remaining) components of the CPA.
    //
    this.domainIntervalProvider = new FsmSyntaxAnalizer(pCfa);
    this.abstractDomain = new FsmDomain();
    this.transferRelation = new FsmTransferRelation();
    this.transferRelation.setDomainIntervalProvider(domainIntervalProvider);
    this.precision = initializePrecision(pConfig, pCfa);
    this.stopOperator = initializeStopOperator();
    this.mergeOperator = initializeMergeOperator();
    this.precisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }

  private MergeOperator initializeMergeOperator() {
    return new FsmMergeOperator(abstractDomain);
  }

  private StopOperator initializeStopOperator() {
    return new StopSepOperator(abstractDomain);
  }

  private FsmPrecision initializePrecision(Configuration config, CFA cfa) throws InvalidConfigurationException {
    return new FsmPrecision();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
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
  public AbstractState getInitialState(CFANode node) {
    return new FsmState(bddFactory);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return precision;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  public Configuration getConfiguration() {
    return config;
  }

  public LogManager getLogger() {
    return logger;
  }


}
