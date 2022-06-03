// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.reachedset;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.AbstractPackageSanityTests;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.alwaystop.AlwaysTopCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    try {
      setDefault(
          Configuration.class,
          Configuration.builder().setOption("analysis.reachedSet", "normal").build());
    } catch (InvalidConfigurationException e) {
      throw new AssertionError(e);
    }
    setDefault(ARGPath.class, new ARGPath(ImmutableList.of(new ARGState(null, null))));
    setDefault(ConfigurableProgramAnalysis.class, AlwaysTopCPA.INSTANCE);
    setDefault(CFANode.class, CFANode.newDummyCFANode("test"));
    setDefault(StateSpacePartition.class, StateSpacePartition.getDefaultPartition());
  }
}
