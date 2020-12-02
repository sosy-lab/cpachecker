// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

public interface AbstractWrapperState extends AbstractState {

  /**
   * Retrieve all wrapped abstract states contained directly in this object.
   *
   * @return A non-empty list of abstract states.
   */
  Iterable<AbstractState> getWrappedStates();
}
