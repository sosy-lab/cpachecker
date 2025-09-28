// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector;


import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

public class LastSparseBitVector {

  public final CIdExpression variable;

  LastSparseBitVector(CIdExpression pVariable) {
    variable = pVariable;
  }
}
