/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPath;

public abstract class SMGAbstractListCandidateSequenceBlock<S> implements SMGAbstractionBlock {

  private final S shape;
  private final int length;
  private final SMGMemoryPath pointerToStartObject;

  public SMGAbstractListCandidateSequenceBlock(S pShape, int pLength,
      SMGMemoryPath pPointerToStartObject) {
    shape = pShape;
    length = pLength;
    pointerToStartObject = pPointerToStartObject;
  }

  @Override
  public boolean isBlocked(SMGAbstractionCandidate pCandidate, UnmodifiableCLangSMG smg) {

    if (!(pCandidate instanceof SMGAbstractListCandidateSequence<?>)) {
      return false;
    }

    SMGAbstractListCandidateSequence<?> lcs = (SMGAbstractListCandidateSequence<?>) pCandidate;
    if (!shape.equals(lcs.getCandidate().getShape())) {
      return false;
    }

    if (length != lcs.getLength()) {
      return false;
    }

    Optional<SMGEdgeHasValue> edge = smg.getHVEdgeFromMemoryLocation(pointerToStartObject);
    if (!edge.isPresent()) {
      return false;
    }

    SMGValue value = edge.orElseThrow().getValue();
    if (!smg.isPointer(value)) {
      return false;
    }

    SMGObject startObjectLock = smg.getPointer(value).getObject();
    if (!startObjectLock.equals(lcs.getCandidate().getStartObject())) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(length, pointerToStartObject, shape);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SMGAbstractListCandidateSequenceBlock<?>)) {
      return false;
    }
    SMGAbstractListCandidateSequenceBlock<?> other =
        (SMGAbstractListCandidateSequenceBlock<?>) obj;
    return length == other.length
        && Objects.equals(pointerToStartObject, other.pointerToStartObject)
        && Objects.equals(shape, other.shape);
  }
}