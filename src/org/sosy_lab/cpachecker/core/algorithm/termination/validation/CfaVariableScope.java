// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * A scope that also finds the variables of the CFA by their qualified names in CPAchecker, which
 * the formulas contain. They differ from the names in the program for renamed variables, e.g.,
 * main::i__1 for a second variable i in the function main.
 */
final class CfaVariableScope implements Scope {

  private final Scope delegate;
  private final ImmutableMap<String, CSimpleDeclaration> variables;

  CfaVariableScope(Scope pDelegate, CFA pCfa) {
    delegate = pDelegate;
    Map<String, CSimpleDeclaration> variablesOfCfa = new HashMap<>();
    for (CFAEdge edge : CFAUtils.allEdges(pCfa)) {
      if (edge instanceof CDeclarationEdge declarationEdge
          && declarationEdge.getDeclaration() instanceof CVariableDeclaration variable) {
        // As in CProgramScope, the declarations do not refer to other variables in initializers
        variablesOfCfa.putIfAbsent(
            variable.getQualifiedName(),
            new CVariableDeclaration(
                variable.getFileLocation(),
                variable.isGlobal(),
                variable.getCStorageClass(),
                variable.getType(),
                variable.getName(),
                variable.getOrigName(),
                variable.getQualifiedName(),
                null));
      }
    }
    variables = ImmutableMap.copyOf(variablesOfCfa);
  }

  @Override
  public @Nullable CSimpleDeclaration lookupVariable(String pName) {
    CSimpleDeclaration variable = delegate.lookupVariable(pName);
    return variable != null ? variable : variables.get(pName);
  }

  @Override
  public boolean isGlobalScope() {
    return delegate.isGlobalScope();
  }

  @Override
  public boolean variableNameInUse(String pName) {
    return delegate.variableNameInUse(pName);
  }

  @Override
  public @Nullable CFunctionDeclaration lookupFunction(String pName) {
    return delegate.lookupFunction(pName);
  }

  @Override
  public @Nullable CComplexType lookupType(String pName) {
    return delegate.lookupType(pName);
  }

  @Override
  public CType lookupTypedef(String pName) {
    return delegate.lookupTypedef(pName);
  }

  @Override
  public void registerDeclaration(CSimpleDeclaration pDeclaration) {
    delegate.registerDeclaration(pDeclaration);
  }

  @Override
  public boolean registerTypeDeclaration(CComplexTypeDeclaration pDeclaration) {
    return delegate.registerTypeDeclaration(pDeclaration);
  }

  @Override
  public String createScopedNameOf(String pName) {
    return delegate.createScopedNameOf(pName);
  }

  @Override
  public String getFileSpecificTypeName(String pType) {
    return delegate.getFileSpecificTypeName(pType);
  }

  @Override
  public boolean isFileSpecificTypeName(String pType) {
    return delegate.isFileSpecificTypeName(pType);
  }
}
