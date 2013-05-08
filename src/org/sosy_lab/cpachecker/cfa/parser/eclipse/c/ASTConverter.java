/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkNotNull;

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
import org.eclipse.cdt.core.dom.ast.c.ICASTArrayDesignator;
import org.eclipse.cdt.core.dom.ast.c.ICASTDesignatedInitializer;
import org.eclipse.cdt.core.dom.ast.c.ICASTDesignator;
import org.eclipse.cdt.core.dom.ast.c.ICASTFieldDesignator;
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTCompoundStatementExpression;
import org.eclipse.cdt.core.dom.ast.gnu.c.IGCCASTArrayRangeDesignator;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CEmptyDesignator;
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
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

class ASTConverter {

  private final LogManager logger;
  private final ASTLiteralConverter literalConverter;
  private final ASTTypeConverter typeConverter;

  private final Scope scope;

  private int anonTypeCounter = 0;

  private final List<CAstNode> preSideAssignments = new ArrayList<>();
  private final List<CAstNode> postSideAssignments = new ArrayList<>();

  // this list is for ternary operators, &&, etc.
  private final List<Pair<IASTExpression, CIdExpression>> conditionalExpressions = new ArrayList<>();

  public ASTConverter(Scope pScope, LogManager pLogger, MachineModel pMachineModel) {
    scope = pScope;
    logger = pLogger;
    typeConverter = new ASTTypeConverter(scope);
    literalConverter = new ASTLiteralConverter(typeConverter, pMachineModel);
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

  public boolean hasConditionalExpression() {
    return !conditionalExpressions.isEmpty();
  }

  public List<Pair<IASTExpression, CIdExpression>> getAndResetConditionalExpressions() {
    List<Pair<IASTExpression, CIdExpression>> result = new ArrayList<>(conditionalExpressions);
    conditionalExpressions.clear();
    return result;
  }

  public List<Pair<IASTExpression, CIdExpression>> getConditionalExpressions() {
    return Collections.unmodifiableList(conditionalExpressions);
  }

  private void addConditionalExpression(IASTExpression e, CIdExpression tempVar) {
    conditionalExpressions.add(Pair.of(checkNotNull(e), checkNotNull(tempVar)));
  }

  BigInteger parseIntegerLiteral(String s, final IASTNode e) {
    return literalConverter.parseIntegerLiteral(s, e);
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
      return addSideAssignmentsForUnaryExpressions(e, ((CAssignment)node).getLeftHandSide(),
          node.getFileLocation(), typeConverter.convert(e.getExpressionType()),
          ((CBinaryExpression)((CAssignment)node).getRightHandSide()).getOperator());

    } else if (node instanceof CAssignment) {
      preSideAssignments.add(node);
      return ((CAssignment) node).getLeftHandSide();

    } else {
      throw new AssertionError("unknown expression " + node);
    }
  }

  private CExpression addSideassignmentsForExpressionsWithoutSideEffects(CAstNode node,
                                                                            IASTExpression e) {
    CIdExpression tmp = createTemporaryVariable(e);

    preSideAssignments.add(new CFunctionCallAssignmentStatement(getLocation(e),
                                                                tmp,
                                                                (CFunctionCallExpression) node));
    return tmp;
  }

  private CIdExpression addSideAssignmentsForUnaryExpressions(IASTExpression e,
                                                              CLeftHandSide exp,
                                                              FileLocation fileLoc,
                                                              CType type,
                                                              BinaryOperator op) {
    CIdExpression tmp = createTemporaryVariable(e);
    preSideAssignments.add(new CExpressionAssignmentStatement(fileLoc, tmp, exp));


    CExpression one = createSideeffectLiteralOne(type, fileLoc, e);
    CBinaryExpression postExp = new CBinaryExpression(fileLoc, type, exp, one, op);
    preSideAssignments.add(new CExpressionAssignmentStatement(fileLoc, exp, postExp));


    return tmp;
  }

  /**
   * Create an expression for the "1" literal that is used for example when
   * replacing "x++" with "x = x + 1";
   * @return
   */
  private CIntegerLiteralExpression createSideeffectLiteralOne(final CType expressionType, FileLocation fileLoc, IASTExpression expression) {
    CType type = expressionType;
    while (type instanceof CTypedefType) {
      type = ((CTypedefType)type).getRealType();
    }

    CType constantType; // the type of the "1"
    if (type instanceof CSimpleType
        || type instanceof CProblemType) { // necessary for parsing "x++" in automata
      constantType = expressionType;
    } else if (type instanceof CPointerType) {
      constantType = CNumericTypes.INT;
    } else {
      throw new CFAGenerationRuntimeException("Prefix operator used for wrong type " + expressionType, expression);
    }

    return new CIntegerLiteralExpression(fileLoc, constantType, BigInteger.ONE);
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
      return literalConverter.convert((IASTLiteralExpression)e);

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
    CIdExpression tmp = createTemporaryVariable(e);
    addConditionalExpression(e, tmp);
    return tmp;
  }

  private CAstNode convert(IGNUASTCompoundStatementExpression e) {
    CIdExpression tmp = createTemporaryVariable(e);
    addConditionalExpression(e, tmp);
    return tmp;
  }

  private CAstNode convertExpressionListAsExpression(IASTExpressionList e) {
    CIdExpression tmp = createTemporaryVariable(e);
    addConditionalExpression(e, tmp);
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
    String name = "__CPAchecker_TMP_";
    int i = 0;
    while (scope.variableNameInUse(name + i, name + i)) {
      i++;
    }
    name += i;

    CVariableDeclaration decl = new CVariableDeclaration(getLocation(e),
                                               false,
                                               CStorageClass.AUTO,
                                               typeConverter.convert(e.getExpressionType()),
                                               name,
                                               name,
                                               null);

    scope.registerDeclaration(decl);
    preSideAssignments.add(decl);

    CIdExpression tmp = new CIdExpression(getLocation(e),
                                                typeConverter.convert(e.getExpressionType()),
                                                name,
                                                decl);
    return tmp;
  }

  private CAstNode convert(IASTBinaryExpression e) {

    switch (e.getOperator()) {
    case IASTBinaryExpression.op_logicalAnd:
    case IASTBinaryExpression.op_logicalOr:
      CIdExpression tmp = createTemporaryVariable(e);
      addConditionalExpression(e, tmp);
      return tmp;
    }

    Pair<BinaryOperator, Boolean> opPair = ASTOperatorConverter.convertBinaryOperator(e);
    BinaryOperator op = opPair.getFirst();
    boolean isAssign = opPair.getSecond();

    FileLocation fileLoc = getLocation(e);
    CType type = typeConverter.convert(e.getExpressionType());

    if (isAssign) {
      CLeftHandSide leftHandSide = (CLeftHandSide) convertExpressionWithoutSideEffects(e.getOperand1());

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
      CExpression leftHandSide = convertExpressionWithoutSideEffects(e.getOperand1());
      CExpression rightHandSide = convertExpressionWithoutSideEffects(e.getOperand2());


      // There is a problem with the parser-determined type for statements of the form
      // long int i = p - q; // p and q are pointers
      // The type of the right hand side is set to int by the parser, but this is wrong on 64bit platforms!
      // We fix this by setting the type to long int (correct would be ptrdiff_t).

      if ((op == BinaryOperator.MINUS)
          && (leftHandSide.getExpressionType() instanceof CPointerType)
          && (rightHandSide.getExpressionType() instanceof CPointerType)
          && (type instanceof CSimpleType)) {

        CSimpleType simpleType = (CSimpleType)type;
        if ((simpleType.getType() == CBasicType.INT)
            && !simpleType.isLong()
            && !simpleType.isLongLong()) {

          type = new CSimpleType(type.isConst(), type.isVolatile(), CBasicType.INT, true, false, true, false, false, false, false);
          logger.log(Level.FINE, "Got pointer difference expression where we needed to change the type from", simpleType, "to", type, "in expression", leftHandSide, "-", rightHandSide, "on line", fileLoc.getStartingLineNumber());
        }
      }

      return new CBinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);
    }
  }

  private CAstNode convert(IASTCastExpression e) {
    return new CCastExpression(getLocation(e), typeConverter.convert(e.getExpressionType()), convertExpressionWithoutSideEffects(e.getOperand()), convert(e.getTypeId()));
  }

  private CFieldReference convert(IASTFieldReference e) {
    CType type = typeConverter.convert(e.getExpressionType());
    CExpression owner = convertExpressionWithoutSideEffects(e.getFieldOwner());
    String fieldName = convert(e.getFieldName());

    // if the owner is a FieldReference itself there's the need for a temporary Variable
    // but only if we are not in global scope, otherwise there will be parsing errors
    if(owner instanceof CFieldReference && !scope.isGlobalScope()) {
      CIdExpression tmpVar = createTemporaryVariable(e.getFieldOwner());
      preSideAssignments.add(new CExpressionAssignmentStatement(getLocation(e), tmpVar, owner));
      owner = tmpVar;
    }

    if (type instanceof CProblemType) {
      CType ownerType = owner.getExpressionType();
      if (ownerType instanceof CElaboratedType) {
        ownerType = ((CElaboratedType)ownerType).getRealType();
      }
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

    // if there is a "var->field" convert it to (*var).field
    if(e.isPointerDereference()) {
      CPointerExpression exp = new CPointerExpression(getLocation(e), type, owner);
      return new CFieldReference(getLocation(e), type, fieldName, exp, false);
    }

    return new CFieldReference(getLocation(e), type, fieldName, owner, e.isPointerDereference());
  }

  private CRightHandSide convert(IASTFunctionCallExpression e) {
    List<CExpression> params = new ArrayList<>();
    for (IASTInitializerClause i : e.getArguments()) {
      params.add(convertExpressionWithoutSideEffects(toExpression(i)));
    }

    CExpression functionName = convertExpressionWithoutSideEffects(e.getFunctionNameExpression());
    CFunctionDeclaration declaration = null;

    if (functionName instanceof CIdExpression) {
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

        return new CBinaryExpression(getLocation(e), CNumericTypes.INT, params.get(0), params.get(1), BinaryOperator.EQUALS);
      }
    }

    return new CFunctionCallExpression(getLocation(e), typeConverter.convert(e.getExpressionType()), functionName, params, declaration);
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

    CType type = typeConverter.convert(e.getExpressionType());

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

  private CAstNode convert(IASTUnaryExpression e) {
    CExpression operand = convertExpressionWithoutSideEffects(e.getOperand());
    FileLocation fileLoc = getLocation(e);
    CType type = typeConverter.convert(e.getExpressionType());

    switch (e.getOperator()) {
    case IASTUnaryExpression.op_bracketedPrimary:
      return operand;

    case IASTUnaryExpression.op_star:
      // if there is a dereference on a field of a dereferenced struct a temporary variable is needed
      if(operand instanceof CFieldReference
          && ((CFieldReference)operand).getFieldOwner() instanceof CPointerExpression) {
        CIdExpression tmpVar = createTemporaryVariable(e.getOperand());
        preSideAssignments.add(new CExpressionAssignmentStatement(fileLoc, tmpVar, operand));
        return new CPointerExpression(fileLoc, type, tmpVar);
      }

      // in case of *& both can be left out
      else if(operand instanceof CUnaryExpression
          && ((CUnaryExpression)operand).getOperator() == UnaryOperator.AMPER) {
        return ((CUnaryExpression)operand).getOperand();
      }

      // in case of ** a temporary variable is needed
      else if(operand instanceof CPointerExpression) {
        CIdExpression tmpVar = createTemporaryVariable(e.getOperand());
        preSideAssignments.add(new CExpressionAssignmentStatement(fileLoc, tmpVar, operand));
        return new CPointerExpression(fileLoc, type, tmpVar);
      }

      // in case of p.e. *(a+b) or +(a-b) or *(a ANY_OTHER_OPERATOR b) a temporary variable is needed
      else if(operand instanceof CBinaryExpression) {
        CIdExpression tmpVar = createTemporaryVariable(e.getOperand());
        preSideAssignments.add(new CExpressionAssignmentStatement(fileLoc, tmpVar, operand));
        return new CPointerExpression(fileLoc, type, tmpVar);
      }

      // if none of the special cases before fits the default unaryExpression is created
      return new CPointerExpression(fileLoc, type, operand);

    case IASTUnaryExpression.op_amper:
      // in case of *& both can be left out
      if(operand instanceof CPointerExpression) {
        return ((CUnaryExpression)operand).getOperand();
      }

      // if none of the special cases before fits the default unaryExpression is created
      return new CUnaryExpression(fileLoc, type, operand, ASTOperatorConverter.convertUnaryOperator(e));

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

      CExpression one = createSideeffectLiteralOne(type, fileLoc, e);
      CBinaryExpression preExp = new CBinaryExpression(fileLoc, type, operand, one, preOp);

      if (operand instanceof CLeftHandSide) {
        return new CExpressionAssignmentStatement(fileLoc, (CLeftHandSide) operand, preExp);
      } else {
        throw new CFAGenerationRuntimeException(operand.toASTString() + " is not applicable for the left side of a statement");
      }

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

      CExpression postOne = createSideeffectLiteralOne(type, fileLoc, e);
      CBinaryExpression postExp = new CBinaryExpression(fileLoc, type, operand, postOne, postOp);

      if (operand instanceof CLeftHandSide) {
        return new CExpressionAssignmentStatement(fileLoc, (CLeftHandSide) operand, postExp);
      } else {
        throw new CFAGenerationRuntimeException(operand.toASTString() + " is not applicable for the left side of a statement");
      }

    default:
      return new CUnaryExpression(fileLoc, type, operand, ASTOperatorConverter.convertUnaryOperator(e));
    }
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
        return new CTypeDefDeclaration(fileLoc, isGlobal, type, name);
      }

      if (type instanceof CFunctionTypeWithNames) {
        if (initializer != null) {
          throw new CFAGenerationRuntimeException("Function definition with initializer", d);
        }
        if (!isGlobal) {
          throw new CFAGenerationRuntimeException("Non-global function definition", d);
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
    CEnumType enumType = new CEnumType(d.isConst(), d.isVolatile(), list, convert(d.getName()));
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
      } else if (v instanceof CUnaryExpression && ((CUnaryExpression) v).getOperator() != UnaryOperator.PLUS){
        assert false : v;
      }

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

    CEnumerator result = new CEnumerator(getLocation(e), convert(e.getName()), value);
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

    // make initial variable
    CDesignator exp = new CEmptyDesignator(fileLoc);

    // convert all designators
    for (int i = 0; i < desInit.length; i++) {
      if (desInit[i] instanceof ICASTFieldDesignator) {
       exp = new CFieldDesignator(fileLoc,
           convert(((ICASTFieldDesignator) desInit[i]).getName()),
           exp);

      } else if (desInit[i] instanceof ICASTArrayDesignator) {
        exp = new CArrayDesignator(fileLoc,
            convertExpressionWithoutSideEffects(((ICASTArrayDesignator) desInit[i]).getSubscriptExpression()),
            exp);

      } else if (desInit[i] instanceof IGCCASTArrayRangeDesignator) {
        exp = new CArrayRangeDesignator(fileLoc,
            convertExpressionWithoutSideEffects(((IGCCASTArrayRangeDesignator) desInit[i]).getRangeFloor()),
            convertExpressionWithoutSideEffects(((IGCCASTArrayRangeDesignator) desInit[i]).getRangeCeiling()),
            exp);

      } else {
        throw new CFAGenerationRuntimeException("Unsupported Designator", desInit[i]);
      }
    }

    return new CDesignatedInitializer(fileLoc, exp, cInit);
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
        preSideAssignments.add(initializer);
        return new CInitializerExpression(getLocation(e), ((CAssignment)initializer).getLeftHandSide());

      } else if (initializer instanceof CFunctionCallExpression) {
        FileLocation loc = getLocation(i);

        if (declaration != null) {
          // This is a variable declaration like "int i = f();"
          // We can replace this with "int i; i = f();"
          CIdExpression var = new CIdExpression(loc, declaration.getType(), declaration.getName(), declaration);
          postSideAssignments.add(new CFunctionCallAssignmentStatement(loc, var,
                                  (CFunctionCallExpression) initializer));
          return null; // empty initializer

        } else {
          // This is something more complicated, like a function call inside an array initializer.
          // We need a temporary variable.

          CIdExpression var = createTemporaryVariable(e);
          preSideAssignments.add(new CFunctionCallAssignmentStatement(loc, var,
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