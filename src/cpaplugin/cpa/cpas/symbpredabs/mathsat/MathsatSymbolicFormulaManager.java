package cpaplugin.cpa.cpas.symbpredabs.mathsat;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

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
    // We need to distinguish assignments from tests. This is needed to 
    // build a formula in SSA form later on, when we have to mapback
    // a counterexample, without adding too many extra variables. Therefore,
    // in the representation of "uninstantiated" symbolic formulas, we
    // use a new binary uninterpreted function ":=" to represent 
    // assignments. When we instantiate the formula, we replace this UIF 
    // with an equality, because now we have an SSA form
    private long assignUfDecl;
    
    // datatype to use for variables, when converting them to mathsat vars
    // can be either MSAT_REAL or MSAT_INT
    // Note that MSAT_INT does not mean that we support the full linear
    // integer arithmetic (LIA)! At the moment, interpolation doesn't work on 
    // LIA, only difference logic or on LRA (i.e. on the rationals). However
    // by setting the vars to be MSAT_INT, the solver tries some heuristics
    // that might work (e.g. tightening of a < b into a <= b - 1, splitting
    // negated equalities, ...)
    private int msatVarType = mathsat.api.MSAT_INT;//REAL;

    public MathsatSymbolicFormulaManager() {
        msatEnv = mathsat.api.msat_create_env();
        assignUfDecl = mathsat.api.msat_declare_uif(msatEnv, ":=", 
                mathsat.api.MSAT_BOOL, 2, new int[]{msatVarType, msatVarType});
    }

    public long getMsatEnv() {
        return msatEnv;
    }

    
    public boolean entails(SymbolicFormula f1, SymbolicFormula f2) {
        MathsatSymbolicFormula m1 = (MathsatSymbolicFormula)f1;
        MathsatSymbolicFormula m2 = (MathsatSymbolicFormula)f2;

        // create a temporary environment for checking the implication
        long env = mathsat.api.msat_create_env();
        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_UF);
        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LRA);
        mathsat.api.msat_set_theory_combination(env, mathsat.api.MSAT_COMB_DTC);
        int ok = mathsat.api.msat_set_option(env, "split_eq", "true");
        assert(ok == 0);

        long t1 = mathsat.api.msat_make_copy_from(env, m1.getTerm(), msatEnv);
        long t2 = mathsat.api.msat_make_copy_from(env, m2.getTerm(), msatEnv);
        long imp = mathsat.api.msat_make_implies(env, t1, t2);
        mathsat.api.msat_assert_formula(env, 
                mathsat.api.msat_make_not(env, imp));
        int res = mathsat.api.msat_solve(env);

        mathsat.api.msat_destroy_env(env);

        return res == mathsat.api.MSAT_UNSAT;
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
                msatVarType);
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
                //return mathsat.api.msat_make_equal(msatEnv, newvar, me);
                return makeAssignment(newvar, me);
            }
            
            case IASTUnaryExpression.op_minus: {
                long mop = buildMsatTerm(operand, ssa, absoluteSSAIndices);
                if (mathsat.api.MSAT_ERROR_TERM(mop)) return mop;
                return mathsat.api.msat_make_negate(msatEnv, mop);
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
                //return mathsat.api.msat_make_equal(msatEnv, mvar, me2);
                return makeAssignment(mvar, me2);
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
    
    private boolean termIsAssignment(long term) {
        return (mathsat.api.msat_term_is_uif(term) != 0 &&
                mathsat.api.msat_term_repr(term).startsWith(":="));
    }
    
    private long makeAssignment(long t1, long t2) {
        return mathsat.api.msat_make_uif(msatEnv, assignUfDecl,
                                         new long[]{t1, t2});
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
            
//            long one = mathsat.api.msat_make_number(msatEnv, "1");

            switch (opType) {
            case IASTBinaryExpression.op_greaterThan:
                // (a > b) --> (b < a) --> (b <= a - 1) on the integers
                result = mathsat.api.msat_make_gt(msatEnv, t1, t2);
//                result = mathsat.api.msat_make_leq(msatEnv, t2,
//                        mathsat.api.msat_make_minus(msatEnv, t1, one));
                break;

            case IASTBinaryExpression.op_greaterEqual:
                // (a >= b) --> (b <= a)
                result = mathsat.api.msat_make_geq(msatEnv, t1, t2);
//                result = mathsat.api.msat_make_leq(msatEnv, t2, t1);
                break;

            case IASTBinaryExpression.op_lessThan:
                // (a < b) --> (a <= b - 1) on the integers
                result = mathsat.api.msat_make_lt(msatEnv, t1, t2);
//                result = mathsat.api.msat_make_leq(msatEnv, t1,
//                        mathsat.api.msat_make_minus(msatEnv, t2, one));
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
                msatVarType);
        long v1 = mathsat.api.msat_make_variable(msatEnv, decl);
        decl = mathsat.api.msat_declare_variable(
                msatEnv, var + "@" + Integer.toString(i2),
                msatVarType);
        long v2 = mathsat.api.msat_make_variable(msatEnv, decl);
        // create the fresh variable
        decl = mathsat.api.msat_declare_variable(
                msatEnv, var + "@" + Integer.toString(newidx),
                msatVarType);
        long v3 = mathsat.api.msat_make_variable(msatEnv, decl);
        // create the two equalities
        long e1 = mathsat.api.msat_make_equal(msatEnv, v3, v1);
        long e2 = mathsat.api.msat_make_equal(msatEnv, v3, v2);
        return new Pair<Long, Long>(e1, e2);
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
            if (termIsAssignment(t)) {
                // treat assignments specially. When we shift, we always have to
                // update the SSA index of the variable being assigned
                long var = mathsat.api.msat_term_get_arg(t, 0);
                if (!cache.containsKey(var)) {
                    String name = mathsat.api.msat_term_repr(var);

                    LazyLogger.log(LazyLogger.DEBUG_1, "SHIFTING ASSIGNMENT: ",
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
                        if (idx == 1) ++idx;
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
        return new Pair<SymbolicFormula, SSAMap>(
                new MathsatSymbolicFormula(cache.get(term)), newssa);
    }

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
                LazyLogger.log(LazyLogger.DEBUG_1, "FOUND UIF IN FORMULA: ", f,
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
    
    private long uninstantiate(long t, Map<Long, Long> cache) {
        if (mathsat.api.msat_term_is_variable(t) != 0) { 
            if (!cache.containsKey(t)) {
                String name = mathsat.api.msat_term_repr(t);
                String[] bits = name.split("@");
                assert(bits.length == 2);
                name = bits[0];
                long d = mathsat.api.msat_declare_variable(msatEnv, name,
                                                           msatVarType);
                long newt = mathsat.api.msat_make_variable(msatEnv, d);
                cache.put(t, newt);
            }
            return cache.get(t);
        } else {
            long[] children = new long[mathsat.api.msat_term_arity(t)];
            for (int i = 0; i < children.length; ++i) {
                children[i] = uninstantiate(mathsat.api.msat_term_get_arg(t, i),
                                            cache);
            }
            return mathsat.api.msat_replace_args(msatEnv, t, children);
        }
    }

    @Override
    public Collection<SymbolicFormula> extractAtoms(SymbolicFormula f,
                                                    boolean uninst) {
        Set<Long> cache = new HashSet<Long>();
        Set<Long> atoms = new HashSet<Long>();
        Map<Long, Long> varcache = new HashMap<Long, Long>();
        
        Stack<Long> toProcess = new Stack<Long>();
        toProcess.push(((MathsatSymbolicFormula)f).getTerm());
        
        while (!toProcess.empty()) {
            long term = toProcess.pop();
            assert(!cache.contains(term));
            cache.add(term);
            
            if (mathsat.api.msat_term_is_atom(term) != 0 &&
                mathsat.api.msat_term_is_true(term) == 0 &&
                mathsat.api.msat_term_is_false(term) == 0) {
                if (uninst) {
                    term = uninstantiate(term, varcache);
                }
                atoms.add(term);
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
 
    // returns an SSA map for the instantiated formula f
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
