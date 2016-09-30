/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.io.PrintStream;

/**
 * An interface for managing memory regions
 */
public interface MemoryRegionManager {
  /**
   * Returns the name of uninterpreted function
   * @param pRegion - a memory region
   * @return the uninterpreted function name
   */
  public String getPointerAccessName(final MemoryRegion pRegion);
  /**
   * Creates a memory region for a given type
   * @param pType - type used to create a region
   * @return New region for the given type
   */
  public MemoryRegion makeMemoryRegion(CType pType);
  /**
   * Creates a region for the field access
   * @param pFieldOwnerType - the type of the field owner structure
   * @param pExpressionType - type of the field
   * @param pFieldName - name of the field
   * @return New memory region for the given field
   */
  public MemoryRegion makeMemoryRegion(CType pFieldOwnerType, CType pExpressionType, String pFieldName);
  /**
   * Adds target to statistics.
   * For calculating how many targets were used to construct formulas.
   * @param pEdge - edge for which the formula is constructed
   * @param pUfName - name of the region (uninterpreted function)
   * @param pTarget - pointer target belonging to the region
   */
  public void addTargetToStats(CFAEdge pEdge, String pUfName, PointerTarget pTarget);
  /**
   * Prints statistics to the specified output
   * @param out - output stream where statistics is printed
   */
  public void printStatistics(PrintStream out);
}
