// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer2;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

/**
 * Instances of this class are configurable program analyses for analyzing a program to gain
 * information about pointer aliasing.
 */
public class PointerCPA extends AbstractCPA {

  @Options(prefix = "cpa.pointer2")
  public static class PointerOptions {

    @Option(
        secure = true,
        values = {"JOIN", "SEP"},
        toUppercase = true,
        description = "which merge operator to use for PointerCPA")
    private String merge = "JOIN";
  }

  /**
   * Gets a factory for creating PointerCPAs.
   *
   * @return a factory for creating PointerCPAs.
   */
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PointerCPA.class).withOptions(PointerOptions.class);
  }

  /**
   * Creates a PointerCPA.
   *
   * @param options the configured options.
   */
  public PointerCPA(PointerOptions options) {
    super(options.merge, "SEP", PointerDomain.INSTANCE, PointerTransferRelation.INSTANCE);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return PointerState.INITIAL_STATE;
  }
}
