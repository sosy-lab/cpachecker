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

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.cfa.parser.eclipse.java.util.NameConverter.convertClassOrInterfaceName;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.ASTConverter.ModifierBean;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.TypeHierarchy.THTypeTable;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.util.NameConverter;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;

public class THTypeConverter extends TypeConverter {

  private final THTypeTable typeTable;

  public THTypeConverter(THTypeTable pTypeTable) {
    typeTable = pTypeTable;
  }

  @Override
  public JClassType convertClassType(ITypeBinding t) {

    checkArgument(t.isClass() || t.isEnum());

    String typeName = convertClassOrInterfaceName(t);

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

    // convert Super Class. Type of class object terminates this recursion.
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

    String typeName = convertClassOrInterfaceName(t);

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

    String name = NameConverter.convertClassOrInterfaceName(t);
    String simpleName = t.getName();

    ModifierBean mB = ModifierBean.getModifiers(t);
    return JClassType.valueOf(name, simpleName, mB.getVisibility(), mB.isFinal(), mB.isNative(),
        mB.isStrictFp(), pSuperClass, pImplementedInterfaces);
  }

  private JInterfaceType createInterfaceType(ITypeBinding t,
      Set<JInterfaceType> pExtendedInterfaces) {

    checkArgument(t.isTopLevel());

    String name = NameConverter.convertClassOrInterfaceName(t);
    String simpleName = t.getName();

    ModifierBean mB = ModifierBean.getModifiers(t);
    return JInterfaceType.valueOf(name, simpleName, mB.getVisibility(), pExtendedInterfaces);
  }

  private JClassType createClassType(ITypeBinding t, JClassType pSuperClass,
      Set<JInterfaceType> pImplementedInterfaces, JClassOrInterfaceType pEnclosingType) {

    checkArgument(!t.isTopLevel());

    String name = NameConverter.convertClassOrInterfaceName(t);
    String simpleName = t.getName();

    ModifierBean mB = ModifierBean.getModifiers(t);
    return JClassType.valueOf(name, simpleName, mB.getVisibility(), mB.isFinal(), mB.isNative(),
        mB.isStrictFp(), pSuperClass, pImplementedInterfaces, pEnclosingType);
  }

  private JInterfaceType createInterfaceType(ITypeBinding t,
      Set<JInterfaceType> pExtendedInterfaces, JClassOrInterfaceType pEnclosingType) {

    checkArgument(!t.isTopLevel());

    String name = NameConverter.convertClassOrInterfaceName(t);
    String simpleName = t.getName();

    ModifierBean mB = ModifierBean.getModifiers(t);
    return JInterfaceType.valueOf(name, simpleName, mB.getVisibility(),
        pExtendedInterfaces, pEnclosingType);
  }

}