// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.abe;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Abstract or intermediate state of ABE-based analysis. Intermediate state (representing formula)
 * is the same for all implementations ({@link ABEIntermediateState}) while abstracted state
 * (representing abstraction) is provided by the interface {@link ABEAbstractedState} and the
 * implementation differs.
 */
public interface ABEState<A extends ABEAbstractedState<A>> extends AbstractState {
  boolean isAbstract();

  CFANode getNode();

  default ABEIntermediateState<A> asIntermediate() {
    return (ABEIntermediateState<A>) this;
  }

  default ABEAbstractedState<A> asAbstracted() {
    return (ABEAbstractedState<A>) this;
  }
}
