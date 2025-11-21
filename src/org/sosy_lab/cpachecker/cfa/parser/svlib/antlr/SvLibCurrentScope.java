// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.antlr;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSmtFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSortDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.smtlib.SmtLibLogic;

class SvLibCurrentScope extends SvLibScope {

  private PersistentMap<String, SvLibSimpleDeclaration> globalVariables;

  private PersistentMap<String, SvLibSimpleDeclaration> globalVariablesQualifiedNames;

  private PersistentMap<String, SvLibParameterDeclaration> procedureDeclarationVariables;

  private PersistentMap<String, SvLibParameterDeclaration>
      procedureDeclarationVariablesQualifiedNames;

  private PersistentMap<String, SvLibProcedureDeclaration> procedureDeclarations;

  public SvLibCurrentScope() {
    super(new ImmutableSet.Builder<>(), new ImmutableMap.Builder<>(), new ImmutableMap.Builder<>());
    globalVariables = PathCopyingPersistentTreeMap.of();
    globalVariablesQualifiedNames = PathCopyingPersistentTreeMap.of();
    procedureDeclarationVariables = PathCopyingPersistentTreeMap.of();
    procedureDeclarationVariablesQualifiedNames = PathCopyingPersistentTreeMap.of();
    procedureDeclarations = PathCopyingPersistentTreeMap.of();
  }

  private SvLibCurrentScope(
      PersistentMap<String, SvLibSimpleDeclaration> pGlobalVariables,
      PersistentMap<String, SvLibSimpleDeclaration> pGlobalVariablesQualifiedNames,
      PersistentMap<String, SvLibParameterDeclaration> pProcedureDeclarationVariables,
      PersistentMap<String, SvLibParameterDeclaration> pProcedureDeclarationVariablesQualifiedNames,
      PersistentMap<String, SvLibProcedureDeclaration> pProcedureDeclarations,
      ImmutableSet.Builder<SmtLibLogic> pLogics,
      ImmutableMap.Builder<String, SvLibSortDeclaration> pSortDeclarations,
      ImmutableMap.Builder<String, SvLibSmtFunctionDeclaration> pFunctionDeclarations) {
    super(pLogics, pSortDeclarations, pFunctionDeclarations);
    globalVariables = pGlobalVariables;
    globalVariablesQualifiedNames = pGlobalVariablesQualifiedNames;
    procedureDeclarationVariables = pProcedureDeclarationVariables;
    procedureDeclarationVariablesQualifiedNames = pProcedureDeclarationVariablesQualifiedNames;
    procedureDeclarations = pProcedureDeclarations;
  }

  @Override
  public SvLibCurrentScope copy() {
    return new SvLibCurrentScope(
        globalVariables,
        globalVariablesQualifiedNames,
        procedureDeclarationVariables,
        procedureDeclarationVariablesQualifiedNames,
        procedureDeclarations,
        logics,
        sortDeclarations,
        functionDeclarations);
  }

  @Override
  public void enterProcedure(List<SvLibParameterDeclaration> pParameters) {
    for (SvLibParameterDeclaration parameter : pParameters) {
      if (globalVariables.containsKey(parameter.getName())
          || procedureDeclarationVariables.containsKey(parameter.getName())) {
        throw new IllegalArgumentException(
            "Parameter with name " + parameter.getName() + " already exists in the scope.");
      }
      procedureDeclarationVariables =
          procedureDeclarationVariables.putAndCopy(parameter.getName(), parameter);
      procedureDeclarationVariablesQualifiedNames =
          procedureDeclarationVariablesQualifiedNames.putAndCopy(
              parameter.getQualifiedName(), parameter);
    }
  }

  @Override
  public void leaveProcedure() {
    // Clear the procedure declaration variables when exiting the function/procedure scope
    procedureDeclarationVariables = PathCopyingPersistentTreeMap.of();
  }

  @Override
  public SvLibSimpleDeclaration getVariable(String pText) {
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
  public SvLibSimpleDeclaration getVariableForQualifiedName(String pText) {
    if (globalVariablesQualifiedNames.containsKey(pText)) {
      return globalVariablesQualifiedNames.get(pText);
    } else if (procedureDeclarationVariablesQualifiedNames.containsKey(pText)) {
      return procedureDeclarationVariablesQualifiedNames.get(pText);
    } else {
      throw new IllegalArgumentException(
          "Variable with name " + pText + " does not exist in the scope.");
    }
  }

  @Override
  public void addVariable(SvLibVariableDeclaration pVariableDeclaration) {
    if (globalVariables.containsKey(pVariableDeclaration.getName())
        || procedureDeclarationVariables.containsKey(pVariableDeclaration.getName())) {
      throw new IllegalArgumentException(
          "Variable with name " + pVariableDeclaration.getName() + " already exists in the scope.");
    }
    globalVariables =
        globalVariables.putAndCopy(pVariableDeclaration.getName(), pVariableDeclaration);
    globalVariablesQualifiedNames =
        globalVariablesQualifiedNames.putAndCopy(
            pVariableDeclaration.getQualifiedName(), pVariableDeclaration);
  }

  @Override
  public void addProcedureDeclaration(SvLibProcedureDeclaration pProcedureDeclaration) {
    String procedureName = pProcedureDeclaration.getName();
    if (procedureDeclarations.containsKey(procedureName)) {
      throw new IllegalArgumentException(
          "Procedure with name " + procedureName + " already exists in the scope.");
    }
    procedureDeclarations = procedureDeclarations.putAndCopy(procedureName, pProcedureDeclaration);
  }

  @Override
  public SvLibProcedureDeclaration getProcedureDeclaration(String pText) {
    return Objects.requireNonNull(procedureDeclarations.get(pText));
  }
}
