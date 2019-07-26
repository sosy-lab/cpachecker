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
package org.sosy_lab.cpachecker.cpa.ifcsecurity;

import java.nio.file.Path;
import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.combet.IFXMLStatistics;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.combetoutputgenerator.StatisticFile;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.AllTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.DepPrecision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.DependencyPrecision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.ImplicitDependencyPrecision;

/**
 * CPA for tracking which variables/functions are dependent on which other variables/functions
 */
@Options(prefix = "cpa.ifcsecurity")
public class DependencyTrackerCPA extends AbstractCPA implements StatisticsProvider, ProofCheckerCPA {

  @SuppressWarnings("unused")
  private LogManager logger;

  @Option(secure = true, description = "get an initial precision from file")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path initialPrecisionFile = null;
  private DependencyPrecision precision;

  @Option(
      secure = true,
      name = "merge",
      toUppercase = true,
      values = { "SEP", "JOIN" },
      description = "which merge operator to use for DependencyTrackerCPA")
  private String mergeType = "JOIN";

  @Option(
      secure = true,
      name = "stop",
      toUppercase = true,
      values = { "SEP", "JOIN" },
      description = "which stop operator to use for DependencyTrackerCPA")
  private String stopType = "SEP";

  @Option(
      secure = true,
      name = "precisionType",
      values = { "pol-indep","pol-dep"},
      description = "which stop operator to use for DependencyTrackerCPA")
  private String precisionType = "pol-indep";


  private final IFXMLStatistics statistics;
  private final StatisticFile statistics2;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DependencyTrackerCPA.class);
  }

  @SuppressWarnings("unused")
  private DependencyTrackerCPA(LogManager pLogger, Configuration pConfig,
      ShutdownNotifier pShutdownNotifier, CFA pCfa) throws InvalidConfigurationException {
    super(
        "irrelevant", // operator-initialization is overridden
        "irrelevant", // operator-initialization is overridden
        DelegateAbstractDomain.<DependencyTrackerState> getInstance(),
        new DependencyTrackerRelation(pLogger));
    pConfig.inject(this);
    this.logger = pLogger;
    precision = choosePrecision(pConfig, pLogger);
    statistics= new IFXMLStatistics(this, pConfig, pCfa,precision);
    statistics2 = new StatisticFile(this, pConfig, pCfa, precision);
  }

  private DependencyPrecision choosePrecision(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    //TODO other choices by option
    if(precisionType.equals("pol-dep")){
      return new ImplicitDependencyPrecision(pConfig, pLogger);
    }
    if(precisionType.equals("pol-indep")){
      return new AllTrackingPrecision(pConfig, pLogger);
    }
    return null;
  }

  public LogManager getLogger() {
    return logger;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(stopType);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    DependencyTrackerState initialstate = new DependencyTrackerState();
    initialstate.setPrec((DepPrecision)precision);
    //TODO Add initial Dependencies
    return initialstate;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return precision;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
    pStatsCollection.add(statistics2);
  }
}
