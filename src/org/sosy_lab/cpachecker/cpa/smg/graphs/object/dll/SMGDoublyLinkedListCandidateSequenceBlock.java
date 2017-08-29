/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll;

import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPath;

public class SMGDoublyLinkedListCandidateSequenceBlock
    implements SMGAbstractionBlock {

  private final SMGDoublyLinkedListShape shape;
  private final int length;
  private final SMGMemoryPath pointerToStartObject;

  public SMGDoublyLinkedListCandidateSequenceBlock(SMGDoublyLinkedListShape pShape, int pLength,
      SMGMemoryPath pPointerToStartObject) {
    super();
    shape = pShape;
    length = pLength;
    pointerToStartObject = pPointerToStartObject;
  }

  @Override
  public boolean isBlocked(SMGAbstractionCandidate pCandidate, CLangSMG smg) {

    if (!(pCandidate instanceof SMGDoublyLinkedListCandidateSequence)) { return false; }

    SMGDoublyLinkedListCandidateSequence dllcs = (SMGDoublyLinkedListCandidateSequence) pCandidate;

    if (!shape.equals(dllcs.getCandidate().getDllShape())) {
      return false;
    }

    if(length != dllcs.getLength()) {
      return false;
    }

    Optional<SMGEdgeHasValue> edge = smg.getHVEdgeFromMemoryLocation(pointerToStartObject);

    if(!edge.isPresent()) {
      return false;
    }

    int value = edge.get().getValue();

    if (!smg.isPointer(value)) {
      return false;
    }

    SMGObject startObjectLock = smg.getPointer(value).getObject();

    if (!startObjectLock.equals(dllcs.getCandidate().getObject())) {
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
    if (!(obj instanceof SMGDoublyLinkedListCandidateSequenceBlock)) {
      return false;
    }
    SMGDoublyLinkedListCandidateSequenceBlock other =
        (SMGDoublyLinkedListCandidateSequenceBlock) obj;
    return length == other.length
        && Objects.equals(pointerToStartObject, other.pointerToStartObject)
        && Objects.equals(shape, other.shape);
  }
}