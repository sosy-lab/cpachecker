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
package org.sosy_lab.cpachecker.cpa.value;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.states.MemoryLocationValueHandler;

/**
 * Memory location value handler that always removes the given memory location
 * from the given state.
 */
public final class UnknownValueAssigner implements MemoryLocationValueHandler {

  /**
   * Remove the given {@link MemoryLocation} from the given {@link ValueAnalysisState}.
   *
   * @param pMemLocation the memory location to remove
   * @param pType the type of the memory location that should be removed
   * @param pState the {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState} to use.
   *    Value assignments will happen in this state
   * @param pValueVisitor unused, may be null
   */
  @Override
  public void handle(
      MemoryLocation pMemLocation,
      Type pType,
      ValueAnalysisState pState,
      @Nullable ExpressionValueVisitor pValueVisitor) {
    pState.forget(pMemLocation);
  }
}
