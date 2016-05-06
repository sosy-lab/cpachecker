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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.policies;



/**
 * Generic Security Class, that generates a Security Class from an Element of the SecurityClassesSet
 */
public class EnumSecurityClass extends SecurityClasses{

  /**
   * Internal Variable containing the Enum-Identifier of the Security class
   */
  SecurityClassesSet sc;

  /**
   * Constructor for the Security-Class
   * @param sc the representive SecurityClass-Element
   */
  public EnumSecurityClass(SecurityClassesSet sc){
    this.sc=sc;
  }

  @Override
  public boolean equals(Object obj) {
    if (this==obj) {
      return true;
   }
   if (obj==null) {
      return false;
   }
   if (!(obj instanceof EnumSecurityClass )) {
      return false; // different class
   }

   EnumSecurityClass other = (EnumSecurityClass) obj;
   if (this.sc==other.sc) {
      return true;
   }
   if (this.sc==null && other.sc!=null) {
      return false;
   }
   // this.content can't be null
   return this.sc.equals(other.sc);
 }

  @Override
  public int compareTo(SecurityClasses obj) {
     if (this==obj) {
        return 0;
     }
     if (obj==null) {
        return 1;
     }
     if (!(obj instanceof EnumSecurityClass )) {
       return (getClass().getName().compareTo(obj.getClass().getName()));
     }
     EnumSecurityClass other=(EnumSecurityClass) obj;
     return ((this.sc).compareTo(other.sc));
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
