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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.logging.Level;
import java.util.regex.Pattern;

import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/**
 * Implements a integer expression that evaluates and returns a <code>int</code> value when <code>eval()</code> is called.
 * The Expression can be evaluated multiple times.
 */
interface AutomatonIntExpr extends AutomatonExpression {

  @Override
  abstract ResultValue<Integer> eval(AutomatonExpressionArguments pArgs);

  /** Stores a constant integer.
   */
  static class Constant implements AutomatonIntExpr {
    private final ResultValue<Integer> constantResult;
    public Constant(int pI) {this.constantResult = new ResultValue<>(Integer.valueOf(pI)); }
    public Constant(String pI) {this(Integer.parseInt(pI)); }
    public int getIntValue() {
      return constantResult.getValue().intValue();
    }
    @Override
    public ResultValue<Integer> eval(AutomatonExpressionArguments pArgs) {return constantResult;}
  }


  /** Loads an {@link AutomatonVariable} from the VariableMap and returns its int value.
   */
  static class VarAccess implements AutomatonIntExpr {

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
    public ResultValue<Integer> eval(AutomatonExpressionArguments pArgs) {
      if (TRANSITION_VARS_PATTERN.matcher(varId).matches()) { // $1  AutomatonTransitionVariables
        // no exception here (would have come in the constructor)
        int key = Integer.parseInt(varId.substring(1));
        String val = pArgs.getTransitionVariable(key);
        if (val == null) {
          pArgs.getLogger().log(Level.WARNING, "could not find the transition variable $" + key + ".");
          return new ResultValue<>("could not find the transition variable $" + key + ".", "AutomatonIntExpr.VarAccess");
        }
        try {
          int value = Integer.parseInt(val);
          return new ResultValue<>(Integer.valueOf(value));
        } catch (NumberFormatException e) {
          pArgs.getLogger().log(Level.WARNING, "could not parse the contents of transition variable $" + key + "=\"" + val +"\".");
          return new ResultValue<>("could not parse the contents of transition variable $" + key + "=\"" + val +"\".", "AutomatonIntExpr.VarAccess");
        }
      } else if (varId.equals("$line")) { // $line  line number in sourcecode
        return new ResultValue<>(Integer.valueOf(pArgs.getCfaEdge().getLineNumber()));
      } else {
        AutomatonVariable variable = pArgs.getAutomatonVariables().get(varId);
        if (variable != null) {
          return new ResultValue<>(Integer.valueOf(variable.getValue()));
        } else {
          pArgs.getLogger().log(Level.WARNING, "could not find the automaton variable " + varId + ".");
          return new ResultValue<>("could not find the automaton variable " + varId + ".", "AutomatonIntExpr.VarAccess");
        }
      }
    }

    @Override
    public String toString() {
      return varId;
    }
  }

  /**
   * Sends a query-String to an <code>AbstractState</code> of another analysis and returns the query-Result.
   */
  static class CPAQuery implements AutomatonIntExpr {
    private final String cpaName;
    private final String queryString;

    public CPAQuery(String pCPAName, String pQuery) {
      cpaName = pCPAName;
      queryString = pQuery;
    }
    @Override
    public ResultValue<Integer> eval(AutomatonExpressionArguments pArgs) {
      // replace transition variables
      String modifiedQueryString = pArgs.replaceVariables(queryString);
      if (modifiedQueryString == null) {
        return new ResultValue<>("Failed to modify queryString \"" + queryString + "\"", "AutomatonIntExpr.CPAQuery");
      }

      for (AbstractState ae : pArgs.getAbstractStates()) {
        if (ae instanceof AbstractQueryableState) {
          AbstractQueryableState aqe = (AbstractQueryableState) ae;
          if (aqe.getCPAName().equals(cpaName)) {
            try {
              Object result = aqe.evaluateProperty(modifiedQueryString);
              if (result instanceof Integer) {
                  String message = "CPA-Check succeeded: ModifiedCheckString: \"" +
                  modifiedQueryString + "\" CPAElement: (" + aqe.getCPAName() + ") \"" +
                  aqe.toString() + "\"";
                  pArgs.getLogger().log(Level.FINER, message);
                  return new ResultValue<>((Integer)result);
              } else if (result instanceof Long) {
                String message = "CPA-Check succeeded: ModifiedCheckString: \"" +
                modifiedQueryString + "\" CPAElement: (" + aqe.getCPAName() + ") \"" +
                aqe.toString() + "\"";
                pArgs.getLogger().log(Level.FINER, message);
                return new ResultValue<>(((Long)result).intValue());
              } else {
                pArgs.getLogger().log(Level.WARNING,
                    "Automaton got a non-Numeric value during Query of the "
                    + cpaName + " CPA on Edge " + pArgs.getCfaEdge().getDescription() +
                    ".");
                return new ResultValue<>("Automaton got a non-Numeric value during Query of the "
                    + cpaName + " CPA on Edge " + pArgs.getCfaEdge().getDescription() +
                    ".", "AutomatonIntExpr.CPAQuery");
              }
            } catch (InvalidQueryException e) {
              pArgs.getLogger().logException(Level.WARNING, e,
                  "Automaton encountered an Exception during Query of the "
                  + cpaName + " CPA on Edge " + pArgs.getCfaEdge().getDescription() +
                ".");
              return new ResultValue<>("Automaton encountered an Exception during Query of the "
                  + cpaName + " CPA on Edge " + pArgs.getCfaEdge().getDescription() +
                  ".", "AutomatonIntExpr.CPAQuery");
            }
          }
        }
      }
      pArgs.getLogger().log(Level.WARNING,
          "Did not find the CPA to be queried "
          + cpaName + " CPA on Edge " + pArgs.getCfaEdge().getDescription() +
        ".");
      return new ResultValue<>("Did not find the CPA to be queried "
          + cpaName + " CPA on Edge " + pArgs.getCfaEdge().getDescription() +
          ".", "AutomatonIntExpr.CPAQuery");
    }
  }
  /** Addition of {@link AutomatonIntExpr} instances.
   */
  static class Plus implements AutomatonIntExpr {

    private final AutomatonIntExpr a;
    private final AutomatonIntExpr b;

    public Plus(AutomatonIntExpr pA, AutomatonIntExpr pB) {
      this.a = pA;
      this.b = pB;
    }
    @Override
    public ResultValue<Integer> eval(AutomatonExpressionArguments pArgs) {
      ResultValue<Integer> resA = a.eval(pArgs);
      if (resA.canNotEvaluate()) {
        return resA;
      }
      ResultValue<Integer> resB = b.eval(pArgs);
      if (resB.canNotEvaluate()) {
        return resB;
      }
      return new ResultValue<>(resA.getValue() + resB.getValue());
    }

    @Override
    public String toString() {
      return "(" + a + " + " + b + ")";
    }
  }

  /** Subtraction of {@link AutomatonIntExpr} instances.
   */
  static class Minus implements AutomatonIntExpr {

    private final AutomatonIntExpr a;
    private final AutomatonIntExpr b;

    public Minus(AutomatonIntExpr pA, AutomatonIntExpr pB) {
      this.a = pA;
      this.b = pB;
    }
    @Override
    public ResultValue<Integer> eval(AutomatonExpressionArguments pArgs) {
      ResultValue<Integer> resA = a.eval(pArgs);
      if (resA.canNotEvaluate()) {
        return resA;
      }
      ResultValue<Integer> resB = b.eval(pArgs);
      if (resB.canNotEvaluate()) {
        return resB;
      }
      return new ResultValue<>(resA.getValue() - resB.getValue());
    }

    @Override
    public String toString() {
      return "(" + a + " - " + b + ")";
    }
  }
}