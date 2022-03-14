// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

final class NameConverter {

  private static final String DELIMITER = "_";

  private NameConverter() {}

  /**
   * This Method uses the binding of a Method to construct the fully qualified unique method name
   * for Methods and constructor in the CFA. Use whenever possible to avoid Inconsistency.
   *
   * @param binding The JDT Binding of a method to be named
   * @return the fully Qualified, unique method name
   */
  public static String convertName(IMethodBinding binding) {

    StringBuilder name =
        new StringBuilder(
            (convertClassOrInterfaceToFullName(binding.getDeclaringClass())
                + DELIMITER
                + binding.getName()));

    final ITypeBinding[] parameterTypes = binding.getParameterTypes();
    if (parameterTypes.length > 0) {
      name.append(DELIMITER);
    }
    Joiner.on(DELIMITER)
        .appendTo(name, from(parameterTypes).transform(NameConverter::convertTypeName));

    return name.toString();
  }

  private static String convertTypeName(ITypeBinding binding) {
    // TODO Erase when Library in class Path
    if (binding.getBinaryName().equals("String")
        || binding.getQualifiedName().equals("java.lang.String")) {
      return "java_lang_String";

    } else if (binding.isArray()) {
      ITypeBinding elementType = binding.getElementType();

      if (elementType.getBinaryName().equals("String")
          || elementType.getQualifiedName().equals("java.lang.String")) {
        return "String[]";
      }
      return elementType.getQualifiedName() + "[]";
    }
    return binding.getQualifiedName();
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

    } else if (classBinding.isRecovered()) {
      // Dirty solution for MissingTypeBindings.
      try {
        final String name = classBinding.getName();
        Class.forName("java.lang." + name);
        return "java.lang." + classBinding.getName();
      } catch (ClassNotFoundException e) {
        return classBinding.getQualifiedName();
      }
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
      return convertAnonymousClassConstructorName(classBinding, ImmutableList.of());

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

    StringBuilder name =
        new StringBuilder(
            convertClassOrInterfaceToFullName(declaringClassBinding)
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
