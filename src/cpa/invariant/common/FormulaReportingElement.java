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
package cpa.invariant.common;

import symbpredabstraction.interfaces.SymbolicFormula;

/**
 * Interface to implement in order for an object (typically abstract element)
 * to be able to be over-approximated by a symbolic formula representing
 * the abstract element.
 * 
 * @author g.theoduloz
 */
public interface FormulaReportingElement {
  
  /**
   * Returns a symbolic formula over-approximating the element
   */
  public SymbolicFormula getFormulaApproximation();
  
}
