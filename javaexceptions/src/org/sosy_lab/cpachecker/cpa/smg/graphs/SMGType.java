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

/** Class for representation of casting values to different types for SMG predicate relations */
public class SMGType {
  private final long castedSize;
  private final boolean castedSigned;
  private final long originSize;
  private final boolean originSigned;

  private SMGType(
      long pCastedSize, boolean pCastedSigned, long pOriginSize, boolean pOriginSigned) {
    castedSize = pCastedSize;
    castedSigned = pCastedSigned;
    originSize = pOriginSize;
    originSigned = pOriginSigned;
  }

  public SMGType(long pCastedSize, boolean pSigned) {
    this(pCastedSize, pSigned, pCastedSize, pSigned);
  }

  public SMGType(SMGType pCastedType, SMGType pOriginType) {
    this(
        pCastedType.getCastedSize(),
        pCastedType.isCastedSigned(),
        pOriginType.getOriginSize(),
        pOriginType.isCastedSigned());
  }

  public static SMGType constructSMGType(
      CType pType, SMGState pState, CFAEdge pEdge, SMGExpressionEvaluator smgExpressionEvaluator)
      throws UnrecognizedCodeException {
    boolean isSigned = false;
    if (pType instanceof CSimpleType) {
      isSigned = pState.getHeap().getMachineModel().isSigned((CSimpleType) pType);
    }
    long size = smgExpressionEvaluator.getBitSizeof(pEdge, pType, pState);
    return new SMGType(size, isSigned);
  }

  public long getCastedSize() {
    return castedSize;
  }

  public boolean isCastedSigned() {
    return castedSigned;
  }

  public long getOriginSize() {
    return originSize;
  }

  public boolean isOriginSigned() {
    return originSigned;
  }

  @Override
  public String toString() {
    return String.format(
        "CAST from '%ssigned %d bit' to '%ssigned %d bit'",
        originSigned ? "" : "un", originSize, castedSigned ? "" : "un", castedSize);
  }
}
