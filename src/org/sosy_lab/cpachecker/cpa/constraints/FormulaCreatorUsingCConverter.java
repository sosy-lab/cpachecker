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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.SymbolicExpressionTransformer;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;

import com.google.common.base.Optional;

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
  private final SymbolicExpressionTransformer toExpressionTransformer;

  private final String functionName;

  public FormulaCreatorUsingCConverter(FormulaManagerView pFormulaManager, CtoFormulaConverter pConverter,
      ValueAnalysisState pValueState, String pFunctionName) {

    formulaManager = pFormulaManager;
    toFormulaTransformer = pConverter;
    toExpressionTransformer = new SymbolicExpressionTransformer(pValueState);
    functionName = pFunctionName;
  }

  @Override
  public BooleanFormula createFormula(Constraint pConstraint) throws UnrecognizedCCodeException, InterruptedException {
    CExpression constraintExpression = pConstraint.accept(toExpressionTransformer);
    return toFormulaTransformer.makePredicate(constraintExpression, getDummyEdge(), functionName, getSsaMapBuilder());
  }

  @Override
  public BooleanFormula transformAssignment(Model.AssignableTerm pTerm, Object termAssignment)
      throws UnrecognizedCCodeException, InterruptedException {
    Formula variable;
    Formula rightFormula;

    if (termAssignment instanceof Number) {

      BigInteger value;
      CExpression expression;

      if (termAssignment instanceof Long) {
        value = BigInteger.valueOf((long) termAssignment);

      } else if (termAssignment instanceof BigInteger) {
        value = (BigInteger) termAssignment;

      } else {
        throw new AssertionError("Unhandled assignment number " + termAssignment);
      }

      expression = new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.LONG_INT, value);
      rightFormula = toFormulaTransformer.makePredicate(expression, getDummyEdge(), functionName, getSsaMapBuilder());

    } else {
      throw new AssertionError("Unhandled assignment object " + termAssignment);
    }

    variable = createVariable(pTerm, formulaManager.getFormulaType(rightFormula));

    return formulaManager.makeEqual(variable, rightFormula);
  }

  private CFAEdge getDummyEdge() {
    return DummyEdge.getInstance(functionName);
  }

  private SSAMap.SSAMapBuilder getSsaMapBuilder() {
    return SSAMap.emptySSAMap().builder();
  }

  private Formula createVariable(Model.AssignableTerm pTerm, FormulaType<?> pType) {
    final String name = pTerm.getName();

    return formulaManager.makeVariable(pType, name);
  }

  private static class DummyEdge implements CFAEdge {

    private static final String UNKNOWN = "unknown";
    private static final FileLocation DUMMY_LOCATION = new FileLocation(0, UNKNOWN, 0, 0, 0);

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
      return Optional.absent();
    }

    @Override
    public int getLineNumber() {
      return 0;
    }

    @Override
    public FileLocation getFileLocation() {
      return DUMMY_LOCATION;
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
