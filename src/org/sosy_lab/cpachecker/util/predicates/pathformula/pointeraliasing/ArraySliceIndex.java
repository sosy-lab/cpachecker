// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;

/**
 * A helper class for indexing by quantified variables, standing for an index {@code i} which can
 * take values from {@code 0 <= i < size}. Indexing the left-hand side and right-hand side by the
 * same index will result in them sharing the same quantifier (or quantifier replacement).
 */
class ArraySliceIndex {
  private final CExpression size;

  ArraySliceIndex(CExpression pSize) {
    size = pSize;
  }

  CExpression getSize() {
    return size;
  }
}
