// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.octagon;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "cpa.octagon.mergeop")
public class OctagonMergeOperator {

  private final OctagonDomain domain;

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

  public static MergeOperator getInstance(OctagonDomain domain, Configuration config)
      throws InvalidConfigurationException {
    OctagonMergeOperator mergeOp = new OctagonMergeOperator(domain, config);

    switch (mergeOp.type) {
      case "SEP":
        return MergeSepOperator.getInstance();
      case "JOIN":
        return mergeOp.new OctagonMergeJoinOperator(domain, config);
      case "WIDENING":
        return mergeOp.new OctagonMergeWideningOperator(domain, config);
      default:
        throw new InvalidConfigurationException("Unknown type for merge operator");
    }
  }

  private OctagonMergeOperator(OctagonDomain domain, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this, OctagonMergeOperator.class);
    this.domain = domain;
  }

  class OctagonMergeJoinOperator extends OctagonMergeOperator implements MergeOperator {

    private OctagonMergeJoinOperator(OctagonDomain domain, Configuration config)
        throws InvalidConfigurationException {
      super(domain, config);
    }

    @Override
    public AbstractState merge(AbstractState el1, AbstractState el2, Precision p)
        throws CPAException {
      if (onlyMergeAtLoopHeads) {
        if (!(((OctagonState) el1).isLoopHead() && ((OctagonState) el2).isLoopHead())) {
          return el2;
        }
      }
      return domain.join(el1, el2);
    }
  }

  class OctagonMergeWideningOperator extends OctagonMergeOperator implements MergeOperator {

    private OctagonMergeWideningOperator(OctagonDomain domain, Configuration config)
        throws InvalidConfigurationException {
      super(domain, config);
    }

    @Override
    public AbstractState merge(AbstractState el1, AbstractState el2, Precision p)
        throws CPAException {
      if (onlyMergeAtLoopHeads) {
        if (!(((OctagonState) el1).isLoopHead() && ((OctagonState) el2).isLoopHead())) {
          return el2;
        }
      }
      return domain.widening((OctagonState) el1, (OctagonState) el2);
    }
  }
}
