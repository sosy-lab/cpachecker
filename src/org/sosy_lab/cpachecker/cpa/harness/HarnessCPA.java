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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

/* intended only for use with a witness to generate for each assignment including
 * an extern c function of type pointer or implementation defined behavior
 * the pointer aliasing to which it must conform.
 * the outcome should be a set of maps from program locations to pointer aliases which must be guaranteed
 * it can be seen as a backwards must alias analysis of a reduced set of pointer variables
 */

@Options(prefix = "cpa.harness")
public class HarnessCPA extends AbstractCPA {


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

  private HarnessCPA(LogManager pLogger, Configuration pConfig, CFA pCFA)
      throws InvalidConfigurationException {
    super(
        DelegateAbstractDomain.getInstance(),
        new HarnessTransferRelation(
            pConfig,
            pLogger,
            pCFA));
    pConfig.inject(this);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(stopType);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new HarnessState();
  }
}
