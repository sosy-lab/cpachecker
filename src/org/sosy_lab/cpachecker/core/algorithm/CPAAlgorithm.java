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
package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCovering;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WaitlistElement;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

public class CPAAlgorithm extends AbstractCPAAlgorithm {

  @Options(prefix="cpa")
  public static class CPAAlgorithmFactory {

    @Option(
      secure = true,
      description = "Which strategy to use for forced coverings (empty for none)",
      name = "forcedCovering"
    )
    @ClassOption(packagePrefix = "org.sosy_lab.cpachecker")
    private @Nullable ForcedCovering.Factory forcedCoveringClass = null;

    @Option(secure=true, description="Do not report 'False' result, return UNKNOWN instead. "
        + " Useful for incomplete analysis with no counterexample checking.")
    private boolean reportFalseAsUnknown = false;

    private final ForcedCovering forcedCovering;

    private final ConfigurableProgramAnalysis cpa;
    private final LogManager logger;
    private final ShutdownNotifier shutdownNotifier;

    public CPAAlgorithmFactory(ConfigurableProgramAnalysis cpa, LogManager logger,
        Configuration config, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {

      config.inject(this);
      this.cpa = cpa;
      this.logger = logger;
      this.shutdownNotifier = pShutdownNotifier;

      if (forcedCoveringClass != null) {
        forcedCovering = forcedCoveringClass.create(config, logger, cpa);
      } else {
        forcedCovering = null;
      }

    }

    public CPAAlgorithm newInstance() {
      return new CPAAlgorithm(cpa, logger, shutdownNotifier, forcedCovering, reportFalseAsUnknown);
    }
  }

  public static CPAAlgorithm create(ConfigurableProgramAnalysis cpa, LogManager logger,
      Configuration config, ShutdownNotifier pShutdownNotifier) throws InvalidConfigurationException {

    return new CPAAlgorithmFactory(cpa, logger, config, pShutdownNotifier).newInstance();
  }


  private CPAAlgorithm(ConfigurableProgramAnalysis cpa, LogManager logger,
      ShutdownNotifier pShutdownNotifier,
      ForcedCovering pForcedCovering,
      boolean pIsImprecise) {

    super(cpa, logger, pShutdownNotifier, pForcedCovering, pIsImprecise);
  }

  @Override
  protected void frontier(ReachedSet pReached, AbstractState pSuccessor, Precision pPrecision) {
    pReached.add(pSuccessor, pPrecision);
  }


  @Override
  protected void update(ReachedSet pReachedSet, List<AbstractState> pToRemove,
      List<Pair<AbstractState, Precision>> pToAdd) {

    pReachedSet.removeAll(pToRemove);
    pReachedSet.addAll(pToAdd);
  }


  @Override
  protected Collection<Pair<? extends AbstractState, ? extends Precision>> getAbstractSuccessors(
      WaitlistElement pElement)
      throws CPATransferException, InterruptedException {

    //TODO Array?
    Preconditions.checkArgument(pElement instanceof DefaultWaitlistElement);

    Collection<Pair<? extends AbstractState, ? extends Precision>> result = Sets.newHashSet();
    DefaultWaitlistElement element = (DefaultWaitlistElement) pElement;
    AbstractState state = element.getAbstractState();
    Precision prec = element.getPrecision();
    Collection<? extends AbstractState> successors =
        transferRelation.getAbstractSuccessors(state, prec);
    successors.forEach(s -> result.add(Pair.of(s, prec)));

    return result;
  }
}
