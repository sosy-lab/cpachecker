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
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.ast.CBasicType;
import org.sosy_lab.cpachecker.cfa.ast.DummyType;
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTArrayTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCompositeTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTElaboratedTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFileLocation;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionDefinition;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTName;
import org.sosy_lab.cpachecker.cfa.ast.IASTNamedTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTPointerTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTTypeId;
import org.sosy_lab.cpachecker.cfa.ast.IASTTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IComplexType;
import org.sosy_lab.cpachecker.cfa.ast.IPointerType;
import org.sosy_lab.cpachecker.cfa.ast.IType;
import org.sosy_lab.cpachecker.cfa.ast.ITypedef;
import org.sosy_lab.cpachecker.cfa.ast.StorageClass;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier.IASTEnumerator;

class ASTConverter {
    
  public static IASTExpression convert(org.eclipse.cdt.core.dom.ast.IASTExpression e) {
    assert !(e instanceof IASTExpression);

    if (e == null) {
      return null;
    
    } else if (e instanceof org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression)e);
      
    } else if (e instanceof org.eclipse.cdt.core.dom.ast.IASTBinaryExpression) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTBinaryExpression)e);
      
    } else if (e instanceof org.eclipse.cdt.core.dom.ast.IASTCastExpression) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTCastExpression)e);
      
    } else if (e instanceof org.eclipse.cdt.core.dom.ast.IASTFieldReference) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTFieldReference)e);
            
    } else if (e instanceof org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression)e);
      
    } else if (e instanceof org.eclipse.cdt.core.dom.ast.IASTIdExpression) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTIdExpression)e);
      
    } else if (e instanceof org.eclipse.cdt.core.dom.ast.IASTLiteralExpression) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTLiteralExpression)e);
      
    } else if (e instanceof org.eclipse.cdt.core.dom.ast.IASTUnaryExpression) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTUnaryExpression)e);
      
    } else if (e instanceof org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression)e);
        
    } else {
      throw new CFAGenerationRuntimeException("", e);
    }
  }
  
  private static IASTArraySubscriptExpression convert(org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression e) {
    return new IASTArraySubscriptExpression(e.getRawSignature(), convert(e.getFileLocation()), convert(e.getExpressionType()), convert(e.getArrayExpression()), convert(e.getSubscriptExpression()));
  }

  private static IASTBinaryExpression convert(org.eclipse.cdt.core.dom.ast.IASTBinaryExpression e) {
    return new IASTBinaryExpression(e.getRawSignature(), convert(e.getFileLocation()), convert(e.getExpressionType()), convert(e.getOperand1()), convert(e.getOperand2()), e.getOperator());
  }
  
  private static IASTCastExpression convert(org.eclipse.cdt.core.dom.ast.IASTCastExpression e) {
    return new IASTCastExpression(e.getRawSignature(), convert(e.getFileLocation()), convert(e.getExpressionType()), convert(e.getOperand()), convert(e.getTypeId()));
  }
  
  private static IASTFieldReference convert(org.eclipse.cdt.core.dom.ast.IASTFieldReference e) {
    return new IASTFieldReference(e.getRawSignature(), convert(e.getFileLocation()), convert(e.getExpressionType()), convert(e.getFieldName()), convert(e.getFieldOwner()), e.isPointerDereference());
  }
  
  private static IASTFunctionCallExpression convert(org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression e) {
    org.eclipse.cdt.core.dom.ast.IASTExpression p = e.getParameterExpression();
    List<IASTExpression> params = new ArrayList<IASTExpression>();
    
    if (p instanceof org.eclipse.cdt.core.dom.ast.IASTExpressionList) {
      org.eclipse.cdt.core.dom.ast.IASTExpression[] ps = ((org.eclipse.cdt.core.dom.ast.IASTExpressionList)p).getExpressions();
      for (org.eclipse.cdt.core.dom.ast.IASTExpression param : ps) {
        params.add(convert(param));
      }
    
    } else if (p != null) {
      params.add(convert(p));
    }
    
    return new IASTFunctionCallExpression(e.getRawSignature(), convert(e.getFileLocation()), convert(e.getExpressionType()), convert(e.getFunctionNameExpression()), params);
  }
  
  private static IASTIdExpression convert(org.eclipse.cdt.core.dom.ast.IASTIdExpression e) {
    return new IASTIdExpression(e.getRawSignature(), convert(e.getFileLocation()), convert(e.getExpressionType()), convert(e.getName()));
  }

  private static IASTLiteralExpression convert(org.eclipse.cdt.core.dom.ast.IASTLiteralExpression e) {
    assert e.getRawSignature().equals(String.valueOf(e.getValue()));
    return new IASTLiteralExpression(e.getRawSignature(), convert(e.getFileLocation()), convert(e.getExpressionType()), e.getKind());
  }

  private static IASTExpression convert(org.eclipse.cdt.core.dom.ast.IASTUnaryExpression e) {
    if (e.getOperator() == org.eclipse.cdt.core.dom.ast.IASTUnaryExpression.op_bracketedPrimary) {
      return convert(e.getOperand());
    }
    
    return new IASTUnaryExpression(e.getRawSignature(), convert(e.getFileLocation()), convert(e.getExpressionType()), convert(e.getOperand()), e.getOperator());
  }

  private static IASTTypeIdExpression convert(org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression e) {
    return new IASTTypeIdExpression(e.getRawSignature(), convert(e.getFileLocation()), convert(e.getExpressionType()), e.getOperator(), convert(e.getTypeId()));
  }

  public static IASTStatement convert(
      final org.eclipse.cdt.core.dom.ast.IASTStatement s) {

    if (s instanceof org.eclipse.cdt.core.dom.ast.IASTExpressionStatement) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTExpressionStatement) s);

    } else if (s instanceof org.eclipse.cdt.core.dom.ast.IASTReturnStatement) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTReturnStatement) s);

    } else {
      throw new CFAGenerationRuntimeException("unknown statement: "
          + s.getClass(), s);
    }
  }

  public static IASTExpressionStatement convert(final org.eclipse.cdt.core.dom.ast.IASTExpressionStatement s) {
      return new IASTExpressionStatement(s.getRawSignature(), convert(s.getFileLocation()), convert(s.getExpression()));
  }
  
  public static IASTReturnStatement convert(final org.eclipse.cdt.core.dom.ast.IASTReturnStatement s) {
    return new IASTReturnStatement(s.getRawSignature(), convert(s.getFileLocation()), convert(s.getReturnValue()));
  }

  private static List<IASTDeclaration> convert(final org.eclipse.cdt.core.dom.ast.IASTDeclaration d) {
    if (d instanceof org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration)d);
    
    } else {
      throw new CFAGenerationRuntimeException("", d);
    }
  }
  
  public static IASTFunctionDefinition convert(final org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition f) {
    Pair<StorageClass, ? extends IASTDeclSpecifier> specifier = convert(f.getDeclSpecifier());
    
    Triple<IASTDeclSpecifier, IASTInitializer, IASTName> declarator = convert(f.getDeclarator(), specifier.getSecond());
    if (!(declarator.getFirst() instanceof IASTFunctionTypeSpecifier)) {
      throw new CFAGenerationRuntimeException("Unsupported nested declarator for function definition", f);
    }
    if (declarator.getSecond() != null) {
      throw new CFAGenerationRuntimeException("Unsupported initializer for function definition", f);
    }
    if (declarator.getThird() == null) {
      throw new CFAGenerationRuntimeException("Missing name for function definition", f);
    }
    
    IASTFunctionTypeSpecifier declSpec = (IASTFunctionTypeSpecifier)declarator.getFirst();
    
    // fake raw signature because otherwise it would contain the whole function body
    String rawSignature = f.getDeclSpecifier().getRawSignature() + " " + f.getDeclarator().getRawSignature();
    
    return new IASTFunctionDefinition(rawSignature, convert(f.getFileLocation()), specifier.getFirst(), declSpec, declarator.getThird());
  }
  
  public static List<IASTDeclaration> convert(final org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration d) {
    IASTFileLocation fileLoc = convert(d.getFileLocation());
    Pair<StorageClass, ? extends IASTDeclSpecifier> specifier = convert(d.getDeclSpecifier());
    StorageClass storageClass = specifier.getFirst();
    IASTDeclSpecifier declSpec = specifier.getSecond();
    
    List<IASTDeclaration> result;
    org.eclipse.cdt.core.dom.ast.IASTDeclarator[] declarators = d.getDeclarators();
    if (declarators == null || declarators.length == 0) {
      // declaration without declarator, i.e. struct prototype
      IASTSimpleDeclaration newSd = new IASTSimpleDeclaration(d.getRawSignature(), fileLoc, declSpec, null);
      IASTDeclaration newD = new IASTDeclaration(newSd.getRawSignature(), fileLoc, storageClass, newSd, null);
      result = Collections.singletonList(newD);
    
    } else if (declarators.length == 1) {
      Triple<IASTDeclSpecifier, IASTInitializer, IASTName> declarator = convert(declarators[0], declSpec);

      IASTSimpleDeclaration newSd = new IASTSimpleDeclaration(d.getRawSignature(), fileLoc, declarator.getFirst(), declarator.getThird());
      IASTDeclaration newD = new IASTDeclaration(newSd.getRawSignature(), fileLoc, storageClass, newSd, declarator.getSecond());
      result = Collections.singletonList(newD);
    
    } else {
      result = new ArrayList<IASTDeclaration>(declarators.length);
      for (org.eclipse.cdt.core.dom.ast.IASTDeclarator c : d.getDeclarators()) {
        Triple<IASTDeclSpecifier, IASTInitializer, IASTName> declarator = convert(c, declSpec);
        
        // fake rawSignature because otherwise the other declarators would appear in it, too
        String rawSignature = d.getDeclSpecifier().getRawSignature() + " " + c.getRawSignature() + ";";
        
        IASTSimpleDeclaration newSd = new IASTSimpleDeclaration(rawSignature, fileLoc, declarator.getFirst(), declarator.getThird());
        IASTDeclaration newD = new IASTDeclaration(rawSignature, fileLoc, storageClass, newSd, declarator.getSecond());

        result.add(newD);
      }
    }
    
    return result;
  }
  
  
  private static Triple<IASTDeclSpecifier, IASTInitializer, IASTName> convert(org.eclipse.cdt.core.dom.ast.IASTDeclarator d, IASTDeclSpecifier specifier) {
    if (d == null) {
      return null;
    
    } else if (d instanceof org.eclipse.cdt.core.dom.ast.IASTArrayDeclarator) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTArrayDeclarator)d, specifier);
      
    } else if (d instanceof org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator)d, specifier);
    
    } else {
      if (d.getNestedDeclarator() != null) {
        throw new CFAGenerationRuntimeException("Nested declarator where not expected", d);
      }
      return Triple.of(
             convertPointerOperators(d.getPointerOperators(), specifier),
             convert(d.getInitializer()),
             convert(d.getName()));
    }
  }
  
  private static IASTDeclSpecifier convertPointerOperators(org.eclipse.cdt.core.dom.ast.IASTPointerOperator[] ps, IASTDeclSpecifier type) {
    for (org.eclipse.cdt.core.dom.ast.IASTPointerOperator p : ps) {
      boolean isConst = false;
      boolean isVolatile = false;
      if (p instanceof org.eclipse.cdt.core.dom.ast.IASTPointer) {
        org.eclipse.cdt.core.dom.ast.IASTPointer pp = (org.eclipse.cdt.core.dom.ast.IASTPointer)p;
        isConst = pp.isConst();
        isVolatile = pp.isVolatile();             
      } else {
        throw new CFAGenerationRuntimeException("Unknown pointer operator", p);
      }
      
      // TODO rawSignature is bad here
      type = new IASTPointerTypeSpecifier(p.getRawSignature(), convert(p.getFileLocation()), isConst, isVolatile, type);
    }
    return type;
  }
  
  private static Triple<IASTDeclSpecifier, IASTInitializer, IASTName> convert(org.eclipse.cdt.core.dom.ast.IASTArrayDeclarator d, IASTDeclSpecifier type)  {
    IASTName name;
    if (d.getNestedDeclarator() != null) {
      Triple<? extends IASTDeclSpecifier, IASTInitializer, IASTName> nestedDeclarator = convert(d.getNestedDeclarator(), type);
      
      assert d.getName().getRawSignature().isEmpty() : d;
      assert nestedDeclarator.getSecond() == null;
      
      type = nestedDeclarator.getFirst();
      name = nestedDeclarator.getThird();
    
    } else {
      name = convert(d.getName());
    }
    
    type = convertPointerOperators(d.getPointerOperators(), type);
    
    // TODO check order of pointer operators and array modifiers
    for (org.eclipse.cdt.core.dom.ast.IASTArrayModifier a : d.getArrayModifiers()) {
      boolean isConst = false;
      boolean isVolatile = false;
      if (a instanceof org.eclipse.cdt.core.dom.ast.c.ICASTArrayModifier) {
        org.eclipse.cdt.core.dom.ast.c.ICASTArrayModifier aa = (org.eclipse.cdt.core.dom.ast.c.ICASTArrayModifier)a;
        isConst = aa.isConst();
        isVolatile = aa.isVolatile();             
      } else {
        throw new CFAGenerationRuntimeException("Unknown array modifier", a);
      }
      
      // TODO rawSignature is bad here
      type = new IASTArrayTypeSpecifier(a.getRawSignature(), convert(a.getFileLocation()), isConst, isVolatile, type, convert(a.getConstantExpression()));
    }
    
    return Triple.of(type, convert(d.getInitializer()), name);
  }
  
  private static Triple<IASTDeclSpecifier, IASTInitializer, IASTName> convert(org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator d, IASTDeclSpecifier returnType) {
    if (!(d instanceof org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator)) {
      throw new CFAGenerationRuntimeException("Unknown non-standard function definition", d);
    }
    org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator sd = (org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator)d;

    // handle return type
    returnType = convertPointerOperators(d.getPointerOperators(), returnType);

    // handle parameters
    List<IASTSimpleDeclaration> paramsList = new ArrayList<IASTSimpleDeclaration>(sd.getParameters().length);
    for (org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration c : sd.getParameters()) {
      if (!c.getRawSignature().equals("void")) {
        paramsList.add(convert(c));
      } else {
        // there may be a function declaration f(void), which is equal to f()
        // we don't want this dummy parameter "void"
        assert sd.getParameters().length == 1 : sd.getRawSignature();
      }
    }
    
    // TODO constant and volatile
    IASTFunctionTypeSpecifier fType = new IASTFunctionTypeSpecifier(returnType.getRawSignature() + " " + d.getRawSignature(), convert(d.getFileLocation()), false, false, returnType, paramsList, sd.takesVarArgs());

    IASTDeclSpecifier type = fType;
    IASTName name;
    if (d.getNestedDeclarator() != null) {
      Triple<? extends IASTDeclSpecifier, IASTInitializer, IASTName> nestedDeclarator = convert(d.getNestedDeclarator(), type);
      
      assert d.getName().getRawSignature().isEmpty() : d;
      assert nestedDeclarator.getSecond() == null;

      type = nestedDeclarator.getFirst();
      name = nestedDeclarator.getThird();
    
    } else {
      name = convert(d.getName());
    }    
    
    fType.setName(name);
    
    return Triple.of(type, convert(d.getInitializer()), name);
  }
  
  
  private static Pair<StorageClass, ? extends IASTDeclSpecifier> convert(org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier d) {
    StorageClass sc;
    switch (d.getStorageClass()) {
    case org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier.sc_unspecified:
    case org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier.sc_auto:
    case org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier.sc_register:
      sc = StorageClass.AUTO;
      break;
      
    case org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier.sc_static:
      sc = StorageClass.STATIC;
      break;

    case org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier.sc_extern:
      sc = StorageClass.EXTERN;
      break;

    case org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier.sc_typedef:
      sc = StorageClass.TYPEDEF;
      break;
      
    default:
      throw new CFAGenerationRuntimeException("Unsupported storage class", d);  
    }
    
    if (d instanceof org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier) {
      return Pair.of(sc, convert((org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier)d));
    
    } else if (d instanceof org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier) {
      return Pair.of(sc, convert((org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier)d));
      
    } else if (d instanceof org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier) {
      return Pair.of(sc, convert((org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier)d));
      
    } else if (d instanceof org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier) {
      return Pair.of(sc, convert((org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier)d));
      
    } else if (d instanceof org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier) {
      return Pair.of(sc, convert((org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier)d));
           
    } else {
      throw new CFAGenerationRuntimeException("", d);
    }
  }
  
  private static IASTCompositeTypeSpecifier convert(org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier d) {
    List<IASTSimpleDeclaration> list = new ArrayList<IASTSimpleDeclaration>(d.getMembers().length);
    for (org.eclipse.cdt.core.dom.ast.IASTDeclaration c : d.getMembers()) {
      List<IASTDeclaration> newCs = convert(c);
      assert !newCs.isEmpty();

      for (IASTDeclaration newC : newCs) {
        if (newC.getStorageClass() != StorageClass.AUTO) {
          throw new CFAGenerationRuntimeException("Unsupported storage class inside composite type", c);
        }
        if (newC.getInitializer() != null) {
          throw new CFAGenerationRuntimeException("Unsupported initializer inside composite type", c);
        }
        list.add(newC.getDeclaration());
      }
    }
    return new IASTCompositeTypeSpecifier(d.getRawSignature(), convert(d.getFileLocation()), d.isConst(), d.isVolatile(), d.getKey(), list, convert(d.getName()));
  }
  
  private static IASTElaboratedTypeSpecifier convert(org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier d) {
    return new IASTElaboratedTypeSpecifier(d.getRawSignature(), convert(d.getFileLocation()), d.isConst(), d.isVolatile(), d.getKind(), convert(d.getName()));
  }
  
  private static IASTEnumerationSpecifier convert(org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier d) {
    List<IASTEnumerator> list = new ArrayList<IASTEnumerator>(d.getEnumerators().length);
    for (org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator c : d.getEnumerators()) {
      list.add(convert(c));
    }
    return new IASTEnumerationSpecifier(d.getRawSignature(), convert(d.getFileLocation()), d.isConst(), d.isVolatile(), list, convert(d.getName()));
  }
  
  private static IASTNamedTypeSpecifier convert(org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier d) {
    return new IASTNamedTypeSpecifier(d.getRawSignature(), convert(d.getFileLocation()), d.isConst(), d.isVolatile(), convert(d.getName()));
  }
  
  private static IASTSimpleDeclSpecifier convert(org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier d) {
    return new IASTSimpleDeclSpecifier(d.getRawSignature(), convert(d.getFileLocation()), d.isConst(), d.isVolatile(), d.getType(), d.isLong(), d.isShort(), d.isSigned(), d.isUnsigned());
  }
  

  private static IASTEnumerator convert(org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator e) {
    return new IASTEnumerator(e.getRawSignature(), convert(e.getFileLocation()), convert(e.getName()), convert(e.getValue()));
  }

  private static IASTInitializer convert(org.eclipse.cdt.core.dom.ast.IASTInitializer i) {
    if (i == null) {
      return null;
    
    } else if (i instanceof org.eclipse.cdt.core.dom.ast.IASTInitializerExpression) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTInitializerExpression)i);
    } else if (i instanceof org.eclipse.cdt.core.dom.ast.IASTInitializerList) {
      return convert((org.eclipse.cdt.core.dom.ast.IASTInitializerList)i);
    } else {
      throw new CFAGenerationRuntimeException("", i);
    }
  }
  
  private static IASTInitializerExpression convert(org.eclipse.cdt.core.dom.ast.IASTInitializerExpression i) {
    return new IASTInitializerExpression(i.getRawSignature(), convert(i.getFileLocation()), convert(i.getExpression()));
  }
  
  private static IASTInitializerList convert(org.eclipse.cdt.core.dom.ast.IASTInitializerList iList) {
    List<IASTInitializer> initializerList = new ArrayList<IASTInitializer>(iList.getInitializers().length);
    for (org.eclipse.cdt.core.dom.ast.IASTInitializer i : iList.getInitializers()) {
      initializerList.add(convert(i));
    }
    return new IASTInitializerList(iList.getRawSignature(), convert(iList.getFileLocation()), initializerList);
    }
  
  public static IASTSimpleDeclaration convert(org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration p) {
    Pair<StorageClass, ? extends IASTDeclSpecifier> specifier = convert(p.getDeclSpecifier());
    if (specifier.getFirst() != StorageClass.AUTO) {
      throw new CFAGenerationRuntimeException("Unsupported storage class for parameters", p);
    }
    
    Triple<IASTDeclSpecifier, IASTInitializer, IASTName> declarator = convert(p.getDeclarator(), specifier.getSecond());
    if (declarator.getSecond() != null) {
      throw new CFAGenerationRuntimeException("Unsupported initializer for parameters", p);
    }
    
    return new IASTSimpleDeclaration(p.getRawSignature(), convert(p.getFileLocation()), declarator.getFirst(), declarator.getThird());
  }
  
  private static IASTFileLocation convert(org.eclipse.cdt.core.dom.ast.IASTFileLocation l) {
    if (l == null) {
      return null;
    }
    return new IASTFileLocation(l.getEndingLineNumber(), l.getFileName(), l.getNodeLength(), l.getNodeOffset(), l.getStartingLineNumber());
  }
  
  private static IASTName convert(org.eclipse.cdt.core.dom.ast.IASTName n) {
    org.eclipse.cdt.core.dom.ast.IBinding binding = n.getBinding();
    if (binding == null) {
      binding = n.resolveBinding();
    }
    
    IType type;
    try {
      if (binding == null) {
        // not sure which C code triggers this 
        type = null;
        
      } else if (binding instanceof org.eclipse.cdt.core.dom.ast.IVariable) {
        type = convert(((org.eclipse.cdt.core.dom.ast.IVariable)binding).getType());
      
      } else if (binding instanceof org.eclipse.cdt.core.dom.ast.IFunction) {
        type = convert(((org.eclipse.cdt.core.dom.ast.IFunction)binding).getType());
      
      } else if (binding instanceof org.eclipse.cdt.core.dom.ast.IEnumerator) {
        type = convert(((org.eclipse.cdt.core.dom.ast.IEnumerator)binding).getType());
  
      } else {
        type = new DummyType(binding.getClass().getSimpleName());
      }
    } catch (org.eclipse.cdt.core.dom.ast.DOMException e) {
      throw new CFAGenerationRuntimeException(e.getMessage());
    }

    return new IASTName(n.getRawSignature(), convert(n.getFileLocation()), type);
  }
  
  private static IASTTypeId convert(org.eclipse.cdt.core.dom.ast.IASTTypeId t) {
    Pair<StorageClass, ? extends IASTDeclSpecifier> specifier = convert(t.getDeclSpecifier());

    Triple<IASTDeclSpecifier, IASTInitializer, IASTName> declarator = convert(t.getAbstractDeclarator(), specifier.getSecond());
    if (declarator.getSecond() != null) {
      throw new CFAGenerationRuntimeException("Unsupported initializer for type ids", t);
    }
    
    return new IASTTypeId(t.getRawSignature(), convert(t.getFileLocation()), specifier.getFirst(), declarator.getFirst(), declarator.getThird());
  }
  
  private static IType convert(org.eclipse.cdt.core.dom.ast.IType t) {
    if (t instanceof org.eclipse.cdt.core.dom.ast.IBasicType) {
      return convert((org.eclipse.cdt.core.dom.ast.IBasicType)t);
 
    } else if (t instanceof org.eclipse.cdt.core.dom.ast.IPointerType) {
      return convert((org.eclipse.cdt.core.dom.ast.IPointerType)t);
      
    } else if (t instanceof org.eclipse.cdt.core.dom.ast.ITypedef) {
      return convert((org.eclipse.cdt.core.dom.ast.ITypedef)t);
      
    } else if (t instanceof org.eclipse.cdt.core.dom.ast.IBinding) {
      return new IComplexType(((org.eclipse.cdt.core.dom.ast.IBinding) t).getName());
      
    } else {
      return new DummyType(t.toString());
    }
  }

  private static CBasicType convert(final org.eclipse.cdt.core.dom.ast.IBasicType t) {

    try {
      
      // The IBasicType has to be an ICBasicType or
      // an IBasicType of type "void" (then it is an ICPPBasicType)
      if (t instanceof org.eclipse.cdt.core.dom.ast.c.ICBasicType) {
        final org.eclipse.cdt.core.dom.ast.c.ICBasicType c =
          (org.eclipse.cdt.core.dom.ast.c.ICBasicType) t;

        return new CBasicType(t.getType(), t.isLong(), t.isShort(), t.isSigned(),
          t.isUnsigned(), c.isComplex(), c.isImaginary(), c.isLongLong());

      } else if (t.getType() == org.eclipse.cdt.core.dom.ast.IBasicType.t_void) {
          
          // the three values isComplex, isImaginary, isLongLong are initialized
          // with FALSE, because we do not know about them
          return new CBasicType(t.getType(), t.isLong(), t.isShort(),
            t.isSigned(), t.isUnsigned(), false, false, false);

      } else {
        throw new CFAGenerationRuntimeException("Unknown type " + t.toString());
      }
      
    } catch (org.eclipse.cdt.core.dom.ast.DOMException e) {
      throw new CFAGenerationRuntimeException(e.getMessage());
    }
  }
  
  private static IPointerType convert(org.eclipse.cdt.core.dom.ast.IPointerType t) {
    try {
      return new IPointerType(convert(t.getType()), t.isConst(), t.isVolatile());
    } catch (org.eclipse.cdt.core.dom.ast.DOMException e) {
      throw new CFAGenerationRuntimeException(e.getMessage());
    }
  }
  
  private static ITypedef convert(org.eclipse.cdt.core.dom.ast.ITypedef t) {
    try {
      return new ITypedef(t.getName(), convert(t.getType()));
    } catch (org.eclipse.cdt.core.dom.ast.DOMException e) {
      throw new CFAGenerationRuntimeException(e.getMessage());
    }
  }
}
