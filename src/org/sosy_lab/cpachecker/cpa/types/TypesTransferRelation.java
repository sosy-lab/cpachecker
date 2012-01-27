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
package org.sosy_lab.cpachecker.cpa.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.IASTArrayTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCompositeTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTElaboratedTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNamedTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTPointerTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.types.Type.ArrayType;
import org.sosy_lab.cpachecker.cpa.types.Type.CompositeType;
import org.sosy_lab.cpachecker.cpa.types.Type.EnumType;
import org.sosy_lab.cpachecker.cpa.types.Type.FunctionType;
import org.sosy_lab.cpachecker.cpa.types.Type.PointerType;
import org.sosy_lab.cpachecker.cpa.types.Type.Primitive;
import org.sosy_lab.cpachecker.cpa.types.Type.PrimitiveType;
import org.sosy_lab.cpachecker.cpa.types.Type.StructType;
import org.sosy_lab.cpachecker.cpa.types.Type.UnionType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

public class TypesTransferRelation implements TransferRelation {

  private FunctionDefinitionNode entryFunctionDefinitionNode = null;
  private boolean entryFunctionProcessed = false;

  @Override
  public Collection<TypesElement> getAbstractSuccessors(
                                              AbstractElement element,
                                              Precision precision,
                                              CFAEdge cfaEdge)
                                              throws CPATransferException {
    // no need to clone as type information is global
    TypesElement successor = (TypesElement)element;

    switch (cfaEdge.getEdgeType()) {
    case DeclarationEdge:
      handleDeclaration(successor, (DeclarationEdge)cfaEdge);
      break;

    case FunctionCallEdge:
      FunctionCallEdge funcCallEdge = (FunctionCallEdge)cfaEdge;
      FunctionDefinitionNode funcDefNode = funcCallEdge.getSuccessor();
      if (successor.getFunction(funcDefNode.getFunctionName()) == null) {
        // we call a function that was not defined
        // probably "analysis.useFunctionDeclarations" is false
        // this is not bad, but we don't get type information for external
        // function

        IASTFunctionDeclaration funcDef = funcDefNode.getFunctionDefinition();
        handleFunctionDeclaration(successor, funcCallEdge,
            funcDef.getDeclSpecifier());
      }
      break;

    case AssumeEdge:
    case StatementEdge:
    case ReturnStatementEdge:
    case FunctionReturnEdge:
      break;
    case BlankEdge:
      //the first function start dummy edge is the actual start of the entry function
      if (!entryFunctionProcessed
          && (cfaEdge.getPredecessor() instanceof CFAFunctionDefinitionNode)) {
        //since by this point all global variables have been processed, we can now process the entry function
        IASTFunctionDeclaration funcDef = entryFunctionDefinitionNode.getFunctionDefinition();
        handleFunctionDeclaration(successor, null, funcDef.getDeclSpecifier());

        entryFunctionProcessed = true;
      }
      break;

    default:
      throw new UnrecognizedCFAEdgeException(cfaEdge);
    }

    return Collections.singleton(successor);
  }

  private void handleDeclaration(TypesElement element,
                                 DeclarationEdge declarationEdge)
                                 throws UnrecognizedCCodeException {
    IASTDeclaration decl = declarationEdge.getDeclaration();
    IType specifier = declarationEdge.getDeclSpecifier();

    if (decl instanceof IASTFunctionDeclaration) {
      handleFunctionDeclaration(element, declarationEdge, (IASTFunctionTypeSpecifier)specifier);

    } else {
      Type type = getType(element, declarationEdge, specifier);

      if (decl instanceof IASTTypeDefDeclaration) {
        element.addTypedef(decl.getName(), type);

      } else if (decl instanceof IASTVariableDeclaration) {

        String functionName = null;
        if (!(decl.isGlobal())) {
          functionName = declarationEdge.getSuccessor().getFunctionName();
        }

        element.addVariable(functionName, decl.getName(), type);
      }
    }
  }

  private void handleFunctionDeclaration(TypesElement element,
                                        CFAEdge cfaEdge,
                                        IASTFunctionTypeSpecifier funcDeclSpecifier)
                                        throws UnrecognizedCCodeException {

    FunctionType function = getType(element, cfaEdge, funcDeclSpecifier);

    if (cfaEdge != null && cfaEdge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
      assert function.getName().equals(cfaEdge.getSuccessor().getFunctionName());
    }

    element.addFunction(function.getName(), function);
  }

  private Type getType(TypesElement element, CFAEdge cfaEdge, IType declSpecifier)
                       throws UnrecognizedCCodeException {
    Type type;
    boolean constant = declSpecifier.isConst();

    if (declSpecifier instanceof IASTSimpleDeclSpecifier) {
      // primitive type
      IASTSimpleDeclSpecifier simpleSpecifier = (IASTSimpleDeclSpecifier)declSpecifier;
      Primitive primitiveType;

      switch (simpleSpecifier.getType()) {

      case CHAR:
        primitiveType = Primitive.CHAR;
        break;

      case INT:
      case UNSPECIFIED:
        if (simpleSpecifier.isShort()) {
          primitiveType = Primitive.SHORT;
        } else if (simpleSpecifier.isLong()) {
          primitiveType = Primitive.LONGLONG;
        } else {
          primitiveType = Primitive.LONG;
        }
        break;

      case FLOAT:
        primitiveType = Primitive.SHORT;
        break;

      case DOUBLE:
        if (simpleSpecifier.isLong()) {
          primitiveType = Primitive.LONGDOUBLE;
        } else {
          primitiveType = Primitive.DOUBLE;
        }
        break;

      case VOID:
        primitiveType = Primitive.VOID;
        break;

      default:
        throw new UnrecognizedCCodeException("Unknown basic type", cfaEdge);
      }

      boolean signed = (simpleSpecifier.isUnsigned() ? false : true);

      type = new PrimitiveType(primitiveType, signed, constant);

    } else if (declSpecifier instanceof IASTCompositeTypeSpecifier) {
      // struct & union
      IASTCompositeTypeSpecifier compositeSpecifier = (IASTCompositeTypeSpecifier)declSpecifier;
      String name = compositeSpecifier.getName();
      CompositeType compType;

      switch (compositeSpecifier.getKey()) {
      case IASTCompositeTypeSpecifier.k_struct:
        compType = new StructType(name, constant);
        name = "struct " + name;
        break;

      case IASTCompositeTypeSpecifier.k_union:
        compType = new StructType(name, constant);
        name = "union " + name;
        break;

      default:
        throw new UnrecognizedCCodeException("Unknown composite type", cfaEdge);
      }

      if (element.getTypedefs().containsKey(name)) {
        // previous forward declaration exists
        compType = (CompositeType)element.getTypedef(name);
        if (!compType.getMembers().isEmpty()) {
          throw new IllegalStateException("Redeclaration of type " + name);
        }

      } else {
        element.addTypedef(name, compType); // add type "struct a"
      }

      for (IASTSimpleDeclaration subDeclaration : compositeSpecifier.getMembers()) {

        Type subType = getType(element, cfaEdge, subDeclaration.getDeclSpecifier());

        if (subDeclaration.getName() != null) {
          String thisSubName = subDeclaration.getName();

          // anonymous struct fields may occur, ignore them
          // TODO they should be added so that the struct has the correct size
          if (!thisSubName.isEmpty()) {
            compType.addMember(thisSubName, subType);
          }
        } else {
          throw new UnrecognizedCCodeException(cfaEdge, subDeclaration);
        }
      }

      type = compType;

    } else if (declSpecifier instanceof IASTElaboratedTypeSpecifier) {
      // type reference like "struct a"
      IASTElaboratedTypeSpecifier elaboratedTypeSpecifier = (IASTElaboratedTypeSpecifier)declSpecifier;
      String typeStr = elaboratedTypeSpecifier.getKind().name().toLowerCase();
      String name = typeStr + " " + elaboratedTypeSpecifier.getName();

      type = element.getTypedef(name);

      if (type == null) {
        // forward declaration

        switch (elaboratedTypeSpecifier.getKind()) {
        case ENUM:
          type = new EnumType(name, constant);
          break;
        case STRUCT:
          type = new StructType(name, constant);
          break;
        case UNION:
          type = new UnionType(name, constant);
          break;
        default:
          throw new RuntimeException("Missing case clause");
        }

        element.addTypedef(name, type);
      }

    } else if (declSpecifier instanceof IASTEnumerationSpecifier) {
      // enum
      IASTEnumerationSpecifier enumSpecifier = (IASTEnumerationSpecifier)declSpecifier;
      String name = enumSpecifier.getName();
      EnumType enumType;

      if (element.getTypedefs().containsKey(name)) {
        // previous forward declaration exists
        enumType = (EnumType)element.getTypedef(name);
        if (!enumType.getEnumerators().isEmpty()) {
          throw new IllegalStateException("Redeclaration of type " + name);
        }

      } else {
        enumType = new EnumType(name, constant);
        element.addTypedef(name, enumType); // add type "enum a"
      }

      for (IASTEnumerator enumerator : enumSpecifier.getEnumerators()) {
        enumType.addEnumerator(enumerator.getName(), enumerator.getValue());
      }

      type = enumType;

    } else if (declSpecifier instanceof IASTNamedTypeSpecifier) {
      // type reference to type declared with typedef
      IASTNamedTypeSpecifier namedTypeSpecifier = (IASTNamedTypeSpecifier)declSpecifier;

      type = element.getTypedef(namedTypeSpecifier.getName());

      //if it is not found in the typedefs, it may be a typedef'd function
      if (type == null) {
        type = element.getFunction(namedTypeSpecifier.getName());
       }

      if (type == null) {
        throw new UnrecognizedCCodeException("Undefined type " + namedTypeSpecifier.getName(), cfaEdge);
       }

    } else if (declSpecifier instanceof IASTArrayTypeSpecifier) {
      // array
      IASTArrayTypeSpecifier arraySpecifier = (IASTArrayTypeSpecifier)declSpecifier;

      type = getType(element, cfaEdge, arraySpecifier.getType());

      int length = 0;

      IASTExpression lengthExpression = arraySpecifier.getLength();
      if (lengthExpression != null) {
        //if the length expression is a literal, get its integer value
        if (lengthExpression instanceof IASTLiteralExpression) {
          Integer value = parseLiteral((IASTLiteralExpression)lengthExpression, cfaEdge);
          if (value != null) {
            length = value;
          }
        //if not, we can't get the value with this cpa alone, and so use the default value
        } else {
          length = 0;
        }
      }
      type = new ArrayType(type, length);

    } else if (declSpecifier instanceof IASTFunctionTypeSpecifier) {
      // function type, e.g. in a function pointer
      IASTFunctionTypeSpecifier funcDeclSpecifier = (IASTFunctionTypeSpecifier)declSpecifier;

      type = getType(element, cfaEdge, funcDeclSpecifier);

    } else if (declSpecifier instanceof IASTPointerTypeSpecifier) {
      // pointer
      IASTPointerTypeSpecifier pointerSpecifier = (IASTPointerTypeSpecifier)declSpecifier;

      type = getType(element, cfaEdge, pointerSpecifier.getType());
      type = new PointerType(type, pointerSpecifier.isConst());

    } else {
      throw new UnrecognizedCCodeException("Unknown type class " + declSpecifier.getClass().getSimpleName(), cfaEdge);
    }

    return type;
  }

  private FunctionType getType(TypesElement element, CFAEdge cfaEdge, IASTFunctionTypeSpecifier funcDeclSpecifier)
                      throws UnrecognizedCCodeException {

    Type returnType = getType(element, cfaEdge, funcDeclSpecifier.getReturnType());

    FunctionType function = new FunctionType(funcDeclSpecifier.getName(), returnType, funcDeclSpecifier.takesVarArgs());

    for (IASTSimpleDeclaration parameter : funcDeclSpecifier.getParameters()) {

      Type parameterType = getType(element, cfaEdge, parameter.getDeclSpecifier());

      String parameterName = null;
      if (parameter.getName() != null) {
        parameterName = parameter.getName();
      }

      function.addParameter(parameterName, parameterType);
    }
    return function;
  }

  private Integer parseLiteral(IASTLiteralExpression expression, CFAEdge edge) throws UnrecognizedCCodeException {
    if (expression instanceof IASTIntegerLiteralExpression) {
      return ((IASTIntegerLiteralExpression)expression).getValue().intValue();

    } else if (expression instanceof IASTFloatLiteralExpression) {
      return null;

    } else if (expression instanceof IASTCharLiteralExpression) {
      return (int)((IASTCharLiteralExpression)expression).getCharacter();

    } else if (expression instanceof IASTStringLiteralExpression) {
      return null;

    } else {
      throw new UnrecognizedCCodeException("unknown literal", edge, expression);
    }
  }

  public void setEntryFunctionDefinitionNode(FunctionDefinitionNode pEntryFunctionDefNode) {
    entryFunctionDefinitionNode = pEntryFunctionDefNode;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
                         List<AbstractElement> otherElements, CFAEdge cfaEdge,
                         Precision precision) {
    return null;
  }
}