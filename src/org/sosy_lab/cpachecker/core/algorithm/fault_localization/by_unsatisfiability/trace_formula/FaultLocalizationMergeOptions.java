// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultUtil;

@Options(prefix = "faultLocalization.merge")
public class FaultLocalizationMergeOptions {

  public enum FaultLocalizationInfoMergeStrategy {
    /** Always use the most recent FaultLocalizationInfo instance */
    REPLACE {
      @Override
      public Fault merge(Fault pOldFault, Fault pNewFault) {
        return pNewFault;
      }
    },
    /** Take the intersection of error-prone CFAEdges */
    INTERSECTION {
      @Override
      public Fault merge(Fault pOldFault, Fault pNewFault) {
        return FaultUtil.intersection(pOldFault, pNewFault);
      }
    },
    /** Take the union of error-prone CFAEdges */
    UNION {
      @Override
      public Fault merge(Fault pOldFault, Fault pNewFault) {
        return FaultUtil.union(pOldFault, pNewFault);
      }
    },
    /** Take the intersection of error-prone CFAEdges but if one set is empty, take the other set */
    RELAXED_INTERSECTION {
      @Override
      public Fault merge(Fault pOldFault, Fault pNewFault) {
        if (pOldFault.isEmpty()) {
          return pNewFault;
        }
        if (pNewFault.isEmpty()) {
          return pOldFault;
        }
        return INTERSECTION.merge(pOldFault, pNewFault);
      }
    },
    /** Use previous list */
    PREVIOUS {
      @Override
      public Fault merge(Fault pOldFault, Fault pNewFault) {
        return pOldFault;
      }
    };

    public abstract Fault merge(Fault pOldFault, Fault pNewFault);
  }

  public enum FaultSelectionStrategy {
    MIN {
      @Override
      public List<Fault> select(List<Fault> pRanked) {
        if (!pRanked.isEmpty()) {
          return ImmutableList.of(
              pRanked.stream().min(Comparator.comparingInt(f -> f.size())).orElseThrow());
        }
        return pRanked;
      }
    },
    MAX {
      @Override
      public List<Fault> select(List<Fault> pRanked) {
        if (!pRanked.isEmpty()) {
          return ImmutableList.of(
              pRanked.stream().max(Comparator.comparingInt(f -> f.size())).orElseThrow());
        }
        return pRanked;
      }
    },
    FIRST {
      @Override
      public List<Fault> select(List<Fault> pRanked) {
        if (!pRanked.isEmpty()) {
          return ImmutableList.of(pRanked.get(0));
        }
        return pRanked;
      }
    },
    FIRST_NONEMPTY_MINIMAL {
      @Override
      public List<Fault> select(List<Fault> pRanked) {
        return ImmutableList.of(
            pRanked.stream()
                .sorted(Comparator.comparingInt(f -> f.size()))
                .filter(f -> !f.isEmpty())
                .findFirst()
                .orElse(new Fault()));
      }
    },
    FIRST_NONEMPTY_MAXIMAL {
      @Override
      public List<Fault> select(List<Fault> pRanked) {
        return ImmutableList.of(
            pRanked.stream()
                .sorted(Comparator.comparingInt(f -> -f.size()))
                .filter(f -> !f.isEmpty())
                .findFirst()
                .orElse(new Fault()));
      }
    },
    CROSSPRODUCT {
      @Override
      public List<Fault> select(List<Fault> pRanked) {
        return pRanked;
      }
    };

    public abstract List<Fault> select(List<Fault> pRanked);
  }

  @Option(description = "how to merge lists of faults")
  private FaultLocalizationInfoMergeStrategy mergeStrategy =
      FaultLocalizationInfoMergeStrategy.RELAXED_INTERSECTION;

  @Option(description = "which faults to use")
  private FaultSelectionStrategy selectionStrategy = FaultSelectionStrategy.FIRST;

  public FaultLocalizationMergeOptions(Configuration pConfiguration)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
  }

  public FaultLocalizationInfoMergeStrategy getMergeStrategy() {
    return mergeStrategy;
  }

  public FaultSelectionStrategy getSelectionStrategy() {
    return selectionStrategy;
  }
}
