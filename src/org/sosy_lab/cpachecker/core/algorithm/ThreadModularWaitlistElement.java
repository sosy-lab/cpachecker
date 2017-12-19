/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WaitlistElement;


public class ThreadModularWaitlistElement implements WaitlistElement {

  private final AbstractState state;
  private final InferenceObject inferenceObject;
  private final Precision precision;

  public ThreadModularWaitlistElement(AbstractState pState, InferenceObject pIO, Precision pPrec) {
    Preconditions.checkNotNull(pState);
    Preconditions.checkNotNull(pIO);
    Preconditions.checkNotNull(pPrec);
    state = pState;
    inferenceObject = pIO;
    precision = pPrec;
  }

  @Override
  public boolean contains(AbstractState pState) {
    return state.equals(pState);
  }

  @Override
  public Collection<AbstractState> getAbstractStates() {
    return Collections.singleton(state);
  }

  public AbstractState getState() {
    return state;
  }

  public InferenceObject getInferenceObject() {
    return inferenceObject;
  }

  public Precision getPrecision() {
    return precision;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Objects.hashCode(inferenceObject);
    result = prime * result + Objects.hashCode(state);
    result = prime * result + Objects.hashCode(precision);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null ||
        getClass() != obj.getClass()) {
      return false;
    }
    ThreadModularWaitlistElement other = (ThreadModularWaitlistElement) obj;
    return Objects.equals(state, other.state)
        && Objects.equals(inferenceObject, other.inferenceObject)
        && Objects.equals(precision, other.precision);
  }
}
