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
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTArrayDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTArrayModifier;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
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
import cpa.pointeranalysis.Memory.InvalidPointerException;
import cpa.pointeranalysis.Memory.LocalVariable;
import cpa.pointeranalysis.Memory.MemoryAddress;
import cpa.pointeranalysis.Memory.MemoryRegion;
import cpa.pointeranalysis.Memory.PointerTarget;
import cpa.pointeranalysis.Memory.StackArray;
import cpa.pointeranalysis.Memory.StackArrayCell;
import cpa.pointeranalysis.Memory.Variable;
import cpa.pointeranalysis.Pointer.PointerOperation;
import cpa.types.Type;
import cpa.types.TypesElement;
import cpa.types.Type.ArrayType;
import cpa.types.Type.FunctionType;
import cpa.types.Type.PointerType;
import exceptions.CPAException;
import exceptions.CPATransferException;
import exceptions.ErrorReachedException;
import exceptions.TransferRelationException;

/**
 * @author Philipp Wendler
 */
public class PointerAnalysisTransferRelation implements TransferRelation {

  /*
   * Exception usage during analysis:
   * 
   * UnreachableStateException: Thrown when the analysis determines that the
   *      current edge represents an infeasible code path of the program. The
   *      exception will be caught silently and the new abstract element will be
   *      the bottom element of the domain.
   * 
   * InvalidPointerException: The program produces a pointer related error.
   *      If it's a non-critical error like incrementing a pointer above the
   *      length of it's memory region, the exception is caught and a warning is
   *      printed. Analysis will then continue, probably with the affected
   *      pointer set to INVALID.
   *      If it's a critical error like dereferencing the pointer from above,
   *      the exception is caught a the top-most level of the analysis, an
   *      error is printed and the new abstract element will be the bottom
   *      element of the domain.
   *      
   * TransferRelationException: The program has invalid syntax, a type error or
   *      C constructs which should not appear in CIL. An error is printed and
   *      analysis will halt completely.
   *      
   * ? extends RuntimeException: These exceptions should never happen during
   *      analysis as they indicate an illegal state, probably due to missing
   *      checks in the call stack of the throwing method. Program will terminate.
   */
  
  private static class UnreachableStateException extends Exception {

    private static final long serialVersionUID = -3075945291940304272L;

  }
  
  private static final String RETURN_VALUE_VARIABLE = "___cpa_temp_result_var_";

  /**
   * Here some information about the last action is stored;
   * the strengthen operator can use this to find out what information could be
   * updated.
   * 
   * This information is stored in a separate object which can be garbage
   * collected after it was used, this reduces the memory footprint of a
   * PointerAnalysisElement.
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

  // TODO: does this methods need to be thread-safe? 
  public static void addWarning(String message, CFAEdge edge, String variable) {
    if (printWarnings) {
      Integer lineNumber = null;
      if (edge != null) {
        lineNumber = edge.getSuccessor().getLineNumber();
      }
      
      Pair<Integer, String> warningIndex = new Pair<Integer, String>(lineNumber, variable);
      if (!warnings.contains(warningIndex)) {
        warnings.add(warningIndex);
        if (lineNumber != null) {
          CPAMain.logManager.log(Level.WARNING, "Warning: " + message + " in line " + lineNumber+": "
              + edge.getRawStatement());
        } else {
          CPAMain.logManager.log(Level.WARNING, "Warning: " + message);
        }
      }
    }
  }
  
  private static void addError(String message, CFAEdge edge) {
    if (printWarnings) {
      int lineNumber = edge.getSuccessor().getLineNumber();
      CPAMain.logManager.log(Level.WARNING, "ERROR: " + message + " in line " + lineNumber + ": "
          + edge.getRawStatement());
    }
  }
  
  public PointerAnalysisTransferRelation(PointerAnalysisDomain domain) {
    this.domain = domain;
  }
  
  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge,
                                              Precision precision)
                                              throws CPATransferException {
    PointerAnalysisElement successor = ((PointerAnalysisElement)element).clone();
    successor.setCurrentEdge(cfaEdge);

    try {
      switch (cfaEdge.getEdgeType()) {
  
      case DeclarationEdge:
        handleDeclaration(successor, (DeclarationEdge)cfaEdge);
        break;
      
      case StatementEdge:
        handleStatement(successor, ((StatementEdge)cfaEdge).getExpression(), cfaEdge);
        break;
        
      case AssumeEdge:
        AssumeEdge assumeEdge = (AssumeEdge)cfaEdge;
        handleAssume(successor, assumeEdge.getExpression(),
                              assumeEdge.getTruthAssumption(), assumeEdge);
        break;
  
      case FunctionCallEdge:
        handleFunctionCall(successor, cfaEdge);
        break;
        
      case ReturnEdge:
        // now handle the complete a = func(x) statement in the CallToReturnEdge
        ReturnEdge returnEdge = (ReturnEdge)cfaEdge;
        CallToReturnEdge ctrEdge = returnEdge.getSuccessor().getEnteringSummaryEdge();
        handleReturnFromFunction(successor, ctrEdge.getExpression(), ctrEdge);
        break;
        
      case BlankEdge:
        break;
        
      case CallToReturnEdge:
      case MultiStatementEdge:
      case MultiDeclarationEdge:
        assert false;
        break;
      
      default:
        throw new ErrorReachedException("Unknown edge type: " + cfaEdge.getEdgeType());
      }
    
    } catch (TransferRelationException e) {
      addError(e.getMessage(), cfaEdge);
      throw new ErrorReachedException(e.getMessage());
    
    } catch (InvalidPointerException e) {
      addError(e.getMessage(), cfaEdge);
      return domain.getBottomElement();
      
    } catch (UnreachableStateException e) {
      return domain.getBottomElement();
    }
    return successor;
  }

  private void handleDeclaration(PointerAnalysisElement element,
                                 DeclarationEdge declaration)
                                 throws TransferRelationException {
    if (declaration.getDeclSpecifier().getStorageClass() == IASTDeclSpecifier.sc_typedef) {
      // ignore, this is a type definition, not a variable declaration
      return;
    }
    IASTDeclSpecifier specifier = declaration.getDeclSpecifier();
    if (   specifier instanceof IASTCompositeTypeSpecifier
        || specifier instanceof IASTElaboratedTypeSpecifier
        || specifier instanceof IASTEnumerationSpecifier) {
      // TODO handle fields & enums
      return;
    }
    
    IASTDeclarator[] declarators = declaration.getDeclarators();
    if (declarators == null || declarators.length != 1) {
      throw new TransferRelationException("Not expected in CIL: " + declaration.getRawStatement());  
    }
  
    if (declarators[0] instanceof IASTFunctionDeclarator) {
      return;
    }
    
    String varName = declarators[0].getName().toString();

    if (declarators[0] instanceof IASTArrayDeclarator) {
      Pointer p = new Pointer(1);
      if (declaration instanceof GlobalDeclarationEdge) {
        element.addNewGlobalPointer(varName, p);
      } else {
        element.addNewLocalPointer(varName, p);
      }
            
      //long length = parseIntegerLiteral(((IASTArrayDeclarator)declarators[0]).)
      IASTArrayModifier[] modifiers = ((IASTArrayDeclarator)(declarators[0])).getArrayModifiers();
      if (modifiers.length != 1 || modifiers[0] == null) {
        throw new TransferRelationException("Unsupported array declaration " + declaration.getRawStatement());
      }
      
      IASTExpression lengthExpression = modifiers[0].getConstantExpression();
      if (!(lengthExpression instanceof IASTLiteralExpression)) {
        throw new TransferRelationException("Variable sized stack arrays are not supported: " + declaration.getRawStatement());
      }
      
      long length = parseIntegerLiteral((IASTLiteralExpression)lengthExpression);
      StackArrayCell array = new StackArrayCell(element.getCurrentFunctionName(), new StackArray(varName, length));
            
      element.pointerOp(new Pointer.Assign(array), p);
      
      // store the pointer so the type analysis CPA can update its
      // type information
      missing = new MissingInformation();
      missing.typeInformationPointer = p;
      missing.typeInformationEdge    = declaration;
      
    } else {
      
      IASTPointerOperator[] operators = declarators[0].getPointerOperators();
      if (operators != null && operators.length > 0) {
      
        Pointer p = new Pointer(operators.length);
        
        if (declaration instanceof GlobalDeclarationEdge) {
          element.addNewGlobalPointer(varName, p);
        } else {
          element.addNewLocalPointer(varName, p);
          element.pointerOp(new Pointer.Assign(Memory.INVALID_POINTER), p);
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
        } else {
          element.addNewLocalPointer(varName, null);
        }
      }
    }
  }
  
  private void handleAssume(PointerAnalysisElement element,
                            IASTExpression expression,
                            boolean isTrueBranch,
                            AssumeEdge assumeEdge)
                            throws TransferRelationException, UnreachableStateException {
    
    if (expression instanceof IASTBinaryExpression) {
      IASTBinaryExpression binaryExpression = (IASTBinaryExpression)expression; 
      
      if (binaryExpression.getOperator() == IASTBinaryExpression.op_equals) {
        handleBinaryAssume(element, binaryExpression, isTrueBranch, assumeEdge);
        
      } else if (binaryExpression.getOperator() == IASTBinaryExpression.op_notequals) {
        handleBinaryAssume(element, binaryExpression, !isTrueBranch, assumeEdge);
      
      } else {
        // assume it's not a pointer comparison
        return;
      }
        
    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      
      if (unaryExpression.getOperator() == IASTUnaryExpression.op_not) {
        handleAssume(element, unaryExpression.getOperand(), !isTrueBranch, assumeEdge);
      
      } else {
        throw new TransferRelationException("Not expected in CIL: " + assumeEdge.getRawStatement());
      }
      
    } else if (expression instanceof IASTIdExpression) {
      // if (a)
      String varName = expression.getRawSignature();
      Pointer p = element.lookupPointer(varName);
      if (p == null) {
        // no pointer
        return;
      }
      boolean isNull = p.contains(Memory.NULL_POINTER);
      
      if (isTrueBranch && isNull && p.getNumberOfTargets() == 1) {
        // p is always null here -> this branch is never reached
        throw new UnreachableStateException();
      }
      if (!isTrueBranch && !isNull) {
        // p is never null here -> this branch is never reached
        throw new UnreachableStateException();
      }
      
      if (isTrueBranch) {
        // p holds, i.e. p != 0 holds, i.e. p cannot point to null
        element.pointerOpAssumeInequality(p, Memory.NULL_POINTER);
        
      } else {
        // !p holds, i.e. p == 0 holds, i.e. p points to null
        element.pointerOpAssumeEquality(p, Memory.NULL_POINTER);
      }
    }
  }
  
  private void handleBinaryAssume(PointerAnalysisElement element,
                                  IASTBinaryExpression expression,
                                  boolean isTrueBranch,
                                  AssumeEdge assumeEdge)
                                  throws TransferRelationException, UnreachableStateException {
   
    IASTExpression leftOp = expression.getOperand1();
    IASTExpression rightOp = expression.getOperand2();
    Pointer leftPointer = element.lookupPointer(leftOp.getRawSignature());
    Pointer rightPointer = element.lookupPointer(rightOp.getRawSignature());
    
    if (leftPointer != null && rightPointer != null) {
      
      if (element.areAliases(leftPointer, rightPointer)) {
        // surely equal
        
        if (!isTrueBranch) {
          // op1 != op2 is never true
          throw new UnreachableStateException();
        }
      
      } else if (leftPointer.isDifferentFrom(rightPointer)) {
        // never equal
        
        if (isTrueBranch) {
          // op1 == op2 is never true
          throw new UnreachableStateException();
        }
        
      } else {
        
        if (isTrueBranch) {
          element.pointerOpAssumeEquality(leftPointer, rightPointer);
        } else {
          element.pointerOpAssumeInequality(leftPointer, rightPointer);
        }
       
      }
    }
  }
  
  private void handleFunctionCall(PointerAnalysisElement element,
                                  CFAEdge cfaEdge) throws TransferRelationException {
    
    FunctionDefinitionNode funcDefNode = (FunctionDefinitionNode)cfaEdge.getSuccessor();
    String funcName = funcDefNode.getFunctionName();
    
    List<String> formalParameters = funcDefNode.getFunctionParameterNames();
    IASTExpression[] actualParameters = ((FunctionCallEdge)cfaEdge).getArguments();
    
    // TODO: relocate parameter handling to strengthen operator
    
    if (formalParameters != null && formalParameters.size() > 0
        && actualParameters != null && actualParameters.length > 0) {
    
      ArrayList<Pointer> actualValues = new ArrayList<Pointer>();
      
      assert formalParameters.size() == actualParameters.length;
                     
      for (int i = 0; i < actualParameters.length; i++) {
        IASTExpression parameter = actualParameters[i];
        
        if (parameter instanceof IASTIdExpression) {
          Pointer p = element.lookupPointer(parameter.getRawSignature());
          actualValues.add(p); // either a pointer or null
        
        } else if (parameter instanceof IASTLiteralExpression) {
          IASTLiteralExpression literal = (IASTLiteralExpression)parameter;
          
          if ((literal.getKind() == IASTLiteralExpression.lk_integer_constant)
             && parseIntegerLiteral(literal) == 0) {
           
             actualValues.add(new Pointer()); // null pointer               
           } else {
             actualValues.add(null); // probably not a pointer
           }
          
        } else if (parameter instanceof IASTUnaryExpression) {
          IASTUnaryExpression unaryExpression = (IASTUnaryExpression)parameter;
          
          if (unaryExpression.getOperator() == IASTUnaryExpression.op_amper
              && unaryExpression.getOperand() instanceof IASTIdExpression) {
            
            String varName = unaryExpression.getOperand().getRawSignature();
            Variable var = element.lookupVariable(varName);
            actualValues.add(new Pointer(var));
          
          } else {
            throw new TransferRelationException("Not expected in CIL: " + cfaEdge.getRawStatement());
          }
        } else {
          throw new TransferRelationException("Not expected in CIL: " + cfaEdge.getRawStatement());
        }
      }
      
      element.callFunction(funcName);
      
      for (int i = 0; i < actualValues.size(); i++) {
        Pointer value = actualValues.get(i);
        if (value != null) {
          Pointer parameter = new Pointer();
          element.addNewLocalPointer(formalParameters.get(i), parameter); // sets location
          element.pointerOp(new Pointer.Assign(value), parameter);
        }
      }

    } else {
      element.callFunction(funcName);
    }
    
    element.addNewLocalPointer(RETURN_VALUE_VARIABLE, null);
    element.addTemporaryTracking(RETURN_VALUE_VARIABLE, new Pointer());

    // always have MissingInformation because we do not know if the function
    // returns a pointer (and the sizeOfTargets of the parameters are not known
    // if there are any)
    missing = new MissingInformation();
  }
  
  private long parseIntegerLiteral(IASTLiteralExpression expression) throws TransferRelationException {
    try {
      String s = expression.getRawSignature();
      if (s.endsWith("U")) {
        s = s.substring(0, s.length()-1);
      }
      return Long.parseLong(s);
    } catch (NumberFormatException e) {
      throw new TransferRelationException("Error parsing " + expression + " as integer constant");
    }
  }

  private void handleReturnFromFunction(PointerAnalysisElement element,
                                        IASTExpression expression,
                                        CFAEdge cfaEdge)
                                        throws TransferRelationException {
    
    Pointer resultPointer = element.lookupPointer(RETURN_VALUE_VARIABLE);
    // resultPointer does not reliably indicate if the function returns a pointer!
    // E.g., without a type information CPA, return 0 will lead to a pointer result
    // even if the function returns an int
    
    // check for references to stack variables in result pointer
    if (resultPointer != null) {
      for (PointerTarget resultTarget : resultPointer.getTargets()) {
        if (resultTarget instanceof LocalVariable) {
          LocalVariable var = (LocalVariable)resultTarget;
          String function = element.getCurrentFunctionName();
          if (function.equals(var.getFunctionName())) {
            // function returns a reference to a local variable
            addWarning("Function " + function + " returns reference to local variable '"
                       + var.getVarName() + "'", cfaEdge, resultTarget.toString());
          }
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
        Pointer leftPointer = element.lookupPointer(leftOperand.getRawSignature());
      
        if (leftPointer != null) {
          if (binExpression.getOperator() == IASTBinaryExpression.op_assign) {
            if (resultPointer != null) {
              // do not use Assign(resultPointer) here, as this would try to make
              // resultPointer an alias of leftPointer
              element.pointerOp(new Pointer.AssignListOfTargets(resultPointer.getTargets()), leftPointer);
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
      CPAMain.logManager.logException(Level.WARNING, e, "");
    }
        
  }
  
  private void handleStatement(PointerAnalysisElement element,
                               IASTExpression expression, CFAEdge cfaEdge)
                               throws TransferRelationException, InvalidPointerException {
    
    if (cfaEdge.isJumpEdge()) {
      // this is the return-statement of a function
      
      // Normally, the resultPointer is there, but if we know through a type
      // information CPA that this function does not return a pointer, it's not.
      
      if (expression != null) {
        // non-void function
        Pointer resultPointer = element.lookupPointer(RETURN_VALUE_VARIABLE);
        if (resultPointer != null) {
          handleAssignment(element, RETURN_VALUE_VARIABLE, resultPointer, false, expression, cfaEdge);
        }
      }
      
    } else if (expression instanceof IASTUnaryExpression) {
      // this is an unary operation (a++)
      
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      if (unaryExpression.getOperand() instanceof IASTIdExpression) {
        Pointer p = element.lookupPointer(unaryExpression.getOperand().getRawSignature());
        
        if (p != null) {
          int typeOfOperator = unaryExpression.getOperator();
          if (   typeOfOperator == IASTUnaryExpression.op_postFixIncr
              || typeOfOperator == IASTUnaryExpression.op_prefixIncr) {
            
            element.pointerOp(new Pointer.AddOffset(1), p);
          
          } else if (
                 typeOfOperator == IASTUnaryExpression.op_postFixIncr
              || typeOfOperator == IASTUnaryExpression.op_prefixIncr) {
            
            element.pointerOp(new Pointer.AddOffset(-1), p);
          
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
        
        handleFree(element, funcExpression, cfaEdge);
              
      } else if (functionName.equals("malloc")) {
        // malloc without assignment (will lead to memory leak)
        addWarning("Memory leak because of calling malloc without using the return value!",
                   cfaEdge, "");
      }      
    
    } else if (expression instanceof IASTBinaryExpression) {
      // statement is a binary expression, e.g. a = b or a += b;
      handleBinaryStatement(element, (IASTBinaryExpression)expression, cfaEdge);
      
    } else {
      throw new TransferRelationException("Invalid C code: " + cfaEdge.getRawStatement());
    }
  }

  private void handleFree(PointerAnalysisElement element,
                          IASTFunctionCallExpression expression, CFAEdge cfaEdge)
                          throws TransferRelationException, InvalidPointerException {
    
    IASTExpression parameter = expression.getParameterExpression();
    if (parameter instanceof IASTIdExpression) {
      Pointer p = element.lookupPointer(parameter.getRawSignature());
    
      if (p == null) {
        throw new TransferRelationException("Freeing non-pointer pointer "
            + parameter.getRawSignature());
      }
      
      List<PointerTarget> newTargets = new ArrayList<PointerTarget>();
      boolean success = false;
      MemoryAddress freeMem = null;
      
      for (PointerTarget target : p.getTargets()) {
        
        if (target instanceof MemoryAddress) {
          freeMem = (MemoryAddress)target;
          if (!freeMem.hasOffset()) {
            addWarning("Possibly freeing pointer " + p.getLocation() + " to "
                + freeMem + " with unknown offset", cfaEdge, freeMem.toString());
            
            newTargets.add(Memory.INVALID_POINTER);
            success = true;     // it may succeed
            freeMem = null;     // but we cannot free it
            
          } else if (freeMem.getOffset() != 0) {
            addWarning("Possibly freeing pointer " + p.getLocation() + " to "
                + freeMem + " with offset != 0", cfaEdge, freeMem.toString());
          
          } else {
            newTargets.add(Memory.INVALID_POINTER);
            success = true;
          }
        
        } else if (target.isNull()) {
          // free(null) is allowed and does nothing!
          success = true;
          newTargets.add(Memory.NULL_POINTER);
        
        } else if (target == Memory.UNKNOWN_POINTER) {
          success = true;
          newTargets.add(Memory.UNKNOWN_POINTER);
          
        } else {
          addWarning("Possibly freeing pointer " + p.getLocation() + " to "
              + target, cfaEdge, target.toString());
        }
      }
      
      if (!success) {
        // all targets fail
        // elevate the above warnings to an error
        throw new InvalidPointerException("Free of pointer " + p.getLocation()
            + " = " + p + " is impossible to succeed (all targets lead to errors)");
      }
      
      if ((p.getNumberOfTargets() == 1) && (freeMem != null)) {
        // free only if there is exactly one target and it is the beginning
        // of a memory region
        element.free(freeMem.getRegion());
      } 
      
      // when the program continues after free(p), p can only contain INVALID, NULL or UNKNOWN targets,
      // depending on what it contained before (MemoryAddress, NULL or UNKNOWN respectively)
      element.pointerOpForAllAliases(new Pointer.AssignListOfTargets(newTargets), p, false);
      
    } else {
      throw new TransferRelationException("This code is not expected in CIL: "
          + cfaEdge.getRawStatement());
    }
  }

  private void handleBinaryStatement(PointerAnalysisElement element,
                                     IASTBinaryExpression expression,
                                     CFAEdge cfaEdge)
                                     throws TransferRelationException, InvalidPointerException {
    
    // left hand side    
    IASTExpression leftExpression = expression.getOperand1();
    String leftVarName = null;
    Pointer leftPointer;
    boolean leftDereference;
    
    if (leftExpression instanceof IASTIdExpression) {
      // a
      leftDereference = false;
      leftVarName = leftExpression.getRawSignature();
      leftPointer = element.lookupPointer(leftVarName);
      
    } else if (leftExpression instanceof IASTUnaryExpression) {
      
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)leftExpression;
      if (unaryExpression.getOperator() == IASTUnaryExpression.op_star) {
        // *a
        leftDereference = true;
        
        leftExpression = unaryExpression.getOperand();
        if (leftExpression instanceof IASTUnaryExpression
            && ((IASTUnaryExpression)leftExpression).getOperator() == IASTUnaryExpression.op_bracketedPrimary) {
          
          leftExpression = ((IASTUnaryExpression)leftExpression).getOperand();
        }
        
        boolean leftCast = false;
        if (leftExpression instanceof IASTCastExpression) {
          leftCast = true;
          leftExpression = ((IASTCastExpression)leftExpression).getOperand();
        }
        
        if (!(leftExpression instanceof IASTIdExpression)) {
          // not a variable at left hand side
          throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
        }
        
        leftPointer = element.lookupPointer(leftExpression.getRawSignature());
        if (leftPointer == null) {
          
          if (!leftCast) {
            throw new TransferRelationException("Dereferencing a non-pointer: " + cfaEdge.getRawStatement());
          } else {
            addWarning("Casting non-pointer value " + leftExpression.getRawSignature() + " to pointer and dereferencing it", cfaEdge, leftExpression.getRawSignature());
          }
        
        } else {
        
          if (!leftPointer.isDereferencable()) {
            throw new InvalidPointerException("Unsafe deref of pointer "
                      + leftPointer.getLocation() + " = " + leftPointer);
          }
          
          if (!leftPointer.isSafe()) {
            addWarning("Potentially unsafe deref of pointer " + leftPointer.getLocation()
                       + " = " + leftPointer, cfaEdge, unaryExpression.getRawSignature());

            // if program continues after deref, pointer did not contain NULL or INVALID
            element.pointerOpAssumeInequality(leftPointer, Memory.NULL_POINTER);
            element.pointerOpAssumeInequality(leftPointer, Memory.INVALID_POINTER);
          }
          
          if (!leftPointer.isPointerToPointer()) {
            // other pointers are not of interest to us
            leftPointer = null;
          }
        }
        
      } else {
        throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
      }
    } else {
      // TODO fields, arrays
      throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
    }
    
    
    // right hand side
    int typeOfOperator = expression.getOperator();
    IASTExpression op2 = expression.getOperand2();
    
    if (typeOfOperator == IASTBinaryExpression.op_assign) {
      // handles *a = x and a = x
      handleAssignment(element, leftVarName, leftPointer, leftDereference, op2, cfaEdge);
    
    } else if (
           typeOfOperator == IASTBinaryExpression.op_minusAssign
        || typeOfOperator == IASTBinaryExpression.op_plusAssign
        ) {
      // a += x 
      
      if (op2 instanceof IASTLiteralExpression) {
        // a += 5
        
        if (leftPointer != null) {
          long offset = parseIntegerLiteral((IASTLiteralExpression)op2);
          if (typeOfOperator == IASTBinaryExpression.op_minusAssign) {
            offset = -offset;
          }
          element.pointerOp(new Pointer.AddOffset(offset), leftPointer, leftDereference);
        }
      
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
    } else {
      throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
    }
  }

  /**
   * Handles an assignment, where the left-hand side is a pointer.
   * If the right-hand side seems to not evaluate to a pointer, the left pointer
   * is just set to unknown (no warning / error etc. is produced). 
   */
  private void handleAssignment(PointerAnalysisElement element, String leftVarName,
                                Pointer leftPointer, boolean leftDereference,
                                IASTExpression expression, CFAEdge cfaEdge)
                                throws TransferRelationException, InvalidPointerException {
    
    if (expression instanceof IASTLiteralExpression) {
      // a = 0
      element.pointerOp(new Pointer.Assign(Memory.NULL_POINTER), leftPointer, leftDereference);

    } else if (expression instanceof IASTCastExpression) {
      // a = (int*)b
      // ignore cast, we do no type-checking
      handleAssignment(element, leftVarName, leftPointer, leftDereference, ((IASTCastExpression)expression).getOperand(), cfaEdge);
      
    } else if (expression instanceof IASTFunctionCallExpression) {
      // a = func()
      
      IASTFunctionCallExpression funcExpression = (IASTFunctionCallExpression)expression;
      String functionName = funcExpression.getFunctionNameExpression().getRawSignature();
      
      if (functionName.equals("malloc")) {
        handleMalloc(element, leftPointer, leftDereference,
            funcExpression.getParameterExpression(), cfaEdge);
        
      } else {
        // if it's an internal call, it's handled in handleReturnFromFunction()
        // it it's an external call and we do not know the function, we cannot
        // do more than set the pointer to unknown
        element.pointerOp(new Pointer.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);
      }
    
    } else if (expression instanceof IASTBinaryExpression) {
      // a = b + c
      
      IASTBinaryExpression binExpression = (IASTBinaryExpression)expression;
      int typeOfOperator = binExpression.getOperator();
      IASTExpression op1 = binExpression.getOperand1();
      IASTExpression op2 = binExpression.getOperand2();

      if (op1 instanceof IASTCastExpression) {
        op1 = ((IASTCastExpression)op1).getOperand();
      }
      if (op1 instanceof IASTUnaryExpression
          && ((IASTUnaryExpression)op1).getOperator() == IASTUnaryExpression.op_bracketedPrimary) {
        op1 = ((IASTUnaryExpression)op1).getOperand();
      }
      
      if (op1 instanceof IASTIdExpression) {
        Pointer rightPointer = element.lookupPointer(op1.getRawSignature());
        
        if (rightPointer == null) {
          if (leftPointer != null) {
            if (element.isPointerVariable(leftPointer.getLocation())) {
              addWarning("Assigning non-pointer value " + binExpression.getRawSignature()
                  + " to pointer " + leftPointer.getLocation(), cfaEdge, binExpression.getRawSignature());

              element.pointerOp(new Pointer.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);
              
            } else {
              // left hand side is a non-pointer variable which temporarily stored a pointer value
              element.removeTemporaryTracking(leftPointer.getLocation());
            }
          }
            
        } else {
          if (leftPointer == null) {
            // start tracking left hand side
            // assigning rightPointer is wrong, but at least it sets the correct
            // target size etc. and it will be overwritten anyway 
            element.addTemporaryTracking(leftVarName, rightPointer);
            leftPointer = element.lookupPointer(leftVarName);
            assert leftPointer != null;
          }
          
          if (!(typeOfOperator == IASTBinaryExpression.op_plus
              || typeOfOperator == IASTBinaryExpression.op_minus)) {

            throw new TransferRelationException("Invalid C code: " + cfaEdge.getRawStatement());
          }

          if (op2 instanceof IASTLiteralExpression) {
            long offset = parseIntegerLiteral((IASTLiteralExpression)op2);
            if (typeOfOperator == IASTBinaryExpression.op_minus) {
              offset = -offset;
            }
            
            element.pointerOp(new Pointer.AddOffsetAndAssign(rightPointer, offset), leftPointer);

          } else if (op2 instanceof IASTIdExpression) {
            missing = new MissingInformation();
            missing.actionLeftPointer  = leftPointer;
            missing.actionRightPointer = rightPointer;
            missing.actionDereferenceFirst = leftDereference;
            missing.actionOffsetNegative = (typeOfOperator == IASTBinaryExpression.op_minus);
            missing.actionASTNode = op2;
            
          } else { 
            throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
          }
        }
        
      } else if (op1 instanceof IASTLiteralExpression) {
        
        if (leftPointer == null) {
          return;
        }
        
        if (op2 instanceof IASTLiteralExpression) {
          addWarning("Assigning non-pointer value " + binExpression.getRawSignature()
              + " to pointer " + leftPointer.getLocation(), cfaEdge, binExpression.getRawSignature());

          element.pointerOp(new Pointer.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);
        
        } else {
          throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
        }
      
      } else {
        throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
      }
      
    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      int op = unaryExpression.getOperator();
      
      if (op == IASTUnaryExpression.op_bracketedPrimary) {
        // a = (x)
        handleAssignment(element, leftVarName, leftPointer, leftDereference, unaryExpression.getOperand(), cfaEdge);      
     
      } else if (op == IASTUnaryExpression.op_amper) {
        // a = &b
        Variable var = element.lookupVariable(unaryExpression.getOperand().getRawSignature());
        
        element.pointerOp(new Pointer.Assign(var), leftPointer, leftDereference);
        
      } else if (op == IASTUnaryExpression.op_minus) {
        if (leftPointer != null) {
          addWarning("Assigning non-pointer value " + unaryExpression.getRawSignature()
              + " to pointer " + leftPointer.getLocation(), cfaEdge, unaryExpression.getRawSignature());

          element.pointerOp(new Pointer.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);
        
        }
        
      } else if (op == IASTUnaryExpression.op_star) {
        // a = *b
        
        expression = unaryExpression.getOperand();
        if (expression instanceof IASTUnaryExpression
            && ((IASTUnaryExpression)expression).getOperator() == IASTUnaryExpression.op_bracketedPrimary) {
          
          expression = ((IASTUnaryExpression)expression).getOperand();
        }
        
        boolean rightCast = false;
        if (expression instanceof IASTCastExpression) {
          rightCast = true;
          expression = ((IASTCastExpression)expression).getOperand();
        }
        
        if (!(expression instanceof IASTIdExpression)) {
          // not a variable at left hand side
          throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
        }
        
        Pointer rightPointer = element.lookupPointer(expression.getRawSignature());
        
        if (rightPointer == null) {
          
          if (!rightCast) {
            throw new TransferRelationException("Dereferencing a non-pointer: " + cfaEdge.getRawStatement());
          } else {
            addWarning("Casting non-pointer value " + expression.getRawSignature() + " to pointer and dereferencing it", cfaEdge, expression.getRawSignature());
          }  
        
        } else {
        
          if (!rightPointer.isDereferencable()) {
            throw new InvalidPointerException("Unsafe deref of pointer "
                        + rightPointer.getLocation() + " = " + rightPointer);
          }
          
          if (!rightPointer.isSafe()) {
            addWarning("Potentially unsafe deref of pointer "  + rightPointer.getLocation()
                       + " = " + rightPointer, cfaEdge, unaryExpression.getRawSignature());

            // if program continues after deref, pointer did not contain NULL or INVALID
            element.pointerOpAssumeInequality(rightPointer, Memory.NULL_POINTER);
            element.pointerOpAssumeInequality(rightPointer, Memory.INVALID_POINTER);
          }
          
          if (leftPointer != null) {
            if (!rightPointer.isPointerToPointer()) {
              if (element.isPointerVariable(leftPointer.getLocation())) {
                addWarning("Assigning non-pointer value " + unaryExpression.getRawSignature()
                    + " to pointer " + leftPointer.getLocation(), cfaEdge, expression.getRawSignature());
         
                element.pointerOp(new Pointer.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);
          
              } else {
                // left hand side is a non-pointer variable which temporarily stored a pointer value
                element.removeTemporaryTracking(leftPointer.getLocation());
              }

            } else {
              element.pointerOp(new Pointer.DerefAndAssign(rightPointer), leftPointer, leftDereference);
            }
            
          } else {
            // ignore assignment to non-pointer variable
          }
        }
        
      } else {
        throw new TransferRelationException("This code is not expected in CIL: " + cfaEdge.getRawStatement());
      }
      
    } else if (expression instanceof IASTIdExpression) {
      // a = b
      Pointer rightPointer = element.lookupPointer(expression.getRawSignature());
      
      if (leftPointer != null) {
        if (rightPointer == null) {
          if (element.isPointerVariable(leftPointer.getLocation())) {
            addWarning("Assigning non-pointer value " + expression.getRawSignature()
                + " to pointer " + leftPointer.getLocation(), cfaEdge, expression.getRawSignature());
     
            element.pointerOp(new Pointer.Assign(Memory.UNKNOWN_POINTER), leftPointer, leftDereference);

          } else {
            // left hand side is a non-pointer variable which temporarily stored a pointer value
            element.removeTemporaryTracking(leftPointer.getLocation());
          }
          
        } else {
          element.pointerOp(new Pointer.Assign(rightPointer), leftPointer, leftDereference);
        }
      } else {
        if (rightPointer != null && leftVarName != null) {
          element.addTemporaryTracking(leftVarName, rightPointer);
          // TODO: assigning pointer variable to non-pointer variable, start tracking this variable 
        }
      }
      
    } else {
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
                            IASTExpression parameter, CFAEdge cfaEdge)
                            throws TransferRelationException {

    Pointer.MallocAndAssign op = new Pointer.MallocAndAssign();
    element.pointerOp(op, pointer, leftDereference);
    MemoryAddress memAddress = op.getMallocResult();
    
    if (parameter instanceof IASTLiteralExpression) {
      long size = parseIntegerLiteral((IASTLiteralExpression)parameter);
      if (size < 0) {
        throw new TransferRelationException("Malloc with size < 0, but malloc takes unsigned parameter");
      }
      if (size > 0x7FFFFFFF) {
        addWarning("Possible sign error: malloc with size > 2GB", cfaEdge, "malloc");
      }
      memAddress.getRegion().setLength(size);
      
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
                         CFAEdge cfaEdge, Precision precision) throws CPATransferException {
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
        addError(e.getMessage(), cfaEdge);
        throw new ErrorReachedException(e.getMessage());
      
      } catch (InvalidPointerException e) {
        addError(e.getMessage(), cfaEdge);
        return domain.getBottomElement();
      }
    }
    
    if (missing != null && missing.actionLeftPointer != null) {
      // strengthen operator did not get the necessary information
      // necessary do to the backup operation with unknown offset
      // this has to be here so it gets executed if there is no ExplicitAnalysis
      
      PointerOperation op;
      if (missing.actionRightPointer != null) {
        op = new Pointer.AddUnknownOffsetAndAssign(missing.actionRightPointer);
      } else {
        op = new Pointer.AddUnknownOffset();
      }
      pointerElement.pointerOp(op, missing.actionLeftPointer, missing.actionDereferenceFirst);
    }

    missing = null;
    return null;
  }
  
  private void strengthen(PointerAnalysisElement pointerElement, 
                          ExplicitAnalysisElement explicitElement,
                          CFAEdge cfaEdge, Precision precision) throws InvalidPointerException, TransferRelationException {
    
    if (missing.mallocSizeMemory != null) {
      Long value = getVariableContent(missing.mallocSizeASTNode, explicitElement, cfaEdge);
      if (value != null) {
        if (value < 0) {
          throw new TransferRelationException("Malloc with size < 0, but malloc takes unsigned parameter");
        }
        if (value > 0x7FFFFFFF) {
          addWarning("Possible sign error: malloc with size > 2GB", cfaEdge, "malloc");
        }
        missing.mallocSizeMemory.getRegion().setLength(value);
      }
    }
    
    if (missing.actionLeftPointer != null) {
      Long value = getVariableContent(missing.actionASTNode, explicitElement, cfaEdge);
      if (value != null) {
        long val = value.longValue();
        if (missing.actionOffsetNegative) {
          val = -val;
        }
      
        PointerOperation op;
        if (missing.actionRightPointer != null) {
          op = new Pointer.AddOffsetAndAssign(missing.actionRightPointer, val);
        
        } else {
          op = new Pointer.AddOffset(val);
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

    if (cfaEdge instanceof FunctionCallEdge) {
      // function call, adjust sizeOfTarget of parameters
      
      FunctionDefinitionNode funcDefNode = (FunctionDefinitionNode)cfaEdge.getSuccessor();
      String funcName = funcDefNode.getFunctionName();
      
      FunctionType function = typesElement.getFunction(funcName);
      for (String paramName : function.getParameters()) {
        Pointer pointer = pointerElement.lookupPointer(paramName);
        if (pointer != null) {
          Type type = function.getParameterType(paramName);
          
          setSizeOfTarget(pointer, type);
        }
      }
      if (function.getReturnType().getTypeClass() != Type.TypeClass.POINTER) {
        pointerElement.removeTemporaryTracking(pointerElement.lookupVariable(RETURN_VALUE_VARIABLE));
      }
      
    } else {
    
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
      
      String varName = declarators[0].getName().getRawSignature();
      Type type = typesElement.getVariableType(functionName, varName);

      setSizeOfTarget(missing.typeInformationPointer, type);
    }
  }
  
  private void setSizeOfTarget(Pointer pointer, Type type) throws TransferRelationException {
    int sizeOfTarget;
    switch (type.getTypeClass()) {
    case POINTER:
      sizeOfTarget = ((PointerType)type).getTargetType().sizeOf();
      break;
      
    case ARRAY:
      sizeOfTarget = ((ArrayType)type).getType().sizeOf();
      break;
      
    default: 
      throw new TransferRelationException("Types determined by TypesCPA und PointerAnalysisCPA differ!");
    }

    pointer.setSizeOfTarget(sizeOfTarget);
  }
}