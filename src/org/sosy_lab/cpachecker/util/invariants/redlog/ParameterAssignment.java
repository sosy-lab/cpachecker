/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.invariants.redlog;

import java.util.HashSet;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.util.invariants.Rational;

public class ParameterAssignment {

  private String parameter = null;
  //private CExpression tree = null;
  private PAType patype = null;
  private Rational value = null;
  private HashSet<String> vars = null;

  public ParameterAssignment(String p) {
    // Set p to be the parameter name, and 0 to be the value.
    parameter = p;
    patype = PAType.NONE;
    try {
      value = new Rational(0, 1);
    } catch (Exception e) {}
  }

  public ParameterAssignment(String p, CExpression RHS) {
    // Parameter name p, and RHS for right-hand side of assignment.
    parameter = p;
    //tree = RHS;
    patype = PAType.CONST;
    Substitution S = new Substitution();
    // Perhaps redlog returns an unsimplified rational
    // expression. Perhaps this is never so; we should ask a
    // redlog expert. For now, we use our evaluate function in
    // case it is unsimplified.
    Rational R = TreeReader.evaluate(RHS, S);
    value = R;
  }

  // TODO: Repair the TreeReader class, and then reactivate the
  // checking features of this constructor, which rely on it.
  /**
  public ParameterAssignment(String p, CExpression RHS) {
    // Parameter name p, and RHS for right-hand side of assignment.
    parameter = p;
    tree = RHS;
    HashSet<String> vars = TreeReader.getAllVars(RHS);
    this.vars = vars;
    if (vars.isEmpty()) {
      patype = PAType.CONST;
      Substitution S = new Substitution();
      // All we know is that there are no variables. Still,
      // perhaps redlog returns an unsimplified rational
      // expression. Perhaps this is never so; we should ask a
      // redlog expert. For now, we use our evaluate function in
      // case it is unsimplified.
      Rational R = TreeReader.evaluate(RHS, S);
      value = R;
    } else {
      patype = PAType.VAR;
    }
  }
  */

  public boolean hasValue() {
    return (value!=null);
  }

  public String getParameter() {
     return parameter;
  }

  public Rational getValue() {
    return value;
  }

  public PAType getType() {
    return patype;
  }

  public HashSet<String> getVars() {
    return vars;
  }

  // To be reactivated after TreeReader has been repaired.
  /**
  public String valueToString() {
    String s = "None";
    if (value != null) {
      s = value.toString();
    } else if (tree != null) {
      s = TreeReader.read(tree);
    }
    return s;
  }
  */

  public static enum PAType {
    NONE,
    CONST,
    VAR,
    ;
  }

}
