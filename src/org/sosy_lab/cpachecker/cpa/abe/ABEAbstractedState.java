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

import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.Optional;

/**
 * Abstracted state of ABE-based analysis.
 * @param <A> Should be parameterized by its own class, e.g.
 *           {@code class CongruenceClass implements
 *           ABEAbstractedState<CongruenceClass>}.
 */
public interface ABEAbstractedState<A extends ABEAbstractedState<A>>
    extends ABEState<A>, FormulaReportingState {

  @Override
  default boolean isAbstract() {
    return true;
  }

  /**
   * @return {@link SSAMap} at which the new formula should start.
   */
  SSAMap getSSAMap();

  /**
   * @return {@link PointerTargetSet} at which the new formula should start.
   */
  PointerTargetSet getPointerTargetSet();

  /**
   * @return Intermediate state which was used to generate {@code this} state.
   */
  Optional<ABEIntermediateState<A>> getGeneratingState();

  /**
   * @return Syntactic sugar: type casting.
   */
  A cast();

  /**
   * @return Instantiated constraint represented by this state.
   * May include additional stored invariants derived from other CPAs.
   */
  BooleanFormula instantiate();
}
