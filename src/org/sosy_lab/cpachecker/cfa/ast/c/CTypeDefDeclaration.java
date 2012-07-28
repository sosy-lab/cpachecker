/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.CFileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class represent typedef declarations.
 * Example code:
 *
 * typedef int my_int;
 */
public final class CTypeDefDeclaration extends ADeclaration implements CDeclaration {

  public CTypeDefDeclaration(CFileLocation pFileLocation, boolean pIsGlobal,
      CType pType, String pName) {
    super(pFileLocation, pIsGlobal, pType, checkNotNull(pName), pName);
  }

  @Override
  public CType getType(){
    return (CType)type;
  }

  @Override
  public String toASTString() {
    return "typedef " + super.toASTString();
  }
}
