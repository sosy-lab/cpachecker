// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.parser;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3FunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SortDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.SmtLibLogic;

class K3CurrentScope extends K3Scope {

  private PersistentMap<String, K3SimpleDeclaration> globalVariables;

  private PersistentMap<String, K3ParameterDeclaration> procedureDeclarationVariables;

  private PersistentMap<String, K3ProcedureDeclaration> procedureDeclarations;

  public K3CurrentScope() {
    super(new ImmutableSet.Builder<>(), new ImmutableMap.Builder<>(), new ImmutableMap.Builder<>());
    globalVariables = PathCopyingPersistentTreeMap.of();
    procedureDeclarationVariables = PathCopyingPersistentTreeMap.of();
    procedureDeclarations = PathCopyingPersistentTreeMap.of();
  }

  private K3CurrentScope(
      PersistentMap<String, K3SimpleDeclaration> pGlobalVariables,
      PersistentMap<String, K3ParameterDeclaration> pProcedureDeclarationVariables,
      PersistentMap<String, K3ProcedureDeclaration> pProcedureDeclarations,
      ImmutableSet.Builder<SmtLibLogic> pLogics,
      ImmutableMap.Builder<String, K3SortDeclaration> pSortDeclarations,
      ImmutableMap.Builder<String, K3FunctionDeclaration> pFunctionDeclarations) {
    super(pLogics, pSortDeclarations, pFunctionDeclarations);
    globalVariables = pGlobalVariables;
    procedureDeclarationVariables = pProcedureDeclarationVariables;
    procedureDeclarations = pProcedureDeclarations;
  }

  @Override
  public K3CurrentScope copy() {
    return new K3CurrentScope(
        globalVariables,
        procedureDeclarationVariables,
        procedureDeclarations,
        logics,
        sortDeclarations,
        functionDeclarations);
  }

  @Override
  public void enterProcedure(List<K3ParameterDeclaration> pParameters) {
    for (K3ParameterDeclaration parameter : pParameters) {
      if (globalVariables.containsKey(parameter.getName())
          || procedureDeclarationVariables.containsKey(parameter.getName())) {
        throw new IllegalArgumentException(
            "Parameter with name " + parameter.getName() + " already exists in the scope.");
      }
      procedureDeclarationVariables =
          procedureDeclarationVariables.putAndCopy(parameter.getName(), parameter);
    }
  }

  @Override
  public void leaveProcedure() {
    // Clear the procedure declaration variables when exiting the function/procedure scope
    procedureDeclarationVariables = PathCopyingPersistentTreeMap.of();
  }

  @Override
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

  @Override
  public void addVariable(K3VariableDeclaration pVariableDeclaration) {
    if (globalVariables.containsKey(pVariableDeclaration.getName())
        || procedureDeclarationVariables.containsKey(pVariableDeclaration.getName())) {
      throw new IllegalArgumentException(
          "Variable with name " + pVariableDeclaration.getName() + " already exists in the scope.");
    }
    globalVariables =
        globalVariables.putAndCopy(pVariableDeclaration.getName(), pVariableDeclaration);
  }

  @Override
  public void addProcedureDeclaration(K3ProcedureDeclaration pProcedureDeclaration) {
    String procedureName = pProcedureDeclaration.getName();
    if (procedureDeclarations.containsKey(procedureName)) {
      throw new IllegalArgumentException(
          "Procedure with name " + procedureName + " already exists in the scope.");
    }
    procedureDeclarations = procedureDeclarations.putAndCopy(procedureName, pProcedureDeclaration);
  }

  @Override
  public K3ProcedureDeclaration getProcedureDeclaration(String pText) {
    return Objects.requireNonNull(procedureDeclarations.get(pText));
  }
}
