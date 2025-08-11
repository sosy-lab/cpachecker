// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclaration;

public class K3Scope {

  private final Map<String, K3SimpleDeclaration> globalVariables;

  private final Map<String, K3ParameterDeclaration> procedureDeclarationVariables;

  private final Map<String, K3ProcedureDeclaration> procedureDeclarations;

  public K3Scope() {
    globalVariables = new HashMap<>();
    procedureDeclarationVariables = new HashMap<>();
    procedureDeclarations = new HashMap<>();
  }

  public void enterFunctionProcedure(List<K3ParameterDeclaration> pParameters) {
    for (K3ParameterDeclaration parameter : pParameters) {
      if (globalVariables.containsKey(parameter.getName())
          || procedureDeclarationVariables.containsKey(parameter.getName())) {
        throw new IllegalArgumentException(
            "Parameter with name " + parameter.getName() + " already exists in the scope.");
      }
      procedureDeclarationVariables.put(parameter.getName(), parameter);
    }
  }

  public void exitFunctionProcedure() {
    // Clear the procedure declaration variables when exiting the function/procedure scope
    procedureDeclarationVariables.clear();
  }

  public K3SimpleDeclaration getVariable(String pText) {
    if (globalVariables.containsKey(pText)) {
      return globalVariables.get(pText);
    } else if (procedureDeclarationVariables.containsKey(pText)) {
      return procedureDeclarationVariables.get(pText);
    } else {
      throw new IllegalArgumentException(
          "Variable with name " + pText + " does not exist in the scope.");
    }
  }

  public void addVariable(K3VariableDeclaration pVariableDeclaration) {
    if (globalVariables.containsKey(pVariableDeclaration.getName())
        || procedureDeclarationVariables.containsKey(pVariableDeclaration.getName())) {
      throw new IllegalArgumentException(
          "Variable with name " + pVariableDeclaration.getName() + " already exists in the scope.");
    }
    globalVariables.put(pVariableDeclaration.getName(), pVariableDeclaration);
  }

  public void addProcedureDeclaration(K3ProcedureDeclaration pProcedureDeclaration) {
    String procedureName = pProcedureDeclaration.getName();
    if (procedureDeclarations.containsKey(procedureName)) {
      throw new IllegalArgumentException(
          "Procedure with name " + procedureName + " already exists in the scope.");
    }
    procedureDeclarations.put(procedureName, pProcedureDeclaration);
  }

  public K3ProcedureDeclaration getFunctionDeclaration(String pText) {
    return Objects.requireNonNull(procedureDeclarations.get(pText));
  }
}
