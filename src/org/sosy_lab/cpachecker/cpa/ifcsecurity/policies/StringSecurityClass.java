// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity.policies;




/**
 * Generic Security Class, that generates a Security Class from a String
 */
public class StringSecurityClass extends SecurityClasses{

  /** Internal Variable containing the String-Identifier of the Security class */
  private final String sc;

  /**
   * Constructor for the Security-Class
    * @param pSC the representive SecurityClass-Element
   */
  public StringSecurityClass(String pSC){
    this.sc=pSC;
  }

  @Override
  public String toString(){
    return this.sc.toString();
  }

  @Override
  public int hashCode() {
    return this.sc.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (!(pObj instanceof StringSecurityClass)) {
      return false;
    }
    return sc.equals(((StringSecurityClass) pObj).sc);
  }
}
