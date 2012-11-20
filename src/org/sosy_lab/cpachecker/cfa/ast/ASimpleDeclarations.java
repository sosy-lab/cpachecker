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
import org.sosy_lab.cpachecker.cfa.types.Type;

import com.google.common.base.Strings;



public class ASimpleDeclarations extends AstNode implements IASimpleDeclaration {

  protected final Type type;
  protected final String name;
  protected final String origName;

  public ASimpleDeclarations(FileLocation pFileLocation, final Type pType, final String pName, final String pOrigName) {
    super(pFileLocation);
    type = pType;
    name = pName;
    origName = pOrigName;
  }

  public ASimpleDeclarations(final FileLocation pFileLocation,
      final Type pType, final String pName) {
    this(pFileLocation, pType, pName, pName);
  }

  @Override
  public String getName() {
    return name;
  }
  @Override
  public String getOrigName() {
    return origName;
  }

  @Override
  public String toASTString() {
    String name = Strings.nullToEmpty(getName());
    return getType().toASTString(name) + ";";
  }

  @Override
  public Type getType() {
    return type;
  }
}