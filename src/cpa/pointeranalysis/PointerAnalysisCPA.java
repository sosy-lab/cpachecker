/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.pointeranalysis;

import cfa.objectmodel.CFAFunctionDefinitionNode;

import common.configuration.Configuration;
import common.configuration.Option;
import common.configuration.Options;

import cpa.common.LogManager;
import cpa.common.defaults.AbstractCPAFactory;
import cpa.common.defaults.MergeJoinOperator;
import cpa.common.defaults.MergeSepOperator;
import cpa.common.defaults.StaticPrecisionAdjustment;
import cpa.common.defaults.StopSepOperator;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.MergeOperator;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperator;
import cpa.common.interfaces.TransferRelation;
import exceptions.InvalidConfigurationException;

/**
 * @author Philipp Wendler
 */
@Options(prefix="cpas.pointeranalysis")
public class PointerAnalysisCPA implements ConfigurableProgramAnalysis {

  private static class PointerAnalysisCPAFactory extends AbstractCPAFactory {
    
    @Override
    public ConfigurableProgramAnalysis createInstance() throws InvalidConfigurationException {
      return new PointerAnalysisCPA(getConfiguration(), getLogger());
    }
  }
  
  public static CPAFactory factory() {
    return new PointerAnalysisCPAFactory();
  }
  
  @Option(name="merge", values={"sep", "join"})
  private String mergeType = "sep";
  
  @Option
  private boolean printWarnings = true;
  
  private final AbstractDomain abstractDomain;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final TransferRelation transferRelation;
  
  private PointerAnalysisCPA(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);
    PointerAnalysisDomain domain = new PointerAnalysisDomain();
    
    MergeOperator mergeOp = null;
    if(mergeType.equals("sep")) {
      mergeOp = MergeSepOperator.getInstance();
    }
    if(mergeType.equals("join")) {
      mergeOp = new MergeJoinOperator(domain.getJoinOperator());
    }

    abstractDomain = domain;
    mergeOperator = mergeOp;
    stopOperator = new StopSepOperator(domain.getPartialOrder());
    transferRelation = new PointerAnalysisTransferRelation(printWarnings, logger);
  }
  
  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public AbstractElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    return new PointerAnalysisElement(pNode.getFunctionName());
  }

  @Override
  public Precision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    return null;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }
}