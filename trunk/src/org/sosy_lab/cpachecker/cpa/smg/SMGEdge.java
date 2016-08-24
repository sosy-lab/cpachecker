/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.base.Objects;

import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

public abstract class SMGEdge {
  final protected int value;
  final protected SMGObject object;

  SMGEdge(int pValue, SMGObject pObject) {
    value = pValue;
    object = pObject;
  }

  public int getValue() {
    return value;
  }

  public SMGObject getObject() {
    return object;
  }

  public abstract boolean isConsistentWith(SMGEdge pOther_edge);

  @Override
  public int hashCode() {
    return Objects.hashCode(object, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof SMGEdge)) {
      return false;
    }
    SMGEdge other = (SMGEdge) obj;
    return value == other.value
        && Objects.equal(object, other.object);
  }
}
