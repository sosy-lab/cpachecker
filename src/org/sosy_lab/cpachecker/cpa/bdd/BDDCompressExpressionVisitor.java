/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bdd;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.sosy_lab.cpachecker.cfa.ast.c.*;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.VariableClassification.Partition;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * This Visitor implements evaluation of expressions,
 * that contain only checks for equality of numbers.
 * In this special case it is possible to store the information in less bits than 32.
 */
public class BDDCompressExpressionVisitor
        extends DefaultCExpressionVisitor<Region[], RuntimeException> {

  /** This map contains tuples (int, region[]) for each intEqual-partition. */
  private static final Map<Partition, Map<BigInteger, Region[]>> INT_REGIONS_MAP = new HashMap<>();

  private final BDDTransferRelation transferRelation;
  private final BitvectorManager bvmgr;
  private final Map<BigInteger, Region[]> intToRegions;
  private final int size;

  /** This Visitor returns a representation for an expression.
   * @param pTransferRelation needed for variableNames
   * @param pPartition info about variables and numbers
   */
  public BDDCompressExpressionVisitor(final BDDTransferRelation pTransferRelation,
                                      final BitvectorManager pBVmgr, final Partition pPartition) {
    Preconditions.checkNotNull(pPartition);
    this.transferRelation = pTransferRelation;
    this.bvmgr = pBVmgr;
    this.size = transferRelation.getBitsize(pPartition, null);
    this.intToRegions = initMappingIntToRegions(pPartition);
  }

  /** This function creates a mapping of intEqual partitions to a mapping of number to bitvector.
   * This allows to compress big numbers to a small number of bits in the BDD. */
  private Map<BigInteger, Region[]> initMappingIntToRegions(final Partition partition) {

    if (!INT_REGIONS_MAP.containsKey(partition)) {
      final Map<BigInteger, Region[]> currentMapping = new HashMap<>();

      // special handling of One and Zero,
      // because they can appear as result of an equality-check.
      // this allows us to check expressions as "((a==0)==5)" with varClass intEQ
      currentMapping.put(BigInteger.ZERO, bvmgr.makeNumber(BigInteger.valueOf(0), size));
      currentMapping.put(BigInteger.ONE, bvmgr.makeNumber(BigInteger.valueOf(1), size));

      int i = 2;
      for (BigInteger num : Sets.difference(
              partition.getValues(), Sets.newHashSet(BigInteger.ZERO, BigInteger.ONE))) {
        currentMapping.put(num, bvmgr.makeNumber(BigInteger.valueOf(i), size));
        i++;
      }
      INT_REGIONS_MAP.put(partition, currentMapping);
    }
    return INT_REGIONS_MAP.get(partition);
  }

  @Override
  protected Region[] visitDefault(CExpression pExp) {
    return null;
  }

  @Override
  public Region[] visit(final CBinaryExpression pE) {

    // for numeral values
    Region[] lVal;
    BigInteger val1 = VariableClassification.getNumber(pE.getOperand1());
    if (val1 == null) {
      lVal = pE.getOperand1().accept(this);
    } else {
      lVal = intToRegions.get(val1);
      assert lVal != null;
    }

    // for numeral values
    BigInteger val2 = VariableClassification.getNumber(pE.getOperand2());
    Region[] rVal;
    if (val2 == null) {
      rVal = pE.getOperand2().accept(this);
    } else {
      rVal = intToRegions.get(val2);
      assert rVal != null;
    }

    if (lVal == null || rVal == null) {
      return null;
    }

    return calculateBinaryOperation(lVal, rVal, bvmgr, pE);
  }

  private static Region[] calculateBinaryOperation(Region[] lVal, Region[] rVal, final BitvectorManager bvmgr,
                                                   final CBinaryExpression binaryExpr) {

    assert lVal.length == rVal.length;
    final BinaryOperator binaryOperator = binaryExpr.getOperator();
    switch (binaryOperator) {
      case EQUALS:
        return bvmgr.wrapLast(bvmgr.makeLogicalEqual(lVal, rVal), lVal.length);
      case NOT_EQUALS:
        return bvmgr.wrapLast(bvmgr.makeNot(bvmgr.makeLogicalEqual(lVal, rVal)), lVal.length);
      default:
        throw new AssertionError("no support for further operators: " + binaryOperator);
    }
  }

  @Override
  public Region[] visit(CCharLiteralExpression pE) {
    return intToRegions.get(BigInteger.valueOf(pE.getCharacter()));
  }

  @Override
  public Region[] visit(CIntegerLiteralExpression pE) {
    return intToRegions.get(pE.getValue());
  }

  @Override
  public Region[] visit(CIdExpression idExp) {
    if (idExp.getDeclaration() instanceof CEnumerator) {
      CEnumerator enumerator = (CEnumerator) idExp.getDeclaration();
      if (enumerator.hasValue()) {
        return intToRegions.get(BigInteger.valueOf(enumerator.getValue()));
      } else {
        return null;
      }
    }
    return transferRelation.createPredicate(idExp, size);
  }
}