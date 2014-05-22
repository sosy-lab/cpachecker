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
package org.sosy_lab.cpachecker.cpa.apron;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix="cpa.octagon.mergeop")
public class ApronMergeJoinOperator implements MergeOperator {

  private final ApronDomain domain;

  @Option(name="type", toUppercase=true, values={"NORMAL", "WIDENING"},
      description="of which type should the merge be? normal, for usual join, widening for"
                + " a widening instead of a join")
  private String joinType = "NORMAL";

  @Option(name="onlyJoinEdgesInSameBlock", description="with this option enabled"
      + "mergeJoin is only used on edges within the same block, i.e. each iteration"
      + "of a loop is a different block, thus the precision of the analysis increases")
  private boolean onlyJoinEdgesInSameBlock = false;

  public ApronMergeJoinOperator(ApronDomain domain, Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    this.domain = domain;
  }

  @Override
  public AbstractState merge(AbstractState el1, AbstractState el2, Precision p) throws CPAException {
    if (onlyJoinEdgesInSameBlock) {
      if (!((ApronState)el1).areInSameBlock((ApronState)el2)) {
        return el2;
      }
    }
    if (joinType.equals("NORMAL")) {
      return domain.join(el1, el2);
    } else if (joinType.equals("WIDENING")) {
        return domain.joinWidening((ApronState)el1, (ApronState)el2);
    } else {
      throw new CPAException("Invalid join type in Octagon merge join operator");
    }
  }
}
