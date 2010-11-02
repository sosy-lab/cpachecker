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
package org.sosy_lab.cpachecker.util.symbpredabstraction;

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
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;

import com.google.common.collect.ImmutableSet;

/**
 * Class containing all the code that converts C code into a formula.
 */
@Options(prefix="cpas.symbpredabs")
public class CtoFormulaConverter {

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
  
  // list of functions that are pure (no side-effects)
  private static final Set<String> PURE_EXTERNAL_FUNCTIONS
      = ImmutableSet.of("__assert_fail", "printf", "puts");
  
  // list of non-deterministic functions
  private static final Set<String> NONDET_EXTERNAL_FUNCTIONS
      = ImmutableSet.of("int_nondet", "malloc", "nondet_int", "random");

  //names for special variables needed to deal with functions
  private static final String VAR_RETURN_NAME = "__retval__";
  private static final String OP_ADDRESSOF_NAME = "__ptrAmp__";
  private static final String OP_STAR_NAME = "__ptrStar__";
  private static final String OP_ARRAY_SUBSCRIPT = "__array__";
  private static final String NONDET_VARIABLE = "__nondet__";
  public static final String PROGRAM_COUNTER_PREDICATE = "__pc__";

  // global variables (do not live in any namespace)
  private final Set<String> globalVars = new HashSet<String>();

  private int nondetCounter = 0;

  private final Set<String> printedWarnings = new HashSet<String>();

  private final Map<String, SymbolicFormula> stringLitToFormula = new HashMap<String, SymbolicFormula>();
  private int nextStringLitIndex = 0;
  
  protected final SymbolicFormulaManager smgr;
  protected final LogManager logger;
  
  public CtoFormulaConverter(Configuration config, SymbolicFormulaManager smgr, LogManager logger) throws InvalidConfigurationException {
    config.inject(this, CtoFormulaConverter.class);
    
    this.smgr = smgr;
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

  private String exprToVarName(IASTExpression e) {
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
  private int makeLvalIndex(String name, SymbolicFormulaList args, SSAMapBuilder ssa) {
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
  
  private SymbolicFormula makeVariable(String var, String function, SSAMapBuilder ssa) {
    int idx;
    if (isNondetVariable(var)) {
      // on every read access to special non-determininism variable, increase index
      var = NONDET_VARIABLE;
      idx = nondetCounter++;
    } else {
      var = scoped(var, function);
      idx = getIndex(var, ssa);
    }
    return smgr.makeVariable(var, idx);
  }
  
  private SymbolicFormula makeAssignment(String var, String function,
          SymbolicFormula rightHandSide, SSAMapBuilder ssa) {
    
    String name = scoped(var, function);
    int idx = makeLvalIndex(name, ssa);
    SymbolicFormula f = smgr.makeVariable(name, idx);
    return smgr.makeAssignment(f, rightHandSide);
  }
  
  private SymbolicFormula makeUIF(String name, SymbolicFormulaList args, SSAMapBuilder ssa) {
    int idx = ssa.getIndex(name, args);
    if (idx <= 0) {
      logger.log(Level.ALL, "DEBUG_3",
          "WARNING: Auto-instantiating lval: ", name, "(", args, ")");
      idx = 1;
      ssa.setIndex(name, args, idx);
    }
    return smgr.makeUIF(name, args, idx);
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
    
    SymbolicFormula reachingPathsFormula = oldFormula.getReachingPathsFormula();
    int branchingCounter = oldFormula.getBranchingCounter();
    
    String function = (edge.getPredecessor() != null) 
                          ? edge.getPredecessor().getFunctionName() : null;

    SSAMapBuilder ssa = oldFormula.getSsa().builder();

    SymbolicFormula edgeFormula;
    switch (edge.getEdgeType()) {
    case StatementEdge: {
      StatementEdge statementEdge = (StatementEdge)edge;

      if (statementEdge.isJumpEdge()) {
        if (statementEdge.getSuccessor().getFunctionName().equals("main")) {
          log(Level.FINEST, "IGNORING return from main:", edge.getRawAST());
          edgeFormula = smgr.makeTrue();
        } else {
          edgeFormula = makeReturn(statementEdge, function, ssa);
        }
      } else {
        edgeFormula = makeStatement(statementEdge.getExpression(), function, ssa);
      }
      break;
    }
    
    case DeclarationEdge: {
      DeclarationEdge d = (DeclarationEdge)edge;
      edgeFormula = makeDeclaration(d.getDeclSpecifier(), d.getDeclarators(), d.isGlobal(), edge, function, ssa);
      break;
    }
    
    case AssumeEdge: {
      branchingCounter++;
      Pair<SymbolicFormula, SymbolicFormula> pair
          = makeAssume((AssumeEdge)edge, function, ssa, branchingCounter);
      edgeFormula = pair.getFirst();
      reachingPathsFormula = smgr.makeAnd(reachingPathsFormula, pair.getSecond());
      break;
    }

    case BlankEdge: {
      assert false : "Handled above";
      edgeFormula = smgr.makeTrue();
      break;
    }

    case FunctionCallEdge: {
      edgeFormula = makeFunctionCall((FunctionCallEdge)edge, function, ssa);
      break;
    }

    case ReturnEdge: {
      // get the expression from the summary edge
      CFANode succ = edge.getSuccessor();
      CallToReturnEdge ce = succ.getEnteringSummaryEdge();
      edgeFormula = makeExitFunction(ce, function, ssa);
      break;
    }

    default:
      throw new UnrecognizedCFAEdgeException(edge);
    }

    SSAMap newSsa = ssa.build();
    if (edgeFormula.isTrue() && (newSsa == oldFormula.getSsa())) {
      // formula is just "true" and SSAMap is identical
      // i.e. no writes to SSAMap, no branching and length should stay the same
      return oldFormula;
    }
    
    SymbolicFormula newFormula = smgr.makeAnd(oldFormula.getSymbolicFormula(), edgeFormula);
    int newLength = oldFormula.getLength() + 1;
    return new PathFormula(newFormula, newSsa, newLength, reachingPathsFormula, branchingCounter);
  }

  private SymbolicFormula makeDeclaration(IASTDeclSpecifier spec,
      IASTDeclarator[] declarators, boolean isGlobal, CFAEdge edge,
      String function, SSAMapBuilder ssa) throws CPATransferException {

    if (spec instanceof IASTEnumerationSpecifier) {
      // extract the fields, and add them as global variables
      assert(isGlobal);
      IASTEnumerationSpecifier.IASTEnumerator[] enums =
        ((IASTEnumerationSpecifier)spec).getEnumerators();
      
      SymbolicFormula result = smgr.makeTrue();
      for (IASTEnumerationSpecifier.IASTEnumerator e : enums) {
        String var = e.getName().getRawSignature();
        globalVars.add(var);
        IASTExpression exp = e.getValue();
        assert(exp != null);

        int idx = 1;
        ssa.setIndex(var, idx);

        SymbolicFormula minit = buildTerm(exp, function, ssa);
        SymbolicFormula mvar = smgr.makeVariable(var, idx);
        SymbolicFormula t = smgr.makeAssignment(mvar, minit);
        result = smgr.makeAnd(result, t);
      }
      return result;
    
    } else if (spec instanceof IASTCompositeTypeSpecifier) {
      // this is the declaration of a struct, just ignore it...
      log(Level.ALL, "Ignoring declaration", spec);
      return smgr.makeTrue();
    
    } else if (spec instanceof IASTSimpleDeclSpecifier ||
               spec instanceof IASTElaboratedTypeSpecifier ||
               spec instanceof IASTNamedTypeSpecifier) {

      if (spec.getStorageClass() == IASTDeclSpecifier.sc_typedef) {
        log(Level.ALL, "Ignoring typedef", spec);
        return smgr.makeTrue();
      }
  
      SymbolicFormula result = smgr.makeTrue();
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
  //    m1 = new MathsatSymbolicFormula(t);
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
            SymbolicFormula minit = buildTerm(exp, function, ssa);
            SymbolicFormula mvar = smgr.makeVariable(var, idx);
            SymbolicFormula t = smgr.makeAssignment(mvar, minit);
            result = smgr.makeAnd(result, t);
          }
  
        } else if (spec.getStorageClass() == IASTDeclSpecifier.sc_extern) {
          log(Level.WARNING, "Ignoring initializer of extern declaration", d);
  
        } else if (isGlobal || initAllVars) {
          // auto-initialize variables to zero

          if (isNondetVariable(varNameWithoutFunction)) {
            logger.log(Level.ALL, "NOT AUTO-INITIALIZING VAR:", var);
          } else {
            SymbolicFormula mvar = smgr.makeVariable(var, idx);
            SymbolicFormula z = smgr.makeNumber(0);
            SymbolicFormula t = smgr.makeAssignment(mvar, z);
            result = smgr.makeAnd(result, t);
            logger.log(Level.ALL, "AUTO-INITIALIZING VAR: ", var);
          }
        }
      }
      return result;

    } else { 
      throw new UnrecognizedCFAEdgeException(edge);
    }
  }

  private SymbolicFormula makeExitFunction(CallToReturnEdge ce, String function,
      SSAMapBuilder ssa) throws CPATransferException {
    
    IASTExpression retExp = ce.getExpression();
    if (retExp instanceof IASTFunctionCallExpression) {
      // this should be a void return, just do nothing...
      return smgr.makeTrue();
      
    } else if (retExp instanceof IASTBinaryExpression) {
      IASTBinaryExpression exp = (IASTBinaryExpression)retExp;
      assert(exp.getOperator() == IASTBinaryExpression.op_assign);
      
      SymbolicFormula retvarFormula = makeVariable(VAR_RETURN_NAME, function, ssa);
      IASTExpression e = exp.getOperand1();
      
      function = ce.getSuccessor().getFunctionName();
      SymbolicFormula outvarFormula = buildLvalueTerm(e, function, ssa);
      return smgr.makeAssignment(outvarFormula, retvarFormula);
    
    } else {
      throw new UnrecognizedCFAEdgeException("UNKNOWN FUNCTION EXIT EXPRESSION: " + ce.getRawStatement());
    }
  }

  private SymbolicFormula makeFunctionCall(FunctionCallEdge edge,
      String callerFunction, SSAMapBuilder ssa) throws CPATransferException {
    
    if (edge.isExternalCall()) {
      throw new UnrecognizedCFAEdgeException(
          "EXTERNAL CALL UNSUPPORTED: " + edge.getRawStatement());
    } else {

      IASTExpression[] actualParams = edge.getArguments();
      int paramsCount = (actualParams == null ? 0 : actualParams.length);
      
      FunctionDefinitionNode fn = (FunctionDefinitionNode)edge.getSuccessor();
      List<IASTParameterDeclaration> formalParams = fn.getFunctionParameters();
      
      assert formalParams.size() == paramsCount;
      if (paramsCount == 0) {
        return smgr.makeTrue();
      }

      String calledFunction = fn.getFunctionName();
      
      int i = 0;
      SymbolicFormula result = smgr.makeTrue();
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
        SymbolicFormula actualParam = buildTerm(actualParams[i++], callerFunction, ssa);
        
        SymbolicFormula eq = makeAssignment(formalParamName, calledFunction, actualParam, ssa);
        
        result = smgr.makeAnd(result, eq);
      }

      return result;
    }
  }

  private SymbolicFormula makeReturn(StatementEdge edge, String function, SSAMapBuilder ssa)
      throws CPATransferException {
    IASTExpression exp = edge.getExpression();
    if (exp == null) {
      // this is a return from a void function, do nothing
      return smgr.makeTrue();
    } else if (exp instanceof IASTUnaryExpression) {
      
      // we have to save the information about the return value,
      // so that we can use it later on, if it is assigned to
      // a variable. We create a function::__retval__ variable
      // that will hold the return value
      SymbolicFormula retval = buildTerm(exp, function, ssa);
      return makeAssignment(VAR_RETURN_NAME, function, retval, ssa); 
    }
    // if we are here, we can't handle the return properly...
    throw new UnrecognizedCFAEdgeException(edge);
  }

  private SymbolicFormula makeStatement(IASTExpression expr, String function,
      SSAMapBuilder ssa) throws CPATransferException {

    SymbolicFormula f = buildTerm(expr, function, ssa);

    if (!smgr.isBoolean(f)) {
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
      return smgr.makeTrue();
    }
    return f;
  }

  private Pair<SymbolicFormula, SymbolicFormula> makeAssume(AssumeEdge assume,
      String function, SSAMapBuilder ssa, int branchingIdx) throws CPATransferException {

    SymbolicFormula edgeFormula = makePredicate(assume.getExpression(),
        assume.getTruthAssumption(), function, ssa);
    
    SymbolicFormula branchingInformation;
    if (addBranchingInformation) {
      // add a unique predicate for each branching decision
      String var = PROGRAM_COUNTER_PREDICATE + assume.getPredecessor().getNodeNumber();
  
      SymbolicFormula predFormula = smgr.makePredicateVariable(var, branchingIdx);
      if (assume.getTruthAssumption() == false) {
        predFormula = smgr.makeNot(predFormula);
      }
      
      branchingInformation = smgr.makeEquivalence(edgeFormula, predFormula);
      branchingInformation = smgr.makeAnd(branchingInformation, predFormula);
    } else {
      branchingInformation = smgr.makeTrue();
    }

    return new Pair<SymbolicFormula, SymbolicFormula>(edgeFormula, branchingInformation);
  }

  private SymbolicFormula buildTerm(IASTExpression exp, String function, SSAMapBuilder ssa)
        throws UnrecognizedCCodeException {
    if (exp instanceof IASTIdExpression) {
      // this is a variable: get the right index for the SSA
      String var = ((IASTIdExpression)exp).getName().getRawSignature();
      return makeVariable(var, function, ssa);
    } else if (exp instanceof IASTLiteralExpression) {
      // this should be a number...
      IASTLiteralExpression lexp = (IASTLiteralExpression)exp;
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
        return smgr.makeNumber(num);
        
      case IASTLiteralExpression.lk_float_constant:
        // parse with valueOf and convert to String again, because Mathsat
        // does not accept all possible C float constants (but Java hopefully does)
        return smgr.makeNumber(Double.valueOf(num).toString());

      case IASTLiteralExpression.lk_char_constant: {
        // we convert to a byte, and take the integer value
        String s = exp.getRawSignature();
        int length = s.length();
        assert(s.charAt(0) == '\'');
        assert(s.charAt(length-1) == '\'');
        int n;

        if (s.charAt(1) == '\\') {
          n = Integer.parseInt(s.substring(2, length-1));
        } else {
          assert (exp.getRawSignature().length() == 3);
          n = exp.getRawSignature().charAt(1);
        }
        return smgr.makeNumber("" + n);
      }

      case IASTLiteralExpression.lk_string_literal: {
        // we create a string constant representing the given
        // string literal
        if (stringLitToFormula.containsKey(exp.getRawSignature())) {
          return stringLitToFormula.get(exp.getRawSignature());
        } else {
          // generate a new string literal. We generate a new UIf
          int n = nextStringLitIndex++;
          SymbolicFormula t = smgr.makeString(n);
          stringLitToFormula.put(exp.getRawSignature(), t);
          return t;
        }
      }
      default:
        throw new UnrecognizedCCodeException("Unknown literal", null, exp);
      }

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
        SymbolicFormula mvar = buildTerm(operand, function, ssa);
        SymbolicFormula newvar = buildLvalueTerm(operand, function, ssa);
        SymbolicFormula me;
        SymbolicFormula one = smgr.makeNumber(1);
        if (op == IASTUnaryExpression.op_postFixIncr ||
            op == IASTUnaryExpression.op_prefixIncr) {
          me = smgr.makePlus(mvar, one);
        } else {
          me = smgr.makeMinus(mvar, one);
        }
        return smgr.makeAssignment(newvar, me);
      }

      case IASTUnaryExpression.op_minus: {
        SymbolicFormula mop = buildTerm(operand, function, ssa);
        return smgr.makeNegate(mop);
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
          SymbolicFormula term = buildTerm(operand, function, ssa);

          // PW make SSA index of * independent from argument
          int idx = getIndex(opname, ssa);
          //int idx = getIndex(
          //    opname, term, ssa, absoluteSSAIndices);

          // build the  function corresponding to this operation.
          return smgr.makeUIF(opname, smgr.makeList(term), idx);

        } else {
          warnUnsafeVar(exp);
          return makeVariable(exprToVarName(exp), function, ssa);
        }

      case IASTUnaryExpression.op_tilde: {
        SymbolicFormula term = buildTerm(operand, function, ssa);
        return smgr.makeBitwiseNot(term);
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
        SymbolicFormula ftmp = makePredicate(exp, true, function, ssa);
        return smgr.makeIfThenElse(ftmp, smgr.makeNumber(1), smgr.makeNumber(0));
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
        SymbolicFormula me2 = buildTerm(e2, function, ssa);
        if (op != IASTBinaryExpression.op_assign) {
          // in this case, we have to get the old SSA instance for
          // reading the value of the variable, and build the
          // corresponding expression
          SymbolicFormula oldvar = buildTerm(e1, function, ssa);
          switch (op) {
          case IASTBinaryExpression.op_plusAssign:
            me2 = smgr.makePlus(oldvar, me2);
            break;
          case IASTBinaryExpression.op_minusAssign:
            me2 = smgr.makeMinus(oldvar, me2);
            break;
          case IASTBinaryExpression.op_multiplyAssign:
            me2 = smgr.makeMultiply(oldvar, me2);
            break;
          case IASTBinaryExpression.op_divideAssign:
            me2 = smgr.makeDivide(oldvar, me2);
            break;
          case IASTBinaryExpression.op_moduloAssign:
            me2 = smgr.makeModulo(oldvar, me2);
            break;
          case IASTBinaryExpression.op_binaryAndAssign:
            me2 = smgr.makeBitwiseAnd(oldvar, me2);
            break;
          case IASTBinaryExpression.op_binaryOrAssign:
            me2 = smgr.makeBitwiseOr(oldvar, me2);
            break;
          case IASTBinaryExpression.op_binaryXorAssign:
            me2 = smgr.makeBitwiseXor(oldvar, me2);
            break;
          case IASTBinaryExpression.op_shiftLeftAssign:
            me2 = smgr.makeShiftLeft(oldvar, me2);
            break;
          case IASTBinaryExpression.op_shiftRightAssign:
            me2 = smgr.makeShiftRight(oldvar, me2);
            break;
          default:
            throw new UnrecognizedCCodeException("Unknown binary operator", null, exp);
          }
        }
        SymbolicFormula mvar = buildLvalueTerm(e1, function, ssa);
        return smgr.makeAssignment(mvar, me2);
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
        SymbolicFormula me1 = buildTerm(e1, function, ssa);
        SymbolicFormula me2 = buildTerm(e2, function, ssa);

        switch (op) {
        case IASTBinaryExpression.op_plus:
          return smgr.makePlus(me1, me2);
        case IASTBinaryExpression.op_minus:
          return smgr.makeMinus(me1, me2);
        case IASTBinaryExpression.op_multiply:
          return smgr.makeMultiply(me1, me2);
        case IASTBinaryExpression.op_divide:
          return smgr.makeDivide(me1, me2);
        case IASTBinaryExpression.op_modulo:
          return smgr.makeModulo(me1, me2);
        case IASTBinaryExpression.op_binaryAnd:
          return smgr.makeBitwiseAnd(me1, me2);
        case IASTBinaryExpression.op_binaryOr:
          return smgr.makeBitwiseOr(me1, me2);
        case IASTBinaryExpression.op_binaryXor:
          return smgr.makeBitwiseXor(me1, me2);
        case IASTBinaryExpression.op_shiftLeft:
          return smgr.makeShiftLeft(me1, me2);
        case IASTBinaryExpression.op_shiftRight:
          return smgr.makeShiftRight(me1, me2);
        default:
          throw new AssertionError("Missing switch case");
        }
      }

      default:
        // this might be a predicate implicitly cast to an int, like this:
        // int tmp = (a == b)
        // Let's see if this is indeed the case...
        SymbolicFormula ftmp = makePredicate(exp, true, function, ssa);
        return smgr.makeIfThenElse(ftmp, smgr.makeNumber(1), smgr.makeNumber(0));
      }

    } else if (exp instanceof IASTFieldReference) {
      if (lvalsAsUif) {
        IASTFieldReference fexp = (IASTFieldReference)exp;
        String field = fexp.getFieldName().getRawSignature();
        IASTExpression owner = fexp.getFieldOwner();
        SymbolicFormula term = buildTerm(owner, function, ssa);

        String tpname = getTypeName(owner.getExpressionType());
        String ufname =
          (fexp.isPointerDereference() ? "->{" : ".{") +
          tpname + "," + field + "}";

        // see above for the case of &x and *x
        return makeUIF(ufname, smgr.makeList(term), ssa);
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
        SymbolicFormula aterm = buildTerm(arrexp, function, ssa);
        SymbolicFormula sterm = buildTerm(subexp, function, ssa);

        String ufname = OP_ARRAY_SUBSCRIPT;
        return makeUIF(ufname, smgr.makeList(aterm, sterm), ssa);

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

  private SymbolicFormula buildLvalueTerm(IASTExpression exp,
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

      SymbolicFormula mvar = smgr.makeVariable(var, idx);
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
      SymbolicFormula term = buildTerm(operand, function, ssa);

      // PW make SSA index of * independent from argument
      int idx = makeLvalIndex(opname, ssa);
      //int idx = makeLvalIndex(opname, term, ssa, absoluteSSAIndices);

      // build the "updated" function corresponding to this operation.
      // what we do is the following:
      // C            |     MathSAT
      // *x = 1       |     <ptr_*>::2(x) = 1
      // ...
      // &(*x) = 2    |     <ptr_&>::2(<ptr_*>::1(x)) = 2
      return smgr.makeUIF(opname, smgr.makeList(term), idx);
      
    } else if (exp instanceof IASTFieldReference) {
      IASTFieldReference fexp = (IASTFieldReference)exp;
      String field = fexp.getFieldName().getRawSignature();
      IASTExpression owner = fexp.getFieldOwner();
      SymbolicFormula term = buildTerm(owner, function, ssa);

      String tpname = getTypeName(owner.getExpressionType());
      String ufname =
        (fexp.isPointerDereference() ? "->{" : ".{") +
        tpname + "," + field + "}";
      SymbolicFormulaList args = smgr.makeList(term);
      int idx = makeLvalIndex(ufname, args, ssa);

      // see above for the case of &x and *x
      return smgr.makeUIF(ufname, args, idx);

    } else if (exp instanceof IASTArraySubscriptExpression) {
      IASTArraySubscriptExpression aexp =
        (IASTArraySubscriptExpression)exp;
      IASTExpression arrexp = aexp.getArrayExpression();
      IASTExpression subexp = aexp.getSubscriptExpression();
      SymbolicFormula aterm = buildTerm(arrexp, function, ssa);
      SymbolicFormula sterm = buildTerm(subexp, function, ssa);

      String ufname = OP_ARRAY_SUBSCRIPT;
      SymbolicFormulaList args = smgr.makeList(aterm, sterm);
      int idx = makeLvalIndex(ufname, args, ssa);

      return smgr.makeUIF(ufname, args, idx);

    } else {
      throw new UnrecognizedCCodeException("Unknown lvalue", null, exp);
    }
  }

  private SymbolicFormula makeExternalFunctionCall(IASTFunctionCallExpression fexp,
        String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {
    IASTExpression fn = fexp.getFunctionNameExpression();
    IASTExpression pexp = fexp.getParameterExpression();
    String func;
    if (fn instanceof IASTIdExpression) {
      func = ((IASTIdExpression)fn).getName().getRawSignature();
      if (NONDET_EXTERNAL_FUNCTIONS.contains(func)) {
        // function call like "random()"
        // ignore parameters and just create a fresh variable for it  
        globalVars.add(func);
        int idx = makeLvalIndex(func, ssa);
        return smgr.makeVariable(func, idx);
        
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
      return smgr.makeVariable(func, 1);
    } else {
      IASTExpression[] args;
      if (pexp instanceof IASTExpressionList) {
        args = ((IASTExpressionList)pexp).getExpressions();
      } else {
        args = new IASTExpression[]{pexp};
      }
      func += "{" + args.length + "}"; // add #arguments to function name to cope with varargs functions
      SymbolicFormula[] mArgs = new SymbolicFormula[args.length];
      for (int i = 0; i < args.length; ++i) {
        mArgs[i] = buildTerm(args[i], function, ssa);
      }

      return smgr.makeUIF(func, smgr.makeList(mArgs));
    }
  }

  protected SymbolicFormula makePredicate(IASTExpression exp, boolean isTrue,
        String function, SSAMapBuilder ssa) throws UnrecognizedCCodeException {
    
    SymbolicFormula result = null;
    
    if (exp instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression)exp);
      int opType = binExp.getOperator();
      IASTExpression op1 = binExp.getOperand1();
      IASTExpression op2 = binExp.getOperand2();

      SymbolicFormula t1 = buildTerm(op1, function, ssa);
      SymbolicFormula t2 = buildTerm(op2, function, ssa);

      switch (opType) {
      case IASTBinaryExpression.op_greaterThan:
        result = smgr.makeGt(t1, t2);
        break;

      case IASTBinaryExpression.op_greaterEqual:
        result = smgr.makeGeq(t1, t2);
        break;

      case IASTBinaryExpression.op_lessThan:
        result = smgr.makeLt(t1, t2);
        break;

      case IASTBinaryExpression.op_lessEqual:
        result = smgr.makeLeq(t1, t2);
        break;

      case IASTBinaryExpression.op_equals:
        result = smgr.makeEqual(t1, t2);
        break;

      case IASTBinaryExpression.op_notequals:
        result = smgr.makeNot(smgr.makeEqual(t1, t2));
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

      if (!smgr.isBoolean(result)) {
        SymbolicFormula z = smgr.makeNumber(0);
        result = smgr.makeNot(smgr.makeEqual(result, z));
      }
    }
    
    if (!isTrue) {
      result = smgr.makeNot(result);
    }
    return result;
  }
  
  protected void addToGlobalVars(String pVar){
    globalVars.add(pVar);
  }
}
