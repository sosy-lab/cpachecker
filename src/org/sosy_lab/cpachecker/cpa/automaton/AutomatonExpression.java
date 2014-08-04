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

import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

interface AutomatonExpression {

  ResultValue<?> eval(AutomatonExpressionArguments pArgs) throws CPATransferException;


  static class StringExpression implements AutomatonExpression {
    private String toPrint;
    public StringExpression(String pString) {
      super();
      this.toPrint = pString;
    }
    @Override
    public ResultValue<?> eval(AutomatonExpressionArguments pArgs) {
      // replace $rawstatement
      String str = toPrint.replaceAll("\\$[rR]aw[Ss]tatement", pArgs.getCfaEdge().getRawStatement());
      // replace $line
      str = str.replaceAll("\\$[Ll]ine", String.valueOf(pArgs.getCfaEdge().getLineNumber()));
      // replace Transition Variables and AutomatonVariables
      str = pArgs.replaceVariables(str);
      if (str == null) {
        return new ResultValue<>("Failure in Variable Replacement in String \"" + toPrint + "\"","ActionExpr.Print");
      } else {
        return new ResultValue<>(str);
      }
    }
  }
  /**
   * Sends a query-String to an <code>AbstractState</code> of another analysis and returns the query-Result.
   */
  static class CPAQuery implements AutomatonExpression {
    private final String cpaName;
    private final String queryString;

    public CPAQuery(String pCPAName, String pQuery) {
      cpaName = pCPAName;
      queryString = pQuery;
    }

    @Override
    public ResultValue<String> eval(AutomatonExpressionArguments pArgs) {
      // replace transition variables
      String modifiedQueryString = pArgs.replaceVariables(queryString);
      if (modifiedQueryString == null) {
        return new ResultValue<>("Failed to modify queryString \"" + queryString + "\"", "AutomatonBoolExpr.CPAQuery");
      }

      for (AbstractState ae : pArgs.getAbstractStates()) {
        if (ae instanceof AbstractQueryableState) {
          AbstractQueryableState aqe = (AbstractQueryableState) ae;
          if (aqe.getCPAName().equals(cpaName)) {
            try {
              Object result = aqe.evaluateProperty(modifiedQueryString);
              return new ResultValue<>(result.toString());
            } catch (InvalidQueryException e) {
              pArgs.getLogger().logException(Level.WARNING, e,
                  "Automaton encountered an Exception during Query of the "
                  + cpaName + " CPA on Edge " + pArgs.getCfaEdge().getDescription());
              return new ResultValue<>("Automaton encountered an Exception during Query of the "
                  + cpaName + " CPA on Edge " + pArgs.getCfaEdge().getDescription(), "AutomatonExpression.CPAQuery");
            }
          }
        }
      }
      return new ResultValue<>("No State of CPA \"" + cpaName + "\" was found!", "AutomatonExpression.CPAQuery");
    }

    @Override
    public String toString() {
      return "EVAL(" + cpaName + "(\"" + queryString + "\"))";
    }
  }


  // TODO: lift CPA Query here

  public static class ResultValue<resultType> {
    private boolean canNotEvaluate = false;
    private String failureMessage = null; // only set if cannotEvaluate == true
    private String failureOrigin = null;  // only set if cannotEvaluate == true
    private resultType value = null;      // only set if cannotEvaluate == false
    public ResultValue(resultType value) {
      this.value = value;
    }
    public ResultValue(String failureMessage, String failureOrigin) {
      this.canNotEvaluate = true;
      this.failureMessage = failureMessage;
      this.failureOrigin = failureOrigin;
    }
    /**
     * Copies the failure messages from the passed result.
     * This Method assumes that the parameter fulfills canNotEvaluate() == true !
     * @param pResA
     */
    public ResultValue(ResultValue<?> pRes) {
      assert pRes.canNotEvaluate;
      this.canNotEvaluate = true;
      this.failureMessage = pRes.failureMessage;
      this.failureOrigin = pRes.failureOrigin;
    }
    boolean canNotEvaluate() {
      return this.canNotEvaluate;
    }
    /**
     * @returns null if cannotEvaluate() == false
     */
    String getFailureMessage() {
      return failureMessage;
    }
    /**
     * @returns null if cannotEvaluate() == false
     */
    String getFailureOrigin() {
      return failureOrigin;
    }
    /**
     * @returns null if cannotEvaluate() == true
     */
    resultType getValue() {
      return value;
    }
  }
}
