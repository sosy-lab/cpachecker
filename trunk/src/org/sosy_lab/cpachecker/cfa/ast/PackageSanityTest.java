// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import com.google.common.testing.AbstractPackageSanityTests;
import java.nio.file.Path;

public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    setDefault(Path.class, Path.of("test"));
  }
}
