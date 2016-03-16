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

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;


/**
 * Implements an Action with side-effects that has no return value.
 * The Action can be executed multiple times.
 */
abstract class AutomatonAction {
  private AutomatonAction() {}
  private static ResultValue<String> defaultResultValue = new ResultValue<>("");

  // in this method the Value inside the resultValueObject is not important (most ActionClasses will return "" as inner value)
  // more important is if the action was evaluated (ResultValue.canNotEvaluate())
  abstract ResultValue<?> eval(AutomatonExpressionArguments pArgs) throws CPATransferException;

  /**
   * Returns if the action can execute on the given AutomatonExpressionArguments.
   * If it cannot execute this is probably because of missing AbstractStates (from other CPAs).
   * @param pArgs the arguments that should be used for execution
   * @throws CPATransferException may be thrown in subclasses
   */
  boolean canExecuteOn(AutomatonExpressionArguments pArgs) throws CPATransferException {
    return true;
  }
  //abstract void execute(AutomatonExpressionArguments pArgs);

  /**
   * Logs a String when executed.
   */
  static class Print extends AutomatonAction {
    private List<AutomatonExpression> toPrint;

    public Print(List<AutomatonExpression> pArgs) { toPrint = pArgs; }

    @Override
    boolean canExecuteOn(AutomatonExpressionArguments pArgs) throws CPATransferException {
      // TODO: every action is computed twice (once here, once in eval)
      for (AutomatonExpression expr : toPrint) {
        ResultValue<?> res = expr.eval(pArgs);
        if (res.canNotEvaluate()) {
          return false;
        }
      }
      return true;
    }
    @Override ResultValue<?> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      StringBuilder sb = new StringBuilder();
      for (AutomatonExpression expr : toPrint) {
        ResultValue<?> res = expr.eval(pArgs);
        if (res.canNotEvaluate()) {
          return res;
        } else {
          sb.append(res.getValue().toString());
        }
      }
      pArgs.appendToLogMessage(sb.toString());
      return defaultResultValue;
    }
  }


  /** Assigns the value of a AutomatonIntExpr to a AutomatonVariable determined by its name.
   */
  static class Assignment extends AutomatonAction {
    private String varId;
    private AutomatonIntExpr var;
    public Assignment(String pVarId, AutomatonIntExpr pVar) {
      this.varId = pVarId;
      this.var = pVar;
    }
    @Override
    boolean canExecuteOn(AutomatonExpressionArguments pArgs) {
      return ! var.eval(pArgs).canNotEvaluate();
    }
    @Override  ResultValue<?> eval(AutomatonExpressionArguments pArgs) {
      ResultValue<Integer> res = var.eval(pArgs);
      if (res.canNotEvaluate()) {
        return res;
      }
      Map<String, AutomatonVariable> vars = pArgs.getAutomatonVariables();
      if (vars.containsKey(varId)) {
        vars.get(varId).setValue(res.getValue());
      } else {
        AutomatonVariable newVar = new AutomatonVariable("int", varId);
        newVar.setValue(res.getValue());
        vars.put(varId, newVar);
        pArgs.getLogger().log(Level.WARNING, "Defined a Variable " + varId + " that was unknown before (not set in automaton Definition).");
      }
      return defaultResultValue;
    }
  }

  /**
   * Modifies the state of a CPA
   */
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
        pArgs.getLogger().log(Level.WARNING, "Modification String \"" + modificationString + "\" could not be processed (Variable not found).");
        return new ResultValue<>("Modification String \"" + modificationString + "\" could not be processed (Variable not found).", "AutomatonActionExpr.CPAModification");
      }
      for (AbstractState ae : pArgs.getAbstractStates()) {
        if (ae instanceof AbstractQueryableState) {
          AbstractQueryableState aqe = (AbstractQueryableState) ae;
          if (aqe.getCPAName().equals(cpaName)) {
            try {
              aqe.modifyProperty(processedModificationString);
            } catch (InvalidQueryException e) {
              pArgs.getLogger().logException(Level.WARNING, e,
                  "Automaton encountered an Exception during Query of the "
                  + cpaName + " CPA (Element " + aqe.toString() + ") on Edge " + pArgs.getCfaEdge().getDescription());
              return defaultResultValue; // try to carry on with the further evaluation
            }
          }
        }
      }
      return new ResultValue<>("Did not find an element of the CPA \"" + cpaName + "\" to be modified.", "AutomatonActionExpr.CPAModification");
    }

    @Override
    public String toString() {
      return "MODIFY(" + cpaName + "(\"" + modificationString + "\"))";
    }

  }
}
