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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.BasicType;
import org.sosy_lab.cpachecker.cfa.ast.DummyType;
import org.sosy_lab.cpachecker.cfa.ast.IASTArrayTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IASTFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IType;
import org.sosy_lab.cpachecker.cfa.ast.StorageClass;

import com.google.common.collect.ImmutableSet;


public class ASTConverter {



  private final LogManager logger;

  private Scope scope;
  private LinkedList<IASTNode> sideAssignments = new LinkedList<IASTNode>();
  private org.eclipse.cdt.core.dom.ast.IASTConditionalExpression conditionalExpression = null;
  private IASTIdExpression conditionalTemporaryVariable = null;

  public ASTConverter(Scope pScope, boolean pIgnoreCasts, LogManager pLogger) {
    scope = pScope;
    logger = pLogger;
  }

  public int numberOfSideAssignments(){
    return sideAssignments.size();
  }

  public IASTNode getNextSideAssignment() {
    return sideAssignments.removeFirst();
  }

  public void resetConditionalExpression() {
    conditionalExpression = null;
  }

  public org.eclipse.cdt.core.dom.ast.IASTConditionalExpression getConditionalExpression() {
    return conditionalExpression;
  }

  public IASTIdExpression getConditionalTemporaryVariable() {
    return conditionalTemporaryVariable;
  }

  private static void check(boolean assertion, String msg, ASTNode astNode) throws CFAGenerationRuntimeException {
    if (!assertion) {
      throw new CFAGenerationRuntimeException(msg);
    }
  }

  @SuppressWarnings({ "cast", "unchecked" })
  public IASTFunctionDeclaration convert(final MethodDeclaration f) {
  //TODO Protoyp implementation for Method Declaration Converting,
  //     Finish when method calls are to be implemented.

    IASTFunctionTypeSpecifier declSpec = new IASTFunctionTypeSpecifier(false , false , convert(f.getReturnType2()) , convertParameterList((List<SingleVariableDeclaration>)f.parameters()) , false );
    String name = f.getName().getFullyQualifiedName();

    IASTFileLocation fileLoc = getFileLocation(f);

    return new IASTFunctionDeclaration(fileLoc, declSpec, name);
  }

  private IType convert(Type t) {
    // TODO Not all Types implemented
    if (t.getNodeType() == ASTNode.PRIMITIVE_TYPE) {
      return convert((PrimitiveType)t);

    } else if(t.getNodeType() == ASTNode.ARRAY_TYPE){
      return convert((ArrayType)t);
    } else {
      return new DummyType(t.toString());
    }
  }

  private IType convert(ITypeBinding t) {
    //TODO Needs to be completed

    if(t.isPrimitive()){
      return new IASTSimpleDeclSpecifier(false, false, convertPrimitiveType(t.getName()), false, false, false, false, false, false, false);
    }

    return null;
  }


  private IASTArrayTypeSpecifier convert(final ArrayType t) {
      //TODO Prototyp implementation for Array Typ
     return new IASTArrayTypeSpecifier(false, false, convert(t.getElementType()), null);

  }

  /**
   * Takes a ASTNode, and tries to get Information of its Placement in the
   * Source Code. If it doesnt't find such information, returns null.
   *
   *
   * @param l A Code piece wrapped in an ASTNode
   * @return FileLocation with Placement Information of the Code Piece, or null
   *          if such Information could not be obtained.
   */
  public IASTFileLocation getFileLocation (ASTNode l) {
    if (l == null) {
      return null;
    } else if(l.getRoot().getNodeType() != ASTNode.COMPILATION_UNIT){
      logger.log(Level.WARNING, "Can't find Placement Information for :" + l.toString());
    }

    //TODO See if this works for FileHierachys
    CompilationUnit co = (CompilationUnit) l.getRoot();


    // TODO Solve File Name Problem
    return new IASTFileLocation(   co.getLineNumber(l.getLength() + l.getStartPosition()),
                                     "Readable.java", l.getLength(), l.getStartPosition(),
                                       co.getLineNumber(l.getStartPosition()));
  }


  private  IASTSimpleDeclSpecifier convert(final PrimitiveType t) {


        PrimitiveType.Code primitiveTypeName = t.getPrimitiveTypeCode();


        //TODO refactor SimpleDeclSpecifier for Information in Java
        //  Object is instantiated here, but filled with modifier Information later
        // to make the above functions more common
        return new IASTSimpleDeclSpecifier(false, false, convertPrimitiveType(primitiveTypeName.toString()), false, false, false, false, false, false, false);

  }

  private BasicType convertPrimitiveType(String primitiveTypeName) {

    BasicType type;
    // TODO BasicType extension for boolean, best if also in abstract with generic.
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
      type = BasicType.INT;
    } else if(primitiveTypeName.equals("void")) {
      type = BasicType.VOID;
    } else {
      throw new CFAGenerationRuntimeException("Unknown primitive type " + primitiveTypeName);
    }

    return type;
}

  private List<IASTParameterDeclaration> convertParameterList(List<SingleVariableDeclaration> ps) {
    List<IASTParameterDeclaration> paramsList = new ArrayList<IASTParameterDeclaration>(ps.size());
    for (org.eclipse.jdt.core.dom.SingleVariableDeclaration c : ps) {
        paramsList.add(convertParameter(c));
    }
    return paramsList;
  }

  private IASTParameterDeclaration convertParameter(SingleVariableDeclaration p) {

    IType type = convert(p.getType());

    return new IASTParameterDeclaration(getFileLocation(p), type, p.getName().getFullyQualifiedName());
  }

  public List<org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration> convert(VariableDeclaration vd){
      List<org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration> result = new ArrayList<org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration>();
      result.add(createVariableDeclaration(vd));
      return result;
  }


  public IASTVariableDeclaration createVariableDeclaration( VariableDeclaration d) {

    if(d.getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION){
      return createVariableDeclaration((SingleVariableDeclaration) d);
    } else {
      return createVariableDeclaration((VariableDeclarationFragment) d);
    }

  }

  private IASTVariableDeclaration createVariableDeclaration(VariableDeclarationFragment d) {

  IASTInitializerExpression initializerExpression = null;

  // If there is no Initializer, StorageClass expects null to be given.
  if(d.getInitializer() != null){
    initializerExpression = new IASTInitializerExpression( getFileLocation(d) , (IASTExpression) convertExpressionWithSideEffects(d.getInitializer()));
  }

    // TODO Refactor in tandem with VariableDeclarationStatment
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

  public List<IASTVariableDeclaration> convert(VariableDeclarationStatement vds){
    // TODO Refactor for effectivness, types don't have to be resolved
    //      redundantly in variableDeclarationFragments

    List<IASTVariableDeclaration> variableDeclarations = new ArrayList<IASTVariableDeclaration>();

    @SuppressWarnings({ "cast", "unchecked" })
    List<VariableDeclarationFragment> variableDeclarationFragments =
                                            (List<VariableDeclarationFragment>)vds.fragments();

    for(VariableDeclarationFragment vdf : variableDeclarationFragments){
        variableDeclarations.add(createVariableDeclaration(vdf));
    }

    return variableDeclarations;
  }

  public IASTVariableDeclaration createVariableDeclaration(SingleVariableDeclaration d) {


    IASTInitializerExpression initializerExpression = null;

      // If there is no Initializer, StorageClass expects null to be given.
    if(d.getInitializer() != null){
      initializerExpression = new IASTInitializerExpression( getFileLocation(d) , (IASTExpression) convertExpressionWithSideEffects(d.getInitializer()));
    }

    //TODO Think about Scope with Storage Class and the meaning behind the names
    return new IASTVariableDeclaration(getFileLocation(d), false,
        StorageClass.AUTO, convert(d.getType()), d.getName().getFullyQualifiedName(), d.getName().getFullyQualifiedName(), initializerExpression);
  }




  public IASTExpression convertExpressionWithoutSideEffects( Expression e) {

    IASTNode node = convertExpressionWithSideEffects(e);

    if (node == null || node instanceof IASTExpression) {
      return (IASTExpression) node;

    } else if (node instanceof IASTFunctionCallExpression) {
      return addSideassignmentsForExpressionsWithoutSideEffects(node, e);

    } else if (node instanceof IASTAssignment) {
      sideAssignments.add(node);
      return ((IASTAssignment) node).getLeftHandSide();

    } else {
      throw new AssertionError("unknown expression " + node);
    }
  }

  private IASTExpression addSideassignmentsForExpressionsWithoutSideEffects(IASTNode node,Expression e) {
    IASTIdExpression tmp = createTemporaryVariable(e);

    //TODO Investigate if FileLoction is instanced
    sideAssignments.add(new IASTFunctionCallAssignmentStatement( node.getFileLocation(),
        tmp,
        (IASTFunctionCallExpression) node));
    return tmp;
  }

  /**
   * creates temporary variables with increasing numbers
   */
  private IASTIdExpression createTemporaryVariable(Expression e) {
    String name = "__CPAchecker_TMP_";
    int i = 0;
    while(scope.variableNameInUse(name+i, name+i)){
      i++;
    }
    name += i;

    // TODO Maybe node as parameter to avoid converting File Location?
    IASTVariableDeclaration decl = new IASTVariableDeclaration(getFileLocation(e),
                                               false,
                                               StorageClass.AUTO,
                                               convert(e.resolveTypeBinding()),
                                               name,
                                               name,
                                               null);

    scope.registerDeclaration(decl);
    sideAssignments.add(decl);
    IASTIdExpression tmp = new IASTIdExpression(decl.getFileLocation(),
                                                convert(e.resolveTypeBinding()),
                                                name,
                                                decl);
    return tmp;
  }


  public IASTStatement convert(final ExpressionStatement s) {

    IASTNode node = convertExpressionWithSideEffects(s.getExpression());

    if (node instanceof IASTExpressionAssignmentStatement) {
      return (IASTExpressionAssignmentStatement)node;

    } else if (node instanceof IASTFunctionCallAssignmentStatement) {
      return (IASTFunctionCallAssignmentStatement)node;

    } else if (node instanceof IASTFunctionCallExpression) {
      return new IASTFunctionCallStatement( getFileLocation(s) , (IASTFunctionCallExpression)node);

    } else if (node instanceof IASTExpression) {
      return new IASTExpressionStatement(  getFileLocation(s) , (IASTExpression)node);

    } else {
      throw new AssertionError();
    }
  }

  private IASTExpression convert(Expression e) {


    switch (e.getNodeType()) {
    case ASTNode.INFIX_EXPRESSION:
      return convert((InfixExpression) e);
    case ASTNode.NUMBER_LITERAL:
      return convert((NumberLiteral) e);
    case ASTNode.CHARACTER_LITERAL:
      return convert((CharacterLiteral) e);
    case ASTNode.STRING_LITERAL:
      return convert((StringLiteral) e);
    case ASTNode.NULL_LITERAL:
      return convert((NullLiteral) e);
    case ASTNode.PREFIX_EXPRESSION:
      return convert((PrefixExpression) e);
    case ASTNode.BOOLEAN_LITERAL:
      return convert((BooleanLiteral) e);
    case ASTNode.SIMPLE_NAME:
      return convert((SimpleName) e);
    case ASTNode.PARENTHESIZED_EXPRESSION:
      return convertExpressionWithoutSideEffects(((ParenthesizedExpression) e).getExpression());
    }

    logger.log(Level.SEVERE, "Expression of typ "+ e.getNodeType() + " not implemented");

    return null;
  }

  private IASTNode convertExpressionWithSideEffects(Expression e) {

    //TODO Expression Implementation

    if (e == null) {
      return null;
    }

    switch(e.getNodeType()){
     case ASTNode.ASSIGNMENT:
       return convert((Assignment)e);
     default: return convert(e);
    }
  }





  private IASTIdExpression convert(SimpleName e) {
    String name = e.getIdentifier();
    IASTSimpleDeclaration declaration = scope.lookupVariable(name);
    if (declaration != null) {
      name = declaration.getName();
    }
    return new IASTIdExpression(getFileLocation(e), convert(e.resolveTypeBinding()), name, declaration);
  }



  private IASTNode convert(Assignment e) {

    IASTFileLocation fileLoc = getFileLocation(e);
    IType type = convert(e.resolveTypeBinding());
    IASTExpression leftHandSide = convertExpressionWithoutSideEffects(e.getLeftHandSide());

    BinaryOperator op = convert(e.getOperator());



    if (op == null) {
      // a = b
      IASTNode rightHandSide =  convertExpressionWithSideEffects(e.getRightHandSide()); // right-hand side may have a function call


      if (rightHandSide instanceof IASTExpression) {
        // a = b
        return new IASTExpressionAssignmentStatement(fileLoc, leftHandSide, (IASTExpression)rightHandSide);

      } else if (rightHandSide instanceof IASTFunctionCallExpression) {
        // a = f()
        return new IASTFunctionCallAssignmentStatement(fileLoc, leftHandSide, (IASTFunctionCallExpression)rightHandSide);

      } else if(rightHandSide instanceof IASTAssignment) {
        sideAssignments.add(rightHandSide);
        return new IASTExpressionAssignmentStatement(fileLoc, leftHandSide, ((IASTAssignment) rightHandSide).getLeftHandSide());
      } else {
        //TODO CFA Exception lacks ASTNode
        throw new CFAGenerationRuntimeException("Expression is not free of side-effects");
      }

    } else {
      // a += b etc.
      IASTExpression rightHandSide = convertExpressionWithoutSideEffects(e.getRightHandSide());

      // first create expression "a + b"
      IASTBinaryExpression exp = new IASTBinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);

      // and now the assignment
      return new IASTExpressionAssignmentStatement(fileLoc, leftHandSide, exp);
    }
  }


  private BinaryOperator convert(Assignment.Operator op) {


    //TODO implement eager operator Needs own class (Do enums  work here?)
    if(op.equals(Assignment.Operator.ASSIGN)){
      return null;
    } else if (op.equals(Assignment.Operator.BIT_AND_ASSIGN)) {
      return BinaryOperator.BINARY_AND;
    } else if (op.equals(Assignment.Operator.BIT_OR_ASSIGN)) {
      return BinaryOperator.BINARY_OR;
    } else if (op.equals(Assignment.Operator.BIT_XOR_ASSIGN)) {
      return BinaryOperator.BINARY_XOR;
    } else if (op.equals(Assignment.Operator.DIVIDE_ASSIGN)) {
      return BinaryOperator.DIVIDE;
    }else if (op.equals(Assignment.Operator.LEFT_SHIFT_ASSIGN)) {
      return BinaryOperator.SHIFT_LEFT;
    } else if (op.equals(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN)) {
      return BinaryOperator.SHIFT_RIGHT;
    } else if (op.equals(Assignment.Operator.MINUS_ASSIGN)) {
      return BinaryOperator.MINUS;
    } else if (op.equals(Assignment.Operator.PLUS_ASSIGN)) {
      return BinaryOperator.PLUS;
    } else if (op.equals(Assignment.Operator.REMAINDER_ASSIGN)) {
      return BinaryOperator.MODULO;
    } else if (op.equals(Assignment.Operator.TIMES_ASSIGN)) {
      return BinaryOperator.MULTIPLY;
    }else {
      logger.log(Level.SEVERE, "Did not find Operator");
      return null;
    }

  }




  private IASTExpression convert(BooleanLiteral e) {
    //TODO When new Naming and structure is ready, change this way of expressing true or false in 1 or 0.
    long bool;
    if(e.booleanValue()){
      bool = 1;
    }else {
      bool = 0;
    }
    return new IASTIntegerLiteralExpression( getFileLocation(e), convert(e.resolveTypeBinding()) , BigInteger.valueOf(Long.valueOf(bool )));
  }


  private IASTExpression convert(PrefixExpression e) {

    //TODO Implement Decrement and Increment

    IASTExpression operand = convertExpressionWithoutSideEffects(e.getOperand());
    IASTFileLocation fileLoc = getFileLocation(e);
    IType type = convert(e.resolveTypeBinding());

   return new IASTUnaryExpression(fileLoc, type, operand, convertUnaryOperator(e.getOperator()));

  }

private UnaryOperator convertUnaryOperator(PrefixExpression.Operator op) {

  //TODO implement eager operator Needs own class (Do enums  work here?)
  if(op.equals(PrefixExpression.Operator.NOT)){
    return UnaryOperator.NOT;
  } else if (op.equals(PrefixExpression.Operator.PLUS)) {
    return UnaryOperator.PLUS;
  } else if (op.equals(PrefixExpression.Operator.COMPLEMENT)) {
    return UnaryOperator.TILDE;
  } else if (op.equals(PrefixExpression.Operator.MINUS)) {
    return UnaryOperator.MINUS;
  }else {
    logger.log(Level.SEVERE, "Did not find Operator");
    return null;
  }

}

  @SuppressWarnings("unchecked")
  private IASTExpression convert(InfixExpression e) {
    IASTFileLocation fileLoc = getFileLocation(e);
    IType type = convert(e.resolveTypeBinding());
    IASTExpression leftHandSide = convertExpressionWithoutSideEffects(e.getLeftOperand());

    BinaryOperator op =   convertBinaryOperator(e.getOperator());

    IASTExpression rightHandSide = convertExpressionWithoutSideEffects(e.getRightOperand());

    IASTExpression binaryExpression = new IASTBinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);

    //TODO Here we could translate the Idea of extended Operands
    //TODO Maybe change tree Structure
    // into the cfa ast model
    // a x b x c is being translated to (((a x b) x c) x d)
    if (e.hasExtendedOperands()) {
      for (Expression extendedOperand : (List<Expression>) e.extendedOperands()) {
        binaryExpression = new IASTBinaryExpression(fileLoc, type, binaryExpression,
                                                 convertExpressionWithoutSideEffects(extendedOperand), op);
      }
    }

    return binaryExpression;
  }

  private BinaryOperator convertBinaryOperator(InfixExpression.Operator op) {

    //TODO implement eager operator Needs own class (Do enums  work here?)
    if(op.equals(InfixExpression.Operator.PLUS)){
      return BinaryOperator.PLUS;
    } else if (op.equals(InfixExpression.Operator.MINUS)) {
      return BinaryOperator.MINUS;
    } else if (op.equals(InfixExpression.Operator.DIVIDE)) {
      return BinaryOperator.DIVIDE;
    } else if (op.equals(InfixExpression.Operator.TIMES)) {
      return BinaryOperator.MULTIPLY;
    } else if (op.equals(InfixExpression.Operator.EQUALS)) {
      return BinaryOperator.EQUALS;
    }else if (op.equals(InfixExpression.Operator.REMAINDER)) {
      return BinaryOperator.MODULO;
    } else if (op.equals(InfixExpression.Operator.CONDITIONAL_AND)) {
      return BinaryOperator.LOGICAL_AND;
    } else if (op.equals(InfixExpression.Operator.CONDITIONAL_OR)) {
      return BinaryOperator.LOGICAL_OR;
    } else if (op.equals(InfixExpression.Operator.GREATER)) {
      return BinaryOperator.GREATER_THAN;
    } else if (op.equals(InfixExpression.Operator.LESS)) {
      return BinaryOperator.LESS_THAN;
    } else if (op.equals(InfixExpression.Operator.GREATER_EQUALS)) {
      return BinaryOperator.GREATER_EQUAL;
    } else if (op.equals(InfixExpression.Operator.LESS_EQUALS)) {
      return BinaryOperator.LESS_EQUAL;
    } else if (op.equals(InfixExpression.Operator.LEFT_SHIFT)) {
      return BinaryOperator.SHIFT_LEFT;
    }else if (op.equals(InfixExpression.Operator.RIGHT_SHIFT_SIGNED)) {
      return BinaryOperator.SHIFT_RIGHT;
    }else {
      logger.log(Level.SEVERE, "Did not find Operator");
      return null;
    }

  }

  private IASTExpression convert(NumberLiteral e) {
    IASTFileLocation fileLoc = getFileLocation(e);
    IType type = convert(e.resolveTypeBinding());
    String valueStr = e.getToken();

    //TODO only Prototype Solution
    BasicType t = ((IASTSimpleDeclSpecifier) type).getType();

    switch(t){
      case INT: return new IASTIntegerLiteralExpression(fileLoc, type, parseIntegerLiteral(valueStr, e));
    case FLOAT:
        return new IASTFloatLiteralExpression(fileLoc, type, parseFloatLiteral(valueStr, e));

    case DOUBLE:
        return new IASTFloatLiteralExpression(fileLoc, type, parseDoubleLiteral(valueStr, e));


    }

    return new IASTIntegerLiteralExpression( getFileLocation(e), convert(e.resolveTypeBinding()) , BigInteger.valueOf(Long.valueOf(e.getToken())));
  }


  private BigDecimal parseDoubleLiteral(String valueStr, NumberLiteral e) {
    // Double and Float Method is at the moment the same
    // This is a placeHolder Method
    return parseFloatLiteral( valueStr,  e);
  }

  private BigDecimal parseFloatLiteral(String valueStr, NumberLiteral e) {
    //TODO This assumes C mechanic, PlaceHolder till Java Conversion

    BigDecimal value;
    try {
      value = new BigDecimal(valueStr);
    } catch (NumberFormatException nfe1) {
      try {
        // this might be a hex floating point literal
        // BigDecimal doesn't support this, but Double does
        // TODO handle hex floating point literals that are too large for Double
        value = BigDecimal.valueOf(Double.parseDouble(valueStr));
      } catch (NumberFormatException nfe2) {
        throw new CFAGenerationRuntimeException("illegal floating point literal");
      }
    }

    return value;
  }


  BigInteger parseIntegerLiteral(String s, ASTNode e) {

    //TODO This assumes C mechanic, PlaceHolder till Java Conversion

    // this might have some modifiers attached (e.g. 0ULL), we have to get rid of them
    int last = s.length()-1;
    int bits = 32;
    boolean signed = true;

    if (s.charAt(last) == 'L' || s.charAt(last) == 'l' ) {
      last--;
      // one 'L' is equal to no 'L' (TODO this assumes a 32bit machine)
    }
    if (s.charAt(last) == 'L' || s.charAt(last) == 'l') {
      last--;
      bits = 64; // two 'L' are a long long
    }
    if (s.charAt(last) == 'U' || s.charAt(last) == 'u') {
      last--;
      signed = false;
    }

    s = s.substring(0, last+1);
    BigInteger result;
    try {
      if (s.startsWith("0x") || s.startsWith("0X")) {
        // this should be in hex format, remove "0x" from the string
        s = s.substring(2);
        result = new BigInteger(s, 16);

      } else if (s.startsWith("0")) {
        result = new BigInteger(s, 8);

      } else {
        result = new BigInteger(s, 10);
      }
    } catch (NumberFormatException _) {
      throw new CFAGenerationRuntimeException("invalid number");
    }
    check(result.compareTo(BigInteger.ZERO) >= 0, "invalid number", e);

    // clear the bits that don't fit in the type
    // a BigInteger with the lowest "bits" bits set to one (e. 2^32-1 or 2^64-1)
    BigInteger mask = BigInteger.ZERO.setBit(bits).subtract(BigInteger.ONE);
    result = result.and(mask);
    assert result.bitLength() <= bits;

    // compute twos complement if necessary
    if (signed && result.testBit(bits-1)) {
      // highest bit is set
      result = result.clearBit(bits-1);

      // a BigInteger for -2^(bits-1) (e.g. -2^-31 or -2^-63)
      BigInteger minValue = BigInteger.ZERO.setBit(bits-1).negate();

      result = minValue.add(result);
    }

    return result;
  }


  IASTStringLiteralExpression convert(StringLiteral e) {
    IASTFileLocation fileLoc = getFileLocation(e);

    //TODO Prototype , String is in java a class Type
    IType type = new DummyType("String");
    return new IASTStringLiteralExpression(fileLoc, type, e.getLiteralValue());
  }

  IASTStringLiteralExpression convert(NullLiteral e) {
    IASTFileLocation fileLoc = getFileLocation(e);

    //TODO Prototype , null has to be created as astType in object model
    IType type = new DummyType("null");
    return new IASTStringLiteralExpression(fileLoc, type, "null");
  }


  IASTCharLiteralExpression convert(CharacterLiteral e) {
    IASTFileLocation fileLoc = getFileLocation(e);
    IType type = convert(e.resolveTypeBinding());
    return new IASTCharLiteralExpression(fileLoc, type, e.charValue());
  }






  public IASTExpression convertBooleanExpression(Expression e){

    IASTExpression exp = convertExpressionWithoutSideEffects(e);
    if (!isBooleanExpression(exp)) {
      IASTExpression zero = new IASTIntegerLiteralExpression(exp.getFileLocation(), exp.getExpressionType(), BigInteger.ZERO);
      return new IASTBinaryExpression(exp.getFileLocation(), exp.getExpressionType(), exp, zero, BinaryOperator.NOT_EQUALS);
    }

    return exp;
  }

  private static final Set<BinaryOperator> BOOLEAN_BINARY_OPERATORS = ImmutableSet.of(
      BinaryOperator.EQUALS,
      BinaryOperator.NOT_EQUALS,
      BinaryOperator.GREATER_EQUAL,
      BinaryOperator.GREATER_THAN,
      BinaryOperator.LESS_EQUAL,
      BinaryOperator.LESS_THAN,
      BinaryOperator.LOGICAL_AND,
      BinaryOperator.LOGICAL_OR);

  private boolean isBooleanExpression(IASTExpression e) {
    if (e instanceof IASTBinaryExpression) {
      return BOOLEAN_BINARY_OPERATORS.contains(((IASTBinaryExpression)e).getOperator());

    } else if (e instanceof IASTUnaryExpression) {
      return ((IASTUnaryExpression) e).getOperator() == UnaryOperator.NOT;

    } else {
      return false;
    }
  }
}
