/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.threadmodular;

import java.util.Objects;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.defaults.EpsilonState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;

public class ThreadModularState extends AbstractSingleWrapperState {
  private static final long serialVersionUID = -178500066693874215L;
  private final InferenceObject inferenceObject;

  public ThreadModularState(AbstractState pWrappedState, InferenceObject pIo) {
    super(pWrappedState);
    inferenceObject = pIo;
  }

  @Override
  public Object getPartitionKey() {
    if (getWrappedState() == EpsilonState.getInstance()) {
      //EpsilonState
      return EpsilonState.getInstance();
    } else {
      return super.getPartitionKey();
    }
  }

  @Override
  public Comparable<?> getPseudoPartitionKey() {
    return super.getPseudoPartitionKey();
  }

  @Override
  public Object getPseudoHashCode() {
    return super.getPseudoHashCode();
  }

  @Override
  public String toString() {
    return super.toString() + " ";
  }

  public InferenceObject getInferenceObject() {
    return inferenceObject;
  }

  @Override
  public int hashCode() {
    return Objects.hash(inferenceObject, getWrappedState());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ThreadModularState other = (ThreadModularState) obj;
    return Objects.equals(inferenceObject, other.inferenceObject)
        && getWrappedState().equals(other.getWrappedState());
  }
}
