// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
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
        ASTConverter.getClassOfJType(JSimpleType.BOOLEAN, ImmutableSet.of());
    assertThat(optionalOfPrimitiveType.orElseThrow()).isEqualTo(boolean.class);
  }

  @Test
  public void testGetClassOfPrimitiveType() {
    assertThat(ASTConverter.getClassOfPrimitiveType(JSimpleType.INT)).isEqualTo(int.class);

    assertThat(ASTConverter.getClassOfPrimitiveType(JSimpleType.LONG)).isEqualTo(long.class);

    assertThat(ASTConverter.getClassOfPrimitiveType(JSimpleType.VOID)).isEqualTo(void.class);
  }

  @Test
  public void testGetClassOfJTypeForNonPrimitiveType() {
    Optional<Class<?>> optionalOfStringClass =
        ASTConverter.getClassOfJType(jClassType, ImmutableSet.of());
    assertThat(optionalOfStringClass.orElseThrow()).isEqualTo(String.class);
  }

  @Test
  public void testGetArrayClass() {
    JArrayType jArrayTypeOfString = new JArrayType(jClassType, 3);
    Optional<Class<?>> optionalOfArrayClass =
        ASTConverter.getClassOfJType(jArrayTypeOfString, ImmutableSet.of());
    assertThat(optionalOfArrayClass.orElseThrow().isArray()).isTrue();
    assertThat(optionalOfArrayClass.orElseThrow().toGenericString())
        .isEqualTo("java.lang.String[][][]");
  }

  private static JClassType createStringJClassType(String pFullyQualifiedName, String pString) {
    return JClassType.valueOf(
        pFullyQualifiedName,
        pString,
        VisibilityModifier.PUBLIC,
        true,
        false,
        false,
        JClassType.createObjectType(),
        ImmutableSet.of());
  }

  @Test
  public void testUnboxing() {
    JClassType jClassTypeOfInteger = createStringJClassType("java.lang.Integer", "Integer");
    assertThat(ASTConverter.unboxJClassType(jClassTypeOfInteger).orElseThrow())
        .isEqualTo(JSimpleType.INT);
  }
}
