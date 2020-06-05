// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import com.google.common.collect.Lists;
import com.google.common.testing.AbstractPackageSanityTests;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    setDefault(Configuration.class, Configuration.defaultConfiguration());
    setDefault(ARGPath.class, new ARGPath(Lists.newArrayList(new ARGState(null, null))));
  }
}
