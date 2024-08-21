// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.abe;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Abstracted state of ABE-based analysis.
 *
 * @param <A> Should be parameterized by its own class, e.g. {@code class CongruenceClass implements
 *     ABEAbstractedState<CongruenceClass>}.
 */
public interface ABEAbstractedState<A extends ABEAbstractedState<A>>
    extends ABEState<A>, FormulaReportingState {

  @Override
  default boolean isAbstract() {
    return true;
  }

  /** Returns {@link SSAMap} at which the new formula should start. */
  SSAMap getSSAMap();

  /** Returns {@link PointerTargetSet} at which the new formula should start. */
  PointerTargetSet getPointerTargetSet();

  /** Returns intermediate state which was used to generate {@code this} state. */
  Optional<ABEIntermediateState<A>> getGeneratingState();

  /** Returns syntactic sugar: type casting. */
  A cast();

  /**
   * Returns instantiated constraint represented by this state. May include additional stored
   * invariants derived from other CPAs.
   */
  BooleanFormula instantiate();
}
