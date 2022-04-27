// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bdd;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.variableclassification.Partition;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassificationBuilder;

/**
 * This Visitor implements evaluation of expressions, that contain only checks for equality of
 * numbers. In this special case it is possible to store the information in less bits than 32.
 */
public class BDDCompressExpressionVisitor extends DefaultCExpressionVisitor<Region[], NoException> {

  /** This map contains tuples (int, region[]) for each intEqual-partition. */
  private static final Map<Partition, ImmutableMap<BigInteger, Region[]>> INT_REGIONS_MAP =
      new HashMap<>();

  protected final PredicateManager predMgr;
  protected final VariableTrackingPrecision precision;
  private final BitvectorManager bvmgr;
  private final ImmutableMap<BigInteger, Region[]> intToRegions;
  protected final int size;
  protected final CFANode location;

  /**
   * This Visitor returns a representation for an expression.
   *
   * @param size length of compressed bitvector
   * @param pPartition info about variables and numbers
   */
  public BDDCompressExpressionVisitor(
      final PredicateManager pPredMgr,
      final VariableTrackingPrecision pPrecision,
      final int size,
      final CFANode pLocation,
      final BitvectorManager pBVmgr,
      final Partition pPartition) {
    Preconditions.checkNotNull(pPartition);
    predMgr = pPredMgr;
    precision = pPrecision;
    bvmgr = pBVmgr;
    this.size = size;
    intToRegions = initMappingIntToRegions(pPartition);
    location = pLocation;
  }

  /**
   * This function creates a mapping of intEqual partitions to a mapping of number to bitvector.
   * This allows to compress big numbers to a small number of bits in the BDD.
   */
  private ImmutableMap<BigInteger, Region[]> initMappingIntToRegions(final Partition partition) {

    if (!INT_REGIONS_MAP.containsKey(partition)) {
      final ImmutableMap.Builder<BigInteger, Region[]> currentMapping = ImmutableMap.builder();

      // special handling of One and Zero,
      // because they can appear as result of an equality-check.
      // this allows us to check expressions as "((a==0)==5)" with varClass intEQ
      currentMapping.put(BigInteger.ZERO, bvmgr.makeNumber(BigInteger.valueOf(0), size));
      currentMapping.put(BigInteger.ONE, bvmgr.makeNumber(BigInteger.valueOf(1), size));

      int i = 2;
      for (BigInteger num :
          Sets.difference(
              partition.getValues(), Sets.newHashSet(BigInteger.ZERO, BigInteger.ONE))) {
        currentMapping.put(num, bvmgr.makeNumber(BigInteger.valueOf(i), size));
        i++;
      }
      INT_REGIONS_MAP.put(partition, currentMapping.buildOrThrow());
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
    BigInteger val1 = VariableClassificationBuilder.getNumber(pE.getOperand1());
    if (val1 == null) {
      lVal = pE.getOperand1().accept(this);
    } else {
      lVal = intToRegions.get(val1);
      assert lVal != null;
    }

    // for numeral values
    BigInteger val2 = VariableClassificationBuilder.getNumber(pE.getOperand2());
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

  private static Region[] calculateBinaryOperation(
      Region[] lVal,
      Region[] rVal,
      final BitvectorManager bvmgr,
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
    return predMgr.createPredicate(
        idExp.getDeclaration().getQualifiedName(),
        idExp.getExpressionType(),
        location,
        size,
        precision);
  }

  @Override
  public Region[] visit(final CCastExpression castExpression) {
    // We do not expect a changing value through a cast, because then this code would be unsound!
    // This assumption might be unrealistic, but it works in many cases.
    // This code also matches the code in VariableClassification, line 1413, where casts are ignored
    // for intEQ.
    return castExpression.getOperand().accept(this);
  }
}
