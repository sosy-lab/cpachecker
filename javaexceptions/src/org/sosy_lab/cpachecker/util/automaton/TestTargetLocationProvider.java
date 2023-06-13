// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.automaton;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.specification.Specification;

public class TestTargetLocationProvider implements TargetLocationProvider {

  private final Set<CFAEdge> testTargets;

  public TestTargetLocationProvider(final Set<CFAEdge> pTestTargets) {
    testTargets = pTestTargets;
  }

  @Override
  public ImmutableSet<CFANode> tryGetAutomatonTargetLocations(
      final CFANode pRootNode, final Specification pSpecification) {
    return transformedImmutableSetCopy(testTargets, CFAEdge::getSuccessor);
  }
}
