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

import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;


public abstract class  JClassOrInterfaceType implements JReferenceType {

  private final VisibilityModifier visibility;
  private final String name;

  protected JClassOrInterfaceType (String fullyQualifiedName  ,final VisibilityModifier pVisibility) {
    name = fullyQualifiedName;
    visibility = pVisibility;

    assert (getVisibility() != VisibilityModifier.PRIVATE) || (getVisibility() != VisibilityModifier.PROTECTED) : " Interfaces can't be private or protected";
  }

  @Override
  public String toASTString(String pDeclarator) {
    return pDeclarator.isEmpty() ? getName() : getName() + " " + pDeclarator;
  }

  public String getName() {
    return name;
  }

  public VisibilityModifier getVisibility() {
    return visibility;
  }

  @Override
  public boolean equals(Object pObj) {

    return pObj instanceof JClassOrInterfaceType && ((JClassOrInterfaceType) pObj).getName().equals(name);
  }


  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
        return name;
  }

  public List<JClassOrInterfaceType> getAllSuperTypesOfType() {

    //TODO Maybe dynamic Binding of getAllSuperTypes here?
    List<JClassOrInterfaceType> result = new LinkedList<>();

    if(this instanceof JClassType){
      result.addAll(((JClassType)this).getAllSuperTypesOfClass()) ;
    } else if(this instanceof JInterfaceType){
      result.addAll(((JInterfaceType)this).getAllSuperTypesOfInterface());
      return result;
    }
    return result;
  }

  public List<JClassOrInterfaceType> getAllSubTypesOfType() {

    List<JClassOrInterfaceType> result = new LinkedList<>();

    if(this instanceof JClassType){
      result.addAll(((JClassType)this).getAllSubTypesOfClass()) ;
    } else if(this instanceof JInterfaceType){
      result.addAll(((JInterfaceType)this).getAllSuperTypesOfInterface());
      return result;
    }
    return result;
  }



}
