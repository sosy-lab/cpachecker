// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/**
 * Implements a integer expression that evaluates and returns a <code>int</code> value when <code>
 * eval()</code> is called. The Expression can be evaluated multiple times.
 */
interface AutomatonIntExpr extends AutomatonExpression<Integer> {

  @Override
  ResultValue<Integer> eval(AutomatonExpressionArguments pArgs);

  /** Stores a constant integer. */
  static class Constant implements AutomatonIntExpr {
    private final ResultValue<Integer> constantResult;

    public Constant(int pI) {
      constantResult = new ResultValue<>(pI);
    }

    public Constant(String pI) {
      this(Integer.parseInt(pI));
    }

    public int getIntValue() {
      return constantResult.getValue();
    }

    @Override
    public ResultValue<Integer> eval(AutomatonExpressionArguments pArgs) {
      return constantResult;
    }

    @Override
    public String toString() {
      return constantResult.toString();
    }

    @Override
    public int hashCode() {
      return constantResult.getValue();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Constant
          && constantResult.getValue().equals(((Constant) o).constantResult.getValue());
    }
  }

  /** Loads an {@link AutomatonVariable} from the VariableMap and returns its int value. */
  static class VarAccess implements AutomatonIntExpr {

    private final String varId;

    private static final Pattern TRANSITION_VARS_PATTERN = Pattern.compile("\\$\\d+");

    public VarAccess(String pId) {
      if (pId.startsWith("$$")) {
        // throws a NumberFormatException and this is good!
        Integer.parseInt(pId.substring(2));
      }
      varId = pId;
    }

    @Override
    public ResultValue<Integer> eval(AutomatonExpressionArguments pArgs) {
      if (TRANSITION_VARS_PATTERN.matcher(varId).matches()) { // $1  AutomatonTransitionVariables
        // no exception here (would have come in the constructor)
        int key = Integer.parseInt(varId.substring(1));
        String val = pArgs.getTransitionVariable(key).toASTString();
        if (val == null) {
          return logAndReturn(pArgs, "could not find the transition variable $%s.", key);
        }
        try {
          int value = Integer.parseInt(val);
          return new ResultValue<>(value);
        } catch (NumberFormatException e) {
          return logAndReturn(
              pArgs, "could not parse the contents of transition variable $%s=\"%s\".", key, val);
        }
      } else if (varId.equals("$line")) { // $line  line number in sourcecode
        return new ResultValue<>(pArgs.getCfaEdge().getLineNumber());
      } else {
        AutomatonVariable variable = pArgs.getAutomatonVariables().get(varId);
        if (variable != null) {
          return new ResultValue<>(variable.getValue());
        } else {
          return logAndReturn(pArgs, "could not find the automaton variable %s.", varId);
        }
      }
    }

    /** log a warning and return a failure value. */
    @FormatMethod
    private ResultValue<Integer> logAndReturn(
        AutomatonExpressionArguments pArgs, @FormatString String message, Object... pObjects) {
      pArgs.getLogger().logf(Level.WARNING, message, pObjects);
      return new ResultValue<>(message, "AutomatonIntExpr.VarAccess");
    }

    @Override
    public String toString() {
      return varId;
    }

    @Override
    public int hashCode() {
      return varId.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof VarAccess && varId.equals(((VarAccess) o).varId);
    }
  }

  /**
   * Sends a query-String to an <code>AbstractState</code> of another analysis and returns the
   * query-Result.
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
        return new ResultValue<>(
            "Failed to modify queryString \"" + queryString + "\"", "AutomatonIntExpr.CPAQuery");
      }

      for (AbstractState ae : pArgs.getAbstractStates()) {
        if (ae instanceof AbstractQueryableState) {
          AbstractQueryableState aqe = (AbstractQueryableState) ae;
          if (aqe.getCPAName().equals(cpaName)) {
            try {
              Object result = aqe.evaluateProperty(modifiedQueryString);
              if (result instanceof NumericValue) {
                result = ((NumericValue) result).getNumber();
              }
              String message =
                  String.format(
                      "CPA-Check succeeded: ModifiedCheckString: \"%s\" CPAElement: (%s) \"%s\"",
                      modifiedQueryString, aqe.getCPAName(), aqe);
              if (result instanceof Integer) {
                pArgs.getLogger().log(Level.FINER, message);
                return new ResultValue<>((Integer) result);
              } else if (result instanceof Long) {
                pArgs.getLogger().log(Level.FINER, message);
                return new ResultValue<>(((Long) result).intValue());
              } else {
                String failureMessage =
                    String.format(
                        "Automaton got a non-Numeric value during Query of the %s CPA on Edge %s.",
                        cpaName, pArgs.getCfaEdge().getDescription());
                pArgs.getLogger().log(Level.WARNING, failureMessage);
                return new ResultValue<>(failureMessage, "AutomatonIntExpr.CPAQuery");
              }
            } catch (InvalidQueryException e) {
              String errorMessage =
                  String.format(
                      "Automaton encountered an Exception during Query of the %s CPA on Edge %s.",
                      cpaName, pArgs.getCfaEdge().getDescription());
              pArgs.getLogger().logException(Level.WARNING, e, errorMessage);
              return new ResultValue<>(errorMessage, "AutomatonIntExpr.CPAQuery");
            }
          }
        }
      }
      String cpaNotAvailableMessage =
          String.format(
              "Did not find the CPA to be queried %s CPA on Edge %s.",
              cpaName, pArgs.getCfaEdge().getDescription());
      pArgs.getLogger().log(Level.WARNING, cpaNotAvailableMessage);
      return new ResultValue<>(cpaNotAvailableMessage, "AutomatonIntExpr.CPAQuery");
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

  static class BinaryAutomatonIntExpr implements AutomatonIntExpr {

    private final AutomatonIntExpr a;
    private final AutomatonIntExpr b;
    private final BiFunction<Integer, Integer, Integer> op;
    private final String repr;

    private BinaryAutomatonIntExpr(
        AutomatonIntExpr pA,
        AutomatonIntExpr pB,
        BiFunction<Integer, Integer, Integer> pOp,
        String pRepr) {
      a = pA;
      b = pB;
      op = pOp;
      repr = pRepr;
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
      return new ResultValue<>(op.apply(resA.getValue(), resB.getValue()));
    }

    @Override
    public String toString() {
      return String.format("(%s %s %s)", a, repr, b);
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b, repr);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o instanceof BinaryAutomatonIntExpr) {
        BinaryAutomatonIntExpr other = (BinaryAutomatonIntExpr) o;
        return a.equals(other.a) && b.equals(other.b) && repr.equals(other.repr);
      }
      return false;
    }
  }

  /** Addition of {@link AutomatonIntExpr} instances. */
  static class Plus extends BinaryAutomatonIntExpr {
    public Plus(AutomatonIntExpr pA, AutomatonIntExpr pB) {
      super(pA, pB, (a, b) -> a + b, "+");
    }
  }

  /** Subtraction of {@link AutomatonIntExpr} instances. */
  static class Minus extends BinaryAutomatonIntExpr {
    public Minus(AutomatonIntExpr pA, AutomatonIntExpr pB) {
      super(pA, pB, (a, b) -> a - b, "-");
    }
  }
}
