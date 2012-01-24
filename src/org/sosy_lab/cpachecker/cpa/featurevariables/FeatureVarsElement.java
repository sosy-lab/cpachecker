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
package org.sosy_lab.cpachecker.cpa.featurevariables;

import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableElement;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

public class FeatureVarsElement implements AbstractQueryableElement {

  private final Region currentState;
  private final NamedRegionManager manager;

  public FeatureVarsElement(Region currentState, NamedRegionManager manager) {
    this.currentState = currentState;
    this.manager = manager;
  }

  public Region getRegion() {
    return currentState;
  }

  @Override
  public String toString() {
    return manager.dumpRegion(currentState);
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    throw new InvalidQueryException("Feature Vars Element cannot check anything");
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.equals("VALUES")) {
      return manager.dumpRegion(this.currentState);
    } else {
      throw new InvalidQueryException("Feature Vars Element can only return the current values (\"VALUES\")");
    }
  }

  @Override
  public String getCPAName() {
    return "FeatureVars";
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {
    throw new InvalidQueryException("Feature Vars Element cannot be modified");
  }

}
