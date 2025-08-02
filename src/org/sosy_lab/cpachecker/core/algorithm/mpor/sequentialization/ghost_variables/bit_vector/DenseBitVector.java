// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class DenseBitVector {

  public final MPORThread thread;

  /** The bit vector for the next statement. */
  public final Optional<CIdExpression> directVariable;

  /** The bit vector for all reachable statements, relative to a location. */
  public final Optional<CIdExpression> reachableVariable;

  public final BitVectorAccessType accessType;

  public final BitVectorEncoding encoding;

  public DenseBitVector(
      MPORThread pThread,
      // note that both direct and reachable can be empty, when there are no global variables
      Optional<CIdExpression> pDirectVariable,
      Optional<CIdExpression> pReachableVariable,
      BitVectorAccessType pAccessType,
      BitVectorEncoding pEncoding) {

    checkArgument(pEncoding.isDense, "encoding must be dense");
    checkArgument(
        !pAccessType.equals(BitVectorAccessType.READ) || pReachableVariable.isEmpty(),
        "for access type READ, the reachable variable must be empty");
    checkArgument(
        !pAccessType.equals(BitVectorAccessType.ACCESS) || pDirectVariable.isEmpty(),
        "for access type ACCESS, the direct variable must be empty");

    thread = pThread;
    directVariable = pDirectVariable;
    reachableVariable = pReachableVariable;
    accessType = pAccessType;
    encoding = pEncoding;
  }
}
