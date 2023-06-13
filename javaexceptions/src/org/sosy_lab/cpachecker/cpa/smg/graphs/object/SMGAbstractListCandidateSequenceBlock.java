// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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

  protected SMGAbstractListCandidateSequenceBlock(
      S pShape, int pLength, SMGMemoryPath pPointerToStartObject) {
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
    SMGAbstractListCandidateSequenceBlock<?> other = (SMGAbstractListCandidateSequenceBlock<?>) obj;
    return length == other.length
        && Objects.equals(pointerToStartObject, other.pointerToStartObject)
        && Objects.equals(shape, other.shape);
  }
}
