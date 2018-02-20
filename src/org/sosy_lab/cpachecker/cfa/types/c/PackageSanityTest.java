/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
  public void testEquals() throws Exception {
    // equals methods of CTypes are not testable with PackageSanityTest
    // because of field origName
  }
}
