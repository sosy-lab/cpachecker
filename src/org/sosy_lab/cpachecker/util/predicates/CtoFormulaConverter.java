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
package org.sosy_lab.cpachecker.util.predicates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.DefaultExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.Defaults;
import org.sosy_lab.cpachecker.cfa.ast.ForwardingExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTArrayTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCompositeTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTElaboratedTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNamedTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTPointerTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IComplexType;
import org.sosy_lab.cpachecker.cfa.ast.IType;
import org.sosy_lab.cpachecker.cfa.ast.ITypedef;
import org.sosy_lab.cpachecker.cfa.ast.RightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.StatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.StorageClass;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.MachineModel;
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Class containing all the code that converts C code into a formula.
 */
@Options(prefix="cpa.predicate")
public class CtoFormulaConverter {

  @Option(description="add special information to formulas about non-deterministic functions")
  protected boolean useNondetFlags = false;

  @Option(description="initialize all variables to 0 when they are declared")
  private boolean initAllVars = false;

  // if true, handle lvalues as *x, &x, s.x, etc. using UIFs. If false, just
  // use variables
  @Option(name="mathsat.lvalsAsUIFs",
      description="use uninterpreted functions for *, & and array access")
  private boolean lvalsAsUif = false;

  @Option(description="list of functions that should be considered as giving "
    + "a non-deterministic return value\n Only predicate analysis honors this option. "
    + "If you specify this option, the default values are not added automatically "
    + "to the list, so you need to specify them explicitly if you need them. "
    + "Mentioning a function in this list has only an effect, if it is an "
    + "'external function', i.e., no source is given in the code for this function.")
  private Set<String> nondetFunctions = ImmutableSet.of(
      "malloc", "__kmalloc", "kzalloc",
      "sscanf",
      "int_nondet", "nondet_int", "random", "__VERIFIER_nondet_int", "__VERIFIER_nondet_pointer",
      "__VERIFIER_nondet_short", "__VERIFIER_nondet_char", "__VERIFIER_nondet_float"
      );

  @Option(description = "the machine model used for functions sizeof and alignof")
  private MachineModel machineModel = MachineModel.LINUX32;

  @Option(description = "handle Pointers")
  private boolean handlePointerAliasing = true;

  @Option(description = "list of functions that provide new memory on the heap."
    + " This is only used, when handling of pointers is enabled.")
  private Set<String> memoryAllocationFunctions = ImmutableSet.of(
      "malloc", "__kmalloc", "kzalloc"
      );

  // list of functions that are pure (no side-effects)
  private static final Set<String> PURE_EXTERNAL_FUNCTIONS
      = ImmutableSet.of("__assert_fail", "free", "kfree",
          "fprintf", "printf", "puts", "printk", "sprintf", "swprintf",
          "strcasecmp", "strchr", "strcmp", "strlen", "strncmp", "strrchr", "strstr"
          );

  // set of functions that may not appear in the source code
  // the value of the map entry is the explanation for the user
  private static final Map<String, String> UNSUPPORTED_FUNCTIONS
      = ImmutableMap.of("pthread_create", "threads");

  //names for special variables needed to deal with functions
  private static final String VAR_RETURN_NAME = "__retval__";
  private static final String OP_ADDRESSOF_NAME = "__ptrAmp__";
  private static final String OP_STAR_NAME = "__ptrStar__";
  private static final String OP_ARRAY_SUBSCRIPT = "__array__";
  public static final String NONDET_VARIABLE = "__nondet__";
  public static final String NONDET_FLAG_VARIABLE = NONDET_VARIABLE + "flag__";

  /** The prefix used for variables representing memory locations. */
  private static final String MEMORY_ADDRESS_VARIABLE_PREFIX = "&";

  /**
   * The prefix used for memory locations derived from malloc calls.
   * (Must start with {@link MEMORY_ADDRESS_VARIABLE_PREFIX}.)
   */
  private static final String MALLOC_VARIABLE_PREFIX =
      MEMORY_ADDRESS_VARIABLE_PREFIX + "#";

  /** The variable name that's used to store the malloc counter in the SSAMap. */
  private static final String MALLOC_COUNTER_VARIABLE_NAME = "#malloc";

  private final Set<String> printedWarnings = new HashSet<String>();

  private final Map<String, Formula> stringLitToFormula = new HashMap<String, Formula>();
  private int nextStringLitIndex = 0;

  protected final ExtendedFormulaManager fmgr;
  protected final LogManager logger;

  private static final int                 VARIABLE_UNSET          = -1;
  private static final int                 VARIABLE_UNINITIALIZED  = 2;

  public CtoFormulaConverter(Configuration config, ExtendedFormulaManager fmgr, LogManager logger) throws InvalidConfigurationException {
    config.inject(this, CtoFormulaConverter.class);

    this.fmgr = fmgr;
    this.logger = logger;
  }

  private void warnUnsafeVar(IASTExpression exp) {
    logDebug("Unhandled expression treated as free variable", exp);
  }

  private void warnUnsafeAssignment() {
    log(Level.WARNING, "Program contains array, pointer, or field access; analysis is imprecise in case of aliasing.");
  }

  private String getLogMessage(String msg, IASTNode astNode) {
    return "Line " + astNode.getFileLocation().getStartingLineNumber()
            + ": " + msg
            + ": " + astNode.toASTString();
  }

  private String getLogMessage(String msg, CFAEdge edge) {
    return "Line " + edge.getLineNumber()
            + ": " + msg
            + ": " + edge.getRawStatement();
  }

  private void logDebug(String msg, IASTNode astNode) {
    if (logger.wouldBeLogged(Level.ALL)) {
      logger.log(Level.ALL, getLogMessage(msg, astNode));
    }
  }

  private void logDebug(String msg, CFAEdge edge) {
    if (logger.wouldBeLogged(Level.ALL)) {
      logger.log(Level.ALL, getLogMessage(msg, edge));
    }
  }

  private void log(Level level, String msg) {
    if (logger.wouldBeLogged(level)
        && printedWarnings.add(msg)) {

      logger.log(level, msg);
    }
  }

  /** Looks up the variable name in the current namespace. */
  private String scopedIfNecessary(IASTIdExpression var, String function) {
    IASTSimpleDeclaration decl = var.getDeclaration();
    boolean isGlobal = false;
    if (decl instanceof IASTDeclaration) {
      isGlobal = ((IASTDeclaration)decl).isGlobal();
    }

    if (isGlobal) {
      return var.getName();
    } else {
      return scoped(var.getName(), function);
    }
  }

  // prefixes function to variable name
  // Call only if you are sure you have a local variable!
  private static String scoped(String var, String function) {
    return function + "::" + var;
  }

  private static String exprToVarName(IASTExpression e) {
    return e.toASTString().replaceAll("[ \n\t]", "");
  }

  private String getTypeName(final IType tp) {

    if (tp instanceof IASTPointerTypeSpecifier) {
      return getTypeName(((IASTPointerTypeSpecifier)tp).getType());

    } else if (tp instanceof ITypedef) {
      return getTypeName(((ITypedef)tp).getType());

    } else if (tp instanceof IComplexType){
      return ((IComplexType)tp).getName();

    } else {
      throw new AssertionError("wrong type");
    }
  }

  /**
   * Produces a fresh new SSA index for an assignment
   * and updates the SSA map.
   */
  private int makeFreshIndex(String varName, SSAMapBuilder ssa) {
    int idx = ssa.getIndex(varName);
    if (idx > 0) {
      idx = idx+1;
    } else {
      idx = VARIABLE_UNINITIALIZED; // AG - IMPORTANT!!! We must start from 2 and
      // not from 1, because this is an assignment,
      // so the SSA index must be fresh.
    }
    ssa.setIndex(varName, idx);
    return idx;
  }

  private int getIndex(String var, SSAMapBuilder ssa) {
    int idx = ssa.getIndex(var);
    if (idx <= 0) {
      logger.log(Level.ALL, "WARNING: Auto-instantiating variable:", var);
      idx = 1;
      ssa.setIndex(var, idx);
    }
    return idx;
  }

  /**
   * Produces a fresh new SSA index for the left-hand side of an assignment
   * and updates the SSA map.
   */
  private int makeLvalIndex(String varName, FormulaList args, SSAMapBuilder ssa) {
    int idx = ssa.getIndex(varName, args);
    if (idx > 0) {
      idx = idx+1;
    } else {
      idx = VARIABLE_UNINITIALIZED; // AG - IMPORTANT!!! We must start from 2 and
      // not from 1, because this is an assignment,
      // so the SSA index must be fresh. If we use 1
      // here, we will have troubles later when
      // shifting indices
    }
    ssa.setIndex(varName, args, idx);
    return idx;
  }

  /**
   * Create a formula for a given variable, which is assumed to be constant.
   * This method does not handle scoping!
   */
  private Formula makeConstant(String var, SSAMapBuilder ssa) {
    // TODO better use variables without index (this piece of code prevents
    // SSAMapBuilder from checking for strict monotony)
    int idx = ssa.getIndex(var);
    assert idx <= 1 : var + " is assumed to be constant there was an assignment to it";
    if (idx != 1) {
      ssa.setIndex(var, 1); // set index so that predicates will be instantiated correctly
    }
    return fmgr.makeVariable(var, 1);
  }

  /**
   * Create a formula for a given variable.
   * This method does not handle scoping and the NON_DET_VARIABLE!
   */
  private Formula makeVariable(String varName, SSAMapBuilder ssa) {
    int idx = getIndex(varName, ssa);
    return fmgr.makeVariable(varName, idx);
  }

  /**
   * Create a formula for a given variable with a fresh index for the left-hand
   * side of an assignment.
   * This method does not handle scoping and the NON_DET_VARIABLE!
   */
  private Formula makeFreshVariable(String varName, SSAMapBuilder ssa) {
    int idx = makeFreshIndex(varName, ssa);
    return fmgr.makeVariable(varName, idx);
  }

  /** Returns the pointer variable belonging to a given IdExpression */
  private Formula makePointerVariable(IASTIdExpression expr, String function,
      SSAMapBuilder ssa) {
    String pVarName = makePointerVariableName(expr, function, ssa);
    return makeVariable(pVarName, ssa);
  }

  /** Takes a (scoped) variable name and returns the pointer variable name. */
  private static String makePointerMask(String scopedId, SSAMapBuilder ssa) {
    return "*<" + scopedId + "," + ssa.getIndex(scopedId) + ">";
  }

  /**
   * Takes a pointer variable name and returns the name of the associated
   * variable.
   */
  private static String removePointerMask(String pointerVariable) {
    assert (isPointerVariable(pointerVariable));

    return pointerVariable.substring(2, pointerVariable.indexOf(','));
  }

  /** Returns the pointer variable name corresponding to a given IdExpression */
  private String makePointerVariableName(IASTIdExpression expr,
      String function, SSAMapBuilder ssa) {

    String scopedId = scopedIfNecessary(expr, function);
    return makePointerMask(scopedId, ssa);
  }

  private String makeMemoryLocationVariableName(String varName) {
    return MEMORY_ADDRESS_VARIABLE_PREFIX + varName;
  }

  // name has to be scoped already
  private Formula makeAssignment(String varName,
          Formula rightHandSide, SSAMapBuilder ssa) {

    Formula lhs = makeFreshVariable(varName, ssa);
    return fmgr.makeAssignment(lhs, rightHandSide);
  }

//  @Override
  public PathFormula makeAnd(PathFormula oldFormula, CFAEdge edge)
      throws CPATransferException {
    // this is where the "meat" is... We have to parse the statement
    // attached to the edge, and convert it to the appropriate formula

    if (edge.getEdgeType() == CFAEdgeType.BlankEdge) {

      // in this case there's absolutely nothing to do, so take a shortcut
      return oldFormula;
    }

    String function = (edge.getPredecessor() != null)
                          ? edge.getPredecessor().getFunctionName() : null;

    SSAMapBuilder ssa = oldFormula.getSsa().builder();
    Constraints constraints = new Constraints();

    Formula edgeFormula;
    switch (edge.getEdgeType()) {
    case StatementEdge: {
      StatementEdge statementEdge = (StatementEdge) edge;
      StatementToFormulaVisitor v;
      if (handlePointerAliasing) {
        v = new StatementToFormulaVisitorPointers(function, ssa, constraints, edge);
      } else {
        v = new StatementToFormulaVisitor(function, ssa, constraints, edge);
      }
      edgeFormula = statementEdge.getStatement().accept(v);
      break;
    }

    case ReturnStatementEdge: {
      ReturnStatementEdge returnEdge = (ReturnStatementEdge)edge;
      edgeFormula = makeReturn(returnEdge.getExpression(), returnEdge, function, ssa, constraints);
      break;
    }

    case DeclarationEdge: {
      DeclarationEdge d = (DeclarationEdge)edge;
      edgeFormula = makeDeclaration(d.getDeclSpecifier(), d.isGlobal(), d, function, ssa, constraints, edge);
      break;
    }

    case AssumeEdge: {
      edgeFormula = makeAssume((AssumeEdge)edge, function, ssa, constraints);
      break;
    }

    case BlankEdge: {
      assert false : "Handled above";
      edgeFormula = fmgr.makeTrue();
      break;
    }

    case FunctionCallEdge: {
      edgeFormula = makeFunctionCall((FunctionCallEdge)edge, function, ssa, constraints);
      break;
    }

    case FunctionReturnEdge: {
      // get the expression from the summary edge
      CallToReturnEdge ce = ((FunctionReturnEdge)edge).getSummaryEdge();
      edgeFormula = makeExitFunction(ce, function, ssa, constraints);
      break;
    }

    default:
      throw new UnrecognizedCFAEdgeException(edge);
    }

    if (useNondetFlags) {
      int lNondetIndex = ssa.getIndex(NONDET_VARIABLE);
      int lFlagIndex = ssa.getIndex(NONDET_FLAG_VARIABLE);

      if (lNondetIndex != lFlagIndex) {
        if (lFlagIndex < 0) {
          lFlagIndex = 1; // ssa indices start with 2, so next flag that is generated also uses index 2
        }

        for (int lIndex = lFlagIndex + 1; lIndex <= lNondetIndex; lIndex++) {
          Formula lAssignment = fmgr.makeAssignment(fmgr.makeVariable(NONDET_FLAG_VARIABLE, lIndex), fmgr.makeNumber(1));
          edgeFormula = fmgr.makeAnd(edgeFormula, lAssignment);
        }

        // update ssa index of nondet flag
        ssa.setIndex(NONDET_FLAG_VARIABLE, lNondetIndex);
      }
    }

    edgeFormula = fmgr.makeAnd(edgeFormula, constraints.get());

    SSAMap newSsa = ssa.build();
    if (edgeFormula.isTrue() && (newSsa == oldFormula.getSsa())) {
      // formula is just "true" and SSAMap is identical
      // i.e. no writes to SSAMap, no branching and length should stay the same
      return oldFormula;
    }

    Formula newFormula = fmgr.makeAnd(oldFormula.getFormula(), edgeFormula);
    int newLength = oldFormula.getLength() + 1;
    return new PathFormula(newFormula, newSsa, newLength);
  }

  private Formula makeDeclaration(IType spec, boolean isGlobal,
      DeclarationEdge edge, String function, SSAMapBuilder ssa,
      Constraints constraints, CFAEdge cfaEdge) throws CPATransferException {

    if (spec instanceof IASTFunctionTypeSpecifier) {
      return fmgr.makeTrue();

    } else if (spec instanceof IASTCompositeTypeSpecifier) {
      // this is the declaration of a struct, just ignore it...
      logDebug("Ignoring declaration", edge);
      return fmgr.makeTrue();

    } else if (spec instanceof IASTSimpleDeclSpecifier ||
               spec instanceof IASTEnumerationSpecifier ||
               spec instanceof IASTElaboratedTypeSpecifier ||
               spec instanceof IASTNamedTypeSpecifier ||
               spec instanceof IASTArrayTypeSpecifier ||
               spec instanceof IASTPointerTypeSpecifier) {

      if (edge.getStorageClass() == StorageClass.TYPEDEF) {
        logDebug("Ignoring typedef", edge);
        return fmgr.makeTrue();
      }

      // ignore type prototypes here
      if (edge.getName() != null) {

        String varNameWithoutFunction = edge.getName();
        String varName;
        if (isGlobal) {
          varName = varNameWithoutFunction;
        } else {
          varName = scoped(varNameWithoutFunction, function);
        }

        // TODO get the type of the variable, and act accordingly

        // if the var is unsigned, add the constraint that it should
        // be > 0
        //    if (((IASTSimpleDeclSpecifier)spec).isUnsigned()) {
        //    long z = mathsat.api.msat_make_number(msatEnv, "0");
        //    long mvar = buildMsatVariable(var, idx);
        //    long t = mathsat.api.msat_make_gt(msatEnv, mvar, z);
        //    t = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), t);
        //    m1 = new MathsatFormula(t);
        //    }

        // just increment index of variable in SSAMap
        // (a declaration contains an implicit assignment, even without initializer)
        // In case of an existing initializer, we increment the index twice
        // (here and below) so that the index 2 only occurs for uninitialized variables.
        makeFreshIndex(varName, ssa);

        // if there is an initializer associated to this variable,
        // take it into account
        IASTInitializer initializer = edge.getInitializer();
        IASTExpression init = null;

        if (initializer == null) {
          if (initAllVars) {
            // auto-initialize variables to zero
            logDebug("AUTO-INITIALIZING VAR: ", edge);
            init = Defaults.forType(spec, null);
          }

        } else if (initializer instanceof IASTInitializerExpression) {
          init = ((IASTInitializerExpression)initializer).getExpression();

        } else {
          logDebug("Ignoring unsupported initializer", initializer);
        }

        if (init != null) {
          // initializer value present

          Formula minit = buildTerm(init, cfaEdge, function, ssa, constraints);
          Formula assignments = makeAssignment(varName, minit, ssa);

          if (handlePointerAliasing) {
            // we need to add the pointer alias
            Formula pAssign = buildDirectSecondLevelAssignment(spec, varName,
                removeCast(init), function, constraints, ssa);
            assignments = fmgr.makeAnd(pAssign, assignments);

            // no need to add pointer updates:
            // the left hand variable cannot yet be aliased
            // because it is newly created
          }

          return assignments;
        }
      }
      return fmgr.makeTrue();

    } else {
      throw new UnrecognizedCFAEdgeException(edge);
    }
  }

  private Formula makeExitFunction(CallToReturnEdge ce, String function,
      SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {

    IASTFunctionCall retExp = ce.getExpression();
    if (retExp instanceof IASTFunctionCallStatement) {
      // this should be a void return, just do nothing...
      return fmgr.makeTrue();

    } else if (retExp instanceof IASTFunctionCallAssignmentStatement) {
      IASTFunctionCallAssignmentStatement exp = (IASTFunctionCallAssignmentStatement)retExp;

      String retVarName = scoped(VAR_RETURN_NAME, function);
      Formula retVar = makeVariable(retVarName, ssa);
      IASTExpression e = exp.getLeftHandSide();

      function = ce.getSuccessor().getFunctionName();
      Formula outvarFormula = buildLvalueTerm(e, ce, function, ssa, constraints);
      Formula assignments = fmgr.makeAssignment(outvarFormula, retVar);

      if (handlePointerAliasing) {
        IASTExpression left = removeCast(e);
        if (left instanceof IASTIdExpression) {
          Formula ptrAssignment = buildDirectReturnSecondLevelAssignment(
              (IASTIdExpression) left, retVarName, function, ssa);
          assignments = fmgr.makeAnd(assignments, ptrAssignment);
        }
      }

       return assignments;
    } else {
      throw new UnrecognizedCCodeException("Unknown function exit expression", ce, retExp.asStatement());
    }
  }

  private Formula makeFunctionCall(FunctionCallEdge edge,
      String callerFunction, SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {

    List<IASTExpression> actualParams = edge.getArguments();

    FunctionDefinitionNode fn = edge.getSuccessor();
    List<IASTParameterDeclaration> formalParams = fn.getFunctionParameters();

    String calledFunction = fn.getFunctionName();

    if (fn.getFunctionDefinition().getDeclSpecifier().takesVarArgs()) {
      if (formalParams.size() > actualParams.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
            "not match function definition", edge);
      }

      logger.log(Level.WARNING, "Ignoring parameters passed as varargs to function", calledFunction,
                                "in line", edge.getLineNumber());

    } else {
      if (formalParams.size() != actualParams.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does " +
            "not match function definition", edge);
      }
    }

    int i = 0;
    Formula result = fmgr.makeTrue();
    for (IASTParameterDeclaration formalParam : formalParams) {
      // get formal parameter name
      String formalParamName = formalParam.getName();
      assert (!formalParamName.isEmpty()) : edge;

      if (formalParam.getDeclSpecifier() instanceof IASTPointerTypeSpecifier) {
        warnUnsafeAssignment();
        logDebug("Ignoring the semantics of pointer for parameter "
            + formalParamName, fn.getFunctionDefinition());
      }

      // get value of actual parameter
      Formula actualParam = buildTerm(actualParams.get(i++), edge, callerFunction, ssa, constraints);

      Formula eq = makeAssignment(scoped(formalParamName, calledFunction), actualParam, ssa);

      result = fmgr.makeAnd(result, eq);
    }

    return result;
  }

  private Formula makeReturn(IASTExpression rightExp, ReturnStatementEdge edge, String function,
      SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {
    if (rightExp == null) {
      // this is a return from a void function, do nothing
      return fmgr.makeTrue();
    } else {

      // we have to save the information about the return value,
      // so that we can use it later on, if it is assigned to
      // a variable. We create a function::__retval__ variable
      // that will hold the return value
      Formula retval = buildTerm(rightExp, edge, function, ssa, constraints);
      String retVarName = scoped(VAR_RETURN_NAME, function);
      Formula assignments = makeAssignment(retVarName, retval, ssa);

      if (handlePointerAliasing) {
        // if the value to be returned may be a pointer, act accordingly
        Formula rightAssignment = buildDirectSecondLevelAssignment(null,
            retVarName, rightExp, function, constraints, ssa);
        assignments = fmgr.makeAnd(assignments, rightAssignment);
      }

      return assignments;
    }
  }

  private Formula makeAssume(AssumeEdge assume, String function,
      SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {

    return makePredicate(assume.getExpression(), assume.getTruthAssumption(),
        assume, function, ssa, constraints);
  }

  private Formula buildTerm(IASTExpression exp, CFAEdge edge, String function,
      SSAMapBuilder ssa, Constraints constraints) throws UnrecognizedCCodeException {
    return toNumericFormula(exp.accept(getExpressionVisitor(edge, function, ssa, constraints)));
  }

  private Formula buildLvalueTerm(IASTExpression exp, CFAEdge edge, String function,
      SSAMapBuilder ssa, Constraints constraints) throws UnrecognizedCCodeException {
    return exp.accept(getLvalueVisitor(edge, function, ssa, constraints));
  }

  private Formula buildDirectReturnSecondLevelAssignment(IASTIdExpression leftId,
      String retVarName, String function, SSAMapBuilder ssa) {

    // include aliases if the left or right side may be a pointer a pointer
    if (maybePointer(leftId, function, ssa)
        || maybePointer((IType) null, retVarName, ssa)) {
      // we assume that either the left or the right hand side is a pointer
      // so we add the equality: *l = *r
      Formula lPVar = makePointerVariable(leftId, function, ssa);
      String retPVarName = makePointerMask(retVarName, ssa);
      Formula retPVar = makeVariable(retPVarName, ssa);
      return fmgr.makeAssignment(lPVar, retPVar);

    } else {
      // we can assume, that no pointers are affected in this assignment
      return fmgr.makeTrue();
    }
  }

  private Formula buildDirectSecondLevelAssignment(IType lType,
      String lVarName, IASTRightHandSide pRight, String function,
      Constraints constraints, SSAMapBuilder ssa) {

    IASTRightHandSide right = removeCast(pRight);
    Formula lVar = makeVariable(lVarName, ssa);

    if (isVariable(right)) {
      // C statement like: s1 = s2;

      // include aliases if the left or right side may be a pointer a pointer
      IASTIdExpression rIdExp = (IASTIdExpression) right;
      if (maybePointer(lType, lVarName, ssa) || maybePointer(rIdExp, function, ssa)) {
        // we assume that either the left or the right hand side is a pointer
        // so we add the equality: *l = *r
        String lPVarName = makePointerMask(lVarName, ssa);
        Formula lPVar = makeVariable(lPVarName, ssa);
        Formula rPVar = makePointerVariable(rIdExp, function, ssa);
        return fmgr.makeAssignment(lPVar, rPVar);

      } else {
        // we can assume, that no pointers are affected in this assignment
        return fmgr.makeTrue();
      }

    } else if (isPointerDereferencing(right)) {
      // C statement like: s1 = *s2;

      String lPVarName = makePointerMask(lVarName, ssa);
      makeFreshIndex(lPVarName, ssa);
      removeOldPointerVariablesFromSsaMap(lPVarName, ssa);
      Formula lPVar = makeVariable(lPVarName, ssa);

      IASTIdExpression rIdExpr = (IASTIdExpression)
          removeCast(((IASTUnaryExpression) right).getOperand());
      Formula rPVar = makePointerVariable(rIdExpr, function, ssa);

      // the dealiased address of the right hand side may be a pointer itself.
      // to ensure tracking, we need to set the left side
      // equal to the dealiased right side or update the pointer
      // r is the right hand side variable, l is the left hand side variable
      // ∀p ∈ maybePointer: (p = *r) ⇒ (l = p ∧ *l = *p)
      List<String> pVars = getAllPointerVariablesFromSsaMap(ssa);
      for (String pVarName : pVars) {
        String varName = removePointerMask(pVarName);
        if(!varName.equals(lVarName)) {

          Formula var = makeVariable(varName, ssa);
          Formula pVar = makeVariable(pVarName, ssa);

          Formula p = fmgr.makeEqual(rPVar, var);

          Formula dirEq = fmgr.makeEqual(lVar, var);
          Formula indirEq = fmgr.makeEqual(lPVar, pVar);
          Formula consequence = fmgr.makeAnd(dirEq, indirEq);

          Formula constraint = fmgr.makeImplication(p, consequence);

          constraints.addConstraint(constraint);
        }
      }

      // no need to add a second level assignment
      return fmgr.makeTrue();

    } else if (isMemoryLocation(right)) {
      // s = &x
      // need to update the pointer on the left hand side
      if (right instanceof IASTUnaryExpression
          && ((IASTUnaryExpression) right).getOperator() == UnaryOperator.AMPER){

        IASTExpression rOperand =
            removeCast(((IASTUnaryExpression) right).getOperand());
        if (rOperand instanceof IASTIdExpression) {
          String rVarName = scopedIfNecessary((IASTIdExpression) rOperand, function);
          Formula rVar = makeVariable(rVarName, ssa);

          String lPVarName = makePointerMask(lVarName, ssa);
          Formula lPVar = makeVariable(lPVarName, ssa);

          return fmgr.makeAssignment(lPVar, rVar);
        }
      }

      // s = malloc()
      // has been handled already
      return fmgr.makeTrue();

    } else {
      // s = 1
      // s = someFunction()
      // s = a + b

      // no second level assignment necessary
      return fmgr.makeTrue();
    }
  }

  private Formula makePredicate(IASTExpression exp, boolean isTrue, CFAEdge edge,
      String function, SSAMapBuilder ssa, Constraints constraints) throws UnrecognizedCCodeException {

    Formula result = toBooleanFormula(exp.accept(getExpressionVisitor(edge, function, ssa, constraints)));

    if (!isTrue) {
      result = fmgr.makeNot(result);
    }
    return result;
  }

  protected Formula makePredicate(IASTExpression exp, CFAEdge edge, String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {
    Constraints constraints = new Constraints();
    Formula f = makePredicate(exp, true, edge, function, ssa, constraints);
    return fmgr.makeAnd(f, constraints.get());
  }

  private ExpressionToFormulaVisitor getExpressionVisitor(CFAEdge pEdge, String pFunction,
      SSAMapBuilder pSsa, Constraints pCo) {
    if (lvalsAsUif) {
      return new ExpressionToFormulaVisitorUIF(pEdge, pFunction, pSsa, pCo);
    } else if (handlePointerAliasing) {
      return new ExpressionToFormulaVisitorPointers(pEdge, pFunction, pSsa, pCo);
    } else {
      return new ExpressionToFormulaVisitor(pEdge, pFunction, pSsa, pCo);
    }
  }

  private LvalueVisitor getLvalueVisitor(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
    if (lvalsAsUif) {
      return new LvalueVisitorUIF(pEdge, pFunction, pSsa, pCo);
    } else if (handlePointerAliasing) {
      return new LvalueVisitorPointers(pEdge, pFunction, pSsa, pCo);
    } else {
      return new LvalueVisitor(pEdge, pFunction, pSsa, pCo);
    }
  }

  private Formula toBooleanFormula(Formula f) {
    // If this is not a predicate, make it a predicate by adding a "!= 0"
    if (!fmgr.isBoolean(f)) {
      Formula z = fmgr.makeNumber(0);
      f = fmgr.makeNot(fmgr.makeEqual(f, z));
    }
    return f;
  }

  private Formula toNumericFormula(Formula f) {
    if (fmgr.isBoolean(f)) {
      f = fmgr.makeIfThenElse(f, fmgr.makeNumber(1), fmgr.makeNumber(0));
    }
    return f;
  }

  private static boolean isVariable(IASTNode exp) {
    return exp instanceof IASTIdExpression;
  }

  private static boolean isPointerDereferencing(IASTNode exp) {
    return (exp instanceof IASTUnaryExpression
        && ((IASTUnaryExpression) exp).getOperator() == UnaryOperator.STAR);
  }

  private static boolean isStaticallyDeclaredPointer(IType expr) {
    return expr instanceof IASTPointerTypeSpecifier;
  }

  /**
   * Returns whether the given variable name is a pointer variable name.
   */
  private static boolean isPointerVariable(String variableName) {
    return variableName.matches("\\*<.*>");
  }

  private boolean isMemoryLocation(IASTNode exp) {

    // memory allocating function?
    if (exp instanceof IASTFunctionCall) {
      IASTExpression fn =
          ((IASTFunctionCall) exp).getFunctionCallExpression().getFunctionNameExpression();

      if (fn instanceof IASTIdExpression) {
        String functionName = ((IASTIdExpression) fn).getName();
        if (memoryAllocationFunctions.contains(functionName)) {
          return true;
        }
      }

    // explicit heap/stack address?
    } else if (exp instanceof IASTUnaryExpression
        && ((IASTUnaryExpression) exp).getOperator() == UnaryOperator.AMPER) {
      return true;
    }

    return false;
  }

  /** Returns if a given expression may be a pointer. */
  private boolean maybePointer(IASTExpression pExp, String function, SSAMapBuilder ssa) {
    IASTExpression exp = removeCast(pExp);
    if (exp instanceof IASTIdExpression) {
      IASTIdExpression idExp = (IASTIdExpression) exp;
      IType type = exp.getExpressionType();
      return maybePointer(type, scopedIfNecessary(idExp, function), ssa);
    }

    return false;
  }

  private boolean maybePointer(IType type, String varName, SSAMapBuilder ssa) {
    if (type != null && isStaticallyDeclaredPointer(type)) {
      return true;
    }

    // check if it has been used as a pointer before
    List<String> pVarNames = getAllPointerVariablesFromSsaMap(ssa);
    String expPVarName = makePointerMask(varName, ssa);
    return pVarNames.contains(expPVarName);
  }

  /**
   * Returns a list of all variable names representing memory locations in
   * the SSAMap. These memory locations are those previously used.
   *
   * Stored memory locations are prefixed with
   * {@link MEMORY_ADDRESS_VARIABLE_PREFIX}.
   */
  private static List<String> getAllMemoryLocationsFromSsaMap(SSAMapBuilder ssa) {
    List<String> memoryLocations = new LinkedList<String>();
    Set<String> ssaVariables = ssa.build().allVariables();

    Pattern p = Pattern.compile("^" + MEMORY_ADDRESS_VARIABLE_PREFIX + ".*");

    for (String variable : ssaVariables) {
      if (p.matcher(variable).matches()) {
        memoryLocations.add(variable);
      }
    }

    return memoryLocations;
  }

  /**
   * Returns a list of all pointer variables stored in the SSAMap.
   */
  private static List<String> getAllPointerVariablesFromSsaMap(SSAMapBuilder ssa) {
    List<String> pointerVariables = new LinkedList<String>();
    Set<String> ssaVariables = ssa.build().allVariables();

    for (String variable : ssaVariables) {
      if (isPointerVariable(variable)) {
        pointerVariables.add(variable);
      }
    }

    return pointerVariables;
  }

  /**
   * Removes all pointer variables belonging to a given variable from a given
   * SSAMapBuilderthat are no longer valid. Validity of an entry expires,
   * when the pointer variable belongs to a variable with an old index.
   *
   * @param newPVar The variable name of the new pointer variable.
   * @param ssa The SSAMapBuilder from which the variables are to be deleted
   */
  private static void removeOldPointerVariablesFromSsaMap(String newPVar,
      SSAMapBuilder ssa) {

    String newVar = removePointerMask(newPVar);

    List<String> pointerVariables = getAllPointerVariablesFromSsaMap(ssa);
    for (String pointerVar : pointerVariables) {
      String oldVar = removePointerMask(pointerVar);
      if (!pointerVar.equals(newPVar) && oldVar.equals(newVar)) {
        ssa.deleteVariable(pointerVar);
      }
    }
  }

  private static IASTExpression removeCast(IASTExpression exp) {
    if (exp instanceof IASTCastExpression) {
      return removeCast(((IASTCastExpression) exp).getOperand());
    }
    return exp;
  }

  private static IASTRightHandSide removeCast(IASTRightHandSide exp) {
    if (exp instanceof IASTCastExpression) {
      return removeCast(((IASTCastExpression) exp).getOperand());
    }
    return exp;
  }

  /**
   * This class tracks constraints which are created during AST traversal but
   * cannot be applied at the time of creation.
   */
  private class Constraints {

    private Formula constraints = fmgr.makeTrue();

    private void addConstraint(Formula pCo) {
      constraints = fmgr.makeAnd(constraints, pCo);
    }

    public Formula get() {
      return constraints;
    }
  }

  private class ExpressionToFormulaVisitor extends DefaultExpressionVisitor<Formula, UnrecognizedCCodeException> {

    protected final CFAEdge       edge;
    protected final String        function;
    protected final SSAMapBuilder ssa;
    protected final Constraints   constraints;

    public ExpressionToFormulaVisitor(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      edge = pEdge;
      function = pFunction;
      ssa = pSsa;
      constraints = pCo;
    }

    @Override
    protected Formula visitDefault(IASTExpression exp)
        throws UnrecognizedCCodeException {
      warnUnsafeVar(exp);
      return makeVariable(scoped(exprToVarName(exp), function), ssa);
    }

    @Override
    public Formula visit(IASTBinaryExpression exp) throws UnrecognizedCCodeException {
      BinaryOperator op = exp.getOperator();
      IASTExpression e1 = exp.getOperand1();
      IASTExpression e2 = exp.getOperand2();

      switch (op) {
      case LOGICAL_AND:
      case LOGICAL_OR: {
        // these operators expect boolean arguments
        Formula me1 = toBooleanFormula(e1.accept(this));
        Formula me2 = toBooleanFormula(e2.accept(this));

        switch (op) {
        case LOGICAL_AND:
          return fmgr.makeAnd(me1, me2);
        case LOGICAL_OR:
          return fmgr.makeOr(me1, me2);
        default:
          throw new AssertionError();
        }
      }

      default: {
        // these other operators expect numeric arguments
        Formula me1 = toNumericFormula(e1.accept(this));
        Formula me2 = toNumericFormula(e2.accept(this));

        switch (op) {
        case PLUS:
          return fmgr.makePlus(me1, me2);
        case MINUS:
          return fmgr.makeMinus(me1, me2);
        case MULTIPLY:
          return fmgr.makeMultiply(me1, me2);
        case DIVIDE:
          return fmgr.makeDivide(me1, me2);
        case MODULO:
          return fmgr.makeModulo(me1, me2);
        case BINARY_AND:
          return fmgr.makeBitwiseAnd(me1, me2);
        case BINARY_OR:
          return fmgr.makeBitwiseOr(me1, me2);
        case BINARY_XOR:
          return fmgr.makeBitwiseXor(me1, me2);
        case SHIFT_LEFT:
          return fmgr.makeShiftLeft(me1, me2);
        case SHIFT_RIGHT:
          return fmgr.makeShiftRight(me1, me2);

        case GREATER_THAN:
          return fmgr.makeGt(me1, me2);
        case GREATER_EQUAL:
          return fmgr.makeGeq(me1, me2);
        case LESS_THAN:
          return fmgr.makeLt(me1, me2);
        case LESS_EQUAL:
          return fmgr.makeLeq(me1, me2);
        case EQUALS:
          return fmgr.makeEqual(me1, me2);
        case NOT_EQUALS:
          return fmgr.makeNot(fmgr.makeEqual(me1, me2));

        default:
          throw new UnrecognizedCCodeException("Unknown binary operator", edge, exp);
        }
      }
      }
    }

    @Override
    public Formula visit(IASTCastExpression cexp) throws UnrecognizedCCodeException {
      // we completely ignore type casts
      logDebug("IGNORING TYPE CAST:", cexp);
      return cexp.getOperand().accept(this);
    }

    @Override
    public Formula visit(IASTIdExpression idExp) {

      if (idExp.getDeclaration() instanceof IASTEnumerator) {
        IASTEnumerator enumerator = (IASTEnumerator)idExp.getDeclaration();
        if (enumerator.hasValue()) {
          return fmgr.makeNumber(Long.toString(enumerator.getValue()));
        } else {
          // We don't know the value here, but we know it is constant.
          return makeConstant(enumerator.getName(), ssa);
        }
      }

      return makeVariable(scopedIfNecessary(idExp, function), ssa);
    }

    @Override
    public Formula visit(IASTFieldReference fExp) throws UnrecognizedCCodeException {
      IASTExpression fieldRef = fExp.getFieldOwner();
      if (fieldRef instanceof IASTIdExpression) {
        IASTSimpleDeclaration decl = ((IASTIdExpression) fieldRef).getDeclaration();
        if (decl instanceof IASTDeclaration && ((IASTDeclaration)decl).isGlobal()) {
          // this is the reference to a global field variable

          // we can omit the warning (no pointers involved),
          // and we don't need to scope the variable reference
          return makeVariable(exprToVarName(fExp), ssa);
        }
      }

      // else do the default
      return super.visit(fExp);
    }

    @Override
    public Formula visit(IASTCharLiteralExpression cExp) throws UnrecognizedCCodeException {
      // we just take the byte value
      return fmgr.makeNumber(cExp.getCharacter());
    }

    @Override
    public Formula visit(IASTIntegerLiteralExpression iExp) throws UnrecognizedCCodeException {
      return fmgr.makeNumber(iExp.getValue().toString());
    }

    @Override
    public Formula visit(IASTFloatLiteralExpression fExp) throws UnrecognizedCCodeException {
      return fmgr.makeNumber(fExp.getValue().toString());
    }

    @Override
    public Formula visit(IASTStringLiteralExpression lexp) throws UnrecognizedCCodeException {
      // we create a string constant representing the given
      // string literal
      String literal = lexp.getValue();
      Formula result = stringLitToFormula.get(literal);

      if (result == null) {
        // generate a new string literal. We generate a new UIf
        int n = nextStringLitIndex++;
        result = fmgr.makeString(n);
        stringLitToFormula.put(literal, result);
      }

      return result;
    }

    @Override
    public Formula visit(IASTUnaryExpression exp) throws UnrecognizedCCodeException {
      IASTExpression operand = exp.getOperand();
      UnaryOperator op = exp.getOperator();

      switch (op) {
      case MINUS: {
        Formula term = toNumericFormula(operand.accept(this));
        return fmgr.makeNegate(term);
      }

      case TILDE: {
        Formula term = toNumericFormula(operand.accept(this));
        return fmgr.makeBitwiseNot(term);
      }

      case NOT: {
        Formula term = toBooleanFormula(operand.accept(this));
        return fmgr.makeNot(term);
      }

      case AMPER:
      case STAR:
        return visitDefault(exp);

      case SIZEOF:
        if (exp.getOperand() instanceof IASTIdExpression) {
          IType lIType =
              ((IASTIdExpression) exp.getOperand()).getExpressionType();
          return handleSizeof(exp, lIType);
        } else {
          return visitDefault(exp);
        }

      default:
        throw new UnrecognizedCCodeException("Unknown unary operator", edge, exp);
      }
    }

    @Override
    public Formula visit(IASTTypeIdExpression tIdExp)
        throws UnrecognizedCCodeException {

      if (tIdExp.getOperator() == TypeIdOperator.SIZEOF) {
        IType lIType = tIdExp.getTypeId().getDeclSpecifier();
        return handleSizeof(tIdExp, lIType);
      } else {
        return visitDefault(tIdExp);
      }
    }

    private Formula handleSizeof(IASTExpression pExp, IType pIType)
        throws UnrecognizedCCodeException {

      if (pIType instanceof IASTSimpleDeclSpecifier) {
        return fmgr.makeNumber(machineModel.getSizeof((IASTSimpleDeclSpecifier) pIType));

      } else {
        return visitDefault(pExp);
      }
    }
  }

  private class ExpressionToFormulaVisitorUIF extends ExpressionToFormulaVisitor {

    public ExpressionToFormulaVisitorUIF(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      super(pEdge, pFunction, pSsa, pCo);
    }

    private Formula makeUIF(String name, FormulaList args, SSAMapBuilder ssa) {
      int idx = ssa.getIndex(name, args);
      if (idx <= 0) {
        logger.log(Level.ALL, "DEBUG_3",
            "WARNING: Auto-instantiating lval: ", name, "(", args, ")");
        idx = 1;
        ssa.setIndex(name, args, idx);
      }
      return fmgr.makeUIF(name, args, idx);
    }

    @Override
    public Formula visit(IASTArraySubscriptExpression aexp) throws UnrecognizedCCodeException {
      IASTExpression arrexp = aexp.getArrayExpression();
      IASTExpression subexp = aexp.getSubscriptExpression();
      Formula aterm = toNumericFormula(arrexp.accept(this));
      Formula sterm = toNumericFormula(subexp.accept(this));

      String ufname = OP_ARRAY_SUBSCRIPT;
      return makeUIF(ufname, fmgr.makeList(aterm, sterm), ssa);
    }

    @Override
    public Formula visit(IASTFieldReference fexp) throws UnrecognizedCCodeException {
      String field = fexp.getFieldName();
      IASTExpression owner = fexp.getFieldOwner();
      Formula term = toNumericFormula(owner.accept(this));

      String tpname = getTypeName(owner.getExpressionType());
      String ufname =
          (fexp.isPointerDereference() ? "->{" : ".{") + tpname + "," + field
              + "}";

      // see above for the case of &x and *x
      return makeUIF(ufname, fmgr.makeList(term), ssa);
    }

    @Override
    public Formula visit(IASTUnaryExpression exp) throws UnrecognizedCCodeException {
      UnaryOperator op = exp.getOperator();
      switch (op) {
      case AMPER:
      case STAR:
        String opname;
        if (op == UnaryOperator.AMPER) {
          opname = OP_ADDRESSOF_NAME;
        } else {
          opname = OP_STAR_NAME;
        }
        Formula term = toNumericFormula(exp.getOperand().accept(this));

        // PW make SSA index of * independent from argument
        int idx = getIndex(opname, ssa);
        //int idx = getIndex(
        //    opname, term, ssa, absoluteSSAIndices);

        // build the  function corresponding to this operation.
        return fmgr.makeUIF(opname, fmgr.makeList(term), idx);

      default:
        return super.visit(exp);
      }
    }
  }

  private class ExpressionToFormulaVisitorPointers extends
      ExpressionToFormulaVisitor {

    public ExpressionToFormulaVisitorPointers(CFAEdge pEdge, String pFunction,
        SSAMapBuilder pSsa, Constraints pCo) {
      super(pEdge, pFunction, pSsa, pCo);
    }

    @Override
    public Formula visit(IASTUnaryExpression exp)
        throws UnrecognizedCCodeException {
      IASTExpression opExp = removeCast(exp.getOperand());
      UnaryOperator op = exp.getOperator();

      switch (op) {
      case AMPER:
        return makeAddressVariable(exp, function);

      case STAR:
        if (opExp instanceof IASTIdExpression) {
          return makePointerVariable((IASTIdExpression) opExp, function, ssa);
        }
        // fall-through

      default:
        return super.visit(exp);
      }
    }

    private Formula makeAddressVariable(IASTUnaryExpression exp, String function)
        throws UnrecognizedCCodeException {

        IASTExpression operand = removeCast(exp.getOperand());
        UnaryOperator op = exp.getOperator();

        if (op != UnaryOperator.AMPER || !(operand instanceof IASTIdExpression)) {
          return super.visitDefault(exp);
        }

        return makeMemoryLocationVariable((IASTIdExpression) operand, function);
    }

    /**
     * Returns a Formula representing the memory location of a given IdExpression.
     * Ensures that the location is unique and not 0.
     *
     * @param function The scope of the variable.
     */
    private Formula makeMemoryLocationVariable(IASTIdExpression exp, String function) {
      String addressVariable = makeMemoryLocationVariableName(scopedIfNecessary(exp, function));

      // a variable address is always initialized, not 0 and cannot change
      if (ssa.getIndex(addressVariable) == VARIABLE_UNSET) {
        List<String> oldMemoryLocations = getAllMemoryLocationsFromSsaMap(ssa);
        Formula newMemoryLocation = makeConstant(addressVariable, ssa);

        // a variable address that is unknown is different from all previously known addresses
        for (String memoryLocation : oldMemoryLocations) {
          Formula oldMemoryLocation = makeConstant(memoryLocation, ssa);
          Formula addressInequality = fmgr.makeNot(fmgr.makeEqual(newMemoryLocation, oldMemoryLocation));

          constraints.addConstraint(addressInequality);
        }

        // a variable address is not 0
        Formula notZero = fmgr.makeNotEqual(newMemoryLocation, fmgr.makeNumber(0));
        constraints.addConstraint(notZero);
      }

      return makeConstant(addressVariable, ssa);
    }
  }

  private class RightHandSideToFormulaVisitor extends
      ForwardingExpressionVisitor<Formula, UnrecognizedCCodeException>
      implements RightHandSideVisitor<Formula, UnrecognizedCCodeException> {

    protected final CFAEdge       edge;
    protected final String        function;
    protected final SSAMapBuilder ssa;
    protected final Constraints   constraints;

    public RightHandSideToFormulaVisitor(String pFunction, SSAMapBuilder pSsa, Constraints pCo, CFAEdge pEdge) {
      super(getExpressionVisitor(pEdge, pFunction, pSsa, pCo));
      edge = pEdge;
      function = pFunction;
      ssa = pSsa;
      constraints = pCo;
    }

    @Override
    public Formula visit(IASTFunctionCallExpression fexp) throws UnrecognizedCCodeException {

      IASTExpression fn = fexp.getFunctionNameExpression();
      List<IASTExpression> pexps = fexp.getParameterExpressions();
      String func;
      if (fn instanceof IASTIdExpression) {
        func = ((IASTIdExpression)fn).getName();
        if (nondetFunctions.contains(func)) {
          // function call like "random()"
          // ignore parameters and just create a fresh variable for it
          return makeFreshVariable(func, ssa);

        } else if (UNSUPPORTED_FUNCTIONS.containsKey(func)) {
          throw new UnsupportedCCodeException(UNSUPPORTED_FUNCTIONS.get(func), edge, fexp);

        } else if (!PURE_EXTERNAL_FUNCTIONS.contains(func)) {
          if (pexps.isEmpty()) {
            // function of arity 0
            log(Level.INFO, "Assuming external function " + func + " to be a constant function.");
          } else {
            log(Level.INFO, "Assuming external function " + func + " to be a pure function.");
          }
        }
      } else {
        log(Level.WARNING, getLogMessage("Ignoring function call through function pointer", fexp));
        func = "<func>{" + fn.toASTString() + "}";
      }

      if (pexps.isEmpty()) {
        // This is a function of arity 0 and we assume its constant.
        return makeConstant(func, ssa);

      } else {
        func += "{" + pexps.size() + "}"; // add #arguments to function name to cope with varargs functions

        List<Formula> args = new ArrayList<Formula>(pexps.size());
        for (IASTExpression pexp : pexps) {
          args.add(toNumericFormula(pexp.accept(this)));
        }

        return fmgr.makeUIF(func, fmgr.makeList(args));
      }
    }
  }

  private class StatementToFormulaVisitor extends RightHandSideToFormulaVisitor implements StatementVisitor<Formula, UnrecognizedCCodeException> {

    public StatementToFormulaVisitor(String pFunction, SSAMapBuilder pSsa, Constraints pConstraints, CFAEdge edge) {
      super(pFunction, pSsa, pConstraints, edge);
    }

    @Override
    public Formula visit(IASTExpressionStatement pIastExpressionStatement) {
      // side-effect free statement, ignore
      return fmgr.makeTrue();
    }

    public Formula visit(IASTAssignment assignment) throws UnrecognizedCCodeException {
      Formula rightVariable = assignment.getRightHandSide().accept(this);
      Formula r = toNumericFormula(rightVariable);
      Formula l = buildLvalueTerm(assignment.getLeftHandSide(), edge, function, ssa, constraints);
      return fmgr.makeAssignment(l, r);
    }

    @Override
    public Formula visit(IASTExpressionAssignmentStatement pIastExpressionAssignmentStatement) throws UnrecognizedCCodeException {
      return visit((IASTAssignment)pIastExpressionAssignmentStatement);
    }

    @Override
    public Formula visit(IASTFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) throws UnrecognizedCCodeException {
      return visit((IASTAssignment)pIastFunctionCallAssignmentStatement);
    }

    @Override
    public Formula visit(IASTFunctionCallStatement fexp) throws UnrecognizedCCodeException {
      // this is an external call
      // visit expression in order to print warnings if necessary
      visit(fexp.getFunctionCallExpression());
      return fmgr.makeTrue();
    }
  }

  private class StatementToFormulaVisitorPointers extends StatementToFormulaVisitor {

    public StatementToFormulaVisitorPointers(String pFunction,
        SSAMapBuilder pSsa, Constraints pConstraints, CFAEdge edge) {
      super(pFunction, pSsa, pConstraints, edge);
    }

    @Override
    public Formula visit(IASTFunctionCallExpression fexp) throws UnrecognizedCCodeException {
      // handle malloc
      IASTExpression fn = fexp.getFunctionNameExpression();
      if (fn instanceof IASTIdExpression) {
        String fName = ((IASTIdExpression)fn).getName();

        if (memoryAllocationFunctions.contains(fName)) {
          // for now all parameters are ignored

          List<String> memoryLocations = getAllMemoryLocationsFromSsaMap(ssa);

          String mallocVarName = makeFreshMallocVariableName();
          Formula mallocVar = makeConstant(mallocVarName, ssa);

          // we must distinguish between two cases:
          // either the result is 0 or it is different from all other memory locations
          // (m != 0) => for all memory locations n: m != n
          Formula ineq = fmgr.makeTrue();
          for (String ml : memoryLocations) {
            Formula n = makeConstant(ml, ssa);

            Formula notEqual = fmgr.makeNotEqual(n, mallocVar);
            ineq = fmgr.makeAnd(notEqual, ineq);
          }

          Formula nullFormula = fmgr.makeNumber(0);
          Formula notEqual = fmgr.makeNotEqual(mallocVar, nullFormula);
          Formula implication = fmgr.makeImplication(notEqual, ineq);

          constraints.addConstraint(implication);
          return mallocVar;
        }
      }

      return super.visit(fexp);
    }

    @Override
    public Formula visit(IASTAssignment assignment)
        throws UnrecognizedCCodeException {
      IASTExpression left = removeCast(assignment.getLeftHandSide());

      if (left instanceof IASTIdExpression) {
        // p = ...
        return handleDirectAssignment(assignment);

      } else if (left instanceof IASTUnaryExpression
          && ((IASTUnaryExpression) left).getOperator() == UnaryOperator.STAR) {
        // *p = ...
        return handleIndirectAssignment(assignment);

      } else {
        return super.visit(assignment);
      }
    }

    /**
     * An indirect assignment does not change the value of the variable on the
     * left hand side. Instead it changes the value stored in the memory location
     * aliased on the left hand side.
     */
    private Formula handleIndirectAssignment(IASTAssignment pAssignment)
        throws UnrecognizedCCodeException {
      IASTExpression lExp = removeCast(pAssignment.getLeftHandSide());
      assert (lExp instanceof IASTUnaryExpression);

      // the following expressions are supported by cil:
      // *p = a;
      // *p = 1;
      // *p = a | b; (or any other binary statement)
      // *p = function();

      IASTUnaryExpression l = (IASTUnaryExpression) lExp;
      assert (l.getOperator() == UnaryOperator.STAR);

      IASTExpression lOperand = removeCast(l.getOperand());
      if (!(lOperand instanceof IASTIdExpression)) {
        // TODO: *(a + 2) = b
        return super.visit(pAssignment);
      }

      IASTRightHandSide r = pAssignment.getRightHandSide();

      String lVarName = scopedIfNecessary((IASTIdExpression) lOperand, function);
      Formula lVar = makeVariable(lVarName, ssa);

      String rVarName = null;
      Formula rPVar = null;
      if (r instanceof IASTIdExpression) {
        rVarName = scopedIfNecessary((IASTIdExpression) r, function);
        rPVar = makePointerVariable((IASTIdExpression) r, function, ssa);
      }

      Formula rightVariable = pAssignment.getRightHandSide().accept(this);
      rightVariable = toNumericFormula(rightVariable);
      Formula lPVar = buildLvalueTerm(pAssignment.getLeftHandSide(), edge, function, ssa, constraints);
      Formula assignments = fmgr.makeAssignment(lPVar, rightVariable);

      updateAllPointers(lVarName, lVar, rVarName, rightVariable);

      boolean doDeepUpdate = (r instanceof IASTIdExpression);
      updateAllMemoryLocations(lVar, rPVar, rightVariable, doDeepUpdate);

      return assignments;
    }

    private void updateAllMemoryLocations(Formula lVar, Formula rPVar, Formula rightVariable, boolean deepUpdate) {
      // for all memory addresses also update the aliasing
      // if the left variable is an alias for an address,
      // then the left side is (deep) equal to the right side
      // otherwise update the variables
      List<String> memAddresses = getAllMemoryLocationsFromSsaMap(ssa);
      if (deepUpdate) {
        for (String memAddress : memAddresses) {
          String varName = getVariableNameFromMemoryAddress(memAddress);

          Formula memAddressVar = makeVariable(memAddress, ssa);

          Formula oldVar = makeVariable(varName, ssa);
          String oldPVarName = makePointerMask(varName, ssa);
          Formula oldPVar = makeVariable(oldPVarName, ssa);

          makeFreshIndex(varName, ssa);

          Formula newVar = makeVariable(varName, ssa);
          String newPVarName = makePointerMask(varName, ssa);
          Formula newPVar = makeVariable(varName, ssa);
          removeOldPointerVariablesFromSsaMap(newPVarName, ssa);

          Formula varEquality = fmgr.makeAssignment(newVar, rightVariable);
          Formula pVarEquality = fmgr.makeAssignment(newPVar, rPVar);
          Formula varUpdate = fmgr.makeAssignment(newVar, oldVar);
          Formula pVarUpdate = fmgr.makeAssignment(newPVar, oldPVar);

          Formula condition = fmgr.makeEqual(lVar, memAddressVar);
          Formula equality = fmgr.makeAnd(varEquality, pVarEquality);
          Formula update = fmgr.makeAnd(varUpdate, pVarUpdate);

          Formula variableUpdate = fmgr.makeIfThenElse(condition, equality, update);
          constraints.addConstraint(variableUpdate);
        }

      } else {
        for (String memAddress : memAddresses) {
          String varName = getVariableNameFromMemoryAddress(memAddress);

          Formula oldVar = makeVariable(varName, ssa);

          makeFreshIndex(varName, ssa);

          Formula newVar = makeVariable(varName, ssa);
          String newPVarName = makePointerMask(varName, ssa);
          removeOldPointerVariablesFromSsaMap(newPVarName, ssa);

          Formula memAddressVar = makeVariable(memAddress, ssa);

          Formula condition = fmgr.makeEqual(lVar, memAddressVar);
          Formula equality = fmgr.makeAssignment(newVar, rightVariable);
          Formula update = fmgr.makeAssignment(newVar, oldVar);

          Formula variableUpdate = fmgr.makeIfThenElse(condition, equality, update);
          constraints.addConstraint(variableUpdate);
        }
      }
    }

    /**
     * Call this method if you need to update all pointers
     */
    private void updateAllPointers(String leftVarName, Formula leftVar,
        String rightVarName, Formula rightVariable) {

      // update all pointer variables (they might have a new value)
      // every variable aliased to the left hand side,
      // has its pointer set to the right hand side,
      // for all other pointer variables, the index is updated
      List<String> pVarNames = getAllPointerVariablesFromSsaMap(ssa);
      for (String pVarName : pVarNames) {
        String varName = removePointerMask(pVarName);
        if (!varName.equals(leftVarName) && !varName.equals(rightVarName)) {
          Formula var = makeVariable(varName, ssa);

          Formula oldPVar = makeVariable(pVarName, ssa);
          makeFreshIndex(pVarName, ssa);
          Formula newPVar = makeVariable(pVarName, ssa);

          Formula condition = fmgr.makeEqual(var, leftVar);
          Formula equality = fmgr.makeAssignment(newPVar, rightVariable);
          Formula indexUpdate = fmgr.makeAssignment(newPVar, oldPVar);

          Formula variableUpdate = fmgr.makeIfThenElse(condition, equality, indexUpdate);
          constraints.addConstraint(variableUpdate);
        }
      }
    }

    /** A direct assignment changes the value of the variable on the left side. */
    private Formula handleDirectAssignment(IASTAssignment assignment)
        throws UnrecognizedCCodeException {
      IASTExpression lExpr = removeCast(assignment.getLeftHandSide());
      assert(lExpr instanceof IASTIdExpression);

      IASTIdExpression left = (IASTIdExpression) lExpr;
      IASTRightHandSide right = removeCast(assignment.getRightHandSide());

      String leftVarName = scopedIfNecessary(left, function);

      // assignment (first level) -- uses superclass
      Formula ri = assignment.getRightHandSide().accept(this);
      Formula rightVariable = toNumericFormula(ri);
      Formula leftVariable = buildLvalueTerm(assignment.getLeftHandSide(), edge, function, ssa, constraints);
      Formula firstLevelFormula = fmgr.makeAssignment(leftVariable, rightVariable);

      // assignment (second level)
      String lVarName = scopedIfNecessary(left, function);
      Formula secondLevelFormula = buildDirectSecondLevelAssignment(
          left.getExpressionType(), lVarName,
          right, function, constraints, ssa);

      Formula assignmentFormula = fmgr.makeAnd(firstLevelFormula, secondLevelFormula);

      // updates
      if (isKnownMemoryLocation(leftVarName)) {
        String leftMemLocationName = makeMemoryLocationVariableName(leftVarName);
        Formula leftMemLocation = makeConstant(leftMemLocationName, ssa);

        // update all pointers:
        // if a pointer is aliased to the assigned variable,
        // update that pointer to reflect the new aliasing,
        // otherwise only update the index
        List<String> pVarNames = getAllPointerVariablesFromSsaMap(ssa);
        Formula newLeftVar = leftVariable;
        for (String pVarName : pVarNames) {
          String varName = removePointerMask(pVarName);
          if (!varName.equals(leftVarName)) {
            Formula var = makeVariable(varName, ssa);
            Formula oldPVar = makeVariable(pVarName, ssa);
            makeFreshIndex(pVarName, ssa);
            Formula newPVar = makeVariable(pVarName, ssa);

            Formula condition = fmgr.makeEqual(var, leftMemLocation);
            Formula equivalence = fmgr.makeAssignment(newPVar, newLeftVar);
            Formula update = fmgr.makeAssignment(newPVar, oldPVar);

            Formula constraint = fmgr.makeIfThenElse(condition, equivalence, update);
            constraints.addConstraint(constraint);
          }
        }

      }
      return assignmentFormula;
    }

    /** Returns whether the address of a given variable has been used before. */
    private boolean isKnownMemoryLocation(String varName) {
      assert varName != null;
      List<String> memLocations = getAllMemoryLocationsFromSsaMap(ssa);
      String memVarName = makeMemoryLocationVariableName(varName);
      return memLocations.contains(memVarName);
    }

    /** Returns a new variable name for every malloc call. */
    private String makeFreshMallocVariableName() {
      int idx = ssa.getIndex(MALLOC_COUNTER_VARIABLE_NAME);

      if (idx == VARIABLE_UNSET) {
        idx = VARIABLE_UNINITIALIZED;
      }

      ssa.setIndex(MALLOC_COUNTER_VARIABLE_NAME, idx + 1);
      return MALLOC_VARIABLE_PREFIX + idx;
    }

    /** Returns the variable name of a memory address variable */
    private String getVariableNameFromMemoryAddress(String memoryAddress) {
      assert(memoryAddress.startsWith(MEMORY_ADDRESS_VARIABLE_PREFIX));

      return memoryAddress.substring(MEMORY_ADDRESS_VARIABLE_PREFIX.length());
    }
  }

  private class LvalueVisitor extends
      DefaultExpressionVisitor<Formula, UnrecognizedCCodeException> {

    protected final CFAEdge       edge;
    protected final String        function;
    protected final SSAMapBuilder ssa;
    protected final Constraints   constraints;

    public LvalueVisitor(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      edge = pEdge;
      function = pFunction;
      ssa = pSsa;
      constraints = pCo;
    }

    @Override
    protected Formula visitDefault(IASTExpression exp) throws UnrecognizedCCodeException {
      throw new UnrecognizedCCodeException("Unknown lvalue", edge, exp);
    }

    @Override
    public Formula visit(IASTIdExpression idExp) {
      String var = scopedIfNecessary(idExp, function);
      return makeFreshVariable(var, ssa);
    }

    protected Formula makeUIF(IASTExpression exp) {
      warnUnsafeAssignment();
      logDebug("Assigning to ", exp);

      String var = scoped(exprToVarName(exp), function);
      return makeFreshVariable(var, ssa);
    }

    @Override
    public Formula visit(IASTUnaryExpression pE) throws UnrecognizedCCodeException {
      return makeUIF(pE);
    }

    @Override
    public Formula visit(IASTFieldReference fExp) throws UnrecognizedCCodeException {

      IASTExpression fieldRef = fExp.getFieldOwner();
      if (fieldRef instanceof IASTIdExpression) {
        IASTSimpleDeclaration decl = ((IASTIdExpression) fieldRef).getDeclaration();
        if (decl instanceof IASTDeclaration && ((IASTDeclaration)decl).isGlobal()) {
          // this is the reference to a global field variable

          // we don't need to scope the variable reference
          String var = exprToVarName(fExp);
          return makeFreshVariable(var, ssa);
        }
      }

      // else do the default
      return makeUIF(fExp);
    }

    @Override
    public Formula visit(IASTArraySubscriptExpression pE) throws UnrecognizedCCodeException {
      return makeUIF(pE);
    }
  }

  private class LvalueVisitorUIF extends LvalueVisitor {

    public LvalueVisitorUIF(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      super(pEdge, pFunction, pSsa, pCo);
    }

    @Override
    public Formula visit(IASTUnaryExpression uExp) throws UnrecognizedCCodeException {
      UnaryOperator op = uExp.getOperator();
      IASTExpression operand = uExp.getOperand();
      String opname;
      switch (op) {
      case AMPER:
        opname = OP_ADDRESSOF_NAME;
        break;
      case STAR:
        opname = OP_STAR_NAME;
        break;
      default:
        throw new UnrecognizedCCodeException("Invalid unary operator for lvalue", edge, uExp);
      }
      Formula term = buildTerm(operand, edge, function, ssa, constraints);

      // PW make SSA index of * independent from argument
      int idx = makeFreshIndex(opname, ssa);
      //int idx = makeLvalIndex(opname, term, ssa, absoluteSSAIndices);

      // build the "updated" function corresponding to this operation.
      // what we do is the following:
      // C            |     MathSAT
      // *x = 1       |     <ptr_*>::2(x) = 1
      // ...
      // &(*x) = 2    |     <ptr_&>::2(<ptr_*>::1(x)) = 2
      return fmgr.makeUIF(opname, fmgr.makeList(term), idx);
    }

    @Override
    public Formula visit(IASTFieldReference fexp) throws UnrecognizedCCodeException {
      String field = fexp.getFieldName();
      IASTExpression owner = fexp.getFieldOwner();
      Formula term = buildTerm(owner, edge, function, ssa, constraints);

      String tpname = getTypeName(owner.getExpressionType());
      String ufname =
        (fexp.isPointerDereference() ? "->{" : ".{") +
        tpname + "," + field + "}";
      FormulaList args = fmgr.makeList(term);
      int idx = makeLvalIndex(ufname, args, ssa);

      // see above for the case of &x and *x
      return fmgr.makeUIF(ufname, args, idx);
    }

    @Override
    public Formula visit(IASTArraySubscriptExpression aexp) throws UnrecognizedCCodeException {
      IASTExpression arrexp = aexp.getArrayExpression();
      IASTExpression subexp = aexp.getSubscriptExpression();
      Formula aterm = buildTerm(arrexp, edge, function, ssa, constraints);
      Formula sterm = buildTerm(subexp, edge, function, ssa, constraints);

      String ufname = OP_ARRAY_SUBSCRIPT;
      FormulaList args = fmgr.makeList(aterm, sterm);
      int idx = makeLvalIndex(ufname, args, ssa);

      return fmgr.makeUIF(ufname, args, idx);

    }
  }

  private class LvalueVisitorPointers extends LvalueVisitor {
    public LvalueVisitorPointers(CFAEdge pEdge, String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      super(pEdge, pFunction, pSsa, pCo);
    }

    @Override
    public Formula visit(IASTCastExpression e) throws UnrecognizedCCodeException {
      return e.getOperand().accept(this);
    }

    private Formula getPointerFormula(IASTExpression pExp) {
      IASTExpression exp = removeCast(pExp);

      if (exp instanceof IASTIdExpression) {
        // *a = ...
        // *((int*) a) = ...
        IASTIdExpression pId = (IASTIdExpression) exp;
        String pVarName = makePointerVariableName(pId, function, ssa);
        makeFreshIndex(pVarName, ssa);
        return makePointerVariable(pId, function, ssa);

      } else {
        // apparently valid cil output:
        // *(&s.f) = ...
        // *(s->f) = ...
        // *(a+b) = ...

        return makeUIF(exp);
      }
    }

    @Override
    public Formula visit(IASTUnaryExpression pE) throws UnrecognizedCCodeException {
      if (pE.getOperator() == UnaryOperator.STAR) {
        return getPointerFormula(pE.getOperand());
      } else {
        return super.visit(pE);
      }
    }
  }
}
