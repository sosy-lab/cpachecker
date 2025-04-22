// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.Scope;

public class AcslScope implements Scope {

  private AcslScope() {}

  public static AcslScope empty() {
    return new AcslScope();
  }

  @Override
  public boolean isGlobalScope() {
    return false;
  }

  @Override
  public boolean variableNameInUse(String name) {
    return false;
  }

  @Override
  public @Nullable AcslSimpleDeclaration lookupVariable(String name) {
    return null;
  }

  @Override
  public @Nullable AcslFunctionDeclaration lookupFunction(String name) {
    return null;
  }

  @SuppressWarnings("unused")
  public @Nullable AcslPredicateDeclaration lookupPredicate(String name) {
    return null;
  }

  @Override
  public @Nullable AcslType lookupType(String name) {
    return null;
  }

  @Override
  public AcslType lookupTypedef(String name) {
    return null;
  }

  @Override
  public void registerDeclaration(ASimpleDeclaration declaration) {}

  @Override
  public boolean registerTypeDeclaration(AbstractDeclaration declaration) {
    return false;
  }

  @Override
  public String createScopedNameOf(String name) {
    return "";
  }

  @Override
  public String getFileSpecificTypeName(String type) {
    return "";
  }

  @Override
  public boolean isFileSpecificTypeName(String type) {
    return false;
  }
}
