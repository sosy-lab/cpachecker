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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTStringLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaList;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

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
  private Set<String> nondetFunctions = ImmutableSet.of("int_nondet", "malloc", "nondet_int", "random");

  // list of functions that are pure (no side-effects)
  private static final Set<String> PURE_EXTERNAL_FUNCTIONS
      = ImmutableSet.of("__assert_fail", "printf", "puts");

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

  public CtoFormulaConverter(Configuration config, FormulaManager fmgr, LogManager logger) throws InvalidConfigurationException {
    config.inject(this, CtoFormulaConverter.class);

    this.fmgr = fmgr;
    this.logger = logger;
  }

  private void warnUnsafeVar(IASTExpression exp) {
    log(Level.WARNING, "Unhandled expression treated as free variable", exp);
  }

  private void log(Level level, String msg, IASTNode astNode) {
    msg = "Line " + astNode.getFileLocation().getStartingLineNumber()
        + ": " + msg
        + ": " + astNode.getRawSignature();

    if (printedWarnings.add(msg)) {
      logger.log(level, 1, msg);
    }
  }

  private void log(Level level, String msg, CFAEdge edge) {
    msg = "Line " + edge.getLineNumber()
        + ": " + msg
        + ": " + edge.getRawStatement();

    if (printedWarnings.add(msg)) {
      logger.log(level, 1, msg);
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
   * Produces a fresh new SSA index for the left-hand side of an assignment
   * and updates the SSA map.
   */
  private int makeLvalIndex(String name, SSAMapBuilder ssa) {
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
      logger.log(Level.ALL, "DEBUG_3",
          "WARNING: Auto-instantiating variable: ", var);
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
    int idx = makeLvalIndex(var, ssa);
    return fmgr.makeVariable(var, idx);
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

    Formula edgeFormula;
    switch (edge.getEdgeType()) {
    case StatementEdge: {
      StatementEdge statementEdge = (StatementEdge)edge;
      StatementToFormulaVisitor v = new StatementToFormulaVisitor(function, ssa);
      edgeFormula = statementEdge.getStatement().accept(v);
      break;
    }

    case ReturnStatementEdge: {
      ReturnStatementEdge returnEdge = (ReturnStatementEdge)edge;
      edgeFormula = makeReturn(returnEdge.getExpression(), function, ssa);
      break;
    }

    case DeclarationEdge: {
      DeclarationEdge d = (DeclarationEdge)edge;
      edgeFormula = makeDeclaration(d.getDeclSpecifier(), d.isGlobal(), d, function, ssa);
      break;
    }

    case AssumeEdge: {
      edgeFormula = makeAssume((AssumeEdge)edge, function, ssa);
      break;
    }

    case BlankEdge: {
      assert false : "Handled above";
      edgeFormula = fmgr.makeTrue();
      break;
    }

    case FunctionCallEdge: {
      edgeFormula = makeFunctionCall((FunctionCallEdge)edge, function, ssa);
      break;
    }

    case FunctionReturnEdge: {
      // get the expression from the summary edge
      CFANode succ = edge.getSuccessor();
      CallToReturnEdge ce = succ.getEnteringSummaryEdge();
      edgeFormula = makeExitFunction(ce, function, ssa);
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

  private Formula makeDeclaration(IType spec,
      boolean isGlobal, DeclarationEdge edge,
      String function, SSAMapBuilder ssa) throws CPATransferException {

    if (spec instanceof IASTFunctionTypeSpecifier) {
      return fmgr.makeTrue();

    } else if (spec instanceof IASTCompositeTypeSpecifier) {
      // this is the declaration of a struct, just ignore it...
      log(Level.ALL, "Ignoring declaration", edge);
      return fmgr.makeTrue();

    } else if (spec instanceof IASTSimpleDeclSpecifier ||
               spec instanceof IASTEnumerationSpecifier ||
               spec instanceof IASTElaboratedTypeSpecifier ||
               spec instanceof IASTNamedTypeSpecifier ||
               spec instanceof IASTArrayTypeSpecifier ||
               spec instanceof IASTPointerTypeSpecifier) {

      if (edge.getStorageClass() == StorageClass.TYPEDEF) {
        log(Level.ALL, "Ignoring typedef", edge);
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

        // if there is an initializer associated to this variable,
        // take it into account
        IASTInitializer initializer = edge.getInitializer();
        IASTExpression init = null;

        if (initializer == null) {
          if (initAllVars) {
            // auto-initialize variables to zero
            logger.log(Level.ALL, "AUTO-INITIALIZING VAR: ", var);
            init = Defaults.forType(spec, null);
          }

        } else if (initializer instanceof IASTInitializerExpression) {
          init = ((IASTInitializerExpression)initializer).getExpression();

        } else {
          log(Level.ALL, "Ignoring unsupported initializer", initializer);
        }

        if (init != null) {
          // initializer value present

          if (isNondetVariable(varNameWithoutFunction)) {
            log(Level.WARNING, "Assignment to special non-determinism variable " + var + " will be ignored.", edge);

          } else {
            Formula minit = buildTerm(init, function, ssa);
            Formula mvar = makeFreshVariable(var, ssa);
            return fmgr.makeAssignment(mvar, minit);
          }
        } else {
          // just increment index of variable in SSAMap
          // (a declaration contains an implicit assignment, even without initializer)
          makeLvalIndex(var, ssa);
        }
      }
      return fmgr.makeTrue();

    } else {
      throw new UnrecognizedCFAEdgeException(edge);
    }
  }

  private Formula makeExitFunction(CallToReturnEdge ce, String function,
      SSAMapBuilder ssa) throws CPATransferException {

    IASTFunctionCall retExp = ce.getExpression();
    if (retExp instanceof IASTFunctionCallStatement) {
      // this should be a void return, just do nothing...
      return fmgr.makeTrue();

    } else if (retExp instanceof IASTFunctionCallAssignmentStatement) {
      IASTFunctionCallAssignmentStatement exp = (IASTFunctionCallAssignmentStatement)retExp;

      Formula retvarFormula = makeVariable(scoped(VAR_RETURN_NAME, function), ssa);
      IASTExpression e = exp.getLeftHandSide();

      function = ce.getSuccessor().getFunctionName();
      Formula outvarFormula = buildLvalueTerm(e, function, ssa);
      return fmgr.makeAssignment(outvarFormula, retvarFormula);

    } else {
      throw new UnrecognizedCFAEdgeException("UNKNOWN FUNCTION EXIT EXPRESSION: " + ce.getRawStatement());
    }
  }

  private Formula makeFunctionCall(FunctionCallEdge edge,
      String callerFunction, SSAMapBuilder ssa) throws CPATransferException {

      List<IASTExpression> actualParams = edge.getArguments();

      FunctionDefinitionNode fn = edge.getSuccessor();
      List<IASTParameterDeclaration> formalParams = fn.getFunctionParameters();

      if (formalParams.size() != actualParams.size()) {
        throw new UnrecognizedCCodeException("Number of parameters on function call does not match function definition", edge);
      }

      String calledFunction = fn.getFunctionName();

      int i = 0;
      Formula result = fmgr.makeTrue();
      for (IASTParameterDeclaration formalParam : formalParams) {
        // get formal parameter name
        String formalParamName = formalParam.getName().toString();
        assert(!formalParamName.isEmpty()) : edge;

        if (formalParam.getDeclSpecifier() instanceof IASTPointerTypeSpecifier) {
          log(Level.WARNING, "Ignoring the semantics of pointer for parameter " + formalParamName,
              fn.getFunctionDefinition());
        }

        // get value of actual parameter
        Formula actualParam = buildTerm(actualParams.get(i++), callerFunction, ssa);

        Formula eq = makeAssignment(scoped(formalParamName, calledFunction), actualParam, ssa);

        result = fmgr.makeAnd(result, eq);
      }

      return result;
  }

  private Formula makeReturn(IASTExpression exp, String function, SSAMapBuilder ssa)
      throws CPATransferException {
    if (exp == null) {
      // this is a return from a void function, do nothing
      return fmgr.makeTrue();
    } else {

      // we have to save the information about the return value,
      // so that we can use it later on, if it is assigned to
      // a variable. We create a function::__retval__ variable
      // that will hold the return value
      Formula retval = buildTerm(exp, function, ssa);
      return makeAssignment(scoped(VAR_RETURN_NAME, function), retval, ssa);
    }
  }

  private Formula makeAssume(AssumeEdge assume,
      String function, SSAMapBuilder ssa) throws CPATransferException {

    return makePredicate(assume.getExpression(), assume.getTruthAssumption(), function, ssa);
  }

  private Formula buildTerm(IASTExpression exp, String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {
    return toNumericFormula(exp.accept(getExpressionVisitor(function, ssa)));
  }

  private Formula buildLvalueTerm(IASTExpression exp, String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {
    return exp.accept(getLvalueVisitor(function, ssa));
  }

  protected Formula makePredicate(IASTExpression exp, boolean isTrue,
      String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {

    Formula result = toBooleanFormula(exp.accept(getExpressionVisitor(function, ssa)));

    if (!isTrue) {
      result = fmgr.makeNot(result);
    }
    return result;
  }

  private ExpressionToFormulaVisitor getExpressionVisitor(String pFunction, SSAMapBuilder pSsa) {
    if (lvalsAsUif) {
      return new ExpressionToFormulaVisitorUIF(pFunction, pSsa);
    } else {
      return new ExpressionToFormulaVisitor(pFunction, pSsa);
    }
  }

  private LvalueVisitor getLvalueVisitor(String pFunction, SSAMapBuilder pSsa) {
    if (lvalsAsUif) {
      return new LvalueVisitorUIF(pFunction, pSsa);
    } else {
      return new LvalueVisitor(pFunction, pSsa);
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

  private class ExpressionToFormulaVisitor extends DefaultExpressionVisitor<Formula, UnrecognizedCCodeException> {

    private final String function;
    protected final SSAMapBuilder ssa;

    public ExpressionToFormulaVisitor(String pFunction, SSAMapBuilder pSsa) {
      function = pFunction;
      ssa = pSsa;
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
      logger.log(Level.ALL, "DEBUG_3", "IGNORING TYPE CAST:",
          cexp.getRawSignature());
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
      case SIZEOF:
        return visitDefault(exp);

      default:
        throw new UnrecognizedCCodeException("Unknown unary operator", null, exp);
      }
    }
  }

  private class ExpressionToFormulaVisitorUIF extends ExpressionToFormulaVisitor {

    public ExpressionToFormulaVisitorUIF(String pFunction, SSAMapBuilder pSsa) {
      super(pFunction, pSsa);
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
        (fexp.isPointerDereference() ? "->{" : ".{") +
        tpname + "," + field + "}";

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

  private class RightHandSideToFormulaVisitor extends ForwardingExpressionVisitor<Formula, UnrecognizedCCodeException>
                                              implements RightHandSideVisitor<Formula, UnrecognizedCCodeException> {

    protected final String function;
    protected final SSAMapBuilder ssa;

    public RightHandSideToFormulaVisitor(String pFunction, SSAMapBuilder pSsa) {
      super(getExpressionVisitor(pFunction, pSsa));
      function = pFunction;
      ssa = pSsa;
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

        } else if (!PURE_EXTERNAL_FUNCTIONS.contains(func)) {
          if (pexps.isEmpty()) {
            // function of arity 0
            log(Level.INFO, "Assuming external function to be a constant function", fn);
          } else {
            log(Level.INFO, "Assuming external function to be a pure function", fn);
          }
        }
      } else {
        log(Level.WARNING, "Ignoring function call through function pointer", fexp);
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

    public StatementToFormulaVisitor(String pFunction, SSAMapBuilder pSsa) {
      super(pFunction, pSsa);
    }

    @Override
    public Formula visit(IASTExpressionStatement pIastExpressionStatement) {
      // side-effect free statement, ignore
      return fmgr.makeTrue();
    }

    private Formula visit(IASTAssignment assignment) throws UnrecognizedCCodeException {
      Formula r = toNumericFormula(assignment.getRightHandSide().accept(this));
      Formula l = buildLvalueTerm(assignment.getLeftHandSide(), function, ssa);
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

  private class LvalueVisitor extends DefaultExpressionVisitor<Formula, UnrecognizedCCodeException> {

    protected final String function;
    protected final SSAMapBuilder ssa;

    public LvalueVisitor(String pFunction, SSAMapBuilder pSsa) {
      function = pFunction;
      ssa = pSsa;
    }

    @Override
    protected Formula visitDefault(IASTExpression exp) throws UnrecognizedCCodeException {
      throw new UnrecognizedCCodeException("Unknown lvalue", null, exp);
    }

    @Override
    public Formula visit(IASTIdExpression idExp) {
      String var = idExp.getName();

      if (isNondetVariable(var)) {
        logger.log(Level.WARNING, "Assignment to special non-determinism variable",
            var, "will be ignored.");
      }
      var = scopedIfNecessary(idExp, function);
      return makeFreshVariable(var, ssa);
    }

    private Formula makeUIF(IASTExpression exp) {
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

    public LvalueVisitorUIF(String pFunction, SSAMapBuilder pSsa) {
      super(pFunction, pSsa);
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
      Formula term = buildTerm(operand, function, ssa);

      // PW make SSA index of * independent from argument
      int idx = makeLvalIndex(opname, ssa);
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
      Formula term = buildTerm(owner, function, ssa);

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
      Formula aterm = buildTerm(arrexp, function, ssa);
      Formula sterm = buildTerm(subexp, function, ssa);

      String ufname = OP_ARRAY_SUBSCRIPT;
      FormulaList args = fmgr.makeList(aterm, sterm);
      int idx = makeLvalIndex(ufname, args, ssa);

      return fmgr.makeUIF(ufname, args, idx);

    }
  }
}
