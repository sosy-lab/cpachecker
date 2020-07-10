// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
