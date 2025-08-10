// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.location;

import com.google.common.base.Preconditions;

public sealed interface PointerLocation extends Comparable<PointerLocation>
    permits HeapLocation, InvalidLocation, DeclaredVariableLocation, NullLocation, StructLocation {
  default boolean isValidFunctionReturn(String callerFunctionName) {
    Preconditions.checkNotNull(callerFunctionName);
    return true;
  }
}
