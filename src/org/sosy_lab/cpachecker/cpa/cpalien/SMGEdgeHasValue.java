/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

public class SMGEdgeHasValue {
  final private CType type;
  final private int offset;
  final private SMGObject object;
  final private int value;

  public int getValue() {
    return value;
  }

  public SMGEdgeHasValue(CType pType, int pOffset, SMGObject pObject, int pValue) {
    type = pType;
    offset = pOffset;
    object = pObject;
    value = pValue;
  }

  @Override
  public String toString() {
    return "sizeof(" + type.toASTString("foo") + ")b @ " + object.getLabel() + "+" + offset + "b has value " + value;
  }

  public SMGObject getObject() {
    return object;
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

  public boolean isConsistentWith(SMGEdgeHasValue other){
    if ((this.object == other.object) &&
        (this.offset == other.offset) &&
        (this.type == other.type)){
      return (this.value == other.value);
    }

    return true;
  }
}