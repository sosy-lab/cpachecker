// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.value;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

/**
 * A class to represent a value which points to an address. This class is mainly used to store value
 * information.
 *
 * <p>TODO I do not like this class. We should avoid instances of this class as part of an SMGState,
 * because it references an SMGObject that might be updated/deleted via another reference in the
 * SMG, and this class remains outdated and leads to invalid behaviour. The information about which
 * value points to which object in the SMG should already be available as SMGEdgePointsTo.
 */
public final class SMGKnownAddressValue extends SMGKnownSymValue implements SMGAddressValue {

  /** The address this value represents. */
  private final SMGAddress address;

  private SMGKnownAddressValue(BigInteger pValue, SMGAddress pAddress) {
    super(pValue);
    checkNotNull(pAddress);
    address = pAddress;
  }

  public static SMGAddressValue valueOf(
      SMGKnownSymbolicValue pAddress, SMGObject pObject, SMGKnownExpValue pOffset) {
    if (pAddress.isZero() && pObject.equals(SMGNullObject.INSTANCE) && pOffset.isZero()) {
      return SMGZeroValue.INSTANCE;
    } else if (pAddress.isUnknown() || pObject == null) {
      return SMGUnknownValue.INSTANCE;
    }
    return new SMGKnownAddressValue(pAddress.getId(), SMGAddress.valueOf(pObject, pOffset));
  }

  public static SMGAddressValue valueOf(SMGEdgePointsTo edge) {
    if (edge.getValue().isZero()
        && edge.getObject().equals(SMGNullObject.INSTANCE)
        && edge.getOffset() == 0) {
      return SMGZeroValue.INSTANCE;
    } else if (edge.getValue().isUnknown() || edge.getObject() == null) {
      return SMGUnknownValue.INSTANCE;
    }
    return new SMGKnownAddressValue(
        ((SMGSymbolicValue) edge.getValue()).getId(),
        SMGAddress.valueOf(edge.getObject(), SMGKnownExpValue.valueOf(edge.getOffset())));
  }

  @Override
  public SMGAddress getAddress() {
    return address;
  }

  @Override
  public String toString() {
    return super.toString() + " with address <" + address + ">";
  }

  @Override
  public SMGKnownExpValue getOffset() {
    return (SMGKnownExpValue) address.getOffset();
  }

  @Override
  public SMGObject getObject() {
    return address.getObject();
  }
}
