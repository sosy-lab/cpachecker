/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.termination;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.termination.TerminationLoopInformation;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class TerminationCPA extends AbstractSingleWrapperCPA {

  private final TerminationLoopInformation terminationInformation;
  private final Configuration config;

  private final AbstractDomain abstractDomain;
  private final TerminationTransferRelation transferRelation;
  private final MergeOperator mergeOperator;
  private final StopOperator stopOperator;
  private final PrecisionAdjustment precisionAdjustment;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TerminationCPA.class);
  }

  public TerminationCPA(
      ConfigurableProgramAnalysis pCpa, CFA pCfa, Configuration pConfig, LogManager pLogger) {
    super(pCpa);

    config = Preconditions.checkNotNull(pConfig);
    terminationInformation = new TerminationLoopInformation(pCfa.getMachineModel(), pLogger);
    transferRelation =
        new TerminationTransferRelation(
            pCpa.getTransferRelation(), terminationInformation, pLogger);
    abstractDomain = new TerminationAbstractDomain(pCpa.getAbstractDomain());
    stopOperator = new TerminationStopOperator(pCpa.getStopOperator());
    mergeOperator = new TerminationMergeOperator(pCpa.getMergeOperator());
    precisionAdjustment = new TerminationPrecisionAdjustment(pCpa.getPrecisionAdjustment());
  }

  public Configuration getConfig() {
    return config;
  }

  public TerminationLoopInformation getTerminationInformation() {
    return terminationInformation;
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
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return TerminationState.createStemState(getWrappedCpa().getInitialState(pNode, pPartition));
  }
}
