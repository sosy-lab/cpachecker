// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.location;

public class PointerLocationComparator {
  /** Compares two PointerTarget objects of different types by their class names. */
  public static int compareByType(PointerLocation a, PointerLocation b) {
    return a.getClass().getName().compareTo(b.getClass().getName());
  }
}
