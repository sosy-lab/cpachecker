// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

/**
 * A component state of a composite state that asks the composite analysis to restart other
 * components from their initial state at the current location, e.g., to forget everything except an
 * abstraction.
 */
public interface ComponentResettingState extends AbstractState {

  /** Whether the component of the given CPA restarts from its initial state. */
  boolean resets(ConfigurableProgramAnalysis pCpa);
}
