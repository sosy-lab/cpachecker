/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package symbpredabstraction.mathsat;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;
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
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;

import symbpredabstraction.PathFormula;
import symbpredabstraction.SSAMap;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import symbpredabstraction.interfaces.TheoremProver;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cfa.objectmodel.c.StatementEdge;
import cmdline.CPAMain;

import common.Pair;

import exceptions.UnrecognizedCFAEdgeException;


/**
 * A SymbolicFormulaManager to deal with MathSAT formulas.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class MathsatSymbolicFormulaManager implements SymbolicFormulaManager {

  public final static int THEORY_EQ = 1;
  public final static int THEORY_UF = 2;
  public final static int THEORY_ARITH = 4;

  // the MathSAT environment in which all terms are created
  private long msatEnv;
  // We need to distinguish assignments from tests. This is needed to
  // build a formula in SSA form later on, when we have to mapback
  // a counterexample, without adding too many extra variables. Therefore,
  // in the representation of "uninstantiated" symbolic formulas, we
  // use a new binary uninterpreted function ":=" to represent
  // assignments. When we instantiate the formula, we replace this UIF
  // with an equality, because now we have an SSA form
  private long assignUfDecl;

  // UF encoding of some unsupported operations
  private long bitwiseAndUfDecl;
  private long bitwiseOrUfDecl;
  private long bitwiseXorUfDecl;
  // private long bitwiseNotUfDecl;
  private long leftShiftUfDecl;
  private long rightShiftUfDecl;
  private long multUfDecl;
  private long divUfDecl;
  private long modUfDecl;

  // datatype to use for variables, when converting them to mathsat vars
  // can be either MSAT_REAL or MSAT_INT
  // Note that MSAT_INT does not mean that we support the full linear
  // integer arithmetic (LIA)! At the moment, interpolation doesn't work on
  // LIA, only difference logic or on LRA (i.e. on the rationals). However
  // by setting the vars to be MSAT_INT, the solver tries some heuristics
  // that might work (e.g. tightening of a < b into a <= b - 1, splitting
  // negated equalities, ...)
  private int msatVarType = mathsat.api.MSAT_REAL;

  // names for special variables needed to deal with functions
  private static final String VAR_RETURN_NAME = "<retval>";
  private static final String FUNCTION_PARAM_NAME = "<param>";
  private static final String OP_ADDRESSOF_NAME = "<ptr_&>";
  private static final String OP_STAR_NAME = "<ptr_*>";
  private static final String OP_ARRAY_SUBSCRIPT = "<arr_[]>";
  private static final String STRING_LIT_FUNCTION = "<string>";

  // a namespace to have a unique name for each variable in the program.
  // Whenever we enter a function, we push its name as namespace. Each
  // variable will be instantiated inside mathsat as namespace::variable
  //private Stack<String> namespaces;
  private String namespace;
  // global variables (do not live in any namespace)
  private Set<String> globalVars;

  // various caches for speeding up expensive tasks
  //
  // cache for splitting arithmetic equalities in extractAtoms
  private Map<Long, Boolean> arithCache;

  // cache for uninstantiating terms (see uninstantiate() below)
  private Map<Long, Long> uninstantiateGlobalCache;

  // cache for replacing assignments
  private Map<Long, Long> replaceAssignmentsCache;
  private Map<Long, Integer> neededTheories;

  private Set<IASTExpression> warnedUnsafeVars =
    new HashSet<IASTExpression>();

  // set of functions that can be lvalues
  private Set<String> lvalueFunctions;

  private Map<String, Long> stringLitToMsat;
  private int nextStringLitIndex;
  private long stringLitUfDecl;

  // if true, handle lvalues as *x, &x, s.x, etc. using UIFs. If false, just
  // ue variables
  private boolean lvalsAsUif;

  public MathsatSymbolicFormulaManager() {
    msatEnv = mathsat.api.msat_create_env();
    if (CPAMain.cpaConfig.getBooleanValue(
        "cpas.symbpredabs.mathsat.useIntegers")) {
      msatVarType = mathsat.api.MSAT_INT;
    } else {
      msatVarType = mathsat.api.MSAT_REAL;
    }
    int[] argtypes = {msatVarType, msatVarType};
    assignUfDecl = mathsat.api.msat_declare_uif(msatEnv, ":=",
        mathsat.api.MSAT_BOOL, 2, argtypes);
    setNamespace("");
    globalVars = new HashSet<String>();
    arithCache = new HashMap<Long, Boolean>();

    bitwiseAndUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_&_",
        msatVarType, 2, argtypes);
    bitwiseOrUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_|_",
        msatVarType, 2, argtypes);
    bitwiseXorUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_^_",
        msatVarType, 2, argtypes);
    // bitwiseNotUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_~_",
    //         msatVarType, 1, new int[]{msatVarType});
    leftShiftUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_<<_",
        msatVarType, 2, argtypes);
    rightShiftUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_>>_",
        msatVarType, 2, argtypes);
    stringLitUfDecl = mathsat.api.msat_declare_uif(msatEnv,
        STRING_LIT_FUNCTION, msatVarType, 1, new int[]{msatVarType});
    multUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_*_",
        msatVarType, 2, argtypes);
    divUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_/_",
        msatVarType, 2, argtypes);
    modUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_%_",
        msatVarType, 2, argtypes);

    uninstantiateGlobalCache = new HashMap<Long, Long>();
    replaceAssignmentsCache = new HashMap<Long, Long>();
    neededTheories = new HashMap<Long, Integer>();

    lvalueFunctions = new HashSet<String>();

    nextStringLitIndex = 0;
    stringLitToMsat = new HashMap<String, Long>();

    lvalsAsUif = CPAMain.cpaConfig.getBooleanValue(
        "cpas.symbpredabs.mathsat.lvalsAsUIFs");
  }

  /**
   * Usage is deprecated outside of the symbpredabstraction.mathsat package.
   * After all external references to this method have been eliminated, it should
   * be made package private and the deprecation tag may be removed again.
   */
  @Deprecated
  public long getMsatEnv() {
    return msatEnv;
  }

  @Override
  public SymbolicFormula createPredicateVariable(SymbolicFormula atom) {
    long tt = ((MathsatSymbolicFormula)atom).getTerm();
    assert(!mathsat.api.MSAT_ERROR_TERM(tt));

    String repr = mathsat.api.msat_term_is_atom(tt) != 0 ?
                    mathsat.api.msat_term_repr(tt) :
                      ("#" + mathsat.api.msat_term_id(tt));
    long d = mathsat.api.msat_declare_variable(msatEnv,
        "\"PRED" + repr + "\"",
        mathsat.api.MSAT_BOOL);
    long var = mathsat.api.msat_make_variable(msatEnv, d);
    assert(!mathsat.api.MSAT_ERROR_TERM(var));
    
    return new MathsatSymbolicFormula(var);
  }
  
  public String dumpFormula(SymbolicFormula f) {
    MathsatSymbolicFormula m = (MathsatSymbolicFormula)f;
    
    return mathsat.api.msat_to_msat(msatEnv, m.getTerm());
  }
  
  public boolean entails(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

    // create a temporary environment for checking the implication
    long env = mathsat.api.msat_create_env();
    mathsat.api.msat_add_theory(env, mathsat.api.MSAT_UF);
    if (CPAMain.cpaConfig.getBooleanValue(
        "cpas.symbpredabs.mathsat.useIntegers")) {
      mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LIA);
      int ok = mathsat.api.msat_set_option(env, "split_eq", "true");
      assert(ok == 0);
    } else {
      mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LRA);
    }
    mathsat.api.msat_set_theory_combination(env, mathsat.api.MSAT_COMB_DTC);

    long t1 = mathsat.api.msat_make_copy_from(env, m1.getTerm(), msatEnv);
    long t2 = mathsat.api.msat_make_copy_from(env, m2.getTerm(), msatEnv);
    long imp = mathsat.api.msat_make_implies(env, t1, t2);
    mathsat.api.msat_assert_formula(env,
        mathsat.api.msat_make_not(env, imp));
    int res = mathsat.api.msat_solve(env);

    mathsat.api.msat_destroy_env(env);

    boolean ret = (res == mathsat.api.MSAT_UNSAT);

    return ret;
  }

  public boolean entails(SymbolicFormula f1, SymbolicFormula f2,
      TheoremProver thmProver) {
    SymbolicFormula toCheck = makeAnd(f1, makeNot(f2));
    
    thmProver.init(TheoremProver.ENTAILMENT_CHECK);
    boolean ret = thmProver.isUnsat(toCheck);
    thmProver.reset();

    return ret;
  }


  public SymbolicFormula makeNot(SymbolicFormula f) {
    MathsatSymbolicFormula m = (MathsatSymbolicFormula)f;

    long a = mathsat.api.msat_make_not(msatEnv, m.getTerm());
    return new MathsatSymbolicFormula(a);
  }
  
  
  public SymbolicFormula makeAnd(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

    long a = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), m2.getTerm());
    return new MathsatSymbolicFormula(a);
  }


  public SymbolicFormula makeOr(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

    long a = mathsat.api.msat_make_or(msatEnv, m1.getTerm(), m2.getTerm());
    return new MathsatSymbolicFormula(a);
  }

  public SymbolicFormula makeEquivalence(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

    long a = mathsat.api.msat_make_iff(msatEnv, m1.getTerm(), m2.getTerm());
    return new MathsatSymbolicFormula(a);
  }

  public SymbolicFormula makeTrue() {
    return new MathsatSymbolicFormula(mathsat.api.msat_make_true(msatEnv));
  }
  
  public SymbolicFormula makeIfThenElse(SymbolicFormula atom, SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula mAtom = (MathsatSymbolicFormula)atom;
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;
    
    long ite = mathsat.api.msat_make_ite(msatEnv, mAtom.getTerm(), m1.getTerm(), m2.getTerm());

    return new MathsatSymbolicFormula(ite);
  }
  
  public PathFormula makeAnd(
      SymbolicFormula f1, CFAEdge edge, SSAMap ssa,
      boolean absoluteSSAIndices)
      throws UnrecognizedCFAEdgeException {
    // this is where the "meat" is... We have to parse the statement
    // attached to the edge, and convert it to the appropriate formula

    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;

    if (edge.getPredecessor() != null) {
      setNamespace(edge.getPredecessor().getFunctionName());
    }

    if (edge.getPredecessor() instanceof FunctionDefinitionNode) {
      // function start
      Pair<SymbolicFormula, SSAMap> p = makeAndEnterFunction(
          m1, (FunctionDefinitionNode)edge.getPredecessor(), ssa, absoluteSSAIndices);
      m1 = (MathsatSymbolicFormula)p.getFirst();
      f1 = m1;
      ssa = p.getSecond();
    }

    switch (edge.getEdgeType()) {
    case StatementEdge: {
      StatementEdge statementEdge = (StatementEdge)edge;

      if (statementEdge.isJumpEdge()) {
        if (statementEdge.getSuccessor().getFunctionName().equals(
            "main")) {
          CPAMain.logManager.log(Level.ALL, "DEBUG_3",
              "MathsatSymbolicFormulaManager, IGNORING return ",
              "from main: ", edge.getRawStatement());
        } else {
          return makeAndReturn(m1, statementEdge, ssa, absoluteSSAIndices);
        }
      } else {
        return makeAndStatement(m1, statementEdge, ssa, absoluteSSAIndices);
      }
      break;
    }

    case DeclarationEdge: {
      return makeAndDeclaration(m1, (DeclarationEdge)edge, ssa, absoluteSSAIndices);
    }

    case AssumeEdge: {
      return makeAndAssume(m1, (AssumeEdge)edge, ssa, absoluteSSAIndices);
    }

    case BlankEdge: {
      break;
    }

    case FunctionCallEdge: {
      return makeAndFunctionCall(m1, (FunctionCallEdge)edge, ssa, absoluteSSAIndices);
    }

    case ReturnEdge: {
      // get the expression from the summary edge
      CFANode succ = edge.getSuccessor();
      CallToReturnEdge ce = succ.getEnteringSummaryEdge();
      PathFormula ret =
        makeAndExitFunction(m1, ce, ssa, absoluteSSAIndices);
      //popNamespace(); - done inside makeAndExitFunction
      return ret;
    }

    default:
      throw new UnrecognizedCFAEdgeException(edge);
    }

    return new PathFormula(f1, ssa);
  }

  private PathFormula makeAndDeclaration(
      MathsatSymbolicFormula m1, DeclarationEdge declarationEdge, SSAMap ssa,
      boolean absoluteSSAIndices) throws UnrecognizedCFAEdgeException {
    // at each declaration, we instantiate the variable in the SSA:
    // this is to avoid problems with uninitialized variables
    SSAMap newssa = new SSAMap();
    newssa.copyFrom(ssa);

    IASTDeclarator[] decls = declarationEdge.getDeclarators();
    IASTDeclSpecifier spec = declarationEdge.getDeclSpecifier();

    boolean isGlobal = declarationEdge instanceof GlobalDeclarationEdge;

    if (spec instanceof IASTEnumerationSpecifier) {
      // extract the fields, and add them as global variables
      assert(isGlobal);
      IASTEnumerationSpecifier.IASTEnumerator[] enums =
        ((IASTEnumerationSpecifier)spec).getEnumerators();
      for (IASTEnumerationSpecifier.IASTEnumerator e : enums) {
        String var = e.getName().getRawSignature();
        globalVars.add(var);
        IASTExpression exp = e.getValue();
        assert(exp != null);

        int idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 1;
        newssa.setIndex(var, idx);

        CPAMain.logManager.log(Level.ALL, "DEBUG_3",
            "Declared enum field: ", var, ", index: ", idx);

        long minit = buildMsatTerm(exp, newssa, absoluteSSAIndices);
        long mvar = buildMsatVariable(var, idx);
        long t = makeAssignment(mvar, minit);
        t = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), t);
        m1 = new MathsatSymbolicFormula(t);
      }
      return new PathFormula(m1, newssa);
    }


    if (!(spec instanceof IASTSimpleDeclSpecifier ||
        spec instanceof IASTElaboratedTypeSpecifier ||
        spec instanceof IASTNamedTypeSpecifier)) {

      if (spec instanceof IASTCompositeTypeSpecifier) {
        // this is the declaration of a struct, just ignore it...
        warn("IGNORING declaration: " + declarationEdge.getRawStatement());
        return new PathFormula(m1, newssa);
      } else {
        throw new UnrecognizedCFAEdgeException(
            "UNSUPPORTED SPECIFIER FOR DECLARATION: " +
            declarationEdge.getRawStatement());
      }
    }

    if (spec.getStorageClass() == IASTDeclSpecifier.sc_typedef) {
      warn("IGNORING typedef: " + declarationEdge.getRawStatement());
      return new PathFormula(m1, newssa);
    }

    for (IASTDeclarator d : decls) {
      String var = d.getName().getRawSignature();
      if (isGlobal) {
        globalVars.add(var);
      }
      var = scoped(var);
      int idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() :
        getNewIndex(var, newssa)/*1*/;
      newssa.setIndex(var, idx);

      CPAMain.logManager.log(Level.ALL, "DEBUG_3",
          "Declared variable: ", var, ", index: ", idx);
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
      if (d.getInitializer() != null) {
        IASTInitializer init = d.getInitializer();
        if (!(init instanceof IASTInitializerExpression)) {
//        throw new UnrecognizedCFAEdgeException(
//        "BAD INITIALIZER: " + edge.getRawStatement());
          warn("UNSUPPORTED INITIALIZER: " +
              declarationEdge.getRawStatement() + ", ignoring it!");
          continue;
        }
        IASTExpression exp =
          ((IASTInitializerExpression)init).getExpression();
        long minit = buildMsatTerm(exp, newssa, absoluteSSAIndices);
        long mvar = buildMsatVariable(var, idx);
        long t = makeAssignment(mvar, minit);
        t = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), t);
        m1 = new MathsatSymbolicFormula(t);
      } else if (spec.getStorageClass() ==
        IASTDeclSpecifier.sc_extern) {
        warn("NOT initializing, because extern declaration: " +
            declarationEdge.getRawStatement());
      } else if (isGlobal ||
          CPAMain.cpaConfig.getBooleanValue(
              "cpas.symbpredabs.initAllVars")) {
        // auto-initialize variables to zero, unless they match
        // the noAutoInitPrefix pattern
        String noAutoInit = CPAMain.cpaConfig.getProperty(
            "cpas.symbpredabs.noAutoInitPrefix", "");
        if (noAutoInit.equals("") ||
            !d.getName().getRawSignature().startsWith(noAutoInit)) {
          long mvar = buildMsatVariable(var, idx);
          long z = mathsat.api.msat_make_number(msatEnv, "0");
          long t = makeAssignment(mvar, z);
          t = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), t);
          m1 = new MathsatSymbolicFormula(t);
          CPAMain.logManager.log(Level.ALL, "DEBUG_3", "AUTO-INITIALIZING",
              (isGlobal ? "GLOBAL" : ""), "VAR: ",
              var, " (", d.getName().getRawSignature(), ")");
        } else {
          CPAMain.logManager.log(Level.ALL, "DEBUG_3",
              "NOT AUTO-INITIALIZING VAR:", var);
        }
      }
    }
    return new PathFormula(m1, newssa);
  }
  
  private Pair<SymbolicFormula, SSAMap> makeAndEnterFunction(
      MathsatSymbolicFormula m1, FunctionDefinitionNode fn, SSAMap ssa,
      boolean absoluteSSAIndices)
      throws UnrecognizedCFAEdgeException {
    List<IASTParameterDeclaration> params = fn.getFunctionParameters();
    if (params.isEmpty()) {
      return new Pair<SymbolicFormula, SSAMap>(m1, ssa);
    }

    SSAMap newssa = new SSAMap();
    newssa.copyFrom(ssa);
    ssa = newssa;

    long term = mathsat.api.msat_make_true(msatEnv);
    int i = 0;
    for (IASTParameterDeclaration param : params) {
      String paramName = scoped(FUNCTION_PARAM_NAME + (i++));
      int idx = ssa.getIndex(paramName);
      if (idx < 0) {
        idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 1;
      }
      long msatParam = buildMsatVariable(paramName, idx);
      if (mathsat.api.MSAT_ERROR_TERM(msatParam)) {
        throw new UnrecognizedCFAEdgeException(
            "ERROR ENTERING FUNCTION: " +
            fn.getFunctionDefinition().getRawSignature());
      }
      if (param.getDeclarator().getPointerOperators().length != 0) {
        warn("Ignoring the semantics of pointer for paramenter: " +
            param.getDeclarator().getName().toString() +
            " in function: " + fn.getFunctionName());
      }
      String pn = param.getDeclarator().getName().toString();
      if (pn.isEmpty()) {
        assert(param.getDeclarator().getNestedDeclarator() != null);
        pn = param.getDeclarator().getNestedDeclarator().
        getName().toString();
      }
      assert(!pn.isEmpty());
      String formalParamName = scoped(pn);
      idx = ssa.getIndex(formalParamName);
      if (idx < 0 || absoluteSSAIndices) {
        idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 2;
      } else {
        idx = getNewIndex(formalParamName, ssa);
      }
      long msatFormalParam = buildMsatVariable(formalParamName, idx);
      if (mathsat.api.MSAT_ERROR_TERM(msatParam)) {
        throw new UnrecognizedCFAEdgeException(
            "ERROR HANDLING FUNCTION CALL: " +
            fn.getFunctionDefinition().getRawSignature());
      }
      ssa.setIndex(formalParamName, idx);
      long eq = makeAssignment(msatFormalParam, msatParam);
      term = mathsat.api.msat_make_and(msatEnv, term, eq);
    }
    term = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), term);
    return new Pair<SymbolicFormula, SSAMap>(
        new MathsatSymbolicFormula(term), ssa);
  }

  private void warn(String msg) {
    CPAMain.logManager.log(Level.ALL, "DEBUG_2", "WARNING:", msg);
  }

  private PathFormula makeAndExitFunction(
      MathsatSymbolicFormula m1, CallToReturnEdge ce, SSAMap ssa,
      boolean absoluteSSAIndices)
      throws UnrecognizedCFAEdgeException {
    IASTExpression retExp = ce.getExpression();
    if (retExp instanceof IASTFunctionCallExpression) {
      // this should be a void return, just do nothing...
      //popNamespace();
      return new PathFormula(m1, ssa);
    } else if (retExp instanceof IASTBinaryExpression) {
      IASTBinaryExpression exp = (IASTBinaryExpression)retExp;
      assert(exp.getOperator() == IASTBinaryExpression.op_assign);
      String retvar = scoped(VAR_RETURN_NAME);

      SSAMap newssa = new SSAMap();
      newssa.copyFrom(ssa);
      ssa = newssa;

      int retidx = ssa.getIndex(retvar);
      if (retidx < 0) {
        retidx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 1;
      }
      ssa.setIndex(retvar, retidx);
      long msatretvar = buildMsatVariable(retvar, retidx);
      IASTExpression e = exp.getOperand1();
      setNamespace(ce.getSuccessor().getFunctionName());
      long msatoutvar = buildMsatLvalueTerm(e, ssa, absoluteSSAIndices);
      long term = makeAssignment(msatoutvar, msatretvar);
      term = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), term);
      return new PathFormula(
          new MathsatSymbolicFormula(term), ssa);
    } else {
      throw new UnrecognizedCFAEdgeException(
          "UNKNOWN FUNCTION EXIT EXPRESSION: " +
          ce.getRawStatement());
    }
  }

  private PathFormula makeAndFunctionCall(
      MathsatSymbolicFormula m1, FunctionCallEdge edge, SSAMap ssa,
      boolean absoluteSSAIndices) throws UnrecognizedCFAEdgeException {
    if (edge.isExternalCall()) {
      throw new UnrecognizedCFAEdgeException(
          "EXTERNAL CALL UNSUPPORTED: " + edge.getRawStatement());
    } else {
      SSAMap newssa = new SSAMap();
      newssa.copyFrom(ssa);
      ssa = newssa;
      
      // build the actual parameters in the caller's context
      long[] msatActualParams;
      if (edge.getArguments() == null) {
        msatActualParams = new long[0];
      } else {
        msatActualParams = new long[edge.getArguments().length];
        IASTExpression[] actualParams = edge.getArguments();
        for (int i = 0; i < msatActualParams.length; ++i) {
          msatActualParams[i] = buildMsatTerm(actualParams[i], ssa,
              absoluteSSAIndices);
          if (mathsat.api.MSAT_ERROR_TERM(msatActualParams[i])) {
            throw new UnrecognizedCFAEdgeException(
                "ERROR CONVERTING: " + edge.getRawStatement());
          }
        }
      }
      // now switch to the context of the function
      FunctionDefinitionNode fn =
        (FunctionDefinitionNode)edge.getSuccessor();
      setNamespace(fn.getFunctionName());
      // create the symbolic vars for the formal parameters
      List<IASTParameterDeclaration> formalParams =
        fn.getFunctionParameters();
      assert(formalParams.size() == msatActualParams.length);

      int i = 0;
      long term = mathsat.api.msat_make_true(msatEnv);
      for (IASTParameterDeclaration param : formalParams) {
        long arg = msatActualParams[i++];
        if (param.getDeclarator().getPointerOperators().length != 0) {
          warn("Ignoring the semantics of pointer for paramenter: " +
              param.getDeclarator().getName().toString() +
              " in function: " + fn.getFunctionName());
        }
        String paramName = scoped(FUNCTION_PARAM_NAME + (i-1));
        int idx = ssa.getIndex(paramName);
        if (idx < 0 || absoluteSSAIndices) {
          idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 2;
        } else {
          idx = getNewIndex(paramName, ssa);
        }
        long msatParam = buildMsatVariable(paramName, idx);
        if (mathsat.api.MSAT_ERROR_TERM(msatParam)) {
          throw new UnrecognizedCFAEdgeException(
              "ERROR HANDLING FUNCTION CALL: " +
              edge.getRawStatement());
        }
        ssa.setIndex(paramName, idx);
        long eq = makeAssignment(msatParam, arg);
        term = mathsat.api.msat_make_and(msatEnv, term, eq);
      }
      assert(!mathsat.api.MSAT_ERROR_TERM(term));
      term = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), term);
      return new PathFormula(
          new MathsatSymbolicFormula(term), ssa);
    }
  }

  private PathFormula makeAndReturn(
      MathsatSymbolicFormula m1, StatementEdge edge, SSAMap ssa,
      boolean absoluteSSAIndices)
      throws UnrecognizedCFAEdgeException {
    IASTExpression exp = edge.getExpression();
    if (exp == null) {
      // this is a return from a void function, do nothing
      return new PathFormula(m1, ssa);
    } else if (exp instanceof IASTUnaryExpression) {
      // we have to save the information about the return value,
      // so that we can use it later on, if it is assigned to
      // a variable. We create a function::<retval> variable
      // that will hold the return value
      String retvalname = scoped(VAR_RETURN_NAME);
      int oldidx = ssa.getIndex(retvalname);
      int idx = 0;
      if (absoluteSSAIndices) {
        idx = SSAMap.getNextSSAIndex();
        if (idx == 1) ++idx;
      } else if (oldidx < 0) {
        idx = 2;
      } else {
        idx = getNewIndex(retvalname, ssa);
      }
      assert(idx > 1);

      SSAMap ssa2 = new SSAMap();
      ssa2.copyFrom(ssa);
      ssa = ssa2;

      long retvar = buildMsatVariable(retvalname, idx);
      ssa.setIndex(retvalname, idx);
      long retval = buildMsatTerm(exp, ssa, absoluteSSAIndices);
      if (!mathsat.api.MSAT_ERROR_TERM(retval)) {
        long term = makeAssignment(retvar, retval);
        if (!mathsat.api.MSAT_ERROR_TERM(term)) {
          term = mathsat.api.msat_make_and(
              msatEnv, m1.getTerm(), term);
          assert(!mathsat.api.MSAT_ERROR_TERM(term));
          return new PathFormula(
              new MathsatSymbolicFormula(term), ssa);
        }
      }
    }
    // if we are here, we can't handle the return properly...
    throw new UnrecognizedCFAEdgeException(edge);
  }

  private void setNamespace(String ns) {
    namespace = ns;
  }

  private String getNamespace() {
    return namespace;
  }

  private long buildMsatVariable(String var, int idx) {
    long decl = mathsat.api.msat_declare_variable(
        msatEnv, var + "@" + idx, msatVarType);
    return mathsat.api.msat_make_variable(msatEnv, decl);
  }

  private long buildMsatUFLvalue(String name, long[] args, int idx) {
    int[] tp = new int[args.length];
    for (int i = 0; i < tp.length; ++i) tp[i] = msatVarType;
    long decl = mathsat.api.msat_declare_uif(
        msatEnv, name + "@" + idx, msatVarType, tp.length, tp);
    return mathsat.api.msat_make_uif(msatEnv, decl, args);
  }

  private long buildMsatUFLvalue(
      String name, SymbolicFormula[] args, int idx) {
    long[] a = new long[args.length];
    for (int i = 0; i < a.length; ++i) {
      a[i] = ((MathsatSymbolicFormula)args[i]).getTerm();
    }
    return buildMsatUFLvalue(name, a, idx);
  }

  private int autoInstantiateVar(String var, SSAMap ssa) {
    CPAMain.logManager.log(Level.ALL, "DEBUG_3",
        "WARNING: Auto-instantiating variable: ", var);
    ssa.setIndex(var, 1);
    return 1;
  }

  private int autoInstantiateLvalue(String name, SymbolicFormula[] args,
      SSAMap ssa) {
    if (args.length == 1) {
      CPAMain.logManager.log(Level.ALL, "DEBUG_3",
          "WARNING: Auto-instantiating lval: ", name, "(", args[0],
      ")");
    } else if (args.length == 2) {
      CPAMain.logManager.log(Level.ALL, "DEBUG_3",
          "WARNING: Auto-instantiating lval: ", name, "(", args[0],
          ",", args[1], ")");
    } else {
      CPAMain.logManager.log(Level.ALL, "DEBUG_3",
          "WARNING: Auto-instantiating lval: ", name, "(", args, ")");
    }
    ssa.setIndex(name, args, 1);
    return 1;
  }


  private String exprToVarName(IASTExpression e) {
    return e.getRawSignature().replaceAll("[ \n\t]", "");
  }

  private long buildMsatTermVar(String var, SSAMap ssa,
      boolean absoluteSSAIndices) {
    var = scoped(var);
    int idx = ssa.getIndex(var);
    if (idx <= 0) {
      if (absoluteSSAIndices) {
        // should not happen...
        return mathsat.api.MSAT_MAKE_ERROR_TERM();
      } else {
        // this might happen in this case
        idx = autoInstantiateVar(var, ssa);
      }
    }
    return buildMsatVariable(var, idx);
  }

  private long buildMsatTerm(IASTExpression exp, SSAMap ssa,
      boolean absoluteSSAIndices) {
    if (exp instanceof IASTIdExpression) {
      // this is a variable: get the right index for the SSA
      String var = ((IASTIdExpression)exp).getName().getRawSignature();
      return buildMsatTermVar(var, ssa, absoluteSSAIndices);
    } else if (exp instanceof IASTLiteralExpression) {
      // this should be a number...
      IASTLiteralExpression lexp = (IASTLiteralExpression)exp;
      String num = lexp.getRawSignature();
      switch (lexp.getKind()) {
      case IASTLiteralExpression.lk_integer_constant:
      case IASTLiteralExpression.lk_float_constant:
        if (num.startsWith("0x")) {
          // this should be in hex format
          // we use Long instead of Integer to avoid getting negative
          // numbers (e.g. for 0xffffff we would get -1)
          num = Long.valueOf(num, 16).toString();
        } else {
          // this might have some modifiers attached (e.g. 0UL), we
          // have to get rid of them
          int pos = num.length()-1;
          while (!Character.isDigit(num.charAt(pos))) {
            --pos;
          }
          num = num.substring(0, pos+1);
        }
        break;
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
        num = "" + n;

      }
      break;
      case IASTLiteralExpression.lk_string_literal: {
        // we create a string constant representing the given
        // string literal
        if (stringLitToMsat.containsKey(exp.getRawSignature())) {
          return stringLitToMsat.get(exp.getRawSignature());
        } else {
          // generate a new string literal. We generate a new UIf
          String n = "" + (nextStringLitIndex++);
          long[] arg = {mathsat.api.msat_make_number(msatEnv, n)};
          long t = mathsat.api.msat_make_uif(msatEnv,
              stringLitUfDecl, arg);
          stringLitToMsat.put(exp.getRawSignature(), t);
          return t;
        }
      }
      default:
        CPAMain.logManager.log(Level.ALL, "DEBUG_1",
            "ERROR, UNKNOWN LITERAL: ", exp.getRawSignature());
      return mathsat.api.MSAT_MAKE_ERROR_TERM();
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
      return mathsat.api.msat_make_number(msatEnv, num);
    } else if (exp instanceof IASTCastExpression) {
      // we completely ignore type casts
      CPAMain.logManager.log(Level.ALL, "DEBUG_3", "IGNORING TYPE CAST:",
          exp.getRawSignature());
      return buildMsatTerm(((IASTCastExpression)exp).getOperand(),
          ssa, absoluteSSAIndices);
    } else if (exp instanceof IASTUnaryExpression) {
      IASTExpression operand = ((IASTUnaryExpression)exp).getOperand();
      int op = ((IASTUnaryExpression)exp).getOperator();
      switch (op) {
      case IASTUnaryExpression.op_bracketedPrimary:
        return buildMsatTerm(operand, ssa, absoluteSSAIndices);
      case IASTUnaryExpression.op_postFixIncr:
      case IASTUnaryExpression.op_prefixIncr:
      case IASTUnaryExpression.op_postFixDecr:
      case IASTUnaryExpression.op_prefixDecr: {
        long mvar = buildMsatTerm(operand, ssa, absoluteSSAIndices);
        long newvar =
          buildMsatLvalueTerm(operand, ssa, absoluteSSAIndices);
        if (mathsat.api.MSAT_ERROR_TERM(mvar)) return mvar;
        if (mathsat.api.MSAT_ERROR_TERM(newvar)) return newvar;
        long me;
        long one = mathsat.api.msat_make_number(msatEnv, "1");
        if (op == IASTUnaryExpression.op_postFixIncr ||
            op == IASTUnaryExpression.op_prefixIncr) {
          me = mathsat.api.msat_make_plus(msatEnv, mvar, one);
        } else {
          me = mathsat.api.msat_make_minus(msatEnv, mvar, one);
        }
        if (mathsat.api.MSAT_ERROR_TERM(me)) return me;
        return makeAssignment(newvar, me);
      }

      case IASTUnaryExpression.op_minus: {
        long mop = buildMsatTerm(operand, ssa, absoluteSSAIndices);
        if (mathsat.api.MSAT_ERROR_TERM(mop)) return mop;
        return mathsat.api.msat_make_negate(msatEnv, mop);
      }

      case IASTUnaryExpression.op_amper:
      case IASTUnaryExpression.op_star:
        if (lvalsAsUif) {
          String opname = null;
          if (op == IASTUnaryExpression.op_amper) {
            opname = OP_ADDRESSOF_NAME;
          } else {
            opname = OP_STAR_NAME;
          }
          long term = buildMsatTerm(operand, ssa, absoluteSSAIndices);
          if (mathsat.api.MSAT_ERROR_TERM(term)) return term;
          
          // PW make SSA index of * independent from argument
          int idx = getLvalIndex(opname, ssa, absoluteSSAIndices);
          //int idx = getNormalIndex(
          //    opname, term, ssa, absoluteSSAIndices);
          
          if (idx <= 0) return mathsat.api.MSAT_MAKE_ERROR_TERM();
          // build the  function corresponding to this operation.
          long decl = mathsat.api.msat_declare_uif(
              msatEnv, opname + "@" + idx, msatVarType, 1,
              new int[]{msatVarType});
          return mathsat.api.msat_make_uif(msatEnv, decl,
              new long[]{term});
        } else {
          warnUnsafeVar(exp);
          return buildMsatTermVar(exprToVarName(exp), ssa,
              absoluteSSAIndices);
        }

      case IASTUnaryExpression.op_sizeof: {
        // TODO
        //return mathsat.api.MSAT_MAKE_ERROR_TERM();
        warnUnsafeVar(exp);
        return buildMsatTermVar(exprToVarName(exp), ssa,
            absoluteSSAIndices);
      }

      default: {
        // this might be a predicate implicitly cast to an int. Let's
        // see if this is indeed the case...
        MathsatSymbolicFormula ftmp = buildFormulaPredicate(
            exp, true, ssa, absoluteSSAIndices);
        if (ftmp == null) {
          return mathsat.api.MSAT_MAKE_ERROR_TERM();
        } else {
          assert(false);
          return mathsat.api.msat_make_ite(msatEnv, ftmp.getTerm(),
              mathsat.api.msat_make_number(msatEnv, "1"),
              mathsat.api.msat_make_number(msatEnv, "0"));
        }
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
        long me2 = buildMsatTerm(e2, ssa, absoluteSSAIndices);
        if (mathsat.api.MSAT_ERROR_TERM(me2)) return me2;
        if (op != IASTBinaryExpression.op_assign) {
          // in this case, we have to get the old SSA instance for
          // reading the value of the variable, and build the
          // corresponding expression
          long oldvar = buildMsatTerm(e1, ssa, absoluteSSAIndices);
          if (mathsat.api.MSAT_ERROR_TERM(oldvar)) return oldvar;
          switch (op) {
          case IASTBinaryExpression.op_plusAssign:
            me2 = mathsat.api.msat_make_plus(msatEnv, oldvar, me2);
            break;
          case IASTBinaryExpression.op_minusAssign:
            me2 = mathsat.api.msat_make_minus(msatEnv, oldvar, me2);
            break;
          case IASTBinaryExpression.op_multiplyAssign:
            if (mathsat.api.msat_term_is_number(me2) != 0) {
              me2 = mathsat.api.msat_make_times(
                  msatEnv, oldvar, me2);
            } else {
              me2 = buildMsatUF(op, oldvar, me2);
            }
            break;
          case IASTBinaryExpression.op_divideAssign:
            if (mathsat.api.msat_term_is_number(me2) != 0) {
              String n = mathsat.api.msat_term_repr(me2);
              if (n.startsWith("(")) {
                n = n.substring(1, n.length()-1);
              }
              String[] frac = n.split("/");
              if (frac.length == 1) {
                n = "1/" + n;
              } else {
                assert(frac.length == 2);
                n = frac[1] + "/" + frac[0];
              }
              me2 = mathsat.api.msat_make_number(msatEnv, n);
              if (!mathsat.api.MSAT_ERROR_TERM(me2)) {
                me2 = mathsat.api.msat_make_times(
                    msatEnv, oldvar, me2);
              }
            } else {
              me2 = buildMsatUF(op, oldvar, me2);
            }
            break;
          case IASTBinaryExpression.op_moduloAssign:
          case IASTBinaryExpression.op_binaryAndAssign:
          case IASTBinaryExpression.op_binaryOrAssign:
          case IASTBinaryExpression.op_binaryXorAssign:
          case IASTBinaryExpression.op_shiftLeftAssign:
          case IASTBinaryExpression.op_shiftRightAssign:
            me2 = buildMsatUF(op, oldvar, me2);
            break;
          }
          if (mathsat.api.MSAT_ERROR_TERM(me2)) return me2;
        }
        long mvar = buildMsatLvalueTerm(e1, ssa, absoluteSSAIndices);
        if (mathsat.api.MSAT_ERROR_TERM(mvar)) return mvar;
        return makeAssignment(mvar, me2);
      }

      case IASTBinaryExpression.op_plus:
      case IASTBinaryExpression.op_minus:
      case IASTBinaryExpression.op_multiply:
      case IASTBinaryExpression.op_divide: {
        long me1 = buildMsatTerm(e1, ssa, absoluteSSAIndices);
        if (mathsat.api.MSAT_ERROR_TERM(me1)) return me1;
        long me2 = buildMsatTerm(e2, ssa, absoluteSSAIndices);
        if (mathsat.api.MSAT_ERROR_TERM(me2)) return me2;

        switch (op) {
        case IASTBinaryExpression.op_plus:
          return mathsat.api.msat_make_plus(msatEnv, me1, me2);
        case IASTBinaryExpression.op_minus:
          return mathsat.api.msat_make_minus(msatEnv, me1, me2);
        case IASTBinaryExpression.op_multiply:
          if (mathsat.api.msat_term_is_number(me1) != 0 ||
              mathsat.api.msat_term_is_number(me2) != 0) {
            return mathsat.api.msat_make_times(msatEnv, me1, me2);
          } else {
            return buildMsatUF(op, me1, me2);
          }
        case IASTBinaryExpression.op_divide:
          if (mathsat.api.msat_term_is_number(me2) != 0) {
            String n = mathsat.api.msat_term_repr(me2);
            if (n.startsWith("(")) {
              n = n.substring(1, n.length()-1);
            }
            String[] frac = n.split("/");
            if (frac.length == 1) {
              n = "1/" + n;
            } else {
              assert(frac.length == 2);
              n = frac[1] + "/" + frac[0];
            }
            me2 = mathsat.api.msat_make_number(msatEnv, n);
            if (!mathsat.api.MSAT_ERROR_TERM(me2)) {
              me2 = mathsat.api.msat_make_times(
                  msatEnv, me1, me2);
            }
          } else {
            me2 = buildMsatUF(op, me1, me2);
          }
          return me2;
        }
        break;
      }

      case IASTBinaryExpression.op_modulo:
      case IASTBinaryExpression.op_binaryAnd:
      case IASTBinaryExpression.op_binaryOr:
      case IASTBinaryExpression.op_binaryXor:
      case IASTBinaryExpression.op_shiftLeft:
      case IASTBinaryExpression.op_shiftRight: 
      {
        long me1 = buildMsatTerm(e1, ssa, absoluteSSAIndices);
        if (mathsat.api.MSAT_ERROR_TERM(me1)) return me1;
        long me2 = buildMsatTerm(e2, ssa, absoluteSSAIndices);
        if (mathsat.api.MSAT_ERROR_TERM(me2)) return me2;
        return buildMsatUF(op, me1, me2);
      }

      default:
        // this might be a predicate implicitly cast to an int. Let's
        // see if this is indeed the case...
        MathsatSymbolicFormula ftmp = buildFormulaPredicate(
            exp, true, ssa, absoluteSSAIndices);
      if (ftmp == null) {
        return mathsat.api.MSAT_MAKE_ERROR_TERM();
      } else {
        System.out.println(exp.getRawSignature());
        assert(false);
        return mathsat.api.msat_make_ite(msatEnv, ftmp.getTerm(),
            mathsat.api.msat_make_number(msatEnv, "1"),
            mathsat.api.msat_make_number(msatEnv, "0"));
      }
      //return mathsat.api.MSAT_MAKE_ERROR_TERM();
      }
    } else if (exp instanceof IASTFieldReference) {
      if (lvalsAsUif) {
        IASTFieldReference fexp = (IASTFieldReference)exp;
        String field = fexp.getFieldName().getRawSignature();
        IASTExpression owner = fexp.getFieldOwner();
        long term = buildMsatTerm(owner, ssa, absoluteSSAIndices);

        if (mathsat.api.MSAT_ERROR_TERM(term)) return term;

        String tpname = getTypeName(owner.getExpressionType());
        String ufname =
          (fexp.isPointerDereference() ? "->{" : ".{") +
          tpname + "," + field + "}";
        int idx = getNormalIndex(ufname, term, ssa, absoluteSSAIndices);
        if (idx <= 0) return mathsat.api.MSAT_MAKE_ERROR_TERM();
        // see above for the case of &x and *x
        long decl = mathsat.api.msat_declare_uif(
            msatEnv, ufname + "@" + idx, msatVarType, 1,
            new int[]{msatVarType});
        return mathsat.api.msat_make_uif(
            msatEnv, decl, new long[]{term});
      } else {
        warnUnsafeVar(exp);
        return buildMsatTermVar(exprToVarName(exp), ssa,
            absoluteSSAIndices);

      }
    } else if (exp instanceof IASTArraySubscriptExpression) {
      if (lvalsAsUif) {
        IASTArraySubscriptExpression aexp =
          (IASTArraySubscriptExpression)exp;
        IASTExpression arrexp = aexp.getArrayExpression();
        IASTExpression subexp = aexp.getSubscriptExpression();
        long aterm = buildMsatTerm(arrexp, ssa, absoluteSSAIndices);
        long sterm = buildMsatTerm(subexp, ssa, absoluteSSAIndices);

        if (mathsat.api.MSAT_ERROR_TERM(aterm)) return aterm;
        if (mathsat.api.MSAT_ERROR_TERM(sterm)) return sterm;

        String ufname = OP_ARRAY_SUBSCRIPT;
        long[] args = {aterm, sterm};
        int idx = getNormalIndex(ufname, args, ssa, absoluteSSAIndices);
        if (idx <= 0) return mathsat.api.MSAT_MAKE_ERROR_TERM();

        long decl = mathsat.api.msat_declare_uif(
            msatEnv, ufname + "@" + idx, msatVarType, 2,
            new int[]{msatVarType, msatVarType});
        return mathsat.api.msat_make_uif(msatEnv, decl,
            new long[]{aterm, sterm});
      } else {
        warnUnsafeVar(exp);
        return buildMsatTermVar(exprToVarName(exp), ssa,
            absoluteSSAIndices);
      }
    } else if (exp instanceof IASTFunctionCallExpression) {
      // this is an external call. We have to create an UIF.
      IASTFunctionCallExpression fexp = (IASTFunctionCallExpression)exp;
      return buildMsatTermExternalFunctionCall(
          fexp, ssa, absoluteSSAIndices);
    } else if (exp instanceof IASTTypeIdExpression) {
      assert(((IASTTypeIdExpression)exp).getOperator() ==
        IASTTypeIdExpression.op_sizeof);
      warnUnsafeVar(exp);
      return buildMsatTermVar(exprToVarName(exp), ssa,
          absoluteSSAIndices);
    }
    // unknown expression, caller will raise exception
    return mathsat.api.MSAT_MAKE_ERROR_TERM();
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
      CPAMain.logManager.logException(Level.WARNING, e, "");
      assert(false);
    }
    return null;
  }

  private int getLvalIndex(String name, SSAMap ssa, boolean absolute) {
    int idx = -1;
    if (absolute) {
      idx = SSAMap.getNextSSAIndex();
    } else {
      int oldidx = ssa.getIndex(name);
      if (oldidx > 0) {
        idx = getNewIndex(name, ssa);
      } else {
        idx = 2; // AG - IMPORTANT!!! We must start from 2 and
        // not from 1, because this is an assignment,
        // so the SSA index must be fresh. If we use 1
        // here, we will have troubles later when
        // shifting indices
      }
    }
    return idx;
  }

  private int getLvalIndex(String name, long[] args, SSAMap ssa,
      boolean absolute) {
    SymbolicFormula[] a = new SymbolicFormula[args.length];
    for (int i = 0; i < a.length; ++i) {
      a[i] = new MathsatSymbolicFormula(args[i]);
    }
    int idx = -1;
    if (absolute) {
      idx = SSAMap.getNextSSAIndex();
    } else {
      int oldidx = ssa.getIndex(name, a);
      if (oldidx > 0) {
        idx = getNewIndex(name, a, ssa);
      } else {
        idx = 2; // AG - IMPORTANT!!! We must start from 2 and
        // not from 1, because this is an assignment,
        // so the SSA index must be fresh. If we use 1
        // here, we will have troubles later when
        // shifting indices
      }
    }
    return idx;
  }

  private int getLvalIndex(String name, long arg, SSAMap ssa,
      boolean absolute) {
    long[] args = {arg};
    return getLvalIndex(name, args, ssa, absolute);
  }

  /*
    private int getNormalIndex(String name, SSAMap ssa, boolean absolute) {
        int idx = ssa.getIndex(name);
        if (idx <= 0) {
            if (absolute) {
                return -1;
            } else {
                idx = autoInstantiateVar(name, ssa);
            }
        }
        return idx;
    }
   */

  private int getNormalIndex(String name, long[] args, SSAMap ssa,
      boolean absolute, boolean autoInstantiate) {
    SymbolicFormula[] a = new SymbolicFormula[args.length];
    for (int i = 0; i < a.length; ++i) {
      a[i] = new MathsatSymbolicFormula(args[i]);
    }
    int idx = ssa.getIndex(name, a);
    if (idx <= 0) {
      if (absolute || !autoInstantiate) {
        return -1;
      } else {
        idx = autoInstantiateLvalue(name, a, ssa);
      }
    }
    return idx;
  }

  private int getNormalIndex(String name, long[] args, SSAMap ssa,
      boolean absolute) {
    return getNormalIndex(name, args, ssa, absolute, true);
  }

  private int getNormalIndex(String name, long arg, SSAMap ssa,
      boolean absolute) {
    long[] args = {arg};
    return getNormalIndex(name, args, ssa, absolute, true);
  }

  private long buildMsatLvalueTerm(IASTExpression exp, SSAMap ssa,
      boolean absoluteSSAIndices) {
    if (exp instanceof IASTIdExpression || !lvalsAsUif) {
      String var = null;
      if (exp instanceof IASTIdExpression) {
        var = ((IASTIdExpression)exp).getName().getRawSignature();
      } else {
        var = exprToVarName(exp);
      }
      var = scoped(var);
      int idx = getLvalIndex(var, ssa, absoluteSSAIndices);
      ssa.setIndex(var, idx);
      long mvar = buildMsatVariable(var, idx);
      return mvar;
    } else if (exp instanceof IASTUnaryExpression) {
      int op = ((IASTUnaryExpression)exp).getOperator();
      IASTExpression operand = ((IASTUnaryExpression)exp).getOperand();
      String opname = null;
      switch (op) {
      case IASTUnaryExpression.op_amper:
        opname = OP_ADDRESSOF_NAME;
        break;
      case IASTUnaryExpression.op_star:
        opname = OP_STAR_NAME;
        break;
      default:
        // invalid lvalue
        return mathsat.api.MSAT_MAKE_ERROR_TERM();
      }
      long term = buildMsatTerm(operand, ssa, absoluteSSAIndices);
      if (mathsat.api.MSAT_ERROR_TERM(term)) return term;

      // PW make SSA index of * independent from argument
      int idx = getLvalIndex(opname, ssa, absoluteSSAIndices);
      //int idx = getLvalIndex(opname, term, ssa, absoluteSSAIndices);
      //ssa.setIndex(opname, new MathsatSymbolicFormula(term), idx);
      
      // build the "updated" function corresponding to this operation.
      // what we do is the following:
      // C            |     MathSAT
      // *x = 1       |     <ptr_*>::2(x) = 1
      // ...
      // &(*x) = 2    |     <ptr_&>::2(<ptr_*>::1(x)) = 2
      long decl = mathsat.api.msat_declare_uif(msatEnv,
          opname + "@" + idx, msatVarType, 1, new int[]{msatVarType});
      return mathsat.api.msat_make_uif(msatEnv, decl, new long[]{term});

    } else if (exp instanceof IASTFieldReference) {
      IASTFieldReference fexp = (IASTFieldReference)exp;
      String field = fexp.getFieldName().getRawSignature();
      IASTExpression owner = fexp.getFieldOwner();
      long term = buildMsatTerm(owner, ssa, absoluteSSAIndices);

      if (mathsat.api.MSAT_ERROR_TERM(term)) return term;

      String tpname = getTypeName(owner.getExpressionType());
      String ufname =
        (fexp.isPointerDereference() ? "->{" : ".{") +
        tpname + "," + field + "}";
      int idx = getLvalIndex(ufname, term, ssa, absoluteSSAIndices);
      SymbolicFormula[] args = {new MathsatSymbolicFormula(term)};
      ssa.setIndex(ufname, args, idx);
      // see above for the case of &x and *x
      long decl = mathsat.api.msat_declare_uif(msatEnv,
          ufname + "@" + idx, msatVarType, 1, new int[]{msatVarType});
      return mathsat.api.msat_make_uif(msatEnv, decl, new long[]{term});

    } else if (exp instanceof IASTArraySubscriptExpression) {
      IASTArraySubscriptExpression aexp =
        (IASTArraySubscriptExpression)exp;
      IASTExpression arrexp = aexp.getArrayExpression();
      IASTExpression subexp = aexp.getSubscriptExpression();
      long aterm = buildMsatTerm(arrexp, ssa, absoluteSSAIndices);
      long sterm = buildMsatTerm(subexp, ssa, absoluteSSAIndices);

      if (mathsat.api.MSAT_ERROR_TERM(aterm)) return aterm;
      if (mathsat.api.MSAT_ERROR_TERM(sterm)) return sterm;

      String ufname = OP_ARRAY_SUBSCRIPT;
      long[] args = {aterm, sterm};
      int idx = getLvalIndex(ufname, args, ssa, absoluteSSAIndices);
      SymbolicFormula[] a = {new MathsatSymbolicFormula(aterm),
          new MathsatSymbolicFormula(sterm)};
      ssa.setIndex(ufname, a, idx);

      long decl = mathsat.api.msat_declare_uif(msatEnv,
          ufname + "@" + idx, msatVarType, 2,
          new int[]{msatVarType, msatVarType});
      return mathsat.api.msat_make_uif(msatEnv, decl,
          new long[]{aterm, sterm});
    }
    // unknown lvalue
    return mathsat.api.MSAT_MAKE_ERROR_TERM();
  }

  private long buildMsatTermExternalFunctionCall(
      IASTFunctionCallExpression fexp, SSAMap ssa,
      boolean absoluteSSAIndices) {
    IASTExpression fn = fexp.getFunctionNameExpression();
    IASTExpression pexp = fexp.getParameterExpression();
    CPAMain.logManager.log(Level.ALL, "External function call " + fn.getRawSignature()
        + " encountered, assuming it has no side effects!");
    if (pexp == null) {
      // this is a function of arity 0. We create a fresh global variable
      // for it (instantiated at 1 because we need an index but it never
      // increases)
      String func = ((IASTIdExpression)fn).getName().getRawSignature();
      globalVars.add(func);
      return buildMsatVariable(func, 1);
    } else {
      IASTExpression[] args = null;
      if (pexp instanceof IASTExpressionList) {
        args = ((IASTExpressionList)pexp).getExpressions();
      } else {
        args = new IASTExpression[]{pexp};
      }
      long[] mArgs = new long[args.length];
      int[] tp = new int[args.length];
      for (int i = 0; i < args.length; ++i) {
        mArgs[i] = buildMsatTerm(args[i], ssa, absoluteSSAIndices);
        tp[i] = msatVarType;
        if (mathsat.api.MSAT_ERROR_TERM(mArgs[i])) {
          return mArgs[i];
        }
      }

      String func = null;
      if (fn instanceof IASTIdExpression) {
        func = ((IASTIdExpression)fn).getName().getRawSignature();
      } else {
        warn("External call through function pointer!: " +
            fexp.getRawSignature());
        func = "<func>{" + fn.getRawSignature() + "}";
      }
      long d = mathsat.api.msat_declare_uif(msatEnv, func, msatVarType,
          tp.length, tp);
      if (mathsat.api.MSAT_ERROR_DECL(d)) {
        return mathsat.api.MSAT_MAKE_ERROR_TERM();
      }
      return mathsat.api.msat_make_uif(msatEnv, d, mArgs);
    }
  }

  private void warnUnsafeVar(IASTExpression exp) {
    if (!warnedUnsafeVars.contains(exp)) {
      warnedUnsafeVars.add(exp);
      warn("unhandled expression: " +
          exp.getRawSignature() + " - treating as a free variable!");
    }
  }

  // create a binary uninterpreted function for the unsupported operation "op"
  private long buildMsatUF(int op, long t1, long t2) {
    long decl = 0;
    switch (op) {
    case IASTBinaryExpression.op_binaryAnd:
    case IASTBinaryExpression.op_binaryAndAssign:
      decl = bitwiseAndUfDecl;
      break;
    case IASTBinaryExpression.op_binaryOr:
    case IASTBinaryExpression.op_binaryOrAssign:
      decl = bitwiseOrUfDecl;
      break;
    case IASTBinaryExpression.op_binaryXor:
    case IASTBinaryExpression.op_binaryXorAssign:
      decl = bitwiseXorUfDecl;
      break;
    case IASTBinaryExpression.op_shiftLeft:
    case IASTBinaryExpression.op_shiftLeftAssign:
      decl = leftShiftUfDecl;
      break;
    case IASTBinaryExpression.op_shiftRight:
    case IASTBinaryExpression.op_shiftRightAssign:
      decl = rightShiftUfDecl;
      break;
    case IASTBinaryExpression.op_multiply:
    case IASTBinaryExpression.op_multiplyAssign:
      decl = multUfDecl;
      break;
    case IASTBinaryExpression.op_divide:
    case IASTBinaryExpression.op_divideAssign:
      decl = divUfDecl;
      break;
    case IASTBinaryExpression.op_modulo:
    case IASTBinaryExpression.op_moduloAssign:
      decl = modUfDecl;
      break;
    default:
      return mathsat.api.MSAT_MAKE_ERROR_TERM();
    }
    long[] args = {t1, t2};
    return mathsat.api.msat_make_uif(msatEnv, decl, args);
  }

  /*
    private long buildMsatUF(int op, long t) {
        if (op == IASTUnaryExpression.op_tilde) {
            return mathsat.api.msat_make_uif(msatEnv, bitwiseNotUfDecl,
                    new long[]{t});
        } else {
            return mathsat.api.MSAT_MAKE_ERROR_TERM();
        }
    }
   */

  // looks up the variable in the current namespace
  private String scoped(String var) {
    if (globalVars.contains(var)) {
      return var;
    } else {
      return getNamespace() + "::" + var;
    }
  }

  /*
   * checks whether the given expression is going to modify the SSAMap. If
   * not, we can avoid copying it
   */
  private boolean needsSSAUpdate(IASTExpression expr) {
    if (expr instanceof IASTUnaryExpression) {
      switch (((IASTUnaryExpression)expr).getOperator()) {
      case IASTUnaryExpression.op_postFixIncr:
      case IASTUnaryExpression.op_prefixIncr:
      case IASTUnaryExpression.op_postFixDecr:
      case IASTUnaryExpression.op_prefixDecr:
        return true;
      }
    } else if (expr instanceof IASTBinaryExpression) {
      switch (((IASTBinaryExpression)expr).getOperator()) {
      case IASTBinaryExpression.op_assign:
      case IASTBinaryExpression.op_plusAssign:
      case IASTBinaryExpression.op_minusAssign:
      case IASTBinaryExpression.op_multiplyAssign:
        return true;
      }
    }
    return false;
  }

  private boolean termIsAssignment(long term) {
    return (mathsat.api.msat_term_is_uif(term) != 0 &&
        mathsat.api.msat_term_repr(term).startsWith(":="));
  }

  private long makeAssignment(long t1, long t2) {
    CPAMain.logManager.log(Level.ALL, "DEBUG_3",
        "MAKE ASSIGNMENT: ", new MathsatSymbolicFormula(t1), " := ",
        new MathsatSymbolicFormula(t2));
    return mathsat.api.msat_make_uif(msatEnv, assignUfDecl,
        new long[]{t1, t2});
  }

  private PathFormula makeAndStatement(
      MathsatSymbolicFormula f1, StatementEdge stmt, SSAMap ssa,
      boolean absoluteSSAIndices)
      throws UnrecognizedCFAEdgeException {
    IASTExpression expr = stmt.getExpression();
    if (needsSSAUpdate(expr)) {
      SSAMap ssa2 = new SSAMap();
      ssa2.copyFrom(ssa);
      ssa = ssa2;
    }
    long f2 = buildMsatTerm(expr, ssa, absoluteSSAIndices);

    if (!mathsat.api.MSAT_ERROR_TERM(f2)) {
      long d = mathsat.api.msat_term_get_decl(f2);
      if (mathsat.api.msat_decl_get_return_type(d) !=
        mathsat.api.MSAT_BOOL) {
        // in this case, we have something like:
          // f(x);
        // i.e. an expression that gets assigned to nothing. Since
        // we don't handle side-effects, this means that the
        // expression has no effect, and we can just drop it
        warn("statment " + stmt.getRawStatement() +
        " has no effect, dropping it!");
        return new PathFormula(f1, ssa);
      }
      long a = mathsat.api.msat_make_and(msatEnv, f1.getTerm(), f2);
      return new PathFormula(
          new MathsatSymbolicFormula(a), ssa);
    } else {
      throw new UnrecognizedCFAEdgeException("STATEMENT: " +
          stmt.getRawStatement());
    }
  }

  protected MathsatSymbolicFormula buildFormulaPredicate(
      IASTExpression exp, boolean isTrue, SSAMap ssa,
      boolean absoluteSSAIndices) {
    if (exp instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression)exp);
      int opType = binExp.getOperator();
      IASTExpression op1 = binExp.getOperand1();
      IASTExpression op2 = binExp.getOperand2();

      long t1 = buildMsatTerm(op1, ssa, absoluteSSAIndices);
      long t2 = buildMsatTerm(op2, ssa, absoluteSSAIndices);

      if (mathsat.api.MSAT_ERROR_TERM(t1) ||
          mathsat.api.MSAT_ERROR_TERM(t2)) {
        return null;
      }
      long result = 0;

      switch (opType) {
      case IASTBinaryExpression.op_greaterThan:
        result = mathsat.api.msat_make_gt(msatEnv, t1, t2);
        break;

      case IASTBinaryExpression.op_greaterEqual:
        result = mathsat.api.msat_make_geq(msatEnv, t1, t2);
        break;

      case IASTBinaryExpression.op_lessThan:
        result = mathsat.api.msat_make_lt(msatEnv, t1, t2);
        break;

      case IASTBinaryExpression.op_lessEqual:
        result = mathsat.api.msat_make_leq(msatEnv, t1, t2);
        break;

      case IASTBinaryExpression.op_equals:
        result = mathsat.api.msat_make_equal(msatEnv, t1, t2);
        break;

      case IASTBinaryExpression.op_notequals:
        result = mathsat.api.msat_make_not(msatEnv,
            mathsat.api.msat_make_equal(msatEnv, t1, t2));
        break;

      default: {
        // check whether this is an implict cast to bool
        long t = buildMsatTerm(exp, ssa, absoluteSSAIndices);
        if (mathsat.api.MSAT_ERROR_TERM(t)) {
          return null;
        } else if (mathsat.api.msat_term_get_type(t) !=
          mathsat.api.MSAT_BOOL) {
          long z = mathsat.api.msat_make_number(msatEnv, "0");
          result = mathsat.api.msat_make_not(msatEnv,
              mathsat.api.msat_make_equal(msatEnv, t, z));
        } else {
          result = t;
        }
        break;
      }
      }
      if (!isTrue) {
        result = mathsat.api.msat_make_not(msatEnv, result);
      }
      // now create the formula
      return new MathsatSymbolicFormula(result);
    } else if (exp instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExp = ((IASTUnaryExpression)exp);
      // ! exp
      if (unaryExp.getOperator() == IASTUnaryExpression.op_not) {
        IASTExpression exp1 = unaryExp.getOperand();
        MathsatSymbolicFormula r = buildFormulaPredicate(exp1, !isTrue,
            ssa, absoluteSSAIndices);
        return r;
      } else if (unaryExp.getOperator() ==
        IASTUnaryExpression.op_bracketedPrimary) {
        return buildFormulaPredicate(
            unaryExp.getOperand(), isTrue, ssa, absoluteSSAIndices);
      } else {
        // build the mathsat term. If this is not a predicate, make
        // it a predicate by adding a "!= 0"
        long t = buildMsatTerm(exp, ssa, absoluteSSAIndices);
        if (mathsat.api.MSAT_ERROR_TERM(t)) {
          return null;
        }
        if (mathsat.api.msat_term_get_type(t) != mathsat.api.MSAT_BOOL){
          long z = mathsat.api.msat_make_number(msatEnv, "0");
          if (isTrue) {
            t = mathsat.api.msat_make_not(msatEnv,
                mathsat.api.msat_make_equal(msatEnv, t, z));
          } else {
            t = mathsat.api.msat_make_equal(msatEnv, t, z);
          }
          if (mathsat.api.MSAT_ERROR_TERM(t)) {
            return null;
          }
        }
        return new MathsatSymbolicFormula(t);
      }
    } else {
      // build the mathsat term. If this is not a predicate, make
      // it a predicate by adding a "!= 0"
      long t = buildMsatTerm(exp, ssa, absoluteSSAIndices);
      if (mathsat.api.MSAT_ERROR_TERM(t)) {
        return null;
      }
      if (mathsat.api.msat_term_get_type(t) != mathsat.api.MSAT_BOOL){
        long z = mathsat.api.msat_make_number(msatEnv, "0");
        if (isTrue) {
          t = mathsat.api.msat_make_not(msatEnv,
              mathsat.api.msat_make_equal(msatEnv, t, z));
        } else {
          t = mathsat.api.msat_make_equal(msatEnv, t, z);
        }
        if (mathsat.api.MSAT_ERROR_TERM(t)) {
          return null;
        }
      }
      return new MathsatSymbolicFormula(t);
    }
  }

  private PathFormula makeAndAssume(
      MathsatSymbolicFormula f1, AssumeEdge assume, SSAMap ssa,
      boolean absoluteSSAIndices) throws UnrecognizedCFAEdgeException {
    MathsatSymbolicFormula f2 = buildFormulaPredicate(
        assume.getExpression(), assume.getTruthAssumption(), ssa,
        absoluteSSAIndices);
    if (f2 == null) {
      throw new UnrecognizedCFAEdgeException("ASSUME: " +
          assume.getRawStatement());
    } else {
      long res = mathsat.api.msat_make_and(msatEnv, f1.getTerm(),
          f2.getTerm());
      return new PathFormula(
          new MathsatSymbolicFormula(res), ssa);
    }
  }

  // creates the two mathsat terms
  // (var@newidx = var@i1) and (var@newidx = var@i2)
  // used by mergeSSAMaps
  // This function used to generate a new index "newidx" at every merge.
  // However, this is not necessary, and it was very inefficient, because it
  // created a lot of extra variables. Now, we have the invariant that
  // newidx is either i1 or i2 (the highest of the two), so merging SSA maps
  // does not generate new variables
  private Pair<Long, Long> makeSSAMerger(String var, int i1, int i2,
      int newidx) {
    // retrieve the mathsat terms corresponding to the two variables
    long v1 = buildMsatVariable(var, i1);
    long v2 = buildMsatVariable(var, i2);
    long e1 = mathsat.api.msat_make_true(msatEnv);
    long e2 = mathsat.api.msat_make_true(msatEnv);
    if (i1 < i2) {
      assert(newidx == i2);
      for (int i = i1+1; i <= i2; ++i) {
        long v = buildMsatVariable(var, i);
        long e = mathsat.api.msat_make_equal(msatEnv, v, v1);
        e1 = mathsat.api.msat_make_and(msatEnv, e1, e);
      }
    } else {
      assert(i2 < i1);
      assert(newidx == i1);
      for (int i = i2+1; i <= i1; ++i) {
        long v = buildMsatVariable(var, i);
        long e = mathsat.api.msat_make_equal(msatEnv, v, v2);
        e2 = mathsat.api.msat_make_and(msatEnv, e2, e);
      }
    }
    return new Pair<Long, Long>(e1, e2);
  }

  private Pair<Long, Long> makeSSAMerger(String name,
      SymbolicFormula[] args, int i1, int i2, int newidx) {
    // retrieve the mathsat terms corresponding to the two variables
    long v1 = buildMsatUFLvalue(name, args, i1);
    long v2 = buildMsatUFLvalue(name, args, i2);
    long e1 = mathsat.api.msat_make_true(msatEnv);
    long e2 = mathsat.api.msat_make_true(msatEnv);
    if (i1 < i2) {
      assert(newidx == i2);
      for (int i = i1+1; i <= i2; ++i) {
        long v = buildMsatUFLvalue(name, args, i);
        long e = mathsat.api.msat_make_equal(msatEnv, v, v1);
        e1 = mathsat.api.msat_make_and(msatEnv, e1, e);
      }
    } else {
      assert(i2 < i1);
      assert(newidx == i1);
      for (int i = i2+1; i <= i1; ++i) {
        long v = buildMsatUFLvalue(name, args, i);
        long e = mathsat.api.msat_make_equal(msatEnv, v, v2);
        e2 = mathsat.api.msat_make_and(msatEnv, e2, e);
      }
    }
    return new Pair<Long, Long>(e1, e2);
  }

  private int getNewIndex(String var, SSAMap ssa) {
    int idx = ssa.getIndex(var);
    if (idx > 0) return idx+1;
    else return 1;
  }

  private int getNewIndex(String f, SymbolicFormula[] args, SSAMap ssa) {
    int idx = ssa.getIndex(f, args);
    if (idx > 0) return idx+1;
    else return 1;
  }

  public Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mergeSSAMaps(
      SSAMap ssa1, SSAMap ssa2, boolean absoluteSSAIndices) {
    SSAMap result = new SSAMap();
    long mt1 = mathsat.api.msat_make_true(msatEnv);
    long mt2 = mathsat.api.msat_make_true(msatEnv);
    for (String var : ssa1.allVariables()) {
      int i1 = ssa1.getIndex(var);
      int i2 = ssa2.getIndex(var);
      assert(i1 > 0);
      if (i2 > 0 && i2 != i1) {
        // we have to merge this variable assignment
        int i3 = Math.max(i1, i2);
        result.setIndex(var, i3);
        Pair<Long, Long> t = makeSSAMerger(var, i1, i2, i3);
        mt1 = mathsat.api.msat_make_and(msatEnv, mt1,
            t.getFirst());
        mt2 = mathsat.api.msat_make_and(msatEnv, mt2,
            t.getSecond());
      } else {
        if (i2 <= 0) {
          // it's not enough to set the SSA index. We *must* also
          // generate a formula saying that the var does not change
          // in this branch!
          long v1 = buildMsatVariable(var, 1);
          for (int i = 2; i <= i1; ++i) {
            long v = buildMsatVariable(var, i);
            long e = mathsat.api.msat_make_equal(msatEnv, v, v1);
            mt2 = mathsat.api.msat_make_and(msatEnv, mt2, e);
          }
        }
        result.setIndex(var, i1);
      }
    }
    for (String var : ssa2.allVariables()) {
      int i2 = ssa2.getIndex(var);
      int i1 = ssa1.getIndex(var);
      assert(i2 > 0);
      if (i1 <= 0) {
        // it's not enough to set the SSA index. We *must* also
        // generate a formula saying that the var does not change
        // in this branch!
        long v1 = buildMsatVariable(var, 1);
        for (int i = 2; i <= i2; ++i) {
          long v = buildMsatVariable(var, i);
          long e = mathsat.api.msat_make_equal(msatEnv, v, v1);
          mt1 = mathsat.api.msat_make_and(msatEnv, mt1, e);
        }
        result.setIndex(var, i2);
      } else {
        //assert(i1 == i2 || result.getIndex(var) > Math.max(i1, i2));
        assert(i1 == i2 || result.getIndex(var) == Math.max(i1, i2));
      }
    }

    for (Pair<String, SymbolicFormula[]> f : ssa1.allFunctions()) {
      int i1 = ssa1.getIndex(f.getFirst(), f.getSecond());
      int i2 = ssa2.getIndex(f.getFirst(), f.getSecond());
      assert(i1 > 0);
      if (i2 > 0 && i2 != i1) {
        // we have to merge this lvalue assignment
        int i3 = Math.max(i1, i2);
        result.setIndex(f.getFirst(), f.getSecond(), i3);
        Pair<Long, Long> t = makeSSAMerger(
            f.getFirst(), f.getSecond(), i1, i2, i3);
        mt1 = mathsat.api.msat_make_and(msatEnv, mt1,
            t.getFirst());
        mt2 = mathsat.api.msat_make_and(msatEnv, mt2,
            t.getSecond());
      } else {
        if (i2 <= 0) {
          // it's not enough to set the SSA index. We *must* also
          // generate a formula saying that the var does not change
          // in this branch!
          long[] args = new long[f.getSecond().length];
          for (int i = 0; i < args.length; ++i) {
            args[i] = ((MathsatSymbolicFormula)
                f.getSecond()[i]).getTerm();
          }
          long v1 = buildMsatUFLvalue(f.getFirst(), args, 1);
          for (int i = 2; i <= i1; ++i) {
            long v = buildMsatUFLvalue(f.getFirst(), args, i);
            long e = mathsat.api.msat_make_equal(msatEnv, v, v1);
            mt2 = mathsat.api.msat_make_and(msatEnv, mt2, e);
          }
        }
        result.setIndex(f.getFirst(), f.getSecond(), i1);
      }
    }
    for (Pair<String, SymbolicFormula[]> f : ssa2.allFunctions()) {
      int i2 = ssa2.getIndex(f.getFirst(), f.getSecond());
      int i1 = ssa1.getIndex(f.getFirst(), f.getSecond());
      assert(i2 > 0);
      if (i1 <= 0) {
        // it's not enough to set the SSA index. We *must* also
        // generate a formula saying that the var does not change
        // in this branch!
        long[] args = new long[f.getSecond().length];
        for (int i = 0; i < args.length; ++i) {
          args[i] = ((MathsatSymbolicFormula)
              f.getSecond()[i]).getTerm();
        }
        long v1 = buildMsatUFLvalue(f.getFirst(), args, 1);
        for (int i = 2; i <= i2; ++i) {
          long v = buildMsatUFLvalue(f.getFirst(), args, i);
          long e = mathsat.api.msat_make_equal(msatEnv, v, v1);
          mt1 = mathsat.api.msat_make_and(msatEnv, mt1, e);
        }
        result.setIndex(f.getFirst(), f.getSecond(), i2);
      } else {
        assert(i1 == i2 ||
            result.getIndex(f.getFirst(), f.getSecond()) ==
              Math.max(i1, i2));
      }
    }

    Pair<SymbolicFormula, SymbolicFormula> sp =
      new Pair<SymbolicFormula, SymbolicFormula>(
          new MathsatSymbolicFormula(mt1),
          new MathsatSymbolicFormula(mt2));
    return new Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap>(
        sp, result);
  }

  // ssa can be null. In this case, all the variables are instantiated
  // at index 1
  @Override
  public SymbolicFormula instantiate(SymbolicFormula f, SSAMap ssa) {
    Stack<Long> toProcess = new Stack<Long>();
    Map<Long, Long> cache = new HashMap<Long, Long>();

    long term = ((MathsatSymbolicFormula)f).getTerm();
    toProcess.push(term);
    while (!toProcess.empty()) {
      long t = toProcess.peek();
      if (cache.containsKey(t)) {
        toProcess.pop();
        continue;
      }
      if (mathsat.api.msat_term_is_variable(t) != 0) {
        toProcess.pop();
        String name = mathsat.api.msat_term_repr(t);
        int idx = (ssa != null ? ssa.getIndex(name) : 1);
        if (idx > 0) {
          // ok, the variable has an instance in the SSA, replace it
          long newt = buildMsatVariable(name, idx);
          assert(!mathsat.api.MSAT_ERROR_TERM(newt));
          cache.put(t, newt);
        } else {
          // the variable is not used in the SSA, keep it as is
          cache.put(t, t);
        }
      } else {
        boolean childrenDone = true;
        long[] newargs = new long[mathsat.api.msat_term_arity(t)];
        for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
          long c = mathsat.api.msat_term_get_arg(t, i);
          if (!cache.containsKey(c)) {
            toProcess.push(c);
            childrenDone = false;
          } else {
            newargs[i] = cache.get(c);
          }
        }
        if (childrenDone) {
          toProcess.pop();
          long newt = mathsat.api.MSAT_MAKE_ERROR_TERM();
          if (termIsAssignment(t)) {
            // now we replace our "fake" assignment with an equality
            assert(newargs.length == 2);
            newt = mathsat.api.msat_make_equal(
                msatEnv, newargs[0], newargs[1]);
          } else {
            String name = null;
            if (mathsat.api.msat_term_is_uif(t) != 0) {
              long d = mathsat.api.msat_term_get_decl(t);
              name = mathsat.api.msat_decl_get_name(d);
            }
            if (name != null && ufCanBeLvalue(name)) {
              int idx = (ssa != null ?
                  getNormalIndex(name, newargs, ssa,
                      false, false) : 1);
              if (idx > 0) {
                int[] tp = new int[newargs.length];
                for (int i = 0; i < tp.length; ++i) {
                  tp[i] = msatVarType;
                }
                long d = mathsat.api.msat_declare_uif(msatEnv,
                    name + "@" + idx,
                    msatVarType, tp.length, tp);
                // ok, the variable has an instance in the SSA,
                // replace it
                newt = mathsat.api.msat_make_uif(
                    msatEnv, d, newargs);
                assert(!mathsat.api.MSAT_ERROR_TERM(newt));
                cache.put(t, newt);
              } else {
                newt = mathsat.api.msat_replace_args(
                    msatEnv, t, newargs);
              }
            } else {
              newt = mathsat.api.msat_replace_args(
                  msatEnv, t, newargs);
            }
          }
          assert(!mathsat.api.MSAT_ERROR_TERM(newt));
          cache.put(t, newt);
        }
      }
    }

    assert(cache.containsKey(term));
    return new MathsatSymbolicFormula(cache.get(term));
  }

  public boolean ufCanBeLvalue(String name) {
    return lvalueFunctions.contains(name) || name.startsWith(".{") ||
    name.startsWith("->{");
  }


  public SymbolicFormula makeFalse() {
    return new MathsatSymbolicFormula(mathsat.api.msat_make_false(msatEnv));
  }


  public PathFormula shift(SymbolicFormula f, SSAMap ssa) {
    Stack<Long> toProcess = new Stack<Long>();
    Map<Long, Long> cache = new HashMap<Long, Long>();

    SSAMap newssa = new SSAMap();

    long term = ((MathsatSymbolicFormula)f).getTerm();
    toProcess.push(term);
    while (!toProcess.empty()) {
      long t = toProcess.peek();
      if (cache.containsKey(t)) {
        toProcess.pop();
        continue;
      }
//    if (termIsAssignment(t)) {
//// treat assignments specially. When we shift, we always have to
//    // update the SSA index of the variable being assigned
//    long var = mathsat.api.msat_term_get_arg(t, 0);
//    if (!cache.containsKey(var)) {
//    String name = mathsat.api.msat_term_repr(var);

//    CPAMain.logManager.log(Level.ALL, "DEBUG_3", "SHIFTING ASSIGNMENT:",
//    new MathsatSymbolicFormula(t), " VAR: ", name);

//    // check whether this is an instantiated variable
//    String[] bits = name.split("@");
//    int idx = -1;
//    assert(bits.length == 2);
//    try {
//    idx = Integer.parseInt(bits[1]);
//    name = bits[0];
//    } catch (NumberFormatException e) {
//    assert(false);
//    }
//    int ssaidx = ssa.getIndex(name);
//    if (ssaidx > 0) {
//    if (idx == 1) {
//    System.out.println("ERROR!!!, Shifting: " +
//    mathsat.api.msat_term_repr(t) + ", var: " +
//    name + ", TERM: " +
//    mathsat.api.msat_term_repr(term));
//    System.out.flush();
//    }
//    assert(idx > 1); //TODO!!!
//    long newvar = buildMsatVariable(name, ssaidx + idx-1);
//    assert(!mathsat.api.MSAT_ERROR_TERM(newvar));
//    cache.put(var, newvar);
//    if (newssa.getIndex(name) < ssaidx + idx-1) {
//    newssa.setIndex(name, ssaidx + idx-1);
//    }
//    } else {
//    cache.put(var, var);
//    if (newssa.getIndex(name) < idx) {
//    newssa.setIndex(name, idx);
//    }
//    }

//    CPAMain.logManager.log(Level.ALL, "DEBUG_3", "SHIFTING ASSIGNMENT,",
//    "RESULT: ", //name, "@", newssa.getIndex(name));
//    mathsat.api.msat_term_repr(cache.get(var)));
//    }
//    }

      if (mathsat.api.msat_term_is_variable(t) != 0) {
        toProcess.pop();
        String name = mathsat.api.msat_term_repr(t);
        // check whether this is an instantiated variable
        String[] bits = name.split("@");
        int idx = -1;
        if (bits.length == 2) {
          try {
            idx = Integer.parseInt(bits[1]);
            name = bits[0];
          } catch (NumberFormatException e) {
            CPAMain.logManager.log(Level.ALL, "DEBUG_1",
                "Bad variable name!: ", name, ", exception: ",
                e);
            assert(false); // should not happen
          }
        }
        if (idx > 0) {
          // ok, the variable is instantiated in the formula
          // retrieve the index in the SSA, and shift
          int ssaidx = ssa.getIndex(name);
          //assert(ssaidx > 0);
          if (ssaidx > 0) {
            long newt = buildMsatVariable(name, ssaidx + idx-1);
            assert(!mathsat.api.MSAT_ERROR_TERM(newt));
            cache.put(t, newt);
            if (newssa.getIndex(name) < ssaidx + idx-1) {
              newssa.setIndex(name, ssaidx + idx-1);
            }
          } else {
            cache.put(t, t);
            if (newssa.getIndex(name) < idx) {
              newssa.setIndex(name, idx);
            }
          }
        } else {
          // the variable is not instantiated, keep it as is
          cache.put(t, t);
        }
      } else {
        boolean childrenDone = true;
        long[] newargs = new long[mathsat.api.msat_term_arity(t)];
        for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
          long c = mathsat.api.msat_term_get_arg(t, i);
          if (!cache.containsKey(c)) {
            toProcess.push(c);
            childrenDone = false;
          } else {
            newargs[i] = cache.get(c);
          }
        }
        if (childrenDone) {
          toProcess.pop();
          long newt = mathsat.api.MSAT_MAKE_ERROR_TERM();
          if (termIsAssignment(t)) {
            newt = mathsat.api.msat_make_equal(
                msatEnv, newargs[0], newargs[1]);
          } else {
            String name = null;
            if (mathsat.api.msat_term_is_uif(t) != 0) {
              long d = mathsat.api.msat_term_get_decl(t);
              name = mathsat.api.msat_decl_get_name(d);
            }
            if (name != null && ufCanBeLvalue(name)) {
              // we have to shift this uif as well
              String[] bits = name.split("@");
              int idx = -1;
              if (bits.length == 2) {
                try {
                  idx = Integer.parseInt(bits[1]);
                  name = bits[0];
                } catch (NumberFormatException e) {
                  CPAMain.logManager.log(Level.ALL, "DEBUG_1",
                      "Bad UF name!: ", name,
                      ", exception: ", e);
                  assert(false); // should not happen
                }
              }
              if (idx > 0) {
                // ok, the UF is instantiated in the formula
                // retrieve the index in the SSA, and shift
                SymbolicFormula[] a =
                  new SymbolicFormula[newargs.length];
                for (int i = 0; i < a.length; ++i) {
                  a[i] = new MathsatSymbolicFormula(
                      newargs[i]);
                }

                int ssaidx = ssa.getIndex(name, a);
                if (ssaidx > 0) {
                  int[] tp = new int[newargs.length];
                  for (int i = 0; i < tp.length; ++i) {
                    tp[i] = msatVarType;
                  }
                  int newidx = ssaidx + idx-1;
                  long d = mathsat.api.msat_declare_uif(
                      msatEnv,
                      name + "@" + newidx,
                      msatVarType, tp.length, tp);
                  newt = mathsat.api.msat_make_uif(
                      msatEnv, d, newargs);
                  assert(!mathsat.api.MSAT_ERROR_TERM(newt));
                  cache.put(t, newt);
                  if (newssa.getIndex(name, a) < newidx) {
                    newssa.setIndex(name, a, newidx);
                  }
                } else {
                  newt = mathsat.api.msat_replace_args(
                      msatEnv, t, newargs);
                  if (newssa.getIndex(name, a) < idx) {
                    newssa.setIndex(name, a, idx);
                  }
                }
              } else {
                // the UF is not instantiated, keep it as is
                newt = mathsat.api.msat_replace_args(
                    msatEnv, t, newargs);
              }
            } else { // "normal" non-variable term
              newt = mathsat.api.msat_replace_args(
                  msatEnv, t, newargs);
            }
          }
          assert(!mathsat.api.MSAT_ERROR_TERM(newt));

          cache.put(t, newt);
        }
      }
    }

    assert(cache.containsKey(term));
    return new PathFormula(
        new MathsatSymbolicFormula(cache.get(term)), newssa);
  }

  // returns true if the given formula contains some uninterpreted
  // functions. This is used to apply some optimizations to MathSAT when
  // free functions are not used
  public boolean hasUninterpretedFunctions(MathsatSymbolicFormula f) {
    Stack<Long> toProcess = new Stack<Long>();
    Set<Long> cache = new HashSet<Long>();

    long term = f.getTerm();
    toProcess.push(term);
    while (!toProcess.empty()) {
      long t = toProcess.peek();
      if (cache.contains(t)) {
        toProcess.pop();
        continue;
      }
      cache.add(t);
      if (mathsat.api.msat_term_is_uif(t) != 0) {
        CPAMain.logManager.log(Level.ALL, "DEBUG_3", "FOUND UIF IN FORMULA:", f,
            ", term is:", new MathsatSymbolicFormula(t));
        return true;
      }
      for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
        long c = mathsat.api.msat_term_get_arg(t, i);
        if (!cache.contains(c)) {
          toProcess.push(c);
        }
      }
    }

    return false;
  }

  private boolean isArithmetic(long t) {
    return (mathsat.api.msat_term_is_plus(t) != 0 ||
        mathsat.api.msat_term_is_minus(t) != 0 ||
        mathsat.api.msat_term_is_negate(t) != 0 ||
        mathsat.api.msat_term_is_times(t) != 0 ||
        mathsat.api.msat_term_is_lt(t) != 0 ||
        mathsat.api.msat_term_is_gt(t) != 0 ||
        mathsat.api.msat_term_is_leq(t) != 0 ||
        mathsat.api.msat_term_is_geq(t) != 0);
  }

  public int getNeededTheories(MathsatSymbolicFormula f) {
    long term = f.getTerm();
    if (neededTheories.containsKey(term)) {
      return neededTheories.get(term);
    }
    Stack<Long> toProcess = new Stack<Long>();
    toProcess.push(term);
    while (!toProcess.empty()) {
      long t = toProcess.peek();
      if (neededTheories.containsKey(t)) {
        toProcess.pop();
        continue;
      }
      int needed = 0;
      boolean childrenDone = true;
      for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
        long c = mathsat.api.msat_term_get_arg(t, i);
        if (neededTheories.containsKey(c)) {
          needed |= neededTheories.get(c);
        } else {
          childrenDone = false;
          toProcess.push(c);
        }
      }
      if (childrenDone) {
        toProcess.pop();
        if (mathsat.api.msat_term_is_equal(t) != 0) {
          needed |= THEORY_EQ;
        } else if (isArithmetic(t)) {
          needed |= THEORY_ARITH;
        } else if (mathsat.api.msat_term_is_uif(t) != 0) {
          needed |= THEORY_UF;
        }
        neededTheories.put(t, needed);
      }
    }
    return neededTheories.get(term);
  }

  // returns a formula with some "static learning" about some bitwise
  // operations, so that they are (a bit) "less uninterpreted"
  public MathsatSymbolicFormula getBitwiseAxioms(MathsatSymbolicFormula f) {
    Stack<Long> toProcess = new Stack<Long>();
    Set<Long> cache = new HashSet<Long>();
    Map<Long, Set<Long>> gs = new HashMap<Long, Set<Long>>();

    boolean andFound = false;

    toProcess.push(f.getTerm());
    while (!toProcess.empty()) {
      long t = toProcess.peek();
      if (cache.contains(t)) {
        toProcess.pop();
        continue;
      }
      cache.add(t);
      if (mathsat.api.msat_term_is_number(t) != 0) {
        Set<Long> s = null;
        if (!gs.containsKey(bitwiseAndUfDecl)) {
          s = new HashSet<Long>();
          gs.put(bitwiseAndUfDecl, s);
        }
        s = gs.get(bitwiseAndUfDecl);
        s.add(t);
      }
      if (mathsat.api.msat_term_is_uif(t) != 0) {
        String r = mathsat.api.msat_term_repr(t);
        if (r.startsWith("_&_")) {
          andFound = true;
//        // ok, found bitwise and, collect numbers
//Set<Long> s = null;
//if (!gs.containsKey(bitwiseAndUfDecl)) {
//        s = new HashSet<Long>();
//        gs.put(bitwiseAndUfDecl, s);
//        }
//        s = gs.get(bitwiseAndUfDecl);
//        for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
//        long c = mathsat.api.msat_term_get_arg(t, i);
//        if (mathsat.api.msat_term_is_number(c) != 0) {
//        s.add(c);
//        }
//        }
        }
      }
      for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
        long c = mathsat.api.msat_term_get_arg(t, i);
        if (!cache.contains(c)) {
          toProcess.push(c);
        }
      }
    }
    long t = mathsat.api.msat_make_true(msatEnv);
    if (andFound) {
      long z = mathsat.api.msat_make_number(msatEnv, "0");
      for (long d : gs.keySet()) {
        Set<Long> s = gs.get(d);
        for (long n : s) {
          long u1 = mathsat.api.msat_make_uif(msatEnv, d,
              new long[]{n, z});
          long u2 = mathsat.api.msat_make_uif(msatEnv, d,
              new long[]{z, n});
          long e1 = mathsat.api.msat_make_equal(msatEnv, u1, z);
          long e2 = mathsat.api.msat_make_equal(msatEnv, u2, z);
          long a = mathsat.api.msat_make_and(msatEnv, e1, e2);
          t = mathsat.api.msat_make_and(msatEnv, t, a);
        }
      }
    }
    return new MathsatSymbolicFormula(t);
  }

  private long uninstantiate(long term, Map<Long, Long> cache) {
    Stack<Long> toProcess = new Stack<Long>();
    toProcess.push(term);
    while (!toProcess.empty()) {
      long t = toProcess.peek();
      if (cache.containsKey(t)) {
        toProcess.pop();
        continue;
      }
      if (mathsat.api.msat_term_is_variable(t) != 0) {
        String name = mathsat.api.msat_term_repr(t);
        String[] bits = name.split("@");
        assert(bits.length == 2) : "Not exactly one '@' in term '" + name + "' when uninstantiating.";
        name = bits[0];
        long d = mathsat.api.msat_declare_variable(msatEnv, name,
            msatVarType);
        long newt = mathsat.api.msat_make_variable(msatEnv, d);
        cache.put(t, newt);
      } else {
        long[] children = new long[mathsat.api.msat_term_arity(t)];
        boolean childrenDone = true;
        for (int i = 0; i < children.length; ++i) {
          long c = mathsat.api.msat_term_get_arg(t, i);
          if (cache.containsKey(c)) {
            children[i] = cache.get(c);
          } else {
            childrenDone = false;
            toProcess.push(c);
          }
        }
        if (childrenDone) {
          toProcess.pop();
          String name = null;
          if (mathsat.api.msat_term_is_uif(t) != 0) {
            long d = mathsat.api.msat_term_get_decl(t);
            name = mathsat.api.msat_decl_get_name(d);
          }
          if (name != null && ufCanBeLvalue(name)) {
            String[] bits = name.split("@");
            assert(bits.length == 2);
            name = bits[0];
            int[] tp = new int[children.length];
            for (int i = 0; i < tp.length; ++i) {
              tp[i] = msatVarType;
            }
            long d = mathsat.api.msat_declare_uif(msatEnv, name,
                msatVarType, tp.length, tp);
            long newt = mathsat.api.msat_make_uif(
                msatEnv, d, children);
            cache.put(t, newt);
          } else {
            cache.put(t, mathsat.api.msat_replace_args(
                msatEnv, t, children));
          }
        }
      }
    }
    return cache.get(term);
  }

  /**
   * Given an "instantiated" formula, returns the corresponding formula in
   * which all the variables are "generic" ones. This is the inverse of the
   * instantiate() method above
   */
  public MathsatSymbolicFormula uninstantiate(MathsatSymbolicFormula f) {
//  Map<Long, Long> cache = new HashMap<Long, Long>();
//  return new MathsatSymbolicFormula(uninstantiate(f.getTerm(), cache));
    return uninstantiate(f, true);
  }

  public MathsatSymbolicFormula uninstantiate(MathsatSymbolicFormula f,
      boolean useGlobalCache) {
    Map<Long, Long> cache;
    if (useGlobalCache) {
      cache = uninstantiateGlobalCache;
    } else {
      cache = new HashMap<Long, Long>();
    }
    return new MathsatSymbolicFormula(uninstantiate(f.getTerm(), cache));
  }


  @Override
  public Collection<SymbolicFormula> extractAtoms(SymbolicFormula f,
      boolean uninst, boolean splitArithEqualities,
      boolean conjunctionsOnly) {
    Set<Long> cache = new HashSet<Long>();
    //Set<Long> atoms = new HashSet<Long>();
    Set<Long> atoms = new TreeSet<Long>(
        new Comparator<Long>() {
          public int compare(Long o1, Long o2) {
            return mathsat.api.msat_term_id(o1) -
            mathsat.api.msat_term_id(o2);
          }
        });
    Map<Long, Long> varcache = uninstantiateGlobalCache;

    Stack<Long> toProcess = new Stack<Long>();
    toProcess.push(((MathsatSymbolicFormula)f).getTerm());

    while (!toProcess.empty()) {
      long term = toProcess.pop();
      assert(!cache.contains(term));
      cache.add(term);

      if (mathsat.api.msat_term_is_true(term) != 0 ||
          mathsat.api.msat_term_is_false(term) != 0) {
        continue;
      }

      if (mathsat.api.msat_term_is_atom(term) != 0) {
        if (uninst) {
          term = uninstantiate(term, varcache);
        }
        if (splitArithEqualities &&
            mathsat.api.msat_term_is_equal(term) != 0 &&
            isPurelyArithmetic(term, arithCache)) {
          long a1 = mathsat.api.msat_term_get_arg(term, 0);
          long a2 = mathsat.api.msat_term_get_arg(term, 1);
          long t1 = mathsat.api.msat_make_leq(msatEnv, a1, a2);
          //long t2 = mathsat.api.msat_make_leq(msatEnv, a2, a1);
          cache.add(t1);
          //cache.add(t2);
          atoms.add(t1);
          //atoms.add(t2);
          atoms.add(term);
        } else {
          atoms.add(term);
        }
      } else if (conjunctionsOnly) {
        if (mathsat.api.msat_term_is_not(term) != 0 ||
            mathsat.api.msat_term_is_and(term) != 0) {
          // ok, go into this formula
          for (int i = 0; i < mathsat.api.msat_term_arity(term); ++i){
            long c = mathsat.api.msat_term_get_arg(term, i);
            if (!cache.contains(c)) {
              toProcess.push(c);
            }
          }
        } else {
          // otherwise, treat this as atomic
          if (uninst) {
            term = uninstantiate(term, varcache);
          }
          atoms.add(term);
        }
      } else {
        for (int i = 0; i < mathsat.api.msat_term_arity(term); ++i){
          long c = mathsat.api.msat_term_get_arg(term, i);
          if (!cache.contains(c)) {
            toProcess.push(c);
          }
        }
      }
    }

    Vector<SymbolicFormula> ret = new Vector<SymbolicFormula>();
    for (long term : atoms) {
      ret.add(new MathsatSymbolicFormula(term));
    }
    return ret;
  }

  // returns true if the given term is a pure arithmetic term
  private boolean isPurelyArithmetic(long term,
      Map<Long, Boolean> arithCache) {
    if (arithCache.containsKey(term)) {
      return arithCache.get(term);
    } else if (mathsat.api.msat_term_is_uif(term) != 0) {
      arithCache.put(term, false);
      return false;
    } else {
      int a = mathsat.api.msat_term_arity(term);
      boolean yes = true;
      for (int i = 0; i < a; ++i) {
        yes |= isPurelyArithmetic(
            mathsat.api.msat_term_get_arg(term, i), arithCache);
      }
      arithCache.put(term, yes);
      return yes;
    }
  }

  public MathsatSymbolicFormula replaceAssignments(MathsatSymbolicFormula f) {
    Stack<Long> toProcess = new Stack<Long>();
    Map<Long, Long> cache = replaceAssignmentsCache;//new HashMap<Long, Long>();

    toProcess.push(f.getTerm());
    while (!toProcess.empty()) {
      long t = toProcess.peek();
      if (cache.containsKey(t)) {
        toProcess.pop();
        continue;
      }
      if (mathsat.api.msat_term_arity(t) == 0) {
        cache.put(t, t);
      } else {
        long[] newargs = new long[mathsat.api.msat_term_arity(t)];
        boolean childrenDone = true;
        for (int i = 0; i < newargs.length; ++i) {
          long c = mathsat.api.msat_term_get_arg(t, i);
          if (!cache.containsKey(c)) {
            childrenDone = false;
            toProcess.push(c);
          } else {
            newargs[i] = cache.get(c);
          }
        }
        if (childrenDone) {
          toProcess.pop();
          long newt = mathsat.api.MSAT_MAKE_ERROR_TERM();
          if (termIsAssignment(t)) {
            newt = mathsat.api.msat_make_equal(
                msatEnv, newargs[0], newargs[1]);
          } else {
            newt = mathsat.api.msat_replace_args(
                msatEnv, t, newargs);
          }
          assert(!mathsat.api.MSAT_ERROR_TERM(newt));
          cache.put(t, newt);
        }
      }
    }
    assert(cache.containsKey(f.getTerm()));
    return new MathsatSymbolicFormula(cache.get(f.getTerm()));
  }

  /**
   * returns an SSA map for the instantiated formula f
   */
  public SSAMap extractSSA(MathsatSymbolicFormula f) {
    SSAMap ssa = new SSAMap();
    Stack<Long> toProcess = new Stack<Long>();
    Set<Long> cache = new HashSet<Long>();

    toProcess.push(f.getTerm());
    while (!toProcess.empty()) {
      long t = toProcess.pop();
      if (cache.contains(t)) {
        continue;
      }
      cache.add(t);
      if (mathsat.api.msat_term_is_variable(t) != 0) {
        String name = mathsat.api.msat_term_repr(t);
        String[] bits = name.split("@");
        if (bits.length == 2) {
          try {
            int idx = Integer.parseInt(bits[1]);
            name = bits[0];
            if (idx > ssa.getIndex(name)) {
              ssa.setIndex(name, idx);
            }
          } catch (NumberFormatException e) {
            assert(false);
          }
        }
      } else {
        for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
          toProcess.push(mathsat.api.msat_term_get_arg(t, i));
        }
      }
    }

    return ssa;
  }

}
