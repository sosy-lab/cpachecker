// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import java.io.PrintStream;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/** An interface for managing memory regions */
public interface MemoryRegionManager {
  /**
   * Returns the name of uninterpreted function
   *
   * @param pRegion - a memory region
   * @return the uninterpreted function name
   */
  String getPointerAccessName(final MemoryRegion pRegion);
  /**
   * Creates a memory region for a given type
   *
   * @param pType - type used to create a region
   * @return New region for the given type
   */
  MemoryRegion makeMemoryRegion(CType pType);
  /**
   * Creates a region for the field access
   *
   * @param pFieldOwnerType - the type of the field owner structure
   * @param pExpressionType - type of the field
   * @param pFieldName - name of the field
   * @return New memory region for the given field
   */
  MemoryRegion makeMemoryRegion(CType pFieldOwnerType, CType pExpressionType, String pFieldName);

  /**
   * Creates a region for accessing a given field
   *
   * @param pFieldOwnerType the type of the field owner structure
   * @param pField the field to access
   * @return New memory region for the given field
   */
  MemoryRegion makeMemoryRegion(CType pFieldOwnerType, CCompositeTypeMemberDeclaration pField);

  /**
   * Adds target to statistics. For calculating how many targets were used to construct formulas.
   *
   * @param pEdge - edge for which the formula is constructed
   * @param pUfName - name of the region (uninterpreted function)
   * @param pTarget - pointer target belonging to the region
   */
  void addTargetToStats(CFAEdge pEdge, String pUfName, PointerTarget pTarget);
  /**
   * Prints statistics to the specified output
   *
   * @param out - output stream where statistics is printed
   */
  void printStatistics(PrintStream out);
}
