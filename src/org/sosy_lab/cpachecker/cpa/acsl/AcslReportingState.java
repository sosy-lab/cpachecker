// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Interface to implement in order for an abstract state to be able to be over-approximated by an
 * Acsl predicate representing the abstract state.
 */
public interface AcslReportingState extends AbstractState {

  /** Returns an Acsl predicate over-approximating the state. */
  AcslPredicate getAcslPredicate();
}
