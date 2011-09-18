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
package org.sosy_lab.cpachecker.util.invariants;

import java.util.List;
import java.util.Vector;

public class Coeff {

  private String a;

  public Coeff(String a) {
    this.a = a;
  }

  public String getValue() {
    return a;
  }

  @Override
  public String toString() {
    String s = a;
    try {
      Integer I = new Integer(a);
      s = I.toString();
    } catch (NumberFormatException e) {}
    return s;
  }

  public void setValue(String b) {
    a = b;
  }

  public Coeff negative() {
    // Add a minus sign or take one away.
    Coeff c = null;
    if (a.startsWith("-")) {
      c = new Coeff( a.substring(1) );
    } else {
      c = new Coeff( "-"+a );
    }
    return c;
  }

  public static Vector<Coeff> makeCoeffList(String[] C) {
    Vector<Coeff> coeffs = new Vector<Coeff>();
    for (int i = 0; i < C.length; i++) {
      coeffs.add( new Coeff( C[i] ) );
    }
    return coeffs;
  }

  public static String coeffsToString(List<Coeff> C) {
    // From a List of Coeffs, create a string listing them all,
    // separated by spaces.
    String s = "";
    for (int i = 0; i < C.size(); i++) {
      s += " "+C.get(i).toString();
    }
    return s;
  }

}