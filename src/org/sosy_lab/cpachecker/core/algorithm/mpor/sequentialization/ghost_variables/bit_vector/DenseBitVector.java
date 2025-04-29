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

  public final CIdExpression idExpression;

  public final BitVectorAccessType accessType;

  public final BitVectorEncoding encoding;

  public DenseBitVector(
      MPORThread pThread,
      CIdExpression pIdExpression,
      BitVectorAccessType pAccessType,
      BitVectorEncoding pEncoding) {

    checkArgument(pEncoding.isDense, "encoding must be dense");
    thread = pThread;
    idExpression = pIdExpression;
    accessType = pAccessType;
    encoding = pEncoding;
  }
}
