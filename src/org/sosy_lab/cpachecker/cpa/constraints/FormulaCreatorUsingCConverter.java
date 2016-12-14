/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints;

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.SymbolicExpressionTransformer;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.NumeralFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Creator for {@link Formula}s using a given {@link CtoFormulaConverter} for creating
 * {@link BooleanFormula}s out of {@link Constraint}s.
 *
 * The properties responsible for the behaviour of PredicateCPA's formula handling influence the
 * behaviour of this class, too.
 * A number of important properties can be found in the classes {@link FormulaEncodingOptions}
 * and {@link FormulaManagerView}.
 */
public class FormulaCreatorUsingCConverter implements FormulaCreator {

  private final FormulaManagerView formulaManager;
  private final CtoFormulaConverter toFormulaTransformer;

  private final String functionName;

  public FormulaCreatorUsingCConverter(
      final FormulaManagerView pFormulaManager,
      final CtoFormulaConverter pConverter,
      final String pFunctionName
  ) {
    formulaManager = pFormulaManager;
    toFormulaTransformer = pConverter;
    functionName = pFunctionName;
  }

  @Override
  public BooleanFormula createFormula(final Constraint pConstraint)
      throws UnrecognizedCCodeException, InterruptedException {

    return createFormula(pConstraint, new IdentifierAssignment());
  }


  @Override
  public BooleanFormula createFormula(
      final Constraint pConstraint,
      final IdentifierAssignment pDefiniteAssignment
  ) throws UnrecognizedCCodeException, InterruptedException {

    final SymbolicExpressionTransformer toExpressionTransformer =
        new SymbolicExpressionTransformer(pDefiniteAssignment);

    CExpression constraintExpression = pConstraint.accept(toExpressionTransformer);

    return toFormulaTransformer.makePredicate(
        constraintExpression, getDummyEdge(), functionName, getSsaMapBuilder());
  }

  @Override
  public BooleanFormula transformAssignment(
      final Formula pVariable,
      final Object pTermAssignment
  ) {
    FormulaType<?> variableType = formulaManager.getFormulaType(pVariable);
    Formula rightFormula = null;

    final NumeralFormulaManagerView<NumeralFormula, NumeralFormula.RationalFormula>
        rationalFormulaManager = formulaManager.getRationalFormulaManager();

    if (pTermAssignment instanceof Number) {

      BigInteger integerValue = null;
      BigDecimal decimalValue = null;

      if (pTermAssignment instanceof Long) {
        integerValue = BigInteger.valueOf((long) pTermAssignment);

      } else if (pTermAssignment instanceof BigInteger) {
        integerValue = (BigInteger) pTermAssignment;

      } else if (pTermAssignment instanceof BigDecimal) {
        decimalValue = (BigDecimal) pTermAssignment;

      } else if (pTermAssignment instanceof Float || pTermAssignment instanceof Double) {
        assert variableType.isFloatingPointType();
        final FloatingPointFormula variableAsFloat = (FloatingPointFormula)pVariable;
        final Double assignmentAsDouble;

        if (pTermAssignment instanceof Float) {
          assignmentAsDouble = ((Float)pTermAssignment).doubleValue();
        } else {
          assignmentAsDouble = (Double)pTermAssignment;
        }

        if (assignmentAsDouble.isNaN()) {
          return getNanFormula(variableAsFloat);

        } else if (assignmentAsDouble.equals(Double.POSITIVE_INFINITY)) {
          return getPositiveInfinityFormula(variableAsFloat);

        } else if (assignmentAsDouble.equals(Double.NEGATIVE_INFINITY)) {
          return getNegativeInfinityFormula(variableAsFloat);

        } else {
          decimalValue = BigDecimal.valueOf(assignmentAsDouble);
        }

      } else if (pTermAssignment instanceof Rational) {
        rightFormula = rationalFormulaManager.makeNumber((Rational) pTermAssignment);
      } else {
        throw new AssertionError("Unhandled assignment number " + pTermAssignment);
      }

      if (integerValue != null) {
        rightFormula = formulaManager.makeNumber(variableType, integerValue);

      } else if (decimalValue != null) {

        if (variableType.isRationalType()) {
          rightFormula = rationalFormulaManager.makeNumber(decimalValue);
        } else {
          assert variableType.isFloatingPointType();
          FormulaType.FloatingPointType variableTypeCastToFloatType =
              (FormulaType.FloatingPointType) variableType;

          rightFormula =
              formulaManager.getFloatingPointFormulaManager().makeNumber(
                  decimalValue, variableTypeCastToFloatType);
        }
      }

    } else {
      throw new AssertionError("Unhandled assignment object " + pTermAssignment);
    }

    assert rightFormula != null;
    return formulaManager.makeEqual(pVariable, rightFormula);
  }

  private BooleanFormula getNanFormula(FloatingPointFormula pFormula) {
    return formulaManager.getFloatingPointFormulaManager().isNaN(pFormula);
  }

  private BooleanFormula getPositiveInfinityFormula(FloatingPointFormula pFormula) {
    FormulaType.FloatingPointType formulaType =
        (FormulaType.FloatingPointType) formulaManager.getFormulaType(pFormula);
    Formula infinityFormula =
        formulaManager.getFloatingPointFormulaManager().makePlusInfinity(formulaType);
    return formulaManager.makeEqual(pFormula, infinityFormula);
  }

  private BooleanFormula getNegativeInfinityFormula(FloatingPointFormula pFormula) {
    FormulaType.FloatingPointType formulaType =
        (FormulaType.FloatingPointType) formulaManager.getFormulaType(pFormula);
    Formula infinityFormula =
        formulaManager.getFloatingPointFormulaManager().makeMinusInfinity(formulaType);

    return formulaManager.makeEqual(pFormula, infinityFormula);
  }

  private CFAEdge getDummyEdge() {
    return DummyEdge.getInstance(functionName);
  }

  private SSAMap.SSAMapBuilder getSsaMapBuilder() {
    return SSAMap.emptySSAMap().builder();
  }

  private static class DummyEdge implements CFAEdge {

    private static final String UNKNOWN = "unknown";

    private static Map<String, DummyEdge> existingEdges = new HashMap<>();

    private final CFANode dummyNode;

    private DummyEdge(String pFunctionName) {
      dummyNode = new CFANode(pFunctionName);
    }

    public static DummyEdge getInstance(String pFunctionName) {
      DummyEdge edge = existingEdges.get(pFunctionName);

      if (edge == null) {
        edge = new DummyEdge(pFunctionName);
        existingEdges.put(pFunctionName, edge);
      }

      return edge;
    }

    @Override
    public CFAEdgeType getEdgeType() {
      return CFAEdgeType.BlankEdge;
    }

    @Override
    public CFANode getPredecessor() {
      return dummyNode;
    }

    @Override
    public CFANode getSuccessor() {
      return dummyNode;
    }

    @Override
    public Optional<? extends AAstNode> getRawAST() {
      return Optional.empty();
    }

    @Override
    public int getLineNumber() {
      return 0;
    }

    @Override
    public FileLocation getFileLocation() {
      return FileLocation.DUMMY;
    }

    @Override
    public String getRawStatement() {
      return UNKNOWN;
    }

    @Override
    public String getCode() {
      return UNKNOWN;
    }

    @Override
    public String getDescription() {
      return UNKNOWN;
    }
  }
}
