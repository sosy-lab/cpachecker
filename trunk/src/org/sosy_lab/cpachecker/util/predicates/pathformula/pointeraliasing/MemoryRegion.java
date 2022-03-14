// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

/** A memory region for pointer analysis */
public interface MemoryRegion {
  /**
   * Returns the type of memory region. The type is used for historical reasons and may be
   * deprecated in the future.
   *
   * @return the type of memory region
   */
  CType getType();
  /**
   * The function returns identifier of the region used for the name of uninterpreted functions.
   * Usually, the name contains a string representation of the type as substring.
   *
   * @return identifier of the region
   */
  String getName(TypeHandlerWithPointerAliasing typeHandler);
}
