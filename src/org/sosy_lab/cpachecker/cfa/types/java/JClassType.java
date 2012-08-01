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
package org.sosy_lab.cpachecker.cfa.types.java;

import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;


public class JClassType implements JReferenceType {

  private final VisibilityModifier visibility;
  private final boolean isFinal;
  private final boolean isAbstract;
  private final boolean strictFp;
  private final String name;


  public JClassType(String pName  ,final VisibilityModifier pVisibility, final boolean pIsFinal,
      final boolean pIsAbstract, final boolean pStrictFp) {
    name = pName;
    visibility = pVisibility;
    isFinal = pIsFinal;
    isAbstract = pIsAbstract;
    strictFp = pStrictFp;

     assert !isFinal && !isAbstract : "Classes can't be abstract and final";
     assert (visibility != VisibilityModifier.PRIVATE) || (visibility != VisibilityModifier.PROTECTED) : " Classes can't be private or protected";

  }

  @Override
  public String toASTString(String pDeclarator) {
    return pDeclarator.isEmpty() ? name : name + " " + pDeclarator;
  }

  public VisibilityModifier getVisibility() {
    return visibility;
  }

  public boolean isFinal() {
    return isFinal;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public String getName() {
    return name;
  }

  public boolean isStrictFp() {
    return strictFp;
  }
}