/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.slicing;

import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;

/**
 * {@link AbstractState} of the {@link SlicingCPA}. Does not store any information itself, but only
 * wraps the abstract state of the CPA wrapped by the slicing CPA.
 */
public class SlicingState extends AbstractSingleWrapperState implements Graphable {

  public static final long serialVersionUID = 0;

  public SlicingState(AbstractState pWrappedState) {
    super(pWrappedState);
  }

  @Override
  public String toDOTLabel() {
    AbstractState wrappedState = getWrappedState();
    if (wrappedState instanceof Graphable) {
      return ((Graphable) wrappedState).toDOTLabel();
    } else {
      return "";
    }
  }

  @Override
  public boolean shouldBeHighlighted() {
    AbstractState wrappedState = getWrappedState();
    if (wrappedState instanceof Graphable) {
      return ((Graphable) wrappedState).shouldBeHighlighted();
    } else {
      return false;
    }
  }
}
