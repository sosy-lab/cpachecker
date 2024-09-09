// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.cpa.smg2.util.ListDebugger.ListElement.ListType.DLL;
import static org.sosy_lab.cpachecker.cpa.smg2.util.ListDebugger.ListElement.ListType.REG;
import static org.sosy_lab.cpachecker.cpa.smg2.util.ListDebugger.ListElement.ListType.SLL;
import static org.sosy_lab.cpachecker.cpa.smg2.util.ListDebugger.ListElement.ListType.ZERO;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.smg2.util.ListDebugger.ListElement;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

public class ListDebuggerTest {

  @Test
  public void testContains() {
    SymbolicValueFactory symFactory = SymbolicValueFactory.getInstance();
    ListElement nestedZero =
        new ListElement(
            Optional.empty(),
            ZERO,
            64,
            32,
            0,
            Optional.empty(),
            Optional.empty(),
            ImmutableMap.of(),
            ImmutableMap.of());
    ListElement nestedNonZeroWZeroValue =
        new ListElement(
            Optional.empty(),
            REG,
            64,
            32,
            0,
            Optional.empty(),
            Optional.empty(),
            ImmutableMap.of(0, 0),
            ImmutableMap.of());
    ListElement nestedNonZeroWAbstrValue1 =
        new ListElement(
            Optional.empty(),
            REG,
            64,
            32,
            0,
            Optional.empty(),
            Optional.empty(),
            ImmutableMap.of(
                0, symFactory.asConstant(symFactory.newIdentifier(null), CNumericTypes.INT)),
            ImmutableMap.of());
    Value symValue = symFactory.asConstant(symFactory.newIdentifier(null), CNumericTypes.INT);
    ListElement nestedNonZeroWAbstrValue2 =
        new ListElement(
            Optional.empty(),
            REG,
            64,
            32,
            0,
            Optional.empty(),
            Optional.empty(),
            ImmutableMap.of(0, symValue),
            ImmutableMap.of());
    ListElement nestedNonZeroWAbstrValue2Copy =
        new ListElement(
            Optional.empty(),
            REG,
            64,
            32,
            0,
            Optional.empty(),
            Optional.empty(),
            ImmutableMap.of(0, symValue),
            ImmutableMap.of());

    ListElement cllEmpty =
        new ListElement(
            Optional.empty(),
            REG,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of());
    // Each nested list is ZERO terminated
    ListElement cllNestedZero =
        new ListElement(
            Optional.empty(),
            REG,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedZero)));
    ListElement cllNestedWAZeroValue =
        new ListElement(
            Optional.empty(),
            REG,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedNonZeroWZeroValue, nestedZero)));
    ListElement cllNestedWAbstrValue1 =
        new ListElement(
            Optional.empty(),
            REG,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedNonZeroWAbstrValue1, nestedZero)));
    ListElement cllNestedWAbstrValue2 =
        new ListElement(
            Optional.empty(),
            REG,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedNonZeroWAbstrValue2, nestedZero)));
    ListElement cllNestedWAbstrValue2Copy =
        new ListElement(
            Optional.empty(),
            REG,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedNonZeroWAbstrValue2, nestedZero)));
    ListElement cllNestedWAbstrValue2CopyCopy =
        new ListElement(
            Optional.empty(),
            REG,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedNonZeroWAbstrValue2Copy, nestedZero)));

    List<Set<ListElement>> allCLLEqSets =
        ImmutableList.of(
            ImmutableSet.of(cllNestedZero),
            ImmutableSet.of(cllEmpty),
            ImmutableSet.of(cllNestedWAZeroValue),
            ImmutableSet.of(cllNestedWAbstrValue1),
            ImmutableSet.of(
                cllNestedWAbstrValue2, cllNestedWAbstrValue2CopyCopy, cllNestedWAbstrValue2Copy));

    ListElement sll1Empty =
        new ListElement(
            Optional.of(1),
            SLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of());
    ListElement sll1NestedZero =
        new ListElement(
            Optional.of(1),
            SLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedZero)));

    ListElement sll2Empty =
        new ListElement(
            Optional.of(2),
            SLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of());
    ListElement sll2NestedZero =
        new ListElement(
            Optional.of(2),
            SLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedZero)));

    ListElement sll1Empty2 =
        new ListElement(
            Optional.of(1),
            SLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of());
    ListElement sll1NestedZero2 =
        new ListElement(
            Optional.of(1),
            SLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedZero)));

    ListElement sll2Empty2 =
        new ListElement(
            Optional.of(2),
            SLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of());
    ListElement sll2NestedZero2 =
        new ListElement(
            Optional.of(2),
            SLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedZero)));

    ListElement sll1WNestedWSymValue =
        new ListElement(
            Optional.of(1),
            SLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedNonZeroWAbstrValue2, nestedZero)));
    ListElement sll2WNestedWSymValue =
        new ListElement(
            Optional.of(2),
            SLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedNonZeroWAbstrValue2, nestedZero)));

    List<Set<ListElement>> allSllEqSets =
        ImmutableList.of(
            ImmutableSet.of(sll1Empty, sll1Empty2),
            ImmutableSet.of(sll1NestedZero, sll1NestedZero2),
            ImmutableSet.of(sll2Empty, sll2Empty2),
            ImmutableSet.of(sll2NestedZero, sll2NestedZero2),
            ImmutableSet.of(sll1WNestedWSymValue),
            ImmutableSet.of(sll2WNestedWSymValue));

    ListElement dll1Empty =
        new ListElement(
            Optional.of(1),
            DLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of());
    ListElement dll1NestedZero =
        new ListElement(
            Optional.of(1),
            DLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedZero)));

    ListElement dll2Empty =
        new ListElement(
            Optional.of(2),
            DLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of());
    ListElement dll2NestedZero =
        new ListElement(
            Optional.of(2),
            DLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedZero)));

    ListElement dll1Empty2 =
        new ListElement(
            Optional.of(1),
            DLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of());
    ListElement dll1NestedZero2 =
        new ListElement(
            Optional.of(1),
            DLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedZero)));

    ListElement dll2Empty2 =
        new ListElement(
            Optional.of(2),
            DLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of());
    ListElement dll2NestedZero2 =
        new ListElement(
            Optional.of(2),
            DLL,
            96,
            32,
            0,
            Optional.of(64),
            Optional.of(0),
            ImmutableMap.of(),
            ImmutableMap.of(0, ImmutableList.of(nestedZero)));

    List<Set<ListElement>> allDllEqSets =
        ImmutableList.of(
            ImmutableSet.of(dll1Empty, dll1Empty2),
            ImmutableSet.of(dll1NestedZero, dll1NestedZero2),
            ImmutableSet.of(dll2Empty, dll2Empty2),
            ImmutableSet.of(dll2NestedZero, dll2NestedZero2));

    List<Set<ListElement>> allEqSets =
        ImmutableList.<Set<ListElement>>builder()
            .addAll(allDllEqSets)
            .addAll(allCLLEqSets)
            .addAll(allSllEqSets)
            .build();

    for (Set<ListElement> equalElements : allEqSets) {
      for (ListElement eqListElem1 : equalElements) {
        for (ListElement eqListElem2 : equalElements) {
          assertThat(eqListElem1).isEqualTo(eqListElem2);
        }
      }
      for (Set<ListElement> allElements : allDllEqSets) {
        if (allElements == equalElements) {
          continue;
        }
        for (ListElement notEqListElem1 : equalElements) {
          for (ListElement notEqListElem2 : allElements) {
            assertThat(notEqListElem1).isNotEqualTo(notEqListElem2);
          }
        }
      }
    }
  }
}
