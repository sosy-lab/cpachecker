/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

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
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
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
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTCompoundStatementExpression;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@SuppressWarnings("deprecation") // several methods are deprecated in CDT 7 but still working
class ASTConverter {

  private final LogManager logger;

  private final Scope scope;

  private final List<CAstNode> preSideAssignments = new ArrayList<>();
  private final List<CAstNode> postSideAssignments = new ArrayList<>();

  // the following 2 fields store whether a ternary operator
  // or a shortcutting operator have been encountered
  private IASTExpression conditionalExpression = null;
  private CIdExpression conditionalTemporaryVariable = null;

  public ASTConverter(Scope pScope, LogManager pLogger) {
    scope = pScope;
    logger = pLogger;
  }

  public List<CAstNode> getAndResetPreSideAssignments() {
    List<CAstNode> result = new ArrayList<>(preSideAssignments);
    preSideAssignments.clear();
    return result;
  }

  public List<CAstNode> getAndResetPostSideAssignments() {
    List<CAstNode> result = new ArrayList<>(postSideAssignments);
    postSideAssignments.clear();
    return result;
  }

  public void resetConditionalExpression() {
    conditionalExpression = null;
  }

  public boolean hasConditionalExpression() {
    return conditionalExpression != null;
  }

  public IASTExpression getAndResetConditionalExpression() {
    IASTExpression result = conditionalExpression;
    conditionalExpression = null;
    return result;
  }

  public CIdExpression getConditionalTemporaryVariable() {
    return conditionalTemporaryVariable;
  }

  public CExpression convertExpressionWithoutSideEffects(
      IASTExpression e) {

    CAstNode node = convertExpressionWithSideEffects(e);
    if (node == null || node instanceof CExpression) {
      return (CExpression) node;

    } else if (node instanceof CFunctionCallExpression) {
      return addSideassignmentsForExpressionsWithoutSideEffects(node, e);

    } else if(e instanceof IASTUnaryExpression && (((IASTUnaryExpression)e).getOperator() == IASTUnaryExpression.op_postFixDecr
                                                   || ((IASTUnaryExpression)e).getOperator() == IASTUnaryExpression.op_postFixIncr)) {
      return addSideAssignmentsForUnaryExpressions(e, ((CAssignment)node).getLeftHandSide(),
          node.getFileLocation(), ASTTypeConverter.convert(e.getExpressionType()),
          ((CBinaryExpression)((CAssignment)node).getRightHandSide()).getOperator());

    } else if (node instanceof CAssignment) {
      preSideAssignments.add(node);
      return ((CAssignment) node).getLeftHandSide();

    } else {
      throw new AssertionError("unknown expression " + node);
    }
  }

  private CExpression addSideassignmentsForExpressionsWithoutSideEffects(CAstNode node,
                                                                            IASTExpression e){
    CIdExpression tmp = createTemporaryVariable(e, null);

    preSideAssignments.add(new CFunctionCallAssignmentStatement(getLocation(e),
                                                                tmp,
                                                                (CFunctionCallExpression) node));
    return tmp;
  }

  private CIdExpression addSideAssignmentsForUnaryExpressions(IASTExpression e,
                                                              CExpression exp,
                                                              FileLocation fileLoc,
                                                              CType type,
                                                              BinaryOperator op) {
    CIdExpression tmp = createTemporaryVariable(e, null);
    preSideAssignments.add(new CExpressionAssignmentStatement(fileLoc, tmp, exp));


    CExpression one = new CIntegerLiteralExpression(fileLoc, type, BigInteger.ONE);
    CBinaryExpression postExp = new CBinaryExpression(fileLoc, type, exp, one, op);
    preSideAssignments.add(new CExpressionAssignmentStatement(fileLoc, exp, postExp));

    return tmp;
}

  protected CAstNode convertExpressionWithSideEffects(IASTExpression e) {
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
      return convert((IASTIdExpression)e);

    } else if (e instanceof IASTLiteralExpression) {
      return ASTLiteralConverter.convert((IASTLiteralExpression)e);

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

  private CAstNode convert(IASTConditionalExpression e) {
    CIdExpression tmp = createTemporaryVariable(e, null);
    conditionalTemporaryVariable = tmp;
    conditionalExpression = e;
    return tmp;
  }

  private CAstNode convert(IGNUASTCompoundStatementExpression e) {
    CIdExpression tmp = createTemporaryVariable(e, null);
    conditionalTemporaryVariable = tmp;
    conditionalExpression = e;
    return tmp;
  }

  private CAstNode convertExpressionListAsExpression(IASTExpressionList e) {
    CIdExpression tmp = createTemporaryVariable(e, null);
    conditionalTemporaryVariable = tmp;
    conditionalExpression = e;
    return tmp;
  }

  private CArraySubscriptExpression convert(IASTArraySubscriptExpression e) {
    return new CArraySubscriptExpression(getLocation(e),
        ASTTypeConverter.convert(e.getExpressionType()),
        convertExpressionWithoutSideEffects(e.getArrayExpression()),
        convertExpressionWithoutSideEffects(e.getSubscriptExpression()));
  }

  /**
   * creates temporary variables with increasing numbers
   */
  private CIdExpression createTemporaryVariable(IASTExpression e, String name) {
    boolean nameWasInUse = true;
    if (name == null) {
      nameWasInUse = false;
      name = "__CPAchecker_TMP_";
      int i = 0;
      while (scope.variableNameInUse(name + i, name + i)) {
        i++;
      }
      name += i;
    }

    CVariableDeclaration decl = new CVariableDeclaration(getLocation(e),
                                               false,
                                               CStorageClass.AUTO,
                                               ASTTypeConverter.convert(e.getExpressionType()),
                                               name,
                                               name,
                                               null);

    if (!nameWasInUse) {
    scope.registerDeclaration(decl);
    preSideAssignments.add(decl);
    }
    CIdExpression tmp = new CIdExpression(getLocation(e),
                                                ASTTypeConverter.convert(e.getExpressionType()),
                                                name,
                                                decl);
    return tmp;
  }

  private CAstNode convert(IASTBinaryExpression e) {

    switch (e.getOperator()) {
    case IASTBinaryExpression.op_logicalAnd:
    case IASTBinaryExpression.op_logicalOr:
      CIdExpression tmp = createTemporaryVariable(e, null);
      conditionalTemporaryVariable = tmp;
      conditionalExpression = e;
      return tmp;
    }

    Pair<BinaryOperator, Boolean> opPair = ASTOperatorConverter.convertBinaryOperator(e);
    BinaryOperator op = opPair.getFirst();
    boolean isAssign = opPair.getSecond();

    FileLocation fileLoc = getLocation(e);
    CType type = ASTTypeConverter.convert(e.getExpressionType());
    CExpression leftHandSide = convertExpressionWithoutSideEffects(e.getOperand1());

    if (isAssign) {

      if (op == null) {
        // a = b
        CAstNode rightHandSide = convertExpressionWithSideEffects(e.getOperand2()); // right-hand side may have a function call


        if (rightHandSide instanceof CExpression) {
          // a = b
          return new CExpressionAssignmentStatement(fileLoc, leftHandSide, (CExpression)rightHandSide);

        } else if (rightHandSide instanceof CFunctionCallExpression) {
          // a = f()
          return new CFunctionCallAssignmentStatement(fileLoc, leftHandSide, (CFunctionCallExpression)rightHandSide);

        } else if (rightHandSide instanceof CAssignment) {
          preSideAssignments.add(rightHandSide);
          return new CExpressionAssignmentStatement(fileLoc, leftHandSide, ((CAssignment) rightHandSide).getLeftHandSide());
        } else {
          throw new CFAGenerationRuntimeException("Expression is not free of side-effects", e);
        }

      } else {
        // a += b etc.
        CExpression rightHandSide = convertExpressionWithoutSideEffects(e.getOperand2());

        // first create expression "a + b"
        CBinaryExpression exp = new CBinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);

        // and now the assignment
        return new CExpressionAssignmentStatement(fileLoc, leftHandSide, exp);
      }

    } else {
      CExpression rightHandSide = convertExpressionWithoutSideEffects(e.getOperand2());
      return new CBinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);
    }
  }

  private CAstNode convert(IASTCastExpression e) {
    return new CCastExpression(getLocation(e), ASTTypeConverter.convert(e.getExpressionType()), convertExpressionWithoutSideEffects(e.getOperand()), convert(e.getTypeId()));
  }

  private CFieldReference convert(IASTFieldReference e) {
    return new CFieldReference(getLocation(e), ASTTypeConverter.convert(e.getExpressionType()), convert(e.getFieldName()), convertExpressionWithoutSideEffects(e.getFieldOwner()), e.isPointerDereference());
  }

  private CFunctionCallExpression convert(IASTFunctionCallExpression e) {
    IASTExpression p = e.getParameterExpression();

    List<CExpression> params;
    if (p instanceof IASTExpressionList) {
      params = convert((IASTExpressionList)p);

    } else {
      params = new ArrayList<>();
      if (p != null) {
        params.add(convertExpressionWithoutSideEffects(p));
      }
    }

    CExpression functionName = convertExpressionWithoutSideEffects(e.getFunctionNameExpression());
    CFunctionDeclaration declaration = null;

    if (functionName instanceof CIdExpression) {
      CSimpleDeclaration d = ((CIdExpression)functionName).getDeclaration();
      if (d instanceof CFunctionDeclaration) {
        // it may also be a variable declaration, when a function pointer is called
        declaration = (CFunctionDeclaration)d;
      }
    }

    return new CFunctionCallExpression(getLocation(e), ASTTypeConverter.convert(e.getExpressionType()), functionName, params, declaration);
  }

  private List<CExpression> convert(IASTExpressionList es) {
    List<CExpression> result = new ArrayList<>(es.getExpressions().length);
    for (IASTExpression expression : es.getExpressions()) {
      result.add(convertExpressionWithoutSideEffects(expression));
    }
    return result;
  }

  private CIdExpression convert(IASTIdExpression e) {
    String name = convert(e.getName());

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
    return new CIdExpression(getLocation(e), ASTTypeConverter.convert(e.getExpressionType()), name, declaration);
  }

  private CAstNode convert(IASTUnaryExpression e) {
    CExpression operand = convertExpressionWithoutSideEffects(e.getOperand());

    if (e.getOperator() == IASTUnaryExpression.op_bracketedPrimary) {
      return operand;
    }


    FileLocation fileLoc = getLocation(e);
    CType type = ASTTypeConverter.convert(e.getExpressionType());



    switch (e.getOperator()) {
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
      CExpression one = new CIntegerLiteralExpression(fileLoc, type, BigInteger.ONE);
      CBinaryExpression preExp = new CBinaryExpression(fileLoc, type, operand, one, preOp);

      return new CExpressionAssignmentStatement(fileLoc, operand, preExp);

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

      CExpression postOne = new CIntegerLiteralExpression(fileLoc, type, BigInteger.ONE);
      CBinaryExpression postExp = new CBinaryExpression(fileLoc, type, operand, postOne, postOp);
      return new CExpressionAssignmentStatement(fileLoc, operand, postExp);

    case IASTUnaryExpression.op_sizeof:
      // There is a bug in org.eclipse.cdt.internal.core.dom.parser.c.CUnaryExpression.
      // For sizeof, it returns the operand type instead of int.
      type = new CSimpleType(false, false, CBasicType.INT, false, false, false, false, false, false, false);

      //$FALL-THROUGH$
    default:
      return new CUnaryExpression(fileLoc, type, operand, ASTOperatorConverter.convertUnaryOperator(e));
    }
  }

  private CTypeIdExpression convert(IASTTypeIdExpression e) {
    return new CTypeIdExpression(getLocation(e), ASTTypeConverter.convert(e.getExpressionType()),
        ASTOperatorConverter.convertTypeIdOperator(e), convert(e.getTypeId()));
  }

  private CTypeIdInitializerExpression convert(IASTTypeIdInitializerExpression e) {
    return new CTypeIdInitializerExpression(getLocation(e), ASTTypeConverter.convert(e.getExpressionType()),
        convert(e.getInitializer()), convert(e.getTypeId()));
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

    if (!(declarator.getFirst() instanceof CFunctionType)) {
      throw new CFAGenerationRuntimeException("Unsupported nested declarator for function definition", f);
    }
    if (declarator.getSecond() != null) {
      throw new CFAGenerationRuntimeException("Unsupported initializer for function definition", f);
    }
    if (declarator.getThird() == null) {
      throw new CFAGenerationRuntimeException("Missing name for function definition", f);
    }

    CFunctionType declSpec = (CFunctionType)declarator.getFirst();
    String name = declarator.getThird();


    FileLocation fileLoc = getLocation(f);

    return new CFunctionDeclaration(fileLoc, declSpec, name);
  }

  public List<CDeclaration> convert(final IASTSimpleDeclaration d) {

    FileLocation fileLoc = getLocation(d);

    Pair<CStorageClass, ? extends CType> specifier = convert(d.getDeclSpecifier());
    CStorageClass cStorageClass = specifier.getFirst();
    CType type = specifier.getSecond();

    List<CDeclaration> result;
    IASTDeclarator[] declarators = d.getDeclarators();
    if (declarators == null || declarators.length == 0) {
      // declaration without declarator, i.e. struct prototype
      CDeclaration newD = createDeclaration(fileLoc, cStorageClass, type, null);
      result = Collections.singletonList(newD);

    } else if (declarators.length == 1) {
      CDeclaration newD = createDeclaration(fileLoc, cStorageClass, type, declarators[0]);
      result = Collections.singletonList(newD);

    } else {
      result = new ArrayList<>(declarators.length);
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
        return new CTypeDefDeclaration(fileLoc, isGlobal, type, name);
      }

      if (type instanceof CFunctionType) {
        if (initializer != null) {
          throw new CFAGenerationRuntimeException("Function definition with initializer", d);
        }
        if (!isGlobal) {
          throw new CFAGenerationRuntimeException("Non-global function definition", d);
        }
        return new CFunctionDeclaration(fileLoc, (CFunctionType)type, name);
      }

      // now it should be a regular variable declaration

      if (cStorageClass == CStorageClass.EXTERN && initializer != null) {
        throw new CFAGenerationRuntimeException("Extern declarations cannot have initializers", d);
      }

      String origName = name;

      if (cStorageClass == CStorageClass.STATIC) {
        if (!isGlobal) {
          isGlobal = true;
          name = "static__" + scope.getCurrentFunctionName() + "__" + name;
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

      CVariableDeclaration declaration = new CVariableDeclaration(fileLoc, isGlobal, cStorageClass, type, name, origName, null);
      scope.registerDeclaration(declaration);

      // Now that we registered the declaration, we can parse the initializer.
      // We cannot do this before, because in the following code, the right "x"
      // actually binds to the left "x"!
      // int x = x;

      declaration.addInitializer(convert(initializer));

      return declaration;

    } else {
      if (type instanceof CCompositeType
          || type instanceof CEnumType
          || type instanceof CElaboratedType) {
        // struct prototype without variable declaration or similar type definitions
        return new CComplexTypeDeclaration(fileLoc, isGlobal, type);
      }

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
      for (IASTNode modifier : modifiers) {
        if (modifier instanceof IASTArrayModifier) {
          type = convert((IASTArrayModifier)modifier, type);

        } else if (modifier instanceof IASTPointerOperator) {
          type = ASTTypeConverter.convert((IASTPointerOperator)modifier, type);

        } else {
          assert false;
        }
      }

      return Triple.of(type, initializer, name);
    }
  }

  private CType convert(IASTArrayModifier am, CType type) {
    if (am instanceof org.eclipse.cdt.core.dom.ast.c.ICASTArrayModifier) {
      org.eclipse.cdt.core.dom.ast.c.ICASTArrayModifier a = (org.eclipse.cdt.core.dom.ast.c.ICASTArrayModifier)am;
      return new CArrayType(a.isConst(), a.isVolatile(), type, convertExpressionWithoutSideEffects(a.getConstantExpression()));

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
    returnType = ASTTypeConverter.convertPointerOperators(d.getPointerOperators(), returnType);
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
    CFunctionType fType = new CFunctionType(false, false, returnType, paramsList, sd.takesVarArgs());
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

    return Triple.of(type, d.getInitializer(), name);
  }


  private Pair<CStorageClass, ? extends CType> convert(IASTDeclSpecifier d) {
    CStorageClass sc = ASTTypeConverter.convertCStorageClass(d);

    if (d instanceof IASTCompositeTypeSpecifier) {
      return Pair.of(sc, convert((IASTCompositeTypeSpecifier)d));

    } else if (d instanceof IASTElaboratedTypeSpecifier) {
      return Pair.of(sc, ASTTypeConverter.convert((IASTElaboratedTypeSpecifier)d));

    } else if (d instanceof IASTEnumerationSpecifier) {
      return Pair.of(sc, convert((IASTEnumerationSpecifier)d));

    } else if (d instanceof IASTNamedTypeSpecifier) {
      return Pair.of(sc, ASTTypeConverter.convert((IASTNamedTypeSpecifier)d));

    } else if (d instanceof IASTSimpleDeclSpecifier) {
      return Pair.of(sc, ASTTypeConverter.convert((IASTSimpleDeclSpecifier)d));

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
    return new CCompositeType(d.isConst(), d.isVolatile(), d.getKey(), list, convert(d.getName()));
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
    return new CEnumType(d.isConst(), d.isVolatile(), list, convert(d.getName()));
  }

  private CEnumerator convert(IASTEnumerationSpecifier.IASTEnumerator e, Long lastValue) {
    Long value = null;

    if (e.getValue() == null && lastValue != null) {
      value = lastValue + 1;
    } else {
      CExpression v = convertExpressionWithoutSideEffects(e.getValue());
      boolean negate = false;

      if (v instanceof CUnaryExpression) {
        CUnaryExpression u = (CUnaryExpression)v;
        assert u.getOperator() == UnaryOperator.MINUS : v;
        negate = true;
        v = u.getOperand();
      }

      if (v instanceof CIntegerLiteralExpression) {
        value = ((CIntegerLiteralExpression)v).asLong();
        if (negate) {
          value = -value;
        }
      } else {
        // ignoring unsupported enum value
        // TODO Warning
      }
    }

    CEnumerator result = new CEnumerator(getLocation(e), convert(e.getName()), value);
    scope.registerDeclaration(result);
    return result;
  }

  private CInitializer convert(IASTInitializer i) {
    if (i == null) {
      return null;

    } else if (i instanceof IASTInitializerExpression) {
      return convert((IASTInitializerExpression)i);
    } else if (i instanceof IASTInitializerList) {
      return convert((IASTInitializerList)i);
    } else if (i instanceof IASTEqualsInitializer) {
      return convert((IASTEqualsInitializer)i);
    } else if (i instanceof org.eclipse.cdt.core.dom.ast.c.ICASTDesignatedInitializer) {
      logger.log(Level.INFO, "Ignoring initializer part in line", i.getFileLocation().getStartingLineNumber() + ":", i.getRawSignature());
      return null;
    } else {
      throw new CFAGenerationRuntimeException("unknown initializer: " + i.getClass().getSimpleName(), i);
    }
  }

  private CInitializerExpression convert(IASTInitializerExpression i) {
    CAstNode initializer = convertExpressionWithSideEffects(i.getExpression());
    if (initializer != null && initializer instanceof CAssignment){
      preSideAssignments.add(initializer);
      return new CInitializerExpression(getLocation(i), ((CAssignment)initializer).getLeftHandSide());
    }

    if (initializer != null && !(initializer instanceof CExpression)) {
      throw new CFAGenerationRuntimeException("Initializer is not free of side-effects", i);
    }

    return new CInitializerExpression(getLocation(i), (CExpression)initializer);
  }

  private CInitializerList convert(IASTInitializerList iList) {
    List<CInitializer> initializerList = new ArrayList<>(iList.getInitializers().length);
    for (IASTInitializer i : iList.getInitializers()) {
      CInitializer newI = convert(i);
      if (newI != null) {
        initializerList.add(newI);
      }
    }

    return new CInitializerList(getLocation(iList), initializerList);
  }

  private CInitializer convert(IASTEqualsInitializer i) {
    IASTInitializerClause ic = i.getInitializerClause();
    if (ic instanceof IASTExpression) {
      IASTExpression e = (IASTExpression)ic;

      CAstNode initializer = convertExpressionWithSideEffects(e);

      if (initializer != null && initializer instanceof CAssignment){
        preSideAssignments.add(initializer);
        return new CInitializerExpression(getLocation(e), ((CAssignment)initializer).getLeftHandSide());
      } else if (initializer != null && initializer instanceof CFunctionCallExpression && i.getParent() instanceof IASTDeclarator) {

        String tmpname = convert(((IASTDeclarator)i.getParent()).getName());
        postSideAssignments.add(new CFunctionCallAssignmentStatement(getLocation(i),
                                                                        createTemporaryVariable(e, tmpname),
                                                                        (CFunctionCallExpression) initializer));
        return null;
      }

      if (initializer != null && !(initializer instanceof CExpression)) {
        throw new CFAGenerationRuntimeException("Initializer is not free of side-effects", e);
      }

      return new CInitializerExpression(getLocation(ic), (CExpression)initializer);

    } else if (ic instanceof IASTInitializerList) {
      return convert((IASTInitializerList)ic);
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
    if (type instanceof CFunctionType) {
      CFunctionType functionType = (CFunctionType) type;
      type = new CPointerType(false, false, functionType);
    }

    return new CParameterDeclaration(getLocation(p), type, declarator.getThird());
  }


  /** This function returns the converted file-location of an IASTNode. */
  static FileLocation getLocation(final IASTNode n) {
    return convert(n.getFileLocation());
  }

  static FileLocation convert(IASTFileLocation l) {
    if (l == null) {
      return null;
    }

    return new FileLocation(l.getEndingLineNumber(), l.getFileName(),
        l.getNodeLength(), l.getNodeOffset(), l.getStartingLineNumber());
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