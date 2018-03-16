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

import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

/** A class to represent an Address. This class is mainly used to store Address Information. */
class SMGKnownAddress extends SMGAddress {

  private SMGKnownAddress(SMGObject pObject, SMGKnownExpValue pOffset) {
    super(pObject, pOffset);
  }

  public static SMGKnownAddress valueOf(SMGObject pObject, long pOffset) {
    return new SMGKnownAddress(pObject, SMGKnownExpValue.valueOf(pOffset));
  }

  public static SMGKnownAddress valueOf(SMGObject object, SMGKnownExpValue offset) {
    return new SMGKnownAddress(object, offset);
  }

  @Override
  public SMGKnownExpValue getOffset() {
    return (SMGKnownExpValue) super.getOffset();
  }
}
