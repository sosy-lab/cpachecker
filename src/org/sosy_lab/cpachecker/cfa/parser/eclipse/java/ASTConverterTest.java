/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;

public class ASTConverterTest {

  @Test
  public void testGetClassOfJType() {
    Optional<Class<?>> optionalOfPrimitiveType =
        ASTConverter.getClassOfJType(JSimpleType.getBoolean(), ImmutableSet.of());
    assertThat(optionalOfPrimitiveType.get()).isEqualTo(boolean.class);
  }

  @Test
  public void testGetClassOfPrimitiveType() {
    Optional<Class<?>> optionalOfPrimitiveType =
        ASTConverter.getClassOfPrimitiveType(JSimpleType.getInt());
    assertThat(optionalOfPrimitiveType.get()).isEqualTo(int.class);

    optionalOfPrimitiveType = ASTConverter.getClassOfPrimitiveType(JSimpleType.getLong());
    assertThat(optionalOfPrimitiveType.get()).isEqualTo(long.class);

    optionalOfPrimitiveType = ASTConverter.getClassOfPrimitiveType(JSimpleType.getVoid());
    assertThat(optionalOfPrimitiveType).isEqualTo(Optional.absent());
  }

  @Test
  public void testGetClassOfJTypeForNonPrimitiveType() {
    JClassOrInterfaceType jClassOrInterfaceType =
        createStringJClassOrInterfaceType("java.lang.String", "String");
    Optional<Class<?>> optionalOfStringClass =
        ASTConverter.getClassOfJType(jClassOrInterfaceType, ImmutableSet.of());
    assertThat(optionalOfStringClass.get()).isEqualTo(String.class);
  }

  @Test
  public void testGetArrayClass() {
    JArrayType jArrayTypeOfString =
        new JArrayType(createStringJClassOrInterfaceType("java.lang.Boolean", "Boolean"), 3);
    Optional<Class<?>> optionalOfArrayClass =
        ASTConverter.getClassOfJType(jArrayTypeOfString, ImmutableSet.of());
    assertThat(optionalOfArrayClass.get().isArray()).isTrue();
    assertThat(optionalOfArrayClass.get().toGenericString()).isEqualTo("java.lang.Boolean[][][]");
  }

  private JClassOrInterfaceType createStringJClassOrInterfaceType(
      String pFullyQualifiedName, String pString) {
    return JClassType.valueOf(
        pFullyQualifiedName,
        pString,
        VisibilityModifier.PUBLIC,
        true,
        false,
        false,
        JClassType.createUnresolvableType(),
        ImmutableSet.of());
  }
}
