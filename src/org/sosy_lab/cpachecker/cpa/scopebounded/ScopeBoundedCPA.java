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
package org.sosy_lab.cpachecker.cpa.scopebounded;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

@Options(prefix = "cpa.scopebounded")
public class ScopeBoundedCPA extends AbstractSingleWrapperCPA {

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  @SuppressWarnings("unused")
  private final Specification specification;

  private final CFA cfa;
  private final Configuration config;

  @Option(description = "Postfix used to detect function stubs (summaries)")
  private String stubPostfix = "___stub";

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ScopeBoundedCPA.class);
  }

  public ScopeBoundedCPA(
      final ConfigurableProgramAnalysis pCpa,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Specification pSpecificaiton,
      final CFA pCFA)
      throws InvalidConfigurationException {

    super(pCpa);

    pConfig.inject(this);

    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    specification = pSpecificaiton;
    cfa = pCFA;
  }

  // override for visibility
  @Override
  protected ConfigurableProgramAnalysis getWrappedCpa() {
    return super.getWrappedCpa();
  }

  LogManager getLogger() {
    return logger;
  }

  Configuration getConfig() {
    return config;
  }

  boolean isStub(final String name) {
    return name.endsWith(stubPostfix);
  }

  boolean hasStub(final String name) {
    return !isStub(name) && cfa.getFunctionHead(name + stubPostfix) != null;
  }

  String originalName(final String stubName) {
    if (!stubName.endsWith(stubPostfix)) {
      throw new IllegalArgumentException("Function " + stubName + "is not a stub");
    }
    return stubName.substring(0, stubName.lastIndexOf(stubPostfix));
  }

  CFA getCFA() {
    return cfa;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return getWrappedCpa().getAbstractDomain();
  }

  @Override
  public AbstractState getInitialState(final CFANode pNode, final StateSpacePartition pPartition)
      throws InterruptedException {
    return getWrappedCpa().getInitialState(pNode, pPartition);
  }

  @Override
  public Precision getInitialPrecision(final CFANode pNode, final StateSpacePartition pPartition)
      throws InterruptedException {
    return getWrappedCpa().getInitialPrecision(pNode, pPartition);
  }

  @Override
  public ScopeBoundedMergeOperator getMergeOperator() {
    return new ScopeBoundedMergeOperator(getWrappedCpa().getMergeOperator());
  }

  @Override
  public ScopeBoundedPrecisionAdjustment getPrecisionAdjustment() {
    return new ScopeBoundedPrecisionAdjustment(getWrappedCpa().getPrecisionAdjustment());
  }

  @Override
  public ScopeBoundedStopOperator getStopOperator() {
    return new ScopeBoundedStopOperator(getWrappedCpa().getStopOperator());
  }

  @Override
  public ScopeBoundedTransferRelation getTransferRelation() {
    return new ScopeBoundedTransferRelation(this, shutdownNotifier);
  }
}
