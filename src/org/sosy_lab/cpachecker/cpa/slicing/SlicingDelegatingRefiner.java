/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.slicing;

import java.util.Collection;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * Refiner for {@link SlicingCPA} that works in tandem with another precision refinement procedure.
 * If you use this refiner, always set <code>cpa.slicing.refinement.takeEagerSlice=true</code> to
 * ensure that full program slices are used.
 *
 * @see SlicingRefiner
 */
public class SlicingDelegatingRefiner implements Refiner, StatisticsProvider {

  private final Refiner delegate;
  private final SlicingRefiner slicingRefiner;

  @Options(prefix = "SlicingDelegatingRefiner")
  private static class SlicingDelegatingRefinerOptions {

    @Option(
      secure = true,
      name = "refiner",
      required = true,
      description = "Refiner that SlicingDelegatingRefiner should delegate to"
    )
    @ClassOption(packagePrefix = "org.sosy_lab.cpachecker")
    private Refiner.Factory delegate = null;
  }

  public static SlicingDelegatingRefiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    SlicingCPA slicingCPA =
        CPAs.retrieveCPAOrFail(pCpa, SlicingCPA.class, SlicingDelegatingRefiner.class);

    Configuration config = slicingCPA.getConfig();
    SlicingDelegatingRefinerOptions options = new SlicingDelegatingRefinerOptions();
    config.inject(options);

    if (options.delegate == null) {
      throw new InvalidConfigurationException("Option SlicingDelegatingRefiner.refiner not set");
    }

    Refiner delegate = options.delegate.create(pCpa);
    SlicingRefiner slicingRefiner = SlicingRefiner.create(pCpa);

    return new SlicingDelegatingRefiner(delegate, slicingRefiner);
  }

  private SlicingDelegatingRefiner(
      final Refiner pDelegateRefiner, final SlicingRefiner pSlicingRefiner) {
    delegate = pDelegateRefiner;
    slicingRefiner = pSlicingRefiner;
  }

  @Override
  public boolean performRefinement(final ReachedSet pReached)
      throws CPAException, InterruptedException {

    // The delegate refiner will cut the ARG at the refinement root that is really necessary
    // -- this is, except for predicate analysis, equal to or lower in the ARG than the
    // refinement roots of slicing.
    // Thus, we don't have to remove any ARG nodes in or after slicing refinement.
    slicingRefiner.updatePrecision(pReached);
    boolean refinementResult = delegate.performRefinement(pReached);

    // Update counterexamples to be imprecise, because the program slice
    // may not reflect the real program semantics, but reflects the real program
    // syntax
    if (!refinementResult) {
      for (ARGPath targetPath : slicingRefiner.getTargetPaths(pReached)) {
        if (slicingRefiner.isFeasible(targetPath, pReached)) {
          CounterexampleInfo cex = slicingRefiner.getCounterexample(targetPath);
          targetPath.getLastState().addCounterexampleInformation(cex);
        }
      }
    }
    return refinementResult;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    slicingRefiner.collectStatistics(statsCollection);
    if (delegate instanceof Statistics) {
      statsCollection.add((Statistics) delegate);
    } else if (delegate instanceof StatisticsProvider) {
      ((StatisticsProvider) delegate).collectStatistics(statsCollection);
    }
  }
}
