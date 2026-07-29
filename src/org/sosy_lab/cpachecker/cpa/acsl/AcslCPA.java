// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * This is a CPA whose goal is to find and inject ACSL invariants. For now this class is used for
 * testing and proof of concept.
 */
@Options(prefix = "cpa.acsl")
public class AcslCPA extends AbstractCPA implements ConfigurableProgramAnalysis {
  protected AcslCPA(
      String mergeType,
      String stopType,
      AbstractDomain domain,
      @Nullable TransferRelation transfer) {
    super(mergeType, stopType, domain, transfer);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(AcslCPA.class);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return null;
  }
}
