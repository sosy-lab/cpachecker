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
package org.sosy_lab.cpachecker.cpa.sign;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
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

@Options(prefix="cpa.sign")
public class SignCPA implements ConfigurableProgramAnalysis {

  private SignDomain domain;

  private SignTransferRelation transfer;

  private LogManager logger;

  @Option(name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="which merge operator to use for SignCPA")
  private String mergeType = "JOIN";

  @Option(name="stop", toUppercase=true, values={"SEP", "JOIN"},
      description="which stop operator to use for SignCPA")
  private String stopType = "SEP";

  private StopOperator stop;
  private MergeOperator merge;

  public SignCPA(LogManager pLogger, Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    domain = new SignDomain();
    transfer = new SignTransferRelation(logger);

    if (stopType.equals("SEP")) {
      stop = new StopSepOperator(domain);
    } else {
      stop = new StopJoinOperator(domain);
    }
    if (mergeType.equals("SEP")) {
      merge = new MergeSepOperator();
    } else {
      merge = new MergeJoinOperator(domain);
    }

    SignState.init(config);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SignCPA.class);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    return SignState.TOP;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return SingletonPrecision.getInstance();
  }

}
