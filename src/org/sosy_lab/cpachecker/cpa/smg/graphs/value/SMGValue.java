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

import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

/**
 * SMGs consists of two types of nodes: {@link SMGObject}s and {@link SMGValue}s. {@link SMGValue}s
 * represent addresses or data stored in {@link SMGObject}s. All values are abstract, such that we
 * only know whether they are equal or not. The only exception is the value 0 that is used to
 * represent 0 in all possible types as well as the address of the {@link SMGNullObject}.
 */
public interface SMGValue extends Comparable<SMGValue> {

  /**
   * For efficiency and performance we define an ordering on SMGValues. The ordering is as follows:
   *
   * <ol>
   *   <li>UNKNOWN
   *   <li>ZERO (special value!)
   *   <li>explicitValues (ordered by their value)
   *   <li>symbolic values (ordered by their id)
   * </ol>
   *
   * For simplification we implement the comparison directly in the interface.
   */
  @Override
  default int compareTo(SMGValue other) {

    // UNKNOWN
    if (this.isUnknown()) {
      return other.isUnknown() ? 0 : -1;
    }

    // ZERO
    if (this.isZero()) {
      if (other.isUnknown()) {
        return 1;
      } else if (other.isZero()) {
        return 0;
      } else {
        return -1;
      }
    }

    // explicitValues (ordered by their value)
    if (this instanceof SMGExplicitValue) {
      if (other.isUnknown() || other.isZero()) {
        return 1;
      } else if (other instanceof SMGExplicitValue) {
        return ((SMGExplicitValue) this).getValue().compareTo(((SMGExplicitValue) other).getValue());
      } else {
        return -1;
      }
    }

    // symbolic values (ordered by their id)
    if (this instanceof SMGSymbolicValue) {
      if (other.isUnknown() || other.isZero() || !(other instanceof SMGSymbolicValue)) {
        return 1;
      } else {
        return ((SMGSymbolicValue) this).getId().compareTo(((SMGSymbolicValue) other).getId());
      }
    }

    throw new AssertionError(String.format("unexpected comparison of '%s' and '%s'", this, other));
  }

  /**
   * Returns whether the current value is ZERO in any of our representations. Note that we return
   * FALSE for UNKNOWN .
   */
  default boolean isZero() {
    return SMGZeroValue.INSTANCE == this;
  }

  default boolean isUnknown() {
    return SMGUnknownValue.INSTANCE == this;
  }

  /** returns a unique identifier that can be used as dot-identifier for graphvis. */
  String asDotId();
}
