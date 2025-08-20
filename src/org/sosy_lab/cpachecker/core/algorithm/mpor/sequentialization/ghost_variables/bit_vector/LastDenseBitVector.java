// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

public class LastDenseBitVector {

  /** The bit vector for all reachable statements, relative to a location. */
  public final CIdExpression reachableVariable;

  public final BitVectorAccessType accessType;

  public final BitVectorEncoding encoding;

  public LastDenseBitVector(
      CIdExpression pReachableVariable,
      BitVectorAccessType pAccessType,
      BitVectorEncoding pEncoding) {

    // TODO why parametrize?
    checkArgument(pEncoding.isDense, "encoding must be dense");

    reachableVariable = pReachableVariable;
    accessType = pAccessType;
    encoding = pEncoding;
  }
}
