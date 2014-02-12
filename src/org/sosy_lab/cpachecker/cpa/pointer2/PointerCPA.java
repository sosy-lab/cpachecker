/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.pointer2;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;

/**
 * Instances of this class are configurable program analyses for analyzing a
 * program to gain information about pointer aliasing.
 */
public class PointerCPA extends AbstractCPA {

  @Options(prefix="cpa.pointer2")
  public static class PointerOptions {

    @Option(values={"JOIN", "SEP"}, toUppercase=true,
        description="which merge operator to use for InvariantCPA")
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
   * @param config the configuration used.
   * @param logger the log manager used.
   * @param options the configured options.
   * @param pReachedSetFactory the reached set factory used.
   * @param pCfa the control flow automaton to analyze.
   * @throws InvalidConfigurationException if the configuration is invalid.
   */
  public PointerCPA(Configuration config, LogManager logger, PointerOptions options,
      ShutdownNotifier pShutdownNotifier, ReachedSetFactory pReachedSetFactory, CFA pCfa) throws InvalidConfigurationException {
    super(options.merge, "SEP", PointerDomain.INSTANCE, PointerTransferRelation.INSTANCE);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    return PointerState.INITIAL_STATE;
  }

}
