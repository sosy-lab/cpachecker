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
package org.sosy_lab.cpachecker.cfa.ast;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;

/**
 * This class represents the core components that occur in each declaration:
 * a type an an (optional) name.
 *
 * It is part of the declaration of types and variables (see {@link IASTDeclaration})
 * and functions (see {@link IASTFunctionDefinition}).
 * It is also used stand-alone for the declaration of members of composite types
 * (e.g. structs) and for the declaration of function parameters.
 */
public abstract class IASTSimpleDeclaration extends IASTNode {

  private final IType    specifier;
  private final String   name;
  private final String   origName;

  public IASTSimpleDeclaration(final IASTFileLocation pFileLocation,
      final IType pSpecifier, final String pName) {
    this(pFileLocation, pSpecifier, pName, pName);
  }

  public IASTSimpleDeclaration(final IASTFileLocation pFileLocation,
      final IType pSpecifier, final String pName, final String pOrigName) {

    super(pFileLocation);

    specifier = checkNotNull(pSpecifier);
    name = pName;
    origName = pOrigName;
  }

  public IType getDeclSpecifier() {
    return specifier;
  }

  public String getName() {
    return name;
  }

  public String getOrigName() {
    return origName;
  }

  @Override
  public String toASTString() {
    String name = Strings.nullToEmpty(getName());
    return getDeclSpecifier().toASTString(name) + ";";
  }
}
