// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.variableclassification;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.AbstractPackageSanityTests;
import java.util.HashMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;

public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    setDefault(
        LogManagerWithoutDuplicates.class,
        new LogManagerWithoutDuplicates(LogManager.createTestLogManager()));
    setDefault(Configuration.class, Configuration.defaultConfiguration());
    setDefault(CFANode.class, CFANode.newDummyCFANode());

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
