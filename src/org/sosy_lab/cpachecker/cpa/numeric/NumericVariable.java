// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric;

import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.numericdomains.Manager;

/**
 * Defines a {@link org.sosy_lab.numericdomains.environment.Variable} which is mapped to a {@link
 * MemoryLocation}.
 */
public class NumericVariable extends org.sosy_lab.numericdomains.environment.Variable {
  private static boolean hasPrintedFloatWarning = false;

  private final MemoryLocation location;

  private final CSimpleType type;

  private NumericVariable(
      String qualifiedVariableName, MemoryLocation pLocation, CSimpleType pType) {
    super(qualifiedVariableName);
    location = pLocation;
    type = pType;
  }

  /**
   * Creates a new {@link org.sosy_lab.numericdomains.environment.Variable} and maps it to a {@link
   * MemoryLocation}.
   *
   * <p>The variable is only created, if the variable is tracked by the precision.
   *
   * @param pDeclaration declaration of the variable
   * @param cfaNode node after the current edge
   * @param pPrecision precision for which the variable is created
   * @return the created variable or {@link Optional#empty()} if the variable is not tracked by the
   *     precision.
   */
  public static Optional<NumericVariable> valueOf(
      CSimpleDeclaration pDeclaration,
      CFANode cfaNode,
      VariableTrackingPrecision pPrecision,
      Manager pManager,
      LogManager pLogManager) {
    String qualifiedVariableName = pDeclaration.getQualifiedName();
    MemoryLocation memoryLocation = MemoryLocation.valueOf(qualifiedVariableName);

    if (pPrecision.isTracking(memoryLocation, pDeclaration.getType(), cfaNode)) {
      if (pDeclaration.getType() instanceof CSimpleType) {
        CSimpleType simpleType = (CSimpleType) pDeclaration.getType();
        if (!hasPrintedFloatWarning && simpleType.getType().isFloatingPointType()) {
          checkHandlesFloat(pManager, pLogManager);
        }
        return Optional.of(new NumericVariable(qualifiedVariableName, memoryLocation, simpleType));
      }
    }
    return Optional.empty();
  }

  private static void checkHandlesFloat(Manager pManager, LogManager pLogManager) {
    if (pManager instanceof org.sosy_lab.numericdomains.elina.PolyhedraManager) {
      pLogManager.log(
          Level.SEVERE,
          "Elina Polyhedra Manager",
          pManager.getDomainLibrary(),
          pManager.getDomainVersion(),
          "does not support floating point variables, do not trust the result.");
    }
    hasPrintedFloatWarning = true;
  }

  /** Returns the {@link MemoryLocation} of this Variable. */
  public MemoryLocation getLocation() {
    return location;
  }

  /** Returns the type of the variable. */
  public CSimpleType getSimpleType() {
    return type;
  }
}
