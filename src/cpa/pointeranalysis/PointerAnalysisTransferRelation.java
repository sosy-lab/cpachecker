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
package cpa.pointeranalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cfa.objectmodel.c.ReturnEdge;
import cfa.objectmodel.c.StatementEdge;
import cmdline.CPAMain;

import common.Pair;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.explicit.ExplicitAnalysisElement;
import cpa.pointeranalysis.Memory.GlobalVariable;
import cpa.pointeranalysis.Memory.InvalidPointerException;
import cpa.pointeranalysis.Memory.LocalVariable;
import cpa.pointeranalysis.Memory.MemoryAddress;
import cpa.pointeranalysis.Memory.MemoryRegion;
import cpa.pointeranalysis.Memory.PointerTarget;
import cpa.pointeranalysis.Memory.Variable;
import cpa.pointeranalysis.PointerAnalysisDomain.IPointerAnalysisElement;
import cpa.types.Type;
import cpa.types.TypesElement;
import cpa.types.Type.PointerType;
import exceptions.CPAException;
import exceptions.CPATransferException;
import exceptions.TransferRelationException;
import exceptions.UnrecognizedCFAEdgeException;

/**
 * @author Philipp Wendler
 */
public class PointerAnalysisTransferRelation implements TransferRelation {

  private static final String RETURN_VALUE_VARIABLE = "___cpa_temp_result_var_";

  /**
   * Here some information about the last action is stored;
   * the strengthen operator can use this to find out what information could be
   * updated.
   * 
   * This information is stored in a separate object which can be garbage
   * collected after it was used, this reduces the memory footprint of a
   * PoitnerAnalysisElement.
   */
  private static class MissingInformation {
    private Pointer         typeInformationPointer = null;
    private DeclarationEdge typeInformationEdge = null;
    
    private Pointer         actionLeftPointer = null;
    private Pointer         actionRightPointer = null;
    private boolean         actionDereferenceFirst = false;
    private boolean         actionOffsetNegative = false;
    private IASTNode        actionASTNode = null;
    
    private MemoryAddress   mallocSizeMemory = null;
    private IASTNode        mallocSizeASTNode = null;
  }
  
  private MissingInformation missing = null;
  
  private final PointerAnalysisDomain domain;
  
  private static boolean printWarnings = Boolean.parseBoolean(CPAMain.cpaConfig.getProperty("pointerAnalysis.printWarnings", "false"));
  private static Set<Pair<Integer, String>> warnings
                  = printWarnings ? new HashSet<Pair<Integer, String>>() : null;

  public static void addWarning(String message, CFAEdge edge, String variable) {
    addMessage("Warning: " + message, edge, variable);
  }
  
  public static void addError(String message, CFAEdge edge, String variable) {
    addMessage("ERROR: " + message, edge, variable);
  }
  
  private static void addMessage(String message, CFAEdge edge, String variable) {
    if (printWarnings) {
      Integer lineNumber = null;
      if (edge != null) {
        lineNumber = edge.getSuccessor().getLineNumber();
      }
      
      Pair<Integer, String> warningIndex = new Pair<Integer, String>(lineNumber, variable);
      if (!warnings.contains(warningIndex)) {
        warnings.add(warningIndex);
        if (lineNumber != null) {
          System.err.println(message + " in line " + lineNumber+": "
              + edge.getRawStatement());
        } else {
          System.err.println(message);
        }
      }
    }
  }

                  
  public PointerAnalysisTransferRelation(PointerAnalysisDomain domain) {
    this.domain = domain;
  }
  
  @Override
  public IPointerAnalysisElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge,
                                              Precision precision)
                                              throws CPATransferException {
    PointerAnalysisElement successor = ((PointerAnalysisElement)element).clone();
    successor.setCurrentEdge(cfaEdge);
    IPointerAnalysisElement result = successor;
    
    switch (cfaEdge.getEdgeType()) {

    case DeclarationEdge:
      try {
        handleDeclaration(successor, (DeclarationEdge)cfaEdge);
      } catch (TransferRelationException e) {
        e.printStackTrace();
      }
      break;
    
    case StatementEdge:
      try {
        result = handleStatement(successor, ((StatementEdge)cfaEdge).getExpression(), cfaEdge);
      } catch (TransferRelationException e) {
        e.printStackTrace();
      } catch (InvalidPointerException e) {
        e.printStackTrace();
        return domain.getBottomElement();
      }
      break;
      
    case AssumeEdge:
      try {
        AssumeEdge assumeEdge = (AssumeEdge)cfaEdge;
        result = handleAssume(successor, assumeEdge.getExpression(),
                              assumeEdge.getTruthAssumption(), assumeEdge);
      } catch (TransferRelationException e) {
        e.printStackTrace();
      }
      break;

    case FunctionCallEdge:
      handleFunctionCall(successor, cfaEdge);
      break;
      
    case ReturnEdge:
      // now handle the complete a = func(x) statement in the CallToReturnEdge
      ReturnEdge returnEdge = (ReturnEdge)cfaEdge;
      CallToReturnEdge ctrEdge = returnEdge.getSuccessor().getEnteringSummaryEdge();
      try {
        handleReturnFromFunction(successor, ctrEdge.getExpression(), ctrEdge);
      } catch (InvalidPointerException e) {
        e.printStackTrace();
      } catch (TransferRelationException e) {
        e.printStackTrace();
      }
      break;
      
    case BlankEdge:
      break;
      
    case CallToReturnEdge:
    case MultiStatementEdge:
    case MultiDeclarationEdge:
      assert false;
      break;
    
    default:
      try {
        throw new UnrecognizedCFAEdgeException("Unknown edge type");
      } catch (UnrecognizedCFAEdgeException e) {
        e.printStackTrace();
      }
    }
    if (result == domain.getBottomElement()) {
      System.out.println("Stopping pointer analysis on line " + cfaEdge);
    }
    return result;
  }

  private void handleDeclaration(PointerAnalysisElement element,
                                 DeclarationEdge declaration)
                                 throws TransferRelationException {
    if (declaration.getDeclSpecifier().getStorageClass() == IASTDeclSpecifier.sc_typedef) {
      // ignore, this is a type definition, not a variable declaration
      return;
    }
    
    IASTDeclarator[] declarators = declaration.getDeclarators();
    if (declarators == null || declarators.length != 1) {
      throw new TransferRelationException("Not expected in CIL: " + declaration.getRawStatement());  
    }
  
    String varName = declarators[0].getName().toString();

    IASTPointerOperator[] operators = declarators[0].getPointerOperators();
    if (operators != null && operators.length > 0) {
    
      Pointer p = new Pointer(operators.length);
      
      if (declaration instanceof GlobalDeclarationEdge) {
        element.addNewGlobalPointer(varName, p);
      } else {
        element.addNewLocalPointer(varName, p);
        element.pointerOp(new PointerOperation.Assign(Memory.INVALID_POINTER), p);
      }
      // store the pointer so the type analysis CPA can update its
      // type information
      missing = new MissingInformation();
      missing.typeInformationPointer = p;
      missing.typeInformationEdge    = declaration;
      
      // initializers do not need to be considered, because they have to be
      // constant and constant pointers are considered null
      // local variables do not have initializers in CIL
    
    } else {
      // store all global variables, so we know whether a variable is global or
      // local
      if (declaration instanceof GlobalDeclarationEdge) {
        element.addNewGlobalPointer(varName, null);
      }
    }
  }
  
  private IPointerAnalysisElement handleAssume(PointerAnalysisElement element,
                                              IASTExpression expression,
                                              boolean isTrueBranch,
                                              AssumeEdge assumeEdge)
                                              throws TransferRelationException {
    
    if (expression instanceof IASTBinaryExpression) {
      IASTBinaryExpression binaryExpression = (IASTBinaryExpression)expression; 
      
      if (binaryExpression.getOperator() == IASTBinaryExpression.op_equals) {
        return handleBinaryAssume(element, binaryExpression, isTrueBranch, assumeEdge);
        
      } else if (binaryExpression.getOperator() == IASTBinaryExpression.op_notequals) {
        return handleBinaryAssume(element, binaryExpression, !isTrueBranch, assumeEdge);
      
      } else {
        throw new TransferRelationException("Not expected in CIL: " + assumeEdge.getRawStatement());        
      }
        
    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      
      if (unaryExpression.getOperator() == IASTUnaryExpression.op_not) {
        return handleAssume(element, unaryExpression.getOperand(), !isTrueBranch, assumeEdge);
      
      } else {
        throw new TransferRelationException("Not expected in CIL: " + assumeEdge.getRawStatement());
      }
      
    } else if (expression instanceof IASTIdExpression) {
      // if (a)
      String varName = expression.getRawSignature();
      Pointer p = element.getPointer(varName);
      boolean isNull = p.contains(Memory.NULL_POINTER);
      
      if (isTrueBranch && isNull && p.getNumberOfTargets() == 1) {
        // p is always null here -> this branch is never reached
        return domain.getBottomElement();
      }
      if (!isTrueBranch && !isNull) {
        // p is never null here -> this branch is never reached
        return domain.getBottomElement();
      }
      
      if (isTrueBranch) {
        // p holds, i.e. p != 0 holds, i.e. p cannot point to null
        element.pointerOpAssumeInequality(p, Memory.NULL_POINTER);
        
      } else {
        // !p holds, i.e. p == 0 holds, i.e. p points to null
        element.pointerOpAssumeEquality(p, Memory.NULL_POINTER);
      }
    }
    return element;
  }
  
  private IPointerAnalysisElement handleBinaryAssume(PointerAnalysisElement element,
                                                    IASTBinaryExpression expression,
                                                    boolean isTrueBranch,
                                                    AssumeEdge assumeEdge)
                                                    throws TransferRelationException {
    
    IASTExpression leftOp = expression.getOperand1();
    IASTExpression rightOp = expression.getOperand2();
    Pointer leftPointer = element.getPointer(leftOp.getRawSignature());
    Pointer rightPointer = element.getPointer(rightOp.getRawSignature());
    
    if (leftPointer != null && rightPointer != null) {
      
      if (element.areAliases(leftPointer, rightPointer)) {
        // surely equal
        
        if (!isTrueBranch) {
          // op1 != op2 is never true
          return domain.getBottomElement();
        }
      
      } else if (leftPointer.isDifferentFrom(rightPointer)) {
        // never equal
        
        if (isTrueBranch) {
          // op1 == op2 is never true
          return domain.getBottomElement();
        }
        
      } else {
        
        if (isTrueBranch) {
          element.pointerOpAssumeEquality(leftPointer, rightPointer);
        } else {
          element.pointerOpAssumeInequality(leftPointer, rightPointer);
        }
       
      }
    }
    return element;
  }
  
  private void handleFunctionCall(PointerAnalysisElement element,
                                  CFAEdge cfaEdge) {
    
    FunctionDefinitionNode funcDefNode = (FunctionDefinitionNode)cfaEdge.getSuccessor();
    String funcName = funcDefNode.getFunctionName();
    
    List<String> formalParameters = funcDefNode.getFunctionParameterNames();
    IASTExpression[] actualParameters = ((FunctionCallEdge)cfaEdge).getArguments();
    
    if (formalParameters != null && formalParameters.size() > 0
        && actualParameters != null && actualParameters.length > 0) {
    
      ArrayList<Pointer> actualValues = new ArrayList<Pointer>();
      
      assert formalParameters.size() == actualParameters.length;
                     
      for (int i = 0; i < actualParameters.length; i++) {
        IASTExpression parameter = actualParameters[i];
        
        if (parameter instanceof IASTIdExpression) {
          Pointer p = element.getPointer(parameter.getRawSignature());
          actualValues.add(p); // either a pointer or null
        
        } else if (parameter instanceof IASTLiteralExpression
                  && Integer.valueOf(0).equals(parseIntegerLiteral((IASTLiteralExpression)parameter))) {
          actualValues.add(new Pointer()); // null pointer
        
        } else {
          // probably not a pointer
          actualValues.add(null);
        }
      }
      
      element.callFunction(funcName);
      
      for (int i = 0; i < actualValues.size(); i++) {
        Pointer value = actualValues.get(i);
        System.out.println("Calling function " + element.getCurrentFunctionName() + " with parameter " + formalParameters.get(i) + "=" + value);
        if (value != null) {
          Pointer parameter = new Pointer();
          element.addNewLocalPointer(formalParameters.get(i), parameter); // sets location
          element.pointerOp(new PointerOperation.Assign(value), parameter);
        }
      }

    } else {
      element.callFunction(funcName);
    }
    
    element.addNewLocalPointer(RETURN_VALUE_VARIABLE, new Pointer());
  }
  
  private Integer parseIntegerLiteral(IASTLiteralExpression expression) {
    try {
      String s = expression.getRawSignature();
      if (s.endsWith("U")) {
        s = s.substring(0, s.length()-1);
      }
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      return null;
    }
  }

  private void handleReturnFromFunction(PointerAnalysisElement element,
                                        IASTExpression expression,
                                        CFAEdge cfaEdge)
                                        throws InvalidPointerException, TransferRelationException {
    
    Pointer resultPointer = element.getPointer(RETURN_VALUE_VARIABLE);
    // resultPointer does not reliably indicate if the function returns a pointer!
    // E.g., without a type information CPA, return 0 will lead to a pointer result
    // even if the function returns an int
    System.out.println(element.getCurrentFunctionName() + " returns " + resultPointer);
    
    // check for references to stack variables in result pointer
    for (PointerTarget resultTarget : resultPointer.getTargets()) {
      if (resultTarget instanceof LocalVariable) {
        LocalVariable var = (LocalVariable)resultTarget;
        String function = element.getCurrentFunctionName();
        if (function.equals(var.getFunctionName())) {
          // function returns a reference to a local variable
          addWarning("Function " + function + " returns reference to local variable "
                     + resultTarget, cfaEdge, resultTarget.toString());
        }
      }
    }

    
    element.returnFromFunction(); // throw away local context
    

    // use function result
    if (expression instanceof IASTBinaryExpression) {
      // a = func()
      IASTBinaryExpression binExpression = (IASTBinaryExpression)expression;
      IASTExpression leftOperand = binExpression.getOperand1();

      if (leftOperand instanceof IASTIdExpression) {
        Pointer leftPointer = element.getPointer(leftOperand.getRawSignature());
      
        if (leftPointer != null) {
          if (binExpression.getOperator() == IASTBinaryExpression.op_assign) {
            if (resultPointer != null) {
              element.pointerOp(new PointerOperation.Assign(resultPointer), leftPointer);
            } else {
              throw new TransferRelationException("Assigning non-pointer value to pointer variable: " + cfaEdge.getRawStatement());
            }
        
          } else {
            // a += func()
            throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
          }

        } else {
          // function result is not assigned to a pointer, ignore
        }
        
      } else {
        // *x = func() etc.
        throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());  
      }
      
    } else if (expression instanceof IASTFunctionCallExpression) {
      // func()
      // ignore
    } else {
      throw new TransferRelationException("Invalid C code: " + cfaEdge.getRawStatement());  
    }
    
    // check for memory leaks
    // TODO better location for calling checkMemoryLeak()? regularly? at end of analysis?
    Collection<MemoryRegion> lostRegions = element.checkMemoryLeak();
    try {
      for (MemoryRegion lostRegion : lostRegions) {
        addWarning("Memory leak: " + lostRegion + " is not freed and has no known pointer towards it",
                   cfaEdge, lostRegion.toString());
        element.free(lostRegion);
      }
    } catch (InvalidPointerException e) {
      // happens only on double free, which is obviously not the case here
      e.printStackTrace();
    }
        
  }
  
  private IPointerAnalysisElement handleStatement(PointerAnalysisElement element,
                               IASTExpression expression, CFAEdge cfaEdge)
                               throws TransferRelationException,
                                      InvalidPointerException {
    
    if (cfaEdge.isJumpEdge()) {
      // this is the return-statement of a function
      
      // Normally, the resultPointer is there, but if we know through a type
      // information CPA that this function does not return a pointer, it's not.
      
      Pointer resultPointer = element.getPointer(RETURN_VALUE_VARIABLE);
      if (resultPointer != null) {
        handleAssignment(element, resultPointer, false, expression, cfaEdge);
      }
      
    } else if (expression instanceof IASTUnaryExpression) {
      // this is an unary operation (a++)
      
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      if (unaryExpression.getOperand() instanceof IASTIdExpression) {
        Pointer p = element.getPointer(unaryExpression.getOperand().getRawSignature());
        
        if (p != null) {
          int typeOfOperator = unaryExpression.getOperator();
          if (   typeOfOperator == IASTUnaryExpression.op_postFixIncr
              || typeOfOperator == IASTUnaryExpression.op_prefixIncr) {
            
            element.pointerOp(new PointerOperation.AddOffset(1), p);
          
          } else if (
                 typeOfOperator == IASTUnaryExpression.op_postFixIncr
              || typeOfOperator == IASTUnaryExpression.op_prefixIncr) {
            
            element.pointerOp(new PointerOperation.AddOffset(-1), p);
          
          } else {
            throw new TransferRelationException("Invalid C code: " + cfaEdge.getRawStatement());  
          }
        }
      }
      
    } else if (expression instanceof IASTFunctionCallExpression) {
      // this is a mere function call (func(a))
      IASTFunctionCallExpression funcExpression = (IASTFunctionCallExpression)expression;
      String functionName = funcExpression.getFunctionNameExpression().getRawSignature();

      if (functionName.equals("free")) {
        
        IASTExpression parameter = funcExpression.getParameterExpression();
        if (parameter instanceof IASTIdExpression) {
          Pointer p = element.getPointer(parameter.getRawSignature());
        
          if (p == null) {
            addWarning("Freeing unknown pointer " + parameter.getRawSignature(),
                       cfaEdge, parameter.getRawSignature());
          } else {
            element.free(p);
          }
        } else {
          throw new TransferRelationException("This code is not expected in CIL: "
              + cfaEdge.getRawStatement());
        }
              
      } else if (functionName.equals("malloc")) {
        // malloc without assignment (will lead to memory leak)
        addWarning("Memory leak because of calling malloc without using the return value!",
                   cfaEdge, "");
      }      
    
    } else if (expression instanceof IASTBinaryExpression) {
      // statement is a binary expression, e.g. a = b or a += b;
      return handleBinaryStatement(element, (IASTBinaryExpression)expression, cfaEdge);
      
    } else {
      throw new TransferRelationException("Invalid C code: " + cfaEdge.getRawStatement());
    }
    return element;
  }

  private IPointerAnalysisElement handleBinaryStatement(PointerAnalysisElement element,
                                     IASTBinaryExpression expression,
                                     CFAEdge cfaEdge)
                                     throws TransferRelationException,
                                     InvalidPointerException {
    
    
    IASTExpression leftExpression = expression.getOperand1();
    Pointer leftPointer;
    boolean leftDereference;
    
    if (leftExpression instanceof IASTIdExpression) {
      // a
      leftDereference = false;
      leftPointer = element.getPointer(leftExpression.getRawSignature());
      
    } else if (leftExpression instanceof IASTUnaryExpression) {
      leftDereference = true;
      
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)leftExpression;
      if (unaryExpression.getOperator() == IASTUnaryExpression.op_star) {
        // *a
        leftPointer = element.getPointer(unaryExpression.getOperand().getRawSignature());
        
        if (leftPointer == null) {
          throw new InvalidPointerException("Dereferencing a non-pointer: " + cfaEdge.getRawStatement());
        }
        
        if (!leftPointer.isDereferencable()) {
          addError("Unsafe deref of pointer " + unaryExpression.getOperand().getRawSignature()
                    + " = " + leftPointer, cfaEdge, unaryExpression.getRawSignature());
          return domain.getBottomElement();
        }
        
        if (!leftPointer.isSafe()) {
          addWarning("Potentially unsafe deref of pointer " + unaryExpression.getOperand().getRawSignature()
                     + " = " + leftPointer, cfaEdge, unaryExpression.getRawSignature());
        }
        
        if (!leftPointer.isPointerToPointer()) {
          // other pointers are not of interest to us
          leftPointer = null;
        }
        
      } else {
        throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
      }
    } else {
      // TODO fields, arrays
      throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
    }
    
    if (leftPointer == null) {
      // writing to a non-pointer variable -- ignore
      return element;
    }
    
    int typeOfOperator = expression.getOperator();
    IASTExpression op2 = expression.getOperand2();
    
    if (typeOfOperator == IASTBinaryExpression.op_assign) {
      // handles *a = x and a = x
      handleAssignment(element, leftPointer, leftDereference, op2, cfaEdge);
    
    } else if (
           typeOfOperator == IASTBinaryExpression.op_minusAssign
        || typeOfOperator == IASTBinaryExpression.op_plusAssign
        ) {
      // a += x 
      
      if (op2 instanceof IASTLiteralExpression) {
        // a += 5
        int offset = Integer.parseInt(op2.getRawSignature());
        if (typeOfOperator == IASTBinaryExpression.op_minusAssign) {
          offset = -offset;
        }
        element.pointerOp(new PointerOperation.AddOffset(offset), leftPointer, leftDereference);
      
      } else if (op2 instanceof IASTIdExpression) {
        // a += b
        missing = new MissingInformation();
        missing.actionLeftPointer = leftPointer;
        missing.actionDereferenceFirst = leftDereference;
        missing.actionASTNode = op2;
        missing.actionOffsetNegative = (typeOfOperator == IASTBinaryExpression.op_minusAssign);
      
      } else {
        throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
      }
    }
    return element;
  }

  /**
   * Handles an assignment, where the left-hand side is a pointer.
   * If the right-hand side seems to not evaluate to a pointer, the left pointer
   * is just set to unknown (no warning / error etc. is produced). 
   */
  private void handleAssignment(PointerAnalysisElement element,
                                Pointer leftPointer, boolean leftDereference,
                                IASTExpression expression, CFAEdge cfaEdge)
                                throws TransferRelationException,
                                       InvalidPointerException {
    
    if (expression instanceof IASTLiteralExpression) {
      // a = 0
      element.pointerOp(new PointerOperation.Assign(Memory.NULL_POINTER), leftPointer, leftDereference);

    } else if (expression instanceof IASTCastExpression) {
      // a = (int*)b
      // ignore cast, we do no type-checking
      handleAssignment(element, leftPointer, leftDereference, ((IASTCastExpression)expression).getOperand(), cfaEdge);
      
    } else if (expression instanceof IASTFunctionCallExpression) {
      // a = func()
      
      IASTFunctionCallExpression funcExpression = (IASTFunctionCallExpression)expression;
      String functionName = funcExpression.getFunctionNameExpression().getRawSignature();
      
      if (functionName.equals("malloc")) {
        handleMalloc(element, leftPointer, leftDereference, funcExpression.getParameterExpression());
        
      } else {
        // if it's an internal call, it's handled in handleReturnFromFunction()
        // it it's an external call and we do not know the function, we cannot
        // do more than set the pointer to unknown
        element.pointerOp(new PointerOperation.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);
      }
    
    } else if (expression instanceof IASTBinaryExpression) {
      // a = b + c
      IASTBinaryExpression binExpression = (IASTBinaryExpression)expression;
      int typeOfOperator = binExpression.getOperator();
      IASTExpression op1 = binExpression.getOperand1();
      IASTExpression op2 = binExpression.getOperand2();

      if (!(op1 instanceof IASTIdExpression)) {
        // in p1 = p2 + x, CIL makes p2 always the left operand of the addition 
        element.pointerOp(new PointerOperation.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);
        throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement()); 
      }
      Pointer rightPointer = element.getPointer(op1.getRawSignature());
      if (rightPointer != null) {
        
        if (!(typeOfOperator == IASTBinaryExpression.op_plus
            || typeOfOperator == IASTBinaryExpression.op_minus)) {
          element.pointerOp(new PointerOperation.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);
          throw new TransferRelationException("Invalid C code: " + cfaEdge.getRawStatement());
        }

        if (op2 instanceof IASTLiteralExpression) {
          String s = op2.getRawSignature();
          if (s.endsWith("U")) {
            s = s.substring(0, s.length()-1);
          }
          int offset = Integer.parseInt(s);
          if (typeOfOperator == IASTBinaryExpression.op_minus) {
            offset = -offset;
          }
          
          element.pointerOp(new PointerOperation.AddOffsetAndAssign(rightPointer, offset), leftPointer);

        } else if (op2 instanceof IASTIdExpression) {
          missing = new MissingInformation();
          missing.actionLeftPointer  = leftPointer;
          missing.actionRightPointer = rightPointer;
          missing.actionDereferenceFirst = leftDereference;
          missing.actionOffsetNegative = (typeOfOperator == IASTBinaryExpression.op_minus);
          missing.actionASTNode = op2;
          
        } else { 
          element.pointerOp(new PointerOperation.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);
          throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
        }
      
      } else {
        // probably assigning a non-pointer value to a pointer
        // set pointer to unknown and ignore otherwise
        element.pointerOp(new PointerOperation.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);
      }
      
    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      int op = unaryExpression.getOperator();
      IASTExpression operand = unaryExpression.getOperand();
      
      if (op == IASTUnaryExpression.op_bracketedPrimary) {
        // a = (x)
        handleAssignment(element, leftPointer, leftDereference, operand, cfaEdge);      
     
      } else if (op == IASTUnaryExpression.op_amper) {
        // a = &b

        String varName = operand.getRawSignature();

        Variable var;
        if (element.getGlobalPointers().containsKey(varName)) {
          var = new GlobalVariable(varName);
        } else {
          var = new LocalVariable(element.getCurrentFunctionName(), varName);
        }
        
        element.pointerOp(new PointerOperation.Assign(var), leftPointer, leftDereference);
        
      } else if (op == IASTUnaryExpression.op_star) {
        // a = *b
        
        Pointer rightPointer = element.getPointer(operand.getRawSignature());
        
        if (rightPointer != null) {
          element.pointerOp(new PointerOperation.DerefAndAssign(rightPointer), leftPointer, leftDereference);
        } else {
          addWarning("Dereferencing a non-pointer variable " + expression.getRawSignature(),
                     cfaEdge, operand.getRawSignature());
          element.pointerOp(new PointerOperation.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);
        }
      
      } else {
        element.pointerOp(new PointerOperation.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);
        throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
      }
      
    } else if (expression instanceof IASTIdExpression) {
      // a = b
      Pointer rightPointer = element.getPointer(expression.getRawSignature());
      
      if (rightPointer != null) {
        element.pointerOp(new PointerOperation.Assign(rightPointer), leftPointer, leftDereference);
      } else {
        addWarning("Assigning non-pointer variable " + expression.getRawSignature()
                   + " to pointer", cfaEdge, expression.getRawSignature());
        element.pointerOp(new PointerOperation.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);
      }
      
    } else {
      element.pointerOp(new PointerOperation.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);
      throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
    }
  }

  /**
   * Does a malloc and allocates the result to the given pointer.
   * @param element the abstract element
   * @param pointer the pointer for the result (may be null)
   * @param parameter the parameter to the malloc call in the AST
   * @throws InvalidPointerException if malloc fails
   * @throws NumberFormatException if argument is a number, not a valid integer
   * @throws TransferRelationException if parameter contains something unexpected
   */
  private void handleMalloc(PointerAnalysisElement element, Pointer pointer,
                            boolean leftDereference,
                            IASTExpression parameter)
                            throws InvalidPointerException,
                                   TransferRelationException {

    PointerOperation.MallocAndAssign op = new PointerOperation.MallocAndAssign();
    element.pointerOp(op, pointer, leftDereference);
    MemoryAddress memAddress = op.getMallocResult();
    
    if (parameter instanceof IASTLiteralExpression) {
      String s = parameter.getRawSignature();
      if (s.endsWith("U")) {
        s = s.substring(0, s.length()-1);
      }
      try {
        // TODO replace with parseIntegerLiteral()
        memAddress.getRegion().setLength(Integer.parseInt(s));
      } catch (NumberFormatException e) {
        throw new TransferRelationException("Invalid C code: malloc(" + parameter.getRawSignature() + ")");
      }
      
    } else if (parameter instanceof IASTIdExpression) {
      // store variable name so the strengthen operator can update the length
      // information if he knows it
      
      missing = new MissingInformation();
      missing.mallocSizeMemory = memAddress;
      missing.mallocSizeASTNode = parameter;
    
    } else {
      throw new TransferRelationException("Not expected in CIL: malloc(" + parameter.getRawSignature() + ")");
    }
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
  public AbstractElement strengthen(AbstractElement element, List<AbstractElement> elements,
                         CFAEdge cfaEdge, Precision precision) {
    if (missing == null) {
      return null;
    }
    if (!(element instanceof PointerAnalysisElement)) {
      return null;
    }
    PointerAnalysisElement pointerElement = (PointerAnalysisElement)element;
    
    for (AbstractElement ae : elements) {
      try {
        if (ae instanceof ExplicitAnalysisElement) {
          strengthen(pointerElement, (ExplicitAnalysisElement)ae, cfaEdge, precision);
        
        } else if (ae instanceof TypesElement) {
          strengthen(pointerElement, (TypesElement)ae, cfaEdge, precision);
        }
      } catch (TransferRelationException e) {
        e.printStackTrace();
      } catch (InvalidPointerException e) {
        e.printStackTrace();
        return domain.getBottomElement();
      }
    }
    
    if (missing != null && missing.actionLeftPointer != null) {
      // strengthen operator did not get the necessary information
      // necessary do to the backup operation with unknown offset
      // this has to be here so it gets executed if there is no ExplicitAnalysis
      
      PointerOperation<InvalidPointerException> op;
      if (missing.actionRightPointer != null) {
        op = new PointerOperation.AddUnknownOffsetAndAssign(missing.actionRightPointer);
      } else {
        op = new PointerOperation.AddUnknownOffset();
      }
      try {
        pointerElement.pointerOp(op, missing.actionLeftPointer, missing.actionDereferenceFirst);
      } catch (InvalidPointerException e) {
        e.printStackTrace();
        return domain.getBottomElement();
      }
    }

    missing = null;
    return null;
  }
  
  private void strengthen(PointerAnalysisElement pointerElement, 
                          ExplicitAnalysisElement explicitElement,
                          CFAEdge cfaEdge, Precision precision) throws InvalidPointerException {
    
    if (missing.mallocSizeMemory != null) {
      Long value = getVariableContent(missing.mallocSizeASTNode, explicitElement, cfaEdge);
      if (value != null) {
        missing.mallocSizeMemory.getRegion().setLength(value);
      }
    }
    
    if (missing.actionLeftPointer != null) {
      Long value = getVariableContent(missing.actionASTNode, explicitElement, cfaEdge);
      if (value != null) {
        int val = value.intValue();
        if (missing.actionOffsetNegative) {
          val = -val;
        }
      
        PointerOperation<InvalidPointerException> op;
        if (missing.actionRightPointer != null) {
          op = new PointerOperation.AddOffsetAndAssign(missing.actionRightPointer, val);
        
        } else {
          op = new PointerOperation.AddOffset(val);
        }
        pointerElement.pointerOp(op, missing.actionLeftPointer, missing.actionDereferenceFirst);

        // mark operation as completed successfully
        missing.actionLeftPointer  = null;
        
      } else {
        // getting variable content failed
        // backup action (adding unknown offset) will be done by caller as if
        // there was no ExplicitAnalysis
      }
    }
    
  }
  
  private Long getVariableContent(IASTNode variable,
                                     ExplicitAnalysisElement explicitElement,
                                     CFAEdge cfaEdge) {
    
    String varName = variable.getRawSignature();
    if (!explicitElement.contains(varName)) {
      varName = cfaEdge.getPredecessor().getFunctionName() + "::" + varName;
    }
    
    if (explicitElement.contains(varName)) {
      return explicitElement.getValueFor(varName);
    } else {
      return null;
    }
  }
  
  private void strengthen(PointerAnalysisElement pointerElement, 
                          TypesElement typesElement,
                          CFAEdge cfaEdge, Precision precision)
                          throws TransferRelationException {

    if (missing.typeInformationPointer == null) {
      return;
    }
    
    // pointer variable declaration
    String functionName = cfaEdge.getSuccessor().getFunctionName();
    if (missing.typeInformationEdge instanceof GlobalDeclarationEdge) {
      functionName = null;
    }
    
    IASTDeclarator[] declarators = missing.typeInformationEdge.getDeclarators();
    if (declarators == null || declarators.length != 1) {
       throw new TransferRelationException("Not expected in CIL: " + missing.typeInformationEdge.getRawStatement());
    }
    
    String name = declarators[0].getName().getRawSignature();
    Type type = typesElement.getVariableType(functionName, name);
    
    if (!(type instanceof PointerType)) {
      throw new TransferRelationException("Types determined by TypesCPA und PointerAnalysisCPA differ!");
    }

    int sizeOfTarget = ((PointerType)type).getTargetType().sizeOf();
    missing.typeInformationPointer.setSizeOfTarget(sizeOfTarget);
  }
}