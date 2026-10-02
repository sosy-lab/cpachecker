// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * A state of another analysis that decides where the predicate analysis computes an abstraction,
 * instead of its block operator.
 */
public interface AbstractionControllingState extends AbstractState {

  /** Whether to compute an abstraction here, or empty if the block operator decides. */
  Optional<Boolean> shouldComputeAbstraction();

  /**
   * Whether the abstraction has to hold at every visit of the current location since the last
   * abstraction, instead of only at the current one.
   */
  boolean mustCoverEveryVisit();

  /**
   * Whether the block after the abstraction starts with an empty pointer-target set, so that it
   * allocates the memory again, as an analysis that starts at the current location does.
   */
  boolean forgetsPointerTargets();
}
