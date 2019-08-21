/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector.PrefixPreference;

/**
 * {@link PathExtractor} that sorts paths by their length or interpolant quality.
 * To sort paths by their interpolant quality, set {@link #itpSortedTargets} by specifying
 * configuration property <code>cpa.value.refinement.itpSortedTargets</code>.
 */
@Options(prefix = "cpa.value.refinement")
public class SortingPathExtractor extends PathExtractor {

  @Option(
      secure = true,
      description = "heuristic to sort targets based on the quality of interpolants derivable from them")
  private boolean itpSortedTargets = false;

  private final PrefixSelector prefixSelector;
  private final PrefixProvider prefixProvider;

  public SortingPathExtractor(
      final PrefixProvider pPrefixProvider,
      final PrefixSelector pPrefixSelector,
      final LogManager pLogger,
      final Configuration pConfig
  ) throws InvalidConfigurationException {

    super(pLogger, pConfig);
    pConfig.inject(this, SortingPathExtractor.class);

    prefixProvider = pPrefixProvider;
    prefixSelector = pPrefixSelector;
  }

  /**
   * This method returns an unsorted, non-empty collection of target states
   * found during the analysis.
   *
   * @param pReached the set of reached states
   * @return the target states
   */
  @Override
  public Collection<ARGState> getTargetStates(final ARGReachedSet pReached) throws RefinementFailedException {
    final Collection<ARGState> targetStates = super.getTargetStates(pReached);
    final Map<ARGState, Integer> targetsWithScores = Maps.toMap(targetStates, this::getScore);
    // sort keys by their values
    return ImmutableList.sortedCopyOf(Comparator.comparing(targetsWithScores::get), targetStates);
  }

  private int getScore(ARGState target) {
    ARGPath path = ARGUtils.getOnePathTo(target);
    if (itpSortedTargets) {
      List<InfeasiblePrefix> prefixes;
      try {
        prefixes = prefixProvider.extractInfeasiblePrefixes(path);
      } catch (CPAException | InterruptedException e) {
        throw new AssertionError(e);
      }
      return prefixSelector.obtainScoreForPrefixes(prefixes, PrefixPreference.DOMAIN_MIN);
    } else {
      return path.size();
    }
  }
}