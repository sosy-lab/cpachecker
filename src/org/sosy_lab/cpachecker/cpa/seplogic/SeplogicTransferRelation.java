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
package org.sosy_lab.cpachecker.cpa.seplogic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCompositeTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTElaboratedTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.seplogic.SeplogicElement.SeplogicQueryUnsuccessful;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Argument;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Empty;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Equality;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Formula;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Inequality;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.OpArgument;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.SeparatingConjunction;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.SpatialPredicate;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.StringArgument;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.VarArgument;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Variable;
import org.sosy_lab.cpachecker.cpa.types.Type;
import org.sosy_lab.cpachecker.cpa.types.TypesElement;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;


public class SeplogicTransferRelation implements TransferRelation {

  boolean entryFunctionProcessed = false;
  long existentialVarIndex = 0;
  private long namespaceCounter = 0;
  private static final String RETVAR = "$RET";

  private VarArgument makeFreshExistential() {
    return new VarArgument(new Variable("_v" + existentialVarIndex++));
  }

  @Override
  public Collection<SeplogicElement> getAbstractSuccessors(
      AbstractElement element, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException {

    SeplogicElement currentElement = (SeplogicElement) element;
    SeplogicElement successor;

    try {
      switch (cfaEdge.getEdgeType()) {

      case DeclarationEdge:
        DeclarationEdge declEdge = (DeclarationEdge) cfaEdge;
        successor =
            handleDeclaration(currentElement, cfaEdge, declEdge.getDeclaration().getName(), declEdge.getDeclaration()
                .getDeclSpecifier());
        break;

      case StatementEdge:
        successor = handleStatement(currentElement, ((StatementEdge) cfaEdge).getStatement(),
            (StatementEdge) cfaEdge);
        break;

      case ReturnStatementEdge:
        IASTExpression expression = ((ReturnStatementEdge) cfaEdge).getExpression();
        if (expression != null) {
          // non-void function
          successor = handleAssignment(currentElement, RETVAR, false, expression, cfaEdge);
        } else {
          successor = currentElement;
        }
        break;

      case AssumeEdge:
        AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
        successor =
            handleAssume(currentElement, assumeEdge.getExpression(), assumeEdge.getTruthAssumption(), assumeEdge);
        break;

      case FunctionCallEdge:
        successor = handleFunctionCall(currentElement, cfaEdge);
        break;

      case FunctionReturnEdge:
        // now handle the complete a = func(x) statement in the CallToReturnEdge
        FunctionReturnEdge returnEdge = (FunctionReturnEdge) cfaEdge;
        CallToReturnEdge ctrEdge = returnEdge.getSuccessor().getEnteringSummaryEdge();
        successor = handleReturnFromFunction(currentElement, ctrEdge.getExpression(), ctrEdge);
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
        e.printStackTrace();
        System.err.println("Pure guard failed (-> false) in CFA Edge: N" + cfaEdge.getPredecessor().getNodeNumber()
            + " -> N" + cfaEdge.getSuccessor().getNodeNumber());
        return Collections.emptySet();
        //return Collections.singleton(currentElement.makeExceptionState(e));
      } else {
        e.printStackTrace();
        System.err.println("Must be a null-dereference in CFA Edge: N" + cfaEdge.getPredecessor().getNodeNumber() + " -> N"
          + cfaEdge.getSuccessor().getNodeNumber());
        return Collections.singleton(currentElement.makeExceptionState(e));
      }
      //throw e;
    }
    if (successor.isFalse()) {
      System.err.println("Successor implies false in CFA Edge: N" + cfaEdge.getPredecessor().getNodeNumber() + " -> N"
          + cfaEdge.getSuccessor().getNodeNumber());
      return Collections.emptySet();
    }

    // XXX a bit too arbitrary
    if (!successor.doBreak() && (cfaEdge.getSuccessor().isLoopStart() || true)) {
      successor = successor.abstract_();
    }
    return Collections.singleton(successor);
  }

  private SeplogicElement handleDeclaration(SeplogicElement element, CFAEdge edge,
      String name,
      IType specifier) throws CPATransferException {

    if (name == null
        && (specifier instanceof IASTElaboratedTypeSpecifier
        || specifier instanceof IASTCompositeTypeSpecifier)) {
      // ignore struct prototypes
      return element;
    }

    if (name == null) { throw new UnrecognizedCCodeException("not expected in CIL", edge); }

    if (specifier instanceof IASTFunctionTypeSpecifier) { return element; }

    if (specifier instanceof IASTCompositeTypeSpecifier
        || specifier instanceof IASTElaboratedTypeSpecifier
        || specifier instanceof IASTEnumerationSpecifier) {

      // structs on stack etc.
      return element;
    }

    String varName = name;
    return element; //XXX wrong
    // throw new UnrecognizedCCodeException("unsupported", edge);

    /* XXX fill
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

  private SeplogicElement handleAssume(SeplogicElement element,
      IASTExpression expression, boolean isTrueBranch, AssumeEdge assumeEdge)
      throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {

    Argument a1, a2;

    if (expression instanceof IASTBinaryExpression) {
      IASTBinaryExpression binaryExpression = (IASTBinaryExpression) expression;

      if (binaryExpression.getOperator() == BinaryOperator.EQUALS) {
        return handleBinaryAssume(element, binaryExpression, isTrueBranch, assumeEdge);

      } else if (binaryExpression.getOperator() == BinaryOperator.NOT_EQUALS) {
        return handleBinaryAssume(element, binaryExpression, !isTrueBranch, assumeEdge);

      } else {
        // assume it's not a pointer comparison
        return element;
      }

    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression) expression;

      if (unaryExpression.getOperator() == UnaryOperator.NOT) {
        return handleAssume(element, unaryExpression.getOperand(), !isTrueBranch, assumeEdge);

      } else if (unaryExpression.getOperator() == UnaryOperator.STAR) {
        // if (*var)
        String varName = expression.toASTString();

        if (!isTrueBranch) { return element; }
        a1 = makeVarArg(varName, element);
        a2 = makeIntegerConstant(0);
        isTrueBranch = !isTrueBranch;
      } else {
        throw new UnrecognizedCCodeException("not expected in CIL", assumeEdge,
            expression);
      }
    } else if (expression instanceof IASTIdExpression) {
      // if (a)
      String varName = ((IASTIdExpression) expression).getName();

      a1 = makeVarArg(varName, element);
      a2 = makeIntegerConstant(0);
    } else if (expression instanceof IASTCastExpression) {
      return handleAssume(element, ((IASTCastExpression) expression).getOperand(), isTrueBranch,
          assumeEdge);
    } else {
      throw new UnrecognizedCCodeException("unsupported", assumeEdge);
    }

    Formula f;
    if (isTrueBranch) {
      f = new Equality(a1, a2);
    } else {
      f = new Inequality(a1, a2);
    }
    try {
      return element.performSpecificationAssignment(new Empty(), f, null);
    } catch (SeplogicQueryUnsuccessful e) {
      e.setIsPureGuard(true);
      throw e;
    }
  }

  private String quoteVar(String varName, SeplogicElement element) {
    return quoteVar(varName, element.getNamespace());
  }

  private String quoteVar(String varName, String localNamespace) {
    varName = localNamespace + "$" + varName;
    return (varName.charAt(0) == '_') ? ("ZZZ" + varName) : varName;
  }

  private VarArgument makeVarArg(String varName, SeplogicElement element) {
    return makeVarArg(varName, element.getNamespace());
  }

  private VarArgument makeVarArg(String varName, String localNamespace) {
    return new VarArgument(new Variable(quoteVar(varName, localNamespace)));
  }

  private SeplogicElement handleBinaryAssume(SeplogicElement element,
      IASTBinaryExpression expression, boolean isTrueBranch,
      AssumeEdge assumeEdge) throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {

    IASTExpression leftOp = expression.getOperand1();
    IASTExpression rightOp = expression.getOperand2();

    Formula f;
    Argument a1 = convertOperandToArgument(leftOp, element);
    Argument a2 = convertOperandToArgument(rightOp, element);
    if (isTrueBranch) {
      f = new Equality(a1, a2);
    } else {
      f = new Inequality(a1, a2);
    }

    try {
      return element.performSpecificationAssignment(new Empty(), f, null);
    } catch (SeplogicQueryUnsuccessful e) {
      e.setIsPureGuard(true);
      throw e;
    }
  }

  private Argument convertOperandToArgument(IASTExpression pLeftOp, SeplogicElement element)
      throws UnrecognizedCCodeException {
    if (pLeftOp instanceof IASTIntegerLiteralExpression) {
      return makeIntegerConstant(((IASTIntegerLiteralExpression) pLeftOp).getValue().longValue());
    } else if (pLeftOp instanceof IASTIdExpression) {
      return makeVarArg(pLeftOp.toASTString(), element);
    } else {
      throw new UnrecognizedCCodeException("unsupported expression type", null, pLeftOp);
    }
  }

  private SeplogicElement handleFunctionCall(SeplogicElement element,
      CFAEdge cfaEdge) throws UnrecognizedCCodeException {

    FunctionDefinitionNode funcDefNode = (FunctionDefinitionNode) cfaEdge.getSuccessor();
    String funcName = funcDefNode.getFunctionName();
    String nsName = funcName + namespaceCounter++;

    List<String> formalParameters = funcDefNode.getFunctionParameterNames();
    List<IASTExpression> actualParameters = ((FunctionCallEdge) cfaEdge).getArguments();

    if (formalParameters != null && formalParameters.size() > 0
        && !actualParameters.isEmpty()) {

      assert formalParameters.size() == actualParameters.size();

      for (int i = 0; i < actualParameters.size(); i++) {
        IASTExpression parameter = actualParameters.get(i);
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

  private SeplogicElement handleReturnFromFunction(SeplogicElement element,
      IASTFunctionCall expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException {

    element = element.popNamespace();

    // use function result
    if (expression instanceof IASTFunctionCallAssignmentStatement) {
      // a = func()
      IASTFunctionCallAssignmentStatement assignExpression = (IASTFunctionCallAssignmentStatement) expression;
      IASTExpression leftOperand = assignExpression.getLeftHandSide();


      if (leftOperand instanceof IASTIdExpression) {
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

    } else if (expression instanceof IASTFunctionCallStatement) {
      // func()
      // ignore
      return element;
    } else {
      throw new UnrecognizedCCodeException(cfaEdge, expression.asStatement());
    }

  }

  private SeplogicElement handleStatement(SeplogicElement element,
      IASTStatement expression, StatementEdge cfaEdge)
      throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {

    if (expression instanceof IASTFunctionCallStatement) {
      // this is a mere function call (func(a))
      IASTFunctionCallExpression funcExpression =
          ((IASTFunctionCallStatement) expression).getFunctionCallExpression();
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

    } else if (expression instanceof IASTAssignment) {
      // statement is an assignment expression, e.g. a = b or a = a+b;
      return handleAssignmentStatement(element, (IASTAssignment) expression, cfaEdge);

    } else {
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
  }

  private SeplogicElement handleFree(SeplogicElement element,
      IASTFunctionCallExpression expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {

    List<IASTExpression> parameters = expression.getParameterExpressions();
    if (parameters.size() != 1) { throw new UnrecognizedCCodeException("Wrong number of arguments for free", cfaEdge,
        expression); }
    IASTExpression parameter = parameters.get(0);

    if (parameter instanceof IASTIdExpression) {
      String ident = ((IASTIdExpression) parameter).getName();
      return element.performSpecificationAssignment(makePointsTo(makeVarArg(ident, element),
          makeFreshExistential()), new Empty(), null);
    } else {
      throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
          parameter);
    }
  }

  private Formula makePointsTo(Argument pA1, Argument pA2) {
    List<Argument> args = new ArrayList<Argument>();
    args.add(pA1);
    args.add(pA2);
    return new SpatialPredicate("NodeLL", args);
  }

  private SeplogicElement handleAssignmentStatement(SeplogicElement element,
      IASTAssignment expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {

    // left hand side
    IASTExpression leftExpression = expression.getLeftHandSide();
    String leftVarName = null;
    boolean leftDereference;

    if (leftExpression instanceof IASTIdExpression) {
      // a
      leftDereference = false;
      leftVarName = ((IASTIdExpression) leftExpression).getName();

    } else if (leftExpression instanceof IASTUnaryExpression) {

      IASTUnaryExpression unaryExpression = (IASTUnaryExpression) leftExpression;
      if (unaryExpression.getOperator() == UnaryOperator.STAR) {
        // *a
        leftDereference = true;

        leftExpression = unaryExpression.getOperand();

        boolean leftCast = false;
        if (leftExpression instanceof IASTCastExpression) {
          leftCast = true;
          leftExpression = ((IASTCastExpression) leftExpression).getOperand();
        }

        if (!(leftExpression instanceof IASTIdExpression)) {
          // not a variable at left hand side
          throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
              leftExpression);
        }

        leftVarName = leftExpression.toASTString();

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
    return handleAssignment(element, leftVarName, leftDereference, op2, cfaEdge);
  }

  private SeplogicElement handleAssignment(SeplogicElement element,
      String leftVarName, boolean leftDereference,
      IASTRightHandSide expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {
    return handleAssignment(element, leftVarName, leftDereference, expression, cfaEdge, element.getNamespace());
  }

  private SeplogicElement handleAssignment(SeplogicElement element,
      String leftVarName, boolean leftDereference,
      IASTRightHandSide expression, CFAEdge cfaEdge, String leftNamespace)
      throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {

    Formula fPre, fPost;
    String sVarName = null;
    boolean isPureGuard = false;

    if (expression instanceof IASTStringLiteralExpression) {
      // char* s = "hello world"
      if (leftDereference) { throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge, expression); }
      fPre = new Empty();
      fPost = new Equality(new VarArgument(new Variable(Formula.RETVAR)), makeFreshExistential()); // XXX wrong
      sVarName = leftVarName;
      isPureGuard = true;
    } else if (expression instanceof IASTLiteralExpression) {
      // a = INT
      if (!(expression instanceof IASTIntegerLiteralExpression)) { throw new UnrecognizedCCodeException(
          "unsupported literal", cfaEdge, expression); }
      long value = ((IASTIntegerLiteralExpression) expression).getValue().longValue();
      Argument rhsArg;
      rhsArg = makeIntegerConstant(value);
      if (leftDereference) {
        fPre = makePointsTo(makeVarArg(leftVarName, leftNamespace), makeFreshExistential());
        fPost = makePointsTo(makeVarArg(leftVarName, leftNamespace), rhsArg);
      } else {
        fPre = new Empty();
        fPost = new Equality(new VarArgument(new Variable(Formula.RETVAR)), rhsArg);
        sVarName = leftVarName;
        isPureGuard = true;
      }
    } else if (expression instanceof IASTCastExpression) {
      // a = (int*)b
      Argument rhsArg = makeVarArg(((IASTCastExpression) expression).getOperand().toASTString(), element);
      if (leftDereference) {
        fPre = makePointsTo(makeVarArg(leftVarName, leftNamespace), makeFreshExistential());
        fPost = makePointsTo(makeVarArg(leftVarName, leftNamespace), rhsArg);
      } else {
        fPre = new Empty();
        fPost = new Equality(new VarArgument(new Variable(Formula.RETVAR)), rhsArg);
        sVarName = leftVarName;
        isPureGuard = true;
      }

    } else if (expression instanceof IASTFunctionCallExpression) {
      // a = func()

      IASTFunctionCallExpression funcExpression =
          (IASTFunctionCallExpression) expression;
      String functionName =
          funcExpression.getFunctionNameExpression().toASTString();

      if (functionName.equals("malloc")) {
        return handleMalloc(element, leftVarName, leftDereference, funcExpression, cfaEdge);

      } else {
        // if it's an internal call, it's handled in handleReturnFromFunction()
        // it it's an external call and we do not know the function, we cannot
        // do more than set the pointer to unknown
        fPre = new Empty();
        fPost = new Equality(new VarArgument(new Variable(Formula.RETVAR)), makeFreshExistential());
        sVarName = leftVarName;
      }

    } else if (expression instanceof IASTBinaryExpression) {
      // a = b + c

      IASTBinaryExpression binExpression = (IASTBinaryExpression) expression;
      BinaryOperator typeOfOperator = binExpression.getOperator();
      IASTExpression op1 = binExpression.getOperand1();
      IASTExpression op2 = binExpression.getOperand2();

      if (op1 instanceof IASTCastExpression) {
        op1 = ((IASTCastExpression) op1).getOperand();
      }


      if (op1 instanceof IASTIdExpression) {
        String rightName = ((IASTIdExpression) op1).getName();

        if (!(typeOfOperator == BinaryOperator.PLUS
        || typeOfOperator == BinaryOperator.MINUS)) { throw new UnrecognizedCCodeException(cfaEdge, binExpression); }

        if (op2 instanceof IASTLiteralExpression) {
          long offset = ((IASTIntegerLiteralExpression) op2).asLong();
          if (typeOfOperator == BinaryOperator.MINUS) {
            offset = -offset;
          }

          List<Argument> args = new ArrayList<Argument>();
          args.add(makeVarArg(rightName, element));
          args.add(makeIntegerConstant(offset));
          fPre = new Empty();
          fPost = new Equality(new VarArgument(new Variable(Formula.RETVAR)), new OpArgument("builtin_plus", args));
          sVarName = leftVarName;
          isPureGuard = true;

        } else if (op2 instanceof IASTIdExpression) {
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
    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression) expression;
      UnaryOperator op = unaryExpression.getOperator();

      if (op == UnaryOperator.AMPER) {
        // a = &b
        // XXX correct?!
        Argument rhsArg = makeVarArg(unaryExpression.getOperand().toASTString(), element);
        if (leftDereference) {
          fPre = makePointsTo(makeVarArg(leftVarName, leftNamespace), makeFreshExistential());
          fPost = makePointsTo(makeVarArg(leftVarName, leftNamespace), rhsArg);
        } else {
          fPre = new Empty();
          fPost = new Equality(new VarArgument(new Variable(Formula.RETVAR)), rhsArg);
          sVarName = leftVarName;
          isPureGuard = true;
        }

      } else if (op == UnaryOperator.MINUS) {
        throw new UnrecognizedCCodeException("unsupported", cfaEdge, expression);
      } else if (op == UnaryOperator.STAR) {
        // a = *b

        expression = unaryExpression.getOperand();

        if (expression instanceof IASTCastExpression) {
          expression = ((IASTCastExpression) expression).getOperand();
        }

        if (!(expression instanceof IASTIdExpression)) {
          // not a variable at left hand side
          throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
              expression);
        }

        if (leftDereference) {
          throw new UnrecognizedCCodeException("unsupported", cfaEdge,
              expression);
        } else {
          Argument rhsArg = makeFreshExistential();
          fPre = makePointsTo(makeVarArg(expression.toASTString(), element), rhsArg);
          fPost = new SeparatingConjunction(fPre, new Equality(new VarArgument(new Variable(Formula.RETVAR)), rhsArg));
          sVarName = leftVarName;
        }

      } else {
        throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
            unaryExpression);
      }

    } else if (expression instanceof IASTIdExpression) {
      // a = b
      String rightName = ((IASTIdExpression) expression).getName();

      Argument lhsArg = makeVarArg(rightName, element);
      if (leftDereference) {
        fPre = makePointsTo(makeVarArg(leftVarName, leftNamespace), makeFreshExistential());
        fPost = makePointsTo(makeVarArg(leftVarName, leftNamespace), lhsArg);
      } else {
        fPre = new Empty();
        fPost = new Equality(new VarArgument(new Variable(Formula.RETVAR)), lhsArg);
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

  private SeplogicElement handleMalloc(SeplogicElement element,
      String pLeftVarName, boolean leftDereference, IASTFunctionCallExpression expression, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException, SeplogicQueryUnsuccessful {

    List<IASTExpression> parameters = expression.getParameterExpressions();
    if (parameters.size() != 1) { throw new UnrecognizedCCodeException("Wrong number of arguments for malloc", cfaEdge,
        expression); }
    IASTExpression parameter = parameters.get(0);
    if (leftDereference)
      throw new UnrecognizedCCodeException("unsupported", cfaEdge, expression);

    long size;
    if (parameter instanceof IASTLiteralExpression) {
      if (!(parameter instanceof IASTIntegerLiteralExpression))
        throw new UnrecognizedCCodeException("non-integers not supported", cfaEdge, parameter);
      size = ((IASTIntegerLiteralExpression) parameter).asLong();
      if (size < 0) { throw new UnrecognizedCCodeException("malloc with size < 0, but malloc takes unsigned parameter",
          cfaEdge, parameter); }
      if (size > 0x7FFFFFFF) { throw new UnrecognizedCCodeException("Malloc too large", cfaEdge, expression); }
    } else if (parameter instanceof IASTIdExpression) {
      String varName = quoteVar(((IASTIdExpression) parameter).getName(), element);
      Long sizeLong = element.extractExplicitValue(varName);
      if (sizeLong == null)
        throw new UnrecognizedCCodeException("Could not extract malloc size (no equalities found)",
            cfaEdge, expression);
      size = sizeLong.longValue();
    } else {
      throw new UnrecognizedCCodeException("not expected in CIL", cfaEdge,
          parameter);
    }

    // XXX freshen correct?
    SeplogicElement curElem =
        element.freshenVariable(quoteVar(pLeftVarName, element)).performSpecificationAssignment(new Empty(),
            makePointsTo(makeVarArg(pLeftVarName, element), makeFreshExistential()), null);
    // XXX completely nuts but enough for list2.cil.c on amd64
    int i = 8;
    while (i < size) {
      curElem =
          curElem.performSpecificationAssignment(
              new Empty(),
              makePointsTo(new OpArgument("builtin_plus", makeVarArg(pLeftVarName, element), makeIntegerConstant(i)),
                  makeFreshExistential()), null);
      i += 8;
    }
    return curElem;

  }

  private OpArgument makeIntegerConstant(long pL) {
    return new OpArgument("numeric_const", new StringArgument("" + pL));
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement element, List<AbstractElement> elements, CFAEdge cfaEdge,
      Precision precision) throws CPATransferException {

    if (!(element instanceof SeplogicElement)) { return null; }

    SeplogicElement seplogicElement = (SeplogicElement) element;

    for (AbstractElement ae : elements) {
      try {
        if (ae instanceof TypesElement) {
          strengthen(seplogicElement, (TypesElement) ae, cfaEdge, precision);
        }

      } catch (UnrecognizedCCodeException e) {
        throw new CPATransferException(e.getMessage());
      }
    }
    return null;
  }

  /**
   * strengthen called for TypesCPA
   */
  private void strengthen(SeplogicElement seplogicElement,
      TypesElement typesElement, CFAEdge cfaEdge, Precision precision)
      throws UnrecognizedCCodeException {

    /* XXX fill
      if (missing.typeInformationPointer == null) {
        return;
      }

      // pointer variable declaration
      String functionName = cfaEdge.getSuccessor().getFunctionName();
      if (missing.typeInformationEdge instanceof GlobalDeclarationEdge) {
        functionName = null;
      }

      String varName = missing.typeInformationName;
      Type type = typesElement.getVariableType(functionName, varName);

      setSizeOfTarget(missing.typeInformationPointer, type);
      */
  }

  /**
   * checks all possible locations for type information of a given name
   */
  private Type findType(TypesElement typeElem, CFAEdge cfaEdge, String varName) {
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

}
