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
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibLogic;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSmtFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSortDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public abstract class SvLibScope {

  protected final ImmutableSet.Builder<SmtLibLogic> logics;

  protected final ImmutableMap.Builder<String, SvLibSortDeclaration> sortDeclarations;

  protected final ImmutableMap.Builder<String, SvLibSmtFunctionDeclaration> functionDeclarations;

  protected SvLibScope(
      ImmutableSet.Builder<SmtLibLogic> pLogics,
      ImmutableMap.Builder<String, SvLibSortDeclaration> pSortDeclarations,
      ImmutableMap.Builder<String, SvLibSmtFunctionDeclaration> pFunctionDeclarations) {
    logics = pLogics;
    sortDeclarations = pSortDeclarations;
    functionDeclarations = pFunctionDeclarations;
  }

  abstract SvLibScope copy();

  abstract void enterProcedure(List<SvLibParameterDeclaration> pParameters);

  abstract void leaveProcedure();

  public abstract SvLibSimpleDeclaration getVariable(String pText);

  public abstract SvLibSimpleDeclaration getVariableForQualifiedName(String pQualifiedName);

  abstract void addVariable(SvLibVariableDeclaration pDeclaration);

  abstract void addProcedureDeclaration(SvLibProcedureDeclaration pDeclaration);

  abstract SvLibProcedureDeclaration getProcedureDeclaration(String pName);

  void addLogic(SmtLibLogic pLogic) {
    logics.add(pLogic);
  }

  Set<SmtLibLogic> getLogics() {
    return logics.build();
  }

  void addSortDeclaration(SvLibSortDeclaration pDeclaration) {
    sortDeclarations.put(pDeclaration.getName(), pDeclaration);
  }

  /**
   * Get the SvLibType for the given name. First, it tries to parse it as a built-in type. If that
   * fails, it looks for a sort declaration with the given name.
   *
   * @param pName the name of the type
   * @return the SvLibType
   */
  SvLibType getTypeForName(String pName) {
    Optional<SvLibType> builtInType = SvLibType.getTypeForString(pName);
    if (builtInType.isPresent()) {
      return builtInType.orElseThrow();
    }

    return Objects.requireNonNull(
            sortDeclarations.buildOrThrow().get(pName),
            "Sort declaration '" + pName + "' not found.")
        .getType();
  }

  void addFunctionDeclaration(SvLibSmtFunctionDeclaration pDeclaration) {
    functionDeclarations.put(pDeclaration.getName(), pDeclaration);
  }

  SvLibSmtFunctionDeclaration getFunctionDeclaration(String pName) {
    return Objects.requireNonNull(
        functionDeclarations.buildOrThrow().get(pName),
        "Function declaration '" + pName + "' not found.");
  }
}
