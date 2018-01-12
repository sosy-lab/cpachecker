/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.cpa.loopinvariants.polynom.visitors.Visitor.NoException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

@Options(prefix = "cpa.predicate.inferenceObjects")
public class PredicateInferenceObjectManager {

  private final Function<String, String> rename = s -> s + "__ENV";

  private final Function<String, String> localRename = s -> {
    if (s.contains("::")) {
      //local var, rename
      return rename.apply(s);
    } else {
      return s;
    }
  };

  private class ExpressionTransformer implements CRightHandSideVisitor<Pair<CExpression, Boolean>, NoException> {

    private final CFAEdge edge;

    public ExpressionTransformer(CFAEdge pEdge) {
      edge = pEdge;
    }

    @Override
    public Pair<CExpression, Boolean> visit(CArraySubscriptExpression pIastArraySubscriptExpression) throws NoException {

      CExpression arrayExpr = pIastArraySubscriptExpression.getArrayExpression();
      Pair<CExpression, Boolean> newArrayExp = arrayExpr.accept(this);

      Boolean isGlobal = newArrayExp.getSecond();
      Pair<CExpression, Boolean> newSubsExp = pIastArraySubscriptExpression.getSubscriptExpression().accept(this);

      FileLocation loc = pIastArraySubscriptExpression.getFileLocation();
      CType type = pIastArraySubscriptExpression.getExpressionType();

      return Pair.of(new CArraySubscriptExpression(loc, type, newArrayExp.getFirst(), newSubsExp.getFirst()), isGlobal);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CFieldReference pIastFieldReference) throws NoException {

      CExpression owner = pIastFieldReference.getFieldOwner();
      Pair<CExpression, Boolean> newOwner = owner.accept(this);

      FileLocation loc = pIastFieldReference.getFileLocation();
      CType type = pIastFieldReference.getExpressionType();
      String name = pIastFieldReference.getFieldName();
      boolean deref = pIastFieldReference.isPointerDereference();

      CFieldReference result = new CFieldReference(loc, type, name, newOwner.getFirst(), deref);
      return Pair.of(result, newOwner.getSecond() || deref);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CIdExpression pIastIdExpression) throws NoException {
      CSimpleDeclaration decl = pIastIdExpression.getDeclaration();
      if (decl instanceof CDeclaration) {
        if (((CDeclaration) decl).isGlobal()) {
          return Pair.of(pIastIdExpression, true);
        } else if (decl instanceof CVariableDeclaration) {
          FileLocation loc = pIastIdExpression.getFileLocation();
          CType type = ((CVariableDeclaration) decl).getType();
          String name = rename.apply(((CVariableDeclaration) decl).getName());
          String origName = rename.apply(((CVariableDeclaration) decl).getOrigName());
          String qualifName = rename.apply(((CVariableDeclaration) decl).getQualifiedName());

          CDeclaration newDecl =
              new CVariableDeclaration(loc, false, CStorageClass.AUTO, type, name, origName, qualifName, null);

          return Pair.of(new CIdExpression(loc, newDecl), false);
        }
      } else if (decl instanceof CParameterDeclaration) {
        FileLocation loc = pIastIdExpression.getFileLocation();
        CType type = ((CParameterDeclaration) decl).getType();
        String name = rename.apply(((CParameterDeclaration) decl).getName());

        CVariableDeclaration newDecl =
            new CVariableDeclaration(loc, false, CStorageClass.AUTO, type, name, name, name, null);

        return Pair.of(new CIdExpression(loc, newDecl), false);
      }

      return Pair.of(pIastIdExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CPointerExpression pPointerExpression) throws NoException {
      CExpression owner = pPointerExpression.getOperand();
      Pair<CExpression, Boolean> newOwner = owner.accept(this);

      FileLocation loc = pPointerExpression.getFileLocation();
      CType type = pPointerExpression.getExpressionType();

      CPointerExpression result = new CPointerExpression(loc, type, newOwner.getFirst());
      return Pair.of(result, true);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CComplexCastExpression pComplexCastExpression) throws NoException {
      CExpression operand = pComplexCastExpression.getOperand();
      Pair<CExpression, Boolean> newOperand = operand.accept(this);

      FileLocation loc = pComplexCastExpression.getFileLocation();
      CType type = pComplexCastExpression.getExpressionType();
      CType newType = pComplexCastExpression.getType();
      boolean real = pComplexCastExpression.isRealCast();

      CComplexCastExpression result = new CComplexCastExpression(loc, type, newOperand.getFirst(), newType, real);
      return Pair.of(result, newOperand.getSecond());
    }

    @Override
    public Pair<CExpression, Boolean> visit(CBinaryExpression pIastBinaryExpression) throws NoException {
      CExpression operand1 = pIastBinaryExpression.getOperand1();
      CExpression operand2 = pIastBinaryExpression.getOperand2();
      Pair<CExpression, Boolean> newOperand1 = operand1.accept(this);
      Pair<CExpression, Boolean> newOperand2 = operand2.accept(this);

      FileLocation loc = pIastBinaryExpression.getFileLocation();
      CType type = pIastBinaryExpression.getExpressionType();
      CType calcType = pIastBinaryExpression.getCalculationType();
      BinaryOperator operator = pIastBinaryExpression.getOperator();

      CBinaryExpression result = new CBinaryExpression(loc, type, calcType, newOperand1.getFirst(), newOperand2.getFirst(), operator);
      return Pair.of(result, newOperand1.getSecond() || newOperand2.getSecond());
    }

    @Override
    public Pair<CExpression, Boolean> visit(CCastExpression pIastCastExpression) throws NoException {
      CExpression operand = pIastCastExpression.getOperand();
      Pair<CExpression, Boolean> newOperand = operand.accept(this);

      if (newOperand == null) {
        return null;
      }

      FileLocation loc = pIastCastExpression.getFileLocation();
      CType type = pIastCastExpression.getExpressionType();

      CCastExpression result = new CCastExpression(loc, type, newOperand.getFirst());
      return Pair.of(result, newOperand.getSecond());
    }

    @Override
    public Pair<CExpression, Boolean> visit(CCharLiteralExpression pIastCharLiteralExpression) throws NoException {
      return Pair.of(pIastCharLiteralExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws NoException {
      return Pair.of(pIastFloatLiteralExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws NoException {
      return Pair.of(pIastIntegerLiteralExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CStringLiteralExpression pIastStringLiteralExpression) throws NoException {
      return Pair.of(pIastStringLiteralExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CTypeIdExpression pIastTypeIdExpression) throws NoException {
      return Pair.of(pIastTypeIdExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CUnaryExpression pIastUnaryExpression) throws NoException {
      CExpression operand = pIastUnaryExpression.getOperand();
      Pair<CExpression, Boolean> newOperand = operand.accept(this);

      if (newOperand == null) {
        return null;
      }

      FileLocation loc = pIastUnaryExpression.getFileLocation();
      CType type = pIastUnaryExpression.getExpressionType();
      UnaryOperator operator = pIastUnaryExpression.getOperator();

      CUnaryExpression result = new CUnaryExpression(loc, type, newOperand.getFirst(), operator);
      return Pair.of(result, newOperand.getSecond());
    }

    @Override
    public Pair<CExpression, Boolean> visit(CImaginaryLiteralExpression PIastLiteralExpression) throws NoException {
      return Pair.of(PIastLiteralExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws NoException {
      return Pair.of(pAddressOfLabelExpression, false);
    }

    @Override
    public Pair<CExpression, Boolean> visit(CFunctionCallExpression pIastFunctionCallExpression) throws NoException {
      CFunctionCallExpression fExp = pIastFunctionCallExpression;
      CIdExpression rhs;

      CFunctionSummaryEdge summary = ((CFunctionReturnEdge) edge).getSummaryEdge();
      final com.google.common.base.Optional<CVariableDeclaration> returnVariableDeclaration =
          summary.getFunctionEntry().getReturnVariable();

      assert returnVariableDeclaration.isPresent();

      rhs = new CIdExpression(fExp.getFileLocation(), returnVariableDeclaration.get());

      return Pair.of(rhs, false);
    }

  }

  @Option(secure = true, name = "useUndefFunctions",
      description = "Use undefined assignments (havoc abstraction)")
  private boolean useUndefFuncs = true;

  @Option(secure = true, name = "abstractionLattice",
      description = "merge inference objects only with equal abstraction")
  private boolean abstractionLattice = false;

  @Option(secure = true, name = "emptyAbstraction",
      description = "do not consider abstraction from inference objects")
  private boolean emptyAbstraction = false;

  private final BooleanFormulaManager mngr;
  private final PredicateAbstractionManager amngr;
  private final FormulaManagerView fmngr;

  public PredicateInferenceObjectManager(Configuration pConfig, BooleanFormulaManager pMngr,
      PredicateAbstractionManager pAbstractionManager, FormulaManagerView pFManager) throws InvalidConfigurationException {
    pConfig.inject(this);
    mngr = pMngr;
    amngr = pAbstractionManager;
    fmngr = pFManager;
  }

  public InferenceObject create(CFAEdge edge, AbstractionFormula a) {
    CStatement stmnt;

    if (edge instanceof CFunctionReturnEdge) {
      CFunctionSummaryEdge summary = ((CFunctionReturnEdge) edge).getSummaryEdge();
      stmnt = summary.getExpression();
    } else if (edge instanceof CStatementEdge) {
      stmnt = ((CStatementEdge) edge).getStatement();
    } else {
      return EmptyInferenceObject.getInstance();
    }

    if (stmnt instanceof CAssignment) {
      CExpression exp = ((CAssignment) stmnt).getLeftHandSide();

      ExpressionTransformer transformer = new ExpressionTransformer(edge);
      Pair<CExpression, Boolean> left = exp.accept(transformer);

      if (left.getSecond()) {

        CRightHandSide right = ((CAssignment) stmnt).getRightHandSide();
        CAssignment newAssignement;

        if (useUndefFuncs || stmnt instanceof CFunctionCallAssignmentStatement && edge instanceof CStatementEdge) {
          CFunctionCallExpression fExp = prepareUndefFunctionFor(right);

          newAssignement = new CFunctionCallAssignmentStatement(fExp.getFileLocation(), (CLeftHandSide) left.getFirst(), fExp);
        } else {
          Pair<CExpression, Boolean> newRight = right.accept(transformer);

          newAssignement = new CExpressionAssignmentStatement(newRight.getFirst().getFileLocation(), (CLeftHandSide) left.getFirst(), newRight.getFirst());
        }
        return new PredicateInferenceObject(Collections.singleton(newAssignement), prepareFormula(a.asFormula()));
      }
    }
    return EmptyInferenceObject.getInstance();
  }

  public InferenceObject join(PredicateInferenceObject pFirst, PredicateInferenceObject pSecond) {
    BooleanFormula abs1 = pFirst.getGuard();
    BooleanFormula abs2 = pSecond.getGuard();
    Set<CAssignment> formulas1 = pFirst.getAction();
    Set<CAssignment> formulas2 = pSecond.getAction();

    Set<CAssignment> newFormulas;
    BooleanFormula newAbs;

    if (abstractionLattice) {
      if (!abs1.equals(abs2)) {
        return pSecond;
      } else {
        newAbs = abs2;
      }
    } else {
      if (mngr.isTrue(abs2)) {
        newAbs = abs2;
      } else if (mngr.isTrue(abs1)) {
        newAbs = abs1;
      } else {
        newAbs = mngr.or(abs1, abs2);
      }
    }

    if (formulas2.containsAll(formulas1)) {
      newFormulas = formulas2;
    } else {
      newFormulas = Sets.union(formulas1, formulas2);
    }

    if (newAbs == abs2 && newFormulas == formulas2) {
      return pSecond;
    } else {
      return new PredicateInferenceObject(newFormulas, newAbs);
    }
  }

  private CFunctionCallExpression prepareUndefFunctionFor(CRightHandSide right) {

    FileLocation loc = right.getFileLocation();
    CType type = right.getExpressionType();

    CFunctionType fType = new CFunctionType(right.getExpressionType(), Collections.emptyList(), false);
    String retType = fType.getReturnType().getCanonicalType().toString();
    retType = retType.replace(" ", "_");
    CFunctionDeclaration funcDecl = new CFunctionDeclaration(loc, fType, "__VERIFIER_nondet_" + retType, Collections.emptyList());
    CExpression name = new CIdExpression(loc, funcDecl);

    return new CFunctionCallExpression(loc, type, name, Collections.emptyList(), funcDecl);
  }

  private BooleanFormula prepareFormula(BooleanFormula formula) {
    if (emptyAbstraction) {
      return mngr.makeTrue();
    }
    BooleanFormula result = fmngr.renameFreeVariablesAndUFs(formula, localRename);
    return result;
  }
}
