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
package org.sosy_lab.cpachecker.util.invariants.redlog;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.sosy_lab.cpachecker.util.invariants.Rational;

public class EliminationHandler {

  private EliminationAnswer EA;

  public EliminationHandler(EliminationAnswer EA) {
    this.EA = EA;
  }

  public HashMap<String, Rational> getParameterValues(Collection<String> params) {
    // Pass the list of parameters for which you want values.  We
    // look for values in the EliminationAnswer that was passed to
    // the constructor.  If we find an EAPair in which every
    // parameter got a constant value, then we return a map from
    // parameter names to Rationals; else we return null.
    // In the future we will
    // implement a "Plan B" in case there are no such EAPairs (in
    // which we will try to deal with the "infinity" variables
    // that Redlog returns when it does not return constants), but
    // for now this has not been implemented.

    HashMap<String, Rational> map = null;

    // Get iterator over the EAPairs in EA.
    Iterator<EAPair> eapairIterator = EA.iterator();

    // variables for loop
    EAPair EAP;
    ParameterManager PM;

    boolean done = false;

    // For a first pass, we check whether any of the EAPairs
    // already sets every parameter equal to a constant. If so, we
    // take the first such one, and quit.
    while (!done && eapairIterator.hasNext()) {
      EAP = eapairIterator.next();
      PM = new ParameterManager(EAP, params);
      PM.makePAs();
      if (PM.allAreConstant()) {
        // In this case every parameter got a constant value.
        map = PM.getRationalValueMap();
        done = true;
      }
    }

    // Now we consider alternative actions, given that in none of
    // the EAPairs was every parameter set equal to a constant.
    // In other words, it is time for "Plan B," in which we try to
    // deal with the infinity variables that redlog must have
    // returned.
    if (!done) {
      // No Plan B yet!
    }

    return map;
  }

}
