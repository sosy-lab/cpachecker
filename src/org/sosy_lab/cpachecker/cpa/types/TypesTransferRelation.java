/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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

import org.sosy_lab.cpachecker.cfa.ast.IASTArrayDeclarator;
import org.sosy_lab.cpachecker.cfa.ast.IASTArrayModifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTCompositeTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclarator;
import org.sosy_lab.cpachecker.cfa.ast.IASTElaboratedTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionDeclarator;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionDefinition;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNamedTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTPointer;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier.IASTEnumerator;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
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

/**
 * @author Philipp Wendler
 */
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

        IASTFunctionDefinition funcDef = funcDefNode.getFunctionDefinition();
        handleFunctionDeclaration(successor, funcCallEdge,
            funcDef.getDeclarator(), funcDef.getDeclSpecifier());
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
          && cfaEdge.getRawStatement().equals("Function start dummy edge")) {
        //since by this point all global variables have been processed, we can now process the entry function
        IASTFunctionDefinition funcDef = entryFunctionDefinitionNode.getFunctionDefinition();
        handleFunctionDeclaration(successor, null, funcDef.getDeclarator(), funcDef.getDeclSpecifier());

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
    IASTDeclSpecifier specifier = declarationEdge.getDeclSpecifier();
    List<IASTDeclarator> declarators = declarationEdge.getDeclarators();

    if ((declarators.size() == 1)
        && (declarators.get(0) instanceof IASTFunctionDeclarator)) {
      handleFunctionDeclaration(element, declarationEdge, (IASTFunctionDeclarator)declarators.get(0), specifier);

    } else {

      Type type = getType(element, declarationEdge, specifier);

      for (IASTDeclarator declarator : declarators) {
        Type thisType = getPointerType(type, declarationEdge, declarator);
        String thisName = declarator.getName().getRawSignature();

        if (specifier.getStorageClass() == IASTDeclSpecifier.sc_typedef) {
          element.addTypedef(thisName, thisType);
        } else {
          String functionName = null;
          if (!(declarationEdge.isGlobal())) {
            functionName = declarationEdge.getSuccessor().getFunctionName();
          }

          element.addVariable(functionName, thisName, thisType);
        }
      }
    }
  }

  private void handleFunctionDeclaration(TypesElement element,
                                        CFAEdge cfaEdge,
                                        IASTFunctionDeclarator funcDeclarator,
                                        IASTDeclSpecifier funcDeclSpecifier)
                                        throws UnrecognizedCCodeException {

    String name;
    //in case of a nested declarator, get the variable name from the inner declarator
    if (funcDeclarator.getNestedDeclarator() != null) {
      name = funcDeclarator.getNestedDeclarator().getName().getRawSignature();
    } else {
    //otherwise there is only one declarator
      name = funcDeclarator.getName().getRawSignature();
    }

    Type returnType = getType(element, cfaEdge, funcDeclSpecifier);
    returnType = getPointerType(returnType, cfaEdge, funcDeclarator);

    FunctionType function = new FunctionType(name, returnType, funcDeclarator.takesVarArgs());

    boolean external = (funcDeclSpecifier.getStorageClass() == IASTDeclSpecifier.sc_extern);

    for (IASTParameterDeclaration parameter : funcDeclarator.getParameters()) {
      IASTDeclarator paramDeclarator = parameter.getDeclarator();

      Type parameterType = getType(element, cfaEdge, parameter.getDeclSpecifier());
      parameterType = getPointerType(parameterType, cfaEdge, paramDeclarator);

      String parameterName = (external ? null : paramDeclarator.getName().getRawSignature());

      function.addParameter(parameterName, parameterType);
    }
    element.addFunction(name, function);
  }

  private Type getType(TypesElement element, CFAEdge cfaEdge, IASTDeclSpecifier declSpecifier)
                       throws UnrecognizedCCodeException {
    Type type;
    boolean constant = declSpecifier.isConst();

    if (declSpecifier instanceof IASTSimpleDeclSpecifier) {
      // primitive type
      IASTSimpleDeclSpecifier simpleSpecifier = (IASTSimpleDeclSpecifier)declSpecifier;
      Primitive primitiveType;

      switch (simpleSpecifier.getType()) {

      case IASTSimpleDeclSpecifier.t_char:
        primitiveType = Primitive.CHAR;
        break;

      case IASTSimpleDeclSpecifier.t_int:
      case IASTSimpleDeclSpecifier.t_unspecified:
        if (simpleSpecifier.isShort()) {
          primitiveType = Primitive.SHORT;
        } else if (simpleSpecifier.isLong()) {
          primitiveType = Primitive.LONGLONG;
        } else {
          primitiveType = Primitive.LONG;
        }
        break;

      case IASTSimpleDeclSpecifier.t_float:
        primitiveType = Primitive.SHORT;
        break;

      case IASTSimpleDeclSpecifier.t_double:
        if (simpleSpecifier.isLong()) {
          primitiveType = Primitive.LONGDOUBLE;
        } else {
          primitiveType = Primitive.DOUBLE;
        }
        break;

      case IASTSimpleDeclSpecifier.t_void:
        primitiveType = Primitive.VOID;
        break;

      default:
        throw new UnrecognizedCCodeException(cfaEdge, simpleSpecifier);
      }

      boolean signed = (simpleSpecifier.isUnsigned() ? false : true);

      type = new PrimitiveType(primitiveType, signed, constant);

    } else if (declSpecifier instanceof IASTCompositeTypeSpecifier) {
      // struct & union
      IASTCompositeTypeSpecifier compositeSpecifier = (IASTCompositeTypeSpecifier)declSpecifier;
      String name = compositeSpecifier.getName().getRawSignature();
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
        throw new UnrecognizedCCodeException(cfaEdge, compositeSpecifier);
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

      for (IASTDeclaration subDeclaration : compositeSpecifier.getMembers()) {
        if (subDeclaration instanceof IASTSimpleDeclaration) {
          IASTSimpleDeclaration simpleSubDeclaration = (IASTSimpleDeclaration)subDeclaration;

          Type subType = getType(element, cfaEdge, simpleSubDeclaration.getDeclSpecifier());

          for (IASTDeclarator declarator : simpleSubDeclaration.getDeclarators()) {
            Type thisSubType = getPointerType(subType, cfaEdge, declarator);
            String thisSubName = declarator.getRawSignature();

            compType.addMember(thisSubName, thisSubType);
          }
        } else {
          throw new UnrecognizedCCodeException(cfaEdge, subDeclaration);
        }
      }

      type = compType;

    } else if (declSpecifier instanceof IASTElaboratedTypeSpecifier) {
      // type reference like "struct a"
      IASTElaboratedTypeSpecifier elaboratedTypeSpecifier = (IASTElaboratedTypeSpecifier)declSpecifier;
      String name = elaboratedTypeSpecifier.getName().getRawSignature();

      switch (elaboratedTypeSpecifier.getKind()) {
      case IASTElaboratedTypeSpecifier.k_enum:
        name = "enum " + name;
        break;
      case IASTElaboratedTypeSpecifier.k_struct:
        name = "struct " + name;
        break;
      case IASTElaboratedTypeSpecifier.k_union:
        name = "union " + name;
        break;

      default:
        throw new UnrecognizedCCodeException(cfaEdge, elaboratedTypeSpecifier);
      }

      type = element.getTypedef(name);

      if (type == null) {
        // forward declaration

        switch (elaboratedTypeSpecifier.getKind()) {
        case IASTElaboratedTypeSpecifier.k_enum:
          type = new EnumType(name, constant);
          break;
        case IASTElaboratedTypeSpecifier.k_struct:
          type = new StructType(name, constant);
          break;
        case IASTElaboratedTypeSpecifier.k_union:
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
      String name = enumSpecifier.getName().getRawSignature();
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
        int value;
        try {
          value = Integer.parseInt(enumerator.getValue().getRawSignature());
        } catch (NumberFormatException e) {
          throw new UnrecognizedCCodeException(e.getMessage(), cfaEdge, enumerator);
        }
        enumType.addEnumerator(enumerator.getName().getRawSignature(), value);
      }

      type = enumType;

    } else if (declSpecifier instanceof IASTNamedTypeSpecifier) {
      // type reference to type declared with typedef
      IASTNamedTypeSpecifier namedTypeSpecifier = (IASTNamedTypeSpecifier)declSpecifier;

      type = element.getTypedef(namedTypeSpecifier.getName().getRawSignature());

      //if it is not found in the typedefs, it may be a typedef'd function
      if (type == null) {
        type = element.getFunction(namedTypeSpecifier.getName().getRawSignature());
       }

      if (type == null) {
        throw new UnrecognizedCCodeException("type not defined", cfaEdge, namedTypeSpecifier);
       }

    } else {
      throw new UnrecognizedCCodeException(cfaEdge, declSpecifier);
    }

    return type;
  }

  private Type getPointerType(Type original, CFAEdge cfaEdge, IASTDeclarator declarator)
                              throws UnrecognizedCCodeException {
    Type result = original;

    if (declarator instanceof IASTArrayDeclarator) {
      IASTArrayModifier[] arrayOps = ((IASTArrayDeclarator)declarator).getArrayModifiers();
      for (IASTArrayModifier arrayOp : arrayOps) {
        int length = 0;

        IASTExpression lengthExpression = arrayOp.getConstantExpression();
        if (lengthExpression != null) {
          try {
            //if the length expression is a literal, get its integer value
            if (lengthExpression instanceof IASTLiteralExpression) {
              length = parseLiteral(lengthExpression).intValue();
            //if not, we can't get the value with this cpa alone, and so use the default value
            } else {
              length = 0;
            }
          } catch (NumberFormatException e) {
            throw new UnrecognizedCCodeException(cfaEdge, declarator);
          }
        }
        result = new ArrayType(result, length);
      }
    }

    IASTPointer[] pointers = declarator.getPointerOperators();
    if (pointers != null) {
      for (IASTPointer pointerOp : pointers) {
        boolean constant = false;
          constant = pointerOp.isConst();
        result = new PointerType(result, constant);
      }
    }
    return result;
  }

  private Long parseLiteral(IASTExpression expression) throws NumberFormatException {
    if (expression instanceof IASTLiteralExpression) {

      int typeOfLiteral = ((IASTLiteralExpression)expression).getKind();
      if (typeOfLiteral == IASTLiteralExpression.lk_integer_constant) {

        String s = expression.getRawSignature();
        if(s.endsWith("L") || s.endsWith("U")){
          s = s.replace("L", "");
          s = s.replace("U", "");
        }
        return Long.valueOf(s);
      }
    }
    return null;
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