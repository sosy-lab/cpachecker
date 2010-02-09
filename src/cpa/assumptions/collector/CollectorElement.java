/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.assumptions.collector;

import assumptions.AssumptionWithLocation;
import cpa.common.interfaces.AbstractElement;

/**
 * Abstract element for the Collector CPA. Encapsulate a
 * symbolic formula
 * 
 * @author g.theoduloz
 */
public class CollectorElement implements AbstractElement {

  private final AssumptionWithLocation assumption;
 
  public CollectorElement(AssumptionWithLocation f)
  {
    assumption = f;
  }
  
  /**
   * Return the invariant in this state. May return
   * a null value in case no invariant is stored.
   */
  public AssumptionWithLocation getCollectedAssumptions()
  {
    return assumption;
  }
  
  @Override
  public boolean isError() {
    return false;
  }
  
  @Override
  public String toString() {
    if (assumption == null)
      if (this == BOTTOM) return "BOTTOM";
      else if (this == TOP) return "TOP";
      else return "(null)";
    else
      return assumption.toString();
  }
  
  /** Bottom */
  public static final CollectorElement BOTTOM = new CollectorElement(null);
  
  /** Top */
  public static final CollectorElement TOP = new CollectorElement(null);

}
