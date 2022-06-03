// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.apron;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "cpa.apron.mergeop")
public class ApronMergeOperator {

  private final ApronDomain domain;

  @Option(
      secure = true,
      name = "type",
      toUppercase = true,
      values = {"SEP", "JOIN", "WIDENING"},
      description = "of which type should the merge be?")
  private String type = "SEP";

  @Option(
      secure = true,
      name = "onlyMergeAtLoopHeads",
      description = "with this option enabled" + " the states are only merged at loop heads")
  private boolean onlyMergeAtLoopHeads = false;

  public static MergeOperator getInstance(ApronDomain domain, Configuration config)
      throws InvalidConfigurationException {
    ApronMergeOperator mergeOp = new ApronMergeOperator(domain, config);

    switch (mergeOp.type) {
      case "SEP":
        return MergeSepOperator.getInstance();
      case "JOIN":
        return mergeOp.new ApronMergeJoinOperator(domain, config);
      case "WIDENING":
        return mergeOp.new ApronMergeWideningOperator(domain, config);
      default:
        throw new InvalidConfigurationException("Unknown type for merge operator");
    }
  }

  private ApronMergeOperator(ApronDomain domain, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);
    this.domain = domain;
  }

  @Options(prefix = "cpa.apron.mergeop")
  class ApronMergeJoinOperator extends ApronMergeOperator implements MergeOperator {

    private ApronMergeJoinOperator(ApronDomain domain, Configuration config)
        throws InvalidConfigurationException {
      super(domain, config);
    }

    @Override
    public AbstractState merge(AbstractState el1, AbstractState el2, Precision p)
        throws CPAException {
      if (onlyMergeAtLoopHeads) {
        if (!(((ApronState) el1).isLoopHead() && ((ApronState) el2).isLoopHead())) {
          return el2;
        }
      }
      return domain.join(el1, el2);
    }
  }

  @Options(prefix = "cpa.apron.mergeop")
  class ApronMergeWideningOperator extends ApronMergeOperator implements MergeOperator {

    private ApronMergeWideningOperator(ApronDomain domain, Configuration config)
        throws InvalidConfigurationException {
      super(domain, config);
    }

    @Override
    public AbstractState merge(AbstractState el1, AbstractState el2, Precision p)
        throws CPAException {
      if (onlyMergeAtLoopHeads) {
        if (!(((ApronState) el1).isLoopHead() && ((ApronState) el2).isLoopHead())) {
          return el2;
        }
      }
      return domain.widening((ApronState) el1, (ApronState) el2);
    }
  }
}
