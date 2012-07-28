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

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.CFileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;

/**
 * This class represents forward declarations of functions.
 * Example code:
 *
 * int foo(int x);
 */
public final class CFunctionDeclaration extends AFunctionDeclaration implements CDeclaration {

  public CFunctionDeclaration(CFileLocation pFileLocation,
      CFunctionType pType, String pName) {
    super(pFileLocation, pType, checkNotNull(pName));
  }

  @Override
  public CFunctionType getType() {
    return (CFunctionType) super.getType();
  }
}
