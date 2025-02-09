// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

@SuppressWarnings("unused")
public class SMGObjectMergeTriple {

  String variableName;
  boolean isGlobal;
  boolean isExternallyAllocated;

  SMGObject leftVariableObject;
  SMGObject rightVariableObject;
  SMGObject mergeObject;

  private SMGObjectMergeTriple(
      String pVariableName,
      SMGObject pLeft,
      SMGObject pRight,
      SMGObject pMergedObject,
      boolean pIsGlobal,
      boolean pIsExternallyAllocated) {
    checkNotNull(pVariableName);
    checkNotNull(pLeft);
    checkNotNull(pRight);
    checkNotNull(pMergedObject);
    variableName = pVariableName;
    leftVariableObject = pLeft;
    rightVariableObject = pRight;
    mergeObject = pMergedObject;
    isGlobal = pIsGlobal;
    isExternallyAllocated = pIsExternallyAllocated;
  }

  public static SMGObjectMergeTriple of(
      String pVariableName,
      SMGObject pLeft,
      SMGObject pRight,
      SMGObject pMergedObject,
      boolean pIsGlobal,
      boolean pIsExternallyAllocated) {
    return new SMGObjectMergeTriple(
        pVariableName, pLeft, pRight, pMergedObject, pIsGlobal, pIsExternallyAllocated);
  }

  public SMGObject getLeftVariableObject() {
    return leftVariableObject;
  }

  public SMGObject getRightVariableObject() {
    return rightVariableObject;
  }

  public SMGObject getMergeObject() {
    return mergeObject;
  }

  public String getVariableName() {
    return variableName;
  }

  public boolean isGlobal() {
    return isGlobal;
  }

  public boolean isExternallyAllocated() {
    return isExternallyAllocated;
  }
}
