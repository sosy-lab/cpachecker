/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.types;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;

@RunWith(Parameterized.class)
@SuppressFBWarnings(
  value = "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
  justification = "Fields are filled by parameterization of JUnit"
)
public class MachineModelGetOffsetTest {

  private final static String TEST_STRUCT = "testStruct";

  private final static String FIRST_BITFIELD_12 = "firstBitfield";
  private final static String SECOND_BITFIELD_10 = "secondBitfield";
  private final static String THIRD_INT = "thirdInt";
  private final static String LAST_INCOMPLETEARRAY = "lastIncomplete";

  @Parameters(name = "{2}: {0}")
  public static Object[][] machineModels() {
    // XXX: Note that this only holds as long as
    // the available MachineModels have a Byte-size
    // of 8 Bits and the used types ('int' and 'long long int')
    // have the same size in all of them.
    //
    // This premise holds for the currently (May 4th, 2017)
    // implemented MachineModels LINUX32 and LINUX64.
    // If a new MachineModel is introduced, which primitives
    // vary stronger in respect to the already implemented ones
    // or you want to test more exhaustively for more types,
    // say 'long int', that vary in size between LINUX32 and
    // LINUX64, you have to either implement the respective scenarios
    // separately or to think of a clever way to determine in-line
    // which expectation to apply.
    Object[][] types =
        new Object[][] {
          // fieldname          // expected offset in bits
          {FIRST_BITFIELD_12, 0},
          {SECOND_BITFIELD_10, 12},
          {THIRD_INT, 32},
          {LAST_INCOMPLETEARRAY, 64}
        };

    // Create a copy of types for each MachineModel and append the MachineModel instance in each row
    MachineModel[] machineModels = MachineModel.values();
    Object[][] result = new Object[machineModels.length * types.length][];
    for (int m = 0; m < machineModels.length; m++) {
      int offset = m * types.length;
      for (int t = 0; t < types.length; t++) {
        result[offset + t] = Arrays.copyOf(types[t], types[t].length + 1);
        result[offset + t][types[t].length] = machineModels[m];
      }
    }

    return result;
  }

  // If you plan on expanding this memberlist for more
  // tests of the struct, remember to update the expectation
  // for LAST_INCOMPLETEARRAY and please refrain from either
  // of these:
  //    a) (DO NOT) insert a new Member after LAST_INCOMPLETE
  //    b) (DO NOT) insert a new Member before THIRD_INT
  private static final ImmutableList<CCompositeTypeMemberDeclaration> FIELDS =
      ImmutableList.of(
          new CCompositeTypeMemberDeclaration(new CBitFieldType(CNumericTypes.UNSIGNED_INT, 12),
              FIRST_BITFIELD_12),
          new CCompositeTypeMemberDeclaration(new CBitFieldType(CNumericTypes.UNSIGNED_INT, 10),
              SECOND_BITFIELD_10),
          new CCompositeTypeMemberDeclaration(CNumericTypes.INT, THIRD_INT),
          new CCompositeTypeMemberDeclaration(
              new CArrayType(false, false, CNumericTypes.LONG_LONG_INT, null),
              LAST_INCOMPLETEARRAY));

  private static final CCompositeType STRUCT =
      new CCompositeType(false, false, ComplexTypeKind.STRUCT, FIELDS,
          TEST_STRUCT, TEST_STRUCT);

  @Parameter(0)
  public String testField;

  @Parameter(1)
  public int expectedOffset;

  @Parameter(2)
  public MachineModel model;

  @Test
  public void testGetFieldOffsetInStruct() {
    assertThat(model.getFieldOffsetInBits(STRUCT, testField)).isEqualTo(expectedOffset);
  }
}
