/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.variableclassification;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.AbstractPackageSanityTests;
import java.util.HashMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    GlobalInfo.getInstance().storeLogManager(LogManager.createTestLogManager());
    setDefault(Configuration.class, Configuration.defaultConfiguration());
    setDefault(CFANode.class, new CFANode(CFunctionDeclaration.DUMMY));

    setDefault(VariableOrField.class, VariableOrField.unknown());
    setDefault(Partition.class, new Partition(new HashMap<>(), HashBasedTable.create()));

    CCompositeType dummystruct =
        new CCompositeType(
            false, false, ComplexTypeKind.STRUCT, ImmutableList.of(), "dummy", "dummy");
    CCompositeType dummyunion =
        new CCompositeType(
            false, false, ComplexTypeKind.UNION, ImmutableList.of(), "dummy", "dummy");
    setDefault(CCompositeType.class, dummystruct);

    setDistinctValues(CCompositeType.class, dummystruct, dummyunion);
  }
}
