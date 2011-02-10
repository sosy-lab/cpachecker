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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
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
  
  @Option(name="nondetFunctions")
  private String[] nondetFunctionsArray = {"int_nondet", "malloc", "nondet_int", "random"};
  private final Set<String> nondetFunctions;
  
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
    
    nondetFunctions = new HashSet<String>(Arrays.asList(nondetFunctionsArray));
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

  private String getTypeName(IType tp) {
    try {
      if (tp instanceof IPointerType) {
        return getTypeName(((IPointerType)tp).getType());
      }
      assert(tp instanceof IBinding);
      if (tp instanceof ITypedef) {
        return getTypeName(((ITypedef)tp).getType());
      }
      return ((IBinding)tp).getName();
    } catch (DOMException e) {
      logger.logException(Level.WARNING, e, "");
      assert(false);
    }
    return null;
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
      edgeFormula = makeDeclaration(d.getDeclSpecifier(), d.getDeclarators(), d.isGlobal(), edge, function, ssa);
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

  private Formula makeDeclaration(IASTDeclSpecifier spec,
      List<IASTDeclarator> declarators, boolean isGlobal, CFAEdge edge,
      String function, SSAMapBuilder ssa) throws CPATransferException {

    if (spec instanceof IASTEnumerationSpecifier) {
      // extract the fields, and add them as global variables
      assert(isGlobal);
      IASTEnumerationSpecifier.IASTEnumerator[] enums =
        ((IASTEnumerationSpecifier)spec).getEnumerators();
      
      Formula result = fmgr.makeTrue();
      for (IASTEnumerationSpecifier.IASTEnumerator e : enums) {
        String var = e.getName().getRawSignature();
        globalVars.add(var);
        IASTExpression exp = e.getValue();
        assert(exp != null);

        int idx = 1;
        ssa.setIndex(var, idx);

        Formula minit = buildTerm(exp, function, ssa);
        Formula mvar = fmgr.makeVariable(var, idx);
        Formula t = fmgr.makeAssignment(mvar, minit);
        result = fmgr.makeAnd(result, t);
      }
      return result;
    
    } else if (spec instanceof IASTCompositeTypeSpecifier) {
      // this is the declaration of a struct, just ignore it...
      log(Level.ALL, "Ignoring declaration", spec);
      return fmgr.makeTrue();
    
    } else if (spec instanceof IASTSimpleDeclSpecifier ||
               spec instanceof IASTElaboratedTypeSpecifier ||
               spec instanceof IASTNamedTypeSpecifier) {

      if (spec.getStorageClass() == IASTDeclSpecifier.sc_typedef) {
        log(Level.ALL, "Ignoring typedef", spec);
        return fmgr.makeTrue();
      }
  
      Formula result = fmgr.makeTrue();
      for (IASTDeclarator d : declarators) {
        if (d instanceof IASTFunctionDeclarator) {
          // ignore function declarations here
          continue;
        }
        
        String varNameWithoutFunction = d.getName().getRawSignature();
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
        IASTInitializer init = d.getInitializer();
        if (init != null) {
          // initializer value present
          if (!(init instanceof IASTInitializerExpression)) {
            log(Level.WARNING, "Ingoring unsupported initializer", init);
          
          } else if (isNondetVariable(varNameWithoutFunction)) {
            log(Level.WARNING, "Assignment to special non-determinism variable " + var + " will be ignored.", d);
          
          } else {
            IASTExpression exp = ((IASTInitializerExpression)init).getExpression();
            Formula minit = buildTerm(exp, function, ssa);
            Formula mvar = fmgr.makeVariable(var, idx);
            Formula t = fmgr.makeAssignment(mvar, minit);
            result = fmgr.makeAnd(result, t);
          }
  
        } else if (spec.getStorageClass() == IASTDeclSpecifier.sc_extern) {
          log(Level.WARNING, "Ignoring initializer of extern declaration", d);
  
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
      
    } else if (retExp instanceof IASTBinaryExpression) {
      IASTBinaryExpression exp = (IASTBinaryExpression)retExp;
      assert(exp.getOperator() == IASTBinaryExpression.op_assign);
      
      Formula retvarFormula = makeVariable(VAR_RETURN_NAME, function, ssa);
      IASTExpression e = exp.getOperand1();
      
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
        String formalParamName = formalParam.getDeclarator().getName().toString();
        if (formalParamName.isEmpty()) {
          assert(formalParam.getDeclarator().getNestedDeclarator() != null);
          formalParamName = formalParam.getDeclarator().getNestedDeclarator().getName().toString();
        }
        assert(!formalParamName.isEmpty());

        if (formalParam.getDeclarator().getPointerOperators().length != 0) {
          log(Level.WARNING, "Ignoring the semantics of pointer for parameter " + formalParamName,
              fn.getFunctionDefinition().getDeclarator());
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
    // this should be a number...
    String num = lexp.getRawSignature();
    switch (lexp.getKind()) {
    case IASTLiteralExpression.lk_integer_constant:
      // this might have some modifiers attached (e.g. 0UL), we
      // have to get rid of them
      int pos = num.length()-1;
      while (!Character.isDigit(num.charAt(pos))) {
        --pos;
      }
      num = num.substring(0, pos+1);
      if (num.startsWith("0x")) {
        // this should be in hex format
        // remove "0x" from the string
        num = num.substring(2);
        // we use Long instead of Integer to avoid getting negative
        // numbers (e.g. for 0xffffff we would get -1)
        num = Long.valueOf(num, 16).toString();
      }
      
      // TODO here we assume 32 bit integers!!! This is because CIL
      // seems to do so as well...
      try {
        Integer.parseInt(num);
      } catch (NumberFormatException nfe) {
        long l = Long.parseLong(num);
        if (l < 0) {
          num = Long.toString(Integer.MAX_VALUE + l);
        } else {
          num = Long.toString(l - ((long)Integer.MAX_VALUE + 1)*2);
        }
      }
      return fmgr.makeNumber(num);
      
    case IASTLiteralExpression.lk_float_constant:
      // parse with valueOf and convert to String again, because Mathsat
      // does not accept all possible C float constants (but Java hopefully does)
      return fmgr.makeNumber(Double.valueOf(num).toString());

    case IASTLiteralExpression.lk_char_constant: {
      // we convert to a byte, and take the integer value
      String s = lexp.getRawSignature();
      assert(s.charAt(0) == '\'');
      assert(s.charAt(s.length()-1) == '\'');
      s = s.substring(1, s.length()-1); // remove ''
      assert s.length() > 0;
      char c = s.charAt(0); // always the first character of s
      int n;

      if (c == '\\') {
        s = s.substring(1); // remove leading \
        assert s.length() >= 1 && s.length() <= 3;
        c = s.charAt(0);
        try {
          if (s.length() == 1 && !Character.isDigit(c)) {
            // something like '\n'
            switch (c) {
            case 'b' : n = '\b'; break;
            case 't' : n = '\t'; break;
            case 'n' : n = '\n'; break;
            case 'f' : n = '\f'; break;
            case 'r' : n = '\r'; break;
            case '"' : n = '\"'; break;
            case '\'' : n = '\''; break;
            case '\\' : n = '\\'; break;
            default:
              throw new UnrecognizedCCodeException("unknown character literal", null, lexp);
            }
          } else if (c == 'x') {
            // something like '\xFF'
            n = Integer.parseInt(s.substring(1), 16);
          } else {
            // something like '\000'
            n = Integer.parseInt(s, 8);
          }
        } catch (NumberFormatException e) {
          throw new UnrecognizedCCodeException("character with illegal number", null, lexp);
        }
      } else {
        // something like 'a'
        assert s.length() == 1;
        n = c;
      }
      return fmgr.makeNumber("" + n);
    }

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
      int op = ((IASTUnaryExpression)exp).getOperator();
      switch (op) {
      case IASTUnaryExpression.op_bracketedPrimary:
        return buildTerm(operand, function, ssa);
      case IASTUnaryExpression.op_postFixIncr:
      case IASTUnaryExpression.op_prefixIncr:
      case IASTUnaryExpression.op_postFixDecr:
      case IASTUnaryExpression.op_prefixDecr: {
        Formula mvar = buildTerm(operand, function, ssa);
        Formula newvar = buildLvalueTerm(operand, function, ssa);
        Formula me;
        Formula one = fmgr.makeNumber(1);
        if (op == IASTUnaryExpression.op_postFixIncr ||
            op == IASTUnaryExpression.op_prefixIncr) {
          me = fmgr.makePlus(mvar, one);
        } else {
          me = fmgr.makeMinus(mvar, one);
        }
        return fmgr.makeAssignment(newvar, me);
      }

      case IASTUnaryExpression.op_minus: {
        Formula mop = buildTerm(operand, function, ssa);
        return fmgr.makeNegate(mop);
      }

      case IASTUnaryExpression.op_amper:
      case IASTUnaryExpression.op_star:
        if (lvalsAsUif) {
          String opname;
          if (op == IASTUnaryExpression.op_amper) {
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

      case IASTUnaryExpression.op_tilde: {
        Formula term = buildTerm(operand, function, ssa);
        return fmgr.makeBitwiseNot(term);
      }

      /* !operand cannot be handled directly in case operand is a variable
       * we would need to know if operand is of type boolean or something else
       * currently ! is handled by the default branch
      case IASTUnaryExpression.op_not: {
        long operandMsat = buildMsatTerm(operand, ssa);
        return mathsat.api.msat_make_not(msatEnv, operandMsat);
      }*/

      case IASTUnaryExpression.op_sizeof: {
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

    } else if (exp instanceof IASTBinaryExpression) {
      int op = ((IASTBinaryExpression)exp).getOperator();
      IASTExpression e1 = ((IASTBinaryExpression)exp).getOperand1();
      IASTExpression e2 = ((IASTBinaryExpression)exp).getOperand2();

      switch (op) {
      case IASTBinaryExpression.op_assign:
      case IASTBinaryExpression.op_plusAssign:
      case IASTBinaryExpression.op_minusAssign:
      case IASTBinaryExpression.op_multiplyAssign:
      case IASTBinaryExpression.op_divideAssign:
      case IASTBinaryExpression.op_moduloAssign:
      case IASTBinaryExpression.op_binaryAndAssign:
      case IASTBinaryExpression.op_binaryOrAssign:
      case IASTBinaryExpression.op_binaryXorAssign:
      case IASTBinaryExpression.op_shiftLeftAssign:
      case IASTBinaryExpression.op_shiftRightAssign: {
        Formula me2 = buildTerm(e2, function, ssa);
        if (op != IASTBinaryExpression.op_assign) {
          // in this case, we have to get the old SSA instance for
          // reading the value of the variable, and build the
          // corresponding expression
          Formula oldvar = buildTerm(e1, function, ssa);
          switch (op) {
          case IASTBinaryExpression.op_plusAssign:
            me2 = fmgr.makePlus(oldvar, me2);
            break;
          case IASTBinaryExpression.op_minusAssign:
            me2 = fmgr.makeMinus(oldvar, me2);
            break;
          case IASTBinaryExpression.op_multiplyAssign:
            me2 = fmgr.makeMultiply(oldvar, me2);
            break;
          case IASTBinaryExpression.op_divideAssign:
            me2 = fmgr.makeDivide(oldvar, me2);
            break;
          case IASTBinaryExpression.op_moduloAssign:
            me2 = fmgr.makeModulo(oldvar, me2);
            break;
          case IASTBinaryExpression.op_binaryAndAssign:
            me2 = fmgr.makeBitwiseAnd(oldvar, me2);
            break;
          case IASTBinaryExpression.op_binaryOrAssign:
            me2 = fmgr.makeBitwiseOr(oldvar, me2);
            break;
          case IASTBinaryExpression.op_binaryXorAssign:
            me2 = fmgr.makeBitwiseXor(oldvar, me2);
            break;
          case IASTBinaryExpression.op_shiftLeftAssign:
            me2 = fmgr.makeShiftLeft(oldvar, me2);
            break;
          case IASTBinaryExpression.op_shiftRightAssign:
            me2 = fmgr.makeShiftRight(oldvar, me2);
            break;
          default:
            throw new UnrecognizedCCodeException("Unknown binary operator", null, exp);
          }
        }
        Formula mvar = buildLvalueTerm(e1, function, ssa);
        return fmgr.makeAssignment(mvar, me2);
      }

      case IASTBinaryExpression.op_plus:
      case IASTBinaryExpression.op_minus:
      case IASTBinaryExpression.op_multiply:
      case IASTBinaryExpression.op_divide:
      case IASTBinaryExpression.op_modulo:
      case IASTBinaryExpression.op_binaryAnd:
      case IASTBinaryExpression.op_binaryOr:
      case IASTBinaryExpression.op_binaryXor:
      case IASTBinaryExpression.op_shiftLeft:
      case IASTBinaryExpression.op_shiftRight: {
        Formula me1 = buildTerm(e1, function, ssa);
        Formula me2 = buildTerm(e2, function, ssa);

        switch (op) {
        case IASTBinaryExpression.op_plus:
          return fmgr.makePlus(me1, me2);
        case IASTBinaryExpression.op_minus:
          return fmgr.makeMinus(me1, me2);
        case IASTBinaryExpression.op_multiply:
          return fmgr.makeMultiply(me1, me2);
        case IASTBinaryExpression.op_divide:
          return fmgr.makeDivide(me1, me2);
        case IASTBinaryExpression.op_modulo:
          return fmgr.makeModulo(me1, me2);
        case IASTBinaryExpression.op_binaryAnd:
          return fmgr.makeBitwiseAnd(me1, me2);
        case IASTBinaryExpression.op_binaryOr:
          return fmgr.makeBitwiseOr(me1, me2);
        case IASTBinaryExpression.op_binaryXor:
          return fmgr.makeBitwiseXor(me1, me2);
        case IASTBinaryExpression.op_shiftLeft:
          return fmgr.makeShiftLeft(me1, me2);
        case IASTBinaryExpression.op_shiftRight:
          return fmgr.makeShiftRight(me1, me2);
        default:
          throw new AssertionError("Missing switch case");
        }
      }
      
      case IASTBinaryExpression.op_logicalAnd:
      case IASTBinaryExpression.op_logicalOr: {
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
        var = ((IASTIdExpression)exp).getName().getRawSignature();
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
      int op = ((IASTUnaryExpression)exp).getOperator();
      IASTExpression operand = ((IASTUnaryExpression)exp).getOperand();
      String opname;
      switch (op) {
      case IASTUnaryExpression.op_amper:
        opname = OP_ADDRESSOF_NAME;
        break;
      case IASTUnaryExpression.op_star:
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
    IASTExpression pexp = fexp.getParameterExpression();
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
        if (pexp == null) {
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

    if (pexp == null) {
      // this is a function of arity 0. We create a fresh global variable
      // for it (instantiated at 1 because we need an index but it never
      // increases)
      // TODO better use variables without index (this piece of code prevents
      // SSAMapBuilder from checking for strict monotony)
      globalVars.add(func);
      ssa.setIndex(func, 1); // set index so that predicates will be instantiated correctly
      return fmgr.makeVariable(func, 1);
    } else {
      IASTExpression[] args;
      if (pexp instanceof IASTExpressionList) {
        args = ((IASTExpressionList)pexp).getExpressions();
      } else {
        args = new IASTExpression[]{pexp};
      }
      func += "{" + args.length + "}"; // add #arguments to function name to cope with varargs functions
      Formula[] mArgs = new Formula[args.length];
      for (int i = 0; i < args.length; ++i) {
        mArgs[i] = buildTerm(args[i], function, ssa);
      }

      return fmgr.makeUIF(func, fmgr.makeList(mArgs));
    }
  }

  protected Formula makePredicate(IASTExpression exp, boolean isTrue,
        String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {
    
    Formula result = null;
    
    if (exp instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression)exp);
      int opType = binExp.getOperator();
      IASTExpression op1 = binExp.getOperand1();
      IASTExpression op2 = binExp.getOperand2();

      Formula t1 = buildTerm(op1, function, ssa);
      Formula t2 = buildTerm(op2, function, ssa);

      switch (opType) {
      case IASTBinaryExpression.op_greaterThan:
        result = fmgr.makeGt(t1, t2);
        break;

      case IASTBinaryExpression.op_greaterEqual:
        result = fmgr.makeGeq(t1, t2);
        break;

      case IASTBinaryExpression.op_lessThan:
        result = fmgr.makeLt(t1, t2);
        break;

      case IASTBinaryExpression.op_lessEqual:
        result = fmgr.makeLeq(t1, t2);
        break;

      case IASTBinaryExpression.op_equals:
        result = fmgr.makeEqual(t1, t2);
        break;

      case IASTBinaryExpression.op_notequals:
        result = fmgr.makeNot(fmgr.makeEqual(t1, t2));
        break;
      }

      // now create the formula
    } else if (exp instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExp = ((IASTUnaryExpression)exp);
      if (unaryExp.getOperator() == IASTUnaryExpression.op_not) {
        // ! exp
        return makePredicate(unaryExp.getOperand(), !isTrue, function, ssa);
      
      } else if (unaryExp.getOperator() == IASTUnaryExpression.op_bracketedPrimary) {
        // (exp)
        return makePredicate(unaryExp.getOperand(), isTrue, function, ssa);
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
