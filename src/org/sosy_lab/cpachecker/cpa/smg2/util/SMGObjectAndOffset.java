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
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public class SMGObjectAndOffset {

  private final SMGObject object;

  private final Value offset;

  private SMGObjectAndOffset(SMGObject pObject, Value pOffset) {
    object = pObject;
    offset = pOffset;
  }

  public static SMGObjectAndOffset of(SMGObject pObject, Value pOffset) {
    Preconditions.checkNotNull(pObject);
    Preconditions.checkNotNull(pOffset);
    return new SMGObjectAndOffset(pObject, pOffset);
  }

  public static SMGObjectAndOffset withZeroOffset(SMGObject pObject) {
    Preconditions.checkNotNull(pObject);
    return new SMGObjectAndOffset(pObject, new NumericValue(BigInteger.ZERO));
  }

  public SMGObject getSMGObject() {
    return object;
  }

  public Value getOffsetForObject() {
    return offset;
  }
}
