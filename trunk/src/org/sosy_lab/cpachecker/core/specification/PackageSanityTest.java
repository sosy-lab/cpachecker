// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.specification;

import com.google.common.testing.AbstractPackageSanityTests;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;

public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    setDefault(Configuration.class, Configuration.defaultConfiguration());
    setDefault(LogManager.class, LogManager.createTestLogManager());
    setDefault(ShutdownNotifier.class, ShutdownNotifier.createDummy());
  }
}
