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
package org.sosy_lab.cpachecker.cpa.coverage;

import java.util.Collection;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.IdentityTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.coverage.CoverageData.CoverageMode;

@Options
public class CoverageCPA implements ConfigurableProgramAnalysis, StatisticsProvider {

  public static CPAFactory factory() {
    AutomaticCPAFactory factory = AutomaticCPAFactory.forType(CoverageCPA.class);
    return factory;
  }

  private final TransferRelation transfer;
  private final PrecisionAdjustment prec;
  private final AbstractDomain domain;
  private final Precision precision;
  private final MergeOperator merge;
  private final StopOperator stop;
  private final Statistics stats;

  // STATIC!! only one instance for CPAchecker
  private static CoverageData cov = null;

  @Option(secure=true, name="coverage.mode",
      description="How should coverage be determined? "
          + "REACHED: from the final ARG, "
          + "TRANSFER: from the edges that explored by the transfer relation")
  private CoverageMode mode = CoverageMode.NONE;

  public CoverageCPA(Configuration pConfig, LogManager pLogger, CFA pCFA) throws InvalidConfigurationException {

    pConfig.inject(this);

    domain = new FlatLatticeDomain(CoverageState.getSingleton());
    prec = StaticPrecisionAdjustment.getInstance();
    precision = SingletonPrecision.getInstance();
    stop = new StopSepOperator(domain);
    merge = new MergeJoinOperator(domain);

    if (mode != CoverageMode.NONE) {
      if (cov == null) {
        cov = new CoverageData(mode);
      } else {
        assert cov.getCoverageMode() == mode;
      }
    }

    transfer = mode == CoverageMode.TRANSFER
        ? new CoverageTransferRelation(pCFA, cov)
        : IdentityTransferRelation.INSTANCE;

    stats = new CoverageStatistics(pConfig, pLogger, pCFA, cov);
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
    return prec;
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) {
    return CoverageState.getSingleton();
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return precision;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (cov != null && cov.getCoverageMode() != CoverageMode.NONE) {
      pStatsCollection.add(stats);
    }
  }

}
