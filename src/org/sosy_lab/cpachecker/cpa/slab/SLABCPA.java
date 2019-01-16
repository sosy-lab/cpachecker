/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.slab;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.SymbolicLocationsUtility;
import org.sosy_lab.cpachecker.util.CPAs;

public class SLABCPA extends AbstractSingleWrapperCPA {

  private SLABDomain domain;
  private Configuration config;
  private LogManager logger;
  private ShutdownNotifier shutdownNotifier;
  private PredicateCPA predicateCpa;
  private CFA cfa;
  private Specification specification;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SLABCPA.class);//.withOptions(BlockOperator.class);
  }

  private SLABCPA(
      ConfigurableProgramAnalysis pWrappedCpa,
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification)
      throws InvalidConfigurationException {
    super(pWrappedCpa);
    domain = new SLABDomain(pWrappedCpa.getAbstractDomain());
    predicateCpa = CPAs.retrieveCPAOrFail(getWrappedCpa(), PredicateCPA.class, SLABCPA.class);
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;
    specification = pSpecification;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new SLABTransferRelation(
        getWrappedCpa().getTransferRelation(),
        cfa,
        new SymbolicLocationsUtility(getPredicateCpa(), specification));
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new SLARGState(
        null, null, true, false, getWrappedCpa().getInitialState(pNode, pPartition));
  }

  public CFA getCfa() {
    return cfa;
  }

  @Override
  public StopOperator getStopOperator() {
    return new SLABStopOperator(this.getAbstractDomain());
  }

  public PredicateCPA getPredicateCpa() {
    return predicateCpa;
  }

  LogManager getLogger() {
    return logger;
  }

  public Configuration getConfiguration() {
    return config;
  }

  public ShutdownNotifier getShutdownNotifier() {
    return shutdownNotifier;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return new SLABMergeOperator(domain);
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }
}
