// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGExpressionEvaluator;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SMGType {
  private int size;
  private boolean isSigned;
  private int originSize;
  private boolean isOriginSigned;

  private SMGType(int pSize, boolean pIsSigned, int pOriginSize, boolean pIsOriginSigned) {
    size = pSize;
    isSigned = pIsSigned;
    originSize = pOriginSize;
    isOriginSigned = pIsOriginSigned;
  }

  public SMGType(int pSize, boolean pIsSigneds) {
    this(pSize, pIsSigneds, pSize, pIsSigneds);
  }

  public SMGType(SMGType pType, SMGType pOriginType) {
    this(pType.getSize(), pType.isSigned(), pOriginType.getOriginSize(), pOriginType.isSigned());
  }

  public static SMGType constructSMGType(
      CType leftSideType,
      SMGState newState,
      CFAEdge edge,
      SMGExpressionEvaluator smgExpressionEvaluator)
      throws UnrecognizedCodeException {
    boolean isSignedLeft = false;
    if (leftSideType instanceof CSimpleType) {
      isSignedLeft = newState.getHeap().getMachineModel().isSigned((CSimpleType) leftSideType);
    }
    int leftSideTypeSize = smgExpressionEvaluator.getBitSizeof(edge, leftSideType, newState);
    return new SMGType(leftSideTypeSize, isSignedLeft);
  }

  public int getSize() {
    return size;
  }

  public boolean isSigned() {
    return isSigned;
  }

  public int getOriginSize() {
    return originSize;
  }

  public boolean getOriginSigned() {
    return isOriginSigned;
  }
}
