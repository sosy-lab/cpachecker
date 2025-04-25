// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.Scope;

public class AcslScope implements Scope {

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
        ImmutableMultimap.of(),
        ImmutableMultimap.of(),
        ImmutableMultimap.of(),
        ImmutableMultimap.of());
  }

  public static AcslScope mutableCopy(AcslScope pScope) {
    return new AcslScope(
        LinkedListMultimap.create(pScope.functionDeclarations),
        LinkedListMultimap.create(pScope.predicateDeclarations),
        LinkedListMultimap.create(pScope.simpleDeclarations),
        LinkedListMultimap.create(pScope.polymorphicTypeDeclarations));
  }

  @Override
  public boolean isGlobalScope() {
    return true;
  }

  @Override
  public boolean variableNameInUse(String name) {
    return simpleDeclarations.containsKey(name);
  }

  @Override
  public @Nullable AcslSimpleDeclaration lookupVariable(String name) {
    if (simpleDeclarations.containsKey(name)) {
      return simpleDeclarations.get(name).iterator().next();
    }

    return null;
  }

  @Override
  public @Nullable AcslFunctionDeclaration lookupFunction(String name) {
    if (functionDeclarations.containsKey(name)) {
      return functionDeclarations.get(name).iterator().next();
    }

    return null;
  }

  @SuppressWarnings("unused")
  public @Nullable AcslPredicateDeclaration lookupPredicate(String name) {
    if (predicateDeclarations.containsKey(name)) {
      return predicateDeclarations.get(name).iterator().next();
    }

    return null;
  }

  @Override
  public @Nullable AcslType lookupType(String name) {
    if (polymorphicTypeDeclarations.containsKey(name)) {
      return polymorphicTypeDeclarations.get(name).iterator().next();
    }

    return null;
  }

  @Override
  public AcslType lookupTypedef(String name) {
    if (polymorphicTypeDeclarations.containsKey(name)) {
      return polymorphicTypeDeclarations.get(name).iterator().next();
    }

    return null;
  }

  @Override
  public void registerDeclaration(ASimpleDeclaration declaration) {
    assert declaration instanceof AcslSimpleDeclaration;
    AcslSimpleDeclaration simpleDeclaration = (AcslSimpleDeclaration) declaration;

    if (simpleDeclaration instanceof AcslFunctionDeclaration pFunctionDeclaration) {
      functionDeclarations.put(pFunctionDeclaration.getName(), pFunctionDeclaration);
    } else if (simpleDeclaration instanceof AcslPredicateDeclaration pPredicateDeclaration) {
      predicateDeclarations.put(pPredicateDeclaration.getName(), pPredicateDeclaration);
    } else {
      simpleDeclarations.put(declaration.getName(), simpleDeclaration);
    }
  }

  @Override
  public boolean registerTypeDeclaration(AbstractDeclaration declaration) {
    if (declaration instanceof AcslTypeDeclaration pTypeDeclaration) {
      AcslType definedType = pTypeDeclaration.getType();
      if (definedType instanceof AcslPolymorphicType pPolymorphicType) {
        polymorphicTypeDeclarations.put(pTypeDeclaration.getName(), pPolymorphicType);
        return true;
      }
    }

    return false;
  }

  @Override
  public String createScopedNameOf(String name) {

    // TODO: Not implemented
    return "";
  }

  @Override
  public String getFileSpecificTypeName(String type) {
    // TODO: Not implemented
    return "";
  }

  @Override
  public boolean isFileSpecificTypeName(String type) {

    // TODO: Not implemented
    return false;
  }
}
