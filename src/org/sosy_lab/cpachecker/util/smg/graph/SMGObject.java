// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import java.math.BigInteger;

public class SMGObject implements SMGNode {

  private static final SMGObject NULL_OBJECT = of(0, BigInteger.ZERO, BigInteger.ZERO, false);

  private final int nestingLevel;
  private final BigInteger size;
  private final BigInteger offset;
  private boolean valid;

  protected SMGObject(int pNestingLevel, BigInteger pSize, BigInteger pOffset, boolean pValid) {
    nestingLevel = pNestingLevel;
    size = pSize;
    offset = pOffset;
    valid = pValid;
  }

  public static SMGObject nullInstance() {
    return NULL_OBJECT;
  }

  public static SMGObject
      of(int pNestingLevel, BigInteger pSize, BigInteger pOffset, boolean pValid) {
    return new SMGObject(pNestingLevel, pSize, pOffset, pValid);
  }

  public BigInteger getSize() {
    return size;
  }

  public BigInteger getOffset() {
    return offset;
  }

  public boolean isValid() {
    return valid;
  }

  public void invalidate() {
    valid = false;
  }

  @Override
  public int getNestingLevel() {
    return nestingLevel;
  }
}
