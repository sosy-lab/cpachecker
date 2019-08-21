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

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
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

  /**
   * Represents an (unique?) identifier of a memory cell (i.e., its address). We do not guarantee
   * that it represents the actual content (data value) of the memory cell.
   *
   * <p>Special case: The value ZERO is special and represents the NULL_ADDRESS, i.e. can be
   * interpreted as an unaccessible memory cell at position ZERO.
   */
  protected final SMGValue value;

  /**
   * A part of the stack or heap containing data. In an {@link SMG} the data is represented as a
   * {@link SMGValue} and is reachable via {@link SMGEdgeHasValue}. Or the object is references from
   * a {@link SMGValue} via {@link SMGEdgePointsTo}.
   */
  protected final SMGObject object;

  /** Offset of the current edge, counted in Bits from the start of the {@link SMGObject}. */
  private final long offset;

  SMGEdge(SMGValue pValue, SMGObject pObject, long pOffset) {
    value = Preconditions.checkNotNull(pValue);
    object = Preconditions.checkNotNull(pObject);
    offset = pOffset;
  }

  /** @see #value */
  public SMGValue getValue() {
    return value;
  }

  /** @see #object */
  public SMGObject getObject() {
    return object;
  }

  /** @see #offset */
  public long getOffset() {
    return offset;
  }

  public abstract boolean isConsistentWith(SMGEdge pOther_edge);

  @Override
  public int hashCode() {
    return Objects.hash(object, value, offset);
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
    return value.equals(other.value)
        && offset == other.offset
        && Objects.equals(object, other.object);
  }
}
