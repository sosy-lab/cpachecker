/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class ExplictPathChecker {

  /**
   * a set of variables not the be tracked
   */
  private Set<String> irrelevantVariables;

  public ExplictPathChecker(Set<String> irrelevantVariables) {
    this.irrelevantVariables = irrelevantVariables;
  }

  public boolean checkPath(ARTElement pRootElement, List<CFAEdge> cfaTrace)
      throws CPAException, InterruptedException {

    try {
      Configuration lConfig = Configuration.builder()
              .setOption("cpa.explicit.precision.ignore.asString", Joiner.on(",").join(irrelevantVariables))
              .build();

      TransferRelation transfer   = new ExplicitTransferRelation(lConfig);
      AbstractElement next        = new ExplicitElement();
      ExplicitPrecision precision = new ExplicitPrecision("", lConfig);

      for(CFAEdge cfaEdge : cfaTrace) {
        Collection<? extends AbstractElement> successors = transfer.getAbstractSuccessors(next, precision, cfaEdge);

        next = determineNextElement(successors);

        // path is not feasible
        if(next == null && cfaEdge != cfaTrace.get(cfaTrace.size() - 1)) {
          return false;
        }
      }

      // path is feasible
      return true;
    } catch (InvalidConfigurationException e) {
      throw new CounterexampleAnalysisFailed("Invalid configuration for checking path: " + e.getMessage(), e);
    }
  }

  private AbstractElement determineNextElement(Collection<? extends AbstractElement> successors) {
    if(successors.isEmpty()) {
      return null;
    }
    else {
      assert(successors.size() == 1);
      return Lists.newArrayList(successors).get(0);
    }
  }
}