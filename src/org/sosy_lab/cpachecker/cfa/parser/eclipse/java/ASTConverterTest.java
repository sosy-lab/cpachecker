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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nonnull;
import org.junit.Assert;
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
        ASTConverter.getClassOfJType(JSimpleType.getBoolean());
    Assert.assertEquals(boolean.class, optionalOfPrimitiveType.get());
  }

  @Test
  public void testGetClassOfPrimitiveType() {
    Optional<Class<?>> optionalOfPrimitiveType =
        ASTConverter.getClassOfPrimitiveType(JSimpleType.getInt());
    Assert.assertEquals(int.class, optionalOfPrimitiveType.get());

    optionalOfPrimitiveType = ASTConverter.getClassOfPrimitiveType(JSimpleType.getLong());
    Assert.assertEquals(long.class, optionalOfPrimitiveType.get());

    optionalOfPrimitiveType = ASTConverter.getClassOfPrimitiveType(JSimpleType.getVoid());
    Assert.assertEquals(Optional.absent(), optionalOfPrimitiveType);
  }

  @Test
  public void testGetClassOfJTypeForNonPrimitiveType() {
    JClassOrInterfaceType jClassOrInterfaceType = createStringJClassOrInterfaceType();
    Optional<Class<?>> optionalOfStringClass = ASTConverter.getClassOfJType(jClassOrInterfaceType);
    Assert.assertEquals(String.class, optionalOfStringClass.get());
  }

  @Test
  public void testGetArrayClass() {
    JArrayType jArrayTypeOfString = new JArrayType(createStringJClassOrInterfaceType(), 3);
    Optional<Class<?>> optionalOfArrayClass = ASTConverter.getClassOfJType(jArrayTypeOfString);
    Assert.assertTrue(optionalOfArrayClass.get().isArray());
    Assert.assertEquals("java.lang.String[][][]", optionalOfArrayClass.get().toGenericString());
  }

  @Nonnull
  private JClassOrInterfaceType createStringJClassOrInterfaceType() {
    return JClassType.valueOf(
        "java.lang.String",
        "String",
        VisibilityModifier.PUBLIC,
        true,
        false,
        false,
        JClassType.createUnresolvableType(),
        ImmutableSet.of());
  }
}
