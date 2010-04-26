/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
/**
 * 
 */
package org.sosy_lab.cpachecker.util.automaton;

/**
 * @author holzera
 *
 */
public class AndLabel<E> implements Label<E> {

  private Label<E> mLabel1;
  private Label<E> mLabel2;
  
  public AndLabel(Label<E> pLabel1, Label<E> pLabel2) {
    assert(pLabel1 != null);
    assert(pLabel2 != null);
    
    mLabel1 = pLabel1;
    mLabel2 = pLabel2;
  }
  
  /* (non-Javadoc)
   * @see cpa.scoperestrictionautomaton.label.Label#matches(java.lang.Object)
   */
  @Override
  public boolean matches(E pE) {
    assert(pE != null);
    
    return (mLabel1.matches(pE) && mLabel2.matches(pE));
  }
  
  @Override
  public boolean equals(Object pObject) {
    if (pObject == null) {
      return false;
    }
    
    if (!(pObject instanceof AndLabel<?>)) {
      return false;
    }
    
    AndLabel<?> lLabel = (AndLabel<?>)pObject;
    
    if ((mLabel1.equals(lLabel.mLabel1) && mLabel2.equals(mLabel2))
        || (mLabel1.equals(lLabel.mLabel2) && mLabel2.equals(mLabel1))) {
      return true;
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return mLabel1.hashCode() + mLabel2.hashCode();
  }

  @Override
  public String toString() {
    return "(" + mLabel1.toString() + " AND " + mLabel2.toString() + ")";
  }
}
