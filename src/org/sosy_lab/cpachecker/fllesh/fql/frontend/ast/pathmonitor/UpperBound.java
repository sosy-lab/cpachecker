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
package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;

public class UpperBound implements PathMonitor {

  private PathMonitor mSubmonitor;
  private int mBound;
  
  public UpperBound(PathMonitor pSubmonitor, int pBound) {
    assert(pSubmonitor != null);
    assert(pBound >= 0);
    
    mSubmonitor = pSubmonitor;
    mBound = pBound;
  }
  
  public int getBound() {
    return mBound;
  }
  
  public PathMonitor getSubmonitor() {
    return mSubmonitor;
  }
  
  @Override
  public String toString() {
    return "(" + mSubmonitor.toString() + ")<=" + mBound;
  }
  
  @Override
  public int hashCode() {
    return 39202 + mSubmonitor.hashCode() + mBound;
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (pOther == this) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      UpperBound lUpperBound = (UpperBound)pOther;
      
      return lUpperBound.mSubmonitor.equals(mSubmonitor) && (lUpperBound.mBound == mBound);
    }
    
    return false;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
