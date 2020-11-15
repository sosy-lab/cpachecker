// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.states;

import org.checkerframework.checker.nullness.qual.Nullable;

public class PointerToMemoryLocation extends MemoryLocation {

  private static final long serialVersionUID = -8910967707373729035L;

  private PointerToMemoryLocation(String pIdentifier, @Nullable Long pOffset) {
    super(pIdentifier, pOffset);
  }

  public static PointerToMemoryLocation valueOf(String pIdentifier) {
      return new PointerToMemoryLocation(pIdentifier, null);
  }
}