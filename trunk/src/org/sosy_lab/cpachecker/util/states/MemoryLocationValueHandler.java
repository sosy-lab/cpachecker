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
package org.sosy_lab.cpachecker.util.states;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Classes implementing this interface provide means for altering the value of
 * {@link MemoryLocation}s in a specific way.
 */
public interface MemoryLocationValueHandler {

  /**
   * Handles the value of the given memory location in the given state in an
   * implementation-dependent way. A call to this method could assign a new value to the given
   * memory location or even remove it from the state.
   *
   * @param pMemLocation the memory location to alter
   * @param pType the type of the variable at the given memory location
   * @param pState the {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState} to use.
   *    Value assignments will happen in this state
   * @param pValueVisitor a value visitor for possibly needed evaluations or computations
   * @throws UnrecognizedCCodeException thrown if the given parameters do not fit.
   *    Other causes for this exception may be implementation-dependent
   */
  void handle(MemoryLocation pMemLocation, Type pType,
      ValueAnalysisState pState, ExpressionValueVisitor pValueVisitor)
      throws UnrecognizedCCodeException;
}
