// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
        new CFunctionTypeWithNames(CNumericTypes.INT, ImmutableList.of(param), true);
    Object reserialized = SerializableTester.reserialize(orig);
    assertThat(reserialized).isInstanceOf(CFunctionType.class);
    assertThat(reserialized).isEqualTo(orig); // this holds, orig.equals(reserialized) not
  }
}
