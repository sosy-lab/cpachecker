/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.common.collect.PersistentLinkedList;
import org.sosy_lab.common.collect.PersistentList;
import org.sosy_lab.cpachecker.cpa.smg.SMGState.Property;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

/**
 * Simple flags are not enough, this class contains more about the nature of the error.
 *
 * <p>This class is immutable.
 */
class SMGErrorInfo {

  private final boolean invalidWrite;
  private final boolean invalidRead;
  private final boolean invalidFree;
  private final boolean hasMemoryLeak;
  private final String errorDescription;
  private final PersistentList<Object> invalidChain;
  private final PersistentList<Object> currentChain;

  private SMGErrorInfo(
      boolean pInvalidWrite,
      boolean pInvalidRead,
      boolean pInvalidFree,
      boolean pHasMemoryLeak,
      String pErrorDescription,
      PersistentList<Object> pInvalidChain,
      PersistentList<Object> pCurrentChain) {
    invalidWrite = pInvalidWrite;
    invalidRead = pInvalidRead;
    invalidFree = pInvalidFree;
    hasMemoryLeak = pHasMemoryLeak;
    errorDescription = pErrorDescription;
    invalidChain = pInvalidChain;
    currentChain = pCurrentChain;
  }

  static SMGErrorInfo of() {
    return new SMGErrorInfo(
        false, false, false, false, "", PersistentLinkedList.of(), PersistentLinkedList.of());
  }

  SMGErrorInfo withErrorMessage(String pErrorDescription) {
    return new SMGErrorInfo(
        invalidWrite,
        invalidRead,
        invalidFree,
        hasMemoryLeak,
        pErrorDescription,
        invalidChain,
        currentChain);
  }

  SMGErrorInfo withProperty(Property pProperty) {
    boolean pInvalidWrite = invalidWrite;
    boolean pInvalidRead = invalidRead;
    boolean pInvalidFree = invalidFree;
    boolean pHasLeaks = hasMemoryLeak;

    switch (pProperty) {
      case INVALID_WRITE:
        pInvalidWrite = true;
        break;
      case INVALID_READ:
        pInvalidRead = true;
        break;
      case INVALID_FREE:
        pInvalidFree = true;
        break;
      case INVALID_HEAP:
        pHasLeaks = true;
        break;
      default:
        throw new AssertionError();
    }

    return new SMGErrorInfo(
        pInvalidWrite,
        pInvalidRead,
        pInvalidFree,
        pHasLeaks,
        errorDescription,
        invalidChain,
        currentChain);
  }

  boolean hasMemoryErrors() {
    return invalidWrite || invalidRead || invalidFree || hasMemoryLeak;
  }

  SMGErrorInfo mergeWith(SMGErrorInfo pOther) {
    return new SMGErrorInfo(
        invalidWrite || pOther.invalidWrite,
        invalidRead || pOther.invalidRead,
        invalidFree || pOther.invalidFree,
        hasMemoryLeak || pOther.hasMemoryLeak,
        errorDescription,
        invalidChain,
        currentChain);
  }

  SMGErrorInfo withClearChain() {
    return new SMGErrorInfo(
        invalidWrite,
        invalidRead,
        invalidFree,
        hasMemoryLeak,
        errorDescription,
        PersistentLinkedList.of(),
        PersistentLinkedList.of());
  }

  SMGErrorInfo moveCurrentChainToInvalidChain() {
    return new SMGErrorInfo(
        invalidWrite,
        invalidRead,
        invalidFree,
        hasMemoryLeak,
        errorDescription,
        invalidChain.withAll(currentChain),
        currentChain);
  }

  public SMGErrorInfo withObject(Object o) {
    return new SMGErrorInfo(
        invalidWrite,
        invalidRead,
        invalidFree,
        hasMemoryLeak,
        errorDescription,
        invalidChain,
        currentChain.with(o));
  }

  SMGErrorInfo withInvalidObject(SMGObject pSmgObject) {
    return withInvalidObjects(Collections.singleton(pSmgObject));
  }

  public SMGErrorInfo withInvalidObjects(Collection<?> pObjects) {
    return new SMGErrorInfo(
        invalidWrite,
        invalidRead,
        invalidFree,
        hasMemoryLeak,
        errorDescription,
        invalidChain.withAll(new ArrayList<>(pObjects)),
        currentChain);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        invalidWrite, invalidRead, invalidFree, hasMemoryLeak, invalidChain, currentChain);
  }

  @Override
  public boolean equals(Object pOther) {
    if (!(pOther instanceof SMGErrorInfo)) {
      return false;
    }
    SMGErrorInfo o = (SMGErrorInfo) pOther;
    return invalidWrite == o.invalidWrite
        && invalidRead == o.invalidRead
        && invalidFree == o.invalidFree
        && hasMemoryLeak == o.hasMemoryLeak
        && invalidChain.equals(o.invalidChain)
        && currentChain.equals(o.currentChain);
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder("ErrorInfo {");
    str.append(errorDescription.isEmpty() ? "<>" : errorDescription).append(", ");
    if (invalidWrite) {
      str.append("invalid write").append(", ");
    }
    if (invalidRead) {
      str.append("invalid read").append(", ");
    }
    if (isInvalidFree()) {
      str.append("invalid free").append(", ");
    }
    if (hasMemoryLeak()) {
      str.append("has memory leak").append(", ");
    }
    return str.append("}").toString();
  }

  String getErrorDescription() {
    return errorDescription;
  }

  boolean isInvalidWrite() {
    return invalidWrite;
  }

  boolean isInvalidRead() {
    return invalidRead;
  }

  boolean isInvalidFree() {
    return invalidFree;
  }

  boolean hasMemoryLeak() {
    return hasMemoryLeak;
  }

  List<Object> getCurrentChain() {
    return currentChain;
  }

  List<Object> getInvalidChain() {
    return invalidChain;
  }
}
