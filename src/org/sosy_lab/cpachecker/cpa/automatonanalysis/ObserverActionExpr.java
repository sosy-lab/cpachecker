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
package org.sosy_lab.cpachecker.cpa.observeranalysis;

import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableElement;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;


/**
 * Implements an Action with side-effects that has no return value.
 * The Action can be executed multiple times.
 */
abstract class ObserverActionExpr {
  private ObserverActionExpr() {};
  
  /**
   * Returns if the action can execute on the given ObserverExpressionArguments.
   * If it cannot execute this is probably because of missing AbstractElements (from other CPAs).
   * @param pArgs
   * @return
   */
  boolean canExecuteOn(ObserverExpressionArguments pArgs) {
    return true;
  }
  abstract void execute(ObserverExpressionArguments pArgs);

  /**
   * Prints a string to System.out when executed.
   * @author rhein
   */
  static class Print extends ObserverActionExpr {
    // the pattern \$\d+ matches Expressions like $1 $2 $3
    static Pattern TRANSITION_VARS_PATTERN = Pattern.compile("\\$\\d+");
    private String toPrint;
    public Print(String pToPrint) { toPrint = pToPrint; }
    @Override void execute(ObserverExpressionArguments pArgs) {
      // replace $rawstatement
      String str = toPrint.replaceAll("\\$[rR]aw[Ss]tatement", pArgs.getCfaEdge().getRawStatement());
      // replace $line
      str = str.replaceAll("\\$[Ll]ine", String.valueOf(pArgs.getCfaEdge().getLineNumber()));
      // replace Transition Variables and observerVariables
      str = pArgs.replaceVariables(str);
      pArgs.appendToLogMessage(str.toString());
    }
  }

  /**
   * Prints the value of an ObserverIntExpr
   * @author rhein
   */
  static class PrintInt extends ObserverActionExpr {
    private ObserverIntExpr toPrint;
    public PrintInt(ObserverIntExpr pIntExpr) {
      toPrint = pIntExpr;
    }
    @Override void execute(ObserverExpressionArguments pArgs) {
      pArgs.appendToLogMessage(toPrint.eval(pArgs));
    }
  }

  /** Assigns the value of a ObserverIntExpr to a ObserverVariable determined by its name.
   * @author rhein
   */
  static class Assignment extends ObserverActionExpr {
    private String varId;
    private ObserverIntExpr var;
    public Assignment(String pVarId, ObserverIntExpr pVar) {
      this.varId = pVarId;
      this.var = pVar;
    }
    @Override  void execute(ObserverExpressionArguments pArgs) {
      Map<String, ObserverVariable> vars = pArgs.getObserverVariables();
      if (vars.containsKey(varId)) {
        vars.get(varId).setValue(var.eval(pArgs));
      } else {
        ObserverVariable newVar = new ObserverVariable("int", varId);
        newVar.setValue(var.eval(pArgs));
        vars.put(varId, newVar);
        pArgs.getLogger().log(Level.WARNING, "Defined a Variable " + varId + " that was unknown before (not set in automaton Definition).");
      }
    }
  }
  /**
   * Modifies the state of a CPA
   * @author rhein
   *
   */
  static class CPAModification extends ObserverActionExpr {
    private final String cpaName;
    private final String modificationString;

    public CPAModification(String pCPAName, String pModification) {
      cpaName = pCPAName;
      modificationString = pModification;
    }
    @Override
    boolean canExecuteOn(ObserverExpressionArguments pArgs) {
      for (AbstractElement ae : pArgs.getAbstractElements()) {
        if (ae instanceof AbstractQueryableElement) {
          AbstractQueryableElement aqe = (AbstractQueryableElement) ae;
          if (aqe.getCPAName().equals(cpaName)) {
            return true;
          }
        }
      }
      return false;
    }
    @Override
    void execute(ObserverExpressionArguments pArgs) {
      // replace transition variables
      String processedModificationString = pArgs.replaceVariables(modificationString);
      if (processedModificationString == null) {
        pArgs.getLogger().log(Level.WARNING, "Modification String \"" + modificationString + "\" could not be processed (Variable not found).");
        return;
      }
      for (AbstractElement ae : pArgs.getAbstractElements()) {
        if (ae instanceof AbstractQueryableElement) {
          AbstractQueryableElement aqe = (AbstractQueryableElement) ae;
          if (aqe.getCPAName().equals(cpaName)) {
            try {
              aqe.modifyProperty(processedModificationString);
            } catch (InvalidQueryException e) {
              pArgs.getLogger().logException(Level.WARNING, e,
                  "ObserverAutomaton encountered an Exception during Query of the "
                  + cpaName + " CPA (Element " + aqe.toString() + ") on Edge " + pArgs.getCfaEdge().getRawStatement());
            }
          }
        }
      }
    }

    @Override
    public String toString() {
      return "MODIFY(" + cpaName + "(\"" + modificationString + "\"))";
    }

  }
}
