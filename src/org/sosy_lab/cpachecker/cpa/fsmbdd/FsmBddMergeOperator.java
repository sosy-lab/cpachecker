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
package org.sosy_lab.cpachecker.cpa.fsmbdd;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Merge-Operator of the FsmBddCPA.
 * Merge is done by joining the abstract states;
 * this is done by constructing the disjunction (OR) of their BDDs.
 */
@Options(prefix="cpa.fsmbdd")
public class FsmBddMergeOperator implements MergeOperator {

  @Option(description="Merge only on abstraction locations.")
  public boolean mergeOnlyOnEqualConditions = true;

  private final FsmBddStatistics statistics;

  public FsmBddMergeOperator(Configuration pConfig, FsmBddStatistics pStatistics) throws InvalidConfigurationException
  {
    this.statistics = pStatistics;
    if (pConfig != null) {
      pConfig.inject(this);
    }
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision) throws CPAException {
    FsmBddState state1 = (FsmBddState) pState1;
    FsmBddState state2 = (FsmBddState) pState2;
    FsmBddState result = state2;

    if (mergeOnlyOnEqualConditions
        && !state1.condBlockEqualToBlockOf(state2)) {
      return result;
    }

//    System.out.printf("M %d <> %d ?\n", state1.getCfaNode().getNodeNumber(), state2.getCfaNode().getNodeNumber());
//    System.out.println(state1);
//    System.out.println(state2);

    if (state1.getStateBdd().equals(state2.getStateBdd())) {
      result = state1.cloneState(state1.getCfaNode());
      if (!state1.condBlockEqualToBlockOf(state2)) {
        result.addToConditionBlock(state2.getConditionBlock(), false);
      }
      statistics.mergesBecauseEqualBdd++;
    } else if (state1.condBlockEqualToBlockOf(state2)) {
        if (state1.getConditionBlock() == null) {
          statistics.mergesBecauseBothEmptySyntax++;
        } else {
          statistics.mergesBecauseEqualSyntax++;
        }

        // Create the joined state by
        // constructing a disjunction (OR) of the BDDs of the given states.
        result = state1.cloneState(state1.getCfaNode());
        result.disjunctStateBddWith(state2);
    }

    if (result.getStateBdd().equals(state2.getStateBdd())
        && result.condBlockEqualToBlockOf(state2)) {
      result = state2;
    }

    if (result != state2) {
      state1.setMergedInto(result);
    }

    return result;
  }


}
