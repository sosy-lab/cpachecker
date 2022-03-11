// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.Objects;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

interface AutomatonExpression<T> {

  ResultValue<T> eval(AutomatonExpressionArguments pArgs) throws CPATransferException;

  static class StringExpression implements AutomatonExpression<String> {
    private String toPrint;

    public StringExpression(String pString) {
      toPrint = pString;
    }

    @Override
    public ResultValue<String> eval(AutomatonExpressionArguments pArgs) {
      // replace $rawstatement
      String str =
          toPrint.replaceAll("\\$[rR]aw[Ss]tatement", pArgs.getCfaEdge().getRawStatement());
      // replace $line
      str = str.replaceAll("\\$[Ll]ine", String.valueOf(pArgs.getCfaEdge().getLineNumber()));
      // replace $location
      str = str.replaceAll("\\$[Ll]ocation", pArgs.getCfaEdge().getFileLocation().toString());
      // replace $file
      str =
          str.replaceAll(
              "\\$[Ff]ile", pArgs.getCfaEdge().getFileLocation().getFileName().toString());
      // replace $states
      str = str.replaceAll("\\$[Ss]tates", pArgs.getAbstractStates().toString());
      // replace Transition Variables and AutomatonVariables
      str = pArgs.replaceVariables(str);
      if (str == null) {
        return new ResultValue<>(
            "Failure in Variable Replacement in String \"" + toPrint + "\"", "ActionExpr.Print");
      } else {
        return new ResultValue<>(str);
      }
    }

    @Override
    public String toString() {
      return toPrint; // TODO correct?
    }

    @Override
    public int hashCode() {
      return toPrint.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof StringExpression && toPrint.equals(((StringExpression) o).toPrint);
    }
  }
  /**
   * Sends a query-String to an <code>AbstractState</code> of another analysis and returns the
   * query-Result.
   */
  static class CPAQuery implements AutomatonExpression<String> {
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
        return new ResultValue<>(
            "Failed to modify queryString \"" + queryString + "\"", "AutomatonBoolExpr.CPAQuery");
      }

      for (AbstractState ae : pArgs.getAbstractStates()) {
        if (ae instanceof AbstractQueryableState) {
          AbstractQueryableState aqe = (AbstractQueryableState) ae;
          if (aqe.getCPAName().equals(cpaName)) {
            try {
              Object result = aqe.evaluateProperty(modifiedQueryString);
              return new ResultValue<>(result.toString());
            } catch (InvalidQueryException e) {
              pArgs
                  .getLogger()
                  .logException(
                      Level.WARNING,
                      e,
                      "Automaton encountered an Exception during Query of the "
                          + cpaName
                          + " CPA on Edge "
                          + pArgs.getCfaEdge().getDescription());
              return new ResultValue<>(
                  "Automaton encountered an Exception during Query of the "
                      + cpaName
                      + " CPA on Edge "
                      + pArgs.getCfaEdge().getDescription(),
                  "AutomatonExpression.CPAQuery");
            }
          }
        }
      }
      return new ResultValue<>(
          "No State of CPA \"" + cpaName + "\" was found!", "AutomatonExpression.CPAQuery");
    }

    @Override
    public String toString() {
      return "EVAL(" + cpaName + "(\"" + queryString + "\"))";
    }

    @Override
    public int hashCode() {
      return Objects.hash(cpaName, queryString);
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof CPAQuery) {
        CPAQuery other = (CPAQuery) o;
        return cpaName.equals(other.cpaName) && queryString.equals(other.queryString);
      }
      return false;
    }
  }

  // TODO: lift CPA Query here

  public static class ResultValue<resultType> {
    private boolean canNotEvaluate = false;
    private String failureMessage = null; // only set if cannotEvaluate == true
    private String failureOrigin = null; // only set if cannotEvaluate == true
    private resultType value = null; // only set if cannotEvaluate == false

    public ResultValue(resultType value) {
      this.value = value;
    }

    public ResultValue(String failureMessage, String failureOrigin) {
      this.canNotEvaluate = true;
      this.failureMessage = failureMessage;
      this.failureOrigin = failureOrigin;
    }
    /**
     * Copies the failure messages from the passed result. This Method assumes that the parameter
     * fulfills canNotEvaluate() == true !
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
    /** Return failure message or {@code null} if {@code cannotEvaluate() == false} */
    String getFailureMessage() {
      return failureMessage;
    }
    /** Return failure origin or {@code null} if {@code cannotEvaluate() == false} */
    String getFailureOrigin() {
      return failureOrigin;
    }
    /** Return value or {@code null} if {@code cannotEvaluate() == false} */
    resultType getValue() {
      return value;
    }

    @Override
    public String toString() {
      if (canNotEvaluate()) {
        return String.format("not evaluated (%s)", failureMessage);
      } else {
        return getValue().toString();
      }
    }
  }
}
