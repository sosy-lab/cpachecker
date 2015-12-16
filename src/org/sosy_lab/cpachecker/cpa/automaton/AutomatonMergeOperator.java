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
package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix="cpa.automaton.merge")
public class AutomatonMergeOperator implements MergeOperator {

  private final ControlAutomatonCPA cpa;
  private final AbstractDomain domain;
  private final AutomatonState topState;

  @Option(secure=true, description="Merge two automata states if one of them is TOP.")
  private boolean onTop  = false;

  @Option(secure=true, description="Merge two automata states if one of them is INACTIVE.")
  private boolean onInactive  = false;

  public AutomatonMergeOperator(Configuration pConfig,
      ControlAutomatonCPA pCpa,
      AbstractDomain pAutomatonDomain,
      AutomatonState pTopState)
    throws InvalidConfigurationException {

    pConfig.inject(this);

    this.cpa = pCpa;
    this.domain = pAutomatonDomain;
    this.topState = pTopState;
  }

  @Override
  public AbstractState merge(AbstractState pE1, AbstractState pE2, Precision pP)
    throws CPAException, InterruptedException {

    AutomatonState e1 = (AutomatonState) pE1;
    AutomatonState e2 = (AutomatonState) pE2;

    AutomatonInternalState e1state = e1.getInternalState();
    AutomatonInternalState e2state = e2.getInternalState();

    if (onTop) {
      boolean anyAutomatonTop =
          domain.isLessOrEqual(topState, e1)
          || domain.isLessOrEqual(topState, e2);

      if (anyAutomatonTop) {
        return new AutomatonState.TOP(cpa);
      }
    }

    if (onInactive) {
      boolean anyAutomatonInactive =
          e1state.equals(AutomatonInternalState.INACTIVE)
          || e2state.equals(AutomatonInternalState.INACTIVE);

      if (anyAutomatonInactive) {
        return new AutomatonState.INACTIVE(cpa);
      }
    }

    // No merge (MERGE-SEP)
    return e2;
  }

}
