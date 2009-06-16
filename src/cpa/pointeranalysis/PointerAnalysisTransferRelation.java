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
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cfa.objectmodel.c.ReturnEdge;
import cfa.objectmodel.c.StatementEdge;
import cmdline.CPAMain;

import common.Pair;

import cpa.common.interfaces.AbstractDomain;
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
import exceptions.CPAException;
import exceptions.CPATransferException;
import exceptions.TransferRelationException;
import exceptions.UnrecognizedCFAEdgeException;

/**
 * @author Philipp Wendler
 */
public class PointerAnalysisTransferRelation implements TransferRelation {

  private static final String RETURN_VALUE_VARIABLE = "___cpa_temp_result_var_";
  
  // here some information about the last action is stored;
  // the strengthen operator can use this to find out what information could be
  // updated
  private MemoryAddress lastActionMalloc = null;
  private Pointer       lastActionPointer = null;
  private IASTNode      lastActionASTNode = null;
  private Pointer       lastActionPointerBackup = null;
  private boolean       lastActionOffsetNegative = false;
  
  private final AbstractDomain domain;
  
  private boolean printWarnings;
  private Set<Pair<Integer, String>> warnings;
  
  public PointerAnalysisTransferRelation(PointerAnalysisDomain domain) {
    this.domain = domain;
    printWarnings = Boolean.parseBoolean(CPAMain.cpaConfig.getProperty("pointerAnalysis.printWarnings", "false"));
    if (printWarnings) {
      warnings = new HashSet<Pair<Integer, String>>();
    }
  }
  
  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge,
                                              Precision precision)
                                              throws CPATransferException {
    
    PointerAnalysisElement successor = ((PointerAnalysisElement)element).clone();
    
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
        handleStatement(successor, ((StatementEdge)cfaEdge).getExpression(), cfaEdge);
      } catch (TransferRelationException e) {
        e.printStackTrace();
      } catch (InvalidPointerException e) {
        e.printStackTrace();
        return domain.getBottomElement();
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
    return successor;
  }

  private void addWarning(String message, CFAEdge edge, String variable) {
    if (printWarnings) {
      Integer lineNumber = null;
      if (edge != null) {
        lineNumber = edge.getSuccessor().getLineNumber();
      }
      
      Pair<Integer, String> warningIndex = new Pair<Integer, String>(lineNumber, variable);
      if (!warnings.contains(warningIndex)) {
        warnings.add(warningIndex);
        if (lineNumber != null) {
          System.out.println("Warning: " + message + " in line " + lineNumber+": "
              + edge.getRawStatement());
        } else {
          System.out.println("Warning: " + message);
        }
      }
    }
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
      throw new TransferRelationException("Unhandled case " + declaration.getRawStatement());  
    }
  
    String varName = declarators[0].getName().toString();

    IASTPointerOperator[] operators = declarators[0].getPointerOperators();
    if (operators != null && operators.length > 0) {
    
      Pointer p = new Pointer(operators.length);
      
      if (declaration instanceof GlobalDeclarationEdge) {
        element.addNewGlobalPointer(varName, p);
      } else {
        element.addNewLocalPointer(varName, p);
        p.setTarget(Memory.INVALID_POINTER);
      }
      // store the pointer so the type analysis CPA can update its
      // type information
      lastActionPointer = p;
      lastActionASTNode = declaration.getDeclSpecifier();
      
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
          actualValues.add(p); // either null or a pointer
        
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
        if (actualValues.get(i) != null) {
          element.addNewLocalPointer(formalParameters.get(i), actualValues.get(i));
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
    
    element.returnFromFunction(); // throw away local context
    
    // use function result
    if (expression instanceof IASTBinaryExpression) {
      // a = func()
      IASTBinaryExpression binExpression = (IASTBinaryExpression)expression;
      IASTExpression leftOperand = binExpression.getOperand1();

      if (leftOperand instanceof IASTIdExpression) {
        Pointer leftPointer = element.getPointer(leftOperand.getRawSignature());
      
        if (leftPointer != null) {
          leftPointer.setTarget(Memory.UNKNOWN_POINTER); // invalidate old pointer

          if (binExpression.getOperator() == IASTBinaryExpression.op_assign) {
            if (resultPointer != null) {
              leftPointer.makeAlias(resultPointer);
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
        addWarning("Memory leak: " + lostRegion + " is not freed and has no known pointer towards it, found", cfaEdge, lostRegion.toString());
        element.free(lostRegion);
      }
    } catch (InvalidPointerException e) {
      // happens only on double free, which is obviously not the case here
      e.printStackTrace();
    }
        
  }
  
  private void handleStatement(PointerAnalysisElement element,
                               IASTExpression expression, CFAEdge cfaEdge)
                               throws TransferRelationException,
                                      InvalidPointerException {
    
    if (cfaEdge.isJumpEdge()) {
      // this is the return-statement of a function
      
      // Normally, the resultPointer is there, but if we know through a type
      // information CPA that this function does not return a pointer, it's not.
      
      Pointer resultPointer = element.getPointer(RETURN_VALUE_VARIABLE);
      if (resultPointer != null) {
        handleAssignment(element, resultPointer, expression, cfaEdge);
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
            p.addOffset(1);
          
          } else if (
                 typeOfOperator == IASTUnaryExpression.op_postFixIncr
              || typeOfOperator == IASTUnaryExpression.op_prefixIncr) {
            p.addOffset(-1);
          
          } else {
            throw new TransferRelationException("Invalid C code: " + cfaEdge.getRawStatement());  
          }
        }
      }
      
    } else if (expression instanceof IASTFunctionCallExpression) {
      // this is a mere function call (func(a))
      IASTFunctionCallExpression funcExpression = (IASTFunctionCallExpression)expression;
      String functionName = funcExpression.getFunctionNameExpression().getRawSignature();
      IASTExpression parameter = funcExpression.getParameterExpression();

      if (functionName.equals("free")) {
        
        Pointer p = extractPointer(element, parameter, cfaEdge);
        if (p == null) {
          addWarning("Freeing unknown pointer " + parameter.getRawSignature(), cfaEdge, parameter.getRawSignature());
        } else {
          element.free(p);
        }
      
      } else if (functionName.equals("malloc")) {
        // malloc without assignment (will lead to memory leak) 
        handleMalloc(element, null, parameter);
      }      
    
    } else if (expression instanceof IASTBinaryExpression) {
      // statement is a binary expression, e.g. a = b or a += b;
      handleBinaryStatement(element, (IASTBinaryExpression)expression, cfaEdge);
      
    } else {
      throw new TransferRelationException("Invalid C code: " + cfaEdge.getRawStatement());
    }
  }
  
  /*private Pointer handleFunctionCall(PointerAnalysisElement element,
                                  IASTFunctionCallExpression expression,
                                  CFAEdge cfaEdge)
                                  throws InvalidPointerException,
                                         TransferRelationException {
    
    String functionName = expression.getFunctionNameExpression().getRawSignature();
    IASTExpression parameter = expression.getParameterExpression();

    if (functionName.equals("free")) {
      
      Pointer p = extractPointer(element, parameter, cfaEdge);
      if (p == null) {
        addWarning("Freeing unknown pointer " + parameter.getRawSignature(), cfaEdge, parameter.getRawSignature());
      } else {
        element.free(p);
      }
    
    } else if (functionName.equals("malloc")) {
      // malloc without assignment (will lead to memory leak) 
      return handleMalloc(element, null, parameter);
    }
    
    return null;
  }*/

  /**
   * This function extracts a pointer from everything, what can appear on the
   * left-hand side of an assignment in CIL (which is variables and
   * pointer dereferences)
   */
  private Pointer extractPointer(PointerAnalysisElement element,
                                 IASTExpression expression, CFAEdge cfaEdge)
                                 throws InvalidPointerException, TransferRelationException {
   
    if (expression instanceof IASTIdExpression) {
      // a
      return element.getPointer(expression.getRawSignature());
      
    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      if (unaryExpression.getOperator() == IASTUnaryExpression.op_star) {
        // *a
        Pointer pointer = element.getPointer(unaryExpression.getOperand().getRawSignature());
        
        if (pointer == null) {
          throw new InvalidPointerException("Dereferencing a non-pointer: " + cfaEdge.getRawStatement());
        }
        
        if (!pointer.isSafe()) {
          addWarning("Unsafe deref of pointer " + unaryExpression.getRawSignature(), cfaEdge, unaryExpression.getRawSignature());
        }
        
        if (pointer.isPointerToPointer()) {
          return pointer.deref(element);
        } else {
          // other pointers are not of interest to us
          return null;
        }
        
      } else {
        throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
      }
    } else {
      // TODO fields, arrays
      throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
    }
  }
  
  
  private void handleBinaryStatement(PointerAnalysisElement element,
                                     IASTBinaryExpression expression,
                                     CFAEdge cfaEdge)
                                     throws TransferRelationException,
                                     InvalidPointerException {
    
    Pointer leftPointer = extractPointer(element, expression.getOperand1(), cfaEdge);
    if (leftPointer == null) {
      // writing to a non-pointer variable -- ignore
      return;
    }
    
    int typeOfOperator = expression.getOperator();
    IASTExpression op2 = expression.getOperand2();
    
    if (typeOfOperator == IASTBinaryExpression.op_assign) {
      // handles *a = x and a = x
      handleAssignment(element, leftPointer, op2, cfaEdge);
    
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
        leftPointer.addOffset(offset);
      
      } else if (op2 instanceof IASTIdExpression) {
        // a += b
        Pointer backup = leftPointer.clone();
        leftPointer.addUnknownOffset();

        lastActionPointerBackup = backup;
        lastActionPointer = leftPointer;
        lastActionASTNode = op2;
        lastActionOffsetNegative = (typeOfOperator == IASTBinaryExpression.op_minusAssign);
      
      } else {
        throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
      }
    }
  }

  /**
   * Handles an assignment, where the left-hand side is a pointer.
   * If the right-hand side seems to not evaluate to a pointer, the left pointer
   * is just set to unknown (no warning / error etc. is produced). 
   */
  private void handleAssignment(PointerAnalysisElement element, Pointer p,
                                IASTExpression expression, CFAEdge cfaEdge)
                                throws TransferRelationException,
                                       InvalidPointerException {
    
    // first, invalidate old pointer (gets overwritten in any case)
    p.setTarget(Memory.UNKNOWN_POINTER);
    
    if (expression instanceof IASTLiteralExpression) {
      // a = 0
      p.setTarget(Memory.NULL_POINTER);

    } else if (expression instanceof IASTCastExpression) {
      // a = (int*)b
      // ignore cast, we do no type-checking
      handleAssignment(element, p, ((IASTCastExpression)expression).getOperand(), cfaEdge);
      
    } else if (expression instanceof IASTFunctionCallExpression) {
      // a = func()
      
      IASTFunctionCallExpression funcExpression = (IASTFunctionCallExpression)expression;
      String functionName = funcExpression.getFunctionNameExpression().getRawSignature();
      
      if (functionName.equals("malloc")) {
        handleMalloc(element, p, funcExpression.getParameterExpression());
        
      } else {
        // if it's an internal call, it's handled in handleReturnFromFunction()
        // it it's an external call and we do not know the function, we cannot
        // do more than set the pointer to unknown
        // (which is already done at the beginning of this method)
      }
    
    } else if (expression instanceof IASTBinaryExpression) {
      // a = b + c
      IASTBinaryExpression binExpression = (IASTBinaryExpression)expression;
      int typeOfOperator = binExpression.getOperator();
      IASTExpression op1 = binExpression.getOperand1();
      IASTExpression op2 = binExpression.getOperand2();

      if (!(op1 instanceof IASTIdExpression)) {
        // in p1 = p2 + x, CIL makes p2 always the left operand of the addition 
        throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement()); 
      }
      Pointer rightPointer = element.getPointer(op1.getRawSignature());
      if (rightPointer != null) {
        
        if (!(typeOfOperator == IASTBinaryExpression.op_plus
            || typeOfOperator == IASTBinaryExpression.op_minus)) {
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
          
          // theoretically, we would have to shift first, and then assign (shift
          // depends on type of pointer, which could change during assign
          // but CIL introduces explicit casts and temporary variables, so this
          // does not happen in CIL
          p.makeAlias(rightPointer);
          p.addOffset(offset);

        } else if (op2 instanceof IASTIdExpression) {
          p.makeAlias(rightPointer);
          p.addUnknownOffset();
          
          lastActionPointerBackup = rightPointer.clone();
          lastActionPointer = p;
          lastActionASTNode = op2;
          lastActionOffsetNegative = (typeOfOperator == IASTBinaryExpression.op_minus);
          
        } else { 
          throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
        }
      
      } else {
        // probably assigning a non-pointer value to a pointer
        // set pointer to unknown (done above) and ignore otherwise
      }
      
    } else if (expression instanceof IASTUnaryExpression
        && ((IASTUnaryExpression)expression).getOperator() == IASTUnaryExpression.op_bracketedPrimary) {
      // a = (x)
      handleAssignment(element, p, ((IASTUnaryExpression)expression).getOperand(), cfaEdge);      
      
    } else if (expression instanceof IASTUnaryExpression
         && ((IASTUnaryExpression)expression).getOperator() == IASTUnaryExpression.op_amper) {
      // a = &b

      String varName = ((IASTUnaryExpression)expression).getOperand().getRawSignature();

      if (element.getGlobalPointers().containsKey(varName)) {
        p.setTarget(new GlobalVariable(varName));
      } else {
        p.setTarget(new LocalVariable(varName));
      }
       
    } else {
      // this handles a = b and a = *b, throws an Exception if code is unknown
      Pointer rightPointer = extractPointer(element, expression, cfaEdge);
      
      if (rightPointer != null) {
        p.makeAlias(rightPointer);
      }
      
    }
  }

  /**
   * Does a malloc and allocates the result to the given pointer.
   * @param element the abstract element
   * @param pointer the pointer for the result (may be null)
   * @param parameter the parameter to the malloc call in the AST
   * @throws InvalidPointerException if malloc fails
   * @throws NumberFormatException if argument is a number, not a valid integer
   */
  private void handleMalloc(PointerAnalysisElement element, Pointer pointer,
                            IASTExpression parameter)
                            throws InvalidPointerException,
                                   NumberFormatException {
    PointerTarget mem = Memory.NULL_POINTER;
    
    if (parameter instanceof IASTLiteralExpression) {
      String s = parameter.getRawSignature();
      if (s.endsWith("U")) {
        s = s.substring(0, s.length()-1);
      }
      mem = element.malloc(Integer.parseInt(s));
      
    } else if (parameter instanceof IASTIdExpression) {
      // allocate now with unknown length, store variable name so the
      // strengthen operator can update the length information if he knows
      // it
      mem = element.malloc();
      lastActionASTNode = parameter;
      lastActionMalloc = (MemoryAddress)mem;
    }
    if (pointer != null) {
      pointer.setTarget(mem);
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
  public void strengthen(AbstractElement element, List<AbstractElement> elements,
                         CFAEdge cfaEdge, Precision precision) {
    PointerAnalysisElement pointerElement = (PointerAnalysisElement)element;
    
    for (AbstractElement ae : elements) {
      if (ae instanceof ExplicitAnalysisElement) {
        strengthen(pointerElement, (ExplicitAnalysisElement)ae, cfaEdge, precision);
      }
    }

    lastActionASTNode = null;
    lastActionMalloc = null;
    lastActionOffsetNegative = false;
    lastActionPointer = null;
    lastActionPointerBackup = null;
  }
  
  private void strengthen(PointerAnalysisElement pointerElement, 
                          ExplicitAnalysisElement explicitElement,
                          CFAEdge cfaEdge, Precision precision) {
    
    if (lastActionMalloc != null) {
      Integer value = getVariableContent(lastActionASTNode, explicitElement, cfaEdge);
      if (value != null) {
        lastActionMalloc.getRegion().setLength(value);
      }
    
    } else if (lastActionPointerBackup != null) {
      Integer value = getVariableContent(lastActionASTNode, explicitElement, cfaEdge);
      if (value != null) {
        int val = value.intValue();
        if (lastActionOffsetNegative) {
          val = -val;
        }
        try {
          lastActionPointerBackup.addOffset(val);
          lastActionPointer.makeAlias(lastActionPointerBackup);
        } catch (InvalidPointerException e) {
          e.printStackTrace();
        }
      }
    }
    
  }
  
  private Integer getVariableContent(IASTNode variable,
                                 ExplicitAnalysisElement explicitElement,
                                 CFAEdge cfaEdge) {
    
    String varName = lastActionASTNode.getRawSignature();
    if (!explicitElement.contains(varName)) {
      varName = cfaEdge.getPredecessor().getFunctionName() + "::" + varName;
    }
    
    if (explicitElement.contains(varName)) {
      return explicitElement.getValueFor(varName);
    } else {
      return null;
    }
  }
}
