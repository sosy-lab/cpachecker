/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.seplogic;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.seplogic.SeplogicState.SeplogicQueryUnsuccessful;
import org.sosy_lab.cpachecker.cpa.seplogic.interfaces.Handle;
import org.sosy_lab.cpachecker.cpa.seplogic.interfaces.PartingstarInterface;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

import com.google.common.base.Optional;


public class SeplogicTransferRelation implements TransferRelation {

  boolean entryFunctionProcessed = false;
  long existentialVarIndex = 0;
  private long namespaceCounter = 0;
  private PartingstarInterface psInterface;
  static final String RETVAR = "$RET";
  private final LogManager logger;


  public SeplogicTransferRelation(SeplogicCPA cpa, LogManager pLogger) {
    super();
    psInterface = cpa.getPartingstarInterface();
    logger = pLogger;
  }

  private Handle makeFreshExistential() {
    return psInterface.makeVar(psInterface.loadString("_v" + existentialVarIndex++));
  }

  @Override
  public Collection<SeplogicState> getAbstractSuccessors(
      AbstractState element, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException {

    SeplogicState currentElement = (SeplogicState) element;
    SeplogicState successor;

    try {
      switch (cfaEdge.getEdgeType()) {

      case DeclarationEdge:
        CDeclarationEdge declEdge = (CDeclarationEdge) cfaEdge;
        successor =
            handleDeclaration(currentElement, cfaEdge, declEdge.getDeclaration().getName(), declEdge.getDeclaration().getType());
        break;

      case StatementEdge:
        successor = handleStatement(currentElement, ((CStatementEdge) cfaEdge).getStatement(),
            (CStatementEdge) cfaEdge);
        break;

      case ReturnStatementEdge:
        Optional<CExpression> expression = ((CReturnStatementEdge) cfaEdge).getExpression();
        if (expression.isPresent()) {
          // non-void function
          successor = handleAssignment(currentElement, RETVAR, false, expression.get(), cfaEdge);
        } else {
          successor = currentElement;
        }
        break;

      case AssumeEdge:
        AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
        successor =
            handleAssume(currentElement, (CExpression) assumeEdge.getExpression(), assumeEdge.getTruthAssumption(), assumeEdge);
        break;

      case FunctionCallEdge:
        successor = handleFunctionCall(currentElement, cfaEdge);
        break;

      case FunctionReturnEdge:
        // now handle the complete a = func(x) statement in the CallToReturnEdge
        FunctionReturnEdge returnEdge = (FunctionReturnEdge) cfaEdge;
        FunctionSummaryEdge ctrEdge = returnEdge.getSuccessor().getEnteringSummaryEdge();
        successor = handleReturnFromFunction(currentElement, (CFunctionCall) ctrEdge.getExpression(), ctrEdge);
        break;

      case BlankEdge:
        //the first function start dummy edge is the actual start of the entry function
        // XXX code?
        successor = currentElement;
        break;
      default:
        throw new UnrecognizedCFAEdgeException(cfaEdge);
      }
    } catch (SeplogicQueryUnsuccessful e) {
      if (e.isPureGuard() != null && e.isPureGuard().booleanValue()) {
        logger.logUserException(Level.WARNING, e, "Pure guard failed (-> false) in CFA Edge: N" + cfaEdge.getPredecessor().getNodeNumber()
            + " -> N" + cfaEdge.getSuccessor().getNodeNumber());
        return Collections.emptySet();
        //return Collections.singleton(currentElement.makeExceptionState(e));
      } else {
        logger.logUserException(Level.WARNING, e, "Must be a null-dereference in CFA Edge: N" + cfaEdge.getPredecessor().getNodeNumber() + " -> N"
          + cfaEdge.getSuccessor().getNodeNumber());
        return Collections.singleton(currentElement.makeExceptionState(e));
      }
      //throw e;
    }
    if (successor.isFalse()) {
      logger.log(Level.WARNING, "Successor implies false in CFA Edge: N" + cfaEdge.getPredecessor().getNodeNumber() + " -> N"
          + cfaEdge.getSuccessor().getNodeNumber());
      return Collections.emptySet();
    }

    // XXX a bit too arbitrary
    if (!successor.doBreak() && (cfaEdge.getSuccessor().isLoopStart() || true)) {
      successor = successor.abstract_();
    }
    return Collections.singleton(successor);
  }

  private SeplogicState handleDeclaration(SeplogicState element, CFAEdge edge,
      String name,
      CType specifier) throws CPATransferException {

    if (name == null
        && (specifier instanceof CElaboratedType
        || specifier instanceof CCompositeType)) {
      // ignore struct prototypes
      return element;
    }

    if (name == null) { throw new UnrecognizedCCodeException("not expected in CIL", edge); }

    if (specifier instanceof CFunctionType) { return element; }

    if (specifier instanceof CCompositeType
        || specifier instanceof CElaboratedType
        || specifier instanceof CEnumType) {

      // structs on stack etc.
      return element;
    }

    return element; //XXX wrong
    // throw new UnrecognizedCCodeException("unsupported", edge);

    /* XXX fill
    String varName = name;
    if (specifier instanceof IASTArrayTypeSpecifier) {
      Pointer p = new Pointer(1);
      element.addNewLocalPointer(varName, p);

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

        if (edge instanceof GlobalDeclarationEdge) {
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
        if (edge instanceof GlobalDeclarationEdge) {
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
      if (edge instanceof GlobalDeclarationEdge) {
        element.addNewGlobalPointer(varName, null);
      } else {
        element.addNewLocalPointer(varName, null);
      }
    }
    */
  }

  private SeplogicState handleAssume(SeplogicState element,
      CExpression expression, boolean isTrueBranch, AssumeEdge assumeEdge)
      throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {

    Handle a1, a2;

    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression) expression;

      if (binaryExpression.getOperator() == BinaryOperator.EQUALS) {
        return handleBinaryAssume(element, binaryExpression, isTrueBranch, assumeEdge);

      } else if (binaryExpression.getOperator() == BinaryOperator.NOT_EQUALS) {
        return handleBinaryAssume(element, binaryExpression, !isTrueBranch, assumeEdge);

      } else {
        // assume it's not a pointer comparison
        return element;
      }

    } else if (expression instanceof CUnaryExpression) {
      throw new UnrecognizedCCodeException("not expected in CIL", assumeEdge,
          expression);
    } else if (expression instanceof CIdExpression) {
      // if (a)
      String varName = ((CIdExpression) expression).getName();

      a1 = makeVarArg(varName, element);
      a2 = makeIntegerConstant(0);
    } else if (expression instanceof CCastExpression) {
      return handleAssume(element, ((CCastExpression) expression).getOperand(), isTrueBranch,
          assumeEdge);
    } else if (expression instanceof CPointerExpression) {
      // if (*var)
      String varName = expression.toASTString();

      if (!isTrueBranch) { return element; }
      a1 = makeVarArg(varName, element);
      a2 = makeIntegerConstant(0);
      isTrueBranch = !isTrueBranch;
    } else {
      throw new UnrecognizedCCodeException("unsupported", assumeEdge);
    }

    Handle f;
    if (isTrueBranch) {
      f = psInterface.makeEq(a1, a2);
    } else {
      f = psInterface.makeIneq(a1, a2);
    }
    try {
      return element.performSpecificationAssignment(psInterface.makeEmp(), f, null);
    } catch (SeplogicQueryUnsuccessful e) {
      e.setIsPureGuard(true);
      throw e;
    }
  }

  private String quoteVar(String varName, SeplogicState element) {
    return quoteVar(varName, element.getNamespace());
  }

  private String quoteVar(String varName, String localNamespace) {
    varName = localNamespace + "$" + varName;
    return (varName.charAt(0) == '_') ? ("ZZZ" + varName) : varName;
  }

  private Handle makeVarArg(String varName, SeplogicState element) {
    return makeVarArg(varName, element.getNamespace());
  }

  private Handle makeVarArg(String varName, String localNamespace) {
    return psInterface.makeVar(psInterface.loadString(quoteVar(varName, localNamespace)));
  }

  private SeplogicState handleBinaryAssume(SeplogicState element,
      CBinaryExpression expression, boolean isTrueBranch,
      AssumeEdge assumeEdge) throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {

    CExpression leftOp = expression.getOperand1();
    CExpression rightOp = expression.getOperand2();

    Handle f;
    Handle a1 = convertOperandToArgument(leftOp, element);
    Handle a2 = convertOperandToArgument(rightOp, element);
    if (isTrueBranch) {
      f = psInterface.makeEq(a1, a2);
    } else {
      f = psInterface.makeIneq(a1, a2);
    }

    try {
      return element.performSpecificationAssignment(psInterface.makeEmp(), f, null);
    } catch (SeplogicQueryUnsuccessful e) {
      e.setIsPureGuard(true);
      throw e;
    }
  }

  private Handle convertOperandToArgument(CExpression pLeftOp, SeplogicState element)
      throws UnrecognizedCCodeException {
    if (pLeftOp instanceof CIntegerLiteralExpression) {
      return makeIntegerConstant(((CIntegerLiteralExpression) pLeftOp).getValue().longValue());
    } else if (pLeftOp instanceof CIdExpression) {
      return makeVarArg(pLeftOp.toASTString(), element);
    } else {
      throw new UnrecognizedCCodeException("unsupported expression type", null, pLeftOp);
    }
  }

  private SeplogicState handleFunctionCall(SeplogicState element,
      CFAEdge cfaEdge) throws UnrecognizedCCodeException {

    CFunctionEntryNode funcDefNode = (CFunctionEntryNode) cfaEdge.getSuccessor();
    String funcName = funcDefNode.getFunctionName();
    String nsName = funcName + namespaceCounter++;

    List<String> formalParameters = funcDefNode.getFunctionParameterNames();
    List<CExpression> actualParameters = ((CFunctionCallEdge) cfaEdge).getArguments();

    if (formalParameters != null && formalParameters.size() > 0
        && !actualParameters.isEmpty()) {

      assert formalParameters.size() == actualParameters.size();

      for (int i = 0; i < actualParameters.size(); i++) {
        CExpression parameter = actualParameters.get(i);
        String formalParameter = formalParameters.get(i);

        element = handleAssignment(element, formalParameter, false, parameter, cfaEdge, nsName);

        /*
        if (parameter instanceof IASTIdExpression) {
          String paramName = ((IASTIdExpression)parameter).getName();
        } else if (parameter instanceof IASTLiteralExpression) {
          IASTLiteralExpression literal = (IASTLiteralExpression)parameter;

          if (literal instanceof IASTIntegerLiteralExpression) {
            long value = ((IASTIntegerLiteralExpression) literal).asLong();
            if (value == 0) {

            }

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
        } */
      }
    }

    return element.pushNamespace(nsName);
  }

  private SeplogicState handleReturnFromFunction(SeplogicState element,
      CFunctionCall expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException {

    element = element.popNamespace();

    // use function result
    if (expression instanceof CFunctionCallAssignmentStatement) {
      // a = func()
      CFunctionCallAssignmentStatement assignExpression = (CFunctionCallAssignmentStatement) expression;
      CExpression leftOperand = assignExpression.getLeftHandSide();


      if (leftOperand instanceof CIdExpression) {
        throw new UnrecognizedCCodeException("unsupported", cfaEdge);
        /* XXX fill
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
        */
      } else {
        // *x = func() etc.
        throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
            assignExpression);
      }

    } else if (expression instanceof CFunctionCallStatement) {
      // func()
      // ignore
      return element;
    } else {
      throw new UnrecognizedCCodeException("Unrecognized", cfaEdge);
    }

  }

  private SeplogicState handleStatement(SeplogicState element,
      CStatement expression, CStatementEdge cfaEdge)
      throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {

    if (expression instanceof CFunctionCallStatement) {
      // this is a mere function call (func(a))
      CFunctionCallExpression funcExpression =
          ((CFunctionCallStatement) expression).getFunctionCallExpression();
      String functionName =
          funcExpression.getFunctionNameExpression().toASTString();

      if (functionName.equals("free")) {
        return handleFree(element, funcExpression, cfaEdge);
      } else if (functionName.equals("malloc")) {
        // malloc without assignment (will lead to memory leak)
        return element; // XXX .performSpecificationAssignment(new Empty(), makePointsTo(new OpArgument("nil", new ArrayList<Argument>()), makeFreshExistential()), null);
      } else {
        return element; // XXX correct?
      }

    } else if (expression instanceof CAssignment) {
      // statement is an assignment expression, e.g. a = b or a = a+b;
      return handleAssignmentStatement(element, (CAssignment) expression, cfaEdge);

    } else {
      throw new UnrecognizedCCodeException(expression.toASTString(), cfaEdge);
    }
  }

  private SeplogicState handleFree(SeplogicState element,
      CFunctionCallExpression expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {

    List<CExpression> parameters = expression.getParameterExpressions();
    if (parameters.size() != 1) { throw new UnrecognizedCCodeException("Wrong number of arguments for free", cfaEdge,
        expression); }
    CExpression parameter = parameters.get(0);

    if (parameter instanceof CIdExpression) {
      String ident = ((CIdExpression) parameter).getName();
      return element.performSpecificationAssignment(makePointsTo(makeVarArg(ident, element),
          makeFreshExistential()), psInterface.makeEmp(), null);
    } else {
      throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
          parameter);
    }
  }

  private Handle makePointsTo(Handle pA1, Handle pA2) {
    return psInterface.makeSpatialPredicate(psInterface.loadString("NodeLL"), pA1, pA2);
  }

  private SeplogicState handleAssignmentStatement(SeplogicState element,
      CAssignment expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {

    // left hand side
    CExpression leftExpression = expression.getLeftHandSide();
    String leftVarName = null;
    boolean leftDereference;

    if (leftExpression instanceof CIdExpression) {
      // a
      leftDereference = false;
      leftVarName = ((CIdExpression) leftExpression).getName();

    } else if (leftExpression instanceof CPointerExpression) {
      // *a
      CPointerExpression ptrExpression = (CPointerExpression)leftExpression;
      leftDereference = true;

      leftExpression = ptrExpression.getOperand();

      if (leftExpression instanceof CCastExpression) {
        leftExpression = ((CCastExpression) leftExpression).getOperand();
      }

      if (!(leftExpression instanceof CIdExpression)) {
        // not a variable at left hand side
        throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
            leftExpression);
      }

      leftVarName = leftExpression.toASTString();
    } else {
      // TODO fields, arrays
      throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
          leftExpression);
    }

    // right hand side
    CRightHandSide op2 = expression.getRightHandSide();

    // handles *a = x and a = x
    return handleAssignment(element, leftVarName, leftDereference, op2, cfaEdge);
  }

  private SeplogicState handleAssignment(SeplogicState element,
      String leftVarName, boolean leftDereference,
      CRightHandSide expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {
    return handleAssignment(element, leftVarName, leftDereference, expression, cfaEdge, element.getNamespace());
  }

  private SeplogicState handleAssignment(SeplogicState element,
      String leftVarName, boolean leftDereference,
      CRightHandSide expression, CFAEdge cfaEdge, String leftNamespace)
      throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {

    Handle fPre, fPost;
    String sVarName = null;
    boolean isPureGuard = false;

    if (expression instanceof CStringLiteralExpression) {
      // char* s = "hello world"
      if (leftDereference) { throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge, expression); }
      fPre = psInterface.makeEmp();
      fPost = psInterface.makeEq(psInterface.makeVar(psInterface.loadString(RETVAR)), makeFreshExistential()); // XXX wrong
      sVarName = leftVarName;
      isPureGuard = true;
    } else if (expression instanceof CLiteralExpression) {
      // a = INT
      if (!(expression instanceof CIntegerLiteralExpression)) { throw new UnrecognizedCCodeException(
          "unsupported literal", cfaEdge, expression); }
      long value = ((CIntegerLiteralExpression) expression).getValue().longValue();
      Handle rhsArg;
      rhsArg = makeIntegerConstant(value);
      if (leftDereference) {
        fPre = makePointsTo(makeVarArg(leftVarName, leftNamespace), makeFreshExistential());
        fPost = makePointsTo(makeVarArg(leftVarName, leftNamespace), rhsArg);
      } else {
        fPre = psInterface.makeEmp();
        fPost = psInterface.makeEq(psInterface.makeVar(psInterface.loadString(RETVAR)), rhsArg);
        sVarName = leftVarName;
        isPureGuard = true;
      }
    } else if (expression instanceof CCastExpression) {
      // a = (int*)b
      CExpression operand = ((CCastExpression) expression).getOperand();
      Handle rhsArg;
      if ("0".equals(operand.toASTString())) {
        rhsArg = psInterface.makeNil();
      } else {
        rhsArg = makeVarArg(operand.toASTString(), element);
      }

      if (leftDereference) {
        fPre = makePointsTo(makeVarArg(leftVarName, leftNamespace), makeFreshExistential());
        fPost = makePointsTo(makeVarArg(leftVarName, leftNamespace), rhsArg);
      } else {
        fPre = psInterface.makeEmp();
        fPost = psInterface.makeEq(psInterface.makeVar(psInterface.loadString(RETVAR)), rhsArg);
        sVarName = leftVarName;
        isPureGuard = true;
      }

    } else if (expression instanceof CFunctionCallExpression) {
      // a = func()

      CFunctionCallExpression funcExpression =
          (CFunctionCallExpression) expression;
      String functionName =
          funcExpression.getFunctionNameExpression().toASTString();

      if (functionName.equals("malloc")) {
        return handleMalloc(element, leftVarName, leftDereference, funcExpression, cfaEdge);

      } else {
        // if it's an internal call, it's handled in handleReturnFromFunction()
        // it it's an external call and we do not know the function, we cannot
        // do more than set the pointer to unknown
        fPre = psInterface.makeEmp();
        fPost = psInterface.makeEq(psInterface.makeVar(psInterface.loadString(RETVAR)), makeFreshExistential());
        sVarName = leftVarName;
      }

    } else if (expression instanceof CBinaryExpression) {
      // a = b + c

      CBinaryExpression binExpression = (CBinaryExpression) expression;
      BinaryOperator typeOfOperator = binExpression.getOperator();
      CExpression op1 = binExpression.getOperand1();
      CExpression op2 = binExpression.getOperand2();

      if (op1 instanceof CCastExpression) {
        op1 = ((CCastExpression) op1).getOperand();
      }


      if (op1 instanceof CIdExpression) {
        String rightName = ((CIdExpression) op1).getName();

        if (!(typeOfOperator == BinaryOperator.PLUS
        || typeOfOperator == BinaryOperator.MINUS)) { throw new UnrecognizedCCodeException(binExpression.toASTString(), cfaEdge); }

        if (op2 instanceof CLiteralExpression) {
          long offset = ((CIntegerLiteralExpression) op2).asLong();
          if (typeOfOperator == BinaryOperator.MINUS) {
            offset = -offset;
          }

          fPre = psInterface.makeEmp();
          fPost = psInterface.makeEq(psInterface.makeVar(psInterface.loadString(RETVAR)), psInterface.makePlus(makeVarArg(rightName, element), makeIntegerConstant(offset)));
          sVarName = leftVarName;
          isPureGuard = true;

        } else if (op2 instanceof CIdExpression) {
          throw new UnrecognizedCCodeException("unsupported variable addition", cfaEdge, op2);

          /* XXX fill
          missing = new MissingInformation();
          missing.actionLeftPointer = leftPointer;
          missing.actionRightPointer = rightPointer;
          missing.actionDereferenceFirst = leftDereference;
          missing.actionOffsetNegative =
              (typeOfOperator == BinaryOperator.MINUS);
          missing.actionASTNode = op2;
          */
        } else {
          throw new UnrecognizedCCodeException("not expected in CIL",
              cfaEdge, op2);
        }
      } else {
        throw new UnrecognizedCCodeException("unsupported", cfaEdge, expression);
      }
      /* XXX fill

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
        */
    } else if (expression instanceof CUnaryExpression) {
      CUnaryExpression unaryExpression = (CUnaryExpression) expression;
      UnaryOperator op = unaryExpression.getOperator();

      if (op == UnaryOperator.AMPER) {
        // a = &b
        // XXX correct?!
        Handle rhsArg = makeVarArg(unaryExpression.getOperand().toASTString(), element);
        if (leftDereference) {
          fPre = makePointsTo(makeVarArg(leftVarName, leftNamespace), makeFreshExistential());
          fPost = makePointsTo(makeVarArg(leftVarName, leftNamespace), rhsArg);
        } else {
          fPre = psInterface.makeEmp();
          fPost = psInterface.makeEq(psInterface.makeVar(psInterface.loadString(RETVAR)), rhsArg);
          sVarName = leftVarName;
          isPureGuard = true;
        }

      } else if (op == UnaryOperator.MINUS) {
        throw new UnrecognizedCCodeException("unsupported", cfaEdge, expression);
      } else {
        throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
            unaryExpression);
      }
    } else if (expression instanceof CPointerExpression) {
      // a = *b

      CPointerExpression ptrExpression = (CPointerExpression) expression;
      expression = ptrExpression.getOperand();

      if (expression instanceof CCastExpression) {
        expression = ((CCastExpression) expression).getOperand();
      }

      if (!(expression instanceof CIdExpression)) {
        // not a variable at left hand side
        throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
            expression);
      }

      if (leftDereference) {
        throw new UnrecognizedCCodeException("unsupported", cfaEdge,
            expression);
      } else {
        Handle rhsArg = makeFreshExistential();
        fPre = makePointsTo(makeVarArg(expression.toASTString(), element), rhsArg);
        fPost = psInterface.makeStar(fPre, psInterface.makeEq(psInterface.makeVar(psInterface.loadString(RETVAR)), rhsArg));
        sVarName = leftVarName;
      }
    } else if (expression instanceof CIdExpression) {
      // a = b
      String rightName = ((CIdExpression) expression).getName();

      Handle lhsArg = makeVarArg(rightName, element);
      if (leftDereference) {
        fPre = makePointsTo(makeVarArg(leftVarName, leftNamespace), makeFreshExistential());
        fPost = makePointsTo(makeVarArg(leftVarName, leftNamespace), lhsArg);
      } else {
        fPre = psInterface.makeEmp();
        fPost = psInterface.makeEq(psInterface.makeVar(psInterface.loadString(RETVAR)), lhsArg);
        sVarName = leftVarName;
        isPureGuard = true;
      }
    } else {
      throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
          expression);
    }

    try {
      return element.performSpecificationAssignment(fPre, fPost,
          (sVarName == null) ? null : quoteVar(sVarName, leftNamespace));
    } catch (SeplogicQueryUnsuccessful e) {
      e.setIsPureGuard(isPureGuard);
      throw e;
    }
  }

  private SeplogicState handleMalloc(SeplogicState element,
      String pLeftVarName, boolean leftDereference, CFunctionCallExpression expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {

    List<CExpression> parameters = expression.getParameterExpressions();
    if (parameters.size() != 1) { throw new UnrecognizedCCodeException("Wrong number of arguments for malloc", cfaEdge,
        expression); }
    CExpression parameter = parameters.get(0);
    if (leftDereference) {
      throw new UnrecognizedCCodeException("unsupported", cfaEdge, expression);
    }

    long size;
    if (parameter instanceof CLiteralExpression) {
      if (!(parameter instanceof CIntegerLiteralExpression)) {
        throw new UnrecognizedCCodeException("non-integers not supported", cfaEdge, parameter);
      }
      size = ((CIntegerLiteralExpression) parameter).asLong();
      if (size < 0) { throw new UnrecognizedCCodeException("malloc with size < 0, but malloc takes unsigned parameter",
          cfaEdge, parameter); }
      if (size > 0x7FFFFFFF) { throw new UnrecognizedCCodeException("Malloc too large", cfaEdge, expression); }
    } else if (parameter instanceof CIdExpression) {
      String varName = quoteVar(((CIdExpression) parameter).getName(), element);
      Long sizeLong = element.extractExplicitValue(varName);
      if (sizeLong == null) {
        throw new UnrecognizedCCodeException("Could not extract malloc size (no equalities found)",
            cfaEdge, expression);
      }
      size = sizeLong.longValue();
    } else {
      throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
          parameter);
    }

    // XXX freshen correct?
    SeplogicState curElem =
        element.freshenVariable(quoteVar(pLeftVarName, element)).performSpecificationAssignment(psInterface.makeEmp(),
            makePointsTo(makeVarArg(pLeftVarName, element), makeFreshExistential()), null);
    // XXX completely nuts but enough for list2.cil.c on amd64
    int i = 8;
    while (i < size) {
      curElem =
          curElem.performSpecificationAssignment(
              psInterface.makeEmp(),
              makePointsTo(psInterface.makePlus(makeVarArg(pLeftVarName, element), makeIntegerConstant(i)),
                  makeFreshExistential()), null);
      i += 8;
    }
    return curElem;

  }

  private Handle makeIntegerConstant(long pL) {
    return psInterface.makeInt(psInterface.loadString("" + pL));
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState element, List<AbstractState> elements, CFAEdge cfaEdge,
      Precision precision) throws CPATransferException {
    return null;
  }
}
