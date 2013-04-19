/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class SMGEdgeHasValue extends SMGEdge {
  final private CType type;
  final private int offset;

  public SMGEdgeHasValue(CType pType, int pOffset, SMGObject pObject, int pValue) {
    super(pValue, pObject);
    type = pType;
    offset = pOffset;
  }

  @Override
  public String toString() {
    return "sizeof(" + type.toASTString("foo") + ")b @ " + object.getLabel() + "+" + offset + "b has value " + value;
  }

  public int getOffset() {
    return offset;
  }

  public CType getType() {
    return type;
  }

  public int getSizeInBytes(MachineModel pMachineModel) {
    return pMachineModel.getSizeof(type);
  }

  @Override
  public boolean isConsistentWith(SMGEdge other) {
    if (! (other instanceof SMGEdgeHasValue)) {
      return false;
    }

    if ((this.object == other.object) &&
        (this.offset == ((SMGEdgeHasValue)other).offset) &&
        (this.type == ((SMGEdgeHasValue)other).type)) {
      return (this.value == other.value);
    }

    return true;
  }

  public boolean overlapsWith(SMGEdgeHasValue other, MachineModel pModel) {
    if (this.object != other.object) {
      throw new IllegalArgumentException("Call of overlapsWith() on Has-Value edges pair not originating from the same object");
    }

    int myStart = offset;
    int otStart = other.getOffset();

    int myEnd = myStart + pModel.getSizeof(type);
    int otEnd = otStart + pModel.getSizeof(other.getType());

    if (myStart < otStart) {
      return (myEnd > otStart);

    } else if ( otStart < myStart ) {
      return (otEnd > myStart);
    }

    // Start offsets are equal, always overlap
    return true;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + offset;
    // "Do not use a hashCode() of CType"
    // result = prime * result + ((type == null) ? 0 : type.hashCode());
    // TODO: Ugly, ugly, ugly!
    // I cannot obtain a hashcode of a type, therefore I cannot obtain hashcode
    // of the Has-Value edge. *Seems* to work not, but is likely to cause
    // problems in the future. Tread lightly.
    result = prime * result + ((type == null) ? 0 : System.identityHashCode(type));
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    SMGEdgeHasValue other = (SMGEdgeHasValue) obj;
    if (offset != other.offset)
      return false;
    if (type == null) {
      if (other.type != null)
        return false;
     } else if (!type.equals(other.type))
      return false;
    return true;
  }
}