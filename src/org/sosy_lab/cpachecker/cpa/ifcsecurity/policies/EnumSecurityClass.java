// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity.policies;



/**
 * Generic Security Class, that generates a Security Class from an Element of the SecurityClassesSet
 */
public class EnumSecurityClass extends SecurityClasses{

  /**
   * Internal Variable containing the Enum-Identifier of the Security class
   */
  private SecurityClassesSet sc;

  /**
   * Constructor for the Security-Class
   * @param pSC the representive SecurityClass-Element
   */
  public EnumSecurityClass(SecurityClassesSet pSC){
    this.sc=pSC;
  }

  @Override
  public boolean equals(Object pObj) {
    if (this==pObj) {
      return true;
   }
   if (pObj==null) {
      return false;
   }
   if (!(pObj instanceof EnumSecurityClass )) {
      return false; // different class
   }

   EnumSecurityClass pOther = (EnumSecurityClass) pObj;
   if (this.sc==pOther.sc) {
      return true;
   }
   if (this.sc!=null && pOther.sc==null) {
      return false;
   }
   // this.content can't be null
   if(this.sc!=null && pOther.sc!= null){
     return this.sc.equals(pOther.sc);
   }
   return true;
 }

  @Override
  public int compareTo(SecurityClasses pObj) {
     if (this==pObj) {
        return 0;
     }
     if (pObj==null) {
        return 1;
     }
     if (!(pObj instanceof EnumSecurityClass )) {
       return (getClass().getName().compareTo(pObj.getClass().getName()));
     }
     EnumSecurityClass other=(EnumSecurityClass) pObj;
     return (this.sc.compareTo(other.sc));
  }

  @Override
  public String toString(){
    return this.sc.toString();
  }

  @Override
  public int hashCode() {
    return this.sc.hashCode();
  }
}
