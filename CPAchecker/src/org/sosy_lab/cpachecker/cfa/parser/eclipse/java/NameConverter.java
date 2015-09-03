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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import com.google.common.base.Joiner;


final class NameConverter {

  private static final String DELIMITER = "_";

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
        convertClassOrInterfaceToFullName(binding.getDeclaringClass())
            + DELIMITER + binding.getName()));

    String[] typeNames = convertTypeNames(binding.getParameterTypes());

    if (typeNames.length > 0) {
      name.append(DELIMITER);
    }

    Joiner.on(DELIMITER).appendTo(name , typeNames);

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

      String declaringClassName = convertClassOrInterfaceToFullName(vb.getDeclaringClass());

      name.append(declaringClassName + DELIMITER);
    }

    name.append(vb.getName());

    return name.toString();
  }

  public static String convertClassOrInterfaceToFullName(ITypeBinding classBinding) {

    if (classBinding.isAnonymous()) {

      // Anonymous types do not have a name, so we just use their key. This way, two anonymous
      // declarations with exactly the same content are only assigned once
      String key = classBinding.getKey();

      // cut the semicolon at the end of the key
      assert key.charAt(key.length() - 1) == ';';
      return key.substring(0, key.length() - 1);

    } else {
      return classBinding.getQualifiedName();
    }
  }

  public static String convertClassOrInterfaceToSimpleName(ITypeBinding classBinding) {

    if (classBinding.isAnonymous()) {

      // Anonymous types do not have a name, so we just use their key. This way, two anonymous
      // declarations with exactly the same content are only assigned once
      String key = classBinding.getKey();

      // cut the semicolon at the end of the key
      assert key.charAt(key.length() - 1) == ';';
      return key.substring(0, key.length() - 1);

    } else {
      return classBinding.getName();
    }
  }

  public static String convertDefaultConstructorName(ITypeBinding classBinding) {
    if (classBinding.isAnonymous()) {
     return convertAnonymousClassConstructorName(classBinding, Collections.<JType>emptyList());

    } else {
      return (convertClassOrInterfaceToFullName(classBinding)
          + DELIMITER
          + convertClassOrInterfaceToSimpleName(classBinding));
    }
  }

  public static String convertAnonymousClassConstructorName(
      ITypeBinding pClassBinding, List<JType> pParameters) {

    ITypeBinding declaringClassBinding = pClassBinding.getDeclaringClass();
    assert declaringClassBinding != null : "Anonymous class must be nested!";

    StringBuilder name = new StringBuilder(convertClassOrInterfaceToFullName(declaringClassBinding)
                                          + "."
                                          + convertClassOrInterfaceToFullName(pClassBinding)
        + DELIMITER
                                          + convertClassOrInterfaceToSimpleName(pClassBinding));

    if (!pParameters.isEmpty()) {
      name.append(DELIMITER);
    }

    List<String> parameterTypeNames = new ArrayList<>(pParameters.size());

    for (JType t : pParameters) {
      parameterTypeNames.add(t.toString());
    }

    Joiner.on(DELIMITER).appendTo(name, parameterTypeNames);

    return name.toString();
  }

  public static String createQualifiedName(String pMethodName, String pVariableName) {
    return pMethodName + "::" + pVariableName;
  }
}