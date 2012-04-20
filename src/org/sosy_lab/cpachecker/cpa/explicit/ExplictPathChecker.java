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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class ExplictPathChecker {

  /**
   * This method acts as the constructor of the class.
   */
  public ExplictPathChecker() {}

  /**
   * This method checks if the given path is feasible, when not tracking the given set of variables.
   *
   * @param path the path to check
   * @param variablesToBeIgnored the variables to ignore; may be empty for a full-precision check
   * @return true, if the path is feasible, else false
   * @throws CPAException
   * @throws InterruptedException
   */
  public boolean checkPath(List<CFAEdge> path, Multimap<CFANode, String> variablesToBeIgnored)
      throws CPAException, InterruptedException {
    try {
      Configuration config = Configuration.builder().build();

      TransferRelation transfer   = new ExplicitTransferRelation(config);
      AbstractElement next        = new ExplicitElement();
      ExplicitPrecision precision = new ExplicitPrecision("", config);

      precision.getIgnore().setMapping(variablesToBeIgnored);

      for(CFAEdge edge : path) {
        Collection<? extends AbstractElement> successors = transfer.getAbstractSuccessors(next, precision, edge);

        next = extractNextElement(successors);

        // path is not feasible
        if(next == null && edge != path.get(path.size() - 1)) {
          return false;
        }
      }

      // path is feasible
      return true;
    } catch (InvalidConfigurationException e) {
      throw new CounterexampleAnalysisFailed("Invalid configuration for checking path: " + e.getMessage(), e);
    }
  }

  /**
   * This method extracts the single successor out of the (hopefully singleton) successor collection.
   *
   * @param successors the collection of successors
   * @return the successor, or null if none exists
   */
  private AbstractElement extractNextElement(Collection<? extends AbstractElement> successors) {
    if(successors.isEmpty()) {
      return null;
    }
    else {
      assert(successors.size() == 1);
      return Lists.newArrayList(successors).get(0);
    }
  }
}