// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

/**
 * This interface represents abstract states that somehow store information about concurrent
 * threads. The interface is intended to provide information about the active threads in the
 * current state.
 */
public interface AbstractStateWithThreads extends AbstractState {

  /**
   * Get the number of the active threads in this state.
   *
   * @return A node of the CFA.
   */
  int getNumberOfActiveThreads();
}
