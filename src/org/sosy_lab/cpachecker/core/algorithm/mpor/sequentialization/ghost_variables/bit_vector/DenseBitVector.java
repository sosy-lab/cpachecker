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
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class DenseBitVector {

  public final MPORThread thread;

  /** The bit vector for the next statement. */
  public final CIdExpression directVariable;

  /** The bit vector for all reachable statements, from this location. */
  public final CIdExpression reachableVariable;

  public final BitVectorAccessType accessType;

  public final BitVectorEncoding encoding;

  public DenseBitVector(
      MPORThread pThread,
      CIdExpression pDirectVariable,
      CIdExpression pReachableVariable,
      BitVectorAccessType pAccessType,
      BitVectorEncoding pEncoding) {

    checkArgument(pEncoding.isDense, "encoding must be dense");
    thread = pThread;
    directVariable = pDirectVariable;
    reachableVariable = pReachableVariable;
    accessType = pAccessType;
    encoding = pEncoding;
  }
}
