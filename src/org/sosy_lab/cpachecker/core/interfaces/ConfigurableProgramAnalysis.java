// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;

/**
 * Interface for classes representing a Configurable Program Analysis.
 *
 * <p>All instances of this class have to have a public static method "factory()" which takes no
 * arguments, returns an instance of {@link CPAFactory} and never fails that is, it never returns
 * null or throws an exception).
 */
public interface ConfigurableProgramAnalysis {
  AbstractDomain getAbstractDomain();

  TransferRelation getTransferRelation();

  MergeOperator getMergeOperator();

  StopOperator getStopOperator();

  /**
   * Returns the precision adjustment operator {@link PrecisionAdjustment} that may adjust the
   * current abstractState and precision using information from the current set of reached states.
   */
  default PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  /**
   * Returns the initial abstract state for the given program location.
   *
   * <p>The returned state should represent all concrete program states that are possible at the
   * given location, subject to the bounds and restrictions of the concrete state space and of this
   * CPA. Thus, the initial state is maximally abstract within those constraints, but it is not
   * necessarily the top element of the abstract domain.
   *
   * @param node a {@link CFANode} at the concrete program location for which the initial state
   *     should be created. This initial location may be ignored should the abstract state returned
   *     represent the top element within its abstract domain.
   */
  AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException;

  /**
   * Returns the initial precision for the initial state.
   *
   * @param node location of the initial state/precision
   * @param partition partition of the initial state/precision
   * @throws InterruptedException if interrupted
   */
  default Precision getInitialPrecision(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return SingletonPrecision.getInstance();
  }
}
