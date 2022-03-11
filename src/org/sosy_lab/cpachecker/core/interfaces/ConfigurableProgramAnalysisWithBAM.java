// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.core.defaults.NoOpReducer;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface ConfigurableProgramAnalysisWithBAM extends ConfigurableProgramAnalysis {

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
  default void setPartitioning(BlockPartitioning pPartitioning) {}

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
