// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;

import com.google.common.base.Preconditions;


class ASTTypeConverter extends TypeConverter {

  private final Scope scope;

  public ASTTypeConverter(Scope pScope) {
    scope = pScope;
  }

    /**
     *  Searches for a type within the Type Hierarchy.
     *  If found, returns it.
     *
     * @param t binding representing the sought after type.
     * @return  Returns a type within the TypeHierachie or a Unspecified Type.
     */
    @Override
    public JInterfaceType convertInterfaceType(ITypeBinding t) {

      if (t.isClass()) {
        return JInterfaceType.createUnresolvableType();
      }

      Preconditions.checkArgument(t.isInterface());

      String typeName = NameConverter.convertClassOrInterfaceToFullName(t);

      if (scope.containsInterfaceType(typeName)) {
        return scope.getInterfaceType(typeName);
      } else {
        return scope.createNewInterfaceType(t);
      }
    }

    /**
     * Converts a Class Type by its Binding.
     * This Method searches in the parsed Type Hierarchy for
     * the type, which is represented by the  given binding.
     *
     * @param t type Binding which represents the sought after type
     * @return The Class Type which is represented by t.
     */
    @Override
    public JClassType convertClassType(ITypeBinding t) {

      Preconditions.checkArgument(t.isClass() || t.isEnum());

      String typeName = NameConverter.convertClassOrInterfaceToFullName(t);

      if (scope.containsClassType(typeName)) {
        return scope.getClassType(typeName);
      } else {
        return scope.createNewClassType(t);
      }
    }
}