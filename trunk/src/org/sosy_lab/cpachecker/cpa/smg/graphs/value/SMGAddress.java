// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.value;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

/** A class to represent an Address. This class is mainly used to store Address Information. */
public final class SMGAddress {

  public static final SMGAddress UNKNOWN = new SMGAddress(null, SMGUnknownValue.INSTANCE);
  public static final SMGAddress ZERO =
      new SMGAddress(SMGNullObject.INSTANCE, SMGZeroValue.INSTANCE);

  /** The SMGObject representing the Memory this address belongs to. */
  private final SMGObject object;

  /** The offset relative to the beginning of object in byte. */
  private final SMGExplicitValue offset;

  SMGAddress(SMGObject pObject, SMGExplicitValue pOffset) {
    checkNotNull(pOffset);
    object = pObject;
    offset = pOffset;
  }

  public boolean isUnknown() {
    return object == null || offset.isUnknown();
  }

  /**
   * Return an address with (offset + pAddedOffset).
   *
   * @param pAddedOffset The offset added to this address.
   */
  public SMGAddress add(SMGExplicitValue pAddedOffset) {

    if (object == null || offset.isUnknown() || pAddedOffset.isUnknown()) {
      return SMGAddress.UNKNOWN;
    }

    return valueOf(object, offset.add(pAddedOffset));
  }

  public SMGExplicitValue getOffset() {
    return offset;
  }

  public SMGObject getObject() {
    return object;
  }

  public static SMGAddress valueOf(SMGObject object, SMGExplicitValue offset) {
    return new SMGAddress(object, offset);
  }

  @Override
  public String toString() {
    if (isUnknown()) {
      return "Unkown";
    }
    return "Object: " + object.getLabel() + " Offset: " + offset;
  }

  @Override
  public int hashCode() {
    return Objects.hash(object, offset);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SMGAddress)) {
      return false;
    }
    SMGAddress other = (SMGAddress) o;
    return Objects.equals(object, other.object) && Objects.equals(offset, other.offset);
  }

  public static SMGAddress valueOf(SMGObject pObj, int pOffset) {
    return new SMGAddress(pObj, SMGKnownExpValue.valueOf(pOffset));
  }
}
