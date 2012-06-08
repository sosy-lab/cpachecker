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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.BasicType;
import org.sosy_lab.cpachecker.cfa.ast.DummyType;
import org.sosy_lab.cpachecker.cfa.ast.IASTArrayTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IType;
import org.sosy_lab.cpachecker.cfa.ast.StorageClass;


public class ASTConverter {



  private final LogManager logger;

  private LinkedList<IASTNode> sideAssignments = new LinkedList<IASTNode>();

  public ASTConverter(Scope pScope, boolean pIgnoreCasts, LogManager pLogger) {
    logger = pLogger;
  }

  public int numberOfSideAssignments(){
    return sideAssignments.size();
  }

  public IASTNode getNextSideAssignment() {
    return sideAssignments.removeFirst();
  }




  @SuppressWarnings({ "cast", "unchecked" })
  public IASTFunctionDeclaration convert(final org.eclipse.jdt.core.dom.MethodDeclaration f) {


    IASTFunctionTypeSpecifier declSpec = new IASTFunctionTypeSpecifier(false , false , convert(f.getReturnType2()) , convertParameterList((List<SingleVariableDeclaration>)f.parameters()) , false );
    String name = f.getName().getFullyQualifiedName();

    IASTFileLocation fileLoc = getFileLocation(f);

    return new IASTFunctionDeclaration(fileLoc, declSpec, name);
  }

  private IType convert(org.eclipse.jdt.core.dom.Type t) {
    if (t.getNodeType() == ASTNode.PRIMITIVE_TYPE) {
      return convert((org.eclipse.jdt.core.dom.PrimitiveType)t);

    } else if(t.getNodeType() == ASTNode.ARRAY_TYPE){
      return convert((org.eclipse.jdt.core.dom.ArrayType)t);
    } else {
      return new DummyType(t.toString());
    }
  }


  private IASTArrayTypeSpecifier convert(final org.eclipse.jdt.core.dom.ArrayType t) {

     return new IASTArrayTypeSpecifier(false, false, convert(t.getElementType()), null);

  }


  public IASTFileLocation getFileLocation (org.eclipse.jdt.core.dom.ASTNode l) {
    if (l == null) {
      return null;
    }

    CompilationUnit co = (CompilationUnit) l.getRoot();


    // TODO Solve File Name Problem
    return new IASTFileLocation(   co.getLineNumber(l.getLength() + l.getStartPosition()), "Readable.java", l.getLength(), l.getStartPosition(), co.getLineNumber(l.getStartPosition()));
  }


  private IASTSimpleDeclSpecifier convert(final org.eclipse.jdt.core.dom.PrimitiveType t) {



        PrimitiveType.Code primitiveTypeName = t.getPrimitiveTypeCode();

        return convertPrimitiveType(primitiveTypeName.toString());
  }

  private IASTSimpleDeclSpecifier convertPrimitiveType(String primitiveTypeName) {





    BasicType type;


    if(primitiveTypeName.equals("boolean") ){
      type = BasicType.BOOL;
    } else if(primitiveTypeName.equals("char")) {
      type = BasicType.CHAR;
    } else if(primitiveTypeName.equals("double")) {
      type = BasicType.DOUBLE;
    } else if(primitiveTypeName.equals("float")) {
      type = BasicType.FLOAT;
    } else if(primitiveTypeName.equals("int")) {
      type = BasicType.INT;
    } else if(primitiveTypeName.equals("boolean")) {
      type = BasicType.UNSPECIFIED;
    } else if(primitiveTypeName.equals("void")) {
      type = BasicType.VOID;
    } else {
      throw new CFAGenerationRuntimeException("Unknown primitive type " + primitiveTypeName);
    }


    return new IASTSimpleDeclSpecifier(false, false, type, false, false, false, false, false, false, false);



}


  private List<IASTParameterDeclaration> convertParameterList(List<SingleVariableDeclaration> ps) {
    List<IASTParameterDeclaration> paramsList = new ArrayList<IASTParameterDeclaration>(ps.size());
    for (org.eclipse.jdt.core.dom.SingleVariableDeclaration c : ps) {

      if (!( c.getType().isPrimitiveType() && ((PrimitiveType) c.getType()).getPrimitiveTypeCode().equals("void"))) {
        paramsList.add(convertParameter(c));
      } else {
        // there may be a function declaration f(void), which is equal to f()
        // we don't want this dummy parameter "void"
        assert ps.size() == 1;
      }
    }
    return paramsList;
  }



  private IASTParameterDeclaration convertParameter(org.eclipse.jdt.core.dom.SingleVariableDeclaration p) {


    IType type = convert(p.getType());


    return new IASTParameterDeclaration(getFileLocation( p), type, p.getName().getFullyQualifiedName());
  }









  public List<org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration> convert(org.eclipse.jdt.core.dom.VariableDeclaration vd){
      List<org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration> result = new ArrayList<org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration>();
      result.add(createVariableDeclaration(vd));
      return result;
  }


  public IASTVariableDeclaration createVariableDeclaration( org.eclipse.jdt.core.dom.VariableDeclaration d) {


    if(d.getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION){
      return createVariableDeclaration((SingleVariableDeclaration) d);
    } else {
      // TODO implement Fragment Declaration
      return createVariableDeclaration((VariableDeclarationFragment) d);
    }

  }

  private IASTVariableDeclaration createVariableDeclaration(VariableDeclarationFragment d) {

  IASTInitializerExpression initializerExpression = null;

    // If there is no Initializer, StorageClass expects null to be given.
  if(d.getInitializer() != null){
    initializerExpression = new IASTInitializerExpression( getFileLocation(d) , convert(d.getInitializer()));
  }

    ASTNode parent = d.getParent();

    Type type = null;

    if(parent.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
      type =  ((VariableDeclarationStatement) parent).getType();
    }else {
      logger.log(Level.SEVERE, "Type of Parent of Variable Declaration Statement not implemented");
    }

    return new IASTVariableDeclaration(getFileLocation(d), false,
        StorageClass.AUTO, convert(type), d.getName().getFullyQualifiedName(), d.getName().getFullyQualifiedName(), initializerExpression);
  }

  public IASTVariableDeclaration createVariableDeclaration( org.eclipse.jdt.core.dom.SingleVariableDeclaration d) {


    IASTInitializerExpression initializerExpression = null;

      // If there is no Initializer, StorageClass expects null to be given.
    if(d.getInitializer() != null){
      initializerExpression = new IASTInitializerExpression( getFileLocation(d) , convert(d.getInitializer()));
    }


    return new IASTVariableDeclaration(getFileLocation(d), false,
        StorageClass.AUTO, convert(d.getType()), d.getName().getFullyQualifiedName(), d.getName().getFullyQualifiedName(), initializerExpression);
  }



  private IASTExpression convert(Expression e) {



    if (e == null) {
      return null;

    } else if (e.getNodeType() == ASTNode.INFIX_EXPRESSION) {
      return convert((InfixExpression)e);

    } else if (e.getNodeType() == ASTNode.NUMBER_LITERAL) {
      return convert((NumberLiteral)e);

    }
    return null;


  }

  private IASTExpression convert(InfixExpression e) {
    IASTFileLocation fileLoc = getFileLocation(e);
    IType type = convert(e.resolveTypeBinding());
    IASTExpression leftHandSide = convert(e.getLeftOperand());




    BinaryOperator op =   convertBinaryOperator(e.getOperator());

    IASTExpression rightHandSide = convert(e.getRightOperand());
    return new IASTBinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);

  }

  private BinaryOperator convertBinaryOperator(Operator pOperator) {


    Operator op = Operator.toOperator(pOperator.toString());

    if(op.equals(Operator.PLUS)){
      return BinaryOperator.PLUS;
    } else if (op.equals(Operator.MINUS)) {
      return BinaryOperator.MINUS;
    } else if (op.equals(Operator.DIVIDE)) {
      return BinaryOperator.DIVIDE;
    } else if (op.equals(Operator.TIMES)) {
      return BinaryOperator.MULTIPLY;
    } else if (op.equals(Operator.EQUALS)) {
      return BinaryOperator.EQUALS;
    }

    logger.log(Level.SEVERE, "Did not find Operator");

    return null;
  }





  private IType convert(ITypeBinding t) {

    t.getName();

    return null;
  }





  private IASTExpression convert(NumberLiteral e) {

    return new IASTIntegerLiteralExpression( getFileLocation(e), convert(e.resolveTypeBinding()) , BigInteger.valueOf(Long.valueOf(e.getToken())));


  }


  //TODO Finish Method for boolean
  /*
  public IASTExpression convertBooleanExpression(Expression e){

    IASTExpression exp = convertExpressionWithoutSideEffects(e);
    if (!isBooleanExpression(exp)) {

      // TODO: probably the type of the zero is not always correct
      IASTExpression zero = new IASTIntegerLiteralExpression(exp.getFileLocation(), exp.getExpressionType(), BigInteger.ZERO);
      return new IASTBinaryExpression(exp.getFileLocation(), exp.getExpressionType(), exp, zero, BinaryOperator.NOT_EQUALS);
    }

    return exp;
  }
*/







}
