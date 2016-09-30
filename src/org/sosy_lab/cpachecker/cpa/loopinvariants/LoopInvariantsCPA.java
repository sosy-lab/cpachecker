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
package org.sosy_lab.cpachecker.cpa.loopinvariants;

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
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

/**
 * This CPA computes a loop invariant of a single loop, which contains only assignments,
 * no conditions are considered. Assignments can only contain linear expressions.
 * To compute the invariant an isympy installation is necessary.
 */
@Options( prefix = "cpa.loopinvariants" )
public class LoopInvariantsCPA extends AbstractCPA implements ConfigurableProgramAnalysis {

  /**
   * Gets a factory for creating LoopInvariantsCPAs.
   *
   * @return a factory for creating LoopInvariantsCPAs.
   */
  public static CPAFactory factory() {
    // return AutomaticCPAFactory.forType(ApronCPA.class);
    return AutomaticCPAFactory.forType(LoopInvariantsCPA.class);
  }

  @Option(
      secure = true,
      toUppercase = true,
      values = { "SEP", "MERGE" },
      description = "this option determines which merge operator to use")
  private String mergeType = "SEP";

  public LoopInvariantsCPA(Configuration pConfig, CFA pCFA, LogManager log) throws InvalidConfigurationException {
    super("SEP", "SEP", DelegateAbstractDomain.<LoopInvariantsState> getInstance(),
        new LoopInvariantsTransferRelation(pCFA, log));
    pConfig.inject(this, LoopInvariantsCPA.class);
  }


  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new LoopInvariantsState();
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }

}
