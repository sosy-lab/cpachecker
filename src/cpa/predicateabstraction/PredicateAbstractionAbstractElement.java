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
package cpa.predicateabstraction;

import cpa.common.interfaces.AbstractElement;
import cpa.symbpredabs.AbstractFormula;

/**
 * AbstractElement for explicit-state lazy abstraction.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class PredicateAbstractionAbstractElement implements AbstractElement {

  private int elemId;
  private AbstractFormula abstraction;

  //private boolean covered;
  //private int mark;

  private static int nextAvailableId = 1;

  PredicateAbstractionAbstractElement(AbstractFormula a) {
    elemId = nextAvailableId++;
    abstraction = a;
    //covered = false;
    //mark = 0;
  }

  public PredicateAbstractionAbstractElement() {
    this(null);
  }

  public int getId() { 
    return elemId; 
  }

  public AbstractFormula getAbstraction() { 
    return abstraction; 
  }

  public void setAbstraction(AbstractFormula a) {
    abstraction = a;
  }

//  public boolean isCovered() {
//    return covered; 
//  }
//  public void setCovered(boolean yes) { 
//    covered = yes; setMark(); 
//  }
//
//  public boolean isMarked() { 
//    return mark > 0; 
//  }
//  public void setMark() { 
//    mark = nextAvailableId++; 
//  }
//  public int getMark() { 
//    return mark; 
//  }

  @Override
  public boolean isError() {
    return false;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof PredicateAbstractionAbstractElement)) {
      return false;
    } else {
      return elemId == ((PredicateAbstractionAbstractElement)o).elemId;
    }
  }

  @Override
  public int hashCode() {
    return elemId;
  }

  @Override
  public String toString() {
    return "E<" + ">(";
//    +
//    (isMarked() ? mark : getId()) + ")";
  }

}
