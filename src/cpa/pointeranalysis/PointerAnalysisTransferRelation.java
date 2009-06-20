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
  
  // here some information about the last action is stored;
  // the strengthen operator can use this to find out what information could be
  // updated
  private IASTNode      lastActionASTNode = null;
  private CFAEdge       lastActionEdge = null;
  private MemoryAddress lastActionMalloc = null;
  private Pointer       lastActionPointer = null;
  private Pointer       lastActionPointerBackup = null;
  private boolean       lastActionOffsetNegative = false;
  private boolean       lastActionDereferenceFirst = false;
  
  private final PointerAnalysisDomain domain;
  
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
      
    case AssumeEdge:
      try {
        successor = handleAssume(successor, (AssumeEdge)cfaEdge);
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
      throw new TransferRelationException("Not exptected in CIL: " + declaration.getRawStatement());  
    }
  
    String varName = declarators[0].getName().toString();

    IASTPointerOperator[] operators = declarators[0].getPointerOperators();
    if (operators != null && operators.length > 0) {
    
      Pointer p = new Pointer(operators.length);
      
      if (declaration instanceof GlobalDeclarationEdge) {
        element.addNewGlobalPointer(varName, p);
      } else {
        element.addNewLocalPointer(varName, p);
        p.assign(Memory.INVALID_POINTER);
      }
      // store the pointer so the type analysis CPA can update its
      // type information
      lastActionPointer = p;
      lastActionEdge = declaration;
      
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
  
  private PointerAnalysisElement handleAssume(PointerAnalysisElement element,
                                              AssumeEdge assumeEdge)
                                              throws TransferRelationException {
    
    IASTExpression expression = assumeEdge.getExpression();
    boolean isTrueBranch = assumeEdge.getTruthAssumption();
    
    return handleAssume(element, expression, isTrueBranch, assumeEdge);
  }
  
  private PointerAnalysisElement handleAssume(PointerAnalysisElement element,
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
        p.removeTarget(Memory.NULL_POINTER);
      } else {
        // !p holds, i.e. p == 0 holds, i.e. p points to null
        p.assign(Memory.NULL_POINTER); // this removes all other targets        
      }
    }
    return element;
  }
  
  private PointerAnalysisElement handleBinaryAssume(PointerAnalysisElement element,
                                                    IASTBinaryExpression expression,
                                                    boolean isTrueBranch,
                                                    AssumeEdge assumeEdge)
                                                    throws TransferRelationException {
    
    IASTExpression leftOp = expression.getOperand1();
    IASTExpression rightOp = expression.getOperand2();
    Pointer leftPointer = element.getPointer(leftOp.getRawSignature());
    Pointer rightPointer = element.getPointer(rightOp.getRawSignature());
    
    if (leftPointer != null && rightPointer != null) {
      
      if (leftPointer.getNumberOfTargets() == 1 && leftPointer.isSubsetOf(rightPointer)) {
        // potentially equal
        
        if (rightPointer.getNumberOfTargets() == 1) {
          // surely equal
          
          if (!isTrueBranch) {
            // op1 != op2 is never true
            return domain.getBottomElement();
          }
        
        } else {
          // leftPointer is strict subset of rightPointer
          
          if (isTrueBranch) {
            // of the several targets of rightPointer, it has to be the one which leftPointer has
            rightPointer.assign(leftPointer);
          } else {
            // remove target of leftPointer from rightPointer (which has other targets)
            rightPointer.removeAllTargets(leftPointer);
          }
          
        }
      } else if (rightPointer.getNumberOfTargets() == 1 && rightPointer.isSubsetOf(leftPointer)) {
        // note that leftPointer.getNumberOfTargets() != 1
        // rightPointer is strict subset of leftPointer

        if (isTrueBranch) {
          leftPointer.assign(rightPointer);
          
        } else {
          leftPointer.removeAllTargets(rightPointer);
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
          leftPointer.assign(Memory.UNKNOWN_POINTER); // invalidate old pointer

          if (binExpression.getOperator() == IASTBinaryExpression.op_assign) {
            if (resultPointer != null) {
              leftPointer.assign(resultPointer);
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
        handleMalloc(element, null, false, parameter);
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
        
        if (!leftPointer.isSafe()) {
          addWarning("Unsafe deref of pointer " + unaryExpression.getRawSignature(), cfaEdge, unaryExpression.getRawSignature());
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
      return;
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
        leftPointer.addOffset(offset, leftDereference, element);
      
      } else if (op2 instanceof IASTIdExpression) {
        // a += b
        Pointer backup = leftPointer.clone();
        leftPointer.addUnknownOffset(leftDereference, element);

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
  private void handleAssignment(PointerAnalysisElement element,
                                Pointer leftPointer, boolean leftDereference,
                                IASTExpression expression, CFAEdge cfaEdge)
                                throws TransferRelationException,
                                       InvalidPointerException {
    
    if (expression instanceof IASTLiteralExpression) {
      // a = 0
      leftPointer.assign(new Pointer(), leftDereference, element);

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
        // (which is already done at the beginning of this method)
        leftPointer.assign(new Pointer(Memory.UNKNOWN_POINTER), leftDereference, element);
      }
    
    } else if (expression instanceof IASTBinaryExpression) {
      // a = b + c
      IASTBinaryExpression binExpression = (IASTBinaryExpression)expression;
      int typeOfOperator = binExpression.getOperator();
      IASTExpression op1 = binExpression.getOperand1();
      IASTExpression op2 = binExpression.getOperand2();

      if (!(op1 instanceof IASTIdExpression)) {
        // in p1 = p2 + x, CIL makes p2 always the left operand of the addition 
        leftPointer.assign(new Pointer(Memory.UNKNOWN_POINTER), leftDereference, element);
        throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement()); 
      }
      Pointer rightPointer = element.getPointer(op1.getRawSignature());
      if (rightPointer != null) {
        
        if (!(typeOfOperator == IASTBinaryExpression.op_plus
            || typeOfOperator == IASTBinaryExpression.op_minus)) {
          leftPointer.assign(new Pointer(Memory.UNKNOWN_POINTER), leftDereference, element);
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
          Pointer rightHandSide = rightPointer.clone();
          rightHandSide.addOffset(offset);
          
          leftPointer.assign(rightHandSide, leftDereference, element);

        } else if (op2 instanceof IASTIdExpression) {
          Pointer rightHandSide = rightPointer.clone();
          rightHandSide.addUnknownOffset();
          
          leftPointer.assign(rightHandSide, leftDereference, element);

          lastActionPointerBackup = rightPointer;
          lastActionPointer = leftPointer;
          lastActionASTNode = op2;
          lastActionOffsetNegative = (typeOfOperator == IASTBinaryExpression.op_minus);
          lastActionDereferenceFirst = leftDereference;
          
        } else { 
          leftPointer.assign(new Pointer(Memory.UNKNOWN_POINTER), leftDereference, element);
          throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
        }
      
      } else {
        // probably assigning a non-pointer value to a pointer
        // set pointer to unknown and ignore otherwise
        leftPointer.assign(new Pointer(Memory.UNKNOWN_POINTER), leftDereference, element);
      }
      
    } else if (expression instanceof IASTUnaryExpression
        && ((IASTUnaryExpression)expression).getOperator() == IASTUnaryExpression.op_bracketedPrimary) {
      // a = (x)
      handleAssignment(element, leftPointer, leftDereference, ((IASTUnaryExpression)expression).getOperand(), cfaEdge);      
      
    } else if (expression instanceof IASTUnaryExpression
         && ((IASTUnaryExpression)expression).getOperator() == IASTUnaryExpression.op_amper) {
      // a = &b

      String varName = ((IASTUnaryExpression)expression).getOperand().getRawSignature();

      Variable var;
      if (element.getGlobalPointers().containsKey(varName)) {
        var = new GlobalVariable(varName);
      } else {
        var = new LocalVariable(varName);
      }
      
      leftPointer.assign(new Pointer(var), leftDereference, element);
       
    } else {
      // this handles a = b and a = *b, throws an Exception if code is unknown
      Pointer rightPointer = extractPointer(element, expression, cfaEdge);
      
      if (rightPointer != null) {
        leftPointer.assign(rightPointer, leftDereference, element);
      } else {
        leftPointer.assign(new Pointer(Memory.UNKNOWN_POINTER), leftDereference, element);
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
   * @throws TransferRelationException if parameter contains something unexpected
   */
  private void handleMalloc(PointerAnalysisElement element, Pointer pointer,
                            boolean leftDereference,
                            IASTExpression parameter)
                            throws InvalidPointerException,
                                   TransferRelationException {
    Pointer malloc;
    
    if (parameter instanceof IASTLiteralExpression) {
      String s = parameter.getRawSignature();
      if (s.endsWith("U")) {
        s = s.substring(0, s.length()-1);
      }
      try {
        malloc = element.malloc(Integer.parseInt(s));
      } catch (NumberFormatException e) {
        throw new TransferRelationException("Invalid C code: malloc(" + parameter.getRawSignature() + ")");
      }
      
    } else if (parameter instanceof IASTIdExpression) {
      // allocate now with unknown length, store variable name so the
      // strengthen operator can update the length information if he knows
      // it
      malloc = element.malloc();
      
      for (PointerTarget target : malloc.getTargets()) {
        if (target instanceof MemoryAddress) {
          lastActionMalloc = (MemoryAddress)target;
          lastActionASTNode = parameter;
          break;
        }
      }
    
    } else {
      throw new TransferRelationException("Not expected in CIL: malloc(" + parameter.getRawSignature() + ")");
    }
    
    if (pointer != null) {
      pointer.assign(malloc, leftDereference, element);
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
      try {
        if (ae instanceof ExplicitAnalysisElement) {
          strengthen(pointerElement, (ExplicitAnalysisElement)ae, cfaEdge, precision);
        
        } else if (ae instanceof TypesElement) {
          strengthen(pointerElement, (TypesElement)ae, cfaEdge, precision);
        }
      } catch (TransferRelationException e) {
        e.printStackTrace();
      }
    }

    lastActionASTNode = null;
    lastActionEdge = null;
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
          Pointer p = lastActionPointerBackup.clone();
          p.addOffset(val);
          lastActionPointer.assign(p, lastActionDereferenceFirst, pointerElement);
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
  
  private void strengthen(PointerAnalysisElement pointerElement, 
                          TypesElement typesElement,
                          CFAEdge cfaEdge, Precision precision)
                          throws TransferRelationException {

    if (lastActionEdge instanceof DeclarationEdge) {
      // pointer variable declaration
      DeclarationEdge declaration = (DeclarationEdge)lastActionEdge;
      String functionName = cfaEdge.getSuccessor().getFunctionName();
      if (declaration instanceof GlobalDeclarationEdge) {
        functionName = null;
      }
      
      IASTDeclarator[] declarators = declaration.getDeclarators();
      if (declarators == null || declarators.length != 1) {
         throw new TransferRelationException("Not expected in CIL: " + declaration.getRawStatement());
      }
      
      String name = declarators[0].getName().getRawSignature();
      Type type = typesElement.getVariableType(functionName, name);
      
      if (!(type instanceof PointerType)) {
        throw new TransferRelationException("Types determined by TypesCPA und PointerAnalysisCPA differ!");
      }

      int sizeOfTarget = ((PointerType)type).getTargetType().sizeOf();
      lastActionPointer.setSizeOfTarget(sizeOfTarget);
    }
  }
}