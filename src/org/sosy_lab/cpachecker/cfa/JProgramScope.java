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

/**
 * Used to store the types of the CFA that are
 * lost when only a single statement or a block of statements
 * of the original program is parsed.
 */
/*
 * This class is currently a stub to ensure working Java analysis without
 * the use of a global scope.
 */
public class JProgramScope implements Scope {

  public JProgramScope(CFA cfa) {
    assert cfa.getLanguage() == Language.JAVA;
  }

  @Override
  public boolean isGlobalScope() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean variableNameInUse(String pName, String pOrigName) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public CSimpleDeclaration lookupVariable(String pName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CFunctionDeclaration lookupFunction(String pName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CComplexType lookupType(String pName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CType lookupTypedef(String pName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void registerDeclaration(CSimpleDeclaration pDeclaration) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean registerTypeDeclaration(CComplexTypeDeclaration pDeclaration) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String createScopedNameOf(String pName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getRenamedTypeName(String pType) {
    // TODO Auto-generated method stub
    return null;
  }

}
