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

public class Concatenation implements PathMonitor {

  private PathMonitor mSubmonitor1;
  private PathMonitor mSubmonitor2;
  
  public Concatenation(PathMonitor pSubmonitor1, PathMonitor pSubmonitor2) {
    assert(pSubmonitor1 != null);
    assert(pSubmonitor2 != null);
    
    mSubmonitor1 = pSubmonitor1;
    mSubmonitor2 = pSubmonitor2;
  }
  
  public PathMonitor getLeftSubmonitor() {
    return mSubmonitor1;
  }
  
  public PathMonitor getRightSubmonitor() {
    return mSubmonitor2;
  }
  
  @Override
  public String toString() {
    return mSubmonitor1.toString() + "." + mSubmonitor2.toString();
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
      Concatenation lMonitor = (Concatenation)pOther;
      
      return lMonitor.mSubmonitor1.equals(mSubmonitor1)
              && lMonitor.mSubmonitor2.equals(mSubmonitor2);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 93223 + mSubmonitor1.hashCode() + mSubmonitor2.hashCode();
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
