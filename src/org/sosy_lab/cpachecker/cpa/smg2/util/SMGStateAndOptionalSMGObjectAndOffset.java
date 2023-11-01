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
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public class SMGStateAndOptionalSMGObjectAndOffset {

  // Always a state, maybe an object and offset (Value, may be Numeric/Symbolic/Unknown)
  private final Optional<SMGObject> object;

  private final Optional<Value> offset;

  private final SMGState state;

  private SMGStateAndOptionalSMGObjectAndOffset(SMGObject pObject, Value pOffset, SMGState pState) {
    object = Optional.of(pObject);
    offset = Optional.of(pOffset);
    state = pState;
  }

  private SMGStateAndOptionalSMGObjectAndOffset(SMGState pState) {
    object = Optional.empty();
    offset = Optional.empty();
    state = pState;
  }

  public static SMGStateAndOptionalSMGObjectAndOffset of(
      SMGObject pObject, Value pOffset, SMGState pState) {
    Preconditions.checkNotNull(pObject);
    Preconditions.checkNotNull(pOffset);
    Preconditions.checkNotNull(pState);
    return new SMGStateAndOptionalSMGObjectAndOffset(pObject, pOffset, pState);
  }

  public static SMGStateAndOptionalSMGObjectAndOffset of(
      SMGObjectAndOffset objAndOff, SMGState pState) {
    Preconditions.checkNotNull(objAndOff);
    Preconditions.checkNotNull(pState);
    return new SMGStateAndOptionalSMGObjectAndOffset(
        objAndOff.getSMGObject(), objAndOff.getOffsetForObject(), pState);
  }

  public static SMGStateAndOptionalSMGObjectAndOffset of(SMGState pState) {
    Preconditions.checkNotNull(pState);
    return new SMGStateAndOptionalSMGObjectAndOffset(pState);
  }

  public static SMGStateAndOptionalSMGObjectAndOffset withZeroOffset(
      SMGObject pObject, SMGState pState) {
    Preconditions.checkNotNull(pObject);
    Preconditions.checkNotNull(pState);
    return new SMGStateAndOptionalSMGObjectAndOffset(
        pObject, new NumericValue(BigInteger.ZERO), pState);
  }

  public SMGObject getSMGObject() {
    return object.orElseThrow();
  }

  public Value getOffsetForObject() {
    return offset.orElseThrow();
  }

  public SMGState getSMGState() {
    return state;
  }

  public boolean hasSMGObjectAndOffset() {
    return object.isPresent() && offset.isPresent();
  }
}
