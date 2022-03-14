// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.octagon;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.octagon.OctagonFloatManager;
import org.sosy_lab.cpachecker.util.octagon.OctagonIntManager;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

@Options(prefix = "cpa.octagon")
public final class OctagonCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(OctagonCPA.class);
  }

  @Option(
      secure = true,
      name = "octagonLibrary",
      toUppercase = true,
      values = {"INT", "FLOAT"},
      description =
          "with this option the number representation in the"
              + " library will be changed between floats and ints.")
  private String octagonLibrary = "INT";

  @Option(
      secure = true,
      name = "initialPrecisionType",
      toUppercase = true,
      values = {"STATIC_FULL", "REFINEABLE_EMPTY"},
      description = "this option determines which initial precision should be used")
  private String precisionType = "STATIC_FULL";

  private final AbstractDomain abstractDomain;
  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final LogManager logger;
  private final Precision precision;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;
  private final OctagonManager octagonManager;

  private OctagonCPA(
      Configuration config, LogManager log, ShutdownNotifier shutdownNotifier, CFA cfa)
      throws InvalidConfigurationException, CPAException {
    if (!cfa.getLoopStructure().isPresent()) {
      throw new CPAException("OctagonCPA cannot work without loop-structure information in CFA.");
    }
    config.inject(this);
    logger = log;
    OctagonDomain octagonDomain = new OctagonDomain(logger);

    if (octagonLibrary.equals("FLOAT")) {
      octagonManager = new OctagonFloatManager();
    } else {
      octagonManager = new OctagonIntManager();
    }

    transferRelation = new OctagonTransferRelation(logger, cfa.getLoopStructure().orElseThrow());
    abstractDomain = octagonDomain;
    mergeOperator = OctagonMergeOperator.getInstance(octagonDomain, config);
    stopOperator = new StopSepOperator(octagonDomain);
    this.config = config;
    this.shutdownNotifier = shutdownNotifier;
    this.cfa = cfa;

    if (precisionType.equals("REFINEABLE_EMPTY")) {
      precision =
          VariableTrackingPrecision.createRefineablePrecision(
              config,
              VariableTrackingPrecision.createStaticPrecision(
                  config, cfa.getVarClassification(), getClass()));

      // static full precision is default
    } else {
      precision =
          VariableTrackingPrecision.createStaticPrecision(
              config, cfa.getVarClassification(), getClass());
    }
  }

  public OctagonManager getManager() {
    return octagonManager;
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
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new OctagonState(logger, octagonManager);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
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
