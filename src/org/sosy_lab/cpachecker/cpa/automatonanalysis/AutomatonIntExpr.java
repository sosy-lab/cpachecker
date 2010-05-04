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
package org.sosy_lab.cpachecker.cpa.automatonanalysis;

import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Implements a integer expression that evaluates and returns a <code>int</code> value when <code>eval()</code> is called.
 * The Expression can be evaluated multiple times.
 * @author rhein
 */
abstract class AutomatonIntExpr {

  private AutomatonIntExpr() {} //nobody can use this

  abstract int eval(AutomatonExpressionArguments pArgs);


  /** Stores a constant integer.
   * @author rhein
   */
  static class Constant extends AutomatonIntExpr {
    int i;
    public Constant(int pI) {this.i = pI; }
    public Constant(String pI) {this.i = Integer.parseInt(pI); }
    public int eval() {return i;}
    @Override public int eval(AutomatonExpressionArguments pArgs) {return i; }
  }


  /** Loads an {@link AutomatonVariable} from the VariableMap and returns its int value.
   * @author rhein
   */
  static class VarAccess extends AutomatonIntExpr {

    private final String varId;

    private static Pattern TRANSITION_VARS_PATTERN = Pattern.compile("\\$\\d+");

    public VarAccess(String pId) {
      if (pId.startsWith("$$")) {
        // throws a NumberFormatException and this is good!
        Integer.parseInt(pId.substring(2));
      }
      this.varId = pId;
    }

    @Override
    public int eval(AutomatonExpressionArguments pArgs) {
      if (TRANSITION_VARS_PATTERN.matcher(varId).matches()) { // $1  AutomatonTransitionVariables
        // no exception here (would have come in the constructor)
        int key = Integer.parseInt(varId.substring(1));
        String val = pArgs.getTransitionVariable(key);
        if (val == null) {
          pArgs.getLogger().log(Level.WARNING, "could not find the transition variable $" + key + ". Returning -1.");
          return -1;
        }
        int value = -1;
        try {
          value = Integer.parseInt(val);
        } catch (NumberFormatException e) {
          pArgs.getLogger().log(Level.WARNING, "could not parse the contents of variable $" + key + "=\"" + val +"\". Returning -1.");
          return -1;
        }
        return value;
      } else if (varId.equals("$line")) { // $line  line number in sourcecode
        return pArgs.getCfaEdge().getLineNumber();
      } else {
        return pArgs.getAutomatonVariables().get(varId).getValue(); // only ints supported so far
      }
    }

    @Override
    public String toString() {
      return varId;
    }
  }


  /** Addition of {@link AutomatonIntExpr} instances.
   * @author rhein
   */
  static class Plus extends AutomatonIntExpr {

    private final AutomatonIntExpr a;
    private final AutomatonIntExpr b;

    public Plus(AutomatonIntExpr pA, AutomatonIntExpr pB) {
      this.a = pA;
      this.b = pB;
    }

    @Override
    public int eval(AutomatonExpressionArguments pArgs) {
      return a.eval(pArgs) + b.eval(pArgs);
    }

    @Override
    public String toString() {
      return "(" + a + " + " + b + ")";
    }
  }


  /** Subtraction of {@link AutomatonIntExpr} instances.
   * @author rhein
   */
  static class Minus extends AutomatonIntExpr {

    private final AutomatonIntExpr a;
    private final AutomatonIntExpr b;

    public Minus(AutomatonIntExpr pA, AutomatonIntExpr pB) {
      this.a = pA;
      this.b = pB;
    }

    @Override
    public int eval(AutomatonExpressionArguments pArgs) {
      return a.eval(pArgs) - b.eval(pArgs);
    }

    @Override
    public String toString() {
      return "(" + a + " - " + b + ")";
    }
  }
}