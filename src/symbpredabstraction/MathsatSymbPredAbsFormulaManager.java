package symbpredabstraction;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import logging.LazyLogger;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import cmdline.CPAMain;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.BlockEdge;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cfa.objectmodel.c.StatementEdge;

import exceptions.UnrecognizedCFAEdgeException;


/**
 * A SymbolicFormulaManager to deal with MathSAT formulas.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */ 
public class MathsatSymbPredAbsFormulaManager implements SymbolicFormulaManager {
    
    // provides information about the type of variables etc.
    // TODO - This is not used at the moment
    class DeclarationInfo {
        // a simple type is one that can be treated as an integer in mathsat
        static final int TP_SIMPLE = 0; 
        // this is a C struct
        static final int TP_COMPOSITE = 1;
        // this is a C array - unused for now        
        static final int TP_ARRAY = 2;
        // an enumerated type - differs from a simple one because it has a
        // range of possible values. not used yet
        static final int TP_ENUM = 3; 
        
        int type; // the type of this info, one of the above
        String name; // the name of the declared variable. 
                     // Can be null for array fields
        int starsOrRange; // for ENUMs, the range, otherwise the 
                          // number of pointer operators ('*') in this 
                          // declaration
        List<DeclarationInfo> fields; // for COMPOSITE/ARRAY types
        
        DeclarationInfo(int type, String name) {
            this.type = type;
            this.name = name;
            if (type == TP_COMPOSITE || type == TP_ARRAY) {
                fields = new Vector<DeclarationInfo>();
            } else {
                fields = null;
            }
            starsOrRange = 0;
        }
        
        DeclarationInfo(int type) { 
            this(type, null);
        }
         
        void addField(DeclarationInfo f) { fields.add(f); }        
        List<DeclarationInfo> getFields() { return fields; }
        
        String getName() { return name; }
        void setName(String n) { name = n; }
        
        int getType() { return type; }
        
        int getNumStars() { return starsOrRange; }
        void setNumStars(int n) { starsOrRange = n; }
        
        int getEnumRange() { return starsOrRange; }
        void setEnumRange(int n) { starsOrRange = n; }
    }

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
    private long bitwiseNotUfDecl;
    private long leftShiftUfDecl;
    private long rightShiftUfDecl;
    
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

    // cache for checking entailement. Can be disabled
    // TODO enable later
//    private boolean entailsUseCache;
//    private Map<Pair<SymbolicFormula, SymbolicFormula>, Boolean> entailsCache;

    // cache for uninstantiating terms (see uninstantiate() below)
    // TODO enable later
    //protected Map<Long, Long> uninstantiateGlobalCache;


    public MathsatSymbPredAbsFormulaManager() {
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
        bitwiseNotUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_~_",
                msatVarType, 1, new int[]{msatVarType});
        leftShiftUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_<<_",
                msatVarType, 2, argtypes);
        rightShiftUfDecl = mathsat.api.msat_declare_uif(msatEnv, "_>>_",
                msatVarType, 2, argtypes);
        
        // TODO enable later
//        entailsUseCache = CPAMain.cpaConfig.getBooleanValue(
//                "cpas.symbpredabs.mathsat.useCache");
//        if (entailsUseCache) {
//            entailsCache = new HashMap<Pair<SymbolicFormula, SymbolicFormula>,
//                                       Boolean>();
//        }
//        uninstantiateGlobalCache = new HashMap<Long, Long>();
    }

    public long getMsatEnv() {
        return msatEnv;
    }
    
    // TODO check
    public boolean equals(SymbolicFormula f1, SymbolicFormula f2) {
        
        // TODO enable later
//       	Pair<SymbolicFormula, SymbolicFormula> key = null;
//           if (entailsUseCache) {
//               key = new Pair<SymbolicFormula, SymbolicFormula>(f1, f2);
//               if (entailsCache.containsKey(key)) {
//                   return entailsCache.get(key);
//               }
//           }
           
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
           long imp1 = mathsat.api.msat_make_implies(env, t1, t2);
           long imp2 = mathsat.api.msat_make_implies(env, t2, t1);
           
           mathsat.api.msat_assert_formula(env, 
                   mathsat.api.msat_make_not(env, imp1));
           int res1 = mathsat.api.msat_solve(env);

           mathsat.api.msat_assert_formula(env, 
                   mathsat.api.msat_make_not(env, imp2));
           int res2 = mathsat.api.msat_solve(env);
           
           mathsat.api.msat_destroy_env(env);

           boolean ret1 = (res1 == mathsat.api.MSAT_UNSAT);
           boolean ret2 = (res2 == mathsat.api.MSAT_UNSAT);
           
        // TODO enable later
//           if (entailsUseCache) {
//               assert(key != null);
//               entailsCache.put(key, ret);
//           }
           
           return ret1 & ret2;
       }
    
    public boolean entails(SymbolicFormula f1, SymbolicFormula f2) {
        
        // TODO enable later
//       	Pair<SymbolicFormula, SymbolicFormula> key = null;
//           if (entailsUseCache) {
//               key = new Pair<SymbolicFormula, SymbolicFormula>(f1, f2);
//               if (entailsCache.containsKey(key)) {
//                   return entailsCache.get(key);
//               }
//           }
           
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
           
        // TODO enable later
//           if (entailsUseCache) {
//               assert(key != null);
//               entailsCache.put(key, ret);
//           }
           
           return ret;
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

    
    public SymbolicFormula makeTrue() {
        return new MathsatSymbolicFormula(mathsat.api.msat_make_true(msatEnv));
    }

    
    public PathFormula makeAnd(
            SymbolicFormula f1, CFAEdge edge, SSAMap ssa, 
            boolean updateSSA, boolean absoluteSSAIndices) 
            throws UnrecognizedCFAEdgeException {

        if (edge instanceof BlockEdge) {
            BlockEdge block = (BlockEdge)edge;
            PathFormula ret = null;
            for (CFAEdge e : block.getEdges()) {
                ret = makeAnd(f1, e, ssa, updateSSA, absoluteSSAIndices);
                f1 = ret.getSymbolicFormula();
                ssa = ret.getSsa();
            }
            assert(ret != null);
            return ret;
        }

        MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
 
        setNamespace(edge.getPredecessor().getFunctionName());
        
        if (isStartOfFunction(edge)) {
        	PathFormula p = makeAndEnterFunction(
                    m1, edge.getPredecessor(), ssa, updateSSA, 
                    absoluteSSAIndices);
            m1 = (MathsatSymbolicFormula)p.getSymbolicFormula();
            f1 = m1;
            ssa = p.getSsa();
        }

        switch (edge.getEdgeType ()) {
        case StatementEdge: {
            StatementEdge statementEdge = (StatementEdge)edge;

            if (statementEdge.isJumpEdge()) {
                if (statementEdge.getSuccessor().getFunctionName().equals(
                        "main")) {
                    LazyLogger.log(LazyLogger.DEBUG_3, 
                            "MathsatSymbolicFormulaManager, IGNORING return ",
                            "from main: ", edge.getRawStatement());
                } else {
                    return makeAndReturn(m1, statementEdge, ssa, updateSSA,
                                         absoluteSSAIndices);
                }
            } else {
                return makeAndStatement(m1, statementEdge, ssa, updateSSA, 
                        absoluteSSAIndices);
            }
            break;
        }

        case DeclarationEdge: {
            // at each declaration, we instantiate the variable in the SSA: 
            // this is to avoid problems with uninitialized variables
            SSAMap newssa = ssa;
            if (!updateSSA) {
                newssa = new SSAMap();
                newssa.copyFrom(ssa);
            }
            IASTDeclarator[] decls = 
                ((DeclarationEdge)edge).getDeclarators();
            IASTDeclSpecifier spec = ((DeclarationEdge)edge).getDeclSpecifier();
            
            if (!(spec instanceof IASTSimpleDeclSpecifier)) {
                throw new UnrecognizedCFAEdgeException(
                        "UNSUPPORTED SPECIFIER FOR DECLARATION: " + 
                        edge.getRawStatement());
            }
            
            boolean isGlobal = edge instanceof GlobalDeclarationEdge;
            for (IASTDeclarator d : decls) {
                String var = d.getName().getRawSignature();
                if (isGlobal) {
                    globalVars.add(var);
                }
                var = scoped(var);
                int idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 1;
                newssa.setIndex(var, idx);

                LazyLogger.log(LazyLogger.DEBUG_3, 
                        "Declared variable: ", var, ", index: ", idx);
                // TODO get the type of the variable, and act accordingly
                
                // if the var is unsigned, add the constraint that it should
                // be > 0
//                if (((IASTSimpleDeclSpecifier)spec).isUnsigned()) {
//                    long z = mathsat.api.msat_make_number(msatEnv, "0");
//                    long mvar = buildMsatVariable(var, idx);
//                    long t = mathsat.api.msat_make_gt(msatEnv, mvar, z);
//                    t = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), t);
//                    m1 = new MathsatSymbolicFormula(t);
//                }
                
                // if there is an initializer associated to this variable,
                // take it into account
                if (d.getInitializer() != null) {
                    IASTInitializer init = d.getInitializer();
                    if (!(init instanceof IASTInitializerExpression)) {
                        throw new UnrecognizedCFAEdgeException(
                                "BAD INITIALIZER: " + edge.getRawStatement());
                    }
                    IASTExpression exp = 
                        ((IASTInitializerExpression)init).getExpression();
                    long minit = buildMsatTerm(exp, newssa, absoluteSSAIndices);
                    long mvar = buildMsatVariable(var, idx);
                    long t = makeAssignment(mvar, minit);
                    t = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), t);
                    m1 = new MathsatSymbolicFormula(t);
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
                        LazyLogger.log(LazyLogger.DEBUG_3, "AUTO-INITIALIZING ",
                                (isGlobal ? "GLOBAL" : ""), "VAR: ",
                                var, " (", d.getName().getRawSignature(), ")");
                    } else {
                        LazyLogger.log(LazyLogger.DEBUG_3, 
                                "NOT AUTO-INITIALIZING VAR: ", var);
                    }
                }
            }
            return new PathFormula(m1, newssa);
        }

        case AssumeEdge: {
            AssumeEdge assumeEdge = (AssumeEdge)edge;
            return makeAndAssume(m1, assumeEdge, ssa, absoluteSSAIndices);
        }

        case BlankEdge: {
            break;
        }

        case FunctionCallEdge: {
            if (!updateSSA) {
                SSAMap newssa = new SSAMap();
                newssa.copyFrom(ssa);
                ssa = newssa;
            }
            return makeAndFunctionCall(m1, (FunctionCallEdge)edge, ssa,
                    absoluteSSAIndices);
        }

        case ReturnEdge: {
            // get the expression from the summary edge
            CFANode succ = edge.getSuccessor();
            CallToReturnEdge ce = succ.getEnteringSummaryEdge();
            PathFormula ret = makeAndExitFunction(m1, ce, ssa, updateSSA, absoluteSSAIndices);
            //popNamespace(); - done inside makeAndExitFunction
            return ret;
        }

        case MultiStatementEdge: {
            throw new UnrecognizedCFAEdgeException("MULTI STATEMENT: " + 
                    edge.getRawStatement());
        }

        case MultiDeclarationEdge: {
            break;
        }
        }

        return new PathFormula(f1, ssa);
    }
    
    private PathFormula makeAndEnterFunction(
            MathsatSymbolicFormula m1, CFANode pred, SSAMap ssa,
            boolean updateSSA, boolean absoluteSSAIndices) 
          throws UnrecognizedCFAEdgeException {
        FunctionDefinitionNode fn = (FunctionDefinitionNode)pred;
        List<IASTParameterDeclaration> params = fn.getFunctionParameters();
        if (params.isEmpty()) {
            return new PathFormula(m1, ssa);
        }
        if (!updateSSA) {
            SSAMap newssa = new SSAMap();
            newssa.copyFrom(ssa);
            ssa = newssa;
        }
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
                throw new UnrecognizedCFAEdgeException("SORRY, POINTERS " +
                        "NOT HANDLED: " + 
                        fn.getFunctionDefinition().getRawSignature());
            } else {
                String formalParamName = 
                    scoped(param.getDeclarator().getName().toString());
                idx = ssa.getIndex(formalParamName);
                if (idx < 0 || absoluteSSAIndices) {
                    idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 1;
                } else {
                    //idx += 1;
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
        }
        term = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), term);
        return new PathFormula(
                new MathsatSymbolicFormula(term), ssa);
    }

    protected boolean isStartOfFunction(CFAEdge edge) {
        return edge.getPredecessor() instanceof FunctionDefinitionNode;
    }

    private PathFormula makeAndExitFunction(
            MathsatSymbolicFormula m1, CallToReturnEdge ce, SSAMap ssa,
            boolean updateSSA, boolean absoluteSSAIndices) 
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
            //assert(ssa.getIndex(retvar) < 0);
            //popNamespace();
            if (!updateSSA) {
                SSAMap newssa = new SSAMap();
                newssa.copyFrom(ssa);
                ssa = newssa;
            }
            int retidx = ssa.getIndex(retvar);
            if (retidx < 0) {
                retidx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 1;
            }
            ssa.setIndex(retvar, retidx);
            long msatretvar = buildMsatVariable(retvar, retidx);
            IASTExpression e = exp.getOperand1();
            // TODO - we assume this is an assignment to a plain variable. If
            // we want to handle structs, this might not be the case anymore...
            assert(e instanceof IASTIdExpression);
            setNamespace(ce.getSuccessor().getFunctionName());
            String outvar = ((IASTIdExpression)e).getName().getRawSignature();
            outvar = scoped(outvar);
            int idx = ssa.getIndex(outvar);
            if (idx < 0) {
                idx = autoInstantiateVar(outvar, ssa);
                if (idx == 1) {
                    ++idx;
                    ssa.setIndex(outvar, idx);
                }
            } else {
                //idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : idx+1;
                idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 
                      getNewIndex(outvar, ssa);
                ssa.setIndex(outvar, idx);
            }
            long msatoutvar = buildMsatVariable(outvar, idx);
            long term = makeAssignment(msatoutvar, msatretvar);
            term = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), term);
            return new PathFormula(new MathsatSymbolicFormula(term), ssa);
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
                    throw new UnrecognizedCFAEdgeException("SORRY, POINTERS " +
                            "NOT HANDLED: " + edge.getRawStatement());
                } else {
                    String paramName = scoped(FUNCTION_PARAM_NAME + (i-1));
                    int idx = ssa.getIndex(paramName);
                    if (idx < 0 || absoluteSSAIndices) {
                        idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 1;
                    } else {
                        //idx += 1;
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
            }
            assert(!mathsat.api.MSAT_ERROR_TERM(term));
            term = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), term);
            return new PathFormula(new MathsatSymbolicFormula(term), ssa);
        }
    }

    private PathFormula makeAndReturn(
            MathsatSymbolicFormula m1, StatementEdge edge, SSAMap ssa,
            boolean updateSSA, boolean absoluteSSAIndices) 
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
            assert(ssa.getIndex(retvalname) < 0);
            int idx = absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 2;
            if (idx == 1) ++idx;
            if (!updateSSA) {
                SSAMap ssa2 = new SSAMap();
                for (String var : ssa.allVariables()) {
                    ssa2.setIndex(var, ssa.getIndex(var));
                }
                ssa = ssa2;
            }
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
        throw new UnrecognizedCFAEdgeException("UNRECOGNIZED: " +
                edge.getRawStatement());
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

    private int autoInstantiateVar(String var, SSAMap ssa) {
        LazyLogger.log(LazyLogger.DEBUG_3, 
                       "WARNING: Auto-instantiating variable: ", var);
        ssa.setIndex(var, 1);
        return 1;
    }

    private long buildMsatTerm(IASTExpression exp, SSAMap ssa, 
            boolean absoluteSSAIndices) {
        if (exp instanceof IASTIdExpression) {
            // this is a variable: get the right index for the SSA
            String var = ((IASTIdExpression)exp).getName().getRawSignature();
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
            default:
                LazyLogger.log(LazyLogger.DEBUG_1, 
                        "ERROR, CAN'T HANDLE STRINGS OR CHARS: ",
                        exp.getRawSignature());
                return mathsat.api.MSAT_MAKE_ERROR_TERM();
            }
            // TODO here we assume 32 bit integers!!! This is because CIL
            // seems to do so as well...
            try {
                Integer.parseInt(num);
            } catch (NumberFormatException nfe) {
                long l = Long.parseLong(num);
                if (l < 0) {
                    num = Long.toString((long)Integer.MAX_VALUE + l);
                } else {
                    num = Long.toString(l - ((long)Integer.MAX_VALUE + 1)*2);
                }
            }
            return mathsat.api.msat_make_number(msatEnv, num);
        } else if (exp instanceof IASTCastExpression) {
            // we completely ignore type casts
            LazyLogger.log(LazyLogger.DEBUG_3, "IGNORING TYPE CAST: ",
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
                String var = operand.getRawSignature();
                var = scoped(var);
                // get the latest "SSA instance" of the variable for reading
                int idx = ssa.getIndex(var);
                if (idx <= 0) {
                    if (absoluteSSAIndices) {
                        return mathsat.api.MSAT_MAKE_ERROR_TERM();
                    } else {
                        idx = autoInstantiateVar(var, ssa);
                    }
                }
                long mvar = buildMsatVariable(var, idx);
                if (mathsat.api.MSAT_ERROR_TERM(mvar)) return mvar;
                // create a new "SSA instance" of the variable with 
                // the new value
                //int newidx = idx+1;
                int oldidx = ssa.getIndex(var);
                int newidx;
                if (oldidx > 0) {
                    newidx = getNewIndex(var, ssa);
                    if (absoluteSSAIndices) {
                        newidx = SSAMap.getNextSSAIndex();
                    }
                } else {
                    newidx = 2; // AG - IMPORTANT! See below
                }
                ssa.setIndex(var, newidx);
                long newvar = buildMsatVariable(var, newidx);
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

            default: {
                // this might be a predicate implicitly cast to an int. Let's
                // see if this is indeed the case...
                MathsatSymbolicFormula ftmp = buildFormulaPredicate(
                        exp, true, ssa, absoluteSSAIndices);
                if (ftmp == null) {
                    return mathsat.api.MSAT_MAKE_ERROR_TERM();
                } else {
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
            case IASTBinaryExpression.op_binaryAndAssign:
            case IASTBinaryExpression.op_binaryOrAssign:
            case IASTBinaryExpression.op_binaryXorAssign:
            case IASTBinaryExpression.op_shiftLeftAssign:
            case IASTBinaryExpression.op_shiftRightAssign: {
                long me2 = buildMsatTerm(e2, ssa, absoluteSSAIndices);
                if (mathsat.api.MSAT_ERROR_TERM(me2)) return me2;
                // create a new "SSA instance" for the variable being assigned
                if (!(e1 instanceof IASTIdExpression)) {
                    // should not happen...
                    return mathsat.api.MSAT_MAKE_ERROR_TERM();
                }
                String var = e1.getRawSignature();
                var = scoped(var);
                if (op != IASTBinaryExpression.op_assign) {
                    // in this case, we have to get the old SSA instance for
                    // reading the value of the variable, and build the 
                    // corresponding expression
                    int oldidx = ssa.getIndex(var);
                    if (oldidx <= 0) {
                        if (absoluteSSAIndices) {
                            return mathsat.api.MSAT_MAKE_ERROR_TERM();
                        } else {
                            oldidx = autoInstantiateVar(var, ssa);
                        }
                    }
                    long oldvar = buildMsatVariable(var, oldidx);
                    if (mathsat.api.MSAT_ERROR_TERM(oldvar)) return oldvar;
                    switch (op) {
                    case IASTBinaryExpression.op_plusAssign:
                        me2 = mathsat.api.msat_make_plus(msatEnv, oldvar, me2);
                        break;
                    case IASTBinaryExpression.op_minusAssign:
                        me2 = mathsat.api.msat_make_minus(msatEnv, oldvar, me2);
                        break;
                    case IASTBinaryExpression.op_multiplyAssign: 
                        me2 = mathsat.api.msat_make_times(msatEnv, oldvar, me2);
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
                            me2 = mathsat.api.MSAT_MAKE_ERROR_TERM();
                        }
                        break;
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
                int idx = -1;
                if (absoluteSSAIndices) {
                    idx = SSAMap.getNextSSAIndex();
                } else {
                    int oldidx = ssa.getIndex(var);
                    if (oldidx > 0) {
                        //idx = oldidx + 1;
                        idx = getNewIndex(var, ssa);
                    } else {
                        idx = 2; // AG - IMPORTANT!!! We must start from 2 and 
                                 // not from 1, because this is an assignment,
                                 // so the SSA index must be fresh. If we use 1
                                 // here, we will have troubles later when
                                 // shifting indices
                        //else idx = 1;
                    }
                }
                ssa.setIndex(var, idx);
                long mvar = buildMsatVariable(var, idx);
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
                    return mathsat.api.msat_make_times(msatEnv, me1, me2);
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
                        me2 = mathsat.api.MSAT_MAKE_ERROR_TERM();
                    }
                    return me2;
                }
                break;
            }
            
            case IASTBinaryExpression.op_binaryAnd:
            case IASTBinaryExpression.op_binaryOr:
            case IASTBinaryExpression.op_binaryXor:
            case IASTBinaryExpression.op_shiftLeft:
            case IASTBinaryExpression.op_shiftRight: {
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
                    return mathsat.api.msat_make_ite(msatEnv, ftmp.getTerm(),
                            mathsat.api.msat_make_number(msatEnv, "1"),
                            mathsat.api.msat_make_number(msatEnv, "0"));
                }                
                //return mathsat.api.MSAT_MAKE_ERROR_TERM();
            }
        }
        // unknown expression, caller will raise exception
        return mathsat.api.MSAT_MAKE_ERROR_TERM();
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
        LazyLogger.log(LazyLogger.DEBUG_3, 
                "MAKE ASSIGNMENT: ", new MathsatSymbolicFormula(t1), " := ",
                new MathsatSymbolicFormula(t2));
        return mathsat.api.msat_make_uif(msatEnv, assignUfDecl,
                                         new long[]{t1, t2});
    }

    private PathFormula makeAndStatement(
            MathsatSymbolicFormula f1, StatementEdge stmt, SSAMap ssa, 
            boolean updateSSA, boolean absoluteSSAIndices) 
            throws UnrecognizedCFAEdgeException {
        IASTExpression expr = stmt.getExpression();
        if (!updateSSA && needsSSAUpdate(expr)) {
            SSAMap ssa2 = new SSAMap();
            for (String key : ssa.allVariables()) {
                ssa2.setIndex(key, ssa.getIndex(key));
            }
            ssa = ssa2;
        }
        long f2 = buildMsatTerm(expr, ssa, absoluteSSAIndices);

        if (!mathsat.api.MSAT_ERROR_TERM(f2)) {
            long a = mathsat.api.msat_make_and(msatEnv, f1.getTerm(), f2);
            return new PathFormula(
                    new MathsatSymbolicFormula(a), ssa);
        } else {
            throw new UnrecognizedCFAEdgeException("STATEMENT: " +
                    stmt.getRawStatement());
        }
    }

    private MathsatSymbolicFormula buildFormulaPredicate(
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

    protected int getNewIndex(String var, SSAMap ssa) {
        int idx = ssa.getIndex(var);
        if (idx > 0) return idx+1;
        else return 1;
    }
    
    protected int getNewIndex(String var, int i1, int i2) {
        return Math.max(i1, i2) + 1;
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

        Pair<SymbolicFormula, SymbolicFormula> sp = 
            new Pair<SymbolicFormula, SymbolicFormula>(
                    new MathsatSymbolicFormula(mt1),
                    new MathsatSymbolicFormula(mt2));
        return new Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap>(
                sp, result);
    }

    // ssa can be null. In this case, all the variables are instantiated 
    // at index 1    
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
            if (termIsAssignment(t)) {
                // treat assignments specially. When we shift, we always have to
                // update the SSA index of the variable being assigned
                long var = mathsat.api.msat_term_get_arg(t, 0);
                if (!cache.containsKey(var)) {
                    String name = mathsat.api.msat_term_repr(var);

                    LazyLogger.log(LazyLogger.DEBUG_3, "SHIFTING ASSIGNMENT: ",
                            new MathsatSymbolicFormula(t), " VAR: ", name);

                    // check whether this is an instantiated variable
                    String[] bits = name.split("@");
                    int idx = -1;
                    assert(bits.length == 2);
                    try {
                        idx = Integer.parseInt(bits[1]);
                        name = bits[0];
                    } catch (NumberFormatException e) {
                        assert(false);
                    }
                    int ssaidx = ssa.getIndex(name);
                    if (ssaidx > 0) {
                        //if (idx == 1) ++idx;
                        assert(idx > 1); //TODO!!!
                        long newvar = buildMsatVariable(name, ssaidx + idx-1);
                        assert(!mathsat.api.MSAT_ERROR_TERM(newvar));
                        cache.put(var, newvar);
                        if (newssa.getIndex(name) < ssaidx + idx-1) {
                            newssa.setIndex(name, ssaidx + idx-1);
                        }
                    } else {
                        cache.put(var, var);
                        if (newssa.getIndex(name) < idx) {
                            newssa.setIndex(name, idx);
                        }
                    }
                    
                    LazyLogger.log(LazyLogger.DEBUG_3, "SHIFTING ASSIGNMENT, ",
                            "RESULT: ", //name, "@", newssa.getIndex(name));
                            mathsat.api.msat_term_repr(cache.get(var)));
                }
            } 
            
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
                        LazyLogger.log(LazyLogger.DEBUG_1, 
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
                        LazyLogger.log(LazyLogger.DEBUG_3, "SHIFTING VAR: ",
                                name, "@", ssaidx, ", ",
                                "RESULT: ", //name, "@", newssa.getIndex(name));
                                mathsat.api.msat_term_repr(newt));
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
                        newt = mathsat.api.msat_replace_args(
                                msatEnv, t, newargs);
                    }
                    assert(!mathsat.api.MSAT_ERROR_TERM(newt));
                    
                    LazyLogger.log(LazyLogger.DEBUG_4, "CACHING: ",
                            new MathsatSymbolicFormula(t),
                            " VAL: ", new MathsatSymbolicFormula(newt));
                    
                    cache.put(t, newt);
                }
            }
        }

        assert(cache.containsKey(term));
        return new PathFormula(new MathsatSymbolicFormula(cache.get(term)), newssa);
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
                LazyLogger.log(LazyLogger.DEBUG_3, "FOUND UIF IN FORMULA: ", f,
                        ", term is: ", new MathsatSymbolicFormula(t));
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
//                    // ok, found bitwise and, collect numbers
//                    Set<Long> s = null;
//                    if (!gs.containsKey(bitwiseAndUfDecl)) {
//                        s = new HashSet<Long>();
//                        gs.put(bitwiseAndUfDecl, s);
//                    }
//                    s = gs.get(bitwiseAndUfDecl);
//                    for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
//                        long c = mathsat.api.msat_term_get_arg(t, i);
//                        if (mathsat.api.msat_term_is_number(c) != 0) {
//                            s.add(c);
//                        }
//                    }
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
                assert(bits.length == 2);
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
                    cache.put(t, mathsat.api.msat_replace_args(
                            msatEnv, t, children));
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
//        Map<Long, Long> cache = new HashMap<Long, Long>();
//        return new MathsatSymbolicFormula(uninstantiate(f.getTerm(), cache));
        return uninstantiate(f, true);
    }
    
    public MathsatSymbolicFormula uninstantiate(MathsatSymbolicFormula f,
                                                boolean useGlobalCache) {
        Map<Long, Long> cache;
        cache = null;
        if (useGlobalCache) {
        	// TODO enable later
            //cache = uninstantiateGlobalCache;
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
        Set<Long> atoms = new HashSet<Long>();
        Map<Long, Long> varcache = new HashMap<Long, Long>();
        
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
        Map<Long, Long> cache = new HashMap<Long, Long>();
        
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

    //-------------------------------------------------------------------------
    // FROM HERE ON ONLY UNUSED STUFF AT THE MOMENT
    //-------------------------------------------------------------------------
    
    
    
    // some useful info...
//    IASTDeclSpecifier tp = param.getDeclSpecifier();
//    System.out.println("SPECIFIER: " + tp.getRawSignature());
//    System.out.println("CLASS: " + tp.getClass());
//    System.out.println("STRUCT?: " + (tp instanceof IASTCompositeTypeSpecifier));
//    System.out.println("PARAMETER: " +
//            param.getRawSignature() + ", class: " + 
//            param.getDeclarator().getClass().getName() + 
//            ", NAME: " + param.getDeclarator().getName().toString() +
//            ", BINDING: " + 
//            param.getDeclarator().getName().resolveBinding() +
//            ", COMPOSITE?: " +
//            (param.getDeclarator().getName().resolveBinding() instanceof ICompositeType)        
//    );
//    System.exit(1);
//    // TODO
    
    /*
    private DeclarationInfo[] getInfo(IASTDeclSpecifier specifier, 
            IASTDeclarator[] declarators) throws UnrecognizedCFAEdgeException {
        // first, extract the type information from the specifier
        IASTName name = null;
        DeclarationInfo base = null;
        if (specifier instanceof IASTNamedTypeSpecifier) {
            // this is a typedef
            name = ((IASTNamedTypeSpecifier)specifier).getName();
        } else if (specifier instanceof IASTElaboratedTypeSpecifier) {
            name = ((IASTElaboratedTypeSpecifier)specifier).getName();
        } else if (specifier instanceof IASTSimpleDeclSpecifier) {
            base = new DeclarationInfo(DeclarationInfo.TP_SIMPLE);
        } else {
            throw new UnrecognizedCFAEdgeException("UNRECOGNIZED SPECIFIER: " +
                    specifier.getRawSignature());
        }
        if (name != null) {
            IBinding binding = name.getBinding();
            assert(binding != null);
            try {
                base = getBindingInfo(binding);
            } catch (Exception e1) {
                throw new UnrecognizedCFAEdgeException(e1.getMessage());
            }
        }
        assert(base != null);
        // now we can build the information for all declared variables
        DeclarationInfo[] ret = new DeclarationInfo[declarators.length];
        for (int i = 0; i < declarators.length; ++i) {
            try {
                ret[i] = getDeclaratorInfo(base, declarators[i]);
            } catch (Exception e) {
                throw new UnrecognizedCFAEdgeException(e.getMessage());
            }
        }
        
        return ret;
    }
    */
    
    /*
    private DeclarationInfo getBindingInfo(IBinding binding) throws Exception {
        IType tp = null;
        int numPointers = 0;
        DeclarationInfo ret = null;
        String name = null;
        
        if (binding instanceof IVariable) {
            name = ((IVariable)binding).getName();
            tp = ((IVariable)binding).getType();
        } else if (binding instanceof ITypedef) {
            try {
                // TODO the whole typedef thing should probably be done
                // with a recursive function. But for now we'll live with 
                // this
                tp = ((ITypedef)binding).getType();
                if (tp instanceof IPointerType) {
                    while (tp instanceof IPointerType) {
                        numPointers++;
                        tp = ((IPointerType)tp).getType();
                    }
                }
            } catch (DOMException e) {
                e.printStackTrace();
                System.exit(0);
            }
        } else if (binding instanceof ICompositeType) {
            tp = (IType)binding;
        } else {
            throw new UnrecognizedCFAEdgeException(
                    "UNRECOGNIZED BINDING: " + binding.getName());
        }
        assert(tp != null);
        if (tp instanceof IBasicType) {
            // treat everything as an int
            ret = new DeclarationInfo(DeclarationInfo.TP_SIMPLE, name);
            ret.setNumStars(numPointers);
        } else if (tp instanceof ICompositeType) {
            ret = new DeclarationInfo(DeclarationInfo.TP_COMPOSITE, name);
            ret.setNumStars(numPointers);
            ICompositeType ctp = (ICompositeType)tp;
            try {
                for (IField f : ctp.getFields()) {
                    ret.addField(getBindingInfo(f));
                }
            } catch (DOMException e) {
                e.printStackTrace();
                System.exit(0);
            }
        } else {
            throw new UnrecognizedCFAEdgeException(
                    "UNRECOGNIZED TYPE: " + tp.toString());
        }            
        
        return ret;
    }
    */
    
    /*
    private DeclarationInfo getDeclaratorInfo(DeclarationInfo base, 
            IASTDeclarator decl) throws Exception {
        DeclarationInfo ret = new DeclarationInfo(base.getType());
        ret.setNumStars(base.getNumStars());
        ret.setName(decl.getName().toString());
        IASTPointerOperator[] ptr = decl.getPointerOperators();
        if (ptr != null) {
            ret.setNumStars(ret.getNumStars() + ptr.length);
        }
        return ret;
    }
    */
}
