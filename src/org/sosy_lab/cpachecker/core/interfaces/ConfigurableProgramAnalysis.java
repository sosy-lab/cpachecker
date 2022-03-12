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
