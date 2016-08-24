/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.abe;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.List;
import java.util.Optional;

/**
 * Interface to be implemented by the subclassing CPA.
 */
public interface ABEManager
    <
        A extends ABEAbstractedState<A>,
        P extends Precision
    > {

  A getInitialState(CFANode node, StateSpacePartition pPartition);
  P getInitialPrecision(CFANode node, StateSpacePartition pPartition);

  /**
   * Convert the intermediate state into abstracted one.
   */
  PrecisionAdjustmentResult performAbstraction(
    ABEIntermediateState<A> pIntermediateState,
    P precision,
    UnmodifiableReachedSet states,
    AbstractState fullState
  ) throws CPATransferException, InterruptedException;

  /**
   * Post-abstraction strengthening.
   */
  Optional<ABEAbstractedState<A>> strengthen(
      ABEAbstractedState<A> pState,
      P pPrecision,
      List<AbstractState> pOtherStates
  );

  /**
   * Coverage checking on abstracted states.
   */
  boolean isLessOrEqual(ABEAbstractedState<A> pState1, ABEAbstractedState<A> pState2);
}
