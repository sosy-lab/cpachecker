// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.nio.ByteOrder;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;

/** SMT heap representation with one huge byte array. */
class SMTHeapWithByteArray implements SMTHeap {

  private static final BitvectorType BYTE_TYPE = FormulaType.getBitvectorTypeWithSize(8);

  private final ArrayFormulaManagerView afmgr;
  private final FormulaManagerView formulaManager;
  private final BitvectorFormulaManager bfmgr;
  private final FormulaType<?> pointerType;
  private final ByteOrder endianness;

  SMTHeapWithByteArray(
      FormulaManagerView pFormulaManager, FormulaType<?> pPointerType, MachineModel pModel) {
    formulaManager = pFormulaManager;
    afmgr = formulaManager.getArrayFormulaManager();
    pointerType = pPointerType;
    endianness = pModel.getEndianness();
    bfmgr = formulaManager.getBitvectorFormulaManager();
  }

  @Override
  public <I extends Formula, E extends Formula> BooleanFormula makePointerAssignment(
      String targetName,
      FormulaType<?> pTargetType,
      int oldIndex,
      int newIndex,
      I address,
      E value) {
    if (pTargetType.isFloatingPointType()) {
      FloatingPointType floatTargetType = (FloatingPointType) pTargetType;
      BitvectorType bvType = FormulaType.getBitvectorTypeWithSize(floatTargetType.getTotalSize());
      FloatingPointFormulaManagerView floatMgr = formulaManager.getFloatingPointFormulaManager();
      BitvectorFormula bvValue = floatMgr.toIeeeBitvector((FloatingPointFormula) value);
      return makePointerAssignment(targetName, bvType, oldIndex, newIndex, address, bvValue);

    } else if (pTargetType.isBitvectorType()) {

      BitvectorType targetType = (BitvectorType) formulaManager.getFormulaType(value);
      checkArgument(pTargetType.equals(targetType));

      FormulaType<I> addressType = formulaManager.getFormulaType(address);
      checkArgument(pointerType.equals(addressType));

      return handleBitvectorAssignment(
          targetName, oldIndex, newIndex, address, addressType, (BitvectorFormula) value);
    } else {
      throw new UnsupportedOperationException(
          "ByteArray Heap encoding does not support " + pTargetType);
    }
  }

  @Override
  public <I extends Formula, E extends Formula> E makePointerDereference(
      String targetName, FormulaType<E> targetType, I address) {
    if (targetType.isFloatingPointType()) {
      FloatingPointType floatType = (FloatingPointType) targetType;
      BitvectorType bvType = FormulaType.getBitvectorTypeWithSize(floatType.getTotalSize());
      BitvectorFormula bvFormula = makePointerDereference(targetName, bvType, address);
      FloatingPointFormulaManagerView floatMgr = formulaManager.getFloatingPointFormulaManager();
      @SuppressWarnings("unchecked")
      E floatFormula = (E) floatMgr.fromIeeeBitvector(bvFormula, floatType);
      return floatFormula;

    } else if (targetType.isBitvectorType()) {
      final FormulaType<I> addressType = formulaManager.getFormulaType(address);
      checkArgument(pointerType.equals(addressType));
      BitvectorType bvTargetType = (BitvectorType) targetType;

      final ArrayFormula<I, BitvectorFormula> arrayFormula =
          afmgr.makeArray(targetName, addressType, BYTE_TYPE);
      @SuppressWarnings("unchecked")
      E returnVal = (E) handleBitvectorDeref(arrayFormula, address, addressType, bvTargetType);
      return returnVal;
    } else {
      throw new UnsupportedOperationException(
          "ByteArray Heap encoding does not support " + targetType);
    }
  }

  @Override
  public <I extends Formula, V extends Formula> V makePointerDereference(
      String targetName, FormulaType<V> targetType, int ssaIndex, I address) {
    if (targetType.isFloatingPointType()) {
      FloatingPointType floatType = (FloatingPointType) targetType;
      BitvectorType bvType = FormulaType.getBitvectorTypeWithSize(floatType.getTotalSize());
      BitvectorFormula bvFormula = makePointerDereference(targetName, bvType, ssaIndex, address);
      FloatingPointFormulaManagerView floatMgr = formulaManager.getFloatingPointFormulaManager();
      @SuppressWarnings("unchecked")
      V floatFormula = (V) floatMgr.fromIeeeBitvector(bvFormula, floatType);
      return floatFormula;

    } else if (targetType.isBitvectorType()) {
      final FormulaType<I> addressType = formulaManager.getFormulaType(address);
      checkArgument(pointerType.equals(addressType));
      BitvectorType bvTargetType = (BitvectorType) targetType;
      final ArrayFormula<I, BitvectorFormula> arrayFormula =
          afmgr.makeArray(targetName, ssaIndex, addressType, BYTE_TYPE);
      @SuppressWarnings("unchecked")
      V returnVal = (V) handleBitvectorDeref(arrayFormula, address, addressType, bvTargetType);
      return returnVal;
    } else {
      throw new UnsupportedOperationException(
          "ByteArray Heap encoding does not support " + targetType);
    }
  }

  private <I extends Formula> BitvectorFormula handleBitvectorDeref(
      ArrayFormula<I, BitvectorFormula> arrayFormula,
      I address,
      FormulaType<I> addressType,
      BitvectorType targetType) {
    final int bitLength = targetType.getSize();
    BitvectorFormula result = afmgr.select(arrayFormula, address);

    if (bitLength < 8) {
      return bfmgr.extract(result, bitLength - 1, 0, false);
    } else if (bitLength == 8) {
      return result;
    } else {
      checkArgument(bitLength % 8 == 0, "Bitvector size %s is not a multiple of 8!", bitLength);

      // result starts with first byte, loop appends the other bytes
      for (int byteOffset = 1; byteOffset < bitLength / 8; byteOffset++) {
        I addressWithOffset =
            formulaManager.makePlus(address, formulaManager.makeNumber(addressType, byteOffset));
        BitvectorFormula nextBVPart = afmgr.select(arrayFormula, addressWithOffset);
        result =
            (endianness == ByteOrder.BIG_ENDIAN)
                ? bfmgr.concat(result, nextBVPart)
                : bfmgr.concat(nextBVPart, result);
      }
      return result;
    }
  }

  private <I extends Formula> BooleanFormula handleBitvectorAssignment(
      String targetName,
      int oldIndex,
      int newIndex,
      I address,
      FormulaType<I> addressType,
      BitvectorFormula value) {
    ArrayFormula<I, BitvectorFormula> oldFormula =
        afmgr.makeArray(targetName, oldIndex, addressType, BYTE_TYPE);
    final int bitLength = bfmgr.getLength(value);

    ImmutableList<BitvectorFormula> bytes;
    if (bitLength < 8) {
      BitvectorFormula remainingBits =
          bfmgr.extract(afmgr.select(oldFormula, address), 7, bitLength, false);
      bytes = ImmutableList.of(bfmgr.concat(remainingBits, value));
    } else if (bitLength == 8) {
      bytes = ImmutableList.of(value);
    } else {
      bytes = splitBitvectorToBytes(value);
    }
    int byteOffset = 0;
    for (BitvectorFormula formula : bytes) {
      I addressWithOffset =
          formulaManager.makePlus(address, formulaManager.makeNumber(addressType, byteOffset++));
      oldFormula = afmgr.store(oldFormula, addressWithOffset, formula);
    }

    final ArrayFormula<I, BitvectorFormula> arrayFormula =
        afmgr.makeArray(targetName, newIndex, addressType, BYTE_TYPE);
    return formulaManager.makeEqual(arrayFormula, oldFormula);
  }

  private <I extends BitvectorFormula> ImmutableList<BitvectorFormula> splitBitvectorToBytes(
      I bitvector) {
    final int bitLength = bfmgr.getLength(bitvector);
    checkArgument(bitLength % 8 == 0, "Bitvector size %s is not a multiple of 8!", bitLength);
    ImmutableList.Builder<BitvectorFormula> builder = ImmutableList.builder();
    for (int bitOffset = 0; bitOffset < bitLength; bitOffset += 8) {
      builder.add(bfmgr.extract(bitvector, bitOffset + 7, bitOffset, true));
    }
    return (endianness == ByteOrder.BIG_ENDIAN) ? builder.build().reverse() : builder.build();
  }
}
