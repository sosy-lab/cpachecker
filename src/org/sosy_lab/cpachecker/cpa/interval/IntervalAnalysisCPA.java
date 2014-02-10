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
package org.sosy_lab.cpachecker.cpa.interval;

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

@Options(prefix="cpa.interval")
public class IntervalAnalysisCPA implements ConfigurableProgramAnalysis {

  /**
   * This method returns a CPAfactory for the interval analysis CPA.
   *
   * @return the CPAfactory for the interval analysis CPA
   */
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(IntervalAnalysisCPA.class);
  }

  @Option(name = "ignoreReferenceCounts",
      description = "do not consider number of references to variables during merge in analysis")
  private boolean ignoreRefCount = false;

  @Option(name="merge", toUppercase=true, values={"SEP", "JOIN"},
          description="which type of merge operator to use for IntervalAnalysisCPA")
  /**
   * the merge type of the interval analysis
   */
  private String mergeType = "SEP";

  /**
   * the abstract domain of the interval analysis
   */
  private AbstractDomain abstractDomain;

  /**
   * the merge operator of the interval analysis
   */
  private MergeOperator mergeOperator;

  /**
   * the stop operator of the interval analysis
   */
  private StopOperator stopOperator;

  /**
   * the transfer relation of the interval analysis
   */
  private TransferRelation transferRelation;

  /**
   * the precision adjustment of the interval analysis
   */
  private PrecisionAdjustment precisionAdjustment;

  /**
   * This method acts as the constructor of the interval analysis CPA.
   *
   * @param config the configuration of the CPAinterval analysis CPA.
   * @throws InvalidConfigurationException
   */
  private IntervalAnalysisCPA(Configuration config) throws InvalidConfigurationException {
    config.inject(this);

    abstractDomain      = new IntervalAnalysisDomain();

    mergeOperator       = mergeType.equals("SEP") ? MergeSepOperator.getInstance() : new MergeJoinOperator(abstractDomain);

    stopOperator        = new StopSepOperator(abstractDomain);

    transferRelation    = new IntervalAnalysisTransferRelation(config);

    precisionAdjustment = StaticPrecisionAdjustment.getInstance();

    IntervalAnalysisState.init(config, ignoreRefCount);
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getAbstractDomain()
   */
  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getMergeOperator()
   */
  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getStopOperator()
   */
  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getTransferRelation()
   */
  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getInitialState(org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode)
   */
  @Override
  public AbstractState getInitialState(CFANode node) {
    return new IntervalAnalysisState();
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getInitialPrecision(org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode)
   */
  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return SingletonPrecision.getInstance();
  }

  /* (non-Javadoc)
   * @see org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis#getPrecisionAdjustment()
   */
  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }
}
