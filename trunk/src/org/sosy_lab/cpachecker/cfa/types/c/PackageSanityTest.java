// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import com.google.common.base.Predicates;
import com.google.common.testing.AbstractPackageSanityTests;
import org.junit.Ignore;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;

@SuppressWarnings("JUnitAmbiguousTestClass") // because of AbstractPackageSanityTests
public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    setDefault(ComplexTypeKind.class, ComplexTypeKind.STRUCT);
    setDefault(CType.class, CVoidType.VOID);

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
