/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.types;

import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTArrayDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTArrayModifier;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointer;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.types.Type.ArrayType;
import cpa.types.Type.CompositeType;
import cpa.types.Type.EnumType;
import cpa.types.Type.FunctionType;
import cpa.types.Type.PointerType;
import cpa.types.Type.Primitive;
import cpa.types.Type.PrimitiveType;
import cpa.types.Type.StructType;
import exceptions.CPAException;
import exceptions.CPATransferException;
import exceptions.TransferRelationException;
import exceptions.UnrecognizedCFAEdgeException;

/**
 * @author Philipp Wendler
 */
public class TypesTransferRelation implements TransferRelation {

  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge,
                                              Precision precision)
                                              throws CPATransferException {
    TypesElement successor = ((TypesElement)element).clone();
    
    switch (cfaEdge.getEdgeType()) {
    case DeclarationEdge:
      try {
        handleDeclaration(successor, (DeclarationEdge)cfaEdge);
      } catch (TransferRelationException e) {
        e.printStackTrace();
      }
      break;
      
    case FunctionCallEdge:
      FunctionCallEdge funcCallEdge = (FunctionCallEdge)cfaEdge;
      if (!funcCallEdge.isExternalCall()) {
        FunctionDefinitionNode funcDefNode = (FunctionDefinitionNode)funcCallEdge.getSuccessor();
        if (successor.getFunction(funcDefNode.getFunctionName()) == null) {
          // we call a function that was not defined
          // probably "analysis.useFunctionDeclarations" is false
          // this is not bad, but we don't get type information for external
          // function

          IASTFunctionDefinition funcDef = funcDefNode.getFunctionDefinition();
          try {
            handleFunctionDeclaration(successor,
                funcDef.getDeclarator(), funcDef.getDeclSpecifier());
          } catch (TransferRelationException e1) {
            e1.printStackTrace();
          }
        }
      }
      break;
      
    case AssumeEdge:
    case StatementEdge:
    case ReturnEdge:
    case BlankEdge:
      break;
      
    case CallToReturnEdge:
    case MultiStatementEdge:
    case MultiDeclarationEdge:
      assert false;
      break;
    
    default:
      try {
        throw new UnrecognizedCFAEdgeException("Unknown edge type: " + cfaEdge.getEdgeType());
      } catch (UnrecognizedCFAEdgeException e) {
        e.printStackTrace();
      }
    }

    return successor;
  }

  private void handleDeclaration(TypesElement element,
                                 DeclarationEdge declarationEdge)
                                 throws TransferRelationException {
    IASTDeclSpecifier specifier = declarationEdge.getDeclSpecifier();
    IASTDeclarator[] declarators = declarationEdge.getDeclarators();

    if ((declarators.length == 1)
        && (declarators[0] instanceof IASTFunctionDeclarator)) {
      handleFunctionDeclaration(element, (IASTFunctionDeclarator)declarators[0], specifier);
    
    } else {
    
      Type type = getType(element, specifier);
      
      for (IASTDeclarator declarator : declarators) {
        Type thisType = getPointerType(type, declarator);
        String thisName = declarator.getName().getRawSignature();
  
        if (specifier.getStorageClass() == IASTDeclSpecifier.sc_typedef) {
          element.addTypedef(thisName, thisType);
        } else {
          String functionName = null;
          if (!(declarationEdge instanceof GlobalDeclarationEdge)) {
            functionName = declarationEdge.getSuccessor().getFunctionName();
          }
          
          element.addVariable(functionName, thisName, thisType);
        }
      }
    }
  }

  private void handleFunctionDeclaration(TypesElement element,
                                        IASTFunctionDeclarator funcDeclarator,
                                        IASTDeclSpecifier funcDeclSpecifier)
                                        throws TransferRelationException {
    
    if (!(funcDeclarator instanceof IASTStandardFunctionDeclarator)) {
      throw new TransferRelationException("Unhandled case: " + funcDeclarator.getRawSignature());
    }
            
    IASTStandardFunctionDeclarator standardFuncDeclarator = (IASTStandardFunctionDeclarator)funcDeclarator;
    
    String name = standardFuncDeclarator.getName().getRawSignature();

    Type returnType = getType(element, funcDeclSpecifier);
    returnType = getPointerType(returnType, standardFuncDeclarator);
    
    FunctionType function = new FunctionType(name, returnType, standardFuncDeclarator.takesVarArgs());
    
    boolean external = (funcDeclSpecifier.getStorageClass() == IASTDeclSpecifier.sc_extern);
    
    for (IASTParameterDeclaration parameter : standardFuncDeclarator.getParameters()) {
      IASTDeclarator paramDeclarator = parameter.getDeclarator();
      
      Type parameterType = getType(element, parameter.getDeclSpecifier());
      parameterType = getPointerType(parameterType, paramDeclarator);
    
      String parameterName = (external ? null : paramDeclarator.getName().getRawSignature());
      
      function.addParameter(parameterName, parameterType);
    }
    element.addFunction(name, function);
  }
  
  private Type getType(TypesElement element, IASTDeclSpecifier declSpecifier)
                       throws TransferRelationException {
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
        throw new TransferRelationException("Unhandled case: " + declSpecifier.getRawSignature());
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
        throw new TransferRelationException("Unhandled case: " + declSpecifier.getRawSignature());
      }
      
      for (IASTDeclaration subDeclaration : compositeSpecifier.getMembers()) {
        if (subDeclaration instanceof IASTSimpleDeclaration) {
          IASTSimpleDeclaration simpleSubDeclaration = (IASTSimpleDeclaration)subDeclaration;
          
          Type subType = getType(element, simpleSubDeclaration.getDeclSpecifier());  

          for (IASTDeclarator declarator : simpleSubDeclaration.getDeclarators()) {
            Type thisSubType = getPointerType(subType, declarator);
            String thisSubName = declarator.getRawSignature();
            
            compType.addMember(thisSubName, thisSubType);
          }
        } else {
          throw new TransferRelationException("Unhandled case: " + declSpecifier.getRawSignature());          
        }
      }
      
      type = compType;
      element.addTypedef(name, type); // add type "struct a"
      
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
        throw new TransferRelationException("Unhandled case: " + declSpecifier.getRawSignature());
      }
      
      type = element.getTypedef(name);
      
    } else if (declSpecifier instanceof IASTEnumerationSpecifier) {
      // enum
      IASTEnumerationSpecifier enumSpecifier = (IASTEnumerationSpecifier)declSpecifier;
      String name = enumSpecifier.getName().getRawSignature();
      EnumType enumType = new EnumType(name, constant);

      
      for (IASTEnumerator enumerator : enumSpecifier.getEnumerators()) {
        int value;
        try {
          value = Integer.parseInt(enumerator.getValue().getRawSignature());
        } catch (NumberFormatException e) {
          throw new TransferRelationException("Not exptected in CIL: " + declSpecifier.getRawSignature());
        }
        enumType.addEnumerator(enumerator.getName().getRawSignature(), value);
      }
      
      type = enumType;
      element.addTypedef("enum " + name, type); // add type "enum a"
      
    } else if (declSpecifier instanceof IASTNamedTypeSpecifier) {
      // type reference to type declared with typedef
      IASTNamedTypeSpecifier namedTypeSpecifier = (IASTNamedTypeSpecifier)declSpecifier;
      
      type = element.getTypedef(namedTypeSpecifier.getName().getRawSignature());
      
    } else {
      throw new TransferRelationException("Unhandled case: " + declSpecifier.getRawSignature());
    }
    return type;
  }

  private Type getPointerType(Type original, IASTDeclarator declarator)
                              throws TransferRelationException {
    Type result = original;

    if (declarator instanceof IASTArrayDeclarator) {
      IASTArrayModifier[] arrayOps = ((IASTArrayDeclarator)declarator).getArrayModifiers();
      for (IASTArrayModifier arrayOp : arrayOps) {
        int length = 0;
        
        IASTExpression lengthExpression = arrayOp.getConstantExpression();
        if (lengthExpression != null) {
          try {
            length = Integer.parseInt(lengthExpression.getRawSignature());
          } catch (NumberFormatException e) {
            throw new TransferRelationException("Not exptected in CIL: " + declarator.getRawSignature());
          }
        }
        result = new ArrayType(result, length);
      }
    }
    
    IASTPointerOperator[] pointerOps = declarator.getPointerOperators();
    if (pointerOps != null) {
      for (IASTPointerOperator pointerOp : pointerOps) {
        boolean constant = false;
        if (pointerOp instanceof IASTPointer) {
          constant = ((IASTPointer)pointerOp).isConst();
        }
        result = new PointerType(result, constant);
      }
    }
    return result;
  }
  
  @Override
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(
                                           AbstractElementWithLocation element,
                                           Precision precision)
                                           throws CPAException,
                                                  CPATransferException {
    throw new CPAException ("Cannot get all abstract successors from non-location domain");
  }

  @Override
  public void strengthen(AbstractElement element,
                         List<AbstractElement> otherElements, CFAEdge cfaEdge,
                         Precision precision) {
  }
}