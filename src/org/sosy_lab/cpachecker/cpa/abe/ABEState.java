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

/**
 * Abstract or intermediate state of ABE-based analysis.
 * Intermediate state (representing formula) is the same for all implementations
 * ({@link ABEIntermediateState}) while abstracted state (representing
 * abstraction)
 * is provided by the interface {@link ABEAbstractedState}
 * and the implementation differs.
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
