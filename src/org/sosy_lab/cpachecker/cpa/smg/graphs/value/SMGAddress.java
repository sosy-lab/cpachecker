/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.graphs.value;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

/** A class to represent an Address. This class is mainly used to store Address Information. */
public class SMGAddress {

  public static final SMGAddress UNKNOWN = new SMGAddress(null, SMGUnknownValue.getInstance());

  /** The SMGObject representing the Memory this address belongs to. */
  private final SMGObject object;

  /** The offset relative to the beginning of object in byte. */
  private final SMGExplicitValue offset;

  protected SMGAddress(SMGObject pObject, SMGExplicitValue pOffset) {
    checkNotNull(pOffset);
    object = pObject;
    offset = pOffset;
  }

  public final boolean isUnknown() {
    return object == null || offset.isUnknown();
  }

  /**
   * Return an address with (offset + pAddedOffset).
   *
   * @param pAddedOffset The offset added to this address.
   */
  public final SMGAddress add(SMGExplicitValue pAddedOffset) {

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
  public final String toString() {

    if (isUnknown()) {
      return "Unkown";
    }

    return "Object: " + object + " Offset: " + offset;
  }

  public static SMGAddress valueOf(SMGObject pObj, int pOffset) {
    return new SMGAddress(pObj, SMGKnownExpValue.valueOf(pOffset));
  }

  public static SMGAddress getUnknownInstance() {
    return UNKNOWN;
  }
}

