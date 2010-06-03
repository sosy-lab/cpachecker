/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;

import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class TransferRelationMonitorCPA extends AbstractSingleWrapperCPA implements StatisticsProvider {

  private static class TransferRelationMonitorCPAFactory extends AbstractSingleWrapperCPAFactory {

    @Override
    public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
      return new TransferRelationMonitorCPA(getChild(), getConfiguration());
    }
  }

  public static CPAFactory factory() {
    return new TransferRelationMonitorCPAFactory();
  }

  private final AbstractDomain abstractDomain;
  private final TransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;
  private final Statistics stats;

  private TransferRelationMonitorCPA(ConfigurableProgramAnalysis pCpa, Configuration config) throws InvalidConfigurationException {
    super(pCpa);
    abstractDomain = new TransferRelationMonitorDomain(this);
    transferRelation = new TransferRelationMonitorTransferRelation(getWrappedCpa(), config);
    precisionAdjustment = StaticPrecisionAdjustment.getInstance(); // TODO
    mergeOperator = new TransferRelationMonitorMerge(getWrappedCpa());
    stopOperator = new TransferRelationMonitorStop(getWrappedCpa());
    stats = new TransferMonitorStatistics();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return this.abstractDomain;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    return new TransferRelationMonitorElement(this, getWrappedCpa().getInitialElement(pNode));
  }

  @Override
  public MergeOperator getMergeOperator() {
    return this.mergeOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return this.precisionAdjustment;
  }

  @Override
  public StopOperator getStopOperator() {
    return this.stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this.transferRelation;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}