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

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;

/**
 * Interface for classes representing a Configurable Program Analysis.
 *
 * All instances of this class have to have a public static method "factory()"
 * which takes no arguments, returns an instance of {@link CPAFactory} and never
 * fails that is, it never returns null or throws an exception).
 */
public interface ConfigurableProgramAnalysis {
  public AbstractDomain getAbstractDomain();
  public TransferRelation getTransferRelation();
  public MergeOperator getMergeOperator();
  public StopOperator getStopOperator();

  /**
   * Returns the precision adjustment operator {@link PrecisionAdjustment} that may adjust the
   * current abstractState and precision using information from the current set of reached states.
   */
  default PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  public AbstractState getInitialState(CFANode node, StateSpacePartition partition) throws InterruptedException;

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
