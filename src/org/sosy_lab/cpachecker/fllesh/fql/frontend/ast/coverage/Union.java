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
package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;

public class Union implements Coverage {

  private Coverage mCoverage1;
  private Coverage mCoverage2;
  
  public Union(Coverage pLeftCoverage, Coverage pRightCoverage) {
    assert(pLeftCoverage != null);
    assert(pRightCoverage != null);
    
    mCoverage1 = pLeftCoverage;
    mCoverage2 = pRightCoverage;
  }
  
  public Coverage getLeftCoverage() {
    return mCoverage1;
  }
  
  public Coverage getRightCoverage() {
    return mCoverage2;
  }
  
  @Override
  public String toString() {
    return "UNION(" + mCoverage1.toString() + ", " + mCoverage2.toString() + ")";
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
      Union lOther = (Union)pOther;
      
      return lOther.mCoverage1.equals(mCoverage1) && lOther.mCoverage2.equals(mCoverage2);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 92877 + mCoverage1.hashCode() + mCoverage2.hashCode();
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
