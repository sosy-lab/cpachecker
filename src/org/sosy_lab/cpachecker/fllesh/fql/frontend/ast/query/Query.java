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
package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.query;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.FQLNode;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.coverage.Coverage;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.PathMonitor;

public class Query implements FQLNode {

  private Coverage mCoverage;
  private PathMonitor mMonitor;
  
  public Query(Coverage pCoverage, PathMonitor pPassingMonitor) {
    assert(pCoverage != null);
    assert(pPassingMonitor != null);
    
    mCoverage = pCoverage;
    mMonitor = pPassingMonitor;
  }
  
  public Coverage getCoverage() {
    return mCoverage;
  }
  
  public PathMonitor getPassingMonitor() {
    return mMonitor;
  }
  
  @Override
  public String toString() {
    return "COVER " + mCoverage.toString() + " PASSING " + mMonitor.toString();
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
      Query lQuery = (Query)pOther;
      
      return mCoverage.equals(lQuery.mCoverage) && mMonitor.equals(lQuery.mMonitor);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 32423 + mCoverage.hashCode() + mMonitor.hashCode();
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }

}
