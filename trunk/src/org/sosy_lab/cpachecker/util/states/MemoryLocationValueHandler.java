// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.states;

import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.value.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Classes implementing this interface provide means for altering the value of {@link
 * MemoryLocation}s in a specific way.
 */
public interface MemoryLocationValueHandler {

  /**
   * Handles the value of the given memory location in the given state in an
   * implementation-dependent way. A call to this method could assign a new value to the given
   * memory location or even remove it from the state.
   *
   * @param pMemLocation the memory location to alter
   * @param pType the type of the variable at the given memory location
   * @param pState the {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState} to use. Value
   *     assignments will happen in this state
   * @param pValueVisitor a value visitor for possibly needed evaluations or computations
   * @throws UnrecognizedCodeException thrown if the given parameters do not fit. Other causes for
   *     this exception may be implementation-dependent
   */
  void handle(
      MemoryLocation pMemLocation,
      Type pType,
      ValueAnalysisState pState,
      ExpressionValueVisitor pValueVisitor)
      throws UnrecognizedCodeException;
}
