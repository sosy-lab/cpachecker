// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// Copyright (C) 2007-2020  Dirk Beyer
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;

public class ASTConverterTest {
  private static JClassType jClassType;

  @BeforeClass
  public static void init() {
    jClassType = createStringJClassType("java.lang.String", "String");
  }

  @Test
  public void testGetClassOfJType() {
    Optional<Class<?>> optionalOfPrimitiveType =
        ASTConverter.getClassOfJType(JSimpleType.getBoolean(), ImmutableSet.of());
    assertThat(optionalOfPrimitiveType.get()).isEqualTo(boolean.class);
  }

  @Test
  public void testGetClassOfPrimitiveType() {
    assertThat(ASTConverter.getClassOfPrimitiveType(JSimpleType.getInt())).isEqualTo(int.class);

    assertThat(ASTConverter.getClassOfPrimitiveType(JSimpleType.getLong())).isEqualTo(long.class);

    assertThat(ASTConverter.getClassOfPrimitiveType(JSimpleType.getVoid())).isEqualTo(void.class);
  }

  @Test
  public void testGetClassOfJTypeForNonPrimitiveType() {
    Optional<Class<?>> optionalOfStringClass =
        ASTConverter.getClassOfJType(jClassType, ImmutableSet.of());
    assertThat(optionalOfStringClass.get()).isEqualTo(String.class);
  }

  @Test
  public void testGetArrayClass() {
    JArrayType jArrayTypeOfString = new JArrayType(jClassType, 3);
    Optional<Class<?>> optionalOfArrayClass =
        ASTConverter.getClassOfJType(jArrayTypeOfString, ImmutableSet.of());
    assertThat(optionalOfArrayClass.get().isArray()).isTrue();
    assertThat(optionalOfArrayClass.get().toGenericString()).isEqualTo("java.lang.String[][][]");
  }

  private static JClassType createStringJClassType(String pFullyQualifiedName, String pString) {
    return JClassType.valueOf(
        pFullyQualifiedName,
        pString,
        VisibilityModifier.PUBLIC,
        true,
        false,
        false,
        JClassType.getTypeOfObject(),
        ImmutableSet.of());
  }

  @Test
  public void testUnboxing() {
    JClassType jClassTypeOfInteger = createStringJClassType("java.lang.Integer", "Integer");
    assertThat(ASTConverter.unboxJClassType(jClassTypeOfInteger).get()).isEqualTo(JBasicType.INT);
  }
}
