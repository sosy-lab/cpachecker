// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
        description = "Refiner that SlicingDelegatingRefiner should delegate to")
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

    Refiner delegate =
        options.delegate.create(pCpa, slicingCPA.getLogger(), slicingCPA.getShutdownNotifier());
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

    final boolean sliceRefinementSuccessful =
        slicingRefiner.updatePrecisionAndRemoveSubtree(pReached);
    if (sliceRefinementSuccessful) {
      return true;
    }

    boolean refinementResult = delegate.performRefinement(pReached);
    // Update counterexamples to be imprecise, because the program slice
    // may not reflect the real program semantics, but reflects the real program
    // syntax
    if (!refinementResult) {
      for (ARGPath targetPath : slicingRefiner.getTargetPaths(pReached)) {
        if (slicingRefiner.isFeasible(targetPath)) {
          CounterexampleInfo cex = slicingRefiner.getCounterexample(targetPath);
          targetPath.getLastState().replaceCounterexampleInformation(cex);
        }
      }
    }
    return refinementResult;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    if (delegate instanceof Statistics) {
      statsCollection.add((Statistics) delegate);
    } else if (delegate instanceof StatisticsProvider) {
      ((StatisticsProvider) delegate).collectStatistics(statsCollection);
    }
  }
}
