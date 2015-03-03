/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.callstack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

import com.google.common.base.Objects;

@Options(prefix = "cpa.callstack")
public class CallstackStateFactory {
  @Option(secure = true, description = "Compare CallstackStates using the identity function,"
      + " that is two states would be equal only if they are the same object"
      + " and not if their call stack contains the same list of functions."
      + " This prevents merging of abstract states inside a called functions"
      + " if they are successors of different abstract states in the caller function.")
  private boolean produceUniqueStates=true;

  public CallstackStateFactory(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  public CallstackState create(@Nullable CallstackState previousElement,
      @Nonnull String function,
      @Nonnull CFANode callerNode) {
    if (produceUniqueStates) {
      return new UniqueCallstackState(previousElement, function, callerNode);
    } else {
      return new ComparableCallstackState(previousElement, function, callerNode);
    }
  }

  /**
   * Callstack State with an {@code equals} implementation which compares the contained
   * attributes.
   */
  private static class ComparableCallstackState extends CallstackState {
    private static final long serialVersionUID = 7294556997204151804L;

    ComparableCallstackState(CallstackState previousElement, @Nonnull String function,
        @Nonnull CFANode callerNode) {
      super(previousElement, function, callerNode);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof CallstackState)) {
        return false;
      }
      CallstackState other = (CallstackState)o;
      return (other.previousState == previousState
          || other.previousState.equals(previousState)) &&
          other.currentFunction.equals(currentFunction) &&
          callerNode.equals(other.callerNode);
    }

    @Override
    public int hashCode() {
      if (hashCache == 0) {
        hashCache = Objects.hashCode(previousState, currentFunction, callerNode);
      }
      return hashCache;
    }

  }
  public class UniqueCallstackState extends CallstackState {

    private static final long serialVersionUID = -9178322430015798219L;

    UniqueCallstackState(CallstackState previousElement, @Nonnull String function,
        @Nonnull CFANode callerNode) {
      super(previousElement, function, callerNode);
    }
  }
}
