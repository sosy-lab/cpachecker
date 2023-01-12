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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public class SMGObjectAndOffsetOrSMGState {

  // Either the state is null or object and offset, never all 3!
  private final @Nullable SMGObject object;

  private final @Nullable BigInteger offset;

  private final @Nullable SMGState state;
  private final boolean newErrorState;

  private SMGObjectAndOffsetOrSMGState(SMGObject pObject, BigInteger pOffset) {
    object = pObject;
    offset = pOffset;
    state = null;
    newErrorState = false;
  }

  private SMGObjectAndOffsetOrSMGState(SMGState pState) {
    object = null;
    offset = null;
    state = pState;
    newErrorState = false;
  }

  private SMGObjectAndOffsetOrSMGState(SMGState pState, boolean pNewErrorState) {
    object = null;
    offset = null;
    state = pState;
    newErrorState = pNewErrorState;
  }

  public static SMGObjectAndOffsetOrSMGState of(SMGObject pObject, BigInteger pOffset) {
    Preconditions.checkNotNull(pObject);
    Preconditions.checkNotNull(pOffset);
    return new SMGObjectAndOffsetOrSMGState(pObject, pOffset);
  }

  public static SMGObjectAndOffsetOrSMGState of(SMGObjectAndOffset objAndOffset) {
    Preconditions.checkNotNull(objAndOffset);
    return new SMGObjectAndOffsetOrSMGState(
        objAndOffset.getSMGObject(), objAndOffset.getOffsetForObject());
  }

  public static SMGObjectAndOffsetOrSMGState of(SMGState pState) {
    Preconditions.checkNotNull(pState);
    return new SMGObjectAndOffsetOrSMGState(pState);
  }

  public static SMGObjectAndOffsetOrSMGState ofErrorState(SMGState pState) {
    Preconditions.checkNotNull(pState);
    return new SMGObjectAndOffsetOrSMGState(pState, true);
  }

  public static SMGObjectAndOffsetOrSMGState withZeroOffset(SMGObject pObject) {
    Preconditions.checkNotNull(pObject);
    return new SMGObjectAndOffsetOrSMGState(pObject, BigInteger.ZERO);
  }

  public SMGObject getSMGObject() {
    Preconditions.checkArgument(state == null);
    Preconditions.checkArgument(object != null);
    Preconditions.checkArgument(offset != null);
    return object;
  }

  public BigInteger getOffsetForObject() {
    Preconditions.checkArgument(state == null);
    Preconditions.checkArgument(object != null);
    Preconditions.checkArgument(offset != null);
    return offset;
  }

  public SMGState getSMGState() {
    Preconditions.checkArgument(state != null);
    Preconditions.checkArgument(object == null);
    Preconditions.checkArgument(offset == null);
    return state;
  }

  public boolean hasSMGState() {
    return state != null;
  }

  public boolean hasSMGObjectAndOffset() {
    return object != null && offset != null;
  }

  public boolean hasNewErrorState() {
    return newErrorState && state != null;
  }
}
