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
package org.sosy_lab.cpachecker.cpa.smg.graphs.edge;

import com.google.common.base.Objects;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

/**
 * SMGs have two kind of edges: {@link SMGEdgeHasValue} and {@link SMGEdgePointsTo}.
 *
 * <p>{@link SMGEdgeHasValue}s lead from {@link SMGObject}s to {@link SMGValue}s. {@link
 * SMGEdgeHasValue}s are labeled by the offset (and type) of the field in which the value is stored
 * within an object.
 *
 * <p>{@link SMGEdgePointsTo}s lead from addresses ({@link SMGValue}s to {@link SMGObject}s. {@link
 * SMGEdgePointsTo} are labeled by an offset (and a {@link SMGTargetSpecifier}). The offset
 * indicates that the address points before, inside, or after an object.
 */
public abstract class SMGEdge {
  final protected int value;
  final protected SMGObject object;
  private final long offset;

  SMGEdge(int pValue, SMGObject pObject, long pOffset) {
    value = pValue;
    object = pObject;
    offset = pOffset;
  }

  public int getValue() {
    return value;
  }

  public SMGObject getObject() {
    return object;
  }

  public long getOffset() {
    return offset;
  }

  public abstract boolean isConsistentWith(SMGEdge pOther_edge);

  @Override
  public int hashCode() {
    return Objects.hashCode(object, value, offset);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SMGEdge)) {
      return false;
    }
    SMGEdge other = (SMGEdge) obj;
    return value == other.value && offset == other.offset && Objects.equal(object, other.object);
  }
}
