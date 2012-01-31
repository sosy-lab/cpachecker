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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.util.invariants.balancer.Assumption.AssumptionType;


public class UsableColumn {

  private LogManager logger;
  private int colNum;
  private List<RationalFunction> entries;
  private Set<Integer> requests;

  public UsableColumn() {}

  public UsableColumn(Matrix mat, Integer j, LogManager lm) {
    logger = lm;
    // We construct this to be the column j in the matrix mat.
    int m = mat.getRowNum();
    entries = new Vector<RationalFunction>(m);
    for (int i = 0; i < m; i++) {
      entries.add( mat.getEntry(i, j) );
    }
    colNum = j;
    requests = new HashSet<Integer>();
    logger.log(Level.ALL, "Constructed UsableColumn with entries:\n",entries);
  }

  public int getColNum() {
    return colNum;
  }

  public void clearRequests() {
    requests = new HashSet<Integer>();
  }

  public void makeRequest(Integer r) {
    requests.add(r);
  }

  public AssumptionSet getRequestedAssumptions() {
    AssumptionSet aset = new AssumptionSet();
    // If no requests were made, then return the empty set.
    if (requests.size() == 0) {
      return aset;
    }
    // Else at least one request was made.
    // This means we need every variable quantity in this column to be nonpositive.
    logger.log(Level.ALL, "Column",colNum,"forming assumptions as requested by rows:",requests,".");
    for (int i = 0; i < entries.size(); i++) {
      RationalFunction f = entries.get(i);
      if (!f.isConstant()) {
        // We only deal with variable f.
        Assumption a;
        if (requests.contains(i)) {
          // If i is the number of one of the rows that requested this column, then
          // we actually need f to be negative.
          a = new Assumption(f, AssumptionType.NEGATIVE);
        } else {
          // Otherwise, we only need f to be nonpositive.
          a = new Assumption(f, AssumptionType.NONPOSITIVE);
        }
        logger.log(Level.ALL, "For variable entry",f,"added assumption",a,".");
        aset.add(a);
      }
    }
    return aset;
  }


}
