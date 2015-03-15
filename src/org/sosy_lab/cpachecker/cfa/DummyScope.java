/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa;

import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;

/**
 * For all languages, where parsing of single or blocks of statements is not yet implemented,
 * use this dummy scope when parsing an automaton {@link AutomatonParser}.
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
