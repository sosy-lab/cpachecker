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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.core.defaults.NoOpReducer;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface ConfigurableProgramAnalysisWithBAM extends
    ConfigurableProgramAnalysis {

  /**
   * Get the reduce operator for BAM.
   *
   * @see Reducer
   * @throws InvalidConfigurationException can be thrown from subclasses
   */
  default Reducer getReducer() throws InvalidConfigurationException {
    return NoOpReducer.getInstance();
  }

  /**
   * Set the block partitioning on the CPA.
   *
   * @param pPartitioning Partitioning used for BAM.
   */
  default void setPartitioning(BlockPartitioning pPartitioning) { }

  /**
   * Check coverage for two abstract state, based on {@link AbstractDomain#isLessOrEqual}.
   *
   * <p>Most CPAs will not need to override this method. It is needed for {@link CallstackCPA},
   * because we have to check the content of the {@link CallstackState} and not its object-identity.
   */
  default boolean isCoveredByRecursiveState(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    return getAbstractDomain().isLessOrEqual(state1, state2);
  }
}
