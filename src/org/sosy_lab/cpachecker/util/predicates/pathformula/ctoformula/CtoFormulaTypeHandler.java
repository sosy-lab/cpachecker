// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.java_smt.api.FormulaType;

public class CtoFormulaTypeHandler {

  protected final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;

  private final FormulaType<?> pointerType;

  private final Map<CCompositeType, ImmutableMap<String, Long>> offsets = new HashMap<>();

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
  public long getSizeof(CType pType) {
    long size = machineModel.getSizeof(pType).longValueExact();
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
  public long getBitSizeof(CType pType) {
    if (pType instanceof CBitFieldType) {
      return ((CBitFieldType) pType).getBitFieldSize();
    }
    return getSizeof(pType) * machineModel.getSizeofCharInBits();
  }

  /**
   * Get the offset of a field, if it is byte-aligned. Offsets of bit fields that do not start at a
   * byte boundary are returned as <code>OptionaLong.empty()</code>.
   */
  public OptionalLong getOffset(CCompositeType compositeType, final String memberName) {
    final long bitOffset = getBitOffset(compositeType, memberName);
    if (bitOffset % machineModel.getSizeofCharInBits() == 0) {
      return OptionalLong.of(bitOffset / machineModel.getSizeofCharInBits());
    } else {
      return OptionalLong.empty();
    }
  }

  /**
   * @see #getOffset(CCompositeType, String)
   */
  public OptionalLong getOffset(
      CCompositeType compositeType, final CCompositeTypeMemberDeclaration member) {
    return getOffset(compositeType, member.getName());
  }

  /**
   * @see #getBitOffset(CCompositeType, String)
   */
  public long getBitOffset(
      CCompositeType compositeType, final CCompositeTypeMemberDeclaration member) {
    return getBitOffset(compositeType, member.getName());
  }

  /**
   * The method is used to speed up member offset computation for declared composite types.
   *
   * @param compositeType The composite type.
   * @param memberName The name of the member of the composite type.
   * @return The offset of the member in the composite type in bits.
   */
  long getBitOffset(CCompositeType compositeType, final String memberName) {
    assert compositeType.getKind() != ComplexTypeKind.ENUM
        : "Enums are not composite: " + compositeType;
    ImmutableMap<String, Long> multiset = offsets.get(compositeType);
    if (multiset == null) {
      Map<CCompositeTypeMemberDeclaration, BigInteger> calculatedOffsets =
          machineModel.getAllFieldOffsetsInBits(compositeType);
      ImmutableMap.Builder<String, Long> memberOffsets =
          ImmutableMap.builderWithExpectedSize(calculatedOffsets.size());
      calculatedOffsets.forEach(
          (key, value) -> memberOffsets.put(key.getName(), value.longValueExact()));
      multiset = memberOffsets.buildOrThrow();
      offsets.put(compositeType, multiset);
    }
    return multiset.get(memberName);
  }

  public FormulaType<?> getPointerType() {
    return pointerType;
  }
}
