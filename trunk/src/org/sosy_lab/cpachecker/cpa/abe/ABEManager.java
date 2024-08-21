// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.abe;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/** Interface to be implemented by the subclassing CPA. */
public interface ABEManager<A extends ABEAbstractedState<A>, P extends Precision> {

  A getInitialState(CFANode node, StateSpacePartition pPartition);

  P getInitialPrecision(CFANode node, StateSpacePartition pPartition);

  /** Convert the intermediate state into abstracted one. */
  PrecisionAdjustmentResult performAbstraction(
      ABEIntermediateState<A> pIntermediateState,
      P precision,
      UnmodifiableReachedSet states,
      AbstractState fullState)
      throws CPATransferException, InterruptedException;

  /** Post-abstraction strengthening. */
  Optional<ABEAbstractedState<A>> strengthen(
      ABEAbstractedState<A> pState, P pPrecision, Iterable<AbstractState> pOtherStates);

  /** Coverage checking on abstracted states. */
  boolean isLessOrEqual(ABEAbstractedState<A> pState1, ABEAbstractedState<A> pState2);
}
