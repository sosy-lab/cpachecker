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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.eclipse.cdt.core.dom.ast.IASTArrayDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTArrayModifier;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTConditionalExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTInitializerList;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTProblemDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTProblemStatement;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;
import org.eclipse.cdt.core.dom.ast.ICompositeType;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.c.ICASTArrayDesignator;
import org.eclipse.cdt.core.dom.ast.c.ICASTDesignatedInitializer;
import org.eclipse.cdt.core.dom.ast.c.ICASTDesignator;
import org.eclipse.cdt.core.dom.ast.c.ICASTFieldDesignator;
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTCompoundStatementExpression;
import org.eclipse.cdt.core.dom.ast.gnu.c.IGCCASTArrayRangeDesignator;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionCallExpression;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CSourceOriginMapping;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.simplification.ExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cfa.simplification.NonRecursiveExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@Options(prefix="cfa")
class ASTConverter {

  @Option(
      description="simplify pointer expressions like s->f to (*s).f with this option " +
        "the cfa is simplified until at maximum one pointer is allowed for left- and rightHandSide")
  private boolean simplifyPointerExpressions = false;

  @Option(
      description="simplify simple const expressions like 1+2")
  private boolean simplifyConstExpressions = true;

  private final ExpressionSimplificationVisitor expressionSimplificator;
  private final NonRecursiveExpressionSimplificationVisitor nonRecursiveExpressionSimplificator;
  private final CBinaryExpressionBuilder binExprBuilder;

  private final LogManager logger;
  private final ASTLiteralConverter literalConverter;
  private final ASTTypeConverter typeConverter;

  /**
   * Given a file name, returns a "nice" representation of it.
   * This should be used for situations where the name is going
   * to be presented to the user.
   * The result may be the empty string, if for example CPAchecker only uses
   * one file (we expect the user to know its name in this case).
   */
  private final Function<String, String> niceFileNameFunction;

  private final CSourceOriginMapping sourceOriginMapping;

  private final Scope scope;

  // this counter is static to make the replacing names for anonymous types, in
  // more than one file (which get parsed with different AstConverters, although
  // they are in the same run) unique
  private static int anonTypeCounter = 0;
  private static int anonTypeMemberCounter = 0;


  private final Sideassignments sideAssignmentStack;
  private final String staticVariablePrefix;

  private static final ContainsProblemTypeVisitor containsProblemTypeVisitor = new ContainsProblemTypeVisitor();

  public ASTConverter(Configuration pConfig, Scope pScope, LogManagerWithoutDuplicates pLogger,
      Function<String, String> pNiceFileNameFunction,
      CSourceOriginMapping pSourceOriginMapping,
      MachineModel pMachineModel, String pStaticVariablePrefix,
      Sideassignments pSideAssignmentStack) throws InvalidConfigurationException {

    pConfig.inject(this);

    this.scope = pScope;
    this.logger = pLogger;
    this.typeConverter = new ASTTypeConverter(scope, this, pStaticVariablePrefix);
    this.literalConverter = new ASTLiteralConverter(typeConverter, pMachineModel);
    this.niceFileNameFunction = pNiceFileNameFunction;
    this.sourceOriginMapping = pSourceOriginMapping;
    this.staticVariablePrefix = pStaticVariablePrefix;
    this.sideAssignmentStack = pSideAssignmentStack;

    this.expressionSimplificator = new ExpressionSimplificationVisitor(pMachineModel, pLogger);
    this.nonRecursiveExpressionSimplificator = new NonRecursiveExpressionSimplificationVisitor(pMachineModel, pLogger);
    this.binExprBuilder = new CBinaryExpressionBuilder(pMachineModel, pLogger);
  }

  public CExpression convertExpressionWithoutSideEffects(
      IASTExpression e) {

    CAstNode node = convertExpressionWithSideEffects(e);
    if (node == null || node instanceof CExpression) {
      return (CExpression) node;

    } else if (node instanceof CFunctionCallExpression) {
      return addSideassignmentsForExpressionsWithoutSideEffects(node, e);

    } else if (e instanceof IASTUnaryExpression && (((IASTUnaryExpression)e).getOperator() == IASTUnaryExpression.op_postFixDecr
                                                   || ((IASTUnaryExpression)e).getOperator() == IASTUnaryExpression.op_postFixIncr)) {
      return addSideAssignmentsForUnaryExpressions(((CAssignment)node).getLeftHandSide(),
          node.getFileLocation(), typeConverter.convert(e.getExpressionType()),
          ((CBinaryExpression)((CAssignment)node).getRightHandSide()).getOperator());

    } else if (node instanceof CAssignment) {
      sideAssignmentStack.addPreSideAssignment(node);
      return ((CAssignment) node).getLeftHandSide();

    } else {
      throw new AssertionError("unknown expression " + node);
    }
  }

  /**
   * Simplify an expression as much as possible.
   * Use this when you always want to evaluate a specific expression if possible,
   * e.g. array lengths (which should be constant if possible).
   */
  CExpression simplifyExpressionRecursively(CExpression exp) {
    return exp.accept(expressionSimplificator);
  }

  /**
   * Do a single step of expression simplification (not recursively).
   * Use this when you do not care about full evaluation,
   * or you know the operands are already evaluated if possible.
   */
  CExpression simplifyExpressionOneStep(CExpression exp) {
    return exp.accept(nonRecursiveExpressionSimplificator);
  }

  private CExpression addSideassignmentsForExpressionsWithoutSideEffects(CAstNode node,
                                                                            IASTExpression e) {
    CIdExpression tmp = createTemporaryVariable(e);

    sideAssignmentStack.addPreSideAssignment(new CFunctionCallAssignmentStatement(getLocation(e),
                                                                tmp,
                                                                (CFunctionCallExpression) node));
    return tmp;
  }


  /** This method builds a preSideAssignment for x=x+1 or x=x-1 and
   * returns a tmp-variable, that has the value of x before the operation.
   *
   * @param exp the "x" of x=x+1
   * @param loc location of the expression
   * @param type result-typeof the operation
   * @param op binary operator, should be PLUS or MINUS */
  private CIdExpression addSideAssignmentsForUnaryExpressions(
      final CLeftHandSide exp, final FileLocation fileLoc,
      final CType type, final BinaryOperator op) {
    final CIdExpression tmp = createInitializedTemporaryVariable(fileLoc, exp.getExpressionType(), exp);
    final CBinaryExpression postExp = binExprBuilder.buildBinaryExpression(exp, CNumericTypes.ONE, op);
    sideAssignmentStack.addPreSideAssignment(new CExpressionAssignmentStatement(fileLoc, exp, postExp));
    return tmp;
  }


  private CComplexTypeDeclaration addSideEffectDeclarationForType(CCompositeType type, FileLocation loc) {
    CComplexTypeDeclaration decl = new CComplexTypeDeclaration(loc, scope.isGlobalScope(), type);

    scope.registerTypeDeclaration(decl);
    sideAssignmentStack.addPreSideAssignment(decl);
    return decl;
  }

  protected CAstNode convertExpressionWithSideEffects(IASTExpression e) {
    CAstNode converted = convertExpressionWithSideEffectsNotSimplified(e);
    if (!simplifyConstExpressions || !(converted instanceof CExpression)) {
      return converted;
    }

    return simplifyExpressionOneStep((CExpression)converted);
  }

  private CAstNode convertExpressionWithSideEffectsNotSimplified(IASTExpression e) {
    assert !(e instanceof CExpression);

    if (e == null) {
      return null;

    } else if (e instanceof IASTArraySubscriptExpression) {
      return convert((IASTArraySubscriptExpression)e);

    } else if (e instanceof IASTBinaryExpression) {
      return convert((IASTBinaryExpression)e);

    } else if (e instanceof IASTCastExpression) {
      return convert((IASTCastExpression)e);

    } else if (e instanceof IASTFieldReference) {
      return convert((IASTFieldReference)e);

    } else if (e instanceof IASTFunctionCallExpression) {
      return convert((IASTFunctionCallExpression)e);

    } else if (e instanceof IASTIdExpression) {
      CExpression exp = convert((IASTIdExpression)e);
      CType type = exp.getExpressionType();

      // this id expression is the name of a function. When there is no
      // functionCallExpressionn or unaryexpression with pointertype and operator.Amper
      // around it, we create it.
      if (type instanceof CFunctionType
          && !(e.getParent() instanceof IASTFunctionCallExpression
              || (e.getParent() instanceof IASTUnaryExpression
                  && ((IASTUnaryExpression) e.getParent()).getOperator() == IASTUnaryExpression.op_amper))) {
        exp = new CUnaryExpression(exp.getFileLocation(),
                                   new CPointerType(type.isConst(),
                                                    type.isVolatile(),
                                                    type),
                                   exp,
                                   UnaryOperator.AMPER);
        }
      return exp;

    } else if (e instanceof IASTLiteralExpression) {
      return literalConverter.convert((IASTLiteralExpression)e, getLocation(e));

    } else if (e instanceof IASTUnaryExpression) {
      return convert((IASTUnaryExpression)e);

    } else if (e instanceof IASTTypeIdExpression) {
      return convert((IASTTypeIdExpression)e);

    } else if (e instanceof IASTTypeIdInitializerExpression) {
      return convert((IASTTypeIdInitializerExpression)e);

    } else if (e instanceof IASTConditionalExpression) {
      return convert((IASTConditionalExpression)e);

    } else if (e instanceof IGNUASTCompoundStatementExpression) {
      return convert((IGNUASTCompoundStatementExpression)e);

    } else if (e instanceof IASTExpressionList) {
      return convertExpressionListAsExpression((IASTExpressionList)e);

    } else {
      throw new CFAGenerationRuntimeException("Unknown expression type " + e.getClass().getSimpleName(), e);
    }
  }

  static enum CONDITION { NORMAL, ALWAYS_FALSE, ALWAYS_TRUE }

  CONDITION getConditionKind(final CExpression condition) {

    if (condition instanceof CIntegerLiteralExpression
        || condition instanceof CCharLiteralExpression) {
      // constant int value
      if (isZero(condition)) {
        return CONDITION.ALWAYS_FALSE;
      } else {
        return CONDITION.ALWAYS_TRUE;
      }
    }
    return CONDITION.NORMAL;
  }

  private CAstNode convert(IASTConditionalExpression e) {
    CExpression condition = convertExpressionWithoutSideEffects(e.getLogicalConditionExpression());
    // Here we call simplify manually, because for conditional expressions
    // we always want a full evaluation because we might be able to prevent
    // a branch in the CFA.
    // In global scope, this is even required because there cannot be any branches.
    CExpression simplifiedCondition = simplifyExpressionRecursively(condition);

    switch (getConditionKind(simplifiedCondition)) {
    case ALWAYS_TRUE:
      return convertExpressionWithSideEffects(e.getPositiveResultExpression());
    case ALWAYS_FALSE:
      return convertExpressionWithSideEffects(e.getNegativeResultExpression());
    default:
    }

    CIdExpression tmp = createTemporaryVariable(e);
    sideAssignmentStack.addConditionalExpression(e, tmp);
    return tmp;
  }

  private boolean isZero(CExpression exp) {
    if (exp instanceof CIntegerLiteralExpression) {
      BigInteger value = ((CIntegerLiteralExpression)exp).getValue();
      return value.equals(BigInteger.ZERO);
    }
    if (exp instanceof CCharLiteralExpression) {
      char value = ((CCharLiteralExpression)exp).getCharacter();
      return value == 0;
    }
    return false;
  }

  private CAstNode convert(IGNUASTCompoundStatementExpression e) {
    CIdExpression tmp = createTemporaryVariable(e);
    sideAssignmentStack.addConditionalExpression(e, tmp);
    return tmp;
  }

  private CAstNode convertExpressionListAsExpression(IASTExpressionList e) {
    CIdExpression tmp = createTemporaryVariable(e);
    sideAssignmentStack.addConditionalExpression(e, tmp);
    return tmp;
  }

  private CArraySubscriptExpression convert(IASTArraySubscriptExpression e) {
    return new CArraySubscriptExpression(getLocation(e),
        typeConverter.convert(e.getExpressionType()),
        convertExpressionWithoutSideEffects(e.getArrayExpression()),
        convertExpressionWithoutSideEffects(toExpression(e.getArgument())));
  }

  /**
   * creates temporary variables with increasing numbers
   */
  private CIdExpression createTemporaryVariable(IASTExpression e) {
    return createInitializedTemporaryVariable(
        getLocation(e), typeConverter.convert(e.getExpressionType()), null);
  }

  /**
   * creates temporary variables with increasing numbers with a certain initializer.
   * If the initializer is 'null', no initializer will be created.
   */
  private CIdExpression createInitializedTemporaryVariable(
      final FileLocation loc, final CType pType, @Nullable CExpression initializer) {
    String name = "__CPAchecker_TMP_";
    int i = 0;
    while (scope.variableNameInUse(name + i, name + i)) {
      i++;
    }
    name += i;

    CInitializerExpression initExp = null;
    if (initializer != null) {
      initExp = new CInitializerExpression(loc, initializer);
    }

    // If there is no initializer, the variable cannot be const.
    // TODO: consider always adding a const modifier if there is an initializer
    CType type = (initializer == null) ? CTypes.withoutConst(pType) : pType;

    CVariableDeclaration decl = new CVariableDeclaration(loc,
                                               false,
                                               CStorageClass.AUTO,
                                               type,
                                               name,
                                               name,
                                               scope.createScopedNameOf(name),
                                               initExp);

    scope.registerDeclaration(decl);
    sideAssignmentStack.addPreSideAssignment(decl);

    return new CIdExpression(loc, decl);
  }

  private CAstNode convert(IASTBinaryExpression e) {

    switch (e.getOperator()) {
    case IASTBinaryExpression.op_logicalAnd:
    case IASTBinaryExpression.op_logicalOr:
      CIdExpression tmp = createTemporaryVariable(e);
      sideAssignmentStack.addConditionalExpression(e, tmp);
      return tmp;
    }

    Pair<BinaryOperator, Boolean> opPair = ASTOperatorConverter.convertBinaryOperator(e);
    BinaryOperator op = opPair.getFirst();
    boolean isAssign = opPair.getSecond();

    FileLocation fileLoc = getLocation(e);
    CExpression leftHandSide = convertExpressionWithoutSideEffects(e.getOperand1());

    if (isAssign) {
      if (!(leftHandSide instanceof CLeftHandSide)) {
        throw new CFAGenerationRuntimeException("Lefthandside of Assignment " + e.getRawSignature() +" is no CLeftHandside but should be.", leftHandSide);
      }
      CLeftHandSide lhs = (CLeftHandSide) leftHandSide;

      if (op == null) {
        // a = b
        CAstNode rightHandSide = convertExpressionWithSideEffects(e.getOperand2()); // right-hand side may have a function call

        if (rightHandSide instanceof CExpression) {
          // a = b
          return new CExpressionAssignmentStatement(fileLoc, lhs, (CExpression)rightHandSide);

        } else if (rightHandSide instanceof CFunctionCallExpression) {
          // a = f()
          return new CFunctionCallAssignmentStatement(fileLoc, lhs, (CFunctionCallExpression)rightHandSide);

        } else if (rightHandSide instanceof CAssignment) {
          sideAssignmentStack.addPreSideAssignment(rightHandSide);
          return new CExpressionAssignmentStatement(fileLoc, lhs, ((CAssignment) rightHandSide).getLeftHandSide());
        } else {
          throw new CFAGenerationRuntimeException("Expression is not free of side-effects", e);
        }

      } else {
        // a += b etc.
        CExpression rightHandSide = convertExpressionWithoutSideEffects(e.getOperand2());

        // first create expression "a + b"
        CBinaryExpression exp = binExprBuilder.buildBinaryExpression(leftHandSide, rightHandSide, op);

        // and now the assignment
        return new CExpressionAssignmentStatement(fileLoc, lhs, exp);
      }

    } else {
      CExpression rightHandSide = convertExpressionWithoutSideEffects(e.getOperand2());
      return binExprBuilder.buildBinaryExpression(leftHandSide, rightHandSide, op);
    }
  }

  private static boolean isPointerToVoid(final IASTExpression e) {
    return (e.getExpressionType() instanceof IPointerType) &&
           ((IPointerType) e.getExpressionType()).getType() instanceof IBasicType &&
           ((IBasicType)((IPointerType) e.getExpressionType()).getType()).getKind() == Kind.eVoid;
  }

  private static boolean isRightHandSide(final IASTExpression e) {
    return e.getParent() instanceof IASTBinaryExpression &&
           ((IASTBinaryExpression) e.getParent()).getOperator() == IASTBinaryExpression.op_assign &&
           ((IASTBinaryExpression) e.getParent()).getOperand2() == e;
  }

  private CAstNode convert(IASTCastExpression e) {
    final CExpression operand;
    final FileLocation loc = getLocation(e);
    /* using #typeConverter.convert(e.getExpressionType()); to recheck if our evaluated
     * castType is valid, is wrong in some cases, so we scip this check, and only
     * use our convert(IASTTypeID) method
     * a case where convert(e.getExpressionType()) fails is:
     * struct lock {
     *   unsigned int slock;
     * }
     *
     * int tmp = (*(volatile typeof(lock->slock) *)&(lock->slock));
     *
     * => the convert(IASTTypeId) method returns (unsigned int)*
     * => the convert(CType) method returns (volatile int)*
     * the second one is obviously wrong, because the unsigned is missing
     */
     final CType castType = convert(e.getTypeId());

    // To recognize and simplify constructs e.g. struct s *ps = (struct s *) malloc(.../* e.g. sizeof(struct s)*/);
    if (e.getOperand() instanceof CASTFunctionCallExpression &&
        castType.getCanonicalType() instanceof CPointerType &&
        isRightHandSide(e) &&
        isPointerToVoid(e.getOperand())) {
      return convertExpressionWithSideEffects(e.getOperand());
    } else {
      operand = convertExpressionWithoutSideEffects(e.getOperand());
    }

    if("__imag__".equals(e.getTypeId().getRawSignature())) {
      return new CComplexCastExpression(loc, castType, operand, castType, false);
    } else if ("__real__".equals(e.getTypeId().getRawSignature())) {
      return new CComplexCastExpression(loc, castType, operand, castType, true);
    }

    if (e.getOperand() instanceof IASTFieldReference && ((IASTFieldReference)e.getOperand()).isPointerDereference()) {
      return createInitializedTemporaryVariable(loc, castType, new CCastExpression(loc, castType, operand));
    } else {
      return new CCastExpression(loc, castType, operand);
    }
  }

  private static class ContainsProblemTypeVisitor implements CTypeVisitor<Boolean, RuntimeException> {

    @Override
    public Boolean visit(final CArrayType t) {
      return t.getType().accept(this);
    }

    @Override
    public Boolean visit(final CCompositeType t) {
      return false;
    }

    @Override
    public Boolean visit(final CElaboratedType t) {
      final CType realType = t.getRealType();
      if (realType != null) {
        return realType.accept(this);
      } else {
        return false;
      }
    }

    @Override
    public Boolean visit(final CEnumType t) {
      return false;
    }

    @Override
    public Boolean visit(final CFunctionType t) {
      for (CType parameterType : t.getParameters()) {
        if (parameterType.accept(this)) {
          return true;
        }
      }
      return t.getReturnType().accept(this);
    }

    @Override
    public Boolean visit(final CPointerType t) {
      return t.getType().accept(this);
    }

    @Override
    public Boolean visit(final CProblemType t) {
      return true;
    }

    @Override
    public Boolean visit(final CSimpleType t) {
      return false;
    }

    @Override
    public Boolean visit(CTypedefType t) {
      return t.getRealType().accept(this);
    }
  }

  private static boolean containsProblemType(final CType type) {
    return type.accept(containsProblemTypeVisitor);
  }

  private CFieldReference convert(IASTFieldReference e) {
    CExpression owner = convertExpressionWithoutSideEffects(e.getFieldOwner());
    String fieldName = convert(e.getFieldName());
    final FileLocation loc = getLocation(e);

    if (e.getExpressionType() instanceof ICompositeType) {
      ICompositeType compositeType = (ICompositeType)e.getExpressionType();
      if (compositeType.getName().isEmpty()) {
        // This is an access of a field with an anonymous struct as type.
        // We gave the anonymous struct a real name (c.f. convert(IASTCompositeTypeDeclSpecifier))
        // and now we need a CType as expression type that also has this new
        // name in it. However, if we just call typeConverter.convert(e.getExpressionType())
        // the name will be empty because CDT's IType instance has of course
        // still the empty name (and there is no chance to get to the correct
        // declaration and the new name if you only have the IType instance).
        // So we lookup the correct CType here (where it is easy)
        // and pre-create the mapping from the anonymous IType instance
        // to the non-anonymous CType instance,
        // which the ASTTypeConverter will then just use automatically.

        CType ownerType = owner.getExpressionType().getCanonicalType();
        if (ownerType instanceof CPointerType) {
          ownerType = ((CPointerType) ownerType).getType().getCanonicalType();
        }
        assert ownerType instanceof CCompositeType : "owner of field has no CCompositeType, but is a: " + ownerType.getClass() + " instead.";

        CCompositeTypeMemberDeclaration field = null;
        for (CCompositeTypeMemberDeclaration f : ((CCompositeType)ownerType).getMembers()) {
          if (fieldName.equals(f.getName())) {
            field = f;
            break;
          }
        }

        if (field == null) {
          throw new CFAGenerationRuntimeException("Cannot access field " + fieldName + " in " + ownerType + " in file " + staticVariablePrefix.split("__")[0], e);
        }

        CType fieldType = field.getType();
        typeConverter.registerType(e.getExpressionType(), fieldType);
      }
    }

    // FOLLOWING IF CLAUSE WILL ONLY BE EVALUATED WHEN THE OPTION cfa.simplifyPointerExpressions IS SET TO TRUE
    // if the owner is a FieldReference itself there's the need for a temporary Variable
    // but only if we are not in global scope, otherwise there will be parsing errors
    if (simplifyPointerExpressions && owner instanceof CFieldReference && !scope.isGlobalScope() && (e.isPointerDereference())) {
      owner = createInitializedTemporaryVariable(loc, owner.getExpressionType(), owner);
    }

    CType type = typeConverter.convert(e.getExpressionType());
    if (containsProblemType(type)) {
      CType ownerType = owner.getExpressionType().getCanonicalType();
      if (e.isPointerDereference()) {
        if (!(ownerType instanceof CPointerType)) {
          throw new CFAGenerationRuntimeException("Dereferencing non-pointer type", e);
        }
        ownerType = ((CPointerType)ownerType).getType();
      }
      ownerType = ownerType.getCanonicalType();

      if (ownerType instanceof CCompositeType) {
        CCompositeType compositeType = (CCompositeType)ownerType;
        boolean foundReplacement = false;
        for (CCompositeTypeMemberDeclaration field : compositeType.getMembers()) {
          if (fieldName.equals(field.getName())) {
            logger.log(Level.FINE, "Replacing type", type, "of field reference", e.getRawSignature(),
                "in line", e.getFileLocation().getStartingLineNumber(),
                "with", field.getType());
            type = field.getType();
            foundReplacement = true;
            break;
          }
        }

        // no matching field found
        if (!foundReplacement) {
          throw new CFAGenerationRuntimeException("Accessing non-existent field of composite type", e);
        }
      }

      logger.log(Level.FINE, "Field reference", e.getRawSignature(), "has unknown type", type);
    }

    // FOLLOWING IF CLAUSE WILL ONLY BE EVALUATED WHEN THE OPTION cfa.simplifyPointerExpressions IS SET TO TRUE
    // if there is a "var->field" convert it to (*var).field
    if (simplifyPointerExpressions && e.isPointerDereference()) {
      CType newType = null;
      CType typeDefType = owner.getExpressionType();

      //unpack typedefs
      while (typeDefType instanceof CTypedefType) {
        typeDefType = ((CTypedefType)typeDefType).getRealType();
      }

      if (typeDefType instanceof CPointerType) {
        newType = ((CPointerType)typeDefType).getType();
      } else {
        throw new CFAGenerationRuntimeException("The owner of the struct with field dereference has an invalid type", owner);
      }

      CPointerExpression exp = new CPointerExpression(loc, newType, owner);

      return new CFieldReference(loc, type, fieldName, exp, false);
    }

    return new CFieldReference(loc, type, fieldName, owner, e.isPointerDereference());
  }

  private CRightHandSide convert(IASTFunctionCallExpression e) {

    CExpression functionName = convertExpressionWithoutSideEffects(e.getFunctionNameExpression());
    CFunctionDeclaration declaration = null;

    if (functionName instanceof CIdExpression) {
      if (((CIdExpression) functionName).getName().equals("__builtin_types_compatible_p")) {
        sideAssignmentStack.enterBlock();
        List<CExpression> params = new ArrayList<>();
        for (IASTInitializerClause i : e.getArguments()) {
          params.add(convertExpressionWithoutSideEffects(toExpression(i)));
        }
        sideAssignmentStack.getAndResetConditionalExpressions();
        sideAssignmentStack.getAndResetPostSideAssignments();
        sideAssignmentStack.getAndResetPreSideAssignments();
        sideAssignmentStack.leaveBlock();
        if (params.size() == 2) {
          // TODO this is not completly right considering arrays and perhaps structs
          // http://www.ocf.berkeley.edu/~pad/tigcc/doc/tigcclib/gnuexts_SEC104___builtin_types_compatible_p.html
          if (areCompatibleTypes(params.get(0).getExpressionType(), params.get(1).getExpressionType())) {
            return CNumericTypes.ONE;
          } else {
            return CNumericTypes.ZERO;
          }
        }
      }
    }

    List<CExpression> params = new ArrayList<>();
    for (IASTInitializerClause i : e.getArguments()) {
      params.add(convertExpressionWithoutSideEffects(toExpression(i)));
    }

    if (functionName instanceof CIdExpression) {
      // this function is a gcc extension which checks if the given parameter is
      // a constant value. We can easily provide this functionality by checking
      // if the parameter is a literal expression.
      // We only do check it if the function is not declared.
      if (((CIdExpression) functionName).getName().equals("__builtin_constant_p")
          && params.size() == 1
          && scope.lookupFunction("__builtin_constant_p") == null) {
        if (params.get(0) instanceof CLiteralExpression) {
          return CNumericTypes.ONE;
        } else {
          return CNumericTypes.ZERO;
        }
      }
      CSimpleDeclaration d = ((CIdExpression)functionName).getDeclaration();
      if (d instanceof CFunctionDeclaration) {
        // it may also be a variable declaration, when a function pointer is called
        declaration = (CFunctionDeclaration)d;
      }

      if ((declaration == null)
          && ((CIdExpression)functionName).getName().equals("__builtin_expect")
          && params.size() == 2) {

        // This is the GCC built-in function __builtin_expect(exp, c)
        // that behaves like (exp == c).
        // http://gcc.gnu.org/onlinedocs/gcc/Other-Builtins.html#index-g_t_005f_005fbuiltin_005fexpect-3345

        return binExprBuilder.buildBinaryExpression(params.get(0), params.get(1), BinaryOperator.EQUALS);
      }
    }

    CType returnType = typeConverter.convert(e.getExpressionType());
    if (containsProblemType(returnType)) {
      // workaround for Eclipse CDT problems
      if (declaration != null) {
        returnType = declaration.getType().getReturnType();
        logger.log(Level.FINE, "Replacing return type", returnType, "of function call", e.getRawSignature(),
            "in line", e.getFileLocation().getStartingLineNumber(),
            "with", returnType);
      } else {
        final CType functionType = functionName.getExpressionType().getCanonicalType();
        if (functionType instanceof CFunctionType) {
          returnType = ((CFunctionType) functionType).getReturnType();
          logger.log(Level.FINE, "Replacing return type", returnType, "of function call", e.getRawSignature(),
              "in line", e.getFileLocation().getStartingLineNumber(),
              "with", returnType);
        }
      }
    }

    return new CFunctionCallExpression(getLocation(e), returnType, functionName, params, declaration);
  }

  // TODO this is not completly right considering arrays and perhaps structs
  // http://www.ocf.berkeley.edu/~pad/tigcc/doc/tigcclib/gnuexts_SEC104___builtin_types_compatible_p.html
  private boolean areCompatibleTypes(CType a, CType b) {
    a = a.getCanonicalType(false, false);
    b = b.getCanonicalType(false, false);
    if (a.equals(b) ||
        a instanceof CEnumType && b instanceof CEnumType) {
      return true;
    }
    return false;
  }

  private CIdExpression convert(IASTIdExpression e) {
    String name = convert(e.getName());

    // if this variable is a static variable it is in the scope
    if (scope.lookupVariable(staticVariablePrefix + name) != null ||
        scope.lookupFunction(staticVariablePrefix + name) != null) {
      name = staticVariablePrefix + name;
    }

    // Try to find declaration.
    // Variables per se actually do not bind stronger than function,
    // but local variables do.
    // Furthermore, a global variable and a function with the same name
    // cannot exist, so the following code works correctly.
    CSimpleDeclaration declaration = scope.lookupVariable(name);
    if (declaration == null) {
      declaration = scope.lookupFunction(name);
    }


    // declaration may still be null here,
    // for example when parsing AST patterns for the AutomatonCPA.


    if (declaration != null) {
      name = declaration.getName(); // may have been renamed
    }

    CType type;
    // Use declaration type when possible to fix issues with anonymous composites, problem types etc.
    if (declaration != null) {
      type = declaration.getType();
    } else {
      type = typeConverter.convert(e.getExpressionType());
    }

    if (declaration instanceof CEnumerator
        && type instanceof CElaboratedType
        && ((CElaboratedType)type).getKind() == ComplexTypeKind.ENUM
        && ((CElaboratedType)type).getRealType() == null) {

      // This is a reference to a value of an anonymous enum ("enum { e }").
      // Such types cannot be looked up, and thus the CElaboratedType misses
      // the reference to the enum type.
      CEnumType enumType = ((CEnumerator)declaration).getEnum();
      // enumType is null if an enum value is referenced inside the enum declaration,
      // e.g. like this: "enum { e1, e2 = e1 }"
      if (enumType != null) {
        type = new CElaboratedType(type.isConst(), type.isVolatile(), ComplexTypeKind.ENUM,
            enumType.getName(), enumType);
      }
    }

    return new CIdExpression(getLocation(e), type, name, declaration);
  }

  private CAstNode convert(final IASTUnaryExpression e) {
    final CExpression operand = convertExpressionWithoutSideEffects(e.getOperand());
    final FileLocation fileLoc = getLocation(e);
    CType type = typeConverter.convert(e.getExpressionType());
    final CType operandType = operand.getExpressionType();

    switch (e.getOperator()) {
    case IASTUnaryExpression.op_bracketedPrimary:
    case IASTUnaryExpression.op_plus:
      return operand;

    case IASTUnaryExpression.op_star:

      if (containsProblemType(type)) {
        if (operandType instanceof CPointerType) {
          type = ((CPointerType) operand.getExpressionType()).getType();
        } else if (operandType instanceof CArrayType) {
          type = ((CArrayType) operand.getExpressionType()).getType();
        } else {
          logger.logf(Level.WARNING,
                      "Dereferencing of a non-pointer in expression %s (%s)",
                      e.getRawSignature(),
                      operand.getExpressionType().toString());
        }
      }
      return simplifyUnaryPointerExpression(operand, fileLoc, type);

    case IASTUnaryExpression.op_amper:

      // FOLLOWING IF CLAUSE WILL ONLY BE EVALUATED WHEN THE OPTION cfa.simplifyPointerExpressions IS SET TO TRUE
      // in case of *& both can be left out
      if (simplifyPointerExpressions && operand instanceof CPointerExpression) {
        return ((CPointerExpression)operand).getOperand();
      }

      if (containsProblemType(type)) {
        type = new CPointerType(true, false, operandType);
      }

      // if none of the special cases before fits the default unaryExpression is created
      return new CUnaryExpression(fileLoc, type, operand, UnaryOperator.AMPER);

    case IASTUnaryExpression.op_prefixIncr:
    case IASTUnaryExpression.op_prefixDecr:
      // instead of ++x, create "x = x+1"

      BinaryOperator preOp;
      switch (e.getOperator()) {
      case IASTUnaryExpression.op_prefixIncr:
        preOp = BinaryOperator.PLUS;
        break;
      case IASTUnaryExpression.op_prefixDecr:
        preOp = BinaryOperator.MINUS;
        break;
      default: throw new AssertionError();
      }

      CBinaryExpression preExp = binExprBuilder.buildBinaryExpression(operand, CNumericTypes.ONE, preOp);
      CLeftHandSide lhsPre = (CLeftHandSide) operand;

      return new CExpressionAssignmentStatement(fileLoc, lhsPre, preExp);

    case IASTUnaryExpression.op_postFixIncr:
    case IASTUnaryExpression.op_postFixDecr:
      // instead of x++ create "x = x + 1"

      BinaryOperator postOp;
      switch (e.getOperator()) {
      case IASTUnaryExpression.op_postFixIncr:
        postOp = BinaryOperator.PLUS;
        break;
      case IASTUnaryExpression.op_postFixDecr:
        postOp = BinaryOperator.MINUS;
        break;
      default: throw new AssertionError();
      }

      CBinaryExpression postExp = binExprBuilder.buildBinaryExpression(operand, CNumericTypes.ONE, postOp);
      CLeftHandSide lhsPost = (CLeftHandSide) operand;
      CExpressionAssignmentStatement result = new CExpressionAssignmentStatement(fileLoc, lhsPost, postExp);

      if (e.getParent() instanceof IASTForStatement
          && e.getPropertyInParent() == IASTForStatement.ITERATION) {
        return result;
      }

      CExpression tmp = createInitializedTemporaryVariable(fileLoc, lhsPost.getExpressionType(), lhsPost);
      sideAssignmentStack.addPreSideAssignment(result);

      return tmp;

    case IASTUnaryExpression.op_not:
      return simplifyUnaryNotExpression(operand);

    default:
      return new CUnaryExpression(fileLoc, type, operand, ASTOperatorConverter.convertUnaryOperator(e));
    }
  }

  private static BinaryOperator getNegatedOperator(final BinaryOperator op) {
    switch (op) {
      case EQUALS:
        return BinaryOperator.NOT_EQUALS;
      case NOT_EQUALS:
        return BinaryOperator.EQUALS;
      case LESS_THAN:
        return BinaryOperator.GREATER_EQUAL;
      case LESS_EQUAL:
        return BinaryOperator.GREATER_THAN;
      case GREATER_THAN:
        return BinaryOperator.LESS_EQUAL;
      case GREATER_EQUAL:
        return BinaryOperator.LESS_THAN;
      default:
        throw new AssertionError("operator can not be negated");
    }
  }

  /** returns an expression, that is exactly the negation of the input. */
  private CExpression simplifyUnaryNotExpression(final CExpression expr) {
    // some binary expressions can be directly negated: "!(a==b)" --> "a!=b"
    if (expr instanceof CBinaryExpression) {
      final CBinaryExpression binExpr = (CBinaryExpression)expr;
      if (CBinaryExpressionBuilder.relationalOperators.contains(binExpr.getOperator())) {
        BinaryOperator inverseOperator = getNegatedOperator(binExpr.getOperator());
        return binExprBuilder.buildBinaryExpression(binExpr.getOperand1(), binExpr.getOperand2(), inverseOperator);
      }
    }

    // at this point, we have an expression, that is not directly boolean (!a, !(a+b), !123), so we compare it with Zero.
    // ISO-C 6.5.3.3: Unary arithmetic operators: The expression !E is equivalent to (0==E).
    // TODO do not wrap numerals, replace them directly with the result? This may be done later with SimplificationVisitor.
    return binExprBuilder.buildBinaryExpression(CNumericTypes.ZERO, expr, BinaryOperator.EQUALS);
  }

  /** returns a CPointerExpression, that may be simplified. */
  private CExpression simplifyUnaryPointerExpression(
          final CExpression operand, final FileLocation fileLoc, final CType type) {

    // FOLLOWING IF CLAUSE WILL ONLY BE EVALUATED WHEN THE OPTION cfa.simplifyPointerExpressions IS SET TO TRUE
    if (simplifyPointerExpressions) {

      final CType operandType = operand.getExpressionType();

      // if there is a dereference on a field of a struct a temporary variable is needed
      if (operand instanceof CFieldReference) {
        CIdExpression tmpVar = createInitializedTemporaryVariable(fileLoc, operandType, operand);
        return new CPointerExpression(fileLoc, type, tmpVar);
      }

      // in case of *(a[index])
      else if(operand instanceof CArraySubscriptExpression) {
        CIdExpression tmpVar = createInitializedTemporaryVariable(fileLoc, operandType, operand);
        return new CPointerExpression(fileLoc, type, tmpVar);
      }

      // in case of *& both can be left out
      else if(operand instanceof CUnaryExpression
          && ((CUnaryExpression)operand).getOperator() == UnaryOperator.AMPER) {
        return ((CUnaryExpression)operand).getOperand();
      }

      // in case of ** a temporary variable is needed
      else if(operand instanceof CPointerExpression) {
        CIdExpression tmpVar = createInitializedTemporaryVariable(fileLoc, operandType, operand);
        return new CPointerExpression(fileLoc, type, tmpVar);
      }

      // in case of p.e. *(a+b) or *(a-b) or *(a ANY_OTHER_OPERATOR b) a temporary variable is needed
      else if(operand instanceof CBinaryExpression) {
        CIdExpression tmpVar = createInitializedTemporaryVariable(fileLoc, operandType, operand);
        return new CPointerExpression(fileLoc, type, tmpVar);
      }
    }

    // if none of the special cases before fits the default unaryExpression is created
    return new CPointerExpression(fileLoc, type, operand);
  }

  private CTypeIdExpression convert(IASTTypeIdExpression e) {
    return new CTypeIdExpression(getLocation(e), typeConverter.convert(e.getExpressionType()),
        ASTOperatorConverter.convertTypeIdOperator(e), convert(e.getTypeId()));
  }

  private CTypeIdInitializerExpression convert(IASTTypeIdInitializerExpression e) {
    return new CTypeIdInitializerExpression(getLocation(e), typeConverter.convert(e.getExpressionType()),
        convert(e.getInitializer(), null), convert(e.getTypeId()));
  }

  public CAstNode convert(final IASTStatement s) {

    if (s instanceof IASTExpressionStatement) {
      return convert((IASTExpressionStatement) s);

    } else if (s instanceof IASTReturnStatement) {
      return convert((IASTReturnStatement) s);

    } else if (s instanceof IASTProblemStatement) {
      throw new CFAGenerationRuntimeException((IASTProblemStatement)s);

    } else {
      throw new CFAGenerationRuntimeException("unknown statement: " + s.getClass(), s);
    }
  }

  public CStatement convert(final IASTExpressionStatement s) {
    return convertExpressionToStatement(s.getExpression());
  }

  public CStatement convertExpressionToStatement(final IASTExpression e) {
    CAstNode node = convertExpressionWithSideEffects(e);

    if (node instanceof CExpressionAssignmentStatement) {
      return (CExpressionAssignmentStatement)node;

    } else if (node instanceof CFunctionCallAssignmentStatement) {
      return (CFunctionCallAssignmentStatement)node;

    } else if (node instanceof CFunctionCallExpression) {
      return new CFunctionCallStatement(getLocation(e), (CFunctionCallExpression)node);

    } else if (node instanceof CExpression) {
      return new CExpressionStatement(getLocation(e), (CExpression)node);

    } else {
      throw new AssertionError();
    }
  }

  public CReturnStatement convert(final IASTReturnStatement s) {
    return new CReturnStatement(getLocation(s), convertExpressionWithoutSideEffects(s.getReturnValue()));
  }

  public CFunctionDeclaration convert(final IASTFunctionDefinition f) {
    Pair<CStorageClass, ? extends CType> specifier = convert(f.getDeclSpecifier());

    CStorageClass cStorageClass = specifier.getFirst();
    if (!(cStorageClass == CStorageClass.AUTO
        || cStorageClass == CStorageClass.STATIC
        || cStorageClass == CStorageClass.EXTERN)) {
      // storage class static is the same as auto, just with reduced visibility to a single compilation unit,
      // and as we only handle single compilation units, we can ignore it. A storage class extern associated
      // with a function definition, while superfluous, unless it's an inline function, is allowed, too.
      throw new CFAGenerationRuntimeException("Unsupported storage class for function definition", f);
    }


    Triple<CType, IASTInitializer, String> declarator = convert(f.getDeclarator(), specifier.getSecond());

    if (!(declarator.getFirst() instanceof CFunctionTypeWithNames)) {
      throw new CFAGenerationRuntimeException("Unsupported nested declarator for function definition", f);
    }
    if (declarator.getSecond() != null) {
      throw new CFAGenerationRuntimeException("Unsupported initializer for function definition", f);
    }
    if (declarator.getThird() == null) {
      throw new CFAGenerationRuntimeException("Missing name for function definition", f);
    }

    CFunctionTypeWithNames declSpec = (CFunctionTypeWithNames)declarator.getFirst();
    String name = declarator.getThird();

    if(cStorageClass == CStorageClass.STATIC) {
      name = staticVariablePrefix + name;
      declSpec = new CFunctionTypeWithNames(declSpec.isConst(),
                                            declSpec.isVolatile(),
                                            declSpec.getReturnType(),
                                            declSpec.getParameterDeclarations(),
                                            declSpec.takesVarArgs());
      declSpec.setName(name);
    }

    FileLocation fileLoc = getLocation(f);

    return new CFunctionDeclaration(fileLoc, declSpec, name, declSpec.getParameterDeclarations());
  }

  public List<CDeclaration> convert(final IASTSimpleDeclaration d) {

    FileLocation fileLoc = getLocation(d);

    Pair<CStorageClass, ? extends CType> specifier = convert(d.getDeclSpecifier());
    CStorageClass cStorageClass = specifier.getFirst();
    CType type = specifier.getSecond();

    IASTDeclarator[] declarators = d.getDeclarators();
    List<CDeclaration> result = new ArrayList<>();

    if (type instanceof CCompositeType
        || type instanceof CEnumType) {
      // struct, union, or enum declaration
      // split type definition from eventual variable declaration
      CComplexType complexType = (CComplexType)type;
      CComplexTypeDeclaration newD = new CComplexTypeDeclaration(fileLoc, scope.isGlobalScope(), complexType);
      result.add(newD);

      // now replace type with an elaborated type referencing the new type
      type = new CElaboratedType(type.isConst(), type.isVolatile(), complexType.getKind(), complexType.getName(), newD.getType());

    } else if (type instanceof CElaboratedType) {
      boolean typeAlreadyKnown = scope.lookupType(((CElaboratedType) type).getQualifiedName()) != null;
      boolean variableDeclaration = declarators != null && declarators.length > 0;
      if (!typeAlreadyKnown || !variableDeclaration) {
        CComplexTypeDeclaration newD = new CComplexTypeDeclaration(fileLoc, scope.isGlobalScope(), (CElaboratedType)type);
        result.add(newD);
      }
    }

    if (declarators != null) {
      for (IASTDeclarator c : declarators) {

        result.add(createDeclaration(fileLoc, cStorageClass, type, c));
      }
    }

    return result;
  }

  private CDeclaration createDeclaration(FileLocation fileLoc, CStorageClass cStorageClass, CType type, IASTDeclarator d) {
    boolean isGlobal = scope.isGlobalScope();

    if (d != null) {
      Triple<CType, IASTInitializer, String> declarator = convert(d, type);

      type = declarator.getFirst();

      IASTInitializer initializer = declarator.getSecond();

      String name = declarator.getThird();

      if (name == null) {
        throw new CFAGenerationRuntimeException("Declaration without name", d);
      }

      // first handle all special cases

      if (cStorageClass == CStorageClass.TYPEDEF) {
        if (initializer != null) {
          throw new CFAGenerationRuntimeException("Typedef with initializer", d);
        }
        return new CTypeDefDeclaration(fileLoc, isGlobal, type, name, scope.createScopedNameOf(name));
      }

      if (type instanceof CFunctionTypeWithNames) {
        if (initializer != null) {
          throw new CFAGenerationRuntimeException("Function definition with initializer", d);
        }

        CFunctionTypeWithNames functionType = (CFunctionTypeWithNames)type;
        return new CFunctionDeclaration(fileLoc, functionType, name, functionType.getParameterDeclarations());
      }

      // now it should be a regular variable declaration

      if (cStorageClass == CStorageClass.EXTERN && initializer != null) {
        throw new CFAGenerationRuntimeException("Extern declarations cannot have initializers", d);
      }

      String origName = name;

      if (cStorageClass == CStorageClass.STATIC) {
        if (!isGlobal) {
          isGlobal = true;
          name = "static__" + ((FunctionScope)scope).getCurrentFunctionName() + "__" + name;
        } else {
          name = staticVariablePrefix + name;
        }
        cStorageClass = CStorageClass.AUTO;
      }

      if (!isGlobal && scope.variableNameInUse(name, name)) {
        String sep = "__";
        int index = 1;
        while (scope.variableNameInUse(name + sep + index, origName)) {
          ++index;
        }
        name = name + sep + index;
      }

      CVariableDeclaration declaration = new CVariableDeclaration(fileLoc,
          isGlobal, cStorageClass, type, name, origName,
          scope.createScopedNameOf(name), null);
      scope.registerDeclaration(declaration);

      // Now that we registered the declaration, we can parse the initializer.
      // We cannot do this before, because in the following code, the right "x"
      // actually binds to the left "x"!
      // int x = x;

      declaration.addInitializer(convert(initializer, declaration));

      return declaration;

    } else {
      throw new CFAGenerationRuntimeException("Declaration without declarator, but type is unknown: " + type.toASTString(""));
    }

  }

  private List<CCompositeTypeMemberDeclaration> convertDeclarationInCompositeType(final IASTDeclaration d) {
    if (d instanceof IASTProblemDeclaration) {
      throw new CFAGenerationRuntimeException((IASTProblemDeclaration)d);
    }

    if (!(d instanceof IASTSimpleDeclaration)) {
      throw new CFAGenerationRuntimeException("unknown declaration type " + d.getClass().getSimpleName(), d);
    }
    IASTSimpleDeclaration sd = (IASTSimpleDeclaration)d;

    Pair<CStorageClass, ? extends CType> specifier = convert(sd.getDeclSpecifier());
    if (specifier.getFirst() != CStorageClass.AUTO) {
      throw new CFAGenerationRuntimeException("Unsupported storage class inside composite type", d);
    }
    CType type = specifier.getSecond();

    if (type instanceof CCompositeType) {
      // Nested struct declaration
      CCompositeType compositeType = (CCompositeType)type;
      addSideEffectDeclarationForType(compositeType, getLocation(d));
      type = new CElaboratedType(compositeType.isConst(), compositeType.isVolatile(),
          compositeType.getKind(), compositeType.getName(), compositeType);
    }

    List<CCompositeTypeMemberDeclaration> result;
    IASTDeclarator[] declarators = sd.getDeclarators();
    if (declarators == null || declarators.length == 0) {
      // declaration without declarator, anonymous struct field?
      CCompositeTypeMemberDeclaration newD = createDeclarationForCompositeType(type, null);
      result = Collections.singletonList(newD);

    } else if (declarators.length == 1) {
      CCompositeTypeMemberDeclaration newD = createDeclarationForCompositeType(type, declarators[0]);
      result = Collections.singletonList(newD);

    } else {
      result = new ArrayList<>(declarators.length);
      for (IASTDeclarator c : declarators) {

        result.add(createDeclarationForCompositeType(type, c));
      }
    }

    return result;
  }

  private CCompositeTypeMemberDeclaration createDeclarationForCompositeType(CType type, IASTDeclarator d) {
    String name = null;

    if (d != null) {
      Triple<CType, IASTInitializer, String> declarator = convert(d, type);


      if (declarator.getSecond() != null) {
        throw new CFAGenerationRuntimeException("Unsupported initializer inside composite type", d);
      }

      type = declarator.getFirst();
      name = declarator.getThird();
    }

    if (name == null) {
      name = "__anon_type_member_" + anonTypeMemberCounter++;
    }

    return new CCompositeTypeMemberDeclaration(type, name);
  }


  private Triple<CType, IASTInitializer, String> convert(IASTDeclarator d, CType specifier) {

    if (d instanceof IASTFunctionDeclarator) {
      return convert((IASTFunctionDeclarator)d, specifier);

    } else {
      // Parsing type declarations in C is complex.
      // For example, array modifiers and pointer operators are declared in the
      // "wrong" way:
      // "int (*drives[4])[6]" is "array 4 of pointer to array 6 of int"
      // (The inner most modifiers are the highest-level ones.)
      // So we don't do this recursively, but instead collect all modifiers
      // and apply them after we have reached the inner-most declarator.

      // Collection of all modifiers (outermost modifier is first).
      List<IASTNode> modifiers = Lists.newArrayListWithExpectedSize(1);


      IASTInitializer initializer = null;
      String name = null;

      // Descend into the nested chain of declators.
      // Find out the name and the initializer, and collect all modifiers.
      IASTDeclarator currentDecl = d;
      while (currentDecl != null) {
        // TODO handle bitfields by checking for instanceof IASTFieldDeclarator

        if (currentDecl instanceof IASTFunctionDeclarator) {
          throw new CFAGenerationRuntimeException("Unsupported declaration nested function declarations", d);
        }

        modifiers.addAll(Arrays.asList(currentDecl.getPointerOperators()));

        if (currentDecl instanceof IASTArrayDeclarator) {
          modifiers.addAll(Arrays.asList(((IASTArrayDeclarator) currentDecl).getArrayModifiers()));
        }

        if (currentDecl.getInitializer() != null) {
          if (initializer != null) {
            throw new CFAGenerationRuntimeException("Unsupported declaration with two initializers", d);
          }
          //xxx
          initializer = currentDecl.getInitializer();
        }

        if (!currentDecl.getName().toString().isEmpty()) {
          if (name != null) {
            throw new CFAGenerationRuntimeException("Unsupported declaration with two names", d);
          }
          name = convert(currentDecl.getName());
        }

        currentDecl = currentDecl.getNestedDeclarator();
      }

      name = Strings.nullToEmpty(name); // there may be no name at all, for example in parameter declarations

      // Add the modifiers to the type.
      CType type = specifier;
      //array modifiers have to be added backwards, otherwise the arraysize is wrong
      // with multidimensional arrays
      List<IASTArrayModifier> tmpArrMod = Lists.newArrayListWithExpectedSize(1);
      for (IASTNode modifier : modifiers) {
        if (modifier instanceof IASTArrayModifier) {
          tmpArrMod.add((IASTArrayModifier) modifier);
        } else if (modifier instanceof IASTPointerOperator) {
          //add accumulated array modifiers before adding next pointer operator
          for (int i = tmpArrMod.size() -1; i >= 0; i--) {
            type = convert(tmpArrMod.get(i), type);
          }
          // clear added modifiers
          tmpArrMod.clear();

          type = typeConverter.convert((IASTPointerOperator)modifier, type);

        } else {
          assert false;
        }
      }

      // add last array modifiers if necessary
      for (int i = tmpArrMod.size() -1; i >= 0; i--) {
        type = convert(tmpArrMod.get(i), type);
      }

      return Triple.of(type, initializer, name);
    }
  }

  private CType convert(IASTArrayModifier am, CType type) {
    if (am instanceof org.eclipse.cdt.core.dom.ast.c.ICASTArrayModifier) {
      org.eclipse.cdt.core.dom.ast.c.ICASTArrayModifier a = (org.eclipse.cdt.core.dom.ast.c.ICASTArrayModifier)am;
      CExpression lengthExp = convertExpressionWithoutSideEffects(a.getConstantExpression());
      if (lengthExp != null) {
        lengthExp = simplifyExpressionRecursively(lengthExp);
      }
      return new CArrayType(a.isConst(), a.isVolatile(), type, lengthExp);

    } else {
      throw new CFAGenerationRuntimeException("Unknown array modifier", am);
    }
  }

  private Triple<CType, IASTInitializer, String> convert(IASTFunctionDeclarator d, CType returnType) {

    if (!(d instanceof IASTStandardFunctionDeclarator)) {
      throw new CFAGenerationRuntimeException("Unknown non-standard function definition", d);
    }
    IASTStandardFunctionDeclarator sd = (IASTStandardFunctionDeclarator)d;

    // handle return type
    returnType = typeConverter.convertPointerOperators(d.getPointerOperators(), returnType);
    if (returnType instanceof CSimpleType) {
      CSimpleType t = (CSimpleType)returnType;
      if (t.getType() == CBasicType.UNSPECIFIED) {
        // type of functions is implicitly int it not specified
        returnType = new CSimpleType(t.isConst(), t.isVolatile(), CBasicType.INT,
            t.isLong(), t.isShort(), t.isSigned(), t.isUnsigned(), t.isComplex(),
            t.isImaginary(), t.isLongLong());
      }
    }


    // handle parameters
    List<CParameterDeclaration> paramsList = convert(sd.getParameters());

    // TODO constant and volatile
    CFunctionTypeWithNames fType = new CFunctionTypeWithNames(false, false, returnType, paramsList, sd.takesVarArgs());
    CType type = fType;

    String name;
    if (d.getNestedDeclarator() != null) {

      Triple<? extends CType, IASTInitializer, String> nestedDeclarator = convert(d.getNestedDeclarator(), type);


      assert d.getName().getRawSignature().isEmpty() : d;
      assert nestedDeclarator.getSecond() == null;

      type = nestedDeclarator.getFirst();
      name = nestedDeclarator.getThird();

    } else {
      name = convert(d.getName());
    }

    fType.setName(name);
    for (CParameterDeclaration param : paramsList) {
      param.setQualifiedName(FunctionScope.createQualifiedName(name, param.getName()));
    }

    return Triple.of(type, d.getInitializer(), name);
  }


  private Pair<CStorageClass, ? extends CType> convert(IASTDeclSpecifier d) {
    CStorageClass sc = typeConverter.convertCStorageClass(d);

    if (d instanceof IASTCompositeTypeSpecifier) {
      return Pair.of(sc, convert((IASTCompositeTypeSpecifier)d));

    } else if (d instanceof IASTElaboratedTypeSpecifier) {
      return Pair.of(sc, typeConverter.convert((IASTElaboratedTypeSpecifier)d));

    } else if (d instanceof IASTEnumerationSpecifier) {
      return Pair.of(sc, convert((IASTEnumerationSpecifier)d));

    } else if (d instanceof IASTNamedTypeSpecifier) {
      return Pair.of(sc, typeConverter.convert((IASTNamedTypeSpecifier)d));

    } else if (d instanceof IASTSimpleDeclSpecifier) {
      return Pair.of(sc, typeConverter.convert((IASTSimpleDeclSpecifier)d));

    } else {
      throw new CFAGenerationRuntimeException("unknown declSpecifier", d);
    }
  }

  private CCompositeType convert(IASTCompositeTypeSpecifier d) {
    List<CCompositeTypeMemberDeclaration> list = new ArrayList<>(d.getMembers().length);

    for (IASTDeclaration c : d.getMembers()) {
      List<CCompositeTypeMemberDeclaration> newCs = convertDeclarationInCompositeType(c);
      assert !newCs.isEmpty();
      list.addAll(newCs);
    }

    ComplexTypeKind kind;
    switch (d.getKey()) {
    case IASTCompositeTypeSpecifier.k_struct:
      kind = ComplexTypeKind.STRUCT;
      break;
    case IASTCompositeTypeSpecifier.k_union:
      kind = ComplexTypeKind.UNION;
    break;
    default:
      throw new CFAGenerationRuntimeException("Unknown key " + d.getKey() + " for composite type", d);
    }

    String name = convert(d.getName());
    if (Strings.isNullOrEmpty(name)) {
      name = "__anon_type_" + anonTypeCounter++;
    }

    CCompositeType compositeType = new CCompositeType(d.isConst(), d.isVolatile(), kind, list, name);

    // in cases like struct s { (struct s)* f }
    // we need to fill in the binding from the inner "struct s" type to the outer
    compositeType.accept(new FillInBindingVisitor(kind, name, compositeType));
    return compositeType;
  }

  private CEnumType convert(IASTEnumerationSpecifier d) {
    List<CEnumerator> list = new ArrayList<>(d.getEnumerators().length);
    Long lastValue = -1L; // initialize with -1, so the first one gets value 0
    for (IASTEnumerationSpecifier.IASTEnumerator c : d.getEnumerators()) {
      CEnumerator newC = convert(c, lastValue);
      list.add(newC);
      if (newC.hasValue()) {
        lastValue = newC.getValue();
      } else {
        lastValue = null;
      }
    }


    String name = convert(d.getName());

    CEnumType enumType = new CEnumType(d.isConst(), d.isVolatile(), list, name);
    for (CEnumerator enumValue : enumType.getEnumerators()) {
      enumValue.setEnum(enumType);
    }
    return enumType;
  }

  private CEnumerator convert(IASTEnumerationSpecifier.IASTEnumerator e, Long lastValue) {
    Long value = null;

    if (e.getValue() == null && lastValue != null) {
      value = lastValue + 1;
    } else {
      CExpression v = convertExpressionWithoutSideEffects(e.getValue());
      boolean negate = false;
      boolean complement = false;

      if (v instanceof CUnaryExpression && ((CUnaryExpression) v).getOperator() == UnaryOperator.MINUS) {
        CUnaryExpression u = (CUnaryExpression)v;
        negate = true;
        v = u.getOperand();
      } else if (v instanceof CUnaryExpression && ((CUnaryExpression) v).getOperator() == UnaryOperator.TILDE) {
        CUnaryExpression u = (CUnaryExpression)v;
        complement = true;
        v = u.getOperand();
      }
      assert !(v instanceof CUnaryExpression) : v;

      if (v instanceof CIntegerLiteralExpression) {
        value = ((CIntegerLiteralExpression)v).asLong();
        if (negate) {
          value = -value;
        } else if(complement) {
          value = ~value;
        }
      } else {
        // ignoring unsupported enum value
        // TODO Warning
      }
    }

    String name = convert(e.getName());
    CEnumerator result = new CEnumerator(getLocation(e), name, scope.createScopedNameOf(name), value);
    scope.registerDeclaration(result);
    return result;
  }

  private IASTExpression toExpression(IASTInitializerClause i) {
    if (i instanceof IASTExpression) {
      return (IASTExpression)i;
    }
    throw new CFAGenerationRuntimeException("Initializer clause in unexpected location", i);
  }

  private CInitializer convert(IASTInitializerClause i, @Nullable CVariableDeclaration declaration) {
    if (i instanceof IASTExpression) {
      CExpression exp = convertExpressionWithoutSideEffects((IASTExpression)i);
      return new CInitializerExpression(exp.getFileLocation(), exp);
    } else if (i instanceof IASTInitializerList) {
      return convert((IASTInitializerList)i, declaration);
    } else if (i instanceof ICASTDesignatedInitializer) {
      return convert((ICASTDesignatedInitializer)i, declaration);
    } else {
      throw new CFAGenerationRuntimeException("unknown initializer claus: " + i.getClass().getSimpleName(), i);
    }
  }

  private CInitializer convert(IASTInitializer i, @Nullable CVariableDeclaration declaration) {
    if (i == null) {
      return null;

    } else if (i instanceof IASTInitializerList) {
      return convert((IASTInitializerList)i, declaration);
    } else if (i instanceof IASTEqualsInitializer) {
      return convert((IASTEqualsInitializer)i, declaration);
    } else if (i instanceof org.eclipse.cdt.core.dom.ast.c.ICASTDesignatedInitializer) {
      return convert((org.eclipse.cdt.core.dom.ast.c.ICASTDesignatedInitializer)i, declaration);
    } else {
      throw new CFAGenerationRuntimeException("unknown initializer: " + i.getClass().getSimpleName(), i);
    }
  }

  private CInitializer convert(org.eclipse.cdt.core.dom.ast.c.ICASTDesignatedInitializer init, @Nullable CVariableDeclaration declaration) {
    ICASTDesignator[] desInit = init.getDesignators();

    CInitializer cInit = convert(init.getOperand(), declaration);

    FileLocation fileLoc = cInit.getFileLocation();

    List<CDesignator> designators = new ArrayList<>(desInit.length);

    // convert all designators
    for (ICASTDesignator designator : desInit) {
      CDesignator r;
      if (designator instanceof ICASTFieldDesignator) {
       r = new CFieldDesignator(fileLoc,
           convert(((ICASTFieldDesignator) designator).getName()));

      } else if (designator instanceof ICASTArrayDesignator) {
        r = new CArrayDesignator(fileLoc,
            convertExpressionWithoutSideEffects(((ICASTArrayDesignator) designator).getSubscriptExpression()));

      } else if (designator instanceof IGCCASTArrayRangeDesignator) {
        r = new CArrayRangeDesignator(fileLoc,
            convertExpressionWithoutSideEffects(((IGCCASTArrayRangeDesignator) designator).getRangeFloor()),
            convertExpressionWithoutSideEffects(((IGCCASTArrayRangeDesignator) designator).getRangeCeiling()));

      } else {
        throw new CFAGenerationRuntimeException("Unsupported Designator", designator);
      }
      designators.add(r);
    }

    return new CDesignatedInitializer(fileLoc, designators, cInit);
  }

  private CInitializerList convert(IASTInitializerList iList, @Nullable CVariableDeclaration declaration) {
    List<CInitializer> initializerList = new ArrayList<>();
    for (IASTInitializerClause i : iList.getClauses()) {
      CInitializer newI = convert(i, declaration);
      if (newI != null) {
        initializerList.add(newI);
      }
    }

    return new CInitializerList(getLocation(iList), initializerList);
  }

  private CInitializer convert(IASTEqualsInitializer i, @Nullable CVariableDeclaration declaration) {
    IASTInitializerClause ic = i.getInitializerClause();
    if (ic instanceof IASTExpression) {
      IASTExpression e = (IASTExpression)ic;

      CAstNode initializer = convertExpressionWithSideEffects(e);
      if (initializer == null) {
        return null;
      }

      if (initializer instanceof CAssignment) {
        sideAssignmentStack.addPreSideAssignment(initializer);
        return new CInitializerExpression(getLocation(e), ((CAssignment)initializer).getLeftHandSide());

      } else if (initializer instanceof CFunctionCallExpression) {
        FileLocation loc = getLocation(i);

        if (declaration != null) {
          // This is a variable declaration like "int i = f();"
          // We can replace this with "int i; i = f();"
          CIdExpression var = new CIdExpression(loc, declaration);
          sideAssignmentStack.addPostSideAssignment(new CFunctionCallAssignmentStatement(loc, var,
                                  (CFunctionCallExpression) initializer));
          return null; // empty initializer

        } else {
          // This is something more complicated, like a function call inside an array initializer.
          // We need a temporary variable.

          CIdExpression var = createTemporaryVariable(e);
          sideAssignmentStack.addPreSideAssignment(new CFunctionCallAssignmentStatement(loc, var,
                                 (CFunctionCallExpression) initializer));
          return new CInitializerExpression(loc, var);
        }
      }

      if (!(initializer instanceof CExpression)) {
        throw new CFAGenerationRuntimeException("Initializer is not free of side-effects, it is a " + initializer.getClass().getSimpleName(), e);
      }

      return new CInitializerExpression(getLocation(ic), (CExpression)initializer);

    } else if (ic instanceof IASTInitializerList) {
      return convert((IASTInitializerList)ic, declaration);
    } else {
      throw new CFAGenerationRuntimeException("unknown initializer: " + i.getClass().getSimpleName(), i);
    }
  }

  private List<CParameterDeclaration> convert(IASTParameterDeclaration[] ps) {
    List<CParameterDeclaration> paramsList = new ArrayList<>(ps.length);
    for (IASTParameterDeclaration c : ps) {
      if (!c.getRawSignature().equals("void")) {
        paramsList.add(convert(c));
      } else {
        // there may be a function declaration f(void), which is equal to f()
        // we don't want this dummy parameter "void"
        assert ps.length == 1;
      }
    }
    return paramsList;
  }

  private CParameterDeclaration convert(IASTParameterDeclaration p) {
    Pair<CStorageClass, ? extends CType> specifier = convert(p.getDeclSpecifier());
    if (specifier.getFirst() != CStorageClass.AUTO) {
      throw new CFAGenerationRuntimeException("Unsupported storage class for parameters", p);
    }


    Triple<CType, IASTInitializer, String> declarator = convert(p.getDeclarator(), specifier.getSecond());

    if (declarator.getSecond() != null) {
      throw new CFAGenerationRuntimeException("Unsupported initializer for parameters", p);
    }

    CType type = declarator.getFirst();
    if (type instanceof CFunctionTypeWithNames) {
      CFunctionTypeWithNames functionType = (CFunctionTypeWithNames) type;
      type = new CPointerType(false, false, functionType);
    }

    return new CParameterDeclaration(getLocation(p), type, declarator.getThird());
  }


  /** This function returns the converted file-location of an IASTNode. */
  FileLocation getLocation(final IASTNode n) {
    return getLocation(n.getFileLocation());
  }

  FileLocation getLocation(IASTFileLocation l) {
    if (l == null) {
      return null;
    }

    String fileName = l.getFileName();
    int startingLineInInput = l.getStartingLineNumber();
    int startingLineInOrigin = startingLineInInput;

    Pair<String, Integer> startingInOrigin = sourceOriginMapping.getOriginLineFromAnalysisCodeLine(
        fileName, startingLineInInput);

    fileName = startingInOrigin.getFirst();
    startingLineInOrigin = startingInOrigin.getSecond();

    return new FileLocation(l.getEndingLineNumber(), fileName,
        niceFileNameFunction.apply(fileName),
        l.getNodeLength(), l.getNodeOffset(),
        startingLineInInput, startingLineInOrigin);
  }

  static String convert(IASTName n) {
    return n.toString(); // TODO verify toString() is the correct method
  }

  private CType convert(IASTTypeId t) {
    Pair<CStorageClass, ? extends CType> specifier = convert(t.getDeclSpecifier());
    if (specifier.getFirst() != CStorageClass.AUTO) {
      throw new CFAGenerationRuntimeException("Unsupported storage class for type ids", t);
    }

    Triple<CType, IASTInitializer, String> declarator = convert(t.getAbstractDeclarator(), specifier.getSecond());

    if (declarator.getSecond() != null) {
      throw new CFAGenerationRuntimeException("Unsupported initializer for type ids", t);
    }
    if (declarator.getThird() != null && !declarator.getThird().trim().isEmpty()) {
      throw new CFAGenerationRuntimeException("Unsupported name for type ids", t);
    }

    return declarator.getFirst();
  }
}