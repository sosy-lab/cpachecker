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
package org.sosy_lab.cpachecker.fllesh.fql.fllesh.cpa;

import org.sosy_lab.cpachecker.cpa.mustmay.MustMayAnalysisElement;

public class QueryStandardElement implements QueryElement {

  private Integer mAutomatonState1;
  private boolean mMustState1;
  
  private Integer mAutomatonState2;
  private boolean mMustState2;
  
  private MustMayAnalysisElement mDataSpace;
  
  public QueryStandardElement(Integer pAutomatonState1, boolean pMustState1, Integer pAutomatonState2, boolean pMustState2, MustMayAnalysisElement pDataSpace) {
    assert(pAutomatonState1 != null);
    assert(pAutomatonState2 != null);
    assert(pDataSpace != null);
    
    mAutomatonState1 = pAutomatonState1;
    mMustState1 = pMustState1;
    
    mAutomatonState2 = pAutomatonState2;
    mMustState2 = pMustState2;
    
    mDataSpace = pDataSpace;
  }
  
  public Integer getAutomatonState1() {
    return mAutomatonState1;
  }
  
  public Integer getAutomatonState2() {
    return mAutomatonState2;
  }
  
  public boolean getMustState1() {
    return mMustState1;
  }
  
  public boolean getMustState2() {
    return mMustState2;
  }
  
  public MustMayAnalysisElement getDataSpace() {
    return mDataSpace;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      QueryStandardElement lElement = (QueryStandardElement)pOther;
      
      return mAutomatonState1.equals(lElement.mAutomatonState1)
              && mMustState1 == lElement.mMustState1
              && mAutomatonState2.equals(lElement.mAutomatonState2)
              && mMustState2 == lElement.mMustState2
              && mDataSpace.equals(lElement.mDataSpace);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 239438 + mAutomatonState1.hashCode() + mAutomatonState2.hashCode() + mDataSpace.hashCode() + (mMustState1?1:0) + (mMustState2?1:0);
  }
  
  @Override
  public String toString() {
    return "( <" + (mMustState1?mAutomatonState1.toString():"bot") + ", " + mAutomatonState1.toString() + ">, <" + (mMustState2?mAutomatonState2.toString():"bot") + ", " + mAutomatonState2.toString() + ">, " + mDataSpace.toString() + ")";
  }
  
  @Override
  public boolean isError() {
    // TODO Auto-generated method stub
    return false;
  }

}
