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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.CFileLocation;
import org.sosy_lab.cpachecker.cfa.types.IAFunctionType;

import com.google.common.base.Strings;


public  class JMethodDeclaration extends AFunctionDeclaration {

  final boolean isFinal;
  final boolean isAbstract;
  final boolean isStatic;
  final boolean isNative;
  final boolean isSynchronized;
  final boolean isStrictfp;
  final VisibilityModifier visibility;


  public JMethodDeclaration(CFileLocation pFileLocation, IAFunctionType pType, String pName , VisibilityModifier pVisibility  ,final boolean pIsFinal
      ,final boolean pIsAbstract, final boolean pIsStatic,final boolean pIsNative,final boolean pIsSynchronized,final boolean pIsStrictfp ) {
    super(pFileLocation, pType, pName);
    visibility = pVisibility;
    isFinal = pIsFinal;
    isAbstract = pIsAbstract;
    isStatic = pIsStatic;
    isNative = pIsNative;
    isSynchronized = pIsSynchronized;
    isStrictfp = pIsStrictfp;


    assert(pVisibility != null);
    assert(isAbstract && !isStatic && !isNative && !isFinal && !isSynchronized && !isStrictfp || (!isAbstract))
    : "Abstract Method may only have one Modifier , either public or protected";

  }


  @Override
  public String toASTString() {
    String name = Strings.nullToEmpty(getName());
    StringBuilder modifier = new StringBuilder() ;

    modifier.append(visibility.getModifierString() + " ");

    if(isAbstract){
      modifier.append("abstract ");
    }
    if(isStatic){
      modifier.append( "static ");
    }
    if(isFinal){
      modifier.append( "final ");
    }
    if(isSynchronized){
      modifier.append("synchronized ");
    }
    if(isNative){
      modifier.append( "native ");
    }
    if(isStrictfp){
      modifier.append("strictfp ");
    }

    return modifier + getType().toASTString(name) + ";";
  }
}