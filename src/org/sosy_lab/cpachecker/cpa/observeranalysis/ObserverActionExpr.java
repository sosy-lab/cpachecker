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
import java.util.regex.Matcher;
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
      // replace Transition Variables
      Matcher matcher = TRANSITION_VARS_PATTERN.matcher(str);
      StringBuffer result = new StringBuffer();
      while (matcher.find()) {
        matcher.appendReplacement(result, "");
        String key = str.substring(matcher.start()+1, matcher.end());
        try {
          int varKey = Integer.parseInt(key);
          String var = pArgs.getTransitionVariable(varKey);
          if (var == null) {
            // this variable has not been set.
            pArgs.getLogger().log(Level.WARNING, "could not replace the transition variable $" + varKey + " (not found).");
            return;
          } else {
            result.append(var);
          }
        } catch (NumberFormatException e) {
          pArgs.getLogger().log(Level.WARNING, "could not parse the int in " + matcher.group() + " , leaving it untouched");
          result.append(matcher.group());
        }
      }
      matcher.appendTail(result);
      pArgs.appendToLogMessage(result.toString());
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

    // the pattern \$\d+ matches Expressions like $1 $2 $3
    private static Pattern TRANSITION_VARS_PATTERN = Pattern.compile("\\$\\d+");

    // the pattern \$\$\d+ matches Expressions like $$x $$y23rinnksd $$observerVar (all terminated by a non-word-character)
    private static Pattern OBSERVER_VARS_PATTERN = Pattern.compile("\\$\\$[a-zA-Z]\\w*");

    public CPAModification(String pCPAName, String pModification) {
      cpaName = pCPAName;
      modificationString = pModification;
    }

    @Override
    void execute(ObserverExpressionArguments pArgs) {
      // replace transition variables
      String processedModificationString = replaceVariables(pArgs, modificationString);
      if (processedModificationString == null) {
        pArgs.getLogger().log(Level.WARNING, "Modification String \"" + modificationString + "\" could not be processed (Variable not found).");
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

    /**
     * This method replaces all references to
     * 1. ObserverVariables (referenced by $$<Name-of-Variable>)
     * 2. TransitionVariables (referenced by $<Number-of-Variable>)
     * with the values of the variables.
     * If the variable is not found the function returns null.
     * @param pArgs
     * @param pQueryString
     * @return
     */
    static String replaceVariables (
        ObserverExpressionArguments pArgs, String pQueryString) {

      // replace references to Transition Variables
      Matcher matcher = TRANSITION_VARS_PATTERN.matcher(pQueryString);
      StringBuffer result = new StringBuffer();
      while (matcher.find()) {
        matcher.appendReplacement(result, "");
        String key = matcher.group().substring(1); // matched string startswith $
        try {
          int varKey = Integer.parseInt(key);
          String var = pArgs.getTransitionVariable(varKey);
          if (var == null) {
            // this variable has not been set.
            pArgs.getLogger().log(Level.WARNING, "could not replace the transition variable $" + varKey + " (not found).");
            return null;
          } else {
            result.append(var);
          }
        } catch (NumberFormatException e) {
          pArgs.getLogger().log(Level.WARNING, "could not parse the int in " + matcher.group() + " , leaving it untouched");
          result.append(matcher.group());
        }
      }
      matcher.appendTail(result);

      // replace references to observer Variables
      matcher = OBSERVER_VARS_PATTERN.matcher(result.toString());
      result = new StringBuffer();
      while (matcher.find()) {
        matcher.appendReplacement(result, "");
        String varName =  matcher.group().substring(2); // matched string starts with $$
        ObserverVariable variable = pArgs.getObserverVariables().get(varName);
        if (variable == null) {
          // this variable has not been set.
          pArgs.getLogger().log(Level.WARNING, "could not replace the Observer variable reference " + varName + " (not found).");
          return null;
        } else {
          result.append(variable.getValue());
        }
      }
      matcher.appendTail(result);
      return result.toString();
    }

    @Override
    public String toString() {
      return "MODIFY(" + cpaName + "(\"" + modificationString + "\"))";
    }

  }
}
