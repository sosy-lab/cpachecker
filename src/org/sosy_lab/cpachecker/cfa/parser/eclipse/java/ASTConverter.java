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
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression.Operator;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.CFileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.IAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanzeCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JConstructorDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JObjectReferenceReturn;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.CFAGenerationRuntimeException;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JConstructorType;
import org.sosy_lab.cpachecker.cfa.types.java.JDummyType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;


public class ASTConverter {

  private static final boolean NOT_GLOBAL = false;

  private static final boolean GLOBAL = true;

  private static final boolean NOT_FINAL = false;

  private final LogManager logger;

  private Scope scope;
  private LinkedList<IAstNode> preSideAssignments = new LinkedList<IAstNode>();
  private LinkedList<IAstNode> postSideAssignments = new LinkedList<IAstNode>();
  private ConditionalExpression conditionalExpression = null;
  private AIdExpression conditionalTemporaryVariable = null;

  public ASTConverter(Scope pScope, boolean pIgnoreCasts, LogManager pLogger) {
    scope = pScope;
    logger = pLogger;
  }

  public IAstNode getNextPostSideAssignment() {
    return postSideAssignments.removeFirst();
  }

  public int numberOfPostSideAssignments() {
    return postSideAssignments.size();
  }

  public int numberOfSideAssignments(){
    return preSideAssignments.size();
  }

  public IAstNode getNextSideAssignment() {
    return preSideAssignments.removeFirst();
  }

  public void resetConditionalExpression() {
    conditionalExpression = null;
  }

  public ConditionalExpression getConditionalExpression() {
    return conditionalExpression;
  }

  public AIdExpression getConditionalTemporaryVariable() {
    return conditionalTemporaryVariable;
  }

  public int numberOfPreSideAssignments() {
    return preSideAssignments.size();
  }

  private static void check(boolean assertion, String msg, ASTNode astNode) throws CFAGenerationRuntimeException {
    if (!assertion) {
      throw new CFAGenerationRuntimeException(msg);
    }
  }

  public IAstNode getNextPreSideAssignment() {
    return preSideAssignments.removeFirst();
  }


  public AFunctionDeclaration convert(final MethodDeclaration md) {

    @SuppressWarnings("unchecked")
    ModifierBean mb = ModifierBean.getModifiers(md.modifiers());

    @SuppressWarnings({ "cast", "unchecked" })
    List<AParameterDeclaration> param = convertParameterList((List<SingleVariableDeclaration>)md.parameters());


    CFileLocation fileLoc = getFileLocation(md);

    if(md.isConstructor()){


      JConstructorType type = new JConstructorType((JClassType) convert( md.resolveBinding().getDeclaringClass()), param , md.isVarargs());

      return new JConstructorDeclaration(fileLoc, type , getFullyQualifiedName(md.resolveBinding()) ,   mb.getVisibility(), mb.isStrictFp);

    } else {

    AFunctionType declSpec = new AFunctionType(convert(md.getReturnType2()) , param , md.isVarargs());

    return new JMethodDeclaration(fileLoc, declSpec, getFullyQualifiedName(md.resolveBinding()), mb.getVisibility(), mb.isFinal(),
        mb.isAbstract(), mb.isStatic(), mb.isNative(), mb.isSynchronized(), mb.isStrictFp());
    }
  }



  private String getFullyQualifiedName(IMethodBinding binding) {

    StringBuilder name;

    if(binding.getName().equals("main")){
      name = new StringBuilder(binding.getName().replace('.', '_'));
    } else {
      name = new StringBuilder((binding.getDeclaringClass().getQualifiedName().replace('.', '_') + "_" + binding.getName()).replace('.', '_'));
      ITypeBinding[] parameterTypes = binding.getParameterTypes();
      String[] typeNames = new String[parameterTypes.length];
      for(int c = 0; c < parameterTypes.length; c++) {
        typeNames[c] = parameterTypes[c].getQualifiedName().replace('.', '_');
      }


      if(typeNames.length > 0){
      name.append("_");
      }
      Joiner.on("_").appendTo( name , typeNames);


    }

    return name.toString();
  }

  private JType convert(Type t) {
    // TODO Not all Types implemented
    if (t.getNodeType() == ASTNode.PRIMITIVE_TYPE) {
      return convert((PrimitiveType)t);

    } else if(t.getNodeType() == ASTNode.ARRAY_TYPE){
      return convert((ArrayType)t);
    } else if(t.getNodeType() == ASTNode.QUALIFIED_TYPE ){
      return convert((QualifiedType)t);
    }else if(t.getNodeType() == ASTNode.SIMPLE_TYPE ){
      return convert((SimpleType)t);
    }else {
      return new JDummyType(t.resolveBinding().getName());
    }
  }

  private JClassType convert(QualifiedType t) {
    ITypeBinding binding = t.resolveBinding();
    ModifierBean mB = ModifierBean.getModifiers(binding);

    return new JClassType(binding.getQualifiedName().replace('.', '_'), mB.visibility, mB.isFinal, mB.isAbstract, mB.isStrictFp);
  }

  private JClassType convert(SimpleType t) {
    ITypeBinding binding = t.resolveBinding();
    ModifierBean mB = ModifierBean.getModifiers(binding);

    return new JClassType(binding.getQualifiedName().replace('.', '_'), mB.visibility, mB.isFinal, mB.isAbstract, mB.isStrictFp);
  }

  private JType convert(ITypeBinding t) {
    //TODO Needs to be completed

    if(t.isPrimitive()){
      return new JSimpleType( convertPrimitiveType(t.getName()));
    } else if(t.isArray()){
      return new JArrayType(  convert(t.getElementType())  , t.getDimensions());
    } else if(t.isClass()) {

      ModifierBean mB = ModifierBean.getModifiers(t);

      return new JClassType(t.getQualifiedName().replace('.', '_'), mB.getVisibility(), mB.isFinal, mB.isAbstract, mB.isStrictFp);
    }

    assert false : "Could Not Find Type";

    return null;
  }


  private JArrayType convert(final ArrayType t) {

    return new JArrayType(convert((t.getElementType())), t.getDimensions() );
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
  public CFileLocation getFileLocation (ASTNode l) {
    if (l == null) {
      return null;
    } else if(l.getRoot().getNodeType() != ASTNode.COMPILATION_UNIT){
      logger.log(Level.WARNING, "Can't find Placement Information for :" + l.toString());
    }

    //TODO See if this works for FileHierachys
    CompilationUnit co = (CompilationUnit) l.getRoot();


    // TODO Solve File Name Problem
    return new CFileLocation(   co.getLineNumber(l.getLength() + l.getStartPosition()),
                                     "Readable.java", l.getLength(), l.getStartPosition(),
                                       co.getLineNumber(l.getStartPosition()));
  }


  private  JSimpleType convert(final PrimitiveType t) {

        PrimitiveType.Code primitiveTypeName = t.getPrimitiveTypeCode();
        return new JSimpleType(convertPrimitiveType(primitiveTypeName.toString()));
  }

  private JBasicType convertPrimitiveType(String primitiveTypeName) {

    JBasicType type;
    if(primitiveTypeName.equals("boolean") ){
      type = JBasicType.BOOLEAN;
    } else if(primitiveTypeName.equals("char")) {
      type = JBasicType.CHAR;
    } else if(primitiveTypeName.equals("double")) {
      type = JBasicType.DOUBLE;
    } else if(primitiveTypeName.equals("float")) {
      type = JBasicType.FLOAT;
    } else if(primitiveTypeName.equals("int")) {
      type = JBasicType.INT;
    } else  if(primitiveTypeName.equals("void")) {
      type = JBasicType.VOID;
    } else if(primitiveTypeName.equals("long")) {
      type = JBasicType.LONG;
    } else if(primitiveTypeName.equals("short")) {
      type = JBasicType.SHORT;
    } else if(primitiveTypeName.equals("byte")) {
      type = JBasicType.BYTE;
    } else {
      throw new CFAGenerationRuntimeException("Unknown primitive type " + primitiveTypeName);
    }

    return type;
  }

  private List<AParameterDeclaration> convertParameterList(List<SingleVariableDeclaration> ps) {
    List<AParameterDeclaration> paramsList = new ArrayList<AParameterDeclaration>(ps.size());
    for (org.eclipse.jdt.core.dom.SingleVariableDeclaration c : ps) {
        paramsList.add(convertParameter(c));
    }
    return paramsList;
  }

  private AParameterDeclaration convertParameter(SingleVariableDeclaration p) {

    org.sosy_lab.cpachecker.cfa.types.Type type = convert(p.getType());

    return new AParameterDeclaration(getFileLocation(p), type, p.getName().getFullyQualifiedName().replace('.', '_'));
  }



  @SuppressWarnings("unchecked")
  public List<IADeclaration> convert(FieldDeclaration fd){

    List<IADeclaration> result = new ArrayList<IADeclaration>();

    Type type = fd.getType();

    CFileLocation fileLoc  = getFileLocation(fd);


    ModifierBean mB = ModifierBean.getModifiers(fd.modifiers());

    assert(!mB.isAbstract) : "Field Variable has this modifier?";
    assert(!mB.isNative) : "Field Variable has this modifier?";
    assert(!mB.isStrictFp) : "Field Variable has this modifier?";
    assert(!mB.isSynchronized) : "Field Variable has this modifier?";

    for( VariableDeclarationFragment vdf  : (List<VariableDeclarationFragment>)fd.fragments()){


      Triple<String , String , AInitializerExpression> nameAndInitializer = getNamesAndInitializer(vdf);


      result.add( new JFieldDeclaration(fileLoc, GLOBAL,
          convert(type) ,    nameAndInitializer.getFirst().replace('.', '.') ,nameAndInitializer.getSecond().replace('.', '.'),
             nameAndInitializer.getThird(),  mB.isFinal, mB.isStatic, mB.isTransient, mB.isVolatile , mB.getVisibility()));

    }
    return result;
}

   static class  ModifierBean {

     private final boolean isFinal;
     private final boolean isStatic;
     private final boolean isVolatile;
     private final boolean isTransient;
     private final boolean isNative;
     private final boolean isAbstract;
     private final boolean isStrictFp;
     private final boolean isSynchronized;
     private final VisibilityModifier visibility;

    public ModifierBean(boolean pIsFinal, boolean pIsStatic, boolean pIsVolatile, boolean pIsTransient,
        VisibilityModifier pVisibility , boolean pIsNative, boolean pIsAbstract, boolean pIsStrictFp,
        boolean pIsSynchronized) {

      visibility = pVisibility;
      isFinal = pIsFinal;
      isStatic = pIsStatic;
      isVolatile = pIsVolatile;
      isTransient = pIsTransient;
      isNative = pIsNative;
      isAbstract = pIsAbstract;
      isStrictFp = pIsStrictFp;
      isSynchronized = pIsSynchronized;
    }

    public static ModifierBean getModifiers(ITypeBinding pBinding) {


      // This int value is the bit-wise or of Modifier constants
      int modifiers = pBinding.getModifiers();

      assert pBinding.isClass() || pBinding.isEnum()
      || pBinding.isInterface() || pBinding.isAnnotation()
      || pBinding.isRecovered(): "This type can't have modifiers";

      VisibilityModifier visibility = null;
      boolean isFinal = false;
      boolean isStatic = false;
      boolean isVolatile = false;
      boolean isTransient = false;
      boolean isNative = false;
      boolean isAbstract = false;
      boolean isStrictFp = false;
      boolean isSynchronized = false;



      // Check all possible bit constants
      for (int bitMask = 1; bitMask < 2049; bitMask = bitMask << 1 ) {


        // Check if n-th bit of modifiers is 1
          switch (modifiers & bitMask) {

          case Modifier.FINAL:
            isFinal = true;
            break;
          case Modifier.STATIC:
            isStatic = true;
            break;
          case Modifier.VOLATILE:
            isVolatile = true;
            break;
          case Modifier.TRANSIENT:
            isTransient = true;
            break;
          case Modifier.PUBLIC:
            assert visibility == null :  "Can only declare one Visibility Modifier";
            visibility = VisibilityModifier.PUBLIC;
            break;
          case Modifier.PROTECTED:
            assert visibility == null : "Can only declare one Visibility Modifier";
            visibility = VisibilityModifier.PROTECTED;
            break;
          case Modifier.PRIVATE:
            assert visibility == null : "Can only declare one Visibility Modifier";
            visibility = VisibilityModifier.PRIVATE;
            break;
          case Modifier.NATIVE:
            isNative = true;
            break;
          case Modifier.ABSTRACT:
            isAbstract = true;
            break;
          case Modifier.STRICTFP:
            isStrictFp = true;
            break;
          case Modifier.SYNCHRONIZED:
            isSynchronized = true;
            break;
          }

        }

      // If no Visibility Modifier is selected, it is None
      if(visibility == null){
        visibility = VisibilityModifier.NONE;
      }


      return new ModifierBean(isFinal, isStatic, isVolatile, isTransient, visibility,
          isNative, isAbstract, isStrictFp, isSynchronized);

    }

    private static ModifierBean getModifiers(List<IExtendedModifier> modifiers) {

      VisibilityModifier visibility = null;
      boolean isFinal = false;
      boolean isStatic = false;
      boolean isVolatile = false;
      boolean isTransient = false;
      boolean isNative = false;
      boolean isAbstract = false;
      boolean isStrictFp = false;
      boolean isSynchronized = false;

      for (IExtendedModifier modifier : modifiers) {

        if (modifier.isModifier()) {
          ModifierKeyword modifierEnum = ((Modifier) modifier).getKeyword();

          switch (modifierEnum.toFlagValue()) {

          case Modifier.FINAL:
            isFinal = true;
            break;
          case Modifier.STATIC:
            isStatic = true;
            break;
          case Modifier.VOLATILE:
            isVolatile = true;
            break;
          case Modifier.TRANSIENT:
            isTransient = true;
            break;
          case Modifier.PUBLIC:
            assert visibility == null :  "Can only declare one Visibility Modifier";
            visibility = VisibilityModifier.PUBLIC;
            break;
          case Modifier.PROTECTED:
            assert visibility == null : "Can only declare one Visibility Modifier";
            visibility = VisibilityModifier.PROTECTED;
            break;
          case Modifier.NONE:
            assert visibility == null : "Can only declare one Visibility Modifier";
            visibility = VisibilityModifier.NONE;
            break;
          case Modifier.PRIVATE:
            assert visibility == null : "Can only declare one Visibility Modifier";
            visibility = VisibilityModifier.PRIVATE;
            break;
          case Modifier.NATIVE:
            isNative = true;
            break;
          case Modifier.ABSTRACT:
            isAbstract = true;
            break;
          case Modifier.STRICTFP:
            isStrictFp = true;
            break;
          case Modifier.SYNCHRONIZED:
            isSynchronized = true;
            break;
          default:
            assert false : " Unkown  Modifier";

          }

        }
      }
      return new ModifierBean(isFinal, isStatic, isVolatile, isTransient, visibility,
          isNative, isAbstract, isStrictFp, isSynchronized);

    }

    public VisibilityModifier getVisibility(){
      return visibility;
    }

    public boolean isFinal() {
      return isFinal;
    }



    public boolean isStatic() {
      return isStatic;
    }



    public boolean isVolatile() {
      return isVolatile;
    }



    public boolean isTransient() {
      return isTransient;
    }

    public boolean isNative() {
      return isNative;
    }



    public boolean isAbstract() {
      return isAbstract;
    }



    public boolean isStrictFp() {
      return isStrictFp;
    }



    public boolean isSynchronized() {
      return isSynchronized;
    }
  }

  private Triple<String , String , AInitializerExpression> getNamesAndInitializer(VariableDeclarationFragment d ) {

    AInitializerExpression initializerExpression = null;

    // If there is no Initializer, JVariableDeclaration expects null to be given.
    if(d.getInitializer() != null){
      initializerExpression = new AInitializerExpression( getFileLocation(d) ,  convertExpressionWithoutSideEffects(d.getInitializer()));
    }


    String name = getFullyQualifiedName(d.resolveBinding());

    return new Triple<String , String ,AInitializerExpression>( name, name, initializerExpression);
  }


  private String getFullyQualifiedName(IVariableBinding vb){
    StringBuilder name = new StringBuilder();

    // Field Variable are declared with Declaring class before Identifier
    // Classname.var
    name.append( vb.getDeclaringClass() != null ? vb.getDeclaringClass().getName() + "." : "");
    name.append(vb.getName());

    return name.toString();

  }


  @SuppressWarnings("unchecked")
  public List<IADeclaration> convert(VariableDeclarationStatement vds){


    List<IADeclaration> variableDeclarations = new ArrayList<IADeclaration>();

    @SuppressWarnings("cast")
    List<VariableDeclarationFragment> variableDeclarationFragments =
                                            (List<VariableDeclarationFragment>)vds.fragments();

    CFileLocation fileLoc = getFileLocation(vds);
    Type type = vds.getType();


    ModifierBean mB = ModifierBean.getModifiers(vds.modifiers());

   assert(!mB.isAbstract) : "Local Variable has abstract modifier?";
   assert(!mB.isNative) : "Local Variable has native modifier?";
   assert(mB.visibility == null) : "Local Variable has Visibility modifier?";
   assert(!mB.isStatic) : "Local Variable has static modifier?";
   assert(!mB.isStrictFp) : "Local Variable has strictFp modifier?";
   assert(!mB.isSynchronized) : "Local Variable has synchronized modifier?";



    for(VariableDeclarationFragment vdf : variableDeclarationFragments){

      Triple<String , String , AInitializerExpression> nameAndInitializer = getNamesAndInitializer(vdf);

        variableDeclarations.add( new JVariableDeclaration(fileLoc, NOT_GLOBAL,
            convert(type) ,    nameAndInitializer.getFirst() ,nameAndInitializer.getSecond()   , nameAndInitializer.getThird() ,  mB.isFinal));
    }

    return variableDeclarations;
  }


  public JVariableDeclaration convert(SingleVariableDeclaration d) {


    Type type = d.getType();

    @SuppressWarnings("unchecked")
    ModifierBean mB = ModifierBean.getModifiers(d.modifiers());

    assert(!mB.isAbstract) : "Local Variable has abstract modifier?";
    assert(!mB.isNative) : "Local Variable has native modifier?";
    assert(mB.visibility != null) : "Local Variable has Visibility modifier?";
    assert(!mB.isStatic) : "Local Variable has static modifier?";
    assert(!mB.isStrictFp) : "Local Variable has strictFp modifier?";
    assert(!mB.isSynchronized) : "Local Variable has synchronized modifier?";

    AInitializerExpression initializerExpression = null;

    // If there is no Initializer, CStorageClass expects null to be given.
    if (d.getInitializer() != null) {
      initializerExpression =
          new AInitializerExpression(getFileLocation(d),
              (IAExpression) convertExpressionWithSideEffects(d.getInitializer()));
    }

    return new JVariableDeclaration(getFileLocation(d), NOT_GLOBAL,
        convert(type), d.getName().getFullyQualifiedName().replace('.', '_'), d.getName().getFullyQualifiedName().replace('.', '_'), initializerExpression,
        mB.isFinal);
  }


  public AReturnStatement convert(final ReturnStatement s) {
    return new AReturnStatement(getFileLocation(s), convertExpressionWithoutSideEffects(s.getExpression()));
  }


  public JExpression convertExpressionWithoutSideEffects( Expression e) {

    IAstNode node = convertExpressionWithSideEffects(e);

    if (node == null || node instanceof JExpression) {
      return  (JExpression) node;

    } else if (node instanceof AFunctionCallExpression) {
      return addSideassignmentsForExpressionsWithoutSideEffects(node, e);

    } else if (node instanceof AAssignment) {
      preSideAssignments.add(node);
      return (JExpression) ((AAssignment) node).getLeftHandSide();

    } else {
      throw new AssertionError("unknown expression " + node);
    }
  }

  private JExpression addSideassignmentsForExpressionsWithoutSideEffects(IAstNode node,Expression e) {
    AIdExpression tmp = createTemporaryVariable(e);

    //TODO Investigate if FileLoction is instanced
    preSideAssignments.add(new AFunctionCallAssignmentStatement( node.getFileLocation(),
        tmp,
        (AFunctionCallExpression) node));
    return tmp;
  }

  /**
   * creates temporary variables with increasing numbers
   */
  private AIdExpression createTemporaryVariable(Expression e) {
    String name = "__CPAchecker_TMP_";
    int i = 0;
    while(scope.variableNameInUse(name+i, name+i)){
      i++;
    }
    name += i;

    JVariableDeclaration decl = new JVariableDeclaration(getFileLocation(e),
                                               NOT_GLOBAL,
                                               convert(e.resolveTypeBinding()),
                                               name,
                                               name,
                                               null, NOT_FINAL);

    scope.registerDeclaration(decl);
    preSideAssignments.add(decl);
    AIdExpression tmp = new AIdExpression(decl.getFileLocation(),
                                                convert(e.resolveTypeBinding()),
                                                name,
                                                decl);
    return tmp;
  }


  public IAStatement convert(final ExpressionStatement s) {

    IAstNode node = convertExpressionWithSideEffects(s.getExpression());

    if (node instanceof AExpressionAssignmentStatement) {
      return (AExpressionAssignmentStatement)node;

    } else if (node instanceof AFunctionCallAssignmentStatement) {
      return (AFunctionCallAssignmentStatement)node;

    } else if (node instanceof AFunctionCallExpression) {
      return new AFunctionCallStatement( getFileLocation(s) , (AFunctionCallExpression)node);

    } else if (node instanceof IAExpression) {
      return new AExpressionStatement(  getFileLocation(s) , (IAExpression)node);

    } else {
      throw new AssertionError();
    }
  }


  public IAstNode convertExpressionWithSideEffects(Expression e) {

    //TODO  All Expression Implementation

    if (e == null) {
      return null;
    }

    switch(e.getNodeType()){
       case ASTNode.ASSIGNMENT:
         return convert((Assignment)e);
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
       case ASTNode.POSTFIX_EXPRESSION:
         return convert((PostfixExpression) e);
       case ASTNode.QUALIFIED_NAME:
         return convert((QualifiedName )e );
       case ASTNode.BOOLEAN_LITERAL:
         return convert((BooleanLiteral) e);
       case ASTNode.FIELD_ACCESS:
        return  convert((FieldAccess) e);
       case ASTNode.SIMPLE_NAME:
         return convert((SimpleName) e);
       case ASTNode.PARENTHESIZED_EXPRESSION:
         return convertExpressionWithoutSideEffects(((ParenthesizedExpression) e).getExpression());
       case ASTNode.METHOD_INVOCATION:
         return convert((MethodInvocation)e);
       case ASTNode.CLASS_INSTANCE_CREATION:
         return convert((ClassInstanceCreation)e);
       case ASTNode.ARRAY_ACCESS:
         return convert( (ArrayAccess) e);
       case ASTNode.ARRAY_CREATION:
         return convert( (ArrayCreation) e);
       case ASTNode.ARRAY_INITIALIZER :
         return convert((ArrayInitializer)e);
       case ASTNode.CONDITIONAL_EXPRESSION :
         return convert( (ConditionalExpression)e);
       case ASTNode.THIS_EXPRESSION:
         assert false : "Expression must be handled in parent node"; return null;
    }

       logger.log(Level.SEVERE, "Expression of typ "+  AstErrorChecker.getTypeName(e.getNodeType()) + " not implemented");
       return null;
  }

  private IAstNode convert(FieldAccess e) {
    scope.registerClasses(e.resolveFieldBinding().getDeclaringClass());

    // This Expression can be ignored, is solved through resolve Binding
    // and does'nt need to be included in the cfa
    if(e.getExpression().getNodeType() == ASTNode.THIS_EXPRESSION){
      return convertExpressionWithoutSideEffects(e.getName());
    } else {
      return convertExpressionWithoutSideEffects(e.getExpression());
    }

  }

  private IAstNode convert(ClassInstanceCreation cIC) {

    scope.registerClasses(cIC.resolveConstructorBinding().getDeclaringClass());



    @SuppressWarnings("unchecked")
    List<Expression> p = cIC.arguments();

    List<JExpression> params;

    if (p.size() > 0) {
      params = convert(p);

    } else {
      params = new ArrayList<JExpression>();
    }

    String name = getFullyQualifiedName(cIC.resolveConstructorBinding());
    JConstructorDeclaration declaration = (JConstructorDeclaration) scope.lookupFunction(name);

    //TODO Investigate if type Right
    IAExpression functionName = new AIdExpression(getFileLocation(cIC), convert(cIC.resolveTypeBinding()), name,  declaration);
    AIdExpression idExpression = (AIdExpression)functionName;

      if (idExpression.getDeclaration() != null) {
        // clone idExpression because the declaration in it is wrong
        // (it's the declaration of an equally named variable)
        // TODO this is ugly

        functionName = new AIdExpression(idExpression.getFileLocation(), idExpression.getExpressionType(), name, declaration);
      }


    return new JClassInstanzeCreation(getFileLocation(cIC),null , (JExpression) functionName, params, declaration);
  }

  private IAstNode convert(ConditionalExpression e) {
    AIdExpression tmp = createTemporaryVariable(e);
    conditionalTemporaryVariable = tmp;
    conditionalExpression = e;
    return tmp;
  }




  private IAstNode convert(ArrayInitializer initializer) {

    if(initializer == null){
      return null;
    }

    JArrayType type = (JArrayType) convert(initializer.resolveTypeBinding());
    List<JExpression> initializerExpressions = new ArrayList<JExpression>();

    @SuppressWarnings("unchecked")
    List<Expression> expressions = initializer.expressions();

    for( Expression  exp : expressions){
      initializerExpressions.add( convertExpressionWithoutSideEffects(exp));
    }


    return new JArrayInitializer(getFileLocation(initializer), initializerExpressions, type);
  }

  private IAstNode convert(ArrayCreation Ace) {



    CFileLocation fileloc = getFileLocation(Ace);
    JArrayInitializer initializer = (JArrayInitializer) convertExpressionWithoutSideEffects(Ace.getInitializer());
    JArrayType type = convert(Ace.getType());
    List<JExpression> length = new ArrayList<JExpression>(type.getDimensions());



    @SuppressWarnings("unchecked")
    List<Expression> dim = Ace.dimensions();

    if(initializer != null){
      for(int dimension = 0; dimension < type.getDimensions(); dimension++){
        //TODO find way to correctly calculate size
        length.add(new JIntegerLiteralExpression(fileloc, new JSimpleType(JBasicType.INT), BigInteger.valueOf(100)));
      }
    } else {

      for(Expression exp : dim){
        length.add(convertExpressionWithoutSideEffects(exp));
      }

    }

    return new JArrayCreationExpression(fileloc ,
                                               type, initializer , length );
  }

  private IAstNode convert(ArrayAccess e) {
    return new AArraySubscriptExpression(getFileLocation(e), convert(e.resolveTypeBinding()), convertExpressionWithoutSideEffects(e.getArray()), convertExpressionWithoutSideEffects(e.getIndex()));
  }

  private IAstNode convert(QualifiedName e) {

    String name;
    IASimpleDeclaration declaration = null;


    if(e.resolveBinding() instanceof IMethodBinding ){
      name = (((IMethodBinding) e.resolveBinding()).getDeclaringClass().getQualifiedName() + "_" + e.resolveBinding().getName()).replace('.', '_');
    } else {
      name = e.getFullyQualifiedName().replace('.', '_');
       declaration = scope.lookupVariable(name);

    }



    if (declaration != null) {
      name = declaration.getName();
    }
    return new AIdExpression(getFileLocation(e), convert(e.resolveTypeBinding()), name, declaration);
  }

  @SuppressWarnings({ "unchecked", "cast" })
  private IAstNode convert(MethodInvocation mi) {

    scope.registerClasses(mi.resolveMethodBinding().getDeclaringClass());

    List<Expression> p = mi.arguments();

    List<JExpression> params;
    if (p.size() > 0) {
      params = convert((List<Expression>)p);

    } else {
      params = new ArrayList<JExpression>();
    }

    IAExpression functionName = convertExpressionWithoutSideEffects(mi.getName());
    IASimpleDeclaration declaration = null;


    if (functionName instanceof AIdExpression) {
      AIdExpression idExpression = (AIdExpression)functionName;
      String name = idExpression.getName();
      declaration = scope.lookupFunction( name);

      if (idExpression.getDeclaration() != null) {
        // clone idExpression because the declaration in it is wrong
        // or the method not yet parsed
        // (it's the declaration of an equally named variable
        //   or the method is part of another class )
        // TODO this is ugly

        functionName = new AIdExpression(idExpression.getFileLocation(), idExpression.getExpressionType(), name, declaration);
      }
    }

    return new AFunctionCallExpression(getFileLocation(mi), convert(mi.resolveTypeBinding()), functionName, params, declaration);
  }

  private List<JExpression> convert(List<Expression> el) {

    List<JExpression> result = new ArrayList<JExpression>(el.size());
    for (Expression expression : el) {
      result.add(convertExpressionWithoutSideEffects(expression));
    }
    return result;
  }

  private AIdExpression convert(SimpleName e) {


    //TODO Complete declaration by finding all Bindings

    IBinding binding = e.resolveBinding();

    String name;
    IASimpleDeclaration declaration = null;

    if(binding instanceof IVariableBinding){
    name = getFullyQualifiedName((IVariableBinding) binding);
    declaration = scope.lookupVariable(name);
    }else if(binding instanceof IMethodBinding ){
      name = getFullyQualifiedName((IMethodBinding) binding);
    } else {
      name = e.getIdentifier();
    }

    return new AIdExpression(getFileLocation(e), convert(e.resolveTypeBinding()), name, declaration);
  }



  private IAstNode convert(Assignment e) {

    CFileLocation fileLoc = getFileLocation(e);
    org.sosy_lab.cpachecker.cfa.types.Type type = convert(e.resolveTypeBinding());
    IAExpression leftHandSide = convertExpressionWithoutSideEffects(e.getLeftHandSide());

    BinaryOperator op = convert(e.getOperator());

    if (op == null) {
      // a = b
      IAstNode rightHandSide =  convertExpressionWithSideEffects(e.getRightHandSide()); // right-hand side may have a function call


      if (rightHandSide instanceof IAExpression) {
        // a = b
        return new AExpressionAssignmentStatement(fileLoc, leftHandSide, (IAExpression)rightHandSide);

      } else if (rightHandSide instanceof AFunctionCallExpression) {
        // a = f()
        return new AFunctionCallAssignmentStatement(fileLoc, leftHandSide, (AFunctionCallExpression)rightHandSide);

      } else if(rightHandSide instanceof AAssignment) {
        preSideAssignments.add(rightHandSide);
        return new AExpressionAssignmentStatement(fileLoc, leftHandSide, ((AAssignment) rightHandSide).getLeftHandSide());
      } else {
        //TODO CFA Exception lacks ASTNode
        throw new CFAGenerationRuntimeException("Expression is not free of side-effects");
      }

    } else {
      // a += b etc.
      IAExpression rightHandSide = convertExpressionWithoutSideEffects(e.getRightHandSide());

      // first create expression "a + b"
      ABinaryExpression exp = new ABinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);

      // and now the assignment
      return new AExpressionAssignmentStatement(fileLoc, leftHandSide, exp);
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

  private IAExpression convert(BooleanLiteral e) {

    return new JBooleanLiteralExpression( getFileLocation(e), convert(e.resolveTypeBinding()) , e.booleanValue());
  }


  private IAstNode convert(PrefixExpression e) {

    PrefixExpression.Operator op = e.getOperator();

    if (op.equals(PrefixExpression.Operator.INCREMENT)
        || op.equals(PrefixExpression.Operator.DECREMENT)) {
     return handlePreFixIncOrDec(e, op);
    }

    IAExpression operand = convertExpressionWithoutSideEffects(e.getOperand());
    CFileLocation fileLoc = getFileLocation(e);
    org.sosy_lab.cpachecker.cfa.types.Type type = convert(e.resolveTypeBinding());

    return new AUnaryExpression(fileLoc, type, operand, convertUnaryOperator(op));
  }

  private IAstNode convert(PostfixExpression e) {
    PostfixExpression.Operator op = e.getOperator();
    return  handlePostFixIncOrDec(e, op);
  }


  private IAstNode handlePostFixIncOrDec(PostfixExpression e, PostfixExpression.Operator op) {

    BinaryOperator postOp = null;
    if (op.equals(PostfixExpression.Operator.INCREMENT)) {

      postOp = BinaryOperator.PLUS;
    } else if (op.equals(PostfixExpression.Operator.DECREMENT)) {
      postOp = BinaryOperator.MINUS;
    }
    assert postOp != null : "Increment/Decrement Severe Error.";

    if (e.getParent() instanceof Expression) {
      new CFAGenerationRuntimeException("Side assignments with Increment/Decrement not yet implemented");
      return null;
    } else {

      CFileLocation fileLoc = getFileLocation(e);
      JType type = convert(e.resolveTypeBinding());
      IAExpression operand = convertExpressionWithoutSideEffects(e.getOperand());

      IAExpression preOne = new JIntegerLiteralExpression(fileLoc, type, BigInteger.ONE);
      ABinaryExpression preExp = new ABinaryExpression(fileLoc, type, operand, preOne, postOp);
      return new AExpressionAssignmentStatement(fileLoc, operand, preExp);
    }
  }

  private IAstNode handlePreFixIncOrDec(PrefixExpression e, Operator op) {

    BinaryOperator preOp = null;
    if (op.equals(PrefixExpression.Operator.INCREMENT)) {

      preOp = BinaryOperator.PLUS;
    } else if (op.equals(PrefixExpression.Operator.DECREMENT)) {
      preOp = BinaryOperator.MINUS;
    }
    assert preOp != null : "Increment/Decrement Severe Error.";


    if (e.getParent() instanceof Expression) {
      new CFAGenerationRuntimeException("Side assignments with Increment/Decrement not yet implemented");
      return null;
    } else {

      CFileLocation fileLoc = getFileLocation(e);
      JType type = convert(e.resolveTypeBinding());
      IAExpression operand = convertExpressionWithoutSideEffects(e.getOperand());

      IAExpression preOne = new JIntegerLiteralExpression(fileLoc, type, BigInteger.ONE);
      ABinaryExpression preExp = new ABinaryExpression(fileLoc, type, operand, preOne, preOp);
      return new AExpressionAssignmentStatement(fileLoc, operand, preExp);
    }
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
  private IAExpression convert(InfixExpression e) {
    CFileLocation fileLoc = getFileLocation(e);
    org.sosy_lab.cpachecker.cfa.types.Type type = convert(e.resolveTypeBinding());
    IAExpression leftHandSide = convertExpressionWithoutSideEffects(e.getLeftOperand());

    BinaryOperator op =   convertBinaryOperator(e.getOperator());

    IAExpression rightHandSide = convertExpressionWithoutSideEffects(e.getRightOperand());

    IAExpression binaryExpression = new ABinaryExpression(fileLoc, type, leftHandSide, rightHandSide, op);

    //TODO Here we could translate the Idea of extended Operands
    //Maybe change tree Structure
    // into the cfa ast model
    // a x b x c is being translated to (((a x b) x c) x d)
    if (e.hasExtendedOperands()) {
      for (Expression extendedOperand : (List<Expression>) e.extendedOperands()) {
        binaryExpression = new ABinaryExpression(fileLoc, type, binaryExpression,
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
    } else if (op.equals(InfixExpression.Operator.RIGHT_SHIFT_SIGNED)) {
      return BinaryOperator.SHIFT_RIGHT;
    } else if(op.equals(InfixExpression.Operator.NOT_EQUALS)){
     return BinaryOperator.NOT_EQUALS;
    } else {
      logger.log(Level.SEVERE, "Did not find Operator");
      return null;
    }

  }

  private IAExpression convert(NumberLiteral e) {
    CFileLocation fileLoc = getFileLocation(e);
    JType type = convert(e.resolveTypeBinding());
    String valueStr = e.getToken();

    JBasicType t = ((JSimpleType) type).getType();

    switch(t){
    case INT:   return new JIntegerLiteralExpression(fileLoc, type, parseIntegerLiteral(valueStr, e ) );
    case FLOAT:  return new JFloatLiteralExpression(fileLoc, type, parseFloatLiteral(valueStr, e));

    case DOUBLE: return new JFloatLiteralExpression(fileLoc, type, parseFloatLiteral(valueStr, e));

    }

    return new JIntegerLiteralExpression( getFileLocation(e), convert(e.resolveTypeBinding()) , BigInteger.valueOf(Long.valueOf(e.getToken())));
  }




  private BigDecimal parseFloatLiteral(String valueStr, NumberLiteral e) {

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


 private BigInteger parseIntegerLiteral(String s, ASTNode e) {
    int last = s.length()-1;
    int bits = 32;

    if (s.charAt(last) == 'L' || s.charAt(last) == 'l' ) {
      last--;
      bits = 64;
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

      } else if (s.startsWith("0b || 0B")) {
        result = new BigInteger(s, 2);

      }else {
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

    return result;
  }


  AStringLiteralExpression convert(StringLiteral e) {
    CFileLocation fileLoc = getFileLocation(e);

    //TODO Prototype , String is in java a class Type
    JType type = new JDummyType("String");
    return new AStringLiteralExpression(fileLoc, type, e.getLiteralValue());
  }

  AStringLiteralExpression convert(NullLiteral e) {
    CFileLocation fileLoc = getFileLocation(e);

    //TODO Prototype , null has to be created as astType in object model
    JType type = new JDummyType("null");
    return new AStringLiteralExpression(fileLoc, type, "null");
  }


  ACharLiteralExpression convert(CharacterLiteral e) {
    CFileLocation fileLoc = getFileLocation(e);
     org.sosy_lab.cpachecker.cfa.types.Type type = convert(e.resolveTypeBinding());
    return new ACharLiteralExpression(fileLoc, type, e.charValue());
  }

  public IAExpression convertBooleanExpression(Expression e){

    JExpression exp = convertExpressionWithoutSideEffects(e);
    if (!isBooleanExpression(exp)) {
      IAExpression zero = new JBooleanLiteralExpression(exp.getFileLocation(), (JType) exp.getExpressionType(), false);
      return new ABinaryExpression(exp.getFileLocation(), exp.getExpressionType(), exp, zero, BinaryOperator.NOT_EQUALS);
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

  private static final int NO_LINE = 0;

  private boolean isBooleanExpression(IAExpression e) {
    if (e instanceof ABinaryExpression) {
      return BOOLEAN_BINARY_OPERATORS.contains(((ABinaryExpression)e).getOperator());

    } else if (e instanceof AUnaryExpression) {
      return ((AUnaryExpression) e).getOperator() == UnaryOperator.NOT;

    } else if(e instanceof JBooleanLiteralExpression){
      return true;
    } else{
      return false;
    }
  }

  public AReturnStatement getConstructorObjectReturn(MethodDeclaration constructor) {

    assert constructor.isConstructor() : "Method " + constructor.toString() + " is no Contructor";

    CFileLocation constructorFileLoc = getFileLocation(constructor);

    CFileLocation fileloc= new CFileLocation(constructorFileLoc.getEndingLineNumber(), constructorFileLoc.getFileName(), NO_LINE, constructorFileLoc.getEndingLineNumber(), constructorFileLoc.getEndingLineNumber());

    JClassType objectReturnType = (JClassType) convert(constructor.resolveBinding().getDeclaringClass());

    //TODO Ugly, change AStringLiteral, maybe complete new Type for Object return
    return new JObjectReferenceReturn(fileloc, new AStringLiteralExpression(fileloc, objectReturnType, objectReturnType.getName()), objectReturnType);
  }
}
