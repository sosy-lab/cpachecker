/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

package org.sosy_lab.cpachecker.util;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;

import com.google.errorprone.annotations.ForOverride;

/**
 * Utility class to visit all wrapped abstract states
 * (including the wrapper states)
 */
public abstract class AbstractWrappedStateVisitor {

  /**
   * Operation to apply on an state when it is visited
   */
  @ForOverride
  protected abstract void process(AbstractState state);

  /**
   * Visit a given abstract state and all its sub-state
   */
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
