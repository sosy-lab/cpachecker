// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity.policies;

/**
 * Abstract Class for representing a Security Level
 */
abstract public class SecurityClasses implements Comparable<SecurityClasses>{


  @Override
  /**
   * Compares this SecurityClass with another SecurityClass
   */
  public int compareTo(SecurityClasses pObj) {
     if (this==pObj) {
        return 0;
     }
     if (pObj==null) {
        return 1;
     }
     return (getClass().getName().compareTo(pObj.getClass().getName()));
  }

}
