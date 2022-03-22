// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bdd;

import java.math.BigInteger;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

class BitvectorComputer {

  private final boolean compressIntEqual;
  private final VariableClassification varClass;
  private final BitvectorManager bvmgr;
  private final NamedRegionManager rmgr;
  private final PredicateManager predmgr;
  private final MachineModel machineModel;

  public BitvectorComputer(
      boolean pCompressIntEqual,
      VariableClassification pVarClass,
      BitvectorManager pBvmgr,
      NamedRegionManager pRmgr,
      PredicateManager pPredmgr,
      MachineModel pMachineModel) {
    compressIntEqual = pCompressIntEqual;
    varClass = pVarClass;
    bvmgr = pBvmgr;
    rmgr = pRmgr;
    predmgr = pPredmgr;
    machineModel = pMachineModel;
  }

  /**
   * This function returns the bitsize for vars of a partition. For a boolean var the value is 1.
   *
   * <p>Compression for IntEqual-vars: For N different values (maybe plus 2 for additional Zero and
   * One) of M different variables there are N+M possible values for a variable (one for each value
   * and one for each (maybe uninitialized) variable). For N+M different values we need at least
   * log_2(N+M) bits in the representation.
   *
   * <p>For other variables, the length of the CType is used.
   */
  int getBitsize(@Nullable Partition partition, @Nullable CType type) {
    if (partition == null) {
      // we know nothing about the partition, so do not track it with BDDCPA
      return 0;
    } else if (varClass.getIntBoolPartitions().contains(partition)) {
      return 1;
    } else if (compressIntEqual && varClass.getIntEqualPartitions().contains(partition)) {
      final Set<BigInteger> values = partition.getValues();
      int N = values.size();
      if (!values.contains(BigInteger.ZERO)) {
        N++;
      }
      if (!values.contains(BigInteger.ONE)) {
        N++;
      }
      int M = partition.getVars().size();
      return (int) Math.ceil(Math.log(N + M) / Math.log(2));
    } else if (type != null) {
      return machineModel.getSizeofInBits(type).intValueExact();
    } else {
      // we know nothing
      return 0;
    }
  }

  /**
   * This function returns a bitvector, that represents the expression. The partition chooses the
   * compression of the bitvector.
   */
  @Nullable Region[] evaluateVectorExpression(
      final Partition partition,
      final CExpression exp,
      CType targetType,
      final CFANode location,
      VariableTrackingPrecision precision)
      throws UnsupportedCodeException {
    final boolean compress =
        (partition != null)
            && compressIntEqual
            && varClass.getIntEqualPartitions().contains(partition);
    if (varClass.getIntBoolPartitions().contains(partition)) {
      Region booleanResult =
          exp.accept(new BDDBooleanExpressionVisitor(predmgr, rmgr, precision, location));
      return (booleanResult == null) ? null : new Region[] {booleanResult};
    } else if (compress) {
      return exp.accept(
          new BDDCompressExpressionVisitor(
              predmgr, precision, getBitsize(partition, null), location, bvmgr, partition));
    } else {
      Region[] value =
          exp.accept(
              new BDDVectorCExpressionVisitor(predmgr, precision, bvmgr, machineModel, location));
      targetType = targetType.getCanonicalType();
      if (value != null && targetType instanceof CSimpleType) {
        // cast to correct length
        final CType sourceType = exp.getExpressionType().getCanonicalType();
        value =
            bvmgr.toBitsize(
                machineModel.getSizeofInBits((CSimpleType) targetType),
                sourceType instanceof CSimpleType
                    && machineModel.isSigned((CSimpleType) sourceType),
                value);
      }
      return value;
    }
  }

  /**
   * This function returns a bitvector, that represents the expression. The partition chooses the
   * compression of the bitvector.
   */
  @Nullable Region[] evaluateVectorExpressionWithPointerState(
      final Partition partition,
      final CExpression exp,
      CType targetType,
      final CFANode location,
      final PointerState pPointerInfo,
      VariableTrackingPrecision precision)
      throws UnsupportedCodeException {
    final boolean compress =
        (partition != null)
            && compressIntEqual
            && varClass.getIntEqualPartitions().contains(partition);
    if (varClass.getIntBoolPartitions().contains(partition)) {
      Region booleanResult =
          exp.accept(
              new BDDPointerBooleanExpressionVisitor(
                  predmgr, rmgr, precision, location, pPointerInfo));
      return (booleanResult == null) ? null : new Region[] {booleanResult};
    } else if (compress) {
      return exp.accept(
          new BDDPointerCompressExpressionVisitor(
              predmgr,
              precision,
              getBitsize(partition, null),
              location,
              bvmgr,
              partition,
              pPointerInfo));
    } else {
      Region[] value =
          exp.accept(
              new BDDPointerVectorCExpressionVisitor(
                  predmgr, precision, bvmgr, machineModel, location, pPointerInfo));
      targetType = targetType.getCanonicalType();
      if (value != null && targetType instanceof CSimpleType) {
        // cast to correct length
        final CType sourceType = exp.getExpressionType().getCanonicalType();
        value =
            bvmgr.toBitsize(
                machineModel.getSizeofInBits((CSimpleType) targetType),
                sourceType instanceof CSimpleType
                    && machineModel.isSigned((CSimpleType) sourceType),
                value);
      }
      return value;
    }
  }
}
