// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;

/**
 * For all languages, where parsing of single or blocks of statements is not yet implemented, use
 * this dummy scope when parsing an automaton {@link AutomatonParser}.
 */
public class DummyScope implements Scope {

  private static final DummyScope DUMMYSCOPE = new DummyScope();

  private DummyScope() {} // Private constructor to insure one instance.

  public static DummyScope getInstance() {
    return DUMMYSCOPE;
  }

  @Override
  public boolean isGlobalScope() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean variableNameInUse(String pName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CSimpleDeclaration lookupVariable(String pName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CFunctionDeclaration lookupFunction(String pName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CComplexType lookupType(String pName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CType lookupTypedef(String pName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerDeclaration(CSimpleDeclaration pDeclaration) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean registerTypeDeclaration(CComplexTypeDeclaration pDeclaration) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String createScopedNameOf(String pName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getFileSpecificTypeName(String pType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isFileSpecificTypeName(String pType) {
    throw new UnsupportedOperationException();
  }
}
