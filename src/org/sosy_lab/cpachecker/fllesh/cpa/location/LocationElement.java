/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.cpa.location;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class LocationElement implements ILocationElement {
  
  public static int NUMBER_OF_INSTANCES = 0;
  
  private final CFANode mCFANode;
  private final int mHashCode;

  public LocationElement(CFANode pLocationNode) {
    mCFANode = pLocationNode;
    mHashCode = mCFANode.getNodeNumber();
    NUMBER_OF_INSTANCES++;
  }

  @Override
  public CFANode getLocationNode() {
    return mCFANode;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
      
    if (pOther == null) {
      return false;
    }
      
    if (getClass().equals(pOther.getClass())) {
      LocationElement lOther = (LocationElement)pOther;
      return (mCFANode.equals(lOther.mCFANode));
    }
    
    return false;
  }

  @Override
  public String toString()
  {
    return Integer.toString(mCFANode.getNodeNumber());
  }

  @Override
  public int hashCode() {
  	return mHashCode;
  }
}
