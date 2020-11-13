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
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;

/** SMT heap representation with one huge byte array. */
class SMTHeapWithByteArray implements SMTHeap {

  private static final String SINGLE_BYTEARRAY_HEAP_NAME = "SINGLE_BYTEARRAY_HEAP_";
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

    if (pTargetType.isBitvectorType()) {

      BitvectorType targetType = (BitvectorType) formulaManager.getFormulaType(value);
      checkArgument(pTargetType.equals(targetType));

      FormulaType<I> addressType = formulaManager.getFormulaType(address);
      checkArgument(pointerType.equals(addressType));

      return handleBitVectorAssignment(
          targetType, addressType, oldIndex, newIndex, address, (BitvectorFormula) value);
    } else {
      throw new UnsupportedOperationException(
          "ByteArray Heap encoding does not support " + pTargetType.toString());
    }
  }

  @Override
  public <I extends Formula, E extends Formula> E makePointerDereference(
      String targetName, FormulaType<E> targetType, I address) {
    final FormulaType<I> addressType = formulaManager.getFormulaType(address);
    checkArgument(pointerType.equals(addressType));
    if (targetType.isBitvectorType()) {
      final FormulaType<I> bvAddressType = formulaManager.getFormulaType(address);
      checkArgument(pointerType.equals(addressType));
      BitvectorType bvTargetType = (BitvectorType) targetType;

      final ArrayFormula<I, BitvectorFormula> arrayFormula =
          afmgr.makeArray(SINGLE_BYTEARRAY_HEAP_NAME, bvAddressType, BYTE_TYPE);
      @SuppressWarnings("unchecked")
      E returnVal = (E) handleBitVectorDeref(arrayFormula, address, bvTargetType);
      return returnVal;
    } else {
      throw new UnsupportedOperationException(
          "ByteArray Heap encoding does not support " + targetType.toString());
    }
  }

  @Override
  public <I extends Formula, V extends Formula> V makePointerDereference(
      String targetName, FormulaType<V> targetType, int ssaIndex, I address) {
    if (targetType.isBitvectorType()) {
      final FormulaType<I> addressType = formulaManager.getFormulaType(address);
      checkArgument(pointerType.equals(addressType));
      BitvectorType bvTargetType = (BitvectorType) targetType;
      final ArrayFormula<I, BitvectorFormula> arrayFormula =
          afmgr.makeArray(SINGLE_BYTEARRAY_HEAP_NAME, ssaIndex, addressType, BYTE_TYPE);
      @SuppressWarnings("unchecked")
      V returnVal = (V) handleBitVectorDeref(arrayFormula, address, bvTargetType);
      return returnVal;
    } else {
      throw new UnsupportedOperationException(
          "ByteArray Heap encoding does not support " + targetType.toString());
    }
  }

  private <I extends Formula> BitvectorFormula handleBitVectorDeref(
      ArrayFormula<I, BitvectorFormula> arrayFormula, I address, BitvectorType targetType) {
    int offset = 0;
    int theN = targetType.getSize();
    BitvectorFormula result = afmgr.select(arrayFormula, address);

    while (offset < theN / 8 - 1) {
      I addressWithOffset =
          formulaManager.makePlus(
              address, formulaManager.makeNumber(formulaManager.getFormulaType(address), ++offset));
      BitvectorFormula nextBVPart = afmgr.select(arrayFormula, addressWithOffset);
      result =
          (endianness == ByteOrder.LITTLE_ENDIAN)
              ? bfmgr.concat(result, nextBVPart)
              : bfmgr.concat(nextBVPart, result);
    }
    return result;
  }

  private <I extends Formula> BooleanFormula handleBitVectorAssignment(
      BitvectorType targetType,
      FormulaType<I> addressType,
      int oldIndex,
      int newIndex,
      I address,
      BitvectorFormula value) {
    int offset = 0;
    int theN = targetType.getSize();

    FormulaType<BitvectorFormula> bv8TargetType = FormulaType.getBitvectorTypeWithSize(8);
    ArrayFormula<I, BitvectorFormula> oldFormula =
        afmgr.makeArray(SINGLE_BYTEARRAY_HEAP_NAME, oldIndex, addressType, bv8TargetType);

    ImmutableList<BitvectorFormula> splits = splitNBitVectorToByteVectors(value, theN);
    for (BitvectorFormula formula : splits) {
      I addressWithOffset =
          formulaManager.makePlus(address, formulaManager.makeNumber(addressType, offset++));
      oldFormula = afmgr.store(oldFormula, addressWithOffset, formula);
    }

    final ArrayFormula<I, BitvectorFormula> arrayFormula =
        afmgr.makeArray(SINGLE_BYTEARRAY_HEAP_NAME, newIndex, addressType, bv8TargetType);
    return formulaManager.makeEqual(arrayFormula, oldFormula);
  }

  private <I extends BitvectorFormula> ImmutableList<BitvectorFormula> splitNBitVectorToByteVectors(
      I oldVector, int n) {
    checkArgument(n % 8 == 0, "Bitvector size is not an multiplex of 8!");
    int length = bfmgr.getLength(oldVector);
    int offset = 0;
    ImmutableList.Builder<BitvectorFormula> builder = ImmutableList.builder();
    while (offset < length) {
      builder.add(bfmgr.extract(oldVector, offset + 7, offset, true));
      offset = offset + 8;
    }
    // TODO check whether this can also be used for words > 64 bit
    return (endianness == ByteOrder.LITTLE_ENDIAN) ? builder.build().reverse() : builder.build();
  }
}
