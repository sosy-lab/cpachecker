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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithABM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

@Options(prefix="cpa.explicit")
public class ExplicitCPA implements ConfigurableProgramAnalysisWithABM, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ExplicitCPA.class);
  }

  @Option(name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="which merge operator to use for ExplicitCPA")
  private String mergeType = "SEP";

  @Option(name="stop", toUppercase=true, values={"SEP", "JOIN", "NEVER"},
      description="which stop operator to use for ExplicitCPA")
  private String stopType = "SEP";

  @Option(name="variableBlacklist",
      description="blacklist regex for variables that won't be tracked by ExplicitCPA")
  private String variableBlacklist = "";

  private ExplicitPrecision precision;

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private PrecisionAdjustment precisionAdjustment;
  private final ExplicitReducer reducer;
  private final ExplicitCPAStatistics statistics;

  private final CFA cfa;

  private final Configuration config;
  private final LogManager logger;

  private ExplicitCPA(Configuration config, LogManager logger, CFA cfa) throws InvalidConfigurationException {
    this.cfa    = cfa;
    this.config = config;
    this.logger = logger;

    config.inject(this);

    abstractDomain      = new ExplicitDomain();
    transferRelation    = new ExplicitTransferRelation(config);
    precision           = initializePrecision(config);
    mergeOperator       = initializeMergeOperator();
    stopOperator        = initializeStopOperator();
    precisionAdjustment = StaticPrecisionAdjustment.getInstance();
    reducer             = new ExplicitReducer();
    statistics          = new ExplicitCPAStatistics();
  }

  private MergeOperator initializeMergeOperator() {
    if(mergeType.equals("SEP")) {
      return MergeSepOperator.getInstance();
    }

    else if(mergeType.equals("JOIN")) {
      return new MergeJoinOperator(abstractDomain);
    }

    return null;
  }

  private StopOperator initializeStopOperator() {
    if(stopType.equals("SEP")) {
      return new StopSepOperator(abstractDomain);
    }

    else if(stopType.equals("JOIN")) {
      return new StopJoinOperator(abstractDomain);
    }

    else if(stopType.equals("NEVER")) {
      return new StopNeverOperator();
    }

    return null;
  }

  private ExplicitPrecision initializePrecision(Configuration config) throws InvalidConfigurationException {
    return new ExplicitPrecision(variableBlacklist, config);
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
  public AbstractElement getInitialElement(CFANode node) {
    return new ExplicitElement();
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return precision;
  }

  ExplicitPrecision getPrecision() {
    return precision;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  protected Configuration getConfiguration() {
    return config;
  }

  protected LogManager getLogger() {
    return logger;
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

  ExplicitCPAStatistics getStats() {
    return statistics;
  }

  CFA getCFA() {
    return cfa;
  }
}
