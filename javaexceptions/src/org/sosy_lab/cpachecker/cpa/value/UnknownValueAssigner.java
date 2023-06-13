// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.states.MemoryLocationValueHandler;

/**
 * Memory location value handler that always removes the given memory location from the given state.
 */
public final class UnknownValueAssigner implements MemoryLocationValueHandler {

  /**
   * Remove the given {@link MemoryLocation} from the given {@link ValueAnalysisState}.
   *
   * @param pMemLocation the memory location to remove
   * @param pType the type of the memory location that should be removed
   * @param pState the {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState} to use. Value
   *     assignments will happen in this state
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
