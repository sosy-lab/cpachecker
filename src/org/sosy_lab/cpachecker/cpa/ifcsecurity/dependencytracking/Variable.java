/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking;

import java.io.Serializable;

/**
 * Class, for wrapping one Variable
 */
public class Variable implements Comparable<Variable>, Serializable{
  private static final long serialVersionUID = -4199046985815841335L;
  /**
   * Identifier of the Variable
   */
  private String name="";

  /**
   * Constructs a Variable with the given identifier.
   * @param pName Identifier of the Variable.
   */
  public Variable(String pName){
    this.name=pName;
  }

  @Override
  public boolean equals(Object obj) {
   if (this==obj) {
      return true;
   }
   if (obj==null) {
      return false;
   }
    if (!(obj instanceof Variable )) {
      return super.equals(obj); // different class
   }
    Variable other = (Variable) obj;
    return (this.compareTo(other)==0);
 }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int compareTo(Variable pOther) {
    if (this==pOther) {
      return 0;
   }
   if (pOther==null) {
      return -1;
   }

   if (this.name==null)  {
      if (this.name==null && pOther.name!=null) {
         return 1;
      }
      return 0;
   }

   // this.content can't be null
   return this.name.compareTo(pOther.name);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

//  @Override
//  public int hashCode(){
//    return this.name.hashCode();
//  }
}
