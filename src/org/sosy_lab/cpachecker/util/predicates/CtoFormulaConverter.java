/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTArrayTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignmentExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCompositeTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTElaboratedTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNamedTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTPointerTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IComplexType;
import org.sosy_lab.cpachecker.cfa.ast.IType;
import org.sosy_lab.cpachecker.cfa.ast.ITypedef;
import org.sosy_lab.cpachecker.cfa.ast.StorageClass;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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

  @Option
  protected boolean useNondetFlags = false;
  
  @Option
  private boolean initAllVars = false;

  @Option
  private String noAutoInitPrefix = "__BLAST_NONDET";
  
  @Option
  private boolean addBranchingInformation = true;

  // if true, handle lvalues as *x, &x, s.x, etc. using UIFs. If false, just
  // use variables
  @Option(name="mathsat.lvalsAsUIFs")
  private boolean lvalsAsUif = false;

  @Option
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
  public static final String PROGRAM_COUNTER_PREDICATE = "__pc__";

  // global variables (do not live in any namespace)
  private final Set<String> globalVars = new HashSet<String>();

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
  protected String scoped(String var, String function) {
    if (globalVars.contains(var)) {
      return var;
    } else {
      return function + "::" + var;
    }
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
      // so the SSA index must be fresh. If we use 1
      // here, we will have troubles later when
      // shifting indices
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
  
  private Formula makeVariable(String var, String function, SSAMapBuilder ssa) {
    int idx;
    if (isNondetVariable(var)) {
      // on every read access to special non-determininism variable, increase index
      var = NONDET_VARIABLE;
      idx = makeLvalIndex(var, ssa);
    } else {
      var = scoped(var, function);
      idx = getIndex(var, ssa);
    }
    return fmgr.makeVariable(var, idx);
  }
  
  private Formula makeAssignment(String var, String function,
          Formula rightHandSide, SSAMapBuilder ssa) {
    
    String name = scoped(var, function);
    int idx = makeLvalIndex(name, ssa);
    Formula f = fmgr.makeVariable(name, idx);
    return fmgr.makeAssignment(f, rightHandSide);
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

//  @Override
  public PathFormula makeAnd(PathFormula oldFormula, CFAEdge edge)
      throws CPATransferException {
    // this is where the "meat" is... We have to parse the statement
    // attached to the edge, and convert it to the appropriate formula

    if (edge.getEdgeType() == CFAEdgeType.BlankEdge) {
      
      // in this case there's absolutely nothing to do, so take a shortcut
      return oldFormula;
    }
    
    Formula reachingPathsFormula = oldFormula.getReachingPathsFormula();
    int branchingCounter = oldFormula.getBranchingCounter();
    
    String function = (edge.getPredecessor() != null) 
                          ? edge.getPredecessor().getFunctionName() : null;

    SSAMapBuilder ssa = oldFormula.getSsa().builder();

    Formula edgeFormula;
    switch (edge.getEdgeType()) {
    case StatementEdge: {
      StatementEdge statementEdge = (StatementEdge)edge;
      edgeFormula = makeStatement(statementEdge.getExpression(), function, ssa);
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
      branchingCounter++;
      Pair<Formula, Formula> pair
          = makeAssume((AssumeEdge)edge, function, ssa, branchingCounter);
      edgeFormula = pair.getFirst();
      reachingPathsFormula = fmgr.makeAnd(reachingPathsFormula, pair.getSecond());
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
    return new PathFormula(newFormula, newSsa, newLength, reachingPathsFormula, branchingCounter);
  }

  private Formula makeDeclaration(IType spec,
      boolean isGlobal, DeclarationEdge edge,
      String function, SSAMapBuilder ssa) throws CPATransferException {

    if (spec instanceof IASTFunctionTypeSpecifier) {
      return fmgr.makeTrue();
    
    } else if (spec instanceof IASTEnumerationSpecifier) {
      // don't need to handle enums, when an enum is referenced,
      // we can get the value from the AST
      return fmgr.makeTrue();
    
    } else if (spec instanceof IASTCompositeTypeSpecifier) {
      // this is the declaration of a struct, just ignore it...
      log(Level.ALL, "Ignoring declaration", edge);
      return fmgr.makeTrue();
    
    } else if (spec instanceof IASTSimpleDeclSpecifier ||
               spec instanceof IASTElaboratedTypeSpecifier ||
               spec instanceof IASTNamedTypeSpecifier ||
               spec instanceof IASTArrayTypeSpecifier ||
               spec instanceof IASTPointerTypeSpecifier) {

      if (edge.getStorageClass() == StorageClass.TYPEDEF) {
        log(Level.ALL, "Ignoring typedef", edge);
        return fmgr.makeTrue();
      }
        
      Formula result = fmgr.makeTrue();

      // ignore type prototypes here
      if (edge.getName() != null) {
        
        String varNameWithoutFunction = edge.getName().getRawSignature();
        if (isGlobal) {
          globalVars.add(varNameWithoutFunction);
        }
        String var = scoped(varNameWithoutFunction, function);
  
        // assign new index to variable
        // (a declaration contains an implicit assignment, even without initializer)
        int idx = makeLvalIndex(var, ssa);
  
        logger.log(Level.ALL, "Declared variable:", var, "index:", idx);
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
        IASTInitializer init = edge.getInitializer();
        if (init != null) {
          // initializer value present
          if (!(init instanceof IASTInitializerExpression)) {
            log(Level.WARNING, "Ingoring unsupported initializer", init);
          
          } else if (isNondetVariable(varNameWithoutFunction)) {
            log(Level.WARNING, "Assignment to special non-determinism variable " + var + " will be ignored.", edge);
          
          } else {
            IASTExpression exp = ((IASTInitializerExpression)init).getExpression();
            Formula minit = buildTerm(exp, function, ssa);
            Formula mvar = fmgr.makeVariable(var, idx);
            Formula t = fmgr.makeAssignment(mvar, minit);
            result = fmgr.makeAnd(result, t);
          }
  
        } else if (edge.getStorageClass() == StorageClass.EXTERN) {
          log(Level.WARNING, "Ignoring initializer of extern declaration", edge);
  
        } else if (isGlobal || initAllVars) {
          // auto-initialize variables to zero

          if (isNondetVariable(varNameWithoutFunction)) {
            logger.log(Level.ALL, "NOT AUTO-INITIALIZING VAR:", var);
          } else {
            Formula mvar = fmgr.makeVariable(var, idx);
            Formula z = fmgr.makeNumber(0);
            Formula t = fmgr.makeAssignment(mvar, z);
            result = fmgr.makeAnd(result, t);
            logger.log(Level.ALL, "AUTO-INITIALIZING VAR: ", var);
          }
        }
      }
      return result;

    } else { 
      throw new UnrecognizedCFAEdgeException(edge);
    }
  }

  private Formula makeExitFunction(CallToReturnEdge ce, String function,
      SSAMapBuilder ssa) throws CPATransferException {
    
    IASTExpression retExp = ce.getExpression();
    if (retExp instanceof IASTFunctionCallExpression) {
      // this should be a void return, just do nothing...
      return fmgr.makeTrue();
      
    } else if (retExp instanceof IASTAssignmentExpression) {
      IASTAssignmentExpression exp = (IASTAssignmentExpression)retExp;
      
      Formula retvarFormula = makeVariable(VAR_RETURN_NAME, function, ssa);
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
      
      assert formalParams.size() == actualParams.size();

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
        
        Formula eq = makeAssignment(formalParamName, calledFunction, actualParam, ssa);
        
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
      return makeAssignment(VAR_RETURN_NAME, function, retval, ssa); 
    }
  }

  private Formula makeStatement(IASTExpression expr, String function,
      SSAMapBuilder ssa) throws CPATransferException {

    Formula f = buildTerm(expr, function, ssa);

    if (!fmgr.isBoolean(f)) {
      // in this case, we have something like:
        // f(x);
      // i.e. an expression that gets assigned to nothing. Since
      // we don't handle side-effects, this means that the
      // expression has no effect, and we can just drop it
      
      // if it is a (external) function call, it was already logged if needed
      // don't log here to avoid warning about cases like printf() 
      if (!(expr instanceof IASTFunctionCallExpression)) {
        log(Level.INFO, "Statement is assumed to be side-effect free, but its return value is not used",
                        expr);
      }
      return fmgr.makeTrue();
    }
    return f;
  }

  private Pair<Formula, Formula> makeAssume(AssumeEdge assume,
      String function, SSAMapBuilder ssa, int branchingIdx) throws CPATransferException {

    Formula edgeFormula = makePredicate(assume.getExpression(),
        assume.getTruthAssumption(), function, ssa);
    
    Formula branchingInformation;
    if (addBranchingInformation) {
      // add a unique predicate for each branching decision
      String var = PROGRAM_COUNTER_PREDICATE + assume.getPredecessor().getNodeNumber();
  
      Formula predFormula = fmgr.makePredicateVariable(var, branchingIdx);
      if (assume.getTruthAssumption() == false) {
        predFormula = fmgr.makeNot(predFormula);
      }
      
      branchingInformation = fmgr.makeEquivalence(edgeFormula, predFormula);
      branchingInformation = fmgr.makeAnd(branchingInformation, predFormula);
    } else {
      branchingInformation = fmgr.makeTrue();
    }

    return Pair.of(edgeFormula, branchingInformation);
  }
  
  private Formula buildLiteralExpression(IASTLiteralExpression lexp) throws UnrecognizedCCodeException {
    if (lexp instanceof IASTCharLiteralExpression) {
      IASTCharLiteralExpression cExp = (IASTCharLiteralExpression)lexp;
      // we just take the byte value
      return fmgr.makeNumber(cExp.getCharacter());
    
    } else if (lexp instanceof IASTIntegerLiteralExpression) {
      IASTIntegerLiteralExpression iExp = (IASTIntegerLiteralExpression)lexp;

      return fmgr.makeNumber(iExp.getValue().toString());
    }
    
    // this should be a number...
    String num = lexp.getRawSignature();
    switch (lexp.getKind()) {
    case IASTLiteralExpression.lk_float_constant:
      // parse with valueOf and convert to String again, because Mathsat
      // does not accept all possible C float constants (but Java hopefully does)
      return fmgr.makeNumber(Double.valueOf(num).toString());

    case IASTLiteralExpression.lk_string_literal: {
      // we create a string constant representing the given
      // string literal
      if (stringLitToFormula.containsKey(lexp.getRawSignature())) {
        return stringLitToFormula.get(lexp.getRawSignature());
      } else {
        // generate a new string literal. We generate a new UIf
        int n = nextStringLitIndex++;
        Formula t = fmgr.makeString(n);
        stringLitToFormula.put(lexp.getRawSignature(), t);
        return t;
      }
    }
    default:
      throw new UnrecognizedCCodeException("Unknown literal", null, lexp);
    }
  }

  private Formula buildTerm(IASTExpression exp, String function, SSAMapBuilder ssa)
        throws UnrecognizedCCodeException {
    if (exp instanceof IASTIdExpression) {
      IASTIdExpression idExp = (IASTIdExpression)exp;
      
      if (idExp.getDeclaration() instanceof IASTEnumerator) {
        IASTEnumerator enumerator = (IASTEnumerator)idExp.getDeclaration();
        return fmgr.makeNumber(Long.toString(enumerator.getValue()));
      }

      // this is a variable: get the right index for the SSA
      String var = ((IASTIdExpression)exp).getName().getRawSignature();
      return makeVariable(var, function, ssa);
    } 
    else if (exp instanceof IASTLiteralExpression) {
      return buildLiteralExpression((IASTLiteralExpression)exp);
    } else if (exp instanceof IASTCastExpression) {
      // we completely ignore type casts
      logger.log(Level.ALL, "DEBUG_3", "IGNORING TYPE CAST:",
          exp.getRawSignature());
      return buildTerm(((IASTCastExpression)exp).getOperand(), function, ssa);
    } else if (exp instanceof IASTUnaryExpression) {
      IASTExpression operand = ((IASTUnaryExpression)exp).getOperand();
      UnaryOperator op = ((IASTUnaryExpression)exp).getOperator();
      switch (op) {
      case MINUS: {
        Formula mop = buildTerm(operand, function, ssa);
        return fmgr.makeNegate(mop);
      }

      case AMPER:
      case STAR:
        if (lvalsAsUif) {
          String opname;
          if (op == UnaryOperator.AMPER) {
            opname = OP_ADDRESSOF_NAME;
          } else {
            opname = OP_STAR_NAME;
          }
          Formula term = buildTerm(operand, function, ssa);

          // PW make SSA index of * independent from argument
          int idx = getIndex(opname, ssa);
          //int idx = getIndex(
          //    opname, term, ssa, absoluteSSAIndices);

          // build the  function corresponding to this operation.
          return fmgr.makeUIF(opname, fmgr.makeList(term), idx);

        } else {
          warnUnsafeVar(exp);
          return makeVariable(exprToVarName(exp), function, ssa);
        }

      case TILDE: {
        Formula term = buildTerm(operand, function, ssa);
        return fmgr.makeBitwiseNot(term);
      }

      /* !operand cannot be handled directly in case operand is a variable
       * we would need to know if operand is of type boolean or something else
       * currently ! is handled by the default branch
      case IASTUnaryExpression.NOT: {
        long operandMsat = buildMsatTerm(operand, ssa);
        return mathsat.api.msat_make_not(msatEnv, operandMsat);
      }*/

      case SIZEOF: {
        // TODO
        warnUnsafeVar(exp);
        return makeVariable(exprToVarName(exp), function, ssa);
      }

      default: {
        // this might be a predicate implicitly cast to an int. Let's
        // see if this is indeed the case...
        Formula ftmp = makePredicate(exp, true, function, ssa);
        return fmgr.makeIfThenElse(ftmp, fmgr.makeNumber(1), fmgr.makeNumber(0));
      }
      }

    } else if (exp instanceof IASTAssignmentExpression) {
      IASTAssignmentExpression assignment = (IASTAssignmentExpression)exp;
      
      Formula r = buildTerm(assignment.getRightHandSide(), function, ssa);
      Formula l = buildLvalueTerm(assignment.getLeftHandSide(), function, ssa);
      return fmgr.makeAssignment(l, r);
      
    } else if (exp instanceof IASTBinaryExpression) {
      BinaryOperator op = ((IASTBinaryExpression)exp).getOperator();
      IASTExpression e1 = ((IASTBinaryExpression)exp).getOperand1();
      IASTExpression e2 = ((IASTBinaryExpression)exp).getOperand2();

      switch (op) {
      case PLUS:
      case MINUS:
      case MULTIPLY:
      case DIVIDE:
      case MODULO:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
      case SHIFT_LEFT:
      case SHIFT_RIGHT: {
        Formula me1 = buildTerm(e1, function, ssa);
        Formula me2 = buildTerm(e2, function, ssa);

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
        default:
          throw new AssertionError("Missing switch case");
        }
      }
      
      case LOGICAL_AND:
      case LOGICAL_OR: {
        throw new UnrecognizedCCodeException("Unknown binary operator", null, exp);
      }

      default:
        // this might be a predicate implicitly cast to an int, like this:
        // int tmp = (a == b)
        // Let's see if this is indeed the case...
        Formula ftmp = makePredicate(exp, true, function, ssa);
        return fmgr.makeIfThenElse(ftmp, fmgr.makeNumber(1), fmgr.makeNumber(0));
      }

    } else if (exp instanceof IASTFieldReference) {
      if (lvalsAsUif) {
        IASTFieldReference fexp = (IASTFieldReference)exp;
        String field = fexp.getFieldName().getRawSignature();
        IASTExpression owner = fexp.getFieldOwner();
        Formula term = buildTerm(owner, function, ssa);

        String tpname = getTypeName(owner.getExpressionType());
        String ufname =
          (fexp.isPointerDereference() ? "->{" : ".{") +
          tpname + "," + field + "}";

        // see above for the case of &x and *x
        return makeUIF(ufname, fmgr.makeList(term), ssa);
      } else {
        warnUnsafeVar(exp);
        return makeVariable(exprToVarName(exp), function, ssa);

      }
    } else if (exp instanceof IASTArraySubscriptExpression) {
      if (lvalsAsUif) {
        IASTArraySubscriptExpression aexp =
          (IASTArraySubscriptExpression)exp;
        IASTExpression arrexp = aexp.getArrayExpression();
        IASTExpression subexp = aexp.getSubscriptExpression();
        Formula aterm = buildTerm(arrexp, function, ssa);
        Formula sterm = buildTerm(subexp, function, ssa);

        String ufname = OP_ARRAY_SUBSCRIPT;
        return makeUIF(ufname, fmgr.makeList(aterm, sterm), ssa);

      } else {
        warnUnsafeVar(exp);
        return makeVariable(exprToVarName(exp), function, ssa);
      }
    } else if (exp instanceof IASTFunctionCallExpression) {
      // this is an external call. We have to create an UIF.
      IASTFunctionCallExpression fexp = (IASTFunctionCallExpression)exp;
      return makeExternalFunctionCall(fexp, function, ssa);
    } else if (exp instanceof IASTTypeIdExpression) {
      assert(((IASTTypeIdExpression)exp).getOperator() ==
        IASTTypeIdExpression.op_sizeof);
      warnUnsafeVar(exp);
      return makeVariable(exprToVarName(exp), function, ssa);

    } else {
      throw new UnrecognizedCCodeException("Unknown expression", null, exp);
    }
  }

  private Formula buildLvalueTerm(IASTExpression exp,
        String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {
    if (exp instanceof IASTIdExpression || !lvalsAsUif) {
      String var;
      if (exp instanceof IASTIdExpression) {
        IASTIdExpression idExp = (IASTIdExpression)exp;
        IASTSimpleDeclaration decl = idExp.getDeclaration();
        var = idExp.getName().getRawSignature();

        // some checks to determine whether our set of global variables
        // and the AST concord
        if (decl == null) {
          assert !globalVars.contains(var) : "Undeclared global variables cannot exist";
        } else {
          if (decl instanceof IASTDeclaration) {
            assert globalVars.contains(var) == ((IASTDeclaration)decl).isGlobal();
          }
        }
        
      } else {
        var = exprToVarName(exp);
      }
      if (isNondetVariable(var)) {
        logger.log(Level.WARNING, "Assignment to special non-determinism variable",
            exp.getRawSignature(), "will be ignored.");
      }
      var = scoped(var, function);
      int idx = makeLvalIndex(var, ssa);

      Formula mvar = fmgr.makeVariable(var, idx);
      return mvar;
    } else if (exp instanceof IASTUnaryExpression) {
      UnaryOperator op = ((IASTUnaryExpression)exp).getOperator();
      IASTExpression operand = ((IASTUnaryExpression)exp).getOperand();
      String opname;
      switch (op) {
      case AMPER:
        opname = OP_ADDRESSOF_NAME;
        break;
      case STAR:
        opname = OP_STAR_NAME;
        break;
      default:
        throw new UnrecognizedCCodeException("Invalid unary operator for lvalue", null, exp);
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
      
    } else if (exp instanceof IASTFieldReference) {
      IASTFieldReference fexp = (IASTFieldReference)exp;
      String field = fexp.getFieldName().getRawSignature();
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

    } else if (exp instanceof IASTArraySubscriptExpression) {
      IASTArraySubscriptExpression aexp =
        (IASTArraySubscriptExpression)exp;
      IASTExpression arrexp = aexp.getArrayExpression();
      IASTExpression subexp = aexp.getSubscriptExpression();
      Formula aterm = buildTerm(arrexp, function, ssa);
      Formula sterm = buildTerm(subexp, function, ssa);

      String ufname = OP_ARRAY_SUBSCRIPT;
      FormulaList args = fmgr.makeList(aterm, sterm);
      int idx = makeLvalIndex(ufname, args, ssa);

      return fmgr.makeUIF(ufname, args, idx);

    } else {
      throw new UnrecognizedCCodeException("Unknown lvalue", null, exp);
    }
  }

  private Formula makeExternalFunctionCall(IASTFunctionCallExpression fexp,
        String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {
    IASTExpression fn = fexp.getFunctionNameExpression();
    List<IASTExpression> pexps = fexp.getParameterExpressions();
    String func;
    if (fn instanceof IASTIdExpression) {
      func = ((IASTIdExpression)fn).getName().getRawSignature();
      if (nondetFunctions.contains(func)) {
        // function call like "random()"
        // ignore parameters and just create a fresh variable for it  
        globalVars.add(func);
        int idx = makeLvalIndex(func, ssa);
        return fmgr.makeVariable(func, idx);
        
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
      // this is a function of arity 0. We create a fresh global variable
      // for it (instantiated at 1 because we need an index but it never
      // increases)
      // TODO better use variables without index (this piece of code prevents
      // SSAMapBuilder from checking for strict monotony)
      globalVars.add(func);
      ssa.setIndex(func, 1); // set index so that predicates will be instantiated correctly
      return fmgr.makeVariable(func, 1);
    } else {
      IASTExpression[] args = pexps.toArray(new IASTExpression[pexps.size()]);
      func += "{" + pexps.size() + "}"; // add #arguments to function name to cope with varargs functions
      Formula[] mArgs = new Formula[args.length];
      for (int i = 0; i < pexps.size(); ++i) {
        mArgs[i] = buildTerm(pexps.get(i), function, ssa);
      }

      return fmgr.makeUIF(func, fmgr.makeList(mArgs));
    }
  }

  protected Formula makePredicate(IASTExpression exp, boolean isTrue,
        String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {
    
    Formula result = null;
    
    if (exp instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression)exp);
      BinaryOperator opType = binExp.getOperator();
      IASTExpression op1 = binExp.getOperand1();
      IASTExpression op2 = binExp.getOperand2();

      if ((opType == BinaryOperator.LOGICAL_AND)
          || (opType == BinaryOperator.LOGICAL_OR)) {
        
        // these operators expect boolean arguments
        Formula t1 = makePredicate(op1, true, function, ssa);
        Formula t2 = makePredicate(op2, true, function, ssa);
        
        switch (opType) {
        case LOGICAL_AND:
          result = fmgr.makeAnd(t1, t2);
          break;
          
        case LOGICAL_OR:
          result = fmgr.makeOr(t1, t2);
          break;
          
        default: throw new AssertionError();
        }
      
      } else {
        // the rest of the operators expect numeric arguments
        Formula t1 = buildTerm(op1, function, ssa);
        Formula t2 = buildTerm(op2, function, ssa);
  
        switch (opType) {
        case GREATER_THAN:
          result = fmgr.makeGt(t1, t2);
          break;
  
        case GREATER_EQUAL:
          result = fmgr.makeGeq(t1, t2);
          break;
  
        case LESS_THAN:
          result = fmgr.makeLt(t1, t2);
          break;
  
        case LESS_EQUAL:
          result = fmgr.makeLeq(t1, t2);
          break;
  
        case EQUALS:
          result = fmgr.makeEqual(t1, t2);
          break;
  
        case NOT_EQUALS:
          result = fmgr.makeNot(fmgr.makeEqual(t1, t2));
          break;
          
        default:
          // do nothing, because it is not a boolean operator
          // will be handled by call to buildTerm()
          break;
        }
      }

      // now create the formula
    } else if (exp instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExp = ((IASTUnaryExpression)exp);
      if (unaryExp.getOperator() == UnaryOperator.NOT) {
        // ! exp
        return makePredicate(unaryExp.getOperand(), !isTrue, function, ssa);
      }
    }

    if (result == null) {
      // not handled above, check whether this is an implict cast to bool
      // build the term. If this is not a predicate, make
      // it a predicate by adding a "!= 0"
      result = buildTerm(exp, function, ssa);

      if (!fmgr.isBoolean(result)) {
        Formula z = fmgr.makeNumber(0);
        result = fmgr.makeNot(fmgr.makeEqual(result, z));
      }
    }
    
    if (!isTrue) {
      result = fmgr.makeNot(result);
    }
    return result;
  }
  
  protected void addToGlobalVars(String pVar){
    globalVars.add(pVar);
  }
}
