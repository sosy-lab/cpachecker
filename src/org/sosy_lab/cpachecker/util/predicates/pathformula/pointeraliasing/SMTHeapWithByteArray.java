/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkArgument;

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

/**
 * SMT heap representation with one huge byte array.
 */
public class SMTHeapWithByteArray implements SMTHeap {

  private final ArrayFormulaManagerView afmgr;

  private final FormulaManagerView formulaManager;
  private final TypeHandlerWithPointerAliasing typeHandler;
  private final MachineModel model;

  private static final String SINGLE_BYTEARRAY_HEAP_NAME = "SINGLE_BYTEARRAY_HEAP_";

  public SMTHeapWithByteArray(
      FormulaManagerView pFormulaManager,
      TypeHandlerWithPointerAliasing pTypeHandle,
      MachineModel pModel) {
    formulaManager = pFormulaManager;
    afmgr = formulaManager.getArrayFormulaManager();
    typeHandler = pTypeHandle;
    model = pModel;
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

      FormulaType<BitvectorFormula> targetType =
          formulaManager.getFormulaType((BitvectorFormula) value);
      checkArgument(pTargetType.equals(targetType));
      FormulaType<BitvectorFormula> addressType =
          formulaManager.getFormulaType((BitvectorFormula) address);
      checkArgument(typeHandler.getPointerType().equals(addressType));

      return handleBitVectorAssignment(
          targetType,
          addressType,
          oldIndex,
          newIndex,
          (BitvectorFormula) address,
          (BitvectorFormula) value);
    } else {
      throw new UnsupportedOperationException(
          "ByteArray Heap encoding does not support " + pTargetType.toString());
    }
  }

  private BooleanFormula handleBitVectorAssignment(
      FormulaType<BitvectorFormula> targetType,
      FormulaType<BitvectorFormula> addressType,
      int oldIndex,
      int newIndex,
      BitvectorFormula address,
      BitvectorFormula value) {

    BitvectorFormulaManager bfmgr = formulaManager.getBitvectorFormulaManager();
    int offset = 0;
    int theN = ((FormulaType.BitvectorType) targetType).getSize();

    // TODO find better way to create FormulaType<Bitvector8> instance
    FormulaType<BitvectorFormula> bv8TargetType =
        formulaManager.getFormulaType(bfmgr.makeBitvector(8, 0));
    ArrayFormula<BitvectorFormula, BitvectorFormula> oldFormula =
        afmgr.makeArray(SINGLE_BYTEARRAY_HEAP_NAME, oldIndex, addressType, bv8TargetType);

    BitvectorFormula[] splits = splitNBitVectorToByteVectors(value, theN);
    for (BitvectorFormula formula : splits) {
      // is this a valid solution? Let JavaSMT Handle address here ... But how?
      BitvectorFormula addressWithOffset =
          bfmgr.add(address, bfmgr.makeBitvector(bfmgr.getLength(address), offset++));
      oldFormula = afmgr.store(oldFormula, addressWithOffset, formula);
    }

    final ArrayFormula<BitvectorFormula, BitvectorFormula> arrayFormula =
        afmgr.makeArray(SINGLE_BYTEARRAY_HEAP_NAME, newIndex, addressType, bv8TargetType);
    return formulaManager.makeEqual(arrayFormula, oldFormula);
  }

  private <I extends BitvectorFormula> BitvectorFormula[] splitNBitVectorToByteVectors(
      I oldVector, int n) {
    checkArgument(n % 8 == 0, "Bitvector size is not an multiplex of 8!");

    BitvectorFormulaManager bfmgr = formulaManager.getBitvectorFormulaManager();
    int length = bfmgr.getLength(oldVector);
    int offset = 0;
    BitvectorFormula[] byteArray = new BitvectorFormula[length / 8];
    while (offset < length) {
      int index =
          model.getEndianness() == ByteOrder.LITTLE_ENDIAN
              ? byteArray.length - 1 - offset / 8
              : offset / 8;
      byteArray[index] = bfmgr.extract(oldVector, offset + 7, offset, true);
      offset = offset + 8;
    }
    return byteArray;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <I extends Formula, E extends Formula> E makePointerDereference(
      String targetName, FormulaType<E> targetType, I address) {
    final FormulaType<I> addressType = formulaManager.getFormulaType(address);
    checkArgument(typeHandler.getPointerType().equals(addressType));
    if (targetType.isBitvectorType()) {
      BitvectorFormula bvAddress = (BitvectorFormula) address;
      // TODO refactor ugly casts
      final FormulaType<BitvectorFormula> bvAddressType = formulaManager.getFormulaType(bvAddress);
      checkArgument(typeHandler.getPointerType().equals(addressType));
      FormulaType<BitvectorFormula> bvTargetType = (FormulaType<BitvectorFormula>) targetType;

      BitvectorFormulaManager bfmgr = formulaManager.getBitvectorFormulaManager();
      FormulaType<BitvectorFormula> bv8TargetType =
          formulaManager.getFormulaType(bfmgr.makeBitvector(8, 0));
      final ArrayFormula<BitvectorFormula, BitvectorFormula> arrayFormula =
          afmgr.makeArray(SINGLE_BYTEARRAY_HEAP_NAME, bvAddressType, bv8TargetType);
      return (E) handleBitVectorDeref(arrayFormula, bvAddress, bvTargetType);
    } else {
      throw new UnsupportedOperationException(
          "ByteArray Heap encoding does not support " + targetType.toString());
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <I extends Formula, V extends Formula> V makePointerDereference(
      String targetName, FormulaType<V> targetType, int ssaIndex, I address) {
    if (targetType.isBitvectorType()) {
      BitvectorFormula bvAddress = (BitvectorFormula) address;
      // TODO refactor ugly casts
      final FormulaType<BitvectorFormula> addressType = formulaManager.getFormulaType(bvAddress);
      checkArgument(typeHandler.getPointerType().equals(addressType));
      FormulaType<BitvectorFormula> bvTargetType = (FormulaType<BitvectorFormula>) targetType;
      BitvectorFormulaManager bfmgr = formulaManager.getBitvectorFormulaManager();
      FormulaType<BitvectorFormula> bv8TargetType =
          formulaManager.getFormulaType(bfmgr.makeBitvector(8, 0));

      final ArrayFormula<BitvectorFormula, BitvectorFormula> arrayFormula =
          afmgr.makeArray(SINGLE_BYTEARRAY_HEAP_NAME, ssaIndex, addressType, bv8TargetType);
      return (V) handleBitVectorDeref(arrayFormula, bvAddress, bvTargetType);
    } else {
      throw new UnsupportedOperationException(
          "ByteArray Heap encoding does not support " + targetType.toString());
    }
  }

  private BitvectorFormula handleBitVectorDeref(
      ArrayFormula<BitvectorFormula, BitvectorFormula> arrayFormula,
      BitvectorFormula address,
      FormulaType<BitvectorFormula> targetType) {
    int offset = 0;
    int theN = ((FormulaType.BitvectorType) targetType).getSize();
    BitvectorFormulaManager bfmgr = formulaManager.getBitvectorFormulaManager();
    BitvectorFormula result = afmgr.select(arrayFormula, address);

    while (offset < theN / 8 - 1) {
      BitvectorFormula addressWithOffset =
          bfmgr.add(address, bfmgr.makeBitvector(bfmgr.getLength(address), ++offset));
      BitvectorFormula nextBVPart = afmgr.select(arrayFormula, addressWithOffset);
      result =
          (model.getEndianness() == ByteOrder.LITTLE_ENDIAN)
              ? bfmgr.concat(result, nextBVPart)
              : bfmgr.concat(nextBVPart, result);
    }
    return result;
  }

}
