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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java.util;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import com.google.common.base.Joiner;


public final class NameConverter {

  private NameConverter() {

  }

  /**
   * This Method uses the binding of a Method to construct the fully qualified unique
   *  method name for Methods and constructor in the CFA. Use whenever possible to avoid
   * Inconsistency.
   *
   * @param binding The JDT Binding of a method to be named
   * @return the fully Qualified, unique method name
   */
  public static String convertName(IMethodBinding binding) {

    StringBuilder name = new StringBuilder((
        convertClassOrInterfaceName(binding.getDeclaringClass())
            + "_" + binding.getName()));

      String[] typeNames = convertTypeNames(binding.getParameterTypes());

      if (typeNames.length > 0) {
      name.append("_");
      }

      Joiner.on("_").appendTo( name , typeNames);


    return name.toString();
  }

  public static String[] convertTypeNames(ITypeBinding[] parameterTypes) {

    String[] typeNames = new String[parameterTypes.length];

    int c = 0;
    for (ITypeBinding parameterTypeBindings : parameterTypes) {

      // TODO Erase when Library in class Path
      if (parameterTypeBindings.getBinaryName().equals("String")
          || parameterTypeBindings.getQualifiedName().equals("java.lang.String")) {

        typeNames[c] = "java_lang_String";
      } else if(parameterTypeBindings.isArray()) {

       ITypeBinding elementType = parameterTypeBindings.getElementType();

       if (elementType.getBinaryName().equals("String")
           || elementType.getQualifiedName().equals("java.lang.String")) {
         typeNames[c] = "String[]";
       } else {
         typeNames[c] = elementType.getQualifiedName() + "[]";
       }
      } else {
        typeNames[c] = parameterTypeBindings.getQualifiedName();
      }

      c++;
    }
    return typeNames;
  }

  public static String convertName(IVariableBinding vb) {
    StringBuilder name = new StringBuilder();

    // Field Variable are declared with Declaring class before Identifier
    if (vb.isField() && vb.getDeclaringClass() != null) {

      String declaringClassName =
          vb.getDeclaringClass().getQualifiedName();

      name.append(declaringClassName + "_");
    }

    name.append(vb.getName());

    return name.toString();
  }

  public static String convertClassOrInterfaceName(ITypeBinding classBinding) {
    return classBinding.getQualifiedName();
  }

  public static String convertDefaultConstructorName(ITypeBinding classBinding) {
    return (classBinding.getQualifiedName() + "_" + classBinding.getName());
  }
}