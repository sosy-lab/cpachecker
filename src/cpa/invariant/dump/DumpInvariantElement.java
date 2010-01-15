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
package cpa.invariant.dump;

import cpa.common.interfaces.AbstractElement;
import cpa.invariant.common.Invariant;

/**
 * Abstract element for the DumpInvariant CPA. Encapsulate a
 * symbolic formula
 * 
 * @author g.theoduloz
 */
public class DumpInvariantElement implements AbstractElement {

  private final Invariant invariant;
 
  public DumpInvariantElement(Invariant f)
  {
    invariant = f;
  }
  
  /**
   * Return the invariant in this state. May return
   * a null value in case no invariant is stored.
   */
  public Invariant getInvariant()
  {
    return invariant;
  }
  
  @Override
  public boolean isError() {
    return false;
  }
  
  @Override
  public String toString() {
    if (invariant == null)
      if (this == BOTTOM) return "BOTTOM";
      else if (this == TOP) return "TOP";
      else return "(null)";
    else
      return invariant.toString();
  }
  
  /** Bottom */
  public static final DumpInvariantElement BOTTOM = new DumpInvariantElement(null);
  
  /** Top */
  public static final DumpInvariantElement TOP = new DumpInvariantElement(null);

}
