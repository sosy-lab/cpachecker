// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.value;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

/** Class representing value Zero, Null and everything else consisting of zeroed memory. */
public final class SMGZeroValue extends SMGKnownExpValue
    implements SMGKnownSymbolicValue, SMGAddressValue {

  public static final SMGZeroValue INSTANCE = new SMGZeroValue();

  private SMGZeroValue() {
    super(BigInteger.ZERO);
  }

  @Override
  public String toString() {
    return "NULL";
  }

  @Override
  public SMGAddress getAddress() {
    return SMGAddress.ZERO;
  }

  @Override
  public SMGObject getObject() {
    return SMGNullObject.INSTANCE;
  }

  @Override
  public SMGExplicitValue getOffset() {
    return this;
  }

  @Override
  public BigInteger getId() {
    return BigInteger.ZERO;
  }
}
