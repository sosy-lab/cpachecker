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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSimpleParsingDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSmtFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSortDeclaration;

public class SvLibCurrentScope extends SvLibScope {

  private PersistentMap<String, SvLibSimpleParsingDeclaration> globalVariables;

  private PersistentMap<String, SvLibSimpleParsingDeclaration> globalVariablesQualifiedNames;

  private PersistentMap<String, SvLibParsingParameterDeclaration> procedureDeclarationVariables;

  private PersistentMap<String, SvLibParsingParameterDeclaration>
      procedureDeclarationVariablesQualifiedNames;

  private PersistentMap<String, SvLibProcedureDeclaration> procedureDeclarations;

  public SvLibCurrentScope() {
    super(new ImmutableSet.Builder<>(), new ImmutableMap.Builder<>(), new LinkedHashMap<>());
    globalVariables = PathCopyingPersistentTreeMap.of();
    globalVariablesQualifiedNames = PathCopyingPersistentTreeMap.of();
    procedureDeclarationVariables = PathCopyingPersistentTreeMap.of();
    procedureDeclarationVariablesQualifiedNames = PathCopyingPersistentTreeMap.of();
    procedureDeclarations = PathCopyingPersistentTreeMap.of();
  }

  private SvLibCurrentScope(
      PersistentMap<String, SvLibSimpleParsingDeclaration> pGlobalVariables,
      PersistentMap<String, SvLibSimpleParsingDeclaration> pGlobalVariablesQualifiedNames,
      PersistentMap<String, SvLibParsingParameterDeclaration> pProcedureDeclarationVariables,
      PersistentMap<String, SvLibParsingParameterDeclaration>
          pProcedureDeclarationVariablesQualifiedNames,
      PersistentMap<String, SvLibProcedureDeclaration> pProcedureDeclarations,
      ImmutableSet.Builder<SmtLibLogic> pLogics,
      ImmutableMap.Builder<String, SvLibSortDeclaration> pSortDeclarations,
      Map<String, SvLibSmtFunctionDeclaration> pFunctionDeclarations) {
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
  public void enterProcedure(List<SvLibParsingParameterDeclaration> pParameters) {
    for (SvLibParsingParameterDeclaration parameter : pParameters) {
      String key = keyOf(parameter.getName());
      if (globalVariables.containsKey(key) || procedureDeclarationVariables.containsKey(key)) {
        throw new IllegalArgumentException(
            "Parameter with name "
                + parameter.getQualifiedName()
                + " already exists in the scope.");
      }
      procedureDeclarationVariables = procedureDeclarationVariables.putAndCopy(key, parameter);
      procedureDeclarationVariablesQualifiedNames =
          procedureDeclarationVariablesQualifiedNames.putAndCopy(
              keyOf(parameter.getQualifiedName()), parameter);
    }
  }

  @Override
  public void leaveProcedure() {
    // Clear the procedure declaration variables when exiting the function/procedure scope
    procedureDeclarationVariables = PathCopyingPersistentTreeMap.of();
  }

  /**
   * The key under which a variable of the given name is stored.
   *
   * <p>A name that is not a simple symbol of SMT-LIB, or that is a reserved word of SV-LIB, is
   * quoted in the script, but the quotes are only a way of writing the name and not part of it, so
   * both forms denote the same variable. Only the name itself is quoted, not the name of the
   * procedure that a qualified name starts with.
   */
  private static String keyOf(String pName) {
    // A qualified name can be quoted as a whole or only in the part after the name of the
    // procedure.
    String name = withoutQuotes(pName);
    int endOfProcedureName = name.lastIndexOf("::");
    if (endOfProcedureName >= 0) {
      return name.substring(0, endOfProcedureName + 2)
          + withoutQuotes(name.substring(endOfProcedureName + 2));
    }
    return name;
  }

  private static String withoutQuotes(String pName) {
    return pName.length() > 1 && pName.startsWith("|") && pName.endsWith("|")
        ? pName.substring(1, pName.length() - 1)
        : pName;
  }

  @Override
  public SvLibSimpleParsingDeclaration getVariable(String pText) {
    String key = keyOf(pText);
    if (globalVariables.containsKey(key)) {
      return globalVariables.get(key);
    } else if (procedureDeclarationVariables.containsKey(key)) {
      return procedureDeclarationVariables.get(key);
    } else {
      throw new IllegalArgumentException(
          "Variable with name " + pText + " does not exist in the scope.");
    }
  }

  @Override
  public SvLibSimpleParsingDeclaration getVariableForQualifiedName(String pText) {
    String key = keyOf(pText);
    if (globalVariablesQualifiedNames.containsKey(key)) {
      return globalVariablesQualifiedNames.get(key);
    } else if (procedureDeclarationVariablesQualifiedNames.containsKey(key)) {
      return procedureDeclarationVariablesQualifiedNames.get(key);
    } else {
      throw new IllegalArgumentException(
          "Variable with name " + pText + " does not exist in the scope.");
    }
  }

  @Override
  public boolean hasVariable(String pText) {
    String key = keyOf(pText);
    return globalVariables.containsKey(key) || procedureDeclarationVariables.containsKey(key);
  }

  @Override
  public boolean hasVariableForQualifiedName(String pText) {
    String key = keyOf(pText);
    return globalVariablesQualifiedNames.containsKey(key)
        || procedureDeclarationVariablesQualifiedNames.containsKey(key);
  }

  @Override
  public void addVariable(SvLibParsingVariableDeclaration pVariableDeclaration) {
    String key = keyOf(pVariableDeclaration.getName());
    if (globalVariables.containsKey(key) || procedureDeclarationVariables.containsKey(key)) {
      throw new IllegalArgumentException(
          "Variable with name " + pVariableDeclaration.getName() + " already exists in the scope.");
    }
    globalVariables = globalVariables.putAndCopy(key, pVariableDeclaration);
    globalVariablesQualifiedNames =
        globalVariablesQualifiedNames.putAndCopy(
            keyOf(pVariableDeclaration.getQualifiedName()), pVariableDeclaration);
  }

  @Override
  public void addProcedureDeclaration(SvLibProcedureDeclaration pProcedureDeclaration) {
    String procedureName = keyOf(pProcedureDeclaration.getName());
    if (procedureDeclarations.containsKey(procedureName)) {
      throw new IllegalArgumentException(
          "Procedure with name "
              + pProcedureDeclaration.getName()
              + " already exists in the scope.");
    }
    procedureDeclarations = procedureDeclarations.putAndCopy(procedureName, pProcedureDeclaration);
  }

  @Override
  public SvLibProcedureDeclaration getProcedureDeclaration(String pText) {
    return Objects.requireNonNull(
        procedureDeclarations.get(keyOf(pText)),
        () -> "Procedure with name " + pText + " does not exist in the scope.");
  }

  @Override
  public boolean hasProcedureDeclaration(String pText) {
    return procedureDeclarations.containsKey(keyOf(pText));
  }
}
