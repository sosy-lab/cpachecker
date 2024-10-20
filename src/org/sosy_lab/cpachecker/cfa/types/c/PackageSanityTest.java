// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.AbstractPackageSanityTests;
import org.junit.Ignore;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;

@SuppressWarnings("JUnitAmbiguousTestClass") // because of AbstractPackageSanityTests
public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    setDefault(ComplexTypeKind.class, ComplexTypeKind.STRUCT);
    setDistinctValues(
        CComplexType.class,
        new CElaboratedType(false, false, ComplexTypeKind.STRUCT, "type", "type", null),
        new CEnumType(false, false, CNumericTypes.INT, ImmutableList.of(), "e", "e"));
    setDistinctValues(CType.class, CVoidType.VOID, CNumericTypes.INT);
    setDistinctValues(CSimpleType.class, CNumericTypes.INT, CNumericTypes.DOUBLE);
    setDefault(CExpression.class, CIntegerLiteralExpression.ONE);

    // CBitFieldType has its own test class
    ignoreClasses(Predicates.equalTo(CBitFieldType.class));
  }

  @Ignore
  @Override
  public void testEquals() {
    // equals methods of CTypes are not testable with PackageSanityTest
    // because of field origName
  }
}
