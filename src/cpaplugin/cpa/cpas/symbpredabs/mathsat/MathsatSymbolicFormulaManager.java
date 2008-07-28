package cpaplugin.cpa.cpas.symbpredabs.mathsat;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.c.AssumeEdge;
import cpaplugin.cfa.objectmodel.c.DeclarationEdge;
import cpaplugin.cfa.objectmodel.c.StatementEdge;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.UnrecognizedCFAEdgeException;
import cpaplugin.cpa.cpas.symbpredabs.logging.LazyLogger;


public class MathsatSymbolicFormulaManager implements SymbolicFormulaManager {

    private long msatEnv;

    public MathsatSymbolicFormulaManager() {
        msatEnv = mathsat.api.msat_create_env();
    }

    public long getMsatEnv() {
        return msatEnv;
    }

    @Override
    public boolean entails(SymbolicFormula f1, SymbolicFormula f2) {
        MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
        MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

        // create a temporary environment for checking the implication
        long env = mathsat.api.msat_create_env();
        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_UF);
        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LRA);
        mathsat.api.msat_set_theory_combination(env, mathsat.api.MSAT_COMB_DTC);

        long t1 = mathsat.api.msat_make_copy_from(env, m1.getTerm(), msatEnv);
        long t2 = mathsat.api.msat_make_copy_from(env, m2.getTerm(), msatEnv);
        long imp = mathsat.api.msat_make_implies(env, t1, t2);
        mathsat.api.msat_assert_formula(env, 
                mathsat.api.msat_make_not(env, imp));
        int res = mathsat.api.msat_solve(env);

        mathsat.api.msat_destroy_env(env);

        return res == mathsat.api.MSAT_UNSAT;
    }

    @Override
    public SymbolicFormula makeAnd(SymbolicFormula f1, SymbolicFormula f2) {
        MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
        MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

        long a = mathsat.api.msat_make_and(msatEnv, m1.getTerm(), m2.getTerm());
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
    public SymbolicFormula makeTrue() {
        return new MathsatSymbolicFormula(mathsat.api.msat_make_true(msatEnv));
    }

    @Override
    public Pair<SymbolicFormula, SSAMap> makeAnd(
            SymbolicFormula f1, CFAEdge edge, SSAMap ssa, 
            boolean updateSSA, boolean absoluteSSAIndices) 
            throws UnrecognizedCFAEdgeException {
        // this is where the "meat" is... We have to parse the statement 
        // attached to the edge, and convert it to the appropriate formula

        MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;

        switch (edge.getEdgeType ()) {
        case StatementEdge: {
            StatementEdge statementEdge = (StatementEdge)edge;

            if (statementEdge.isJumpEdge()) {
                if (statementEdge.getSuccessor().getFunctionName().equals(
                "main")) {
                    LazyLogger.log(LazyLogger.DEBUG_1, 
                            "MathsatSymbolicFormulaManager, IGNORING return ",
                            "from main: ", edge.getRawStatement());                    
                } else {
                    throw new UnrecognizedCFAEdgeException(
                            "EXIT FROM FUNCTION: " + edge.getRawStatement());
                }
            } else {
                return makeAndStatement(m1, statementEdge, ssa, updateSSA, 
                        absoluteSSAIndices);
            }
            break;
        }

        case DeclarationEdge: {
            if (absoluteSSAIndices) {
                // at each declaration, we instantiate the variable in the SSA: 
                // this is to avoid problems with uninitialized variables
                SSAMap newssa = ssa;
                if (!updateSSA) {
                    newssa = new SSAMap();
                    for (String var : ssa.allVariables()) {
                        newssa.setIndex(var, ssa.getIndex(var));
                    }
                }
                IASTDeclarator[] decls = 
                    ((DeclarationEdge)edge).getDeclarators();
                for (IASTDeclarator d : decls) {
                    String var = d.getName().getRawSignature();
                    int idx = SSAMap.getNextSSAIndex();
                    newssa.setIndex(var, idx);

                    LazyLogger.log(LazyLogger.DEBUG_1, 
                            "Declared variable: ", var, ", index: ", idx);
                    // TODO get the type of the variable, and act accordingly
                }
                return new Pair<SymbolicFormula, SSAMap>(f1, newssa);
            }
            break;
        }

        case AssumeEdge: {
            AssumeEdge assumeEdge = (AssumeEdge)edge;
            return makeAndAssume(m1, assumeEdge, ssa, absoluteSSAIndices);
        }

        case BlankEdge: {
            break;
        }

        case FunctionCallEdge: {
            throw new UnrecognizedCFAEdgeException("FUNCTION CALL: " + 
                    edge.getRawStatement());

            //            FunctionCallEdge functionCallEdge = (FunctionCallEdge)edge;
            //            CallToReturnEdge summaryEdge = 
            //                edge.getPredecessor().getLeavingSummaryEdge();
            //
            //            if (functionCallEdge.isExternalCall()) {
            //                try {
            //
            //                } catch (Exception e) {
            //                    e.printStackTrace();
            //                }
            //                break;
            //            }
            //            try {
            //                
            //            } catch (Exception e) {
            //                e.printStackTrace();
            //            }
        }

        case ReturnEdge: {
            //            throw new UnrecognizedCFAEdgeException("RETURN: " + 
            //                    edge.getRawStatement());
            //            ReturnEdge exitEdge = (ReturnEdge)edge;
            LazyLogger.log(LazyLogger.DEBUG_1, 
                    "MathsatSymbolicFormulaManager, IGNORING return edge: ",
                    edge.getRawStatement());

            break;
        }

        case MultiStatementEdge: {
            throw new UnrecognizedCFAEdgeException("MULTI STATEMENT: " + 
                    edge.getRawStatement());
        }

        case MultiDeclarationEdge: {
            break;
        }
        }

        return new Pair<SymbolicFormula, SSAMap>(f1, ssa);
    }

    private long buildMsatVariable(String var, int idx) {
        long decl = mathsat.api.msat_declare_variable(
                msatEnv, var + "@" + Integer.toString(idx),
                mathsat.api.MSAT_REAL);
        return mathsat.api.msat_make_variable(msatEnv, decl);
    }

    private int autoInstantiateVar(String var, SSAMap ssa) {
        LazyLogger.log(LazyLogger.DEBUG_2, 
                       "WARNING: Auto-instantiating variable: ", var);
        ssa.setIndex(var, 1);
        return 1;
    }

    private long buildMsatTerm(IASTExpression exp, SSAMap ssa, 
            boolean absoluteSSAIndices) {
        if (exp instanceof IASTIdExpression) {
            // this is a variable: get the right index for the SSA
            String var = ((IASTIdExpression)exp).getName().getRawSignature();
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
            String num = ((IASTLiteralExpression)exp).getRawSignature();
            return mathsat.api.msat_make_number(msatEnv, num);
        } else if (exp instanceof IASTUnaryExpression) {
            IASTExpression operand = ((IASTUnaryExpression)exp).getOperand();
            int op = ((IASTUnaryExpression)exp).getOperator();
            switch (op) {
            case IASTUnaryExpression.op_postFixIncr:
            case IASTUnaryExpression.op_prefixIncr: 
            case IASTUnaryExpression.op_postFixDecr:
            case IASTUnaryExpression.op_prefixDecr: {
                String var = operand.getRawSignature();
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
                int newidx = idx+1;
                if (absoluteSSAIndices) {
                    newidx = SSAMap.getNextSSAIndex();
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
                return mathsat.api.msat_make_equal(msatEnv, newvar, me);
            }

            default:
                return mathsat.api.MSAT_MAKE_ERROR_TERM();
            }
        } else if (exp instanceof IASTBinaryExpression) {
            int op = ((IASTBinaryExpression)exp).getOperator();
            IASTExpression e1 = ((IASTBinaryExpression)exp).getOperand1();
            IASTExpression e2 = ((IASTBinaryExpression)exp).getOperand2();

            switch (op) {
            case IASTBinaryExpression.op_assign: 
            case IASTBinaryExpression.op_plusAssign:
            case IASTBinaryExpression.op_minusAssign:
            case IASTBinaryExpression.op_multiplyAssign: {
                long me2 = buildMsatTerm(e2, ssa, absoluteSSAIndices);
                if (mathsat.api.MSAT_ERROR_TERM(me2)) return me2;
                // create a new "SSA instance" for the variable being assigned
                if (!(e1 instanceof IASTIdExpression)) {
                    // should not happen...
                    return mathsat.api.MSAT_MAKE_ERROR_TERM();
                }
                String var = e1.getRawSignature();
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
                    }
                    if (mathsat.api.MSAT_ERROR_TERM(me2)) return me2;
                }
                int idx = -1;
                if (absoluteSSAIndices) {
                    idx = SSAMap.getNextSSAIndex();
                } else {
                    int oldidx = ssa.getIndex(var);
                    if (oldidx > 0) idx = oldidx + 1;
                    else idx = 1;
                }
                ssa.setIndex(var, idx);
                long mvar = buildMsatVariable(var, idx);
                if (mathsat.api.MSAT_ERROR_TERM(mvar)) return mvar;
                return mathsat.api.msat_make_equal(msatEnv, mvar, me2);
            }

            case IASTBinaryExpression.op_plus:
            case IASTBinaryExpression.op_minus:
            case IASTBinaryExpression.op_multiply: {
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
                }
                break;
            }

            default:
                return mathsat.api.MSAT_MAKE_ERROR_TERM();
            }
        }
        // unknown expression, caller will raise exception
        return mathsat.api.MSAT_MAKE_ERROR_TERM();
    }

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

    private Pair<SymbolicFormula, SSAMap> makeAndStatement(
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
            return new Pair<SymbolicFormula, SSAMap>(
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

            default:
                return null;
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
                // this is a negation (e.g. "! unaryExp")
                if (exp1 instanceof IASTUnaryExpression) {
                    IASTUnaryExpression unaryExp1 = ((IASTUnaryExpression)exp1);
                    // this is a parenthesized expression (e.g. "(exp)")
                    if (unaryExp1.getOperator() == 
                        IASTUnaryExpression.op_bracketedPrimary) {
                        IASTExpression exp2 = unaryExp1.getOperand();
                        // this is a binary parenthesized expression 
                        // (e.g. "(binaryExp)")
                        if (exp2 instanceof IASTBinaryExpression) {
                            IASTBinaryExpression binExp2 = 
                                (IASTBinaryExpression)exp2;
                            MathsatSymbolicFormula r = 
                                buildFormulaPredicate(binExp2, !isTrue, ssa, 
                                        absoluteSSAIndices);
                            if (r != null) {
                                long res = mathsat.api.msat_make_not(
                                        msatEnv, r.getTerm());
                                return new MathsatSymbolicFormula(res);
                            }
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
        return null;
    }

    private Pair<SymbolicFormula, SSAMap> makeAndAssume(
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
            return new Pair<SymbolicFormula, SSAMap>(
                    new MathsatSymbolicFormula(res), ssa);
        }
    }

    // creates the two mathsat terms 
    // (var@newidx = var@i1) and (var@newidx = var@i2)
    // used by mergeSSAMaps
    private Pair<Long, Long> makeSSAMerger(String var, int i1, int i2, 
                                           int newidx) {
        // retrieve the mathsat terms corresponding to the two variables
        long decl = mathsat.api.msat_declare_variable(
                msatEnv, var + "@" + Integer.toString(i1), 
                mathsat.api.MSAT_REAL);
        long v1 = mathsat.api.msat_make_variable(msatEnv, decl);
        decl = mathsat.api.msat_declare_variable(
                msatEnv, var + "@" + Integer.toString(i2),
                mathsat.api.MSAT_REAL);
        long v2 = mathsat.api.msat_make_variable(msatEnv, decl);
        // create the fresh variable
        decl = mathsat.api.msat_declare_variable(
                msatEnv, var + "@" + Integer.toString(newidx),
                mathsat.api.MSAT_REAL);
        long v3 = mathsat.api.msat_make_variable(msatEnv, decl);
        // create the two equalities
        long e1 = mathsat.api.msat_make_equal(msatEnv, v3, v1);
        long e2 = mathsat.api.msat_make_equal(msatEnv, v3, v2);
        return new Pair<Long, Long>(e1, e2);
    }

    @Override
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
                int i3 = 
                    absoluteSSAIndices ? SSAMap.getNextSSAIndex() : 
                        (Math.max(i1, i2)+1);
                    result.setIndex(var, i3);
                    Pair<Long, Long> t = makeSSAMerger(var, i1, i2, i3);
                    mt1 = mathsat.api.msat_make_and(msatEnv, mt1, t.getFirst());
                    mt2 = mathsat.api.msat_make_and(msatEnv, mt2, 
                                                    t.getSecond());
            } else {
                result.setIndex(var, i1);
            }
        }
        for (String var : ssa2.allVariables()) {
            int i2 = ssa2.getIndex(var);
            int i1 = ssa1.getIndex(var);
            assert(i2 > 0);
            if (i1 <= 0) {
                result.setIndex(var, i2);
            } else {
                assert(i1 == i2 || result.getIndex(var) > Math.max(i1, i2));
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
                    long newt = 
                        mathsat.api.msat_replace_args(msatEnv, t, newargs);
                    assert(!mathsat.api.MSAT_ERROR_TERM(newt));
                    cache.put(t, newt);
                }
            }
        }

        assert(cache.containsKey(term));
        return new MathsatSymbolicFormula(cache.get(term));
    }

    @Override
    public SymbolicFormula makeFalse() {
        return new MathsatSymbolicFormula(mathsat.api.msat_make_false(msatEnv));
    }

    @Override
    public Pair<SymbolicFormula, SSAMap> shift(SymbolicFormula f, SSAMap ssa) {
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
                    assert(ssaidx > 0);
                    long newt = buildMsatVariable(name, ssaidx + idx-1);
                    assert(!mathsat.api.MSAT_ERROR_TERM(newt));
                    cache.put(t, newt);
                    newssa.setIndex(name, ssaidx + idx-1);
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
                    long newt = 
                        mathsat.api.msat_replace_args(msatEnv, t, newargs);
                    assert(!mathsat.api.MSAT_ERROR_TERM(newt));
                    cache.put(t, newt);
                }
            }
        }

        assert(cache.containsKey(term));
        return new Pair<SymbolicFormula, SSAMap>(
                new MathsatSymbolicFormula(cache.get(term)), newssa);
    }
}
