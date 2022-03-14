// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.variableclassification;

import com.google.common.collect.HashBasedTable;
import com.google.common.testing.EqualsTester;
import java.util.HashMap;
import org.junit.Test;

public class PartitionTest {

  @Test
  public void testEquals() {
    Partition empty = new Partition(new HashMap<>(), HashBasedTable.create());
    Partition empty2 = new Partition(new HashMap<>(), HashBasedTable.create());

    Dependencies deps = new Dependencies();
    deps.addVar("a");
    Partition onePartition = deps.getPartitionForVar("a");

    new EqualsTester()
        .addEqualityGroup(empty)
        .addEqualityGroup(onePartition)
        .addEqualityGroup(empty2)
        .testEquals();
  }
}
