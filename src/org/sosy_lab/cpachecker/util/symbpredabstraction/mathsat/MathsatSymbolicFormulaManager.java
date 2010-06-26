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
package org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.GlobalDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver;

import com.google.common.base.Preconditions;


/**
 * A SymbolicFormulaManager to deal with MathSAT formulas.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
@Options(prefix="cpas.symbpredabs")
public class MathsatSymbolicFormulaManager implements SymbolicFormulaManager {

  public final static int THEORY_EQ = 1;
  public final static int THEORY_UF = 2;
  public final static int THEORY_ARITH = 4;

  @Option(name="mathsat.useIntegers")
  private boolean useIntegers = false;

  @Option
  private boolean initAllVars = false;

  @Option
  private String noAutoInitPrefix = "__BLAST_NONDET";

  // if true, handle lvalues as *x, &x, s.x, etc. using UIFs. If false, just
  // ue variables
  @Option(name="mathsat.lvalsAsUIFs")
  private boolean lvalsAsUif = false;

  @Option
  private boolean useBitwiseAxioms = false;
  
  // the MathSAT environment in which all terms are created
  private final long msatEnv;

  // We need to distinguish assignments from tests. This is needed to
  // build a formula in SSA form later on, when we have to mapback
  // a counterexample, without adding too many extra variables. Therefore,
  // in the representation of "uninstantiated" symbolic formulas, we
  // use a new binary uninterpreted function ":=" to represent
  // assignments. When we instantiate the formula, we replace this UIF
  // with an equality, because now we have an SSA form
  private final long assignUfDecl;

  // UF encoding of some unsupported operations
  private final long bitwiseAndUfDecl;
  private final long bitwiseOrUfDecl;
  private final long bitwiseXorUfDecl;
  private final long bitwiseNotUfDecl;
  private final long leftShiftUfDecl;
  private final long rightShiftUfDecl;
  private final long multUfDecl;
  private final long divUfDecl;
  private final long modUfDecl;

  // datatype to use for variables, when converting them to mathsat vars
  // can be either MSAT_REAL or MSAT_INT
  // Note that MSAT_INT does not mean that we support the full linear
  // integer arithmetic (LIA)! At the moment, interpolation doesn't work on
  // LIA, only difference logic or on LRA (i.e. on the rationals). However
  // by setting the vars to be MSAT_INT, the solver tries some heuristics
  // that might work (e.g. tightening of a < b into a <= b - 1, splitting
  // negated equalities, ...)
  private final int msatVarType;
  private final int[] msatVarType1;
  private final int[] msatVarType2;

  // names for special variables needed to deal with functions
  private static final String VAR_RETURN_NAME = "__retval__";
  private static final String FUNCTION_PARAM_NAME = "__param__";
  private static final String OP_ADDRESSOF_NAME = "__ptrAmp__";
  private static final String OP_STAR_NAME = "__ptrStar__";
  private static final String OP_ARRAY_SUBSCRIPT = "__array__";

  // a namespace to have a unique name for each variable in the program.
  // Whenever we enter a function, we push its name as namespace. Each
  // variable will be instantiated inside mathsat as namespace::variable
  //private Stack<String> namespaces;
  private String namespace;
  // global variables (do not live in any namespace)
  private final Set<String> globalVars = new HashSet<String>();

  // various caches for speeding up expensive tasks
  //
  // cache for splitting arithmetic equalities in extractAtoms
  private final Map<Long, Boolean> arithCache = new HashMap<Long, Boolean>();

  // cache for uninstantiating terms (see uninstantiate() below)
  private final Map<Long, Long> uninstantiateCache = new HashMap<Long, Long>();

  // cache for replacing assignments
  private final Map<Long, Long> replaceAssignmentsCache = new HashMap<Long, Long>();
  private final Map<Long, Integer> neededTheories = new HashMap<Long, Integer>();

  private final Set<String> printedWarnings = new HashSet<String>();

  private final Map<String, Long> stringLitToMsat = new HashMap<String, Long>();
  private int nextStringLitIndex = 0;
  private final long stringLitUfDecl;

  private final LogManager logger;
  private final MathsatAbstractionPrinter absPrinter;  
  
  public MathsatSymbolicFormulaManager(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this, MathsatSymbolicFormulaManager.class);
    this.logger = logger;
    msatEnv = mathsat.api.msat_create_env();
    if (useIntegers) {
      msatVarType = mathsat.api.MSAT_INT;
    } else {
      msatVarType = mathsat.api.MSAT_REAL;
    }
    setNamespace("");

    msatVarType1 = new int[]{msatVarType};
    msatVarType2 = new int[]{msatVarType, msatVarType};
    assignUfDecl = mathsat.api.msat_declare_uif(msatEnv, ":=",
        mathsat.api.MSAT_BOOL, 2, msatVarType2);

    bitwiseAndUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_&_",
        msatVarType, 2, msatVarType2);
    bitwiseOrUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_|_",
        msatVarType, 2, msatVarType2);
    bitwiseXorUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_^_",
        msatVarType, 2, msatVarType2);
    bitwiseNotUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_~_",
        msatVarType, 1, msatVarType1);
    leftShiftUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_<<_",
        msatVarType, 2, msatVarType2);
    rightShiftUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_>>_",
        msatVarType, 2, msatVarType2);
    multUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_*_",
        msatVarType, 2, msatVarType2);
    divUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_/_",
        msatVarType, 2, msatVarType2);
    modUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_%_",
        msatVarType, 2, msatVarType2);

    stringLitUfDecl = mathsat.api.msat_declare_uif(msatEnv, "__string__",
        msatVarType, 1, msatVarType1);
    
    absPrinter = new MathsatAbstractionPrinter(msatEnv, "abs", logger);
  }

  long getMsatEnv() {
    return msatEnv;
  }

  private long[] getTerm(SymbolicFormula[] f) {
    int length = f.length;
    long[] result = new long[length];
    for (int i = 0; i < length; i++) {
      result[i] = ((MathsatSymbolicFormula)f[i]).getTerm();
    }
    return result;
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

  @Override
  public String dumpFormula(SymbolicFormula f) {
    MathsatSymbolicFormula m = (MathsatSymbolicFormula)f;

    return mathsat.api.msat_to_msat(msatEnv, m.getTerm());
  }

  @Override
  public void dumpAbstraction(SymbolicFormula curState, SymbolicFormula edgeFormula,
      SymbolicFormula predDef, List<SymbolicFormula> importantPreds) {
    
    absPrinter.printMsatFormat(curState, edgeFormula, predDef, importantPreds);
    absPrinter.printNusmvFormat(curState, edgeFormula, predDef, importantPreds);
    absPrinter.nextNum();
  }

  @Override
  public SymbolicFormula makeNot(SymbolicFormula f) {
    MathsatSymbolicFormula m = (MathsatSymbolicFormula)f;

    long a = mathsat.api.msat_make_not(msatEnv, m.getTerm());
    return new MathsatSymbolicFormula(a);
  }


  @Override
  public SymbolicFormula makeAnd(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

    long a = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), m2.getTerm());
    return new MathsatSymbolicFormula(a);
  }

  private MathsatSymbolicFormula makeAnd(MathsatSymbolicFormula f1, long f2) {
    long a = mathsat.api.msat_make_and(msatEnv, f1.getTerm(), f2);
    return new MathsatSymbolicFormula(a);
  }

  @Override
  public SymbolicFormula makeOr(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

    long a = mathsat.api.msat_make_or(msatEnv, m1.getTerm(), m2.getTerm());
    return new MathsatSymbolicFormula(a);
  }

  @Override
  public SymbolicFormula makeEquivalence(SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

    long a = mathsat.api.msat_make_iff(msatEnv, m1.getTerm(), m2.getTerm());
    return new MathsatSymbolicFormula(a);
  }

  @Override
  public SymbolicFormula makeTrue() {
    return new MathsatSymbolicFormula(mathsat.api.msat_make_true(msatEnv));
  }

  @Override
  public SymbolicFormula parseInfix(String s) {
    long f = mathsat.api.msat_from_string(msatEnv, s);
    Preconditions.checkArgument(!mathsat.api.MSAT_ERROR_TERM(f), "Could not parse formula as Mathsat formula.");

    return new MathsatSymbolicFormula(f);
  }

  @Override
  public SymbolicFormula makeIfThenElse(SymbolicFormula atom, SymbolicFormula f1, SymbolicFormula f2) {
    MathsatSymbolicFormula mAtom = (MathsatSymbolicFormula)atom;
    MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
    MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

    long ite = mathsat.api.msat_make_ite(msatEnv, mAtom.getTerm(), m1.getTerm(), m2.getTerm());

    return new MathsatSymbolicFormula(ite);
  }

  @Override
  public PathFormula makeAnd(
      SymbolicFormula f1, CFAEdge edge, SSAMap ssa)
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
          m1, (FunctionDefinitionNode)edge.getPredecessor(), ssa);
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
          logger.log(Level.ALL, "DEBUG_3",
              "MathsatSymbolicFormulaManager, IGNORING return ",
              "from main: ", edge.getRawStatement());
        } else {
          return makeAndReturn(m1, statementEdge, ssa);
        }
      } else {
        return makeAndStatement(m1, statementEdge, ssa);
      }
      break;
    }

    case DeclarationEdge: {
      return makeAndDeclaration(m1, (DeclarationEdge)edge, ssa);
    }

    case AssumeEdge: {
      return makeAndAssume(m1, (AssumeEdge)edge, ssa);
    }

    case BlankEdge: {
      break;
    }

    case FunctionCallEdge: {
      return makeAndFunctionCall(m1, (FunctionCallEdge)edge, ssa);
    }

    case ReturnEdge: {
      // get the expression from the summary edge
      CFANode succ = edge.getSuccessor();
      CallToReturnEdge ce = succ.getEnteringSummaryEdge();
      PathFormula ret =
        makeAndExitFunction(m1, ce, ssa);
      //popNamespace(); - done inside makeAndExitFunction
      return ret;
    }

    default:
      throw new UnrecognizedCFAEdgeException(edge);
    }

    return new PathFormula(f1, ssa);
  }

  private PathFormula makeAndDeclaration(
      MathsatSymbolicFormula m1, DeclarationEdge declarationEdge, SSAMap ssa)
      throws UnrecognizedCFAEdgeException {
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

        int idx = 1;
        newssa.setIndex(var, 1);

        logger.log(Level.ALL, "DEBUG_3",
            "Declared enum field: ", var, ", index: ", idx);

        long minit = buildMsatTerm(exp, newssa);
        long mvar = buildMsatVariable(var, idx);
        long t = makeAssignment(mvar, minit);
        m1 = makeAnd(m1, t);
      }
      return new PathFormula(m1, newssa);
    }


    if (!(spec instanceof IASTSimpleDeclSpecifier ||
        spec instanceof IASTElaboratedTypeSpecifier ||
        spec instanceof IASTNamedTypeSpecifier)) {

      if (spec instanceof IASTCompositeTypeSpecifier) {
        // this is the declaration of a struct, just ignore it...
        log(Level.ALL, "Ignoring declaration", spec);
        return new PathFormula(m1, newssa);
      } else {
        throw new UnrecognizedCFAEdgeException(
            "UNSUPPORTED SPECIFIER FOR DECLARATION: " +
            declarationEdge.getRawStatement());
      }
    }

    if (spec.getStorageClass() == IASTDeclSpecifier.sc_typedef) {
      log(Level.ALL, "Ignoring typedef", spec);
      return new PathFormula(m1, newssa);
    }

    for (IASTDeclarator d : decls) {
      String var = d.getName().getRawSignature();
      if (isGlobal) {
        globalVars.add(var);
      }
      var = scoped(var);
      // TODO here makeLvalIndex(var, ssa) should be used
      int idx = ssa.getIndex(var);
      if (idx > 0) idx++;
      else idx = 1;
      newssa.setIndex(var, idx);

      logger.log(Level.ALL, "DEBUG_3",
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
          log(Level.ALL, "Ingoring unsupported initializer", init);
          continue;
        }
        IASTExpression exp =
          ((IASTInitializerExpression)init).getExpression();
        long minit = buildMsatTerm(exp, newssa);
        long mvar = buildMsatVariable(var, idx);
        long t = makeAssignment(mvar, minit);
        m1 = makeAnd(m1, t);
      } else if (spec.getStorageClass() ==
        IASTDeclSpecifier.sc_extern) {
        log(Level.ALL, "Ignoring initializer of extern declaration", d);
      } else if (isGlobal || initAllVars) {
        // auto-initialize variables to zero, unless they match
        // the noAutoInitPrefix pattern
        if (noAutoInitPrefix.equals("") ||
            !d.getName().getRawSignature().startsWith(noAutoInitPrefix)) {
          long mvar = buildMsatVariable(var, idx);
          long z = mathsat.api.msat_make_number(msatEnv, "0");
          long t = makeAssignment(mvar, z);
          m1 = makeAnd(m1, t);
          logger.log(Level.ALL, "DEBUG_3", "AUTO-INITIALIZING",
              (isGlobal ? "GLOBAL" : ""), "VAR: ",
              var, " (", d.getName().getRawSignature(), ")");
        } else {
          logger.log(Level.ALL, "DEBUG_3",
              "NOT AUTO-INITIALIZING VAR:", var);
        }
      }
    }
    return new PathFormula(m1, newssa);
  }

  private Pair<SymbolicFormula, SSAMap> makeAndEnterFunction(
      MathsatSymbolicFormula m1, FunctionDefinitionNode fn, SSAMap ssa)
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
      int idx = getIndex(paramName, ssa);
      long msatParam = buildMsatVariable(paramName, idx);
      if (mathsat.api.MSAT_ERROR_TERM(msatParam)) {
        throw new UnrecognizedCFAEdgeException(
            "ERROR ENTERING FUNCTION: " +
            fn.getFunctionDefinition().getRawSignature());
      }
      if (param.getDeclarator().getPointerOperators().length != 0) {
        log(Level.WARNING, "Ignoring the semantics of pointer for parameter "
            + param.getDeclarator().getName(), fn.getFunctionDefinition().getDeclarator());
      }
      String pn = param.getDeclarator().getName().toString();
      if (pn.isEmpty()) {
        assert(param.getDeclarator().getNestedDeclarator() != null);
        pn = param.getDeclarator().getNestedDeclarator().
        getName().toString();
      }
      assert(!pn.isEmpty());
      String formalParamName = scoped(pn);
      idx = makeLvalIndex(formalParamName, newssa);
      long msatFormalParam = buildMsatVariable(formalParamName, idx);
      if (mathsat.api.MSAT_ERROR_TERM(msatParam)) {
        throw new UnrecognizedCFAEdgeException(
            "ERROR HANDLING FUNCTION CALL: " +
            fn.getFunctionDefinition().getRawSignature());
      }
      long eq = makeAssignment(msatFormalParam, msatParam);
      term = mathsat.api.msat_make_and(msatEnv, term, eq);
    }
    return new PathFormula(makeAnd(m1, term), ssa);
  }

  private void log(Level level, String msg, IASTNode astNode) {
    msg = "Line " + astNode.getFileLocation().getStartingLineNumber()
        + ": " + msg
        + ": " + astNode.getRawSignature();

    if (printedWarnings.add(msg)) {
      logger.log(level, 1, msg);
    }
  }

  private PathFormula makeAndExitFunction(
      MathsatSymbolicFormula m1, CallToReturnEdge ce, SSAMap ssa)
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

      int retidx = getIndex(retvar, ssa);
      long msatretvar = buildMsatVariable(retvar, retidx);
      IASTExpression e = exp.getOperand1();
      setNamespace(ce.getSuccessor().getFunctionName());
      long msatoutvar = buildMsatLvalueTerm(e, ssa);
      long term = makeAssignment(msatoutvar, msatretvar);
      return new PathFormula(makeAnd(m1, term), ssa);
    } else {
      throw new UnrecognizedCFAEdgeException(
          "UNKNOWN FUNCTION EXIT EXPRESSION: " +
          ce.getRawStatement());
    }
  }

  private PathFormula makeAndFunctionCall(
      MathsatSymbolicFormula m1, FunctionCallEdge edge, SSAMap ssa)
      throws UnrecognizedCFAEdgeException {
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
          msatActualParams[i] = buildMsatTerm(actualParams[i], ssa);
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
          log(Level.WARNING, "Ignoring the semantics of pointer for parameter "
              + param.getDeclarator().getName(), fn.getFunctionDefinition().getDeclarator());
        }
        String paramName = scoped(FUNCTION_PARAM_NAME + (i-1));
        int idx = makeLvalIndex(paramName, ssa);
        long msatParam = buildMsatVariable(paramName, idx);
        if (mathsat.api.MSAT_ERROR_TERM(msatParam)) {
          throw new UnrecognizedCFAEdgeException(
              "ERROR HANDLING FUNCTION CALL: " +
              edge.getRawStatement());
        }
        long eq = makeAssignment(msatParam, arg);
        term = mathsat.api.msat_make_and(msatEnv, term, eq);
      }
      assert(!mathsat.api.MSAT_ERROR_TERM(term));
      return new PathFormula(makeAnd(m1, term), ssa);
    }
  }

  private PathFormula makeAndReturn(
      MathsatSymbolicFormula m1, StatementEdge edge, SSAMap ssa)
      throws UnrecognizedCFAEdgeException {
    IASTExpression exp = edge.getExpression();
    if (exp == null) {
      // this is a return from a void function, do nothing
      return new PathFormula(m1, ssa);
    } else if (exp instanceof IASTUnaryExpression) {
      SSAMap ssa2 = new SSAMap();
      ssa2.copyFrom(ssa);
      ssa = ssa2;
      
      // we have to save the information about the return value,
      // so that we can use it later on, if it is assigned to
      // a variable. We create a function::__retval__ variable
      // that will hold the return value
      String retvalname = scoped(VAR_RETURN_NAME);
      int idx = makeLvalIndex(retvalname, ssa);

      long retvar = buildMsatVariable(retvalname, idx);
      long retval = buildMsatTerm(exp, ssa);
      if (!mathsat.api.MSAT_ERROR_TERM(retval)) {
        long term = makeAssignment(retvar, retval);
        return new PathFormula(makeAnd(m1, term), ssa);
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

  private String exprToVarName(IASTExpression e) {
    return e.getRawSignature().replaceAll("[ \n\t]", "");
  }

  private long buildMsatTermVar(String var, SSAMap ssa) {
    var = scoped(var);
    int idx = getIndex(var, ssa);
    return buildMsatVariable(var, idx);
  }

  private long buildMsatTerm(IASTExpression exp, SSAMap ssa) {
    if (exp instanceof IASTIdExpression) {
      // this is a variable: get the right index for the SSA
      String var = ((IASTIdExpression)exp).getName().getRawSignature();
      return buildMsatTermVar(var, ssa);
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
        logger.log(Level.ALL, "DEBUG_1",
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
      logger.log(Level.ALL, "DEBUG_3", "IGNORING TYPE CAST:",
          exp.getRawSignature());
      return buildMsatTerm(((IASTCastExpression)exp).getOperand(), ssa);
    } else if (exp instanceof IASTUnaryExpression) {
      IASTExpression operand = ((IASTUnaryExpression)exp).getOperand();
      int op = ((IASTUnaryExpression)exp).getOperator();
      switch (op) {
      case IASTUnaryExpression.op_bracketedPrimary:
        return buildMsatTerm(operand, ssa);
      case IASTUnaryExpression.op_postFixIncr:
      case IASTUnaryExpression.op_prefixIncr:
      case IASTUnaryExpression.op_postFixDecr:
      case IASTUnaryExpression.op_prefixDecr: {
        long mvar = buildMsatTerm(operand, ssa);
        long newvar =
          buildMsatLvalueTerm(operand, ssa);
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
        long mop = buildMsatTerm(operand, ssa);
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
          long term = buildMsatTerm(operand, ssa);
          if (mathsat.api.MSAT_ERROR_TERM(term)) return term;

          // PW make SSA index of * independent from argument
          int idx = getIndex(opname, ssa);
          //int idx = getIndex(
          //    opname, term, ssa, absoluteSSAIndices);

          // build the  function corresponding to this operation.
          long decl = mathsat.api.msat_declare_uif(
              msatEnv, opname + "@" + idx, msatVarType, 1, msatVarType1);
          return mathsat.api.msat_make_uif(msatEnv, decl,
              new long[]{term});
        } else {
          warnUnsafeVar(exp);
          return buildMsatTermVar(exprToVarName(exp), ssa);
        }

      case IASTUnaryExpression.op_tilde: {
        long term = buildMsatTerm(operand, ssa);
        if (mathsat.api.MSAT_ERROR_TERM(term)) {
          return term;
        }
        return mathsat.api.msat_make_uif(msatEnv, bitwiseNotUfDecl, new long[]{term});
      }

      /* !operand cannot be handled directly in case operand is a variable
       * we would need to know if operand is of type boolean or something else
       * currently ! is handled by the default branch
      case IASTUnaryExpression.op_not: {
        long operandMsat = buildMsatTerm(operand, ssa);
        if (mathsat.api.MSAT_ERROR_TERM(operandMsat)) {
          return operandMsat;
        }
        return mathsat.api.msat_make_not(msatEnv, operandMsat);
      }*/

      case IASTUnaryExpression.op_sizeof: {
        // TODO
        //return mathsat.api.MSAT_MAKE_ERROR_TERM();
        warnUnsafeVar(exp);
        return buildMsatTermVar(exprToVarName(exp), ssa);
      }

      default: {
        // this might be a predicate implicitly cast to an int. Let's
        // see if this is indeed the case...
        MathsatSymbolicFormula ftmp = buildFormulaPredicate(exp, true, ssa);
        if (ftmp == null) {
          return mathsat.api.MSAT_MAKE_ERROR_TERM();
        } else {
          //assert(false);
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
        long me2 = buildMsatTerm(e2, ssa);
        if (mathsat.api.MSAT_ERROR_TERM(me2)) return me2;
        if (op != IASTBinaryExpression.op_assign) {
          // in this case, we have to get the old SSA instance for
          // reading the value of the variable, and build the
          // corresponding expression
          long oldvar = buildMsatTerm(e1, ssa);
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
                  msatEnv, me2, oldvar);
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
                    msatEnv, me2, oldvar);
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
          default:
            return mathsat.api.MSAT_MAKE_ERROR_TERM();
          }
          if (mathsat.api.MSAT_ERROR_TERM(me2)) return me2;
        }
        long mvar = buildMsatLvalueTerm(e1, ssa);
        if (mathsat.api.MSAT_ERROR_TERM(mvar)) return mvar;
        return makeAssignment(mvar, me2);
      }

      case IASTBinaryExpression.op_plus:
      case IASTBinaryExpression.op_minus:
      case IASTBinaryExpression.op_multiply:
      case IASTBinaryExpression.op_divide: {
        long me1 = buildMsatTerm(e1, ssa);
        if (mathsat.api.MSAT_ERROR_TERM(me1)) return me1;
        long me2 = buildMsatTerm(e2, ssa);
        if (mathsat.api.MSAT_ERROR_TERM(me2)) return me2;

        switch (op) {
        case IASTBinaryExpression.op_plus:
          return mathsat.api.msat_make_plus(msatEnv, me1, me2);
        case IASTBinaryExpression.op_minus:
          return mathsat.api.msat_make_minus(msatEnv, me1, me2);
        case IASTBinaryExpression.op_multiply:
          if (mathsat.api.msat_term_is_number(me1) != 0) {
            return mathsat.api.msat_make_times(msatEnv, me1, me2);
          } else if (mathsat.api.msat_term_is_number(me2) != 0) {
            return mathsat.api.msat_make_times(msatEnv, me2, me1);
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
                  msatEnv, me2, me1);
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
        long me1 = buildMsatTerm(e1, ssa);
        if (mathsat.api.MSAT_ERROR_TERM(me1)) return me1;
        long me2 = buildMsatTerm(e2, ssa);
        if (mathsat.api.MSAT_ERROR_TERM(me2)) return me2;
        return buildMsatUF(op, me1, me2);
      }

      default:
        // this might be a predicate implicitly cast to an int, like this:
        // int tmp = (a == b)
        // Let's see if this is indeed the case...
        MathsatSymbolicFormula ftmp = buildFormulaPredicate(
            exp, true, ssa);
      if (ftmp == null) {
        return mathsat.api.MSAT_MAKE_ERROR_TERM();
      } else {
        // PW why this assertion? without it, everything seems to work nicely
        //System.out.println(exp.getRawSignature());
        //assert(false);
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
        long term = buildMsatTerm(owner, ssa);

        if (mathsat.api.MSAT_ERROR_TERM(term)) return term;

        String tpname = getTypeName(owner.getExpressionType());
        String ufname =
          (fexp.isPointerDereference() ? "->{" : ".{") +
          tpname + "," + field + "}";
        long[] aterm = {term};
        int idx = getIndex(ufname, aterm, ssa, true);

        // see above for the case of &x and *x
        long decl = mathsat.api.msat_declare_uif(
            msatEnv, ufname + "@" + idx, msatVarType, 1, msatVarType1);
        return mathsat.api.msat_make_uif(msatEnv, decl, aterm);
      } else {
        warnUnsafeVar(exp);
        return buildMsatTermVar(exprToVarName(exp), ssa);

      }
    } else if (exp instanceof IASTArraySubscriptExpression) {
      if (lvalsAsUif) {
        IASTArraySubscriptExpression aexp =
          (IASTArraySubscriptExpression)exp;
        IASTExpression arrexp = aexp.getArrayExpression();
        IASTExpression subexp = aexp.getSubscriptExpression();
        long aterm = buildMsatTerm(arrexp, ssa);
        long sterm = buildMsatTerm(subexp, ssa);

        if (mathsat.api.MSAT_ERROR_TERM(aterm)) return aterm;
        if (mathsat.api.MSAT_ERROR_TERM(sterm)) return sterm;

        String ufname = OP_ARRAY_SUBSCRIPT;
        long[] args = {aterm, sterm};
        int idx = getIndex(ufname, args, ssa, true);

        long decl = mathsat.api.msat_declare_uif(
            msatEnv, ufname + "@" + idx, msatVarType, 2, msatVarType2);
        return mathsat.api.msat_make_uif(msatEnv, decl,
            new long[]{aterm, sterm});
      } else {
        warnUnsafeVar(exp);
        return buildMsatTermVar(exprToVarName(exp), ssa);
      }
    } else if (exp instanceof IASTFunctionCallExpression) {
      // this is an external call. We have to create an UIF.
      IASTFunctionCallExpression fexp = (IASTFunctionCallExpression)exp;
      return buildMsatTermExternalFunctionCall(fexp, ssa);
    } else if (exp instanceof IASTTypeIdExpression) {
      assert(((IASTTypeIdExpression)exp).getOperator() ==
        IASTTypeIdExpression.op_sizeof);
      warnUnsafeVar(exp);
      return buildMsatTermVar(exprToVarName(exp), ssa);
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
      logger.logException(Level.WARNING, e, "");
      assert(false);
    }
    return null;
  }

  /**
   * Produces a fresh new SSA index for the left-hand side of an assignment
   * and updates the SSA map.
   */
  private int makeLvalIndex(String name, SSAMap ssa) {
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
  
  private int getIndex(String var, SSAMap ssa) {
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
  private int makeLvalIndex(String name, SymbolicFormula[] args, SSAMap ssa) {
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

  private int getIndex(String name, long[] args, SSAMap ssa, boolean autoInstantiate) {
    SymbolicFormula[] a = new SymbolicFormula[args.length];
    for (int i = 0; i < a.length; ++i) {
      a[i] = new MathsatSymbolicFormula(args[i]);
    }
    int idx = ssa.getIndex(name, a);
    if (idx <= 0) {
      if (!autoInstantiate) {
        return -1;
      } else {
        logger.log(Level.ALL, "DEBUG_3",
            "WARNING: Auto-instantiating lval: ", name, "(", a, ")");
        idx = 1;
        ssa.setIndex(name, a, idx);
      }
    }
    return idx;
  }

  private long buildMsatLvalueTerm(IASTExpression exp, SSAMap ssa) {
    if (exp instanceof IASTIdExpression || !lvalsAsUif) {
      String var = null;
      if (exp instanceof IASTIdExpression) {
        var = ((IASTIdExpression)exp).getName().getRawSignature();
      } else {
        var = exprToVarName(exp);
      }
      var = scoped(var);
      int idx = makeLvalIndex(var, ssa);

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
      long term = buildMsatTerm(operand, ssa);
      if (mathsat.api.MSAT_ERROR_TERM(term)) return term;

      // PW make SSA index of * independent from argument
      int idx = makeLvalIndex(opname, ssa);
      //int idx = makeLvalIndex(opname, term, ssa, absoluteSSAIndices);

      // build the "updated" function corresponding to this operation.
      // what we do is the following:
      // C            |     MathSAT
      // *x = 1       |     <ptr_*>::2(x) = 1
      // ...
      // &(*x) = 2    |     <ptr_&>::2(<ptr_*>::1(x)) = 2
      long decl = mathsat.api.msat_declare_uif(msatEnv,
          opname + "@" + idx, msatVarType, 1, msatVarType1);
      return mathsat.api.msat_make_uif(msatEnv, decl, new long[]{term});

    } else if (exp instanceof IASTFieldReference) {
      IASTFieldReference fexp = (IASTFieldReference)exp;
      String field = fexp.getFieldName().getRawSignature();
      IASTExpression owner = fexp.getFieldOwner();
      long term = buildMsatTerm(owner, ssa);

      if (mathsat.api.MSAT_ERROR_TERM(term)) return term;

      String tpname = getTypeName(owner.getExpressionType());
      String ufname =
        (fexp.isPointerDereference() ? "->{" : ".{") +
        tpname + "," + field + "}";
      SymbolicFormula[] args = {new MathsatSymbolicFormula(term)};
      int idx = makeLvalIndex(ufname, args, ssa);

      // see above for the case of &x and *x
      long decl = mathsat.api.msat_declare_uif(msatEnv,
          ufname + "@" + idx, msatVarType, 1, msatVarType1);
      return mathsat.api.msat_make_uif(msatEnv, decl, new long[]{term});

    } else if (exp instanceof IASTArraySubscriptExpression) {
      IASTArraySubscriptExpression aexp =
        (IASTArraySubscriptExpression)exp;
      IASTExpression arrexp = aexp.getArrayExpression();
      IASTExpression subexp = aexp.getSubscriptExpression();
      long aterm = buildMsatTerm(arrexp, ssa);
      long sterm = buildMsatTerm(subexp, ssa);

      if (mathsat.api.MSAT_ERROR_TERM(aterm)) return aterm;
      if (mathsat.api.MSAT_ERROR_TERM(sterm)) return sterm;

      String ufname = OP_ARRAY_SUBSCRIPT;
      SymbolicFormula[] args = {new MathsatSymbolicFormula(aterm), new MathsatSymbolicFormula(sterm)};
      int idx = makeLvalIndex(ufname, args, ssa);

      long decl = mathsat.api.msat_declare_uif(msatEnv,
          ufname + "@" + idx, msatVarType, 2, msatVarType2);
      return mathsat.api.msat_make_uif(msatEnv, decl,
          new long[]{aterm, sterm});
    }
    // unknown lvalue
    return mathsat.api.MSAT_MAKE_ERROR_TERM();
  }

  private long buildMsatTermExternalFunctionCall(
      IASTFunctionCallExpression fexp, SSAMap ssa) {
    IASTExpression fn = fexp.getFunctionNameExpression();
    String func;
    if (fn instanceof IASTIdExpression) {
      log(Level.INFO, "Assuming external function to be a pure function", fn);
      func = ((IASTIdExpression)fn).getName().getRawSignature();
    } else {
      log(Level.WARNING, "Ignoring function call through function pointer", fexp);
      func = "<func>{" + fn.getRawSignature() + "}";
    }

    IASTExpression pexp = fexp.getParameterExpression();
    if (pexp == null) {
      // this is a function of arity 0. We create a fresh global variable
      // for it (instantiated at 1 because we need an index but it never
      // increases)
      globalVars.add(func);
      return buildMsatVariable(func, 1);
    } else {
      IASTExpression[] args;
      if (pexp instanceof IASTExpressionList) {
        args = ((IASTExpressionList)pexp).getExpressions();
      } else {
        args = new IASTExpression[]{pexp};
      }
      func += "{" + args.length + "}"; // add #arguments to function name to cope with varargs functions
      long[] mArgs = new long[args.length];
      int[] tp = new int[args.length];
      for (int i = 0; i < args.length; ++i) {
        mArgs[i] = buildMsatTerm(args[i], ssa);
        tp[i] = msatVarType;
        if (mathsat.api.MSAT_ERROR_TERM(mArgs[i])) {
          return mArgs[i];
        }
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
    log(Level.WARNING, "Unhandled expression treated as free variable", exp);
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

  private boolean isAssignment(long term) {
    return mathsat.api.msat_term_get_decl(term) == assignUfDecl;
  }

  private long makeAssignment(long t1, long t2) {
    return mathsat.api.msat_make_uif(msatEnv, assignUfDecl, new long[]{t1, t2});
  }

  private PathFormula makeAndStatement(
      MathsatSymbolicFormula f1, StatementEdge stmt, SSAMap ssa)
      throws UnrecognizedCFAEdgeException {
    IASTExpression expr = stmt.getExpression();
    if (needsSSAUpdate(expr)) {
      SSAMap ssa2 = new SSAMap();
      ssa2.copyFrom(ssa);
      ssa = ssa2;
    }
    long f2 = buildMsatTerm(expr, ssa);

    if (!mathsat.api.MSAT_ERROR_TERM(f2)) {
      long d = mathsat.api.msat_term_get_decl(f2);
      if (mathsat.api.msat_decl_get_return_type(d) !=
        mathsat.api.MSAT_BOOL) {
        // in this case, we have something like:
          // f(x);
        // i.e. an expression that gets assigned to nothing. Since
        // we don't handle side-effects, this means that the
        // expression has no effect, and we can just drop it
        log(Level.INFO, "Statement is assumed to be side-effect free, but its return value is not used",
            stmt.getExpression());
        return new PathFormula(f1, ssa);
      }
      return new PathFormula(makeAnd(f1, f2), ssa);
    } else {
      throw new UnrecognizedCFAEdgeException("STATEMENT: " +
          stmt.getRawStatement());
    }
  }

  protected MathsatSymbolicFormula buildFormulaPredicate(
      IASTExpression exp, boolean isTrue, SSAMap ssa) {
    if (exp instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression)exp);
      int opType = binExp.getOperator();
      IASTExpression op1 = binExp.getOperand1();
      IASTExpression op2 = binExp.getOperand2();

      long t1 = buildMsatTerm(op1, ssa);
      long t2 = buildMsatTerm(op2, ssa);

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
        long t = buildMsatTerm(exp, ssa);
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
        MathsatSymbolicFormula r = buildFormulaPredicate(exp1, !isTrue, ssa);
        return r;
      } else if (unaryExp.getOperator() ==
        IASTUnaryExpression.op_bracketedPrimary) {
        return buildFormulaPredicate(unaryExp.getOperand(), isTrue, ssa);
      } else {
        // build the mathsat term. If this is not a predicate, make
        // it a predicate by adding a "!= 0"
        long t = buildMsatTerm(exp, ssa);
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
      long t = buildMsatTerm(exp, ssa);
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

  private PathFormula makeAndAssume(MathsatSymbolicFormula f1,
      AssumeEdge assume, SSAMap ssa) throws UnrecognizedCFAEdgeException {
    MathsatSymbolicFormula f2 = buildFormulaPredicate(
        assume.getExpression(), assume.getTruthAssumption(), ssa);
    if (f2 == null) {
      throw new UnrecognizedCFAEdgeException("ASSUME: " + assume.getRawStatement());
    } else {
      return new PathFormula(makeAnd(f1, f2), ssa);
    }
  }

  // creates the two mathsat terms
  // (var@newidx = var@i1) and (var@newidx = var@i2)
  // used by mergeSSAMaps (where newidx = max(i1, i2))
  private Pair<Long, Long> makeSSAMerger(String var, int i1, int i2) {
    // retrieve the mathsat terms corresponding to the two variables
    long v1 = buildMsatVariable(var, i1);
    long v2 = buildMsatVariable(var, i2);
    long e1 = mathsat.api.msat_make_true(msatEnv);
    long e2 = mathsat.api.msat_make_true(msatEnv);
    if (i1 < i2) {
      for (int i = i1+1; i <= i2; ++i) {
        long v = buildMsatVariable(var, i);
        long e = mathsat.api.msat_make_equal(msatEnv, v, v1);
        e1 = mathsat.api.msat_make_and(msatEnv, e1, e);
      }
    } else {
      assert(i2 < i1);
      for (int i = i2+1; i <= i1; ++i) {
        long v = buildMsatVariable(var, i);
        long e = mathsat.api.msat_make_equal(msatEnv, v, v2);
        e2 = mathsat.api.msat_make_and(msatEnv, e2, e);
      }
    }
    return new Pair<Long, Long>(e1, e2);
  }

  private Pair<Long, Long> makeSSAMerger(String name,
      long[] args, int i1, int i2) {
    // retrieve the mathsat terms corresponding to the two variables
    long v1 = buildMsatUFLvalue(name, args, i1);
    long v2 = buildMsatUFLvalue(name, args, i2);
    long e1 = mathsat.api.msat_make_true(msatEnv);
    long e2 = mathsat.api.msat_make_true(msatEnv);
    if (i1 < i2) {
      for (int i = i1+1; i <= i2; ++i) {
        long v = buildMsatUFLvalue(name, args, i);
        long e = mathsat.api.msat_make_equal(msatEnv, v, v1);
        e1 = mathsat.api.msat_make_and(msatEnv, e1, e);
      }
    } else {
      assert(i2 < i1);
      for (int i = i2+1; i <= i1; ++i) {
        long v = buildMsatUFLvalue(name, args, i);
        long e = mathsat.api.msat_make_equal(msatEnv, v, v2);
        e2 = mathsat.api.msat_make_and(msatEnv, e2, e);
      }
    }
    return new Pair<Long, Long>(e1, e2);
  }

  @Override
  public Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mergeSSAMaps(
      SSAMap ssa1, SSAMap ssa2) {
    SSAMap result = new SSAMap();
    long mt1 = mathsat.api.msat_make_true(msatEnv);
    long mt2 = mathsat.api.msat_make_true(msatEnv);
    for (String var : ssa1.allVariables()) {
      int i1 = ssa1.getIndex(var);
      int i2 = ssa2.getIndex(var);
      assert(i1 > 0);
      if (i2 > 0 && i2 != i1) {
        // we have to merge this variable assignment
        result.setIndex(var, Math.max(i1, i2));
        Pair<Long, Long> t = makeSSAMerger(var, i1, i2);
        mt1 = mathsat.api.msat_make_and(msatEnv, mt1, t.getFirst());
        mt2 = mathsat.api.msat_make_and(msatEnv, mt2, t.getSecond());
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
        assert(i1 == i2 || result.getIndex(var) == Math.max(i1, i2));
      }
    }

    for (Pair<String, SymbolicFormula[]> f : ssa1.allFunctions()) {
      int i1 = ssa1.getIndex(f.getFirst(), f.getSecond());
      int i2 = ssa2.getIndex(f.getFirst(), f.getSecond());
      assert(i1 > 0);
      if (i2 > 0 && i2 != i1) {
        // we have to merge this lvalue assignment
        result.setIndex(f.getFirst(), f.getSecond(), Math.max(i1, i2));
        long[] args = getTerm(f.getSecond());
        Pair<Long, Long> t = makeSSAMerger(f.getFirst(), args, i1, i2);
        mt1 = mathsat.api.msat_make_and(msatEnv, mt1, t.getFirst());
        mt2 = mathsat.api.msat_make_and(msatEnv, mt2, t.getSecond());
      } else {
        if (i2 <= 0) {
          // it's not enough to set the SSA index. We *must* also
          // generate a formula saying that the var does not change
          // in this branch!
          long[] args = getTerm(f.getSecond());
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
        long[] args = getTerm(f.getSecond());
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
          if (isAssignment(t)) {
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
                  getIndex(name, newargs, ssa, false) : 1);
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

  boolean ufCanBeLvalue(String name) {
    return name.startsWith(".{") || name.startsWith("->{");
  }


  @Override
  public SymbolicFormula makeFalse() {
    return new MathsatSymbolicFormula(mathsat.api.msat_make_false(msatEnv));
  }


  /**
   * As a side effect, this method does the same thing as {@link #replaceAssignments(SymbolicFormula)}
   * to the formula.
   */
  @Override
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
            logger.log(Level.ALL, "DEBUG_1",
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
          if (isAssignment(t)) {
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
                  logger.log(Level.ALL, "DEBUG_1",
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

  int getNeededTheories(SymbolicFormula f) {
    long term = ((MathsatSymbolicFormula)f).getTerm();
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

  /**
   * Looks for uninterpreted functions in the formula and adds bitwise
   * axioms for them.
   */
  @Override
  public SymbolicFormula prepareFormula(SymbolicFormula f) {
    if (useBitwiseAxioms) {
      SymbolicFormula bitwiseAxioms = getBitwiseAxioms(f);
      if (!bitwiseAxioms.isTrue()) {
        f = makeAnd(f, bitwiseAxioms);

        logger.log(Level.ALL, "DEBUG_3", "ADDED BITWISE AXIOMS:", bitwiseAxioms);
      }
    }
    return f;
  }
  
  /**
   * Looks for uninterpreted functions in the formulas and adds bitwise
   * axioms for them to the last formula.
   */
  @Override
  public void prepareFormulas(List<SymbolicFormula> formulas) {
    if (useBitwiseAxioms) {
      SymbolicFormula bitwiseAxioms = makeTrue();
  
      for (SymbolicFormula fm : formulas) {
        SymbolicFormula a = getBitwiseAxioms(fm);
        if (!a.isTrue()) {
          bitwiseAxioms = makeAnd(bitwiseAxioms, a);  
        }
      }
  
      if (!bitwiseAxioms.isTrue()) {
        logger.log(Level.ALL, "DEBUG_3", "ADDING BITWISE AXIOMS TO THE",
            "LAST GROUP: ", bitwiseAxioms);
        formulas.set(formulas.size()-1, makeAnd(formulas.get(formulas.size()-1), bitwiseAxioms));
      }
    }
  }
  
  // returns a formula with some "static learning" about some bitwise
  // operations, so that they are (a bit) "less uninterpreted"
  // Currently it add's the following formulas for each number literal n that
  // appears in the formula: "(n & 0 == 0) and (0 & n == 0)"
  // But only if an bitwise "and" occurs in the formula. 
  public MathsatSymbolicFormula getBitwiseAxioms(SymbolicFormula f) {
    Deque<Long> toProcess = new ArrayDeque<Long>();
    Set<Long> seen = new HashSet<Long>();
    Set<Long> allLiterals = new HashSet<Long>();

    boolean andFound = false;

    toProcess.add(((MathsatSymbolicFormula)f).getTerm());
    while (!toProcess.isEmpty()) {
      long t = toProcess.pollLast();

      if (mathsat.api.msat_term_is_number(t) != 0) {
        allLiterals.add(t);
      }
      if (mathsat.api.msat_term_is_uif(t) != 0) {
        String r = mathsat.api.msat_term_repr(t);
        if (r.startsWith("_&_")) {
          andFound = true;
        }
      }
      for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
        long c = mathsat.api.msat_term_get_arg(t, i);
        if (seen.add(c)) {
          // was not already contained in seen
          toProcess.add(c);
        }
      }
    }

    long result = mathsat.api.msat_make_true(msatEnv);
    if (andFound) {
      long z = mathsat.api.msat_make_number(msatEnv, "0");
      for (long n : allLiterals) {
        long u1 = mathsat.api.msat_make_uif(msatEnv, bitwiseAndUfDecl, new long[]{n, z});
        long u2 = mathsat.api.msat_make_uif(msatEnv, bitwiseAndUfDecl, new long[]{z, n});
        long e1 = mathsat.api.msat_make_equal(msatEnv, u1, z);
        long e2 = mathsat.api.msat_make_equal(msatEnv, u2, z);
        long a = mathsat.api.msat_make_and(msatEnv, e1, e2);
        result = mathsat.api.msat_make_and(msatEnv, result, a);
      }
    }
    return new MathsatSymbolicFormula(result);
  }

  private long uninstantiate(long term) {
    Map<Long, Long> cache = uninstantiateCache;
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
    assert(cache.containsKey(term));
    return cache.get(term);
  }

  /**
   * Given an "instantiated" formula, returns the corresponding formula in
   * which all the variables are "generic" ones. This is the inverse of the
   * instantiate() method above
   */
  protected MathsatSymbolicFormula uninstantiate(SymbolicFormula f) {
    return new MathsatSymbolicFormula(uninstantiate(((MathsatSymbolicFormula)f).getTerm()));
  }

  @Override
  public SymbolicFormula[] getInstantiatedAt(SymbolicFormula[] args,
      SSAMap ssa, Map<SymbolicFormula, SymbolicFormula> cache) {
    Stack<Long> toProcess = new Stack<Long>();
    SymbolicFormula[] ret = new SymbolicFormula[args.length];
    for (SymbolicFormula f : args) {
        toProcess.push(((MathsatSymbolicFormula)f).getTerm());
    }
  
    while (!toProcess.empty()) {
        long t = toProcess.peek();
        SymbolicFormula tt = new MathsatSymbolicFormula(t);
        if (cache.containsKey(tt)) {
            toProcess.pop();
            continue;
        }
        if (mathsat.api.msat_term_is_variable(t) != 0) {
            toProcess.pop();
            String name = mathsat.api.msat_term_repr(t);
            assert(ssa.getIndex(name) > 0);
            cache.put(tt, instantiate(
                    new MathsatSymbolicFormula(t), ssa));
        } else if (mathsat.api.msat_term_is_uif(t) != 0) {
            long d = mathsat.api.msat_term_get_decl(t);
            String name = mathsat.api.msat_decl_get_name(d);
            if (ufCanBeLvalue(name)) {
                SymbolicFormula[] cc =
                    new SymbolicFormula[mathsat.api.msat_term_arity(t)];
                boolean childrenDone = true;
                for (int i = 0; i < cc.length; ++i) {
                    long c = mathsat.api.msat_term_get_arg(t, i);
                    SymbolicFormula f = new MathsatSymbolicFormula(c);
                    if (cache.containsKey(f)) {
                        cc[i] = cache.get(f);
                    } else {
                        toProcess.push(c);
                        childrenDone = false;
                    }
                }
                if (childrenDone) {
                    toProcess.pop();
                    if (ssa.getIndex(name, cc) < 0) {
                        ssa.setIndex(name, cc, 1);
                    }
                    cache.put(tt, instantiate(tt, ssa));
                }
            } else {
                toProcess.pop();
                cache.put(tt, tt);
            }
        } else {
            toProcess.pop();
            cache.put(tt, tt);
        }
    }
    for (int i = 0; i < ret.length; ++i) {
        assert(cache.containsKey(args[i]));
        ret[i] = cache.get(args[i]);
    }
    return ret;
  }
  
  private static final Comparator<Long> MathsatComparator = new Comparator<Long>() {
    @Override
    public int compare(Long o1, Long o2) {
      return mathsat.api.msat_term_id(o1) - mathsat.api.msat_term_id(o2);
    }
  };

  @Override
  public Collection<SymbolicFormula> extractAtoms(SymbolicFormula f,
      boolean uninst, boolean splitArithEqualities,
      boolean conjunctionsOnly) {
    Set<Long> cache = new HashSet<Long>();
    //Set<Long> atoms = new HashSet<Long>();
    Set<Long> atoms = new TreeSet<Long>(MathsatComparator);

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
          term = uninstantiate(term);
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
            term = uninstantiate(term);
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

    ArrayList<SymbolicFormula> ret = new ArrayList<SymbolicFormula>(atoms.size());
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

  /**
   * The path formulas created by this class have an uninterpreted function :=
   * where an assignment should be. This method replaces all those appearances
   * by equalities (which is a valid representation of an assignment for a SSA
   * formula).
   */
  @Override
  public MathsatSymbolicFormula replaceAssignments(SymbolicFormula f) {
    Stack<Long> toProcess = new Stack<Long>();
    Map<Long, Long> cache = replaceAssignmentsCache;

    long term = ((MathsatSymbolicFormula)f).getTerm();
    toProcess.push(term);
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
          if (isAssignment(t)) {
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
    assert(cache.containsKey(term));
    return new MathsatSymbolicFormula(cache.get(term));
  }

  /**
   * returns an SSA map for the instantiated formula f
   */
  @Override
  public SSAMap extractSSA(SymbolicFormula f) {
    SSAMap ssa = new SSAMap();
    Stack<Long> toProcess = new Stack<Long>();
    Set<Long> cache = new HashSet<Long>();

    toProcess.push(((MathsatSymbolicFormula)f).getTerm());
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

  @Override
  public void collectVarNames(SymbolicFormula term, Set<String> vars,
                              Set<Pair<String, SymbolicFormula[]>> lvals) {

    Deque<Long> toProcess = new ArrayDeque<Long>();
    toProcess.push(((MathsatSymbolicFormula)term).getTerm());
    // TODO - this assumes the term is small! There is no memoizing yet!!
    while (!toProcess.isEmpty()) {
        long t = toProcess.pop();
        if (mathsat.api.msat_term_is_variable(t) != 0) {
            vars.add(mathsat.api.msat_term_repr(t));
        } else {
            for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
                toProcess.push(mathsat.api.msat_term_get_arg(t, i));
            }
            if (mathsat.api.msat_term_is_uif(t) != 0) {
                long d = mathsat.api.msat_term_get_decl(t);
                String name = mathsat.api.msat_decl_get_name(d);
                if (ufCanBeLvalue(name)) {
                    int n = mathsat.api.msat_term_arity(t);
                    SymbolicFormula[] a = new SymbolicFormula[n];
                    for (int i = 0; i < n; ++i) {
                        a[i] = new MathsatSymbolicFormula(
                                mathsat.api.msat_term_get_arg(t, i));
                    }
                    lvals.add(new Pair<String, SymbolicFormula[]>(name, a));
                }
            }
        }
    }
  }
  
  @Override
  public SymbolicFormulaManager.AllSatCallback getAllSatCallback(FormulaManager mgr, AbstractFormulaManager amgr) {
    return new AllSatCallback(mgr, amgr, logger);
  }
  
  /**
   * callback used to build the predicate abstraction of a formula
   * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
   */
  private static class AllSatCallback implements TheoremProver.AllSatCallback, SymbolicFormulaManager.AllSatCallback {
      private final FormulaManager mgr;
      private final AbstractFormulaManager amgr;
      private final LogManager logger;
      
      private long totalTime = 0;
      private int count = 0;

      private AbstractFormula formula;
      private final Deque<AbstractFormula> cubes = new ArrayDeque<AbstractFormula>();

      private AllSatCallback(FormulaManager mgr, AbstractFormulaManager amgr, LogManager logger) {
          this.mgr = mgr;
          this.amgr = amgr;
          this.logger = logger;
          this.formula = amgr.makeFalse();
      }

      @Override
      public long getTotalTime() {
          return totalTime;
      }

      @Override
      public int getCount() {
        return count;
      }

      @Override
      public AbstractFormula getResult() {
          if (cubes.size() > 0) {
              buildBalancedOr();
          }
          return formula;
      }

      private void buildBalancedOr() {
          cubes.add(formula);
          while (cubes.size() > 1) {
              AbstractFormula b1 = cubes.remove();
              AbstractFormula b2 = cubes.remove();
              cubes.add(amgr.makeOr(b1, b2));
          }
          assert(cubes.size() == 1);
          formula = cubes.remove();
      }

      @Override
      public void modelFound(List<SymbolicFormula> model) {
          logger.log(Level.ALL, "Allsat found model", model);
          long start = System.currentTimeMillis();

          // the abstraction is created simply by taking the disjunction
          // of all the models found by msat_all_sat, and storing them
          // in a BDD
          // first, let's create the BDD corresponding to the model
          Deque<AbstractFormula> curCube = new ArrayDeque<AbstractFormula>();
          AbstractFormula m = amgr.makeTrue();
          for (SymbolicFormula f : model) {
              long t = ((MathsatSymbolicFormula)f).getTerm();

              AbstractFormula v;
              if (mathsat.api.msat_term_is_not(t) != 0) {
                  t = mathsat.api.msat_term_get_arg(t, 0);
                  v = mgr.getPredicate(new MathsatSymbolicFormula(t)).getFormula();
                  v = amgr.makeNot(v);
              } else {
                v = mgr.getPredicate(f).getFormula();
              }
              curCube.add(v);
          }
          // now, add the model to the bdd
          curCube.add(m);
          while (curCube.size() > 1) {
              AbstractFormula v1 = curCube.remove();
              AbstractFormula v2 = curCube.remove();
              curCube.add(amgr.makeAnd(v1, v2));
          }
          assert(curCube.size() == 1);
          m = curCube.remove();
          cubes.add(m);

          count++;

          long end = System.currentTimeMillis();
          totalTime += (end - start);
      }
  }
}