/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.loopbound;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "cpa.loopbound")
public class LoopBoundPrecisionAdjustment implements PrecisionAdjustment {

  @Option(secure=true, description="threshold for unrolling loops of the program (0 is infinite)\n"
  + "works only if assumption storage CPA is enabled, because otherwise it would be unsound")
  private int maxLoopIterations = 0;

  @Option(secure=true, description="threshold for adjusting the threshold for unrolling loops of the program (0 is infinite).\n"
  + "only relevant in combination with a non-static maximum loop iteration adjuster.")
  private int maxLoopIterationsUpperBound = 0;

  @Option(secure=true, description="this option controls how the maxLoopIterations condition is adjusted when a condition adjustment is invoked.")
  private MaxLoopIterationAdjusters maxLoopIterationAdjusterFactory = MaxLoopIterationAdjusters.STATIC;

  @Option(secure=true,
      description="Number of loop iterations before the loop counter is"
          + " abstracted. Zero is equivalent to no limit.")
  private int loopIterationsBeforeAbstraction = 0;

  private final LogManager logger;

  public LoopBoundPrecisionAdjustment(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    pConfig.inject(this);
    if (maxLoopIterations < 0) {
      throw new InvalidConfigurationException("cpa.loopbound.maxLoopIterations must be a non-negative value, but is set to " + maxLoopIterations);
    }
    logger = pLogger;
  }

  public int getMaxLoopIterations() {
    return maxLoopIterations;
  }

  public void setMaxLoopIterations(int pMaxLoopIterations) {
    Preconditions.checkArgument(pMaxLoopIterations >= 0);
    maxLoopIterations = pMaxLoopIterations;
  }

  public int getLoopIterationsBeforeAbstraction() {
    return loopIterationsBeforeAbstraction == 0 ? Integer.MAX_VALUE : loopIterationsBeforeAbstraction;
  }

  private void setLoopIterationsBeforeAbstraction(int pLoopIterationsBeforeAbstraction) {
    Preconditions.checkArgument(pLoopIterationsBeforeAbstraction >= 0);
    loopIterationsBeforeAbstraction = pLoopIterationsBeforeAbstraction;
  }

  public void incrementLoopIterationsBeforeAbstraction() {
    setLoopIterationsBeforeAbstraction(getLoopIterationsBeforeAbstraction() + 1);
  }

  @Override
  public String toString() {
    return "k = " + maxLoopIterations + ", adjustment strategy = " + maxLoopIterationAdjusterFactory;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(AbstractState pState, Precision pPrecision,
      UnmodifiableReachedSet pStates, Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState) throws CPAException, InterruptedException {

    LoopBoundPrecision precision = (LoopBoundPrecision) pPrecision;
    LoopBoundPrecision adjustedPrecision = precision
        .withMaxLoopIterations(maxLoopIterations)
        .withLoopIterationsBeforeAbstraction(getLoopIterationsBeforeAbstraction());

    LoopBoundState state = (LoopBoundState) pState;
    LoopBoundState adjustedState = state
        .setStop(maxLoopIterations > 0 && state.getDeepestIteration() > maxLoopIterations)
        .enforceAbstraction(getLoopIterationsBeforeAbstraction());

    PrecisionAdjustmentResult result = PrecisionAdjustmentResult.create(adjustedState, adjustedPrecision, Action.CONTINUE);

    return Optional.of(result);
  }

  private static interface MaxLoopIterationAdjuster {

    int adjust(int currentValue);

    boolean canAdjust(int currentValue);

  }

  private static interface MaxLoopIterationAdjusterFactory {

    MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(LoopBoundPrecisionAdjustment pPrecisionAdjustment);

  }

  private static enum MaxLoopIterationAdjusters implements MaxLoopIterationAdjusterFactory {

    STATIC {

      @Override
      public MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(LoopBoundPrecisionAdjustment pPrecisionAdjustment) {
        return StaticLoopIterationAdjuster.INSTANCE;
      }

    },

    INCREMENT {

      @Override
      public MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(LoopBoundPrecisionAdjustment pPrecisionAdjustment) {
        return new IncrementalLoopIterationAdjuster(pPrecisionAdjustment);
      }

    },

    DOUBLE {

      @Override
      public MaxLoopIterationAdjuster getMaxLoopIterationAdjuster(LoopBoundPrecisionAdjustment pPrecisionAdjustment) {
        return new DoublingLoopIterationAdjuster(pPrecisionAdjustment);
      }


    }

  }

  private static enum StaticLoopIterationAdjuster implements MaxLoopIterationAdjuster {

    INSTANCE;

    @Override
    public int adjust(int pCurrentValue) {
      return pCurrentValue;
    }

    @Override
    public boolean canAdjust(int pCurrentValue) {
      return false;
    }

  }

  private static class IncrementalLoopIterationAdjuster implements MaxLoopIterationAdjuster {

    private final LoopBoundPrecisionAdjustment precisionAdjustment;

    public IncrementalLoopIterationAdjuster(LoopBoundPrecisionAdjustment pPrecisionAdjustment) {
      this.precisionAdjustment = pPrecisionAdjustment;
    }

    @Override
    public int adjust(int pCurrentValue) {
      return ++pCurrentValue;
    }

    @Override
    public boolean canAdjust(int pCurrentValue) {
      return precisionAdjustment.maxLoopIterationsUpperBound <= 0 || pCurrentValue < precisionAdjustment.maxLoopIterationsUpperBound;
    }

  }

  private static class DoublingLoopIterationAdjuster implements MaxLoopIterationAdjuster {

    private final LoopBoundPrecisionAdjustment precisionAdjustment;

    public DoublingLoopIterationAdjuster(LoopBoundPrecisionAdjustment pPrecisionAdjustment) {
      this.precisionAdjustment = pPrecisionAdjustment;
    }

    @Override
    public int adjust(int pCurrentValue) {
      return 2 * pCurrentValue;
    }

    @Override
    public boolean canAdjust(int pCurrentValue) {
      return precisionAdjustment.maxLoopIterationsUpperBound <= 0 || pCurrentValue * 2 <= precisionAdjustment.maxLoopIterationsUpperBound;
    }

  }

  public boolean nextState() {
    MaxLoopIterationAdjuster maxLoopIterationAdjuster = this.maxLoopIterationAdjusterFactory.getMaxLoopIterationAdjuster(this);
    if (maxLoopIterationAdjuster.canAdjust(getMaxLoopIterations())) {
      int adjustedMaxLoopIterations = maxLoopIterationAdjuster.adjust(getMaxLoopIterations());
      logger.log(Level.INFO, "Adjusting maxLoopIterations to " + adjustedMaxLoopIterations);
      setMaxLoopIterations(adjustedMaxLoopIterations);
      return true;
    }
    return false;
  }

}
