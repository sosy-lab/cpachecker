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
package org.sosy_lab.cpachecker.cpa.octagon;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
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
import org.sosy_lab.cpachecker.cpa.octagon.precision.RefineableOctagonPrecision;
import org.sosy_lab.cpachecker.cpa.octagon.precision.StaticFullOctagonPrecision;
import org.sosy_lab.cpachecker.exceptions.InvalidCFAException;
import org.sosy_lab.cpachecker.util.octagon.OctagonFloatManager;
import org.sosy_lab.cpachecker.util.octagon.OctagonIntManager;
import org.sosy_lab.cpachecker.util.octagon.OctagonManager;

@Options(prefix="cpa.octagon")
public final class OctagonCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(OctagonCPA.class).withOptions(OctagonOptions.class);
  }

  @Option(name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="which merge operator to use for OctagonCPA?")
  private String mergeType = "SEP";

  @Option(name="octagonLibrary", toUppercase=true, values={"INT", "FLOAT"},
      description="with this option the number representation in the"
          + " library will be changed between floats and ints.")
  private String octagonLibrary = "INT";

  @Option(name="initialPrecisionType", toUppercase=true, values={"STATIC_FULL", "REFINEABLE_EMPTY"},
      description="this option determines which initial precision should be used")
  private String precisionType = "STATIC_FULL";

  /**
   * In this inner class the options which are needed in several classes of this
   * cpa are kept.
   */
  @Options(prefix="cpa.octagon")
  public static class OctagonOptions {

    @Option(name="handleFloats",
        description="with this option the evaluation of float variables can be toggled.")
    private boolean handleFloats = false;

    public boolean shouldHandleFloats() {
      return handleFloats;
    }
  }

  private final OctagonOptions octagonOptions;
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
  private final OctagonManager octagonManager;

  private OctagonCPA(Configuration config, LogManager log,
                     ShutdownNotifier shutdownNotifier, CFA cfa,
                     OctagonOptions pOctagonOptions)
                     throws InvalidConfigurationException, InvalidCFAException {
    config.inject(this);
    octagonOptions = pOctagonOptions;
    logger = log;
    OctagonDomain octagonDomain = new OctagonDomain(logger);

    if (octagonLibrary.equals("FLOAT")) {
      octagonManager = new OctagonFloatManager();
    } else {
      octagonManager = new OctagonIntManager();
    }

    this.transferRelation = new OctagonTransferRelation(logger, cfa, octagonOptions);

    MergeOperator octagonMergeOp = null;
    if (mergeType.equals("JOIN")) {
      octagonMergeOp = new OctagonMergeJoinOperator(octagonDomain, config);
    } else {
      // default is sep
      octagonMergeOp = MergeSepOperator.getInstance();
    }

    StopOperator octagonStopOp = new StopSepOperator(octagonDomain);

    this.abstractDomain = octagonDomain;
    this.mergeOperator = octagonMergeOp;
    this.stopOperator = octagonStopOp;
    this.precisionAdjustment = StaticPrecisionAdjustment.getInstance();
    this.config = config;
    this.shutdownNotifier = shutdownNotifier;
    this.cfa = cfa;

    if (precisionType.equals("REFINEABLE_EMPTY")) {
      precision = new RefineableOctagonPrecision(config);

      // static full precision is default
    } else {
      precision = new StaticFullOctagonPrecision(octagonOptions);
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
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public AbstractState getInitialState(CFANode node) {
    return new OctagonState(logger, octagonManager);
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

  public OctagonOptions getOctagonOptions() {
    return octagonOptions;
  }
}
