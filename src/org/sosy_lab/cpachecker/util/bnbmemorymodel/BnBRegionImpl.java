/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.bnbmemorymodel;

import org.sosy_lab.cpachecker.cfa.types.c.CType;


public class BnBRegionImpl implements BnBRegion {
  private final CType regionParent;
  private final CType elemType;
  private final String elemName;

  /**
   * @param pType - types that will be present in this region
   * @param pStructType - struct with fields or null if global
   * @param name - field name
   */
  public BnBRegionImpl(CType pType, CType pStructType, String name){
    regionParent = pStructType;
    elemType = pType;
    elemName = name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((elemName == null) ? 0 : elemName.hashCode());
    result = prime * result + ((elemType == null) ? 0 : elemType.hashCode());
    result = prime * result + ((regionParent == null) ? 0 : regionParent.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    BnBRegionImpl other = (BnBRegionImpl) obj;
    if (elemName == null) {
      if (other.elemName != null) {
        return false;
      }
    } else if (!elemName.equals(other.elemName)) {
      return false;
    }
    if (elemType == null) {
      if (other.elemType != null) {
        return false;
      }
    } else if (!elemType.equals(other.elemType)) {
      return false;
    }
    if (regionParent == null) {
      if (other.regionParent != null) {
        return false;
      }
    } else if (!regionParent.equals(other.regionParent)) {
      return false;
    }
    return true;
  }

  @Override
  public CType getType(){
    return elemType;
  }

  @Override
  public CType getRegionParent(){
    return regionParent;
  }

  @Override
  public String getElem(){
    return elemName;
  }

  @Override
  public String toString(){
    String result = "";
    result += "Type: " + elemType + '\n';
    result += "Parent: " + regionParent.toString() + '\n';
    result += '\t' + "Elem: " + elemName + '\n';
    return result;
  }
}
