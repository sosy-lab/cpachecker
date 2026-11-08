// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl.antlr;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTypeVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslPolymorphicType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;

/**
 * Symbol table for the logic declarations of ACSL annotations. It is independent of the C scope.
 */
public class AcslScope {

  private final Multimap<String, AcslFunctionDeclaration> functionDeclarations;

  private final Multimap<String, AcslPredicateDeclaration> predicateDeclarations;

  private final Multimap<String, AcslSimpleDeclaration> simpleDeclarations;

  private final Multimap<String, AcslPolymorphicType> polymorphicTypeDeclarations;

  private AcslScope(
      Multimap<String, AcslFunctionDeclaration> pFunctionDeclarations,
      Multimap<String, AcslPredicateDeclaration> pPredicateDeclarations,
      Multimap<String, AcslSimpleDeclaration> pSimpleDeclarations,
      Multimap<String, AcslPolymorphicType> pPolymorphicTypeDeclarations) {
    functionDeclarations = pFunctionDeclarations;
    predicateDeclarations = pPredicateDeclarations;
    simpleDeclarations = pSimpleDeclarations;
    polymorphicTypeDeclarations = pPolymorphicTypeDeclarations;
  }

  public static AcslScope empty() {
    return new AcslScope(
        ImmutableListMultimap.of(),
        ImmutableListMultimap.of(),
        ImmutableListMultimap.of(),
        ImmutableListMultimap.of());
  }

  public static AcslScope mutableCopy(AcslScope pScope) {
    return new AcslScope(
        LinkedListMultimap.create(pScope.functionDeclarations),
        LinkedListMultimap.create(pScope.predicateDeclarations),
        LinkedListMultimap.create(pScope.simpleDeclarations),
        LinkedListMultimap.create(pScope.polymorphicTypeDeclarations));
  }

  public @Nullable AcslSimpleDeclaration lookupVariable(String name) {
    if (simpleDeclarations.containsKey(name)) {
      return simpleDeclarations.get(name).iterator().next();
    }

    return null;
  }

  public @Nullable AcslFunctionDeclaration lookupFunction(String name) {
    if (functionDeclarations.containsKey(name)) {
      return functionDeclarations.get(name).iterator().next();
    }

    return null;
  }

  public @Nullable AcslPredicateDeclaration lookupPredicate(String name) {
    if (predicateDeclarations.containsKey(name)) {
      return predicateDeclarations.get(name).iterator().next();
    }

    return null;
  }

  public @Nullable AcslType lookupType(String name) {
    if (polymorphicTypeDeclarations.containsKey(name)) {
      return polymorphicTypeDeclarations.get(name).iterator().next();
    }

    return null;
  }

  public void registerDeclaration(AcslSimpleDeclaration declaration) {
    if (declaration instanceof AcslFunctionDeclaration pFunctionDeclaration) {
      functionDeclarations.put(pFunctionDeclaration.getName(), pFunctionDeclaration);
    } else if (declaration instanceof AcslPredicateDeclaration pPredicateDeclaration) {
      predicateDeclarations.put(pPredicateDeclaration.getName(), pPredicateDeclaration);
    } else {
      simpleDeclarations.put(declaration.getName(), declaration);
    }
  }

  /**
   * Register a type declaration.
   *
   * @return True if the type was registered, False if it is not a polymorphic type.
   */
  public boolean registerTypeDeclaration(AcslTypeVariableDeclaration declaration) {
    if (declaration.getType() instanceof AcslPolymorphicType pPolymorphicType) {
      polymorphicTypeDeclarations.put(declaration.getName(), pPolymorphicType);
      return true;
    }

    return false;
  }
}
