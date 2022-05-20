// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.java_smt.api.FormulaType;

public class CtoFormulaTypeHandler {

  protected final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;

  private final FormulaType<?> pointerType;

  public CtoFormulaTypeHandler(LogManager pLogger, MachineModel pMachineModel) {
    logger = new LogManagerWithoutDuplicates(pLogger);
    machineModel = pMachineModel;
    pointerType = FormulaType.getBitvectorTypeWithSize(machineModel.getSizeofPtrInBits());
  }

  /**
   * Returns the size in bytes of the given type. Always use this method instead of
   * machineModel.getSizeOf, because this method can handle dereference-types.
   *
   * @param pType the type to calculate the size of.
   * @return the size in bytes of the given type.
   */
  public int getSizeof(CType pType) {
    int size = machineModel.getSizeof(pType).intValueExact();
    if (size == 0) {
      CType type = pType.getCanonicalType();
      if (type instanceof CArrayType) {
        // C11 §6.7.6.2 (1)
        logger.logOnce(Level.WARNING, "Type", pType, "is a zero-length array, this is undefined.");
      } else if (type instanceof CCompositeType) {
        // UNDEFINED:
        // http://stackoverflow.com/questions/1626446/what-is-the-size-of-an-empty-struct-in-c
        logger.logOnce(Level.WARNING, "Type", pType, "has no fields, this is undefined.");
      } else {
        logger.logOnce(Level.WARNING, "Type", pType, "has size 0 bytes.");
      }
    }
    return size;
  }

  /**
   * Returns the size in bits of the given type. Always use this method instead of
   * machineModel.getSizeOf, because this method can handle dereference-types.
   *
   * @param pType the type to calculate the size of.
   * @return the size in bits of the given type.
   */
  public int getBitSizeof(CType pType) {
    if (pType instanceof CBitFieldType) {
      return ((CBitFieldType) pType).getBitFieldSize();
    }
    return getSizeof(pType) * machineModel.getSizeofCharInBits();
  }

  public FormulaType<?> getPointerType() {
    return pointerType;
  }
}
