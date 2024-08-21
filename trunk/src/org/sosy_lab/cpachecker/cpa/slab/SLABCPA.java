// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slab;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
import org.sosy_lab.cpachecker.core.specification.Specification;
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
    return AutomaticCPAFactory.forType(SLABCPA.class); // .withOptions(BlockOperator.class);
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
    return new SLABStopOperator(getAbstractDomain());
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
