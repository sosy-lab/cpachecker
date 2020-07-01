/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.testtargets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

@Options(prefix="testcase")
public class TestTargetCPA extends AbstractCPA {

  private final TestTargetPrecisionAdjustment precisionAdjustment;
  private final TransferRelation transferRelation;

  @Option(
    secure = true,
    name = "generate.parallel",
    description = "set to true if run multiple test case generation instances in parallel"
  )
  private boolean runParallel = false;

  @Option(
    secure = true,
    name = "targets.type", // adapt CPAMain.java if adjust name
    description = "Which CFA edges to use as test targets"
  )
  private TestTargetType targetType = TestTargetType.ASSUME;

  @Option(
    secure = true,
    name = "targets.optimization.strategy",
    description = "Which strategy to use to optimize set of test target edges"
  )
  private TestTargetAdaption targetOptimization = TestTargetAdaption.NONE;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TestTargetCPA.class);
  }

  public TestTargetCPA(final CFA pCfa, final Configuration pConfig)
      throws InvalidConfigurationException {
    super("sep", "sep", DelegateAbstractDomain.<TestTargetState>getInstance(), null);

    pConfig.inject(this);

    precisionAdjustment = new TestTargetPrecisionAdjustment();
    transferRelation =
        new TestTargetTransferRelation(
            TestTargetProvider.getTestTargets(pCfa, runParallel, targetType, targetOptimization));
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public AbstractState getInitialState(final CFANode pNode, final StateSpacePartition pPartition)
      throws InterruptedException {
    return TestTargetState.noTargetState();
  }

  @Override
  public TestTargetPrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

}
