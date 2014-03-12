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

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.util.NameConverter;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;

import com.google.common.base.Preconditions;


public class ASTTypeConverter extends TypeConverter {

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

      String typeName = NameConverter.convertClassOrInterfaceName(t);

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

      String typeName = NameConverter.convertClassOrInterfaceName(t);

      if (scope.containsClassType(typeName)) {
        return scope.getClassType(typeName);
      } else {
        return scope.createNewClassType(t);
      }
    }
}