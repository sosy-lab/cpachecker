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
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate.Predicates;

public class ConditionalCoverage implements Coverage {

  private Coverage mCoverage;
  private Predicates mPreconditions;
  private Predicates mPostconditions;
  
  public ConditionalCoverage(Predicates pPreconditions, Coverage pCoverage, Predicates pPostconditions) {
    assert(pPreconditions != null);
    assert(pCoverage != null);
    assert(pPostconditions != null);
    
    mPreconditions = pPreconditions;
    mCoverage = pCoverage;
    mPostconditions = pPostconditions;
  }
  
  public Predicates getPreconditions() {
    return mPreconditions;
  }
  
  public Predicates getPostconditions() {
    return mPostconditions;
  }
  
  public Coverage getCoverage() {
    return mCoverage;
  }
  
  @Override
  public String toString() {
    return mPreconditions.toString() + " " + mCoverage.toString() + " " + mPostconditions.toString();
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
      ConditionalCoverage lOther = (ConditionalCoverage)pOther;
      
      return (lOther.mCoverage.equals(mCoverage) && lOther.mPreconditions.equals(mPreconditions) && lOther.mPostconditions.equals(mPostconditions));
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 712803 + mCoverage.hashCode() + mPreconditions.hashCode() + mPostconditions.hashCode();
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
