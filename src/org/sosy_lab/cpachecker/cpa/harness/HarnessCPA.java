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
package org.sosy_lab.cpachecker.cpa.harness;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;

/* intended only for use with a witness to generate for each assignment including
 * an extern c function of type pointer or implementation defined behavior
 * the pointer aliasing to which it must conform.
 * the outcome should be a set of maps from program locations to pointer aliases which must be guaranteed
 * it can be seen as a backwards must alias analysis of a reduced set of pointer variables
 */

@Options(prefix = "cpa.harness")
public class HarnessCPA implements ConfigurableProgramAnalysisWithBAM, WrapperCPA {

  protected final TransferRelation transfer;
  protected final ConfigurableProgramAnalysis wrappedCpa;

  @Option(secure=true, name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="which merge operator to use for HarnessCPA")
  private String mergeType = "SEP";

  @Option(
    secure = true,
    name = "stop",
    toUppercase = true,
    values = {"SEP", "JOIN", "NEVER"},
      description="which stop operator to use for HarnessCPA")
  private String stopType = "SEP";

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(HarnessCPA.class);
  }

  private HarnessCPA(
      ConfigurableProgramAnalysis pCpa,
      LogManager pLogger,
      Configuration pConfig,
      CFA pCFA,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    wrappedCpa = pCpa;
    pConfig.inject(this);
    transfer = new HarnessTransferRelation(pCpa, pConfig, pLogger, pCFA, pShutdownNotifier);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public HarnessPrecisionAdjustment getPrecisionAdjustment() {
    return new HarnessPrecisionAdjustment(wrappedCpa.getPrecisionAdjustment());
  }

  protected ConfigurableProgramAnalysis getWrappedCpa() {
    return wrappedCpa;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return new HarnessMergeOperator(wrappedCpa.getMergeOperator());
  }

  @Override
  public StopOperator getStopOperator() {
    return new HarnessStopOperator(wrappedCpa.getStopOperator());
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return wrappedCpa.getInitialPrecision(pNode, pPartition);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    AbstractState wrappedInitialState = getWrappedCpa().getInitialState(pNode, pPartition);
    HarnessState initialHarnessState = new HarnessState(wrappedInitialState);
    return initialHarnessState;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new FlatLatticeDomain();
  }

  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    } else if (pType.isAssignableFrom(wrappedCpa.getClass())) {
      return pType.cast(wrappedCpa);
    } else if (wrappedCpa instanceof WrapperCPA) {
      return ((WrapperCPA) wrappedCpa).retrieveWrappedCpa(pType);
    } else {
      return null;
    }
  }

  @Override
  public ImmutableList<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return ImmutableList.of(wrappedCpa);
  }

}
