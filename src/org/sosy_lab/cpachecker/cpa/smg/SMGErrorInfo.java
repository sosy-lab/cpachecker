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

import java.util.Objects;
import org.sosy_lab.cpachecker.cpa.smg.SMGState.Property;

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

  private SMGErrorInfo(
      boolean pInvalidWrite,
      boolean pInvalidRead,
      boolean pInvalidFree,
      boolean pHasMemoryLeak,
      String pErrorDescription) {
    invalidWrite = pInvalidWrite;
    invalidRead = pInvalidRead;
    invalidFree = pInvalidFree;
    hasMemoryLeak = pHasMemoryLeak;
    errorDescription = pErrorDescription;
  }

  static SMGErrorInfo of() {
    return new SMGErrorInfo(false, false, false, false, "");
  }

  SMGErrorInfo withErrorMessage(String pErrorDescription) {
    return new SMGErrorInfo(
        invalidWrite, invalidRead, invalidFree, hasMemoryLeak, pErrorDescription);
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

    return new SMGErrorInfo(pInvalidWrite, pInvalidRead, pInvalidFree, pHasLeaks, errorDescription);
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
        errorDescription);
  }

  SMGErrorInfo withClearChain() {
    return new SMGErrorInfo(
        invalidWrite, invalidRead, invalidFree, hasMemoryLeak, errorDescription);
  }

  SMGErrorInfo moveCurrentChainToInvalidChain() {
    return new SMGErrorInfo(
        invalidWrite, invalidRead, invalidFree, hasMemoryLeak, errorDescription);
  }

  @Override
  public int hashCode() {
    return Objects.hash(invalidWrite, invalidRead, invalidFree, hasMemoryLeak);
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
        && hasMemoryLeak == o.hasMemoryLeak;
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
}
