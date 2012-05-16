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
package org.sosy_lab.cpachecker.cpa.functionpointer;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithABM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class FunctionPointerCPA extends AbstractSingleWrapperCPA implements ConfigurableProgramAnalysisWithABM {

  private FunctionPointerDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private PrecisionAdjustment precisionAdjustment;
  private final Reducer reducer;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(FunctionPointerCPA.class);
  }

  private FunctionPointerCPA(ConfigurableProgramAnalysis pCpa, CFA pCfa, LogManager pLogger, Configuration pConfig) throws InvalidConfigurationException {
    super(pCpa);
    this.abstractDomain = new FunctionPointerDomain(pCpa.getAbstractDomain());

    MergeOperator wrappedMerge = getWrappedCpa().getMergeOperator();
    if (wrappedMerge == MergeSepOperator.getInstance()) {
      this.mergeOperator = MergeSepOperator.getInstance();
    } else {
      this.mergeOperator = new FunctionPointerMergeOperator(wrappedMerge);
    }

    this.stopOperator = new FunctionPointerStopOperator(pCpa.getStopOperator());
    this.transferRelation = new FunctionPointerTransferRelation(pCpa.getTransferRelation(), pCfa, pLogger, pConfig);
    this.precisionAdjustment = new FunctionPointerPrecisionAdjustment(pCpa.getPrecisionAdjustment());
    if (pCpa instanceof ConfigurableProgramAnalysisWithABM) {
      Reducer wrappedReducer = ((ConfigurableProgramAnalysisWithABM)pCpa).getReducer();
      if (wrappedReducer != null) {
        reducer = new FunctionPointerReducer(wrappedReducer);
      } else {
        reducer = null;
      }
    } else {
      reducer = null;
    }
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
  public AbstractElement getInitialElement(CFANode pNode) {
    return FunctionPointerElement.createEmptyElement(getWrappedCpa().getInitialElement(pNode));
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return getWrappedCpa().getInitialPrecision(pNode);
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }
}