/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.SerializableTester;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;

public class CFunctionTypeWithNamesTest {

  @Test
  public void testSerializable() {
    // Serialization of CFunctionTypeWithNames looses parameter names and returns a CFunctionType,
    // so we need to test this manually.
    CParameterDeclaration param =
        new CParameterDeclaration(FileLocation.DUMMY, CNumericTypes.INT, "param");
    CFunctionTypeWithNames orig =
        new CFunctionTypeWithNames(true, true, CNumericTypes.INT, ImmutableList.of(param), true);
    Object reserialized = SerializableTester.reserialize(orig);
    assertThat(reserialized).isInstanceOf(CFunctionType.class);
    assertThat(reserialized).isEqualTo(orig); // this holds, orig.equals(reserialized) not
  }

  @Test
  public void testStringRepr_FunctionReturningPointer() {
    CType returnType = new CPointerType(false, false, CNumericTypes.DOUBLE);
    CType type =
        new CFunctionType(false, false, returnType, ImmutableList.of(CNumericTypes.INT), false);
    assertThat(type.toASTString("test")).isEqualTo("double *test(int)");
  }

  @Test
  public void testStringRepr_FunctionReturningFunctionPointer() {
    CType returnType =
        new CPointerType(
            false,
            false,
            new CFunctionType(
                false, false, CVoidType.VOID, ImmutableList.of(CNumericTypes.DOUBLE), false));
    CType type =
        new CFunctionType(false, false, returnType, ImmutableList.of(CNumericTypes.INT), false);
    assertThat(type.toASTString("test")).isEqualTo("void (*test(int))(double)");
  }

  @Test
  public void testStringRepr_FunctionReturningFunctionPointerReturningPointer() {
    CType returnType =
        new CPointerType(
            false,
            false,
            new CFunctionType(
                false,
                false,
                new CPointerType(false, false, CNumericTypes.CHAR),
                ImmutableList.of(CNumericTypes.DOUBLE),
                false));
    CType type =
        new CFunctionType(false, false, returnType, ImmutableList.of(CNumericTypes.INT), false);
    assertThat(type.toASTString("test")).isEqualTo("char *(*test(int))(double)");
  }
}
