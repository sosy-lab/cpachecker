// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import com.google.common.testing.EqualsTester;
import java.nio.file.Path;
import org.junit.Test;

public class FileLocationTest {

  @Test
  public void testEquals() {
    final Path p = Path.of("f1");
    new EqualsTester()
        .addEqualityGroup(
            new FileLocation(p, 1, 1, 1, 1), new FileLocation(p, "f1", 1, 1, 1, 1, 10, 10, true))
        .addEqualityGroup(new FileLocation(Path.of("f2"), 1, 1, 1, 1))
        .addEqualityGroup(new FileLocation(p, 2, 1, 1, 1))
        .addEqualityGroup(new FileLocation(p, 1, 2, 1, 1))
        .addEqualityGroup(new FileLocation(p, 1, 1, 2, 1))
        .addEqualityGroup(new FileLocation(p, 1, 1, 1, 2))
        .testEquals();
  }
}
