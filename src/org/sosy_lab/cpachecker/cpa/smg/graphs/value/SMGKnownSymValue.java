// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.value;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import java.math.BigInteger;
import org.sosy_lab.common.UniqueIdGenerator;

public class SMGKnownSymValue extends SMGKnownValue implements SMGKnownSymbolicValue {

  public static final SMGKnownSymValue TRUE = new SMGKnownSymValue(BigInteger.valueOf(-1));

  /** every symbolic value gets its own ID. */
  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  SMGKnownSymValue(BigInteger pValue) {
    super(pValue);
  }

  /** Get a new symbolic SMGValue for a new memory location or region or whatever. */
  public static SMGKnownSymbolicValue of() {
    // We never return ZERO here, because ZERO is potentially for NULL.
    return valueOf(idGenerator.getFreshId() + 1);
  }

  @VisibleForTesting // use for testing only!
  public static SMGKnownSymbolicValue valueOf(int pValue) {
    return valueOf(BigInteger.valueOf(pValue));
  }

  private static SMGKnownSymbolicValue valueOf(BigInteger pValue) {
    checkNotNull(pValue);
    if (pValue.equals(BigInteger.ZERO)) {
      return SMGZeroValue.INSTANCE;
    } else {
      return new SMGKnownSymValue(pValue);
    }
  }

  @Override
  public final boolean equals(Object pObj) {
    return pObj instanceof SMGKnownSymValue && super.equals(pObj);
  }

  @Override
  public int hashCode() {
    return super.hashCode(); // equals() in this class checks nothing more
  }

  @Override
  public String asDotId() {
    return "Sym" + getValue();
  }

  @Override
  public BigInteger getId() {
    return getValue();
  }
}
