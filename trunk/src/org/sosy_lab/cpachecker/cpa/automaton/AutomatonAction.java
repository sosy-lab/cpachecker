// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable.AutomatonIntVariable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable.AutomatonSetVariable;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/**
 * Implements an Action with side-effects that has no return value. The Action can be executed
 * multiple times.
 */
abstract class AutomatonAction {
  private AutomatonAction() {}

  private static ResultValue<String> defaultResultValue = new ResultValue<>("");

  // in this method the Value inside the resultValueObject is not important (most ActionClasses will
  // return "" as inner value)
  // more important is if the action was evaluated (ResultValue.canNotEvaluate())
  abstract ResultValue<?> eval(AutomatonExpressionArguments pArgs) throws CPATransferException;

  /**
   * Returns if the action can execute on the given AutomatonExpressionArguments. If it cannot
   * execute this is probably because of missing AbstractStates (from other CPAs).
   *
   * @param pArgs the arguments that should be used for execution
   * @throws CPATransferException may be thrown in subclasses
   */
  boolean canExecuteOn(AutomatonExpressionArguments pArgs) throws CPATransferException {
    return true;
  }
  // abstract void execute(AutomatonExpressionArguments pArgs);

  /** Logs a String when executed. */
  static class Print extends AutomatonAction {
    protected final List<AutomatonExpression<?>> toPrint;

    public Print(List<AutomatonExpression<?>> pArgs) {
      toPrint = pArgs;
    }

    @Override
    boolean canExecuteOn(AutomatonExpressionArguments pArgs) throws CPATransferException {
      // TODO: every action is computed twice (once here, once in eval)
      for (AutomatonExpression<?> expr : toPrint) {
        ResultValue<?> res = expr.eval(pArgs);
        if (res.canNotEvaluate()) {
          return false;
        }
      }
      return true;
    }

    @Override
    ResultValue<?> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      StringBuilder sb = new StringBuilder();
      for (AutomatonExpression<?> expr : toPrint) {
        ResultValue<?> res = expr.eval(pArgs);
        if (res.canNotEvaluate()) {
          return res;
        } else {
          sb.append(res.getValue().toString());
        }
      }
      print(pArgs, sb.toString());
      return defaultResultValue;
    }

    protected void print(AutomatonExpressionArguments pArgs, String s) {
      pArgs.appendToLogMessage(s);
    }

    @Override
    public String toString() {
      return "PRINT \"" + Joiner.on("\" \"").join(toPrint) + "\"";
    }
  }

  /** Logs a String when executed. Prints each String only once. */
  static class PrintOnce extends Print {
    private final Set<String> alreadyPrintedMessages = Sets.newConcurrentHashSet();

    public PrintOnce(List<AutomatonExpression<?>> pArgs) {
      super(pArgs);
    }

    @Override
    protected void print(AutomatonExpressionArguments pArgs, String s) {
      if (alreadyPrintedMessages.add(s)) {
        pArgs.appendToLogMessage(s);
      }
    }

    @Override
    public String toString() {
      return "PRINTONCE \"" + Joiner.on("\" \"").join(toPrint) + "\"";
    }
  }

  /** Assigns the value of a AutomatonIntExpr to a AutomatonVariable determined by its name. */
  static class Assignment extends AutomatonAction {
    private final String varId;
    private final AutomatonIntExpr var;

    public Assignment(String pVarId, AutomatonIntExpr pVar) {
      varId = pVarId;
      var = pVar;
    }

    @Override
    boolean canExecuteOn(AutomatonExpressionArguments pArgs) {
      return !var.eval(pArgs).canNotEvaluate();
    }

    @Override
    ResultValue<?> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      ResultValue<Integer> res = var.eval(pArgs);
      if (res.canNotEvaluate()) {
        return res;
      }
      Map<String, AutomatonVariable> vars = pArgs.getAutomatonVariables();
      if (vars.containsKey(varId)) {
        AutomatonVariable automatonVariable = vars.get(varId);
        if (automatonVariable instanceof AutomatonIntVariable) {
          ((AutomatonIntVariable) automatonVariable).setValue(res.getValue());
        } else {
          throw new CPATransferException(
              "Cannot assign integer expression to variable '" + automatonVariable.getName() + "'");
        }
      } else {
        throw new CPATransferException("Automaton variable '" + varId + "' does not exist");
      }
      return defaultResultValue;
    }

    @Override
    public String toString() {
      return String.format("DO %s=%s", varId, var);
    }
  }

  /** Change the value of a AutomatonSetVariable by adding or removing values. */
  static class SetAssignment extends AutomatonAction {
    private final String varId;
    private final boolean action;
    private final String value;

    public SetAssignment(String pVarId, String pValue, boolean pAction) {
      varId = pVarId;
      action = pAction;
      value = pValue;
    }

    @Override
    boolean canExecuteOn(AutomatonExpressionArguments pArgs) {
      return true;
    }

    @Override
    ResultValue<?> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      Map<String, AutomatonVariable> vars = pArgs.getAutomatonVariables();
      if (vars.containsKey(varId)) {
        AutomatonVariable automatonVariable = vars.get(varId);
        if (automatonVariable instanceof AutomatonSetVariable) {
          String substitutedValue = pArgs.replaceVariables(value);
          if (action) {
            ((AutomatonSetVariable<?>) automatonVariable).add(substitutedValue);
          } else {
            ((AutomatonSetVariable<?>) automatonVariable).remove(substitutedValue);
          }
        } else {
          throw new CPATransferException(
              "Automaton variable '"
                  + automatonVariable.getName()
                  + "' cannot be used in set modifications");
        }
      } else {
        throw new CPATransferException("Automaton variable '\" + varId + \"' does not exist");
      }
      return defaultResultValue;
    }

    @Override
    public String toString() {
      return String.format("DO %s[%s]=%s", varId, value, action);
    }
  }

  /** Modifies the state of a CPA */
  static class CPAModification extends AutomatonAction {
    private final String cpaName;
    private final String modificationString;

    public CPAModification(String pCPAName, String pModification) {
      cpaName = pCPAName;
      modificationString = pModification;
    }

    @Override
    boolean canExecuteOn(AutomatonExpressionArguments pArgs) {
      if (pArgs.replaceVariables(modificationString) == null) {
        return false;
      }
      for (AbstractState ae : pArgs.getAbstractStates()) {
        if (ae instanceof AbstractQueryableState) {
          AbstractQueryableState aqe = (AbstractQueryableState) ae;
          if (aqe.getCPAName().equals(cpaName)) {
            return true;
          }
        }
      }
      return false;
    }

    @Override
    ResultValue<?> eval(AutomatonExpressionArguments pArgs) {
      // replace transition variables
      String processedModificationString = pArgs.replaceVariables(modificationString);
      if (processedModificationString == null) {
        pArgs
            .getLogger()
            .log(
                Level.WARNING,
                "Modification String \""
                    + modificationString
                    + "\" could not be processed (Variable not found).");
        return new ResultValue<>(
            "Modification String \""
                + modificationString
                + "\" could not be processed (Variable not found).",
            "AutomatonActionExpr.CPAModification");
      }
      for (AbstractState ae : pArgs.getAbstractStates()) {
        if (ae instanceof AbstractQueryableState) {
          AbstractQueryableState aqe = (AbstractQueryableState) ae;
          if (aqe.getCPAName().equals(cpaName)) {
            try {
              aqe.modifyProperty(processedModificationString);
            } catch (InvalidQueryException e) {
              pArgs
                  .getLogger()
                  .logException(
                      Level.WARNING,
                      e,
                      "Automaton encountered an Exception during Query of the "
                          + cpaName
                          + " CPA (Element "
                          + aqe
                          + ") on Edge "
                          + pArgs.getCfaEdge().getDescription());
              return defaultResultValue; // try to carry on with the further evaluation
            }
          }
        }
      }
      return new ResultValue<>(
          "Did not find an element of the CPA \"" + cpaName + "\" to be modified.",
          "AutomatonActionExpr.CPAModification");
    }

    @Override
    public String toString() {
      return "MODIFY(" + cpaName + "(\"" + modificationString + "\"))";
    }
  }
}
