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
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3FunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SortDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Type;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3VariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.SmtLibLogic;

public abstract class K3Scope {

  protected final ImmutableSet.Builder<SmtLibLogic> logics;

  protected final ImmutableMap.Builder<String, K3SortDeclaration> sortDeclarations;

  protected final ImmutableMap.Builder<String, K3FunctionDeclaration> functionDeclarations;

  protected K3Scope(
      ImmutableSet.Builder<SmtLibLogic> pLogics,
      ImmutableMap.Builder<String, K3SortDeclaration> pSortDeclarations,
      ImmutableMap.Builder<String, K3FunctionDeclaration> pFunctionDeclarations) {
    logics = pLogics;
    sortDeclarations = pSortDeclarations;
    functionDeclarations = pFunctionDeclarations;
  }

  abstract K3Scope copy();

  abstract void enterProcedure(List<K3ParameterDeclaration> pParameters);

  abstract void leaveProcedure();

  abstract K3SimpleDeclaration getVariable(String pText);

  abstract void addVariable(K3VariableDeclaration pDeclaration);

  abstract void addProcedureDeclaration(K3ProcedureDeclaration pDeclaration);

  abstract K3ProcedureDeclaration getProcedureDeclaration(String pName);

  void addLogic(SmtLibLogic pLogic) {
    logics.add(pLogic);
  }

  Set<SmtLibLogic> getLogics() {
    return logics.build();
  }

  void addSortDeclaration(K3SortDeclaration pDeclaration) {
    sortDeclarations.put(pDeclaration.getName(), pDeclaration);
  }

  /**
   * Get the K3Type for the given name. First, it tries to parse it as a built-in type. If that
   * fails, it looks for a sort declaration with the given name.
   *
   * @param pName the name of the type
   * @return the K3Type
   */
  K3Type getTypeForName(String pName) {
    Optional<K3Type> builtInType = K3Type.getTypeForString(pName);
    if (builtInType.isPresent()) {
      return builtInType.orElseThrow();
    }

    return Objects.requireNonNull(
            sortDeclarations.build().get(pName), "Sort declaration '" + pName + "' not found.")
        .getType();
  }

  void addFunctionDeclaration(K3FunctionDeclaration pDeclaration) {
    functionDeclarations.put(pDeclaration.getName(), pDeclaration);
  }

  K3FunctionDeclaration getFunctionDeclaration(String pName) {
    return Objects.requireNonNull(
        functionDeclarations.build().get(pName), "Function declaration '" + pName + "' not found.");
  }
}
