// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.cfa.parser.eclipse.java.NameConverter.convertClassOrInterfaceToFullName;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.ASTConverter.ModifierBean;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.TypeHierarchy.THTypeTable;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;

class THTypeConverter extends TypeConverter {

  private final THTypeTable typeTable;

  public THTypeConverter(THTypeTable pTypeTable) {
    typeTable = pTypeTable;
  }

  @Override
  public JClassType convertClassType(ITypeBinding t) {

    checkArgument(t.isClass() || t.isEnum());

    String typeName = convertClassOrInterfaceToFullName(t);

    // check if type is was already converted.
    if (typeTable.containsType(typeName)) {
      JClassOrInterfaceType type = typeTable.getType(typeName);

      if (type instanceof JClassType) {
        return (JClassType) type;
      } else {
        throw new CFAGenerationRuntimeException("Class Type " + typeName +
            " was parsed as Interface.");
      }
    }

    // convert super class. Type of class 'Object' terminates this recursion.
    ITypeBinding superClass = t.getSuperclass();

    JClassType superClassType;

    if (superClass != null) {
      superClassType = convertClassType(superClass);
    } else {
      superClassType = JClassType.createUnresolvableType();
    }

    ITypeBinding[] interfaceBindings = t.getInterfaces();
    Set<JInterfaceType> interfaces = new HashSet<>();

    for (ITypeBinding itInterface : interfaceBindings) {
      interfaces.add(convertInterfaceType(itInterface));
    }

    JClassType resultType;

    if (t.isTopLevel()) {
      resultType = createClassType(t, superClassType, interfaces);
    } else {
      JClassOrInterfaceType enclosingType = convertEnclosingType(t);
      resultType = createClassType(t, superClassType, interfaces, enclosingType);
    }

    typeTable.registerType(resultType);

    return resultType;
  }

  @Override
  public JInterfaceType convertInterfaceType(ITypeBinding t) {

    if (t.isClass()) {
      return JInterfaceType.createUnresolvableType();
    }

    checkArgument(t.isInterface());

    String typeName = convertClassOrInterfaceToFullName(t);

    // check if type is was already converted.
    if (typeTable.containsType(typeName)) {
      JClassOrInterfaceType type = typeTable.getType(typeName);

      if (type instanceof JInterfaceType) {
        return (JInterfaceType) type;
      } else {
        throw new CFAGenerationRuntimeException("Interface type " + typeName +
            " was parsed as class type.");
      }
    }

    // Recursion stops, if no Super Types exist anymore
    ITypeBinding[] interfaceBindings = t.getInterfaces();
    Set<JInterfaceType> interfaces = new HashSet<>();

    for (ITypeBinding itInterface : interfaceBindings) {
      interfaces.add(convertInterfaceType(itInterface));
    }

    JInterfaceType type;

    if (t.isTopLevel()) {

      type = createInterfaceType(t, interfaces);

    } else {

      JClassOrInterfaceType enclosingType = convertEnclosingType(t);
      type = createInterfaceType(t, interfaces, enclosingType);
    }

    typeTable.registerType(type);

    return type;
  }

  private JClassOrInterfaceType convertEnclosingType(ITypeBinding pT) {

    ITypeBinding enclosingTypeBinding = pT.getDeclaringClass();

    if (enclosingTypeBinding == null) {
      return JClassType.createUnresolvableType();
    } else {
      return convertClassOrInterfaceType(enclosingTypeBinding);
    }
  }

  private JClassType createClassType(ITypeBinding t, JClassType pSuperClass,
      Set<JInterfaceType> pImplementedInterfaces) {

    checkArgument(t.isTopLevel());

    String name = NameConverter.convertClassOrInterfaceToFullName(t);
    String simpleName = t.getName();

    ModifierBean mB = ModifierBean.getModifiers(t);
    return JClassType.valueOf(name, simpleName, mB.getVisibility(), mB.isFinal(), mB.isNative(),
        mB.isStrictFp(), pSuperClass, pImplementedInterfaces);
  }

  private JInterfaceType createInterfaceType(ITypeBinding t,
      Set<JInterfaceType> pExtendedInterfaces) {

    checkArgument(t.isTopLevel());

    String name = NameConverter.convertClassOrInterfaceToFullName(t);
    String simpleName = t.getName();

    ModifierBean mB = ModifierBean.getModifiers(t);
    return JInterfaceType.valueOf(name, simpleName, mB.getVisibility(), pExtendedInterfaces);
  }

  private JClassType createClassType(ITypeBinding t, JClassType pSuperClass,
      Set<JInterfaceType> pImplementedInterfaces, JClassOrInterfaceType pEnclosingType) {

    checkArgument(!t.isTopLevel());

    String name = NameConverter.convertClassOrInterfaceToFullName(t);
    String simpleName = t.getName();

    if (simpleName.isEmpty()) {
      // if type has no name, use its full name as simple name
      simpleName = name;
    }

    ModifierBean mB = ModifierBean.getModifiers(t);
    return JClassType.valueOf(name, simpleName, mB.getVisibility(), mB.isFinal(), mB.isNative(),
        mB.isStrictFp(), pSuperClass, pImplementedInterfaces, pEnclosingType);
  }

  private JInterfaceType createInterfaceType(ITypeBinding t,
      Set<JInterfaceType> pExtendedInterfaces, JClassOrInterfaceType pEnclosingType) {

    checkArgument(!t.isTopLevel());

    String name = NameConverter.convertClassOrInterfaceToFullName(t);
    String simpleName = t.getName();

    ModifierBean mB = ModifierBean.getModifiers(t);
    return JInterfaceType.valueOf(name, simpleName, mB.getVisibility(),
        pExtendedInterfaces, pEnclosingType);
  }

}