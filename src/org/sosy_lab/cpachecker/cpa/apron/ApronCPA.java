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
package org.sosy_lab.cpachecker.cpa.apron;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
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
import org.sosy_lab.cpachecker.cpa.apron.precision.RefineableApronPrecision;
import org.sosy_lab.cpachecker.cpa.apron.precision.StaticFullApronPrecision;
import org.sosy_lab.cpachecker.exceptions.InvalidCFAException;

import apron.ApronException;

@Options(prefix="cpa.apron")
public final class ApronCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ApronCPA.class);
  }

  @Option(name="initialPrecisionType", toUppercase=true, values={"STATIC_FULL", "REFINEABLE_EMPTY"},
      description="this option determines which initial precision should be used")
  private String precisionType = "STATIC_FULL";

  private final AbstractDomain abstractDomain;
  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final LogManager logger;
  private final Precision precision;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;
  private final ApronManager apronManager;

  private ApronCPA(Configuration config, LogManager log,
                     ShutdownNotifier shutdownNotifier, CFA cfa)
                     throws InvalidConfigurationException, InvalidCFAException {
    config.inject(this);
    logger = log;
    ApronDomain apronDomain = new ApronDomain(logger);

    apronManager = new ApronManager(config);

    this.transferRelation = new ApronTransferRelation(logger, cfa);

    MergeOperator apronMergeOp = ApronMergeOperator.getInstance(apronDomain, config);

    StopOperator apronStopOp = new StopSepOperator(apronDomain);

    this.abstractDomain = apronDomain;
    this.mergeOperator = apronMergeOp;
    this.stopOperator = apronStopOp;
    this.precisionAdjustment = StaticPrecisionAdjustment.getInstance();
    this.config = config;
    this.shutdownNotifier = shutdownNotifier;
    this.cfa = cfa;

    if (precisionType.equals("REFINEABLE_EMPTY")) {
      precision = new RefineableApronPrecision(config);

      // static full precision is default
    } else {
      precision = new StaticFullApronPrecision();
    }
  }

  public ApronManager getManager() {
    return apronManager;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
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
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public AbstractState getInitialState(CFANode node) {
    try {
      return new ApronState(logger, apronManager);
    } catch (ApronException e) {
      throw new RuntimeException("An error occured while operating with the apron library", e);
    }
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return precision;
  }

  public Configuration getConfiguration() {
    return config;
  }

  public LogManager getLogger() {
    return logger;
  }

  public ShutdownNotifier getShutdownNotifier() {
    return shutdownNotifier;
  }

  public CFA getCFA() {
    return cfa;
  }
}
