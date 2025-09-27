// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import java.util.Optional;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public class SMGObjectAndOffsetMaybeNestingLvl {

  private final SMGObject object;

  private final Value offset;

  // Pointer level of towards abstracted memory
  private final Optional<Integer> pointerLevel;

  private SMGObjectAndOffsetMaybeNestingLvl(SMGObject pObject, Value pOffset) {
    object = pObject;
    offset = pOffset;
    pointerLevel = Optional.empty();
  }

  private SMGObjectAndOffsetMaybeNestingLvl(
      SMGObject pObject, Value pOffset, int pointerNestingLevelTowardsObject) {
    object = pObject;
    offset = pOffset;
    pointerLevel = Optional.of(pointerNestingLevelTowardsObject);
  }

  public static SMGObjectAndOffsetMaybeNestingLvl of(SMGObject pObject, Value pOffset) {
    Preconditions.checkNotNull(pObject);
    Preconditions.checkNotNull(pOffset);
    return new SMGObjectAndOffsetMaybeNestingLvl(pObject, pOffset);
  }

  public static SMGObjectAndOffsetMaybeNestingLvl of(
      SMGObject pObject, Value pOffset, int pointerNestingLevelTowardsObject) {
    Preconditions.checkNotNull(pObject);
    Preconditions.checkNotNull(pOffset);
    return new SMGObjectAndOffsetMaybeNestingLvl(
        pObject, pOffset, pointerNestingLevelTowardsObject);
  }

  public static SMGObjectAndOffsetMaybeNestingLvl withZeroOffset(SMGObject pObject) {
    Preconditions.checkNotNull(pObject);
    return new SMGObjectAndOffsetMaybeNestingLvl(pObject, new NumericValue(BigInteger.ZERO));
  }

  public SMGObject getSMGObject() {
    return object;
  }

  public Value getOffsetForObject() {
    return offset;
  }

  public Optional<Integer> getPointerLevelForObject() {
    return pointerLevel;
  }
}
