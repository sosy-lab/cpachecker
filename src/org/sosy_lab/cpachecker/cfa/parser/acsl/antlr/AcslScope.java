// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl.antlr;

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTypeVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

/**
 * Immutable symbol table for the logic declarations of ACSL annotations. It is independent of the C
 * scope.
 */
public final class AcslScope {

  private static final AcslScope EMPTY =
      new AcslScope(
          PathCopyingPersistentTreeMap.of(),
          PathCopyingPersistentTreeMap.of(),
          PathCopyingPersistentTreeMap.of(),
          PathCopyingPersistentTreeMap.of());

  private final PersistentMap<String, AcslFunctionDeclaration> functionDeclarations;
  private final PersistentMap<String, AcslPredicateDeclaration> predicateDeclarations;
  private final PersistentMap<String, AcslSimpleDeclaration> variableDeclarations;
  private final PersistentMap<String, AcslType> typeVariables;

  private AcslScope(
      PersistentMap<String, AcslFunctionDeclaration> pFunctionDeclarations,
      PersistentMap<String, AcslPredicateDeclaration> pPredicateDeclarations,
      PersistentMap<String, AcslSimpleDeclaration> pVariableDeclarations,
      PersistentMap<String, AcslType> pTypeVariables) {
    functionDeclarations = pFunctionDeclarations;
    predicateDeclarations = pPredicateDeclarations;
    variableDeclarations = pVariableDeclarations;
    typeVariables = pTypeVariables;
  }

  public static AcslScope empty() {
    return EMPTY;
  }

  /**
   * Returns a scope that additionally contains the given declarations. A declaration hides a
   * declaration of the same kind with the same name, e.g., a binder of a quantifier hides a
   * parameter of a logic function.
   */
  public AcslScope withDeclarations(Iterable<? extends AcslSimpleDeclaration> pDeclarations) {
    PersistentMap<String, AcslFunctionDeclaration> functions = functionDeclarations;
    PersistentMap<String, AcslPredicateDeclaration> predicates = predicateDeclarations;
    PersistentMap<String, AcslSimpleDeclaration> variables = variableDeclarations;
    PersistentMap<String, AcslType> types = typeVariables;
    for (AcslSimpleDeclaration declaration : pDeclarations) {
      switch (declaration) {
        case AcslFunctionDeclaration function ->
            functions = functions.putAndCopy(function.getName(), function);
        case AcslPredicateDeclaration predicate ->
            predicates = predicates.putAndCopy(predicate.getName(), predicate);
        case AcslTypeVariableDeclaration typeVariable ->
            types = types.putAndCopy(typeVariable.getName(), typeVariable.getType());
        case AcslParameterDeclaration parameter ->
            variables = variables.putAndCopy(parameter.getName(), parameter);
        case AcslCVariableDeclaration variable ->
            variables = variables.putAndCopy(variable.getName(), variable);
      }
    }
    return new AcslScope(functions, predicates, variables, types);
  }

  public AcslScope withDeclaration(AcslSimpleDeclaration pDeclaration) {
    return withDeclarations(ImmutableList.of(pDeclaration));
  }

  public @Nullable AcslSimpleDeclaration lookupVariable(String name) {
    return variableDeclarations.get(name);
  }

  public @Nullable AcslFunctionDeclaration lookupFunction(String name) {
    return functionDeclarations.get(name);
  }

  public @Nullable AcslPredicateDeclaration lookupPredicate(String name) {
    return predicateDeclarations.get(name);
  }

  public @Nullable AcslType lookupType(String name) {
    return typeVariables.get(name);
  }
}
