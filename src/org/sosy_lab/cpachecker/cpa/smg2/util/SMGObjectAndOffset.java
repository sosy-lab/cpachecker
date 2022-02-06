// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public class SMGObjectAndOffset {

  private final SMGObject object;

  private final BigInteger offset;

  public SMGObjectAndOffset(SMGObject pObject, BigInteger pOffset) {
    object = pObject;
    offset = pOffset;
  }

  public SMGObject getSMGObject() {
    return object;
  }

  public BigInteger getOffsetForObject() {
    return offset;
  }
}
