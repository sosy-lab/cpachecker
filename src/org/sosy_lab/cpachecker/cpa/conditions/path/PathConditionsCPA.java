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
package org.sosy_lab.cpachecker.cpa.conditions.path;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopAlwaysOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;

/**
 * CPA for path conditions ({@link PathCondition}).
 * It can be configured to work with any condition that implements this interface.
 */
@Options(prefix="cpa.conditions.path")
public class PathConditionsCPA implements ConfigurableProgramAnalysis, AdjustableConditionCPA, StatisticsProvider {

  @Option(description="The condition", name="condition", required=true)
  @ClassOption(packagePrefix="org.sosy_lab.cpachecker.cpa.conditions.path")
  private Class<? extends PathCondition> conditionClass;

  private final PathCondition condition;

  private final AbstractDomain domain = new FlatLatticeDomain();
  private final TransferRelation transfer = new TransferRelation() {
      @Override
      public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState pElement, Precision pPrecision,
          CFAEdge pCfaEdge) {
        return Collections.singleton(condition.getAbstractSuccessor(pElement, pCfaEdge));
      }

      @Override
      public Collection<? extends AbstractState> strengthen(AbstractState pElement,
          List<AbstractState> pOtherElements, CFAEdge pCfaEdge, Precision pPrecision) {
        return null;
      }
    };


  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PathConditionsCPA.class);
  }

  private PathConditionsCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);

    Class<?>[] argumentTypes = { Configuration.class, LogManager.class };
    Object[] argumentValues = { config, logger };
    condition = Classes.createInstance(PathCondition.class, conditionClass, argumentTypes, argumentValues);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (condition instanceof StatisticsProvider) {
      ((StatisticsProvider)condition).collectStatistics(pStatsCollection);

    } else if (condition instanceof Statistics) {
      pStatsCollection.add((Statistics)condition);
    }
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    return condition.getInitialState(pNode);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return SingletonPrecision.getInstance();
  }

  @Override
  public boolean adjustPrecision() {
    return condition.adjustPrecision();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return StopAlwaysOperator.getInstance();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }
}
