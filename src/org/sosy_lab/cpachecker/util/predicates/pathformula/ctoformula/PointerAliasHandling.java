/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.types.CtoFormulaTypeUtils.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.types.CtoFormulaTypeUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

@Options(prefix="cpa.predicate")
class PointerAliasHandling extends CtoFormulaConverter {

  static final String POINTER_VARIABLE = "__content_of__";
  static final Predicate<String> IS_POINTER_VARIABLE = CtoFormulaConverter.startsWith(POINTER_VARIABLE);

  /** The prefix used for variables representing memory locations. */
  static final String MEMORY_ADDRESS_VARIABLE_PREFIX = "__address_of__";
  static final Predicate<String> IS_MEMORY_ADDRESS_VARIABLE = CtoFormulaConverter.startsWith(MEMORY_ADDRESS_VARIABLE_PREFIX);

  /**
   * The prefix used for memory locations derived from malloc calls.
   * (Must start with {@link #MEMORY_ADDRESS_VARIABLE_PREFIX}.)
   */
  static final String MALLOC_VARIABLE_PREFIX =
      MEMORY_ADDRESS_VARIABLE_PREFIX + "#";

  /** The variable name that's used to store the malloc counter in the SSAMap. */
  static final String MALLOC_COUNTER_VARIABLE_NAME = "#malloc";


  /** Takes a (scoped) variable name and returns the pointer variable name. */
  static String makePointerMaskName(String scopedId, SSAMapBuilder ssa) {
    return POINTER_VARIABLE + scopedId + "__at__" + ssa.getIndex(scopedId) + "__end";
  }

  static Variable makePointerMask(Variable pointerVar, SSAMapBuilder ssa) {
    Variable ptrMask = Variable.create(makePointerMaskName(pointerVar.getName(), ssa), dereferencedType(pointerVar.getType()));
    if (isDereferenceType(ptrMask.getType())) {
      // lookup in ssa map: Maybe we assigned a size to this current variable
      CType savedVarType = ssa.getType(ptrMask.getName());
      if (savedVarType != null) {
        ptrMask = ptrMask.withType(savedVarType);
        assert isDereferenceType(savedVarType)
            : "The savedVar should also be a DereferenceType!";
      }
    }
    return ptrMask;
  }
  /**
   * Takes a pointer variable name and returns the name of the associated
   * variable.
   */
  @VisibleForTesting
  static String removePointerMask(String pointerVariable) {
    assert (IS_POINTER_VARIABLE.apply(pointerVariable));

    return pointerVariable.substring(POINTER_VARIABLE.length(), pointerVariable.lastIndexOf("__at__"));
  }

  static Variable removePointerMaskVariable(String pointerVar, CType type) {
    return Variable.create(removePointerMask(pointerVar), makePointerType(type));
  }

  /**Returns the concatenation of MEMORY_ADDRESS_VARIABLE_PREFIX and varName */
  private static String makeMemoryLocationVariableName(String varName) {
    return MEMORY_ADDRESS_VARIABLE_PREFIX + varName;
  }

  /**Returns the concatenation of MEMORY_ADDRESS_VARIABLE_PREFIX and varName */
  static Variable makeMemoryLocationVariable(Variable varName) {
    return Variable.create(makeMemoryLocationVariableName(varName.getName()), makePointerType(varName.getType()));
  }

  private static boolean maybePointer(Variable var, SSAMapBuilder ssa) {
    if (var.getType() instanceof CPointerType) {
      return true;
    }

    // check if it has been used as a pointer before
    String expPtrVarName = makePointerMaskName(var.getName(), ssa);
    return ssa.getType(expPtrVarName) != null;
  }

  /**
   * Returns a list of all variable names representing memory locations in
   * the SSAMap. These memory locations are those previously used.
   *
   * Stored memory locations are prefixed with
   * {@link #MEMORY_ADDRESS_VARIABLE_PREFIX}.
   */
  static Set<Map.Entry<String, CType>> getAllMemoryLocationsFromSsaMap(SSAMapBuilder ssa) {
    return Sets.filter(ssa.allVariablesWithTypes(), liftToVariable(IS_MEMORY_ADDRESS_VARIABLE));
  }

  private static Predicate<Map.Entry<String, CType>> liftToVariable(final Predicate<? super String> stringPred) {
    return new Predicate<Map.Entry<String, CType>>() {
      @Override
      public boolean apply(Map.Entry<String, CType> pInput) {
        return stringPred.apply(pInput.getKey());
      }};
  }

  /**
   * Returns a list of all pointer variables stored in the SSAMap.
   */
  static Set<Map.Entry<String, CType>> getAllPointerVariablesFromSsaMap(SSAMapBuilder ssa) {
    return Sets.filter(ssa.allVariablesWithTypes(), liftToVariable(IS_POINTER_VARIABLE));
  }

  /**
   * Removes all pointer variables belonging to a given variable from a given
   * SSAMapBuilderthat are no longer valid. Validity of an entry expires,
   * when the pointer variable belongs to a variable with an old index.
   *
   * @param newPVar The variable name of the new pointer variable.
   * @param ssa The SSAMapBuilder from which the variables are to be deleted
   */
  static void removeOldPointerVariablesFromSsaMap(String newPVar,
      SSAMapBuilder ssa) {

    String newVar = removePointerMask(newPVar);

    for (String ptrVarName : from(ssa.allVariables()).filter(IS_POINTER_VARIABLE)) {
      String oldVar = removePointerMask(ptrVarName);
      if (!ptrVarName.equals(newPVar) && oldVar.equals(newVar)) {
        ssa.deleteVariable(ptrVarName);
      }
    }
  }


  @Option(description = "Handle field aliasing formulas.")
  boolean omitNonPointerInFieldAliasing = true;

  @Option(description = "Handle field aliasing formulas.")
  boolean handleFieldAliasing = false;

  private final TooComplexVisitor tooComplexVisitor;

  PointerAliasHandling(Configuration pConfig, FormulaManagerView pFmgr,
      MachineModel pMachineModel, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pFmgr, pMachineModel, pLogger);
    pConfig.inject(this, PointerAliasHandling.class);

    if (handleFieldAliasing && !handleFieldAccess) {
      throw new InvalidConfigurationException("Enabling field-aliasing when field-access is disabled is unsupported!");
    }

    this.tooComplexVisitor = new TooComplexVisitor(handleFieldAccess);
  }

  @Override
  protected StatementToFormulaVisitor getStatementVisitor(CFAEdge pEdge, String pFunction,
      SSAMapBuilder pSsa, Constraints pConstraints) {
    ExpressionToFormulaVisitorPointers ev = getCExpressionVisitor(pEdge, pFunction, pSsa, pConstraints);
    return new StatementToFormulaVisitorPointers(ev, this);
  }

  @Override
  protected ExpressionToFormulaVisitorPointers getCExpressionVisitor(CFAEdge pEdge, String pFunction,
      SSAMapBuilder pSsa, Constraints pCo) {
    return new ExpressionToFormulaVisitorPointers(this, pEdge, pFunction, pSsa, pCo);
  }

  @Override
  protected LvalueVisitor getLvalueVisitor(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
    return new LvalueVisitorPointers(this, pEdge, pFunction, pSsa, pCo);
  }

  @Override
  protected BooleanFormula makeReturn(CExpression rightExp, CReturnStatementEdge edge, String function,
      SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {

    BooleanFormula assignments = super.makeReturn(rightExp, edge, function, ssa, constraints);

    if (rightExp != null) {
      String retVarName = getReturnVarName(function);

      CType returnType =
          ((CFunctionEntryNode)edge.getSuccessor().getEntryNode())
            .getFunctionDefinition()
            .getType()
            .getReturnType();

      // if the value to be returned may be a pointer, act accordingly
      BooleanFormula rightAssignment = buildDirectSecondLevelAssignment(
          Variable.create(retVarName, returnType), rightExp, function, constraints, ssa);
      assignments = bfmgr.and(assignments, rightAssignment);
    }

    return assignments;
  }


  @Override
  protected BooleanFormula makeExitFunction(CFunctionSummaryEdge ce, String function,
      SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {

    BooleanFormula assignments = super.makeExitFunction(ce, function, ssa, constraints);

    CFunctionCall retExp = ce.getExpression();
    if (retExp instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement exp = (CFunctionCallAssignmentStatement)retExp;
      String retVarName = getReturnVarName(function);

      CFunctionCallExpression funcCallExp = exp.getRightHandSide();
      CType retType = getReturnType(funcCallExp, ce);

      Formula retVar = makeVariable(retVarName, retType, ssa);
      CExpression e = exp.getLeftHandSide();

      function = ce.getSuccessor().getFunctionName();
      retVar = makeCast(retType, e.getExpressionType(), retVar);

      CExpression left = removeCast(e);
      if (left instanceof CIdExpression) {
        BooleanFormula ptrAssignment = buildDirectReturnSecondLevelAssignment(
            e, Variable.create(retVarName, retType), function, ssa);
        assignments = bfmgr.and(assignments, ptrAssignment);
      }
    }
    return assignments;
  }

  /** Looks up the variable name in the current namespace. */
  Variable scopedIfNecessary(CExpression exp, SSAMapBuilder ssa, String function) {
    assert
        isSupportedExpression(exp)
        : "Can only handle supported expressions";
    Variable name;
    if (exp instanceof CIdExpression) {
      name = super.scopedIfNecessary((CIdExpression)exp, ssa, function);
    } else if (exp instanceof CFieldReference) {
      CFieldReference fExp = (CFieldReference) exp;
      CExpression owner = getRealFieldOwner(fExp);

      // Now we have the struct
      name = scopedIfNecessary(owner, ssa, function);

      // name is now the struct:
      // for example for a pointer to a struct __content_of__ptr__at__2__end

      // construct the field
      name = makeFieldVariable(name, fExp, ssa);
    } else if (exp instanceof CUnaryExpression) {
      CUnaryExpression unary = (CUnaryExpression) exp;
      name = scopedIfNecessary(unary.getOperand(), ssa, function);
      switch (unary.getOperator()) {
      case STAR:
        name = makePointerMask(name, ssa);
        break;
      case AMPER:
        name = makeMemoryLocationVariable(name);
        break;
      case PLUS:
      case MINUS:
      case TILDE:
      case NOT:
      case SIZEOF:
        default:
          throw new AssertionError("Operator not supported in scopedIfNecessary");
      }
    } else if (exp instanceof CCastExpression) {
      // Just ignore
      CCastExpression cast = (CCastExpression) exp;
      name = scopedIfNecessary(cast.getOperand(), ssa, function);
    } else {
      throw new AssertionError("Can't create more complex Variables for Fieldaccess");
    }
    return name;
  }

  private boolean isMemoryLocation(CExpression exp) {
    exp = removeCast(exp);

    // memory allocating function?
    if (exp instanceof CFunctionCall) {
      CExpression fn =
          ((CFunctionCall) exp).getFunctionCallExpression().getFunctionNameExpression();

      if (fn instanceof CIdExpression) {
        String functionName = ((CIdExpression) fn).getName();
        if (memoryAllocationFunctions.contains(functionName)) {
          return true;
        }
      }

    // explicit heap/stack address?
    } else if (exp instanceof CUnaryExpression
        && ((CUnaryExpression) exp).getOperator() == UnaryOperator.AMPER) {
      return true;
    } else if (exp instanceof CFieldReference && !isIndirectFieldReference((CFieldReference)exp)) {
      return isMemoryLocation(((CFieldReference)exp).getFieldOwner());
    }


    return false;
  }

  BooleanFormula buildDirectSecondLevelAssignment(
      Variable lVarName,
      CExpression right, String function,
      Constraints constraints, SSAMapBuilder ssa) {

    if (!hasRepresentableDereference(lVarName)) {
      // The left side is a type that should not be dereferenced, so no 2nd level assignment
      return bfmgr.makeBoolean(true);
    }

    if (!isSupportedExpression(right)) {
      if (isTooComplexExpression(right)) {
        // The right side is too complex
        warnToComplex(right);
      }
      // If it is not too complex we probably need no 2nd level
      // TODO: Check the statement above.
      return bfmgr.makeBoolean(true);
    }

    Formula lVar = makeVariable(lVarName, ssa);

    Variable lPtrVarName = makePointerMask(lVarName, ssa);
    CType leftPtrType = lPtrVarName.getType();
    if (right instanceof CIdExpression ||
        (handleFieldAccess && right instanceof CFieldReference && !isIndirectFieldReference((CFieldReference)right))) {
      // C statement like: s1 = s2; OR s1 = s2.d;

      // include aliases if the left or right side may be a pointer a pointer
      // Assume no pointers affected if unrepresentable values are assigned
      Variable rightVar = scopedIfNecessary(right, ssa, function);
      if ((maybePointer(lVarName, ssa) || maybePointer(rightVar, ssa)) &&
          hasRepresentableDereference(right)) {
        // we assume that either the left or the right hand side is a pointer
        // so we add the equality: *l = *r
        Variable rPtrVarName = makePointerMask(rightVar, ssa);

        boolean leftT, rightT;
        if ((leftT = isDereferenceType(leftPtrType)) |
            (rightT = isDereferenceType(rPtrVarName.getType()))) {
          // One of those types is no pointer so try to guess dereferenced type.

          if (leftT) {
            if (rightT) {
              // Right is actually no pointer but was used as pointer before, so we can use its type.
            }

            // OK left is no pointer, but right is, for example:
            // l = r; // l is unsigned int and r is *long
            // now we assign *l to the type of *r if *l was not assigned before
            CType currentGuess = getGuessedType(leftPtrType);
            if (currentGuess == null) {
              lPtrVarName =
                  lPtrVarName.withType(setGuessedType(leftPtrType, rPtrVarName.getType()));
            } else {
              if (getSizeof(rPtrVarName.getType()) != getSizeof(currentGuess)) {
                log(Level.WARNING, "Second assignment of an variable that is no pointer with different size");
              }
            }
          } else {
            assert rightT : "left and right side are no pointers, however maybePointer was true for one side!";
            // OK right is no pointer, but left is, for example:
            // l = r; // l is unsigned long* and r is unsigned int

            // r was probably assigned with a pointer before and should have a size
            if (!(right.getExpressionType() instanceof CFunctionType)) {
              // ignore function pointer assignments
              CType currentGuess = getGuessedType(rPtrVarName.getType());
              if (currentGuess == null) {
                // TODO: This currently happens when assigning a function to a function pointer.
                // NOTE: Should we set the size of r in this case?
                log(Level.FINEST, "Pointer " + lVarName.getName() + " is assigned the value of variable " +
                    right.toASTString() + " which contains a non-pointer value in line " +
                    right.getFileLocation().getStartingLineNumber());
              } else {
                if (getSizeof(rPtrVarName.getType()) != getSizeof(currentGuess)) {
                  log(Level.WARNING, "Assignment of a pointer from a variable that was assigned by a pointer with different size!");
                }
              }
            }
          }
        }

        Formula lPtrVar = makeVariable(lPtrVarName, ssa);
        Formula rPtrVar = makeVariable(rPtrVarName, ssa);

        return makeNondetAssignment(lPtrVar, rPtrVar);
      } else {
        // we can assume, that no pointers are affected in this assignment
        return bfmgr.makeBoolean(true);
      }

    } else if ((right instanceof CUnaryExpression &&
                   ((CUnaryExpression) right).getOperator() == UnaryOperator.STAR) ||
               (handleFieldAccess && right instanceof CFieldReference && isIndirectFieldReference((CFieldReference)right))) {
      // C statement like: s1 = *s2;
      // OR s1 = *(s.b)
      // OR s1 = s->b

      if (isDereferenceType(leftPtrType)) {
        // We have an assignment to a non-pointer type
        CType guess = getGuessedType(leftPtrType);
        if (guess == null) {
          // We have to guess the size of the dereferenced type here
          // but there is no good guess.
          // TODO: if right side is a **(pointer of a pointer) type use it.
        }
      }

      makeFreshIndex(lPtrVarName.getName(), lPtrVarName.getType(), ssa);
      removeOldPointerVariablesFromSsaMap(lPtrVarName.getName(), ssa);

      Formula lPtrVar = makeVariable(lPtrVarName, ssa);

      if (!(isSupportedExpression(right)) ||
          !(hasRepresentableDereference(right))) {
        // these are statements like s1 = *(s2->f)
        warnToComplex(right);
        return bfmgr.makeBoolean(true);
      }

      Variable rPtrVarName = scopedIfNecessary(right, ssa, function);
      Formula rPtrVar = makeVariable(rPtrVarName, ssa);
      //Formula rPtrVar = makePointerVariable(rRawExpr, function, ssa);

      // the dealiased address of the right hand side may be a pointer itself.
      // to ensure tracking, we need to set the left side
      // equal to the dealiased right side or update the pointer
      // r is the right hand side variable, l is the left hand side variable
      // ∀p ∈ maybePointer: (p = *r) ⇒ (l = p ∧ *l = *p)
      // Note: l = *r holds because of current statement
      for (final Map.Entry<String, CType> ptrVarName : getAllPointerVariablesFromSsaMap(ssa)) {
        Variable varName = removePointerMaskVariable(ptrVarName.getKey(), ptrVarName.getValue());

        if (!varName.equals(lVarName)) {

          Formula var = makeVariable(varName, ssa);
          Formula ptrVar = makeVariable(ptrVarName.getKey(), ptrVarName.getValue(), ssa);

          // p = *r. p is a pointer but *r can be anything
          BooleanFormula ptr = makeNondetAssignment(rPtrVar, var);
          // l = p. p is a pointer but l can be anything.
          BooleanFormula dirEq = makeNondetAssignment(lVar, var);

          // *l = *p. Both can be anything.
          BooleanFormula indirEq =
              makeNondetAssignment(lPtrVar, ptrVar);

          BooleanFormula consequence = bfmgr.and(dirEq, indirEq);
          BooleanFormula constraint = bfmgr.implication(ptr, consequence);
          constraints.addConstraint(constraint);
        }
      }

      // no need to add a second level assignment
      return bfmgr.makeBoolean(true);

    } else if (isMemoryLocation(right)) {
      // s = &x
      // OR s = (&x).t
      // need to update the pointer on the left hand side
      if (right instanceof CUnaryExpression
          && ((CUnaryExpression) right).getOperator() == UnaryOperator.AMPER) {

        CExpression rOperand =
            removeCast(((CUnaryExpression) right).getOperand());
        if (rOperand instanceof CIdExpression &&
            hasRepresentableDereference(lVarName)) {
          Variable rVarName = scopedIfNecessary(rOperand, ssa, function);
          Formula rVar = makeVariable(rVarName, ssa);

          if (isDereferenceType(leftPtrType)) {
            // s is no pointer and if *s was not guessed jet we can use the type of x.
            CType guess = getGuessedType(leftPtrType);
            if (guess == null) {
              lPtrVarName =
                  lPtrVarName.withType(setGuessedType(leftPtrType, rOperand.getExpressionType()));
            } else {
              if (getSizeof(guess) != getSizeof(rOperand.getExpressionType())) {
                log(Level.WARNING, "Size of an old guess doesn't match with the current guess: " + lPtrVarName.getName());
              }
            }
          }
          Formula lPtrVar = makeVariable(lPtrVarName, ssa);

          return makeNondetAssignment(lPtrVar, rVar);
        }
      } else if (right instanceof CFieldReference) {
        // Weird Case
        log(Level.WARNING, "Strange MemoryLocation: " + right.toASTString());
      }

      // s = malloc()
      // has been handled already
      return bfmgr.makeBoolean(true);

    } else {
      // s = someFunction()
      // s = a + b
      // s = a[i]

      // no second level assignment necessary
      return bfmgr.makeBoolean(true);
    }
  }

  private BooleanFormula buildDirectReturnSecondLevelAssignment(CExpression leftId,
      Variable retVarName, String function, SSAMapBuilder ssa) {

    // include aliases if the left or right side may be a pointer a pointer
    // We only can write *l = *r if the types after dereference have some formula
    // type correspondence (e.g. boolean, real, bit vector etc.)
    // Types of unknown sizes, e.g. CProblemType, can't be represented and would
    // otherwise cause throwing exceptions
    Variable leftVar = scopedIfNecessary(leftId, ssa, function);
    if ((maybePointer(leftVar, ssa)
        || maybePointer(retVarName, ssa)) &&
        hasRepresentableDereference(leftId) &&
        hasRepresentableDereference(retVarName)) {
      // we assume that either the left or the right hand side is a pointer
      // so we add the equality: *l = *r
      Variable leftPtrMask = makePointerMask(leftVar, ssa);
      Formula lPtrVar =  makeVariable(leftPtrMask, ssa);
      Variable retPtrVarName = makePointerMask(retVarName, ssa);

      Formula retPtrVar = makeVariable(retPtrVarName, ssa);
      return makeNondetAssignment(lPtrVar, retPtrVar);

    } else {
      // we can assume, that no pointers are affected in this assignment
      return bfmgr.makeBoolean(true);
    }
  }

  private boolean isTooComplexExpression(CExpression c) {
    if (!c.accept(tooComplexVisitor)) {
      return false;
    }

    return IndirectionVisitor.getIndirectionLevel(c) > supportedIndirectionLevel;
  }
}

class ExpressionToFormulaVisitorPointers extends ExpressionToFormulaVisitor {

  @SuppressWarnings("hiding")
  private final PointerAliasHandling conv;

  public ExpressionToFormulaVisitorPointers(PointerAliasHandling pCtoFormulaConverter, CFAEdge pEdge, String pFunction,
      SSAMapBuilder pSsa, Constraints pCo) {
    super(pCtoFormulaConverter, pEdge, pFunction, pSsa, pCo);
    conv = pCtoFormulaConverter;
  }

  @Override
  public Formula visit(CUnaryExpression exp)
      throws UnrecognizedCCodeException {
    UnaryOperator op = exp.getOperator();

    switch (op) {
    case AMPER:
      CExpression operand = CtoFormulaConverter.removeCast(exp.getOperand());

      if (conv.isSupportedExpression(operand)) {
        return makeMemLocationVariable(operand, function);
      } else {
        return super.visit(exp);
      }

    case STAR:
      // *tmp or *(tmp->field) or *(s.a)
      if (conv.isSupportedExpression(exp)) {
        Variable fieldPtrMask  = conv.scopedIfNecessary(exp, ssa, function);
        Formula f = conv.makeVariable(fieldPtrMask, ssa);

        // *((type*)tmp) or *((type*)(tmp->field)) or *((type*)(s.a))
        if (exp.getOperand() instanceof CCastExpression) {
          CCastExpression cast = (CCastExpression) exp.getOperand();
          // Use fieldPtrMask.getType because of possible type guessing.
          f = conv.makeExtractOrConcatNondet(fieldPtrMask.getType(), dereferencedType(cast.getExpressionType()), f);
        }
        return f;
      }

      //$FALL-THROUGH$
    default:
      return super.visit(exp);
    }
  }

  /**
   * Returns a Formula representing the memory location of a given IdExpression.
   * Ensures that the location is unique and not 0.
   *
   * @param function The scope of the variable.
   */
  private Formula makeMemLocationVariable(CExpression exp, String function) {
    Variable v =
        conv.scopedIfNecessary(exp, ssa, function);
    Variable addressVariable = PointerAliasHandling.makeMemoryLocationVariable(v);

    // a variable address is always initialized, not 0 and cannot change
    if (ssa.getIndex(addressVariable.getName()) == CtoFormulaConverter.VARIABLE_UNSET) {
      Iterable<Map.Entry<String, CType>> oldMemoryLocations = PointerAliasHandling.getAllMemoryLocationsFromSsaMap(ssa);
      Formula newMemoryLocation = conv.makeConstant(addressVariable, ssa);

      // a variable address that is unknown is different from all previously known addresses
      for (Map.Entry<String, CType> memoryLocation : oldMemoryLocations) {
        Formula oldMemoryLocation = conv.makeConstant(memoryLocation.getKey(), memoryLocation.getValue(), ssa);
        BooleanFormula addressInequality = conv.bfmgr.not(conv.fmgr.makeEqual(newMemoryLocation, oldMemoryLocation));

        constraints.addConstraint(addressInequality);
      }

      // a variable address is not 0
      BooleanFormula notZero = conv.bfmgr.not(conv.fmgr.makeEqual(newMemoryLocation, conv.fmgr.makeNumber(conv.getFormulaTypeFromCType(addressVariable.getType()), 0)));
      constraints.addConstraint(notZero);
    }

    return conv.makeConstant(addressVariable, ssa);
  }
}


class StatementToFormulaVisitorPointers extends StatementToFormulaVisitor {

  @SuppressWarnings("hiding")
  private final PointerAliasHandling conv;

  public StatementToFormulaVisitorPointers(ExpressionToFormulaVisitorPointers pDelegate,
      PointerAliasHandling pConv) {
    super(pDelegate);
    conv = pConv;
  }

  @Override
  public Formula visit(CFunctionCallExpression fexp) throws UnrecognizedCCodeException {
    // handle malloc
    CExpression fn = fexp.getFunctionNameExpression();
    if (fn instanceof CIdExpression) {
      String fName = ((CIdExpression)fn).getName();

      if (conv.memoryAllocationFunctions.contains(fName)) {

        CType expType = fexp.getExpressionType();
        if (!(expType instanceof CPointerType)) {
          conv.log(Level.WARNING, "Memory allocation function ("+fName+") with invalid return type (" + expType +"). Missing includes or file not preprocessed?");
        }

        FormulaType<?> t = conv.getFormulaTypeFromCType(expType);

        // for now all parameters are ignored
        Set<Map.Entry<String, CType>> memoryLocations = PointerAliasHandling.getAllMemoryLocationsFromSsaMap(ssa);

        String mallocVarName = makeFreshMallocVariableName(expType);
        Formula mallocVar = conv.makeConstant(mallocVarName, expType, ssa);

        // we must distinguish between two cases:
        // either the result is 0 or it is different from all other memory locations
        // (m != 0) => for all memory locations n: m != n
        BooleanFormula ineq = conv.bfmgr.makeBoolean(true);
        for (Map.Entry<String, CType> ml : memoryLocations) {
          Formula n = conv.makeConstant(ml.getKey(), ml.getValue(), ssa);

          BooleanFormula notEqual = conv.bfmgr.not(conv.fmgr.makeEqual(n, mallocVar));
          ineq = conv.bfmgr.and(notEqual, ineq);
        }

        Formula nullFormula = conv.fmgr.makeNumber(t, 0);
        BooleanFormula notEqual = conv.bfmgr.not(conv.fmgr.makeEqual(mallocVar, nullFormula));
        BooleanFormula implication = conv.bfmgr.implication(notEqual, ineq);

        constraints.addConstraint(implication);
        return mallocVar;
      }
    }

    return super.visit(fexp);
  }


  @Override
  public BooleanFormula visit(CAssignment assignment)
      throws UnrecognizedCCodeException {
    CExpression left = CtoFormulaConverter.removeCast(assignment.getLeftHandSide());

    if (left instanceof CIdExpression) {
      // p = ...
      return handleDirectAssignment(assignment);

    } else if (left instanceof CUnaryExpression
        && ((CUnaryExpression) left).getOperator() == UnaryOperator.STAR) {
      // *p = ...
      return handleIndirectAssignment(assignment);

    } else if (conv.handleFieldAccess && left instanceof CFieldReference) {
      // p->t = ...
      // p.s = ...

      CFieldReference fieldRef = (CFieldReference)left;
      if (conv.isSupportedExpression(left)) {
        if (!isIndirectFieldReference(fieldRef)) {
          // p.s = ... which we handle quite similar to the p = ... case
          return handleDirectAssignment(assignment);
        } else {
          // p->s = ... which we handle quite similar to the *p = ... case
          return handleIndirectAssignment(assignment);
        }
      }
    }

    conv.warnToComplex(assignment);
    return super.visit(assignment);
  }

  /**
   * An indirect assignment does not change the value of the variable on the
   * left hand side. Instead it changes the value stored in the memory location
   * aliased on the left hand side.
   */
  private BooleanFormula handleIndirectAssignment(CAssignment pAssignment)
      throws UnrecognizedCCodeException {
    CExpression lExpr = CtoFormulaConverter.removeCast(pAssignment.getLeftHandSide());

    assert (lExpr instanceof CUnaryExpression || (lExpr instanceof CFieldReference && isIndirectFieldReference((CFieldReference)lExpr)))
        : "Unsupported leftHandSide in Indirect Assignment";


    CUnaryExpression leftSide;
    if (lExpr instanceof CUnaryExpression) {
      // the following expressions are supported by cil:
      // *p = a;
      // *p = 1;
      // *p = a | b; (or any other binary statement)
      // *p = function();
      // *s.t = ...
      leftSide = (CUnaryExpression) lExpr;
    } else {
      // p->s = ... is the same as (*p).s = ... which we see as *p = ... (because the bitvector was changed)
      CFieldReference l = (CFieldReference) lExpr;
      assert isIndirectFieldReference(l) : "No pointer-dereferencing in handleIndirectFieldAssignment";

      leftSide = (CUnaryExpression)getRealFieldOwner(l);
    }

    assert leftSide.getOperator() == UnaryOperator.STAR  : "Expected pointer dereferencing";

    if (!conv.isSupportedExpression(leftSide)) {
      // TODO: *(a + 2) = b
      // NOTE: We do not support multiple levels of indirection (**t)
      // *(p->t) = ...
      conv.warnToComplex(leftSide);
      return super.visit(pAssignment);
    }

    //SSAMapBuilder oldssa = new SSAMapBuilder(ssa.build());
    Variable lVarName = conv.scopedIfNecessary(leftSide.getOperand(), ssa, function);
    Variable lPtrVarName = PointerAliasHandling.makePointerMask(lVarName, ssa);
    Formula lVar = conv.makeVariable(lVarName, ssa);
    Formula lPtrVar = conv.makeVariable(lPtrVarName, ssa);

    // It could be that we have to fill the structure with nondet bits, because of a cast
    if (leftSide.getOperand() instanceof CCastExpression) {
      CCastExpression ptrCast = (CCastExpression)leftSide.getOperand();

      lPtrVar = conv.makeExtractOrConcatNondet(
          lPtrVarName.getType(), // Possible type guessing
          dereferencedType(ptrCast.getExpressionType()),
          lPtrVar);
    }

    CRightHandSide r = pAssignment.getRightHandSide();

    // NOTE: When doDeepUpdate is false rVarName will only be used for comparison in updateAllPointers
    // So this is OK.
    Variable rVarName = Variable.create(null, r.getExpressionType());
    Formula rPtrVar = null;
    boolean doDeepUpdate = false;
    // We need r to be one level lower than we support, as we need its pointer mask for a deep update
    if (r instanceof CExpression &&
        conv.isSupportedExpression((CExpression) r, 1) &&
        CtoFormulaConverter.hasRepresentableDereference((CExpression) r)) {
      rVarName = conv.scopedIfNecessary((CExpression) r, ssa, function);
      rPtrVar = conv.makeVariable(PointerAliasHandling.makePointerMask(rVarName, ssa), ssa);
      doDeepUpdate = true;
    }

    // assignment (first level) -- uses superclass
    Triple<Formula, Formula, BooleanFormula> assignmentFormulas = visitAssignment(pAssignment);
    Formula rightVariable = assignmentFormulas.getFirst();
    BooleanFormula assignments = assignmentFormulas.getThird();

    if (!(lExpr instanceof CUnaryExpression)) {
      // NOTE: rightVariable is only the changed field, set it to the complete bitvector
      rightVariable = conv.replaceField((CFieldReference) lExpr, lPtrVar, Optional.of(rightVariable));

      // We can't make a pointermask from the right side.
      doDeepUpdate = false;
      rVarName = Variable.create(null, r.getExpressionType()); // Only for comparison
    }

    // To ensure tracking we have to update all pointers which are equal to p
    // (now they point to the right side)
    // update all pointer variables (they might have a new value)
    // every variable aliased to the left hand side,
    // has its pointer set to the right hand side,
    // for all other pointer variables, the index is updated
    Set<Map.Entry<String, CType>> ptrVarNames = PointerAliasHandling.getAllPointerVariablesFromSsaMap(ssa);
    for (Map.Entry<String, CType> ptrVarName : ptrVarNames) {
      Variable varName = PointerAliasHandling.removePointerMaskVariable(ptrVarName.getKey(), ptrVarName.getValue());

      if (!varName.equals(lVarName) && !varName.equals(rVarName)) {
        Formula var = conv.makeVariable(varName, ssa);

        Formula oldPtrVar = conv.makeVariable(ptrVarName.getKey(), ptrVarName.getValue(), ssa);
        conv.makeFreshIndex(ptrVarName.getKey(), ptrVarName.getValue(), ssa);
        Formula newPtrVar = conv.makeVariable(ptrVarName.getKey(), ptrVarName.getValue(), ssa);
        BooleanFormula condition;
        if (isDereferenceType(ptrVarName.getValue())) {
          // Variable from a aliasing formula, they are always to small, so fill up with nondet bits to make a pointer.
          condition = conv.makeNondetAssignment(var, lVar);
        } else {
          assert conv.fmgr.getFormulaType(var) == conv.fmgr.getFormulaType(lVar)
              : "Make sure all memory variables are pointers! (Did you forget to process your file with cil first or are you missing some includes?)";
          condition = conv.fmgr.makeEqual(var, lVar);
        }
        //BooleanFormula condition = fmgr.makeEqual(var, leftVar);
        BooleanFormula equality = conv.makeNondetAssignment(newPtrVar, rightVariable);

        BooleanFormula indexUpdate = conv.fmgr.assignment(newPtrVar, oldPtrVar);

        BooleanFormula variableUpdate = conv.bfmgr.ifThenElse(condition, equality, indexUpdate);
        constraints.addConstraint(variableUpdate);
      }
    }

    // for all memory addresses also update the aliasing
    // if the left variable is an alias for an address,
    // then the left side is (deep) equal to the right side
    // otherwise update the variables
    Set<Map.Entry<String, CType>> memAddresses = PointerAliasHandling.getAllMemoryLocationsFromSsaMap(ssa);
    if (doDeepUpdate) {
      for (Map.Entry<String, CType> memAddress : memAddresses) {
        Variable varName = getVariableFromMemoryAddress(memAddress.getKey(), memAddress.getValue());
        //String varName = getVariableNameFromMemoryAddress(memAddress.getName());

        if (!varName.equals(lVarName) && CtoFormulaConverter.hasRepresentableDereference(varName)) {
          // we assume that cases like the following are illegal and do not occur
          // (gcc 4.6 gives an error):
          // p = &p;
          // *p = &a;

          Formula memAddressVar = conv.makeVariable(memAddress.getKey(), memAddress.getValue(), ssa);

          // *m_old
          Formula oldVar = conv.makeVariable(varName, ssa);
          Variable oldPtrVarName = PointerAliasHandling.makePointerMask(varName, ssa);
          // **m_old
          Formula oldPtrVar = conv.makeVariable(oldPtrVarName, ssa);

          conv.makeFreshIndex(varName.getName(), varName.getType(), ssa);

          // *m_new
          Formula newVar = conv.makeVariable(varName, ssa);
          Variable newPtrVarName = PointerAliasHandling.makePointerMask(varName, ssa);
          // **m_new
          Formula newPtrVar = conv.makeVariable(newPtrVarName, ssa);
          PointerAliasHandling.removeOldPointerVariablesFromSsaMap(newPtrVarName.getName(), ssa);

          // Let right be r and left *p and m the current memory-address
          // We create the formula of the comments above.

          // *m_new = r (they don't need to have the same types so use makeNondetAssignment)
          BooleanFormula varEquality = conv.makeNondetAssignment(newVar, rightVariable);
          // **m_new = *r
          BooleanFormula ptrVarEquality = conv.makeNondetAssignment(newPtrVar, rPtrVar);
          // *m_new = *m_old
          BooleanFormula varUpdate = conv.fmgr.assignment(newVar, oldVar);
          // **m_new = **m_old
          BooleanFormula ptrVarUpdate = conv.fmgr.assignment(newPtrVar, oldPtrVar);

          // p = m
          assert conv.fmgr.getFormulaType(lVar) == conv.fmgr.getFormulaType(memAddressVar)
              : "Make sure all memory variables are pointers! (Did you forget to process your file with cil first or are you missing some includes?)";
          BooleanFormula condition = conv.fmgr.makeEqual(lVar, memAddressVar);

          // **m_new = *r && *m_new = r
          BooleanFormula equality = conv.bfmgr.and(varEquality, ptrVarEquality);
          // *m_new = *m_old && **m_new = **m_old
          BooleanFormula update = conv.bfmgr.and(varUpdate, ptrVarUpdate);

          // if p = m then *m_new = r && **m_new = *r else *m_new = *m_old && **m_new = **m_old
          // means when the pointer equals to our current memory address we
          // know that this memory address contains the right side.
          // If not we know that this memory address was unchanged (same as before).
          BooleanFormula variableUpdate = conv.bfmgr.ifThenElse(condition, equality, update);
          constraints.addConstraint(variableUpdate);
        }
      }

    } else {
      // no deep update of pointers required
      Map<String, Formula> memberMaskMap = null;
      CType expType = simplifyType(leftSide.getExpressionType());
      if (conv.handleFieldAliasing) {
        // Read comment below.
        if (expType instanceof CCompositeType) {
          CCompositeType structType = (CCompositeType)expType;
          memberMaskMap = new HashMap<>();
          for (CCompositeTypeMemberDeclaration member : structType.getMembers()) {
            // TODO: check if we can omit member with a maybePointer call.
            // I think we can't because even when the current member was not used as pointer
            // the same member could be used as pointer on an other variable.
            if (conv.omitNonPointerInFieldAliasing && !CtoFormulaTypeUtils.isPointerType(member.getType())) {
              continue;
            }

            CFieldReference leftField =
                new CFieldReference(null, member.getType(), member.getName(), leftSide, false);

            Formula g_s = conv.accessField(leftField, rightVariable);
            // From g->s we search *(g->s)
            // Start with nondet bits
            CType maskType = dereferencedType(member.getType());
            int fieldMaskSize = conv.getSizeof(maskType) * conv.machineModel.getSizeofCharInBits();
            Formula content_of_g_s =
                conv.changeFormulaSize(0, fieldMaskSize, conv.efmgr.makeBitvector(0, 0));

            for (Map.Entry<String, CType> inner_ptrVarName : ptrVarNames) {
              Variable inner_varName = PointerAliasHandling.removePointerMaskVariable(inner_ptrVarName.getKey(), inner_ptrVarName.getValue());
              if (inner_varName.equals(lVarName) || inner_varName.equals(rVarName)) {
                continue;
              }

              Formula k = conv.makeVariable(inner_varName, ssa);
              BooleanFormula cond = conv.makeNondetAssignment(k, g_s);

              Formula found = conv.makeVariable(inner_ptrVarName.getKey(), inner_ptrVarName.getValue(), ssa);
              found = conv.changeFormulaSize(conv.efmgr.getLength((BitvectorFormula) found), fieldMaskSize, (BitvectorFormula) found);

              content_of_g_s = conv.bfmgr.ifThenElse(cond, found, content_of_g_s);
            }

            memberMaskMap.put(member.getName(), content_of_g_s);
          }
        }
      } else {
        warnFieldAliasing();
      }


      for (Map.Entry<String, CType> memAddress : memAddresses) {
        Variable varName = getVariableFromMemoryAddress(memAddress.getKey(), memAddress.getValue());

        if (varName.equals(lVarName) || !CtoFormulaConverter.hasRepresentableDereference(varName)) {
          continue;
        }
        // *m_old
        Formula oldVar = conv.makeVariable(varName, ssa);
        conv.makeFreshIndex(varName.getName(), varName.getType(), ssa);
        // *m_new
        Formula newVar = conv.makeVariable(varName, ssa);
        // **m_new
        Variable newPtrVarName = PointerAliasHandling.makePointerMask(varName, ssa);
        PointerAliasHandling.removeOldPointerVariablesFromSsaMap(newPtrVarName.getName(), ssa);

        // m_new
        Formula memAddressVar = conv.makeVariable(memAddress.getKey(), memAddress.getValue(), ssa);

        assert conv.fmgr.getFormulaType(lVar) == conv.fmgr.getFormulaType(memAddressVar)
            : "Make sure all memory variables are pointers! (Did you forget to process your file with cil first or are you missing some includes?)";
        // p = m_new
        BooleanFormula condition = conv.fmgr.makeEqual(lVar, memAddressVar);
        // *m_new = r
        BooleanFormula equality = conv.makeNondetAssignment(newVar, rightVariable);

        // *m_new = *m_old
        BooleanFormula update = conv.makeNondetAssignment(newVar, oldVar);

        if (memberMaskMap != null) {
          // When we found a address which was changed
          // and if this is a structure, we also have to update
          // the pointer masks of the fields.

          // The content_of_g_s formula doesn't have to be generated for all mem addresses
          // we can do this above for all

          // Currently we are in the statement *g = ...
          // Now say the condition is met and we found a p with p = m_new
          // This basically means we have a variable f with &f = m_new
          // For every changed field s we have to set the pointer mask
          // *(f.s) = *(g->s)
          // the *(g->s) part is the tricky one.
          // To get the content we basically have to search all pointers again.
          // If one pointer k is equal to g->s we can use *k
          // We build the following formula: Let {k_1,...,k_n} = maybepointer
          // if (k_1 = g->s) then *k_1 else
          //    if (k_2 = g->s) then *k_2 else
          //        ... else nondetbits

          // Ok now we have to do something if varname is actually
          CCompositeType structType = (CCompositeType)expType;

          // Note we only handle aliasing for memory addresses which make sense.
          // varname should be the same structure
          if (areEqual(varName.getType(), structType)) {
            for (CCompositeTypeMemberDeclaration member : structType.getMembers()) {
              Formula content_of_g_s = memberMaskMap.get(member.getName());
              if (content_of_g_s == null) {
                continue;
              }

              CFieldReference leftField =
                  new CFieldReference(null, member.getType(), member.getName(), leftSide, false);

              Variable f_s = conv.makeFieldVariable(varName, leftField, ssa);
              Variable content_of_f_s_Name = PointerAliasHandling.makePointerMask(f_s, ssa);

              Formula content_of_f_s_old = conv.makeVariable(content_of_f_s_Name, ssa);
              conv.makeFreshIndex(content_of_f_s_Name.getName(), content_of_f_s_Name.getType(), ssa);
              Formula content_of_f_s_new = conv.makeVariable(content_of_f_s_Name, ssa);
              equality =
                  conv.bfmgr.and(equality, conv.fmgr.makeEqual(content_of_g_s, content_of_f_s_new));
              update =
                  conv.bfmgr.and(update, conv.fmgr.makeEqual(content_of_f_s_old, content_of_f_s_new));
            }
          }
        }

        // if p = m then *m_new = r else *m_new = *m_old
        // means when the pointer equals to our current memory address we
        // know that this memory address contains the right side.
        // If not we know that this memory address was unchanged (same as before).
        BooleanFormula variableUpdate = conv.bfmgr.ifThenElse(condition, equality, update);
        constraints.addConstraint(variableUpdate);
      }
    }

    return assignments;
  }



  private Variable getVariableFromMemoryAddress(String pMemAddress, CType type) {
    return Variable.create(
        getVariableNameFromMemoryAddress(pMemAddress),
        dereferencedType(type));
  }

  /** A direct assignment changes the value of the variable on the left side. */
  private BooleanFormula handleDirectAssignment(CAssignment assignment)
      throws UnrecognizedCCodeException {
    CExpression lExpr = assignment.getLeftHandSide();
    assert (lExpr instanceof CIdExpression
        || (lExpr instanceof CFieldReference && !isIndirectFieldReference((CFieldReference)lExpr)))
        : "We currently can't handle too complex lefthandside-Expressions";

    CRightHandSide right = CtoFormulaConverter.removeCast(assignment.getRightHandSide());

    Variable leftVarName = conv.scopedIfNecessary(lExpr, ssa, function);

    // assignment (first level)
    // Just the assignment formula p = t
    Triple<Formula, Formula, BooleanFormula> assignmentFormulas = visitAssignment(assignment);
    Formula leftVariable = assignmentFormulas.getSecond();
    BooleanFormula assignmentFormula = assignmentFormulas.getThird();

    // assignment (second level) *p = *t if necessary
    // if right hand side is a function call, there is no aliasing
    if (right instanceof CExpression) {
      Variable lVarName = conv.scopedIfNecessary(lExpr, ssa, function);
      BooleanFormula secondLevelFormula = conv.buildDirectSecondLevelAssignment(
          lVarName, (CExpression)right, function, constraints, ssa);

      assignmentFormula = conv.bfmgr.and(assignmentFormula, secondLevelFormula);
    }

    updatePointerAliasedTo(leftVarName, leftVariable);


    if (conv.handleFieldAliasing) {
      if (lExpr instanceof CIdExpression) {
        // If the left side is a simple CIdExpression "l = ..." than we have to see if
        // l is a structure and handle pointer aliasing for all fields.

        CType leftType = lExpr.getExpressionType().getCanonicalType();
        if (leftType instanceof CCompositeType && right instanceof CExpression) {
          // There are 3 cases:
          // - the right side is of the form *r
          //     for every member we have to emit
          //     ∀p ∈ maybePointer: (p = r->t) ⇒ (l.t = p ∧ *(l.t) = *p)
          // - the right side is of the form r
          //     for every member we have to emit *(s.t) = *(r.t)
          // - the right side is of the form &r, this should not happen
          //     for every member we have to emit *(s.t) = *((&r).t)
          // -> Exactly this does the buildDirectSecondLevelAssignment method for us
          // Note: No 2nd level assignment for statements like t = call();

          if (!leftType.equals(right.getExpressionType().getCanonicalType())) {
            throw new UnrecognizedCCodeException("Struct assignment with incompatible types", edge, assignment);
          }

          CCompositeType structureType = (CCompositeType) leftType;
          for (CCompositeTypeMemberDeclaration member : structureType.getMembers()) {
            // We pretend to have a assignment of the form
            // l.t = (r).t
            CFieldReference leftField =
                new CFieldReference(null, member.getType(), member.getName(), lExpr, false);

            CFieldReference rightField =
                new CFieldReference(null, member.getType(), member.getName(), (CExpression)right, false);
            Variable leftFieldVar = conv.scopedIfNecessary(leftField, ssa, function);
            BooleanFormula secondLevelFormulaForMember = conv.buildDirectSecondLevelAssignment(
                leftFieldVar, rightField, function, constraints, ssa);
            assignmentFormula = conv.bfmgr.and(assignmentFormula, secondLevelFormulaForMember);

            // Also update all pointers aliased to the field.
            Formula leftFieldFormula = conv.makeVariable(leftFieldVar, ssa);
            updatePointerAliasedTo(leftFieldVar, leftFieldFormula);
          }
        }
      } else {
        CFieldReference fexp = (CFieldReference)lExpr;
        assert !isIndirectFieldReference(fexp)
          : "direct assignment but indirect expression!";
        // If we have a CFieldReference on the left "l.t = ..." than we only have to update
        // the single field.
        // This is what we have done with the above buildDirectSecondLevelAssignment-Call.
        // And from the imaginary right side will exist no pointer so there is nothing to do.

        // Well we have to update all pointers aliased to the structure.
        // Because above we updated the field.
        CExpression owner = getRealFieldOwner(fexp);
        Variable structVar = conv.scopedIfNecessary(owner, ssa, function);
        Formula structFormula = conv.makeVariable(structVar, ssa);
        updatePointerAliasedTo(structVar, structFormula);
      }
    } else {
      warnFieldAliasing();
    }

    return assignmentFormula;
  }

  private void warnFieldAliasing() {
    if (conv.handleFieldAccess) {
      conv.log(Level.WARNING, "You should enable handleFieldAliasing if possible.");
    }
  }

  private void updatePointerAliasedTo(Variable leftVarName, Formula leftVariable) {
    // updates
    if (isKnownMemoryLocation(leftVarName)) {
      Variable leftMemLocationName = PointerAliasHandling.makeMemoryLocationVariable(leftVarName);
      Formula leftMemLocation = conv.makeConstant(leftMemLocationName, ssa);

      // update all pointers:
      // if a pointer is aliased to the assigned variable,
      // update that pointer to reflect the new aliasing,
      // otherwise only update the index
      for (Map.Entry<String, CType> ptrVarName : PointerAliasHandling.getAllPointerVariablesFromSsaMap(ssa)) {
        Variable varName = PointerAliasHandling.removePointerMaskVariable(ptrVarName.getKey(), ptrVarName.getValue());
        if (!varName.equals(leftVarName)) {
          Formula var = conv.makeVariable(varName, ssa);
          Formula oldPtrVar = conv.makeVariable(ptrVarName.getKey(), ptrVarName.getValue(), ssa);
          conv.makeFreshIndex(ptrVarName.getKey(), ptrVarName.getValue(), ssa);
          Formula newPtrVar = conv.makeVariable(ptrVarName.getKey(), ptrVarName.getValue(), ssa);
          BooleanFormula condition;
          if (isDereferenceType(ptrVarName.getValue())) {
            condition = conv.makeNondetAssignment(var, leftMemLocation);
          } else {
            assert conv.fmgr.getFormulaType(var) == conv.fmgr.getFormulaType(leftMemLocation)
                : "Make sure all memory variables are pointers! (Did you forget to process your file with cil first or are you missing some includes?)";
            condition = conv.fmgr.makeEqual(var, leftMemLocation);
          }
          // leftVariable can be anything
          BooleanFormula equivalence = conv.makeNondetAssignment(newPtrVar, leftVariable);
          BooleanFormula update = conv.fmgr.assignment(newPtrVar, oldPtrVar);

          BooleanFormula constraint = conv.bfmgr.ifThenElse(condition, equivalence, update);
          constraints.addConstraint(constraint);
        }
      }
    }
  }

  /** Returns whether the address of a given variable has been used before. */
  private boolean isKnownMemoryLocation(Variable varName) {
    assert varName.getName() != null;
    Variable memVarName = PointerAliasHandling.makeMemoryLocationVariable(varName);
    return ssa.allVariables().contains(memVarName.getName());
  }

  /** Returns a new variable name for every malloc call.
   * @param pT */
  private String makeFreshMallocVariableName(CType pT) {
    int idx = ssa.getIndex(PointerAliasHandling.MALLOC_COUNTER_VARIABLE_NAME);

    if (idx == CtoFormulaConverter.VARIABLE_UNSET) {
      idx = CtoFormulaConverter.VARIABLE_UNINITIALIZED;
    }

    ssa.setIndex(PointerAliasHandling.MALLOC_COUNTER_VARIABLE_NAME, pT, idx + 1);
    return PointerAliasHandling.MALLOC_VARIABLE_PREFIX + idx;
  }

  /** Returns the variable name of a memory address variable */
  private String getVariableNameFromMemoryAddress(String memoryAddress) {
    assert (memoryAddress.startsWith(PointerAliasHandling.MEMORY_ADDRESS_VARIABLE_PREFIX));

    return memoryAddress.substring(PointerAliasHandling.MEMORY_ADDRESS_VARIABLE_PREFIX.length());
  }
}


class LvalueVisitorPointers extends LvalueVisitor {

  @SuppressWarnings("hiding")
  private final PointerAliasHandling conv;

  public LvalueVisitorPointers(PointerAliasHandling pCtoFormulaConverter, CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
    super(pCtoFormulaConverter, pEdge, pFunction, pSsa, pCo);
    conv = pCtoFormulaConverter;
  }

  @Override
  public Formula visit(CCastExpression e) throws UnrecognizedCCodeException {
    Formula inner = e.getOperand().accept(this);
    return conv.makeCast(e, inner);
  }


  @Override
  public Formula visit(CUnaryExpression pE) throws UnrecognizedCCodeException {
    if (pE.getOperator() == UnaryOperator.STAR) {
      // When the expression is supported we can create a Variable.
      if (conv.isSupportedExpression(pE)) {
        // *a = ...
        Variable ptrVarName = conv.scopedIfNecessary(pE, ssa, function);
        conv.makeFreshIndex(ptrVarName.getName(), ptrVarName.getType(), ssa);
        Formula f = conv.makeVariable(ptrVarName, ssa);

        // *((int*) a) = ...
        if (pE.getOperand() instanceof CCastExpression) {
          CCastExpression cast = (CCastExpression) pE.getOperand();
          // Use ptrVarName.getType() because of possible type guessing
          f = conv.makeExtractOrConcatNondet(ptrVarName.getType(), dereferencedType(cast.getExpressionType()), f);
        }
        return f;
      } else {
        // apparently valid cil output:
        // *(&s.f) = ...
        // *(s->f) = ...
        // *(a+b) = ...

        return giveUpAndJustMakeVariable(pE);
      }
    } else {
      // &f = ... which doesn't make much sense.
      conv.log(Level.WARNING, "Strange addressof operator on the left side:" + pE.toString());
      return super.visit(pE);
    }
  }
}