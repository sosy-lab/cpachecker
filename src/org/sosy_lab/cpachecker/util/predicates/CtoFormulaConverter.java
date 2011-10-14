/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

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

  @Option(description="if initAllVars is true, we get rid of all non-determinism. "
    + "This might not be desirable. If the following property is set to a non-empty value, "
    + "all variables starting with this prefix will not be initialized automatically")
  // TODO this is not only about missing initialization, it should be renamed to nondetVariables
  private String noAutoInitPrefix = "__BLAST_NONDET";

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

  @Option(description = "the machine model used for functions sizeof and alignof",
          values={"32-Linux", "64-Linux"})
  private String machineModel = "32-Linux";

  private MachineModel mMachineModel;

  @Option(description = "handle Pointers")
  private boolean handlePointerAliasing = false;

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

  private final Set<String> printedWarnings = new HashSet<String>();

  private final Map<String, Formula> stringLitToFormula = new HashMap<String, Formula>();
  private int nextStringLitIndex = 0;

  protected final FormulaManager fmgr;
  protected final LogManager logger;

  private static final int                 VARIABLE_UNSET          = -1;
  private static final int                 VARIABLE_UNINITIALIZED  = 2;

  public CtoFormulaConverter(Configuration config, FormulaManager fmgr, LogManager logger) throws InvalidConfigurationException {
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
            + ": " + astNode.getRawSignature();
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

  // looks up the variable in the current namespace
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

  private boolean isNondetVariable(String var) {
    return (!noAutoInitPrefix.isEmpty()) && var.startsWith(noAutoInitPrefix);
  }

  private static String exprToVarName(IASTExpression e) {
    return e.getRawSignature().replaceAll("[ \n\t]", "");
  }

  private String getTypeName(final IType tp) {

    if (tp instanceof IASTPointerTypeSpecifier) {
      return getTypeName(((IASTPointerTypeSpecifier)tp).getType());

    } else if (tp instanceof ITypedef) {
      return getTypeName(((ITypedef)tp).getType());

    } else if (tp instanceof IComplexType){
      return ((IComplexType)tp).getName();

    } else throw new AssertionError("wrong type");
  }

  /**
   * Produces a fresh new SSA index for an assignment
   * and updates the SSA map.
   */
  private int makeFreshIndex(String name, SSAMapBuilder ssa) {
    int idx = ssa.getIndex(name);
    if (idx > 0) {
      idx = idx+1;
    } else {
      idx = 2; // AG - IMPORTANT!!! We must start from 2 and
      // not from 1, because this is an assignment,
      // so the SSA index must be fresh.
    }
    ssa.setIndex(name, idx);
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
  private int makeLvalIndex(String name, FormulaList args, SSAMapBuilder ssa) {
    int idx = ssa.getIndex(name, args);
    if (idx > 0) {
      idx = idx+1;
    } else {
      idx = 2; // AG - IMPORTANT!!! We must start from 2 and
      // not from 1, because this is an assignment,
      // so the SSA index must be fresh. If we use 1
      // here, we will have troubles later when
      // shifting indices
    }
    ssa.setIndex(name, args, idx);
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
  private Formula makeVariable(String var, SSAMapBuilder ssa) {
    int idx = getIndex(var, ssa);
    return fmgr.makeVariable(var, idx);
  }

  /**
   * Create a formula for a given variable with a fresh index for the left-hand
   * side of an assignment.
   * This method does not handle scoping and the NON_DET_VARIABLE!
   */
  private Formula makeFreshVariable(String var, SSAMapBuilder ssa) {
    int idx = makeFreshIndex(var, ssa);
    return fmgr.makeVariable(var, idx);
  }

  private Formula makePointerVariable(IASTIdExpression expr, String function,
      SSAMapBuilder ssa) {
    String variableName = makePointerVariableName(expr, function, ssa);
    return makeVariable(variableName, ssa);
  }

  private String makePointerMask(String scopedId, SSAMapBuilder ssa) {
    String pointerId = "*<" + scopedId + "," + ssa.getIndex(scopedId) + ">";
    return pointerId;
  }

  private String makePointerVariableName(IASTIdExpression expr,
      String function, SSAMapBuilder ssa) {
    String scopedId = scopedIfNecessary(expr, function);
    return makePointerMask(scopedId, ssa);
  }

  private String makeMemoryLocationVariableName(String var) {
    return "&" + var;
  }

  // name has to be scoped already
  private Formula makeAssignment(String name,
          Formula rightHandSide, SSAMapBuilder ssa) {

    Formula lhs = makeFreshVariable(name, ssa);
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
        v = new StatementToFormulaVisitorPointers(function, ssa, constraints);
      } else {
        v = new StatementToFormulaVisitor(function, ssa, constraints);
      }
      edgeFormula = statementEdge.getStatement().accept(v);
      break;
    }

    case ReturnStatementEdge: {
      ReturnStatementEdge returnEdge = (ReturnStatementEdge)edge;
      edgeFormula = makeReturn(returnEdge.getExpression(), function, ssa, constraints);
      break;
    }

    case DeclarationEdge: {
      DeclarationEdge d = (DeclarationEdge)edge;
      edgeFormula = makeDeclaration(d.getDeclSpecifier(), d.isGlobal(), d, function, ssa, constraints);
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

    edgeFormula = constraints.extend(edgeFormula);

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
      Constraints constraints) throws CPATransferException {

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
        String var;
        if (isGlobal) {
          var = varNameWithoutFunction;
        } else {
          var = scoped(varNameWithoutFunction, function);
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
        makeFreshIndex(var, ssa);

        // if there is an initializer associated to this variable,
        // take it into account
        IASTInitializer initializer = edge.getInitializer();
        IASTRightHandSide init = null;

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

          if (isNondetVariable(varNameWithoutFunction)) {
            log(Level.WARNING, getLogMessage("Ignoring initial value of special non-determinism variable " + var, edge));

          } else {
            Formula minit = buildTerm(init, function, ssa, constraints);
            return makeAssignment(var, minit, ssa);
          }
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

      Formula retvarFormula = makeVariable(scoped(VAR_RETURN_NAME, function), ssa);
      IASTExpression e = exp.getLeftHandSide();

      function = ce.getSuccessor().getFunctionName();
      Formula outvarFormula = buildLvalueTerm(e, function, ssa, constraints);
      return fmgr.makeAssignment(outvarFormula, retvarFormula);

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
      Formula actualParam = buildTerm(actualParams.get(i++), callerFunction, ssa, constraints);

      Formula eq = makeAssignment(scoped(formalParamName, calledFunction), actualParam, ssa);

      result = fmgr.makeAnd(result, eq);
    }

    return result;
  }

  private Formula makeReturn(IASTExpression exp, String function,
      SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {
    if (exp == null) {
      // this is a return from a void function, do nothing
      return fmgr.makeTrue();
    } else {

      // we have to save the information about the return value,
      // so that we can use it later on, if it is assigned to
      // a variable. We create a function::__retval__ variable
      // that will hold the return value
      Formula retval = buildTerm(exp, function, ssa, constraints);
      return makeAssignment(scoped(VAR_RETURN_NAME, function), retval, ssa);
    }
  }

  private Formula makeAssume(AssumeEdge assume, String function,
      SSAMapBuilder ssa, Constraints constraints) throws CPATransferException {

    return makePredicate(assume.getExpression(), assume.getTruthAssumption(),
        function, ssa, constraints);
  }

  private Formula makeNotEqual(Formula f1, Formula f2) {
    return fmgr.makeNot(fmgr.makeEqual(f1, f2));
  }

  private Formula buildTerm(IASTRightHandSide exp, String function,
      SSAMapBuilder ssa, Constraints ax) throws UnrecognizedCCodeException {
    return toNumericFormula(exp.accept(new RightHandSideToFormulaVisitor(function, ssa, ax)));
  }

  private Formula buildTerm(IASTExpression exp, String function,
      SSAMapBuilder ssa, Constraints constraints) throws UnrecognizedCCodeException {
    return toNumericFormula(exp.accept(getExpressionVisitor(function, ssa, constraints)));
  }

  private Formula buildLvalueTerm(IASTExpression exp, String function,
      SSAMapBuilder ssa, Constraints constraints) throws UnrecognizedCCodeException {
    return exp.accept(getLvalueVisitor(function, ssa, constraints));
  }

  protected Formula makePredicate(IASTExpression exp, boolean isTrue,
      String function, SSAMapBuilder ssa, Constraints constraints) throws UnrecognizedCCodeException {

    Formula result = toBooleanFormula(exp.accept(getExpressionVisitor(function, ssa, constraints)));

    if (!isTrue) {
      result = fmgr.makeNot(result);
    }
    return result;
  }

  private ExpressionToFormulaVisitor getExpressionVisitor(String pFunction,
      SSAMapBuilder pSsa, Constraints pCo) {
    if (lvalsAsUif) {
      return new ExpressionToFormulaVisitorUIF(pFunction, pSsa, pCo);
    } else if (handlePointerAliasing) {
      return new ExpressionToFormulaVisitorPointers(pFunction, pSsa, pCo);
    } else {
      return new ExpressionToFormulaVisitor(pFunction, pSsa, pCo);
    }
  }

  private LvalueVisitor getLvalueVisitor(String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
    if (lvalsAsUif) {
      return new LvalueVisitorUIF(pFunction, pSsa, pCo);
    } else if (handlePointerAliasing) {
      return new LvalueVisitorPointers(pFunction, pSsa, pCo);
    } else {
      return new LvalueVisitor(pFunction, pSsa, pCo);
    }
  }

  private boolean isPointerDereferencing(IASTExpression e) {
    return (e instanceof IASTUnaryExpression)
        && ((IASTUnaryExpression) e).getOperator() == UnaryOperator.STAR;
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

  private List<String> getAllMemoryLocationsFromSsaMap(SSAMapBuilder ssa) {
    List<String> memoryLocations = new LinkedList<String>();
    Set<String> ssaVariables = ssa.build().allVariables();

    Pattern p = Pattern.compile("&.*");

    for (String variable : ssaVariables) {
      if (p.matcher(variable).matches()) {
        memoryLocations.add(variable);
      }
    }

    return memoryLocations;
  }

  private MachineModel getMachineModel() {
    if (mMachineModel == null) {
      if (this.machineModel.equals("32-Linux")) {
        mMachineModel = new MachineModel32Linux();
      } else if (this.machineModel.equals("64-Linux")) {
        mMachineModel = new MachineModel64Linux();
      } else {
        logger.log(Level.SEVERE, "Unknown machine model."
            + " Falling back to default, i.e., a 32 bit Linux machine.");
        mMachineModel = new MachineModel32Linux();
      }
    }

    return mMachineModel;
  }

  /**
   * This class tracks constraints which are created during AST traversal but
   * cannot be applied at the time of creation.
   */
  protected class Constraints {

    private Formula constraints = fmgr.makeTrue();

    public Constraints() {}

    private void addConstraint(Formula pCo) {
      if (constraints.isTrue()) {
        constraints = pCo;
      } else {
        constraints = fmgr.makeAnd(constraints, pCo);
      }
    }

    /**
     * Applies the constraints to a given Formula and returns the extended Formula.
     * This constraint container is cleared and set to TRUE.
     */
    public Formula extend(Formula f) {
      if (constraints.isTrue()) {
        return f;
      } else {
        Formula extendedFormula = fmgr.makeAnd(f, constraints);
        constraints = fmgr.makeTrue();

        return extendedFormula;
      }
    }
  }

  private class ExpressionToFormulaVisitor extends DefaultExpressionVisitor<Formula, UnrecognizedCCodeException> {

    protected final String        function;
    protected final SSAMapBuilder ssa;
    protected final Constraints   constraints;

    public ExpressionToFormulaVisitor(String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
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
          throw new UnrecognizedCCodeException("Unknown binary operator", null, exp);
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

      String var = idExp.getName();

      if (isNondetVariable(var)) {
        // on every read access to special non-determininism variable use a fresh instance
        return makeFreshVariable(NONDET_VARIABLE, ssa);

      } else {
        return makeVariable(scopedIfNecessary(idExp, function), ssa);
      }

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
      String literal = lexp.getRawSignature();
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
        throw new UnrecognizedCCodeException("Unknown unary operator", null, exp);
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
        IASTSimpleDeclSpecifier lSimpleDeclSpec =
            (IASTSimpleDeclSpecifier) pIType;
        MachineModel lMachineModel = getMachineModel();

        switch (lSimpleDeclSpec.getType()) {
        case UNSPECIFIED: {
          return visitDefault(pExp);
        }
        case VOID: {
          return fmgr.makeNumber(lMachineModel.getSizeofVoid());
        }
        case BOOL: {
          return fmgr.makeNumber(lMachineModel.getSizeofBool());
        }
        case CHAR: {
          return fmgr.makeNumber(lMachineModel.getSizeofChar());
        }
        case INT: {
          if (lSimpleDeclSpec.isLongLong()) {
            return fmgr.makeNumber(lMachineModel.getSizeofLongLongInt());
          } else if (lSimpleDeclSpec.isLong()) {
            return fmgr.makeNumber(lMachineModel.getSizeofLongInt());
          } else if (lSimpleDeclSpec.isShort()) {
            return fmgr.makeNumber(lMachineModel.getSizeofShort());
          } else {
            return fmgr.makeNumber(lMachineModel.getSizeofInt());
          }
        }
        case FLOAT: {
          return fmgr.makeNumber(lMachineModel.getSizeofFloat());
        }
        case DOUBLE: {
          if (lSimpleDeclSpec.isLong()) {
            return fmgr.makeNumber(lMachineModel.getSizeofLongDouble());
          } else {
            return fmgr.makeNumber(lMachineModel.getSizeofDouble());
          }
        }
        default:
          return visitDefault(pExp);
        }
      } else {
        return visitDefault(pExp);
      }
    }
  }

  private class ExpressionToFormulaVisitorUIF extends ExpressionToFormulaVisitor {

    public ExpressionToFormulaVisitorUIF(String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      super(pFunction, pSsa, pCo);
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

    public ExpressionToFormulaVisitorPointers(String pFunction,
        SSAMapBuilder pSsa, Constraints pCo) {
      super(pFunction, pSsa, pCo);
    }

    @Override
    public Formula visit(IASTBinaryExpression exp)
        throws UnrecognizedCCodeException {
      if (containsPointerDereferencing(exp)) {
        IASTExpression e1 = exp.getOperand1();
        Formula me1 = getExprFormula(e1);

        IASTExpression e2 = exp.getOperand2();
        Formula me2 = getExprFormula(e2);

        Formula exprFormula = makeCompoundFormula(exp, me1, me2);
        return exprFormula;
      } else {
        return super.visit(exp);
      }
    }

    @Override
    public Formula visit(IASTUnaryExpression exp)
        throws UnrecognizedCCodeException {
      UnaryOperator op = exp.getOperator();

      switch (op) {
      case AMPER:
        return makeAddressVariable(exp, function);

      case STAR:
        if (exp.getOperand() instanceof IASTIdExpression) {
          return makePointerVariable((IASTIdExpression) exp.getOperand(), function, ssa);
        }
        throw new UnrecognizedCCodeException(null, exp);

      default:
        return super.visit(exp);
      }
    }

    private Formula makeAddressVariable(IASTExpression exp, String function)
        throws UnrecognizedCCodeException {

      if (exp instanceof IASTUnaryExpression) {
        IASTExpression operand = ((IASTUnaryExpression) exp).getOperand();
        UnaryOperator op = ((IASTUnaryExpression) exp).getOperator();

        if (op != UnaryOperator.AMPER || !(operand instanceof IASTIdExpression)) {
          return super.visitDefault(exp);
        }

        return makeMemoryLocationVariable((IASTIdExpression) operand, function);

      } else {
        // not yet implemented: pointer arithmetic
        log(Level.WARNING, exp.getRawSignature() + " is not yet implemented");
        return fmgr.makeTrue();
      }
    }

    private Formula makeMemoryLocationVariable(IASTIdExpression exp, String function) {
      String addressVariable = makeMemoryLocationVariableName(scopedIfNecessary(exp, function));

      // a variable address is always initialized, not 0 and cannot change
      if (ssa.getIndex(addressVariable) == VARIABLE_UNSET) {
        List<String> oldMemoryLocations = getAllMemoryLocationsFromSsaMap(ssa);

        ssa.setIndex(addressVariable, VARIABLE_UNINITIALIZED + 1);
        Formula newMemoryLocation = makeVariable(addressVariable, ssa);

        // a memory address that is unknown is different from all previously known addresses
        for (String memoryLocation : oldMemoryLocations) {
          Formula oldMemoryLocation = makeVariable(memoryLocation, ssa);
          Formula addressInequality = fmgr.makeNot(fmgr.makeEqual(newMemoryLocation, oldMemoryLocation));

          constraints.addConstraint(addressInequality);
        }

        // a variable address is not 0
        Formula notZero = makeNotEqual(newMemoryLocation, fmgr.makeNumber(0));
        constraints.addConstraint(notZero);
      }

      return makeVariable(addressVariable, ssa);
    }

    private Formula makeCompoundFormula(IASTBinaryExpression exp, Formula me1,
        Formula me2) throws UnrecognizedCCodeException {
      switch (exp.getOperator()) {
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
        throw new UnrecognizedCCodeException(exp.getRawSignature(), null, exp);
      }
    }

    private Formula getExprFormula(IASTExpression exp)
        throws UnrecognizedCCodeException {
      if (isPointerDereferencing(exp)) {

        IASTExpression operand = ((IASTUnaryExpression) exp).getOperand();
        if (operand instanceof IASTIdExpression) {
          return makePointerVariable((IASTIdExpression) operand, function, ssa);
        } else {
          throw new UnrecognizedCCodeException(exp.getRawSignature(), null, exp);
        }
      }

      return exp.accept(this);
    }

    private boolean containsPointerDereferencing(IASTBinaryExpression expr) {
      IASTExpression e1 = expr.getOperand1();
      IASTExpression e2 = expr.getOperand2();

      return isPointerDereferencing(e1) || isPointerDereferencing(e2);
    }
  }

  private class RightHandSideToFormulaVisitor extends
      ForwardingExpressionVisitor<Formula, UnrecognizedCCodeException>
      implements RightHandSideVisitor<Formula, UnrecognizedCCodeException> {

    protected final String        function;
    protected final SSAMapBuilder ssa;
    protected final Constraints   constraints;

    public RightHandSideToFormulaVisitor(String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      super(getExpressionVisitor(pFunction, pSsa, pCo));
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
          throw new UnsupportedCCodeException(UNSUPPORTED_FUNCTIONS.get(func), fexp);

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
        func = "<func>{" + fn.getRawSignature() + "}";
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

    public StatementToFormulaVisitor(String pFunction, SSAMapBuilder pSsa, Constraints pConstraints) {
      super(pFunction, pSsa, pConstraints);
    }

    @Override
    public Formula visit(IASTExpressionStatement pIastExpressionStatement) {
      // side-effect free statement, ignore
      return fmgr.makeTrue();
    }

    public Formula visit(IASTAssignment assignment) throws UnrecognizedCCodeException {
      Formula rightVariable = assignment.getRightHandSide().accept(this);
      Formula r = toNumericFormula(rightVariable);
      Formula l = buildLvalueTerm(assignment.getLeftHandSide(), function, ssa, constraints);
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
        SSAMapBuilder pSsa, Constraints pConstraints) {
      super(pFunction, pSsa, pConstraints);
    }

    @Override
    public Formula visit(IASTFunctionCallExpression fexp) throws UnrecognizedCCodeException {
      // handle malloc
      IASTExpression fn = fexp.getFunctionNameExpression();
      if (fn instanceof IASTIdExpression) {
        // TODO: cil allows sizeof(int) in malloc calls
        String fName = ((IASTIdExpression)fn).getName();

        if (memoryAllocationFunctions.contains(fName)) {
          // TODO: for now all parameters are ignored

          List<String> memoryLocations = getAllMemoryLocationsFromSsaMap(ssa);

          String mallocVarName = makeFreshMallocVariableName();
          Formula mallocVar = makeVariable(mallocVarName, ssa);

          // we must distinguish between two cases:
          // either the result is 0 or it is different from all other memory locations
          // (m != 0) => for all memory locations n: m != n
          Formula ineq = fmgr.makeTrue();
          for (String ml : memoryLocations) {
            Formula n = makeVariable(ml, ssa);

            Formula notEqual = makeNotEqual(n, mallocVar);
            ineq = fmgr.makeAnd(notEqual, ineq);
          }

          Formula nullFormula = fmgr.makeNumber(0);
          Formula implication = makeImplication(makeNotEqual(mallocVar, nullFormula), ineq);

          constraints.addConstraint(implication);
          return mallocVar;
        }
      }

      return super.visit(fexp);
    }

    @Override
    public Formula visit(IASTAssignment assignment)
        throws UnrecognizedCCodeException {
      IASTExpression left = assignment.getLeftHandSide();

      if (left instanceof IASTIdExpression) {
        return handleDirectAssignment(assignment);

      } else if (left instanceof IASTUnaryExpression
          && ((IASTUnaryExpression) left).getOperator() == UnaryOperator.STAR) {
        return handleIndirectAssignment(assignment);

      } else {
        throw new UnrecognizedCCodeException(
            "left hand side of assignment unsupported: ", null, left);
      }
    }

    private Formula handleIndirectAssignment(IASTAssignment pAssignment)
        throws UnrecognizedCCodeException {
      assert (pAssignment.getLeftHandSide() instanceof IASTUnaryExpression);

      IASTUnaryExpression l = (IASTUnaryExpression) pAssignment.getLeftHandSide();
      assert (l.getOperator() == UnaryOperator.STAR);

      IASTExpression lOperand = l.getOperand();
      if (!(lOperand instanceof IASTIdExpression)) {
        throw new UnrecognizedCCodeException("left hand side unknown", null, lOperand);
      }

      String leftSideVarName = scopedIfNecessary((IASTIdExpression) lOperand, function);
      Formula oldPVar = makePointerVariable((IASTIdExpression) lOperand, function, ssa);

      Formula as = super.visit(pAssignment);

      Formula newPVar = makePointerVariable((IASTIdExpression) lOperand, function, ssa);

      // update all variables (they might have a new value)
      List<String> vars = getAllVariableNames();
      for (String v : vars) {
        if (!v.equals(leftSideVarName)) {
          String pVarName = makePointerMask(v, ssa);
          if (ssa.getIndex(pVarName) != VARIABLE_UNSET) {

            Formula oldVariable = makeVariable(v, ssa);
            Formula newVariable = makeFreshVariable(v, ssa);

            Formula condition = fmgr.makeEqual(oldVariable, oldPVar);
            Formula equality = fmgr.makeAssignment(newVariable, newPVar);
            Formula indexUpdate = fmgr.makeAssignment(newVariable, oldVariable);

            Formula variableUpdate = fmgr.makeIfThenElse(condition, equality, indexUpdate);
            constraints.addConstraint(variableUpdate);
          } else {

            // TODO


          }
        }
      }

      // TODO update ssa map

      return as;
    }

    private Formula handleDirectAssignment(IASTAssignment assignment)
        throws UnrecognizedCCodeException {
      IASTExpression left = assignment.getLeftHandSide();
      IASTRightHandSide right = assignment.getRightHandSide();

      Formula oldVariable = null;

      boolean doPointerUpdate = requiresPointerUpdates(assignment);
      if (doPointerUpdate) {
        oldVariable = leftHandSideIdToFormula(left);
      }

      Formula assignmentFormula = super.visit(assignment);

      if (isPointerAssignment(assignment)) {
        Formula leftVariable = makeLeftVariable(left);
        Formula rightVariable = makeRightVariable(right);

        if (rightVariable != null) {
          Formula eq = fmgr.makeAssignment(leftVariable, rightVariable);
          assignmentFormula = fmgr.makeAnd(assignmentFormula, eq);

          removeOldPointerVariablesFromSsaMap(makeLeftVariableName(left));
        }
      }

      if (doPointerUpdate) {
        Formula newVariable = leftHandSideIdToFormula(left);
        String rightVariableName = null;

        if (right instanceof IASTIdExpression) {
          rightVariableName =
              scopedIfNecessary((IASTIdExpression) right, function);
        } else if (right instanceof IASTUnaryExpression
            && ((IASTUnaryExpression) right).getOperator() == UnaryOperator.AMPER
            && ((IASTUnaryExpression) right).getOperand() instanceof IASTIdExpression) {

          IASTExpression r = ((IASTUnaryExpression) right).getOperand();
          rightVariableName = scopedIfNecessary((IASTIdExpression) r, function);
        }

        // update all pointers:
        // if a pointer is aliased to the newly assigned location,
        // update that pointer to reflect the new aliasing,
        // otherwise only update the index
        for (String ssaVariable : getAllPointerVariablesFromSsaMap()) {

          String ssaVariableName =
              getVariableNameFromPointerVariable(ssaVariable);
          if (!ssaVariableName.equals(rightVariableName)) {

            Formula oldSsaVariable = makeVariable(ssaVariable, ssa);
            Formula newSsaVariable = makeFreshVariable(ssaVariable, ssa);

            Formula condition = fmgr.makeEqual(oldVariable, oldSsaVariable);
            Formula ifEqual = fmgr.makeAssignment(newSsaVariable, newVariable);
            Formula ifNotEqual = fmgr.makeAssignment(newSsaVariable, oldSsaVariable);

            Formula ifThenElse =  fmgr.makeIfThenElse(condition, ifEqual, ifNotEqual);
            assignmentFormula = fmgr.makeAnd(assignmentFormula, ifThenElse);
          }
        }
      }

      return assignmentFormula;
    }

    private Formula makeImplication(Formula p, Formula q) {
      Formula left = fmgr.makeNot(p);
      return fmgr.makeOr(left, q);
    }

    private String makeFreshMallocVariableName() {
      // TODO: find a better way (without using the SSA map)
      final String mallocVariableName = "#malloc";

      int idx = ssa.getIndex(mallocVariableName);

      if (idx == VARIABLE_UNSET) {
        idx = 2;
      }

      ssa.setIndex(mallocVariableName, idx + 1);

      return "&#" + idx;
    }

    /**
     * Removes all pointer variables belonging to a given variable from the SSA map
     * that are no longer valid. Validity of an entry expires, when the pointer
     * variable belongs to a variable with an old index.
     *
     * @param newPVar The variable name of the new pointer variable.
     */
    private void removeOldPointerVariablesFromSsaMap(String newPVar) {
      String newVar = getVariableNameFromPointerVariable(newPVar);

      List<String> pointerVariables = getAllPointerVariablesFromSsaMap();
      for (String pointerVar : pointerVariables) {
        String oldVar = getVariableNameFromPointerVariable(pointerVar);
        if (!pointerVar.equals(newPVar) && oldVar.equals(newVar)) {
          ssa.deleteVariable(pointerVar);
        }
      }
    }

    private boolean isStaticallyDeclaredPointer(IType expr) {
      String declaration = expr.toASTString();
      return Pattern.matches(".*\\*", declaration);
    }

    private String getVariableNameFromPointerVariable(String pointerVariable) {
      assert (isPointerVariable(pointerVariable));

      return pointerVariable.substring(2, pointerVariable.indexOf(','));
    }

    private Formula makeLeftVariable(IASTExpression left) {
      String leftPointerVariableName = makeLeftVariableName(left);

      int idx = makeFreshIndex(leftPointerVariableName, ssa);
      if (idx == VARIABLE_UNINITIALIZED) {
        // conform with the rule, that initialized variables are always larger then 2
        makeFreshIndex(leftPointerVariableName, ssa);
      }

      Formula leftVariable =
          makePointerVariable((IASTIdExpression) left, function, ssa);
      return leftVariable;
    }

    private String makeLeftVariableName(IASTExpression left) {
      IASTIdExpression l = (IASTIdExpression) left;
      String leftPointerVariableName =
          makePointerVariableName(l, function, ssa);
      return leftPointerVariableName;
    }

    private Formula makeRightVariable(IASTRightHandSide right) throws UnrecognizedCCodeException {
      Formula rightVariable = null;

      if (right instanceof IASTIdExpression) {
        rightVariable = makePointerVariable((IASTIdExpression) right, function, ssa);

      } else if (right instanceof IASTUnaryExpression
          && ((IASTUnaryExpression) right).getOperator() == UnaryOperator.AMPER
          && ((IASTUnaryExpression) right).getOperand() instanceof IASTIdExpression) {
        IASTUnaryExpression r = (IASTUnaryExpression) right;

        // the pointer points to the value of expr
        IASTIdExpression rId = (IASTIdExpression) r.getOperand();
        rightVariable = makeVariable(scopedIfNecessary(rId, function), ssa);

      } else if (right instanceof IASTUnaryExpression
          && ((IASTUnaryExpression) right).getOperator() == UnaryOperator.STAR
          && ((IASTUnaryExpression) right).getOperand() instanceof IASTIdExpression) {
        IASTUnaryExpression r = (IASTUnaryExpression) right;
        IASTIdExpression rightId = (IASTIdExpression) r.getOperand();
        rightVariable = makePointerVariable(rightId, function, ssa);

      } else if (right instanceof IASTFunctionCallExpression) {
        // treatment of malloc:
        // memory location can be anything, so no value is set,
        // instead only the index of the variable is adjusted

        // TODO: special treatment is needed for functions that allocate and
        // initialize memory (like kzalloc/kcalloc)
      } else {
        // unsupported: pointer arithmetic
        logger.log(Level.WARNING, right.getRawSignature()
            + " not supported, analysis may be imprecise");
      }
      return rightVariable;
    }

    private Formula leftHandSideIdToFormula(IASTExpression left)
        throws UnrecognizedCCodeException {
      if (left instanceof IASTIdExpression) {
        String leftVarName =
            scopedIfNecessary((IASTIdExpression) left, function);
        return makeVariable(leftVarName, ssa);

//      } else if (left instanceof IASTUnaryExpression
//          && ((IASTUnaryExpression) left).getOperator() == UnaryOperator.STAR
//          && ((IASTUnaryExpression) left).getOperand() instanceof IASTIdExpression) {
//
//        // not handled here

      } else {
        throw new UnrecognizedCCodeException(null, left);
      }
    }

    private boolean requiresPointerUpdates(IASTAssignment assignment) {
      // unless pointer is uninitialized
      IASTExpression left = assignment.getLeftHandSide();

      if (left instanceof IASTIdExpression) {

        String varName = scopedIfNecessary(((IASTIdExpression) left), function);
        if (ssa.getIndex(varName) == VARIABLE_UNINITIALIZED) {
          return false;
        }

        String pointerVarName = makePointerVariableName((IASTIdExpression) left, function, ssa);
        boolean isInSsaMap = ssa.getIndex(pointerVarName) != VARIABLE_UNSET;
        if (!isInSsaMap) {
          return !isStaticallyDeclaredPointer(assignment.getLeftHandSide().getExpressionType());
        }
      }

      return true;
    }

    private boolean isPointerAssignment(IASTAssignment assignment) {
      // case 1: left side is a pointer
      if (isStaticallyDeclaredPointer(assignment.getLeftHandSide()
          .getExpressionType())) { return true; }

      // case 2: right side may be memory location
      IASTRightHandSide right = assignment.getRightHandSide();
      if (right instanceof IASTUnaryExpression
          && ((IASTUnaryExpression) right).getOperator() == UnaryOperator.AMPER) {
        return true;
      } else if (right instanceof IASTIdExpression) {
        // create pointer identifier
        String pointerVariable =
            makePointerVariableName((IASTIdExpression) right, function, ssa);
        int pointerVariableIndex = ssa.getIndex(pointerVariable);

        if (pointerVariableIndex != VARIABLE_UNSET) { return true; }
      }

      return false;
    }

    private boolean isPointerVariable(String variable) {
      final Pattern pointerVariablePattern = Pattern.compile("\\*<.*>");
      return pointerVariablePattern.matcher(variable).matches();
    }

    private List<String> getAllVariableNames() {
      Set<String> allEntries = ssa.build().allVariables();

      List<String> allVariables = new LinkedList<String>();
      Pattern p = Pattern.compile("[a-zA-Z].*");
      for (String var : allEntries) {
        if (p.matcher(var).matches()) {
          allVariables.add(var);
        }
      }

      return allVariables;
    }

    private List<String> getAllPointerVariablesFromSsaMap() {
      List<String> pointerVariables = new LinkedList<String>();
      Set<String> ssaVariables = ssa.build().allVariables();

      for (String variable : ssaVariables) {
        if (isPointerVariable(variable)) {
          pointerVariables.add(variable);
        }
      }

      return pointerVariables;
    }
  }

  private class LvalueVisitor extends
      DefaultExpressionVisitor<Formula, UnrecognizedCCodeException> {

    protected final String        function;
    protected final SSAMapBuilder ssa;
    protected final Constraints   constraints;

    public LvalueVisitor(String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      function = pFunction;
      ssa = pSsa;
      constraints = pCo;
    }

    @Override
    protected Formula visitDefault(IASTExpression exp) throws UnrecognizedCCodeException {
      throw new UnrecognizedCCodeException("Unknown lvalue", null, exp);
    }

    @Override
    public Formula visit(IASTIdExpression idExp) {
      String var = idExp.getName();

      if (isNondetVariable(var)) {
        log(Level.WARNING, "Assignment to special non-determinism variable" + var + "will be ignored.");
      }
      var = scopedIfNecessary(idExp, function);
      return makeFreshVariable(var, ssa);
    }

    private Formula makeUIF(IASTExpression exp) {
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

    public LvalueVisitorUIF(String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      super(pFunction, pSsa, pCo);
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
        throw new UnrecognizedCCodeException("Invalid unary operator for lvalue", null, uExp);
      }
      Formula term = buildTerm(operand, function, ssa, constraints);

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
      Formula term = buildTerm(owner, function, ssa, constraints);

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
      Formula aterm = buildTerm(arrexp, function, ssa, constraints);
      Formula sterm = buildTerm(subexp, function, ssa, constraints);

      String ufname = OP_ARRAY_SUBSCRIPT;
      FormulaList args = fmgr.makeList(aterm, sterm);
      int idx = makeLvalIndex(ufname, args, ssa);

      return fmgr.makeUIF(ufname, args, idx);

    }
  }

  private class LvalueVisitorPointers extends LvalueVisitor {
    public LvalueVisitorPointers(String pFunction, SSAMapBuilder pSsa, Constraints pCo) {
      super(pFunction, pSsa, pCo);
    }

    @Override
    public Formula visit(IASTUnaryExpression pE) throws UnrecognizedCCodeException {
      if (pE.getOperator() != UnaryOperator.STAR
          || !(pE.getOperand() instanceof IASTIdExpression)) {
        throw new UnrecognizedCCodeException(null, pE);
      }

      IASTIdExpression pId = (IASTIdExpression) pE.getOperand();

      String pVarName = makePointerVariableName(pId, function, ssa);
      makeFreshIndex(pVarName, ssa);

      return makePointerVariable(pId, function, ssa);
    }
  }


  private abstract class MachineModel {
    // numeric types
    protected int     mSizeofShort;
    protected int     mSizeofInt;
    protected int     mSizeofLongInt;
    protected int     mSizeofLongLongInt;
    protected int     mSizeofFloat;
    protected int     mSizeofDouble;
    protected int     mSizeofLongDouble;

    // other
    protected int     mSizeofVoid;
    protected int     mSizeofBool;

    // according to ANSI C, sizeof(char) is always 1
    private final int mSizeofChar = 1;

    public int getSizeofShort() {
      return mSizeofShort;
    }

    public int getSizeofInt() {
      return mSizeofInt;
    }

    public int getSizeofLongInt() {
      return mSizeofLongInt;
    }

    public int getSizeofLongLongInt() {
      return mSizeofLongLongInt;
    }

    public int getSizeofFloat() {
      return mSizeofFloat;
    }

    public int getSizeofDouble() {
      return mSizeofDouble;
    }

    public int getSizeofLongDouble() {
      return mSizeofLongDouble;
    }

    public int getSizeofVoid() {
      return mSizeofVoid;
    }

    public int getSizeofBool() {
      return mSizeofBool;
    }

    public int getSizeofChar() {
      return mSizeofChar;
    }
  }

  /**
   * Machine model representing a 32bit Linux machine
   */
  private class MachineModel32Linux extends MachineModel {

    public MachineModel32Linux() {
      // numeric types
      mSizeofShort = 2;
      mSizeofInt = 4;
      mSizeofLongInt = 4;
      mSizeofLongLongInt = 8;
      mSizeofFloat = 4;
      mSizeofDouble = 8;
      mSizeofLongDouble = 12;

      // other
      mSizeofVoid = 1;
      mSizeofBool = 1;
    }

  }

  /**
   * Machine model representing a 64bit Linux machine
   */
  private class MachineModel64Linux extends MachineModel {

    public MachineModel64Linux() {
      // numeric types
      mSizeofShort = 2;
      mSizeofInt = 4;
      mSizeofLongInt = 8;
      mSizeofLongLongInt = 8;
      mSizeofFloat = 4;
      mSizeofDouble = 8;
      mSizeofLongDouble = 16;

      // other
      mSizeofVoid = 1;
      mSizeofBool = 1;
    }
  }
}
