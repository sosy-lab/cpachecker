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

import java.util.Iterator;
import java.util.LinkedList;

import org.sosy_lab.common.Pair;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.pathmonitor.PathMonitor;

public class Sequence implements Coverage, Iterable<Pair<PathMonitor, Coverage>> {

  private PathMonitor mFinalMonitor;
  
  private LinkedList<Pair<PathMonitor, Coverage>> mSequence;
  
  public Sequence(PathMonitor pInitialMonitor, Coverage pCoverage, PathMonitor pFinalMonitor) {
    assert(pInitialMonitor != null);
    assert(pCoverage != null);
    assert(pFinalMonitor != null);
    
    mFinalMonitor = pFinalMonitor;
    
    mSequence = new LinkedList<Pair<PathMonitor, Coverage>>();
    
    mSequence.add(new Pair<PathMonitor, Coverage>(pInitialMonitor, pCoverage));
  }
  
  public PathMonitor getFinalMonitor() {
    return mFinalMonitor;
  }
  
  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);
    
    return pVisitor.visit(this);
  }
  
  public void extend(PathMonitor pMonitor, Coverage pCoverage) {
    assert(pMonitor != null);
    assert(pCoverage != null);
    
    mSequence.add(new Pair<PathMonitor, Coverage>(pMonitor, pCoverage));
  }
  
  public int size() {
    return mSequence.size();
  }
  
  public Pair<PathMonitor, Coverage> get(int pIndex) {
    assert(0 <= pIndex);
    assert(pIndex < mSequence.size());
    
    return mSequence.get(pIndex);
  }
  
  @Override
  public String toString() {
    StringBuffer lBuffer = new StringBuffer();
    
    for (Pair<PathMonitor, Coverage> lPair : mSequence) {
      PathMonitor lMonitor = lPair.getFirst();
      Coverage lCoverage = lPair.getSecond();
      
      lBuffer.append("-[");
      lBuffer.append(lMonitor.toString());
      lBuffer.append("]>");
      lBuffer.append(lCoverage.toString());
    }

    lBuffer.append("-[");
    lBuffer.append(mFinalMonitor.toString());
    lBuffer.append("]>");
    
    return lBuffer.toString();
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
      Sequence lSequence = (Sequence)pOther;
      
      return lSequence.mSequence.equals(mSequence) 
              && lSequence.mFinalMonitor.equals(mFinalMonitor);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 398221 + mSequence.hashCode() + mFinalMonitor.hashCode();
  }

  @Override
  public Iterator<Pair<PathMonitor, Coverage>> iterator() {
    return mSequence.iterator();
  }

}
