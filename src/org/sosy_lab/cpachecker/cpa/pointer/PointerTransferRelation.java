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
package org.sosy_lab.cpachecker.cpa.pointer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.IASTArrayTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCompositeTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTElaboratedTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTPointerTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState;
import org.sosy_lab.cpachecker.cpa.pointer.Memory.InvalidPointerException;
import org.sosy_lab.cpachecker.cpa.pointer.Memory.LocalVariable;
import org.sosy_lab.cpachecker.cpa.pointer.Memory.MemoryAddress;
import org.sosy_lab.cpachecker.cpa.pointer.Memory.MemoryRegion;
import org.sosy_lab.cpachecker.cpa.pointer.Memory.PointerTarget;
import org.sosy_lab.cpachecker.cpa.pointer.Memory.StackArray;
import org.sosy_lab.cpachecker.cpa.pointer.Memory.StackArrayCell;
import org.sosy_lab.cpachecker.cpa.pointer.Memory.Variable;
import org.sosy_lab.cpachecker.cpa.pointer.Pointer.PointerOperation;
import org.sosy_lab.cpachecker.cpa.pointer.PointerState.ElementProperty;
import org.sosy_lab.cpachecker.cpa.types.Type;
import org.sosy_lab.cpachecker.cpa.types.Type.ArrayType;
import org.sosy_lab.cpachecker.cpa.types.Type.FunctionType;
import org.sosy_lab.cpachecker.cpa.types.Type.PointerType;
import org.sosy_lab.cpachecker.cpa.types.Type.TypeClass;
import org.sosy_lab.cpachecker.cpa.types.TypesState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

public class PointerTransferRelation implements TransferRelation {

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
   * UnrecognizedCCodeException: The program has invalid syntax, a type error or
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
   * PointerState.
   */
  private static class MissingInformation {
    private Pointer         typeInformationPointer = null;
    private CFAEdge         typeInformationEdge    = null;
    private String          typeInformationName    = null;

    private Pointer         actionLeftPointer      = null;
    private Pointer         actionRightPointer     = null;
    private boolean         actionDereferenceFirst = false;
    private boolean         actionOffsetNegative   = false;
    private IASTNode        actionASTNode          = null;

    private MemoryAddress   mallocSizeMemory       = null;
    private IASTNode        mallocSizeASTNode      = null;
  }

  private MissingInformation                missing            = null;

  private static boolean                    printWarnings      = false;
  private static Set<Pair<Integer, String>> warnings           = null;
  private static LogManager                 logger             = null;
  private static LinkedList<MemoryRegion>   memoryLeakWarnings = null;

  private FunctionDefinitionNode entryFunctionDefinitionNode = null;
  private boolean entryFunctionProcessed = false;

  public PointerTransferRelation(boolean pPrintWarnings,
      LogManager pLogger) {
    printWarnings = pPrintWarnings;
    warnings = printWarnings ? new HashSet<Pair<Integer, String>>() : null;
    logger = pLogger;
    memoryLeakWarnings = printWarnings ? new LinkedList<MemoryRegion>() : null;
  }

  public static void addWarning(String message, CFAEdge edge, String variable) {
    if (printWarnings) {
      Integer lineNumber = null;
      if (edge != null) {
        lineNumber = edge.getLineNumber();
      }

      Pair<Integer, String> warningIndex = Pair.of(lineNumber, variable);
      if (!warnings.contains(warningIndex)) {
        warnings.add(warningIndex);
        if (lineNumber != null) {
          logger.log(Level.WARNING, "Warning: " + message + " in line "
              + lineNumber + ": " + edge.getDescription());
        } else {
          logger.log(Level.WARNING, "Warning: " + message);
        }
      }
    }
  }

  public static void addMemoryLeakWarning(String message, CFAEdge edge,
      MemoryRegion memoryRegion) {
    if (printWarnings) {
      Integer lineNumber = null;
      if (edge != null) {
        lineNumber = edge.getLineNumber();
      }

      MemoryRegion warningIndex = memoryRegion;
      if (!memoryLeakWarnings.contains(warningIndex)) {
        memoryLeakWarnings.add(warningIndex);
        if (lineNumber != null) {
          logger.log(Level.WARNING, "Warning: " + message + " in line "
              + lineNumber + ": " + edge.getDescription());
        } else {
          logger.log(Level.WARNING, "Warning: " + message);
        }
      }
    }
  }

  private static void addError(String message, CFAEdge edge) {
    if (printWarnings) {
      int lineNumber = edge.getLineNumber();
      logger.log(Level.WARNING, "ERROR: " + message + " in line " + lineNumber
          + ": " + edge.getDescription());
    }
  }

  @Override
  public Collection<PointerState> getAbstractSuccessors(
      AbstractState element, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException {

    PointerState successor = ((PointerState)element).clone();
    if (successor.isTarget()) {
      return Collections.emptySet();
    }

    successor.setCurrentEdge(cfaEdge);
    successor.clearProperties();

    try {
      switch (cfaEdge.getEdgeType()) {

      case DeclarationEdge:
        DeclarationEdge declEdge = (DeclarationEdge)cfaEdge;

        // ignore type definitions, struct prototypes etc.
        if (declEdge.getDeclaration() instanceof IASTVariableDeclaration) {
          IASTVariableDeclaration decl = (IASTVariableDeclaration)declEdge.getDeclaration();
          handleDeclaration(successor, cfaEdge, decl.isGlobal(), decl.getName(), decl.getDeclSpecifier());
        }
        break;

      case StatementEdge:
        handleStatement(successor, ((StatementEdge)cfaEdge).getStatement(),
                                                      (StatementEdge)cfaEdge);
        break;

      case ReturnStatementEdge:
        // this is the return-statement of a function

        // Normally, the resultPointer is there, but if we know through a type
        // information CPA that this function does not return a pointer, it's not.

        IASTExpression expression = ((ReturnStatementEdge)cfaEdge).getExpression();
        if (expression != null) {
          // non-void function
          Pointer resultPointer = successor.lookupPointer(RETURN_VALUE_VARIABLE);
          if (resultPointer != null) {
            handleAssignment(successor, RETURN_VALUE_VARIABLE, resultPointer,
                false, expression, cfaEdge);
          }
        }
        break;

      case AssumeEdge:
        AssumeEdge assumeEdge = (AssumeEdge)cfaEdge;
        handleAssume(successor, assumeEdge.getExpression(),
                assumeEdge.getTruthAssumption(), assumeEdge);
        break;

      case FunctionCallEdge:
        handleFunctionCall(successor, cfaEdge);
        break;

      case FunctionReturnEdge:
        // now handle the complete a = func(x) statement in the CallToReturnEdge
        FunctionReturnEdge returnEdge = (FunctionReturnEdge)cfaEdge;
        CallToReturnEdge ctrEdge = returnEdge.getSuccessor().getEnteringSummaryEdge();
        handleReturnFromFunction(successor, ctrEdge.getExpression(), ctrEdge);
        break;

      case BlankEdge:
        //the first function start dummy edge is the actual start of the entry function
        if (!entryFunctionProcessed
            && (cfaEdge.getPredecessor() instanceof CFAFunctionDefinitionNode)) {

          //since by this point all global variables have been processed, we can now process the entry function
          //by first creating its context...
          successor.callFunction(entryFunctionDefinitionNode.getFunctionName());

          List<IASTParameterDeclaration> l = entryFunctionDefinitionNode.getFunctionParameters();

          //..then adding all parameters as local variables
          for (IASTParameterDeclaration dec : l) {
            IType declSpecifier = dec.getDeclSpecifier();
            handleDeclaration(successor, cfaEdge, false, dec.getName(), declSpecifier);
          }
          entryFunctionProcessed = true;
        }
        break;

      default:
        throw new UnrecognizedCFAEdgeException(cfaEdge);
      }

    } catch (InvalidPointerException e) {
      addError(e.getMessage(), cfaEdge);
      // removed because the cpas should not declare errors any more
      //successor.setError(true);

      //assert that at least one flag is set
      if (successor.getProperties().isEmpty()) {
        logger.log(Level.WARNING, "InvalidPointerException thrown but no Flag set");
      }
      return Collections.singleton(successor);

    } catch (UnreachableStateException e) {
      return Collections.emptySet();
    }

    Collection<MemoryRegion> lostRegions = successor.checkMemoryLeak();
    if (!lostRegions.isEmpty()) {
      for (MemoryRegion lostRegion : lostRegions) {
        addMemoryLeakWarning("Memory leak: " + lostRegion
            + " is not freed and has no known pointer towards it", cfaEdge,
            lostRegion);
        //element.free(lostRegion);
      }
    }
    return Collections.singleton(successor);
  }

  private void handleDeclaration(PointerState element, CFAEdge edge,
      final boolean global, String name, IType specifier) throws CPATransferException {

    if (name == null) {
      throw new UnrecognizedCCodeException("not expected in CIL", edge);
    }

    if (specifier instanceof IASTCompositeTypeSpecifier
        || specifier instanceof IASTElaboratedTypeSpecifier
        || specifier instanceof IASTEnumerationSpecifier) {

      // structs on stack etc.
      return;
    }

    String varName = name;

    if (specifier instanceof IASTArrayTypeSpecifier) {
      Pointer p = new Pointer(1);
      if (global) {
        element.addNewGlobalPointer(varName, p);
      } else {
        element.addNewLocalPointer(varName, p);
      }

      //long length = parseIntegerLiteral(((IASTArrayDeclarator)declarator).)
      IType nestedSpecifier = ((IASTArrayTypeSpecifier)specifier).getType();
      if (!(nestedSpecifier instanceof IASTSimpleDeclSpecifier)) {
        throw new UnrecognizedCCodeException("unsupported array declaration", edge);
      }

      IASTExpression lengthExpression = ((IASTArrayTypeSpecifier)specifier).getLength();
      if (!(lengthExpression instanceof IASTLiteralExpression)) {
        throw new UnrecognizedCCodeException("variable sized stack arrays are not supported", edge);
      }

      long length = parseIntegerLiteral((IASTLiteralExpression)lengthExpression, edge);
      StackArrayCell array = new StackArrayCell(element.getCurrentFunctionName(),
                                                  new StackArray(varName, length));

      element.pointerOp(new Pointer.Assign(array), p);

      // store the pointer so the type analysis CPA can update its
      // type information
      missing = new MissingInformation();
      missing.typeInformationPointer = p;
      missing.typeInformationEdge = edge;
      missing.typeInformationName = name;

    } else if (specifier instanceof IASTPointerTypeSpecifier) {

      int depth = 0;
      IType nestedSpecifier = specifier;
      do {
        nestedSpecifier = ((IASTPointerTypeSpecifier)nestedSpecifier).getType();
        depth++;
      } while (nestedSpecifier instanceof IASTPointerTypeSpecifier);


      if (nestedSpecifier instanceof IASTElaboratedTypeSpecifier) {
        // declaration of pointer to struct

        Pointer ptr = new Pointer(depth);

        if (global) {
          element.addNewGlobalPointer(varName, ptr);
          element.pointerOp(new Pointer.Assign(Memory.UNINITIALIZED_POINTER),
              ptr);

        } else {
          // edge is instance of LocalDeclarationEdge

          element.addNewLocalPointer(varName, ptr);

          if (entryFunctionProcessed) {
            element.pointerOp(
                new Pointer.Assign(Memory.UNINITIALIZED_POINTER), ptr);
          } else {
            // ptr is a function parameter
            element.pointerOp(
                new Pointer.Assign(Memory.UNKNOWN_POINTER), ptr);
          }
        }

        missing = new MissingInformation();
        missing.typeInformationPointer = ptr;
        missing.typeInformationEdge = edge;
        missing.typeInformationName = name;

      } else {
        Pointer p = new Pointer(depth);
        if (global) {
          element.addNewGlobalPointer(varName, p);
          element.pointerOp(new Pointer.Assign(Memory.UNINITIALIZED_POINTER), p);
        } else {
          element.addNewLocalPointer(varName, p);
          //if the entryFunction has not yet been processed, this means this pointer is a parameter
          //and should be considered unknown rather than uninitialized
          PointerTarget pTarg =
            (!entryFunctionProcessed ? Memory.UNKNOWN_POINTER : Memory.UNINITIALIZED_POINTER);
          element.pointerOp(new Pointer.Assign(pTarg), p);

        }
        // store the pointer so the type analysis CPA can update its
        // type information
        missing = new MissingInformation();
        missing.typeInformationPointer = p;
        missing.typeInformationEdge = edge;
        missing.typeInformationName = name;

        // initializers do not need to be considered, because they have to be
        // constant and constant pointers are considered null
        // local variables do not have initializers in CIL
      }

    } else {
      if (global) {
        element.addNewGlobalPointer(varName, null);
      } else {
        element.addNewLocalPointer(varName, null);
      }
    }
  }

  private void handleAssume(PointerState element,
      IASTExpression expression, boolean isTrueBranch, AssumeEdge assumeEdge)
      throws UnrecognizedCCodeException, UnreachableStateException, InvalidPointerException {

    if (expression instanceof IASTBinaryExpression) {
      IASTBinaryExpression binaryExpression = (IASTBinaryExpression)expression;

      if (binaryExpression.getOperator() == BinaryOperator.EQUALS) {
        handleBinaryAssume(element, binaryExpression, isTrueBranch, assumeEdge);

      } else if (binaryExpression.getOperator() == BinaryOperator.NOT_EQUALS) {
        handleBinaryAssume(element, binaryExpression, !isTrueBranch, assumeEdge);

      } else {
        // assume it's not a pointer comparison
        return;
      }

    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;

      if (unaryExpression.getOperator() == UnaryOperator.NOT) {
        handleAssume(element, unaryExpression.getOperand(), !isTrueBranch,
            assumeEdge);

      } else if (unaryExpression.getOperator() == UnaryOperator.STAR) {
        // if (*var)
        String varName = expression.toASTString();
        Pointer p = element.lookupPointer(varName);

        if (p == null) {
          throw new UnrecognizedCCodeException("Trying to dereference a non-pointer variable",
              assumeEdge, expression);
        }

        boolean isNull = (p.contains(Memory.NULL_POINTER));
        boolean isUninitialized = p.contains(Memory.UNINITIALIZED_POINTER);

        if (isNull && p.getNumberOfTargets() == 1) {
          addError("Trying to dereference a NULL pointer" , assumeEdge);
        }

        if (isUninitialized && p.getNumberOfTargets() == 1) {
          // C actually allows this in special cases
          addWarning("Trying to dereference an uninitialized pointer" , assumeEdge, varName);
        }

        if (isTrueBranch) {
          // *p holds, i.e. *p != 0 holds, i.e. p cannot be NULL
          element.pointerOpAssumeInequality(p, Memory.NULL_POINTER);
        }

      } else {

        throw new UnrecognizedCCodeException("not expected in CIL", assumeEdge,
            expression);
      }
    } else if (expression instanceof IASTIdExpression) {
      // if (a)
      String varName = ((IASTIdExpression)expression).getName();
      Pointer p = element.lookupPointer(varName);
      if (p == null) {
        // no pointer
        return;
      }
      boolean isNull = (p.contains(Memory.NULL_POINTER));

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

    } else if (expression instanceof IASTCastExpression) {

      handleAssume(element, ((IASTCastExpression)expression).getOperand(), isTrueBranch,
          assumeEdge);

    }
  }

  private void handleBinaryAssume(PointerState element,
      IASTBinaryExpression expression, boolean isTrueBranch,
      AssumeEdge assumeEdge) throws UnrecognizedCCodeException,
      UnreachableStateException {

    IASTExpression leftOp = expression.getOperand1();
    IASTExpression rightOp = expression.getOperand2();
    Pointer leftPointer = element.lookupPointer(leftOp.toASTString());
    Pointer rightPointer = element.lookupPointer(rightOp.toASTString());

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

  private void handleFunctionCall(PointerState element,
      CFAEdge cfaEdge) throws UnrecognizedCCodeException {

    FunctionDefinitionNode funcDefNode = (FunctionDefinitionNode)cfaEdge.getSuccessor();
    String funcName = funcDefNode.getFunctionName();

    List<String> formalParameters = funcDefNode.getFunctionParameterNames();
    List<IASTExpression> actualParameters = ((FunctionCallEdge)cfaEdge).getArguments();

    // TODO: relocate parameter handling to strengthen operator

    if (formalParameters != null && formalParameters.size() > 0
        && !actualParameters.isEmpty()) {

      ArrayList<Pointer> actualValues = new ArrayList<Pointer>();

      assert formalParameters.size() == actualParameters.size();

      for (int i = 0; i < actualParameters.size(); i++) {
        IASTExpression parameter = actualParameters.get(i);

        if (parameter instanceof IASTIdExpression) {
          Pointer p = element.lookupPointer(((IASTIdExpression)parameter).getName());
          actualValues.add(p); // either a pointer or null

        } else if (parameter instanceof IASTLiteralExpression) {
          IASTLiteralExpression literal = (IASTLiteralExpression)parameter;

          if (literal instanceof IASTIntegerLiteralExpression
              && parseIntegerLiteral(literal, cfaEdge) == 0) {

            actualValues.add(new Pointer()); // null pointer
          } else {
            actualValues.add(null); // probably not a pointer
          }

        } else if (parameter instanceof IASTUnaryExpression) {
          IASTUnaryExpression unaryExpression = (IASTUnaryExpression)parameter;

          if (unaryExpression.getOperator() == UnaryOperator.AMPER
              && unaryExpression.getOperand() instanceof IASTIdExpression) {

            String varName = unaryExpression.getOperand().toASTString();
            Variable var = element.lookupVariable(varName);
            actualValues.add(new Pointer(var));

          } else {
            throw new UnrecognizedCCodeException("not expected in CIL",
                cfaEdge, unaryExpression);
          }
        } else {
          throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
              parameter);
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

  private long parseIntegerLiteral(IASTLiteralExpression expression, CFAEdge edge)
      throws UnrecognizedCCodeException {

    if (!(expression instanceof IASTIntegerLiteralExpression)) {
      throw new UnrecognizedCCodeException("integer expression expected", edge, expression);
    }
    return ((IASTIntegerLiteralExpression)expression).asLong();
  }

  private void handleReturnFromFunction(PointerState element,
      IASTFunctionCall expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException {

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
            addWarning("Function " + function
                + " returns reference to local variable '" + var.getVarName()
                + "'", cfaEdge, resultTarget.toString());
          }
        }
      }
    }

    element.returnFromFunction(); // throw away local context

    // use function result
    if (expression instanceof IASTFunctionCallAssignmentStatement) {
      // a = func()
      IASTFunctionCallAssignmentStatement assignExpression = (IASTFunctionCallAssignmentStatement)expression;
      IASTExpression leftOperand = assignExpression.getLeftHandSide();

      if (leftOperand instanceof IASTIdExpression) {
        Pointer leftPointer =
            element.lookupPointer(((IASTIdExpression)leftOperand).getName());

        if (leftPointer != null) {
          if (resultPointer != null) {
            // do not use Assign(resultPointer) here, as this would try to make
            // resultPointer an alias of leftPointer
            element.pointerOp(new Pointer.AssignListOfTargets(resultPointer
                .getTargets()), leftPointer);
          } else {

            throw new UnrecognizedCCodeException(
                "assigning non-pointer value to pointer variable", cfaEdge,
                assignExpression);
          }
        } else {
          // function result is not assigned to a pointer, ignore
        }

      } else {
        // *x = func() etc.
        throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
            assignExpression);
      }

    } else if (expression instanceof IASTFunctionCallStatement) {
      // func()
      // ignore
    } else {
      throw new UnrecognizedCCodeException(cfaEdge, expression.asStatement());
    }

    // check for memory leaks
    // TODO better location for calling checkMemoryLeak()? regularly? at end of analysis?
    Collection<MemoryRegion> lostRegions = element.checkMemoryLeak();

    for (MemoryRegion lostRegion : lostRegions) {
      addMemoryLeakWarning("Memory leak: " + lostRegion
          + " is not freed and has no known pointer towards it", cfaEdge,
          lostRegion);
      //element.free(lostRegion);
    }

  }

  private void handleStatement(PointerState element,
      IASTStatement expression, StatementEdge cfaEdge)
      throws UnrecognizedCCodeException, InvalidPointerException {

    if (expression instanceof IASTFunctionCallStatement) {
      // this is a mere function call (func(a))
      IASTFunctionCallExpression funcExpression =
          ((IASTFunctionCallStatement)expression).getFunctionCallExpression();
      String functionName =
          funcExpression.getFunctionNameExpression().toASTString();

      if (functionName.equals("free")) {

        handleFree(element, funcExpression, cfaEdge);

      } else if (functionName.equals("malloc")) {
        // malloc without assignment (will lead to memory leak)
        element.addProperty(ElementProperty.MEMORY_LEAK);
        addWarning(
            "Memory leak because of calling malloc without using the return value!",
            cfaEdge, "");
      }

    } else if (expression instanceof IASTAssignment) {
      // statement is an assignment expression, e.g. a = b or a = a+b;
      handleAssignmentStatement(element, (IASTAssignment)expression, cfaEdge);

    } else {
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
  }

  private void handleFree(PointerState element,
      IASTFunctionCallExpression expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException, InvalidPointerException {

    List<IASTExpression> parameters = expression.getParameterExpressions();
    if (parameters.size() != 1) {
      throw new UnrecognizedCCodeException("Wrong number of arguments for free", cfaEdge, expression);
    }
    IASTExpression parameter = parameters.get(0);

    if (parameter instanceof IASTIdExpression) {
      Pointer p = element.lookupPointer(((IASTIdExpression)parameter).getName());

      if (p == null) {
        throw new UnrecognizedCCodeException("freeing non-pointer pointer",
                                                        cfaEdge, parameter);
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
            success = true; // it may succeed
            freeMem = null; // but we cannot free it

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
          //addWarning("Freeing a NULL-pointer at " + p.getLocation() + " - no harm is done, but maybe check your code, if this pointer can hold non-NULL values at the time it's being freed", cfaEdge, target.toString());

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
        element.addProperty(ElementProperty.INVALID_FREE);
        throw new InvalidPointerException("Free of pointer " + p.getLocation()
            + " = " + p
            + " is impossible to succeed (all targets lead to errors)");
      }

      // free only if there is exactly one target and it is the beginning
      // of a memory region or the pointer has two targets and one of them
      // is the NULL-pointer (because malloc leaves us with at least one NULL-pointer. if the malloc result is unchecked)
      if ((p.getNumberOfTargets() == 1
            || (p.getNumberOfTargets() == 2 && p.contains(Memory.NULL_POINTER)))
          && (freeMem != null)) {

        try {
          element.free(freeMem.getRegion());
        } catch (InvalidPointerException e) {
          // intercept the Exception and add the DOUBLE_FREE flag, then throw again
          element.addProperty(ElementProperty.DOUBLE_FREE);
          throw e;
        }

      }

      // when the program continues after free(p), p can only contain INVALID, NULL or UNKNOWN targets,
      // depending on what it contained before (MemoryAddress, NULL or UNKNOWN respectively)
      //element.pointerOpForAllAliases(new Pointer.AssignListOfTargets(newTargets), p, false);

    } else {
      throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
          parameter);
    }
  }

  private void handleAssignmentStatement(PointerState element,
      IASTAssignment expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException, InvalidPointerException {

    // left hand side
    IASTExpression leftExpression = expression.getLeftHandSide();
    String leftVarName = null;
    Pointer leftPointer;
    boolean leftDereference;

    if (leftExpression instanceof IASTIdExpression) {
      // a
      leftDereference = false;
      leftVarName = ((IASTIdExpression)leftExpression).getName();
      leftPointer = element.lookupPointer(leftVarName);

    } else if (leftExpression instanceof IASTUnaryExpression) {

      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)leftExpression;
      if (unaryExpression.getOperator() == UnaryOperator.STAR) {
        // *a
        leftDereference = true;

        leftExpression = unaryExpression.getOperand();

        boolean leftCast = false;
        if (leftExpression instanceof IASTCastExpression) {
          leftCast = true;
          leftExpression = ((IASTCastExpression)leftExpression).getOperand();
        }

        if (!(leftExpression instanceof IASTIdExpression)) {
          // not a variable at left hand side
          throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
              leftExpression);
        }

        leftPointer = element.lookupPointer(leftExpression.toASTString());
        leftVarName = leftExpression.toASTString();
        if (leftPointer == null) {
          element.addProperty(ElementProperty.UNSAFE_DEREFERENCE);
          if (!leftCast) {
            throw new UnrecognizedCCodeException("dereferencing a non-pointer",
                cfaEdge, leftExpression);
          } else {
            addWarning("Casting non-pointer value "
                + leftExpression.toASTString()
                + " to pointer and dereferencing it", cfaEdge, leftExpression
                .toASTString());
          }

        } else {

          if (!leftPointer.isDereferencable()) {
            element.addProperty(ElementProperty.UNSAFE_DEREFERENCE);
            throw new InvalidPointerException("Unsafe deref of pointer "
                + leftPointer.getLocation() + " = " + leftPointer);
            }

          if (!leftPointer.isSafe()) {
            element.addProperty(ElementProperty.POTENTIALLY_UNSAFE_DEREFERENCE);
            addWarning("Potentially unsafe deref of pointer "
                + leftPointer.getLocation() + " = " + leftPointer, cfaEdge,
                unaryExpression.toASTString());

            // if program continues after deref, pointer did not contain NULL, INVALID or UNINITIALIZED
            element.pointerOpAssumeInequality(leftPointer, Memory.NULL_POINTER);
            element.pointerOpAssumeInequality(leftPointer,
                Memory.INVALID_POINTER);
            element.pointerOpAssumeInequality(leftPointer,
                Memory.UNINITIALIZED_POINTER);
          }

          if (!leftPointer.isPointerToPointer()) {
            // other pointers are not of interest to us
            leftPointer = null;
          }
        }

      } else {
        throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
            unaryExpression);
      }
    } else {
      // TODO fields, arrays
      throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
          leftExpression);
    }

    // right hand side
    IASTRightHandSide op2 = expression.getRightHandSide();

    // handles *a = x and a = x
    handleAssignment(element, leftVarName, leftPointer, leftDereference, op2,
        cfaEdge);
  }

  /**
   * Handles an assignment, where the left-hand side is a pointer.
   * If the right-hand side seems to not evaluate to a pointer, the left pointer
   * is just set to unknown (no warning / error etc. is produced).
   */
  private void handleAssignment(PointerState element,
      String leftVarName, Pointer leftPointer, boolean leftDereference,
      IASTRightHandSide expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException, InvalidPointerException {

    if (expression instanceof IASTStringLiteralExpression) {
      // char* s = "hello world"
      // TODO we have currently no way of storing the information that this pointer
      // points to somewhere in the data region
      element.pointerOp(new Pointer.Assign(Memory.UNKNOWN_POINTER),
          leftPointer, leftDereference);

    } else if (expression instanceof IASTLiteralExpression) {
      // a = 0
      element.pointerOp(new Pointer.Assign(Memory.NULL_POINTER), leftPointer,
          leftDereference);

    } else if (expression instanceof IASTCastExpression) {
      // a = (int*)b
      // ignore cast, we do no type-checking
      handleAssignment(element, leftVarName, leftPointer, leftDereference,
                       ((IASTCastExpression)expression).getOperand(), cfaEdge);

    } else if (expression instanceof IASTFunctionCallExpression) {
      // a = func()

      IASTFunctionCallExpression funcExpression =
          (IASTFunctionCallExpression)expression;
      String functionName =
          funcExpression.getFunctionNameExpression().toASTString();

      if (functionName.equals("malloc")) {
        handleMalloc(element, leftPointer, leftDereference, funcExpression, cfaEdge);

      } else {
        // if it's an internal call, it's handled in handleReturnFromFunction()
        // it it's an external call and we do not know the function, we cannot
        // do more than set the pointer to unknown
        element.pointerOp(new Pointer.Assign(Memory.UNKNOWN_POINTER),
            leftPointer, leftDereference);
      }

    } else if (expression instanceof IASTBinaryExpression) {
      // a = b + c

      IASTBinaryExpression binExpression = (IASTBinaryExpression)expression;
      BinaryOperator typeOfOperator = binExpression.getOperator();
      IASTExpression op1 = binExpression.getOperand1();
      IASTExpression op2 = binExpression.getOperand2();

      if (op1 instanceof IASTCastExpression) {
        op1 = ((IASTCastExpression)op1).getOperand();
      }

      if (op1 instanceof IASTIdExpression) {
        Pointer rightPointer = element.lookupPointer(((IASTIdExpression)op1).getName());

        if (rightPointer == null) {
          if (leftPointer != null) {
            if (element.isPointerVariable(leftPointer.getLocation())) {
              addWarning("Assigning non-pointer value "
                  + binExpression.toASTString() + " to pointer "
                  + leftPointer.getLocation(), cfaEdge, binExpression
                  .toASTString());

              element.pointerOp(new Pointer.Assign(Memory.UNKNOWN_POINTER),
                  leftPointer, leftDereference);

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

          if (!(typeOfOperator == BinaryOperator.PLUS
              || typeOfOperator == BinaryOperator.MINUS)) {
            throw new UnrecognizedCCodeException(cfaEdge, binExpression);
          }

          if (op2 instanceof IASTLiteralExpression) {
            long offset = parseIntegerLiteral((IASTLiteralExpression)op2, cfaEdge);
            if (typeOfOperator == BinaryOperator.MINUS) {
              offset = -offset;
            }

            element.pointerOp(new Pointer.AddOffsetAndAssign(rightPointer,
                offset), leftPointer);

          } else if (op2 instanceof IASTIdExpression) {
            missing = new MissingInformation();
            missing.actionLeftPointer = leftPointer;
            missing.actionRightPointer = rightPointer;
            missing.actionDereferenceFirst = leftDereference;
            missing.actionOffsetNegative =
                (typeOfOperator == BinaryOperator.MINUS);
            missing.actionASTNode = op2;

          } else {
            throw new UnrecognizedCCodeException("not expected in CIL",
                cfaEdge, op2);
          }
        }

      } else if (op1 instanceof IASTLiteralExpression) {

        if (leftPointer == null) {
          return;
        }

        if (op2 instanceof IASTLiteralExpression) {
          addWarning("Assigning non-pointer value "
              + binExpression.toASTString() + " to pointer "
              + leftPointer.getLocation(), cfaEdge, binExpression
              .toASTString());

          element.pointerOp(new Pointer.Assign(Memory.UNKNOWN_POINTER),
              leftPointer, leftDereference);

        } else {
          throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
              op2);
        }

      } else {
        throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
            op1);
      }

    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      UnaryOperator op = unaryExpression.getOperator();

      if (op == UnaryOperator.AMPER) {
        // a = &b
        Variable var =
            element.lookupVariable(unaryExpression.getOperand()
                .toASTString());

        element
            .pointerOp(new Pointer.Assign(var), leftPointer, leftDereference);

      } else if (op == UnaryOperator.MINUS) {
        if (leftPointer != null) {
          addWarning("Assigning non-pointer value "
              + unaryExpression.toASTString() + " to pointer "
              + leftPointer.getLocation(), cfaEdge, unaryExpression
              .toASTString());

          element.pointerOp(new Pointer.Assign(Memory.UNKNOWN_POINTER),
              leftPointer, leftDereference);

        }

      } else if (op == UnaryOperator.STAR) {
        // a = *b

        expression = unaryExpression.getOperand();

        boolean rightCast = false;
        if (expression instanceof IASTCastExpression) {
          rightCast = true;
          expression = ((IASTCastExpression)expression).getOperand();
        }

        if (!(expression instanceof IASTIdExpression)) {
          // not a variable at left hand side
          throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
              expression);
        }

        Pointer rightPointer =
            element.lookupPointer(expression.toASTString());

        if (rightPointer == null) {

          if (!rightCast) {
            throw new UnrecognizedCCodeException("dereferencing a non-pointer",
                cfaEdge, expression);
          } else {
            addWarning("Casting non-pointer value "
                + expression.toASTString()
                + " to pointer and dereferencing it", cfaEdge, expression
                .toASTString());
          }

        } else {

          if (!rightPointer.isDereferencable()) {
            element.addProperty(ElementProperty.UNSAFE_DEREFERENCE);
            throw new InvalidPointerException("Unsafe deref of pointer "
                                              + rightPointer.getLocation()
                                              + " = " + rightPointer);
          }

          if (!rightPointer.isSafe()) {
            element.addProperty(ElementProperty.POTENTIALLY_UNSAFE_DEREFERENCE);
            addWarning("Potentially unsafe deref of pointer "
                + rightPointer.getLocation() + " = " + rightPointer, cfaEdge,
                unaryExpression.toASTString());

            // if program continues after deref, pointer did not contain NULL or INVALID or UNINITIALIZED
            element
                .pointerOpAssumeInequality(rightPointer, Memory.NULL_POINTER);
            element.pointerOpAssumeInequality(rightPointer,
                Memory.INVALID_POINTER);
            element.pointerOpAssumeInequality(rightPointer,
                Memory.UNINITIALIZED_POINTER);
          }

          if (leftPointer != null) {
            if (!rightPointer.isPointerToPointer()) {
              if (element.isPointerVariable(leftPointer.getLocation())) {
                addWarning("Assigning non-pointer value "
                    + unaryExpression.toASTString() + " to pointer "
                    + leftPointer.getLocation(), cfaEdge, expression
                    .toASTString());

                element.pointerOp(new Pointer.Assign(Memory.UNKNOWN_POINTER),
                    leftPointer, leftDereference);

              } else {
                // left hand side is a non-pointer variable which temporarily stored a pointer value
                element.removeTemporaryTracking(leftPointer.getLocation());
              }

            } else {
              element.pointerOp(new Pointer.DerefAndAssign(rightPointer),
                  leftPointer, leftDereference);
            }

          } else {
            // ignore assignment to non-pointer variable
          }
        }

      } else {
        throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
            unaryExpression);
      }

    } else if (expression instanceof IASTIdExpression) {
      // a = b
      Pointer rightPointer =
          element.lookupPointer(((IASTIdExpression)expression).getName());

      if (leftPointer != null) {
        if (rightPointer == null) {
          if (element.isPointerVariable(leftPointer.getLocation())) {

            if (((IASTIdExpression)expression).getName().equals(
                "NULL")) {
              element.pointerOp(new Pointer.Assign(Memory.NULL_POINTER),
                  leftPointer, leftDereference);
            } else {
              element.pointerOp(new Pointer.Assign(Memory.UNKNOWN_POINTER),
                  leftPointer, leftDereference);
              addWarning("Assigning non-pointer value "
                  + expression.toASTString() + " to pointer "
                  + leftPointer.getLocation(), cfaEdge, expression
                  .toASTString());
            }

          } else {
            // left hand side is a non-pointer variable which temporarily stored a pointer value
            element.removeTemporaryTracking(leftPointer.getLocation());
          }

        } else {
          element.pointerOp(new Pointer.Assign(rightPointer), leftPointer,
              leftDereference);
        }
      } else {
        if (rightPointer != null && leftVarName != null) {
          element.addTemporaryTracking(leftVarName, rightPointer);
        }
      }

    } else {
      throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
          expression);
    }

    // we can assume, that after any assignment the pointer is not uninitialized anymore ...
    // it either contains NULL, UNKNOWN or an actual pointer target
    if (leftPointer != null
        && leftPointer.contains(Memory.UNINITIALIZED_POINTER)) {
      element.pointerOpAssumeInequality(leftPointer,
          Memory.UNINITIALIZED_POINTER);

    }
  }

  /**
   * Does a malloc and allocates the result to the given pointer.
   *
   * @param element the abstract element
   * @param pointer the pointer for the result (may be null)
   * @param expression the parameter to the malloc call in the AST
   * @throws InvalidPointerException if malloc fails
   * @throws NumberFormatException if argument is a number, not a valid integer
   * @throws UnrecognizedCCodeException if parameter contains something unexpected
   */
  private void handleMalloc(PointerState element, Pointer pointer,
      boolean leftDereference, IASTFunctionCallExpression expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException {

    List<IASTExpression> parameters = expression.getParameterExpressions();
    if (parameters.size() != 1) {
      throw new UnrecognizedCCodeException("Wrong number of arguments for malloc", cfaEdge, expression);
    }
    IASTExpression parameter = parameters.get(0);

    Pointer.MallocAndAssign op = new Pointer.MallocAndAssign();
    element.pointerOp(op, pointer, leftDereference);
    MemoryAddress memAddress = op.getMallocResult();

    if (parameter instanceof IASTLiteralExpression) {
      long size = parseIntegerLiteral((IASTLiteralExpression)parameter, cfaEdge);
      if (size < 0) {
        throw new UnrecognizedCCodeException("malloc with size < 0, but malloc takes unsigned parameter",
                                              cfaEdge, parameter);
      }
      if (size > 0x7FFFFFFF) {
        addWarning("Possible sign error: malloc with size > 2GB", cfaEdge,
            "malloc");
      }
      memAddress.getRegion().setLength(size);

    } else if (parameter instanceof IASTIdExpression) {
      // store variable name so the strengthen operator can update the length
      // information if he knows it

      missing = new MissingInformation();
      missing.mallocSizeMemory = memAddress;
      missing.mallocSizeASTNode = parameter;

    } else {
      throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
          parameter);
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState element, List<AbstractState> elements, CFAEdge cfaEdge,
      Precision precision) throws CPATransferException {

    if (missing == null) {
      return null;
    }

    if (!(element instanceof PointerState)) {
      return null;
    }

    PointerState pointerState = (PointerState)element;

    for (AbstractState ae : elements) {
      try {
        if (ae instanceof ExplicitState) {
          strengthen(pointerState, (ExplicitState)ae, cfaEdge,
              precision);

        } else if (ae instanceof TypesState) {
          strengthen(pointerState, (TypesState)ae, cfaEdge, precision);
        }

      } catch (UnrecognizedCCodeException e) {
        addError(e.getMessage(), cfaEdge);
        return new ArrayList<AbstractState>();

      } catch (InvalidPointerException e) {
        addError(e.getMessage(), cfaEdge);
        return new ArrayList<AbstractState>();
      }
    }

    if (missing != null && missing.actionLeftPointer != null) {
      // strengthen operator did not get the necessary information
      // necessary do to the backup operation with unknown offset
      // this has to be here so it gets executed if there is no ExplicitCPA

      PointerOperation op;
      if (missing.actionRightPointer != null) {
        op = new Pointer.AddUnknownOffsetAndAssign(missing.actionRightPointer);
      } else {
        op = new Pointer.AddUnknownOffset();
      }
      pointerState.pointerOp(op, missing.actionLeftPointer,
          missing.actionDereferenceFirst);
    }

    missing = null;
    return null;
  }

  /**
   * strengthen called for ExplicitCPA
   */
  private void strengthen(PointerState pointerElement,
      ExplicitState explicitState, CFAEdge cfaEdge,
      Precision precision) throws InvalidPointerException,
      UnrecognizedCCodeException {

    if (missing.mallocSizeMemory != null) {
      Long value =
          getVariableContent(missing.mallocSizeASTNode, explicitState,
              cfaEdge);
      if (value != null) {
        if (value < 0) {
          // TODO better exception or warning
          throw new UnrecognizedCCodeException("malloc with size < 0, "
              + "but malloc takes unsigned parameter", cfaEdge);
        }
        if (value > 0x7FFFFFFF) {
          addWarning("Possible sign error: malloc with size > 2GB", cfaEdge,
              "malloc");
        }
        missing.mallocSizeMemory.getRegion().setLength(value);
      }
    }

    if (missing.actionLeftPointer != null) {
      Long value =
          getVariableContent(missing.actionASTNode, explicitState, cfaEdge);

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
        pointerElement.pointerOp(op, missing.actionLeftPointer,
            missing.actionDereferenceFirst);

        // mark operation as completed successfully
        missing.actionLeftPointer = null;

      } else {
        // getting variable content failed
        // backup action (adding unknown offset) will be done by caller as if
        // there was no ExplicitCPA
      }
    }

  }

  private Long getVariableContent(IASTNode variable,
      ExplicitState explicitState, CFAEdge cfaEdge) {

    String varName = variable.toASTString();
    if (!explicitState.contains(varName)) {
      varName = cfaEdge.getPredecessor().getFunctionName() + "::" + varName;
    }

    if (explicitState.contains(varName)) {
      return explicitState.getValueFor(varName);
    } else {
      return null;
    }
  }

  /**
   * strengthen called for TypesCPA
   */
  private void strengthen(PointerState pointerElement,
      TypesState typesState, CFAEdge cfaEdge, Precision precision)
      throws UnrecognizedCCodeException {

    if (cfaEdge instanceof FunctionCallEdge) {
      // function call, adjust sizeOfTarget of parameters

      FunctionDefinitionNode funcDefNode =
          (FunctionDefinitionNode)cfaEdge.getSuccessor();
      String funcName = funcDefNode.getFunctionName();

      FunctionType function = typesState.getFunction(funcName);
      for (String paramName : function.getParameters()) {
        Pointer pointer = pointerElement.lookupPointer(paramName);
        if (pointer != null) {
          Type type = function.getParameterType(paramName);

          setSizeOfTarget(pointer, type);
        }
      }
      if (function.getReturnType().getTypeClass() != Type.TypeClass.POINTER) {
        pointerElement.removeTemporaryTracking(pointerElement
            .lookupVariable(RETURN_VALUE_VARIABLE));
      }

    } else {

      if (missing.typeInformationPointer == null) {
        return;
      }

      // pointer variable declaration
      String functionName = cfaEdge.getSuccessor().getFunctionName();
      if (missing.typeInformationEdge instanceof DeclarationEdge
          && ((DeclarationEdge) missing.typeInformationEdge).getDeclaration().isGlobal()) {
        functionName = null;
      }

      String varName = missing.typeInformationName;
      Type type = typesState.getVariableType(functionName, varName);

      setSizeOfTarget(missing.typeInformationPointer, type);
    }
  }

  /**
   * TODO call, implementation
   * recursively traverses all fields of a struct
   */
  @SuppressWarnings("unused")
  private void handleStructDeclaration(PointerState element,
                                       TypesState typeElem, Type.CompositeType structType,
                                       String varName,String recursiveVarName) {

    Set<String> members = structType.getMembers();

    for (String member : members) {
      Type t = structType.getMemberType(member);
      //for a field that is itself a struct, repeat the whole process
      if (t != null && t.getTypeClass() == TypeClass.STRUCT) {
        handleStructDeclaration(element, typeElem, (Type.CompositeType)t, member,
            recursiveVarName + "." + member);
      } else {
        //TODO handle pointers
      }
    }
  }

  /**
   * checks all possible locations for type information of a given name
   */
  private Type findType(TypesState typeElem, CFAEdge cfaEdge, String varName) {
    Type t = null;
    //check type definitions
    t = typeElem.getTypedef(varName);
    //if this fails, check functions
    if (t == null) {
      t = typeElem.getFunction(varName);
    }
    //if this also fails, check variables for the global context
    if (t == null) {
      t = typeElem.getVariableType(null, varName);
    }
    try {
      //if again there was no result, check local variables and function parameters
      if (t == null) {
        t = typeElem.getVariableType(cfaEdge.getSuccessor().getFunctionName(), varName);
      }
    } catch (IllegalArgumentException e) {
      //if nothing at all can be found, just return null
    }
    return t;
  }

  /**
   * TODO call
   * checks whether a given expression is a field reference;
   * if yes, find the type of the referenced field, if no, try to determine the type of the variable
   */
  @SuppressWarnings("unused")
  private Type checkForFieldReferenceType(IASTExpression exp, TypesState typeElem,
                                          CFAEdge cfaEdge) {

    String name = exp.toASTString();
    Type t = null;

    if (exp instanceof IASTFieldReference) {
      String[] s = name.split("[.]");
      t = findType(typeElem, cfaEdge, s[0]);
      int i = 1;

      //follow the field reference to its end
      while (t != null && t.getTypeClass() == TypeClass.STRUCT && i < s.length) {
        t = ((Type.CompositeType)t).getMemberType(s[i]);
        i++;
      }

    //if exp is not a field reference, simply try to find the type of the associated variable name
    } else {
      t = findType(typeElem, cfaEdge, name);
    }
    return t;
  }

  /**
   * TODO call, implementation
   * recursively checks the fields of a struct being assigned to another struct of
   * the same type, setting the assignee's fields accordingly
   */
  @SuppressWarnings("unused")
  private void checkFields(PointerState element, CFAEdge cfaEdge, IASTExpression exp,
                           TypesState typeElem, Type.CompositeType structType,
                           String leftName, String rightName,
                           String recursiveLeftName, String recursiveRightName) {

    Set<String> members = structType.getMembers();

    //check all members
    for (String member : members) {
      Type t = structType.getMemberType(member);

      //for a field that is itself a struct, repeat the whole process
      if (t != null && t.getTypeClass() == TypeClass.STRUCT) {
        checkFields(element, cfaEdge, exp, typeElem, (Type.CompositeType)t, member, member,
                         recursiveLeftName + "." + member, recursiveRightName + "." + member);

      //else, check the assigned variable and set the assignee accordingly
      } else {
        //TODO handle copying of pointers
      }
    }
  }

  private void setSizeOfTarget(Pointer pointer, Type type) {

    switch (type.getTypeClass()) {

    case POINTER:
      Type targetType = ((PointerType)type).getTargetType();
      if (targetType.getTypeClass() == TypeClass.STRUCT) {
        pointer.setSizeOfTarget(1);
      } else {
        pointer.setSizeOfTarget(targetType.sizeOf());
      }
      break;

    case ARRAY:
      pointer.setSizeOfTarget(((ArrayType)type).getType().sizeOf());
      break;

    default:
      addWarning("Types determined by TypesCPA und PointerCPA differ!",
          null, pointer.getLocation().toString());
    }
  }

  public void setEntryFunctionDefinitionNode(FunctionDefinitionNode pEntryFunctionDefNode) {
    entryFunctionDefinitionNode = pEntryFunctionDefNode;
  }

}