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


public class SMGEdgePointsTo extends SMGEdge {
  final private int offset;
  //TODO: Implement target specifier later

  public SMGEdgePointsTo(int pValue, SMGObject pObject, int pOffset) {
    super(pValue, pObject);
    offset = pOffset;
  }
  @Override
  public String toString() {
    return value + "->" + object.getLabel() + "+" + offset + 'b';
  }

  public int getOffset() {
    return offset;
  }

  @Override
  public boolean isConsistentWith(SMGEdge other){
    /*
     * different value- > different place
     * same value -> same place
     */
    if (! (other instanceof SMGEdgePointsTo))
      return false;

    if (this.value != other.value){
      if (this.offset == ((SMGEdgePointsTo)other).offset && this.object == other.object){
        return false;
      }
    }
    else
      if (this.offset != ((SMGEdgePointsTo)other).offset || this.object != other.object){
        return false;
      }

    return true;
  }
}
