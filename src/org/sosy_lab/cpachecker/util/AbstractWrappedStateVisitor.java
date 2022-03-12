// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.errorprone.annotations.ForOverride;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;

/** Utility class to visit all wrapped abstract states (including the wrapper states) */
public abstract class AbstractWrappedStateVisitor {

  /** Operation to apply on an state when it is visited */
  @ForOverride
  protected abstract void process(AbstractState state);

  /** Visit a given abstract state and all its sub-state */
  public final void visit(AbstractState state) {
    process(state);

    if (state instanceof AbstractWrapperState) {
      AbstractWrapperState wrapperState = (AbstractWrapperState) state;
      for (AbstractState wrappedState : wrapperState.getWrappedStates()) {
        visit(wrappedState);
      }
    }
  }
}
