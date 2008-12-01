package cpa.symbpredabs.mathsat;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import cmdline.CPAMain;

import cpa.symbpredabs.InterpolatingTheoremProver;
import cpa.symbpredabs.SymbolicFormula;

public class MathsatInterpolatingProver implements InterpolatingTheoremProver {

    private long msatEnv;
    private long env;
    private boolean useSharedEnv;
    private Map<Long, Integer> formulaToItpGroup;
    private Map<Long, Long> copyFromCache;

    public MathsatInterpolatingProver(MathsatSymbolicFormulaManager mgr,
                                      boolean useSharing) {
        msatEnv = mgr.getMsatEnv();
        formulaToItpGroup = new HashMap<Long, Integer>();
        env = 0;
        useSharedEnv = useSharing;
        copyFromCache = new HashMap<Long, Long>();
    }

    @Override
    public void init() {
        formulaToItpGroup.clear();

        if (useSharedEnv) {
            env = mathsat.api.msat_create_shared_env(msatEnv);
        } else {
            env = mathsat.api.msat_create_env();
        }

        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_UF);
        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LRA);

        boolean theoryCombinationNeeded = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useDtc");

        if (theoryCombinationNeeded) {
            mathsat.api.msat_set_theory_combination(env,
                    mathsat.api.MSAT_COMB_DTC);
        } else if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useIntegers")) {
            int ok = mathsat.api.msat_set_option(env, "split_eq", "true");
            assert(ok == 0);
        }
        int ok = mathsat.api.msat_set_option(env, "sl", "0");
        assert(ok == 0);

        mathsat.api.msat_init_interpolation(env);
    }

    @Override
    public void addFormula(SymbolicFormula f) {
        long t = ((MathsatSymbolicFormula)f).getTerm();
        if (!useSharedEnv) {
            long t2 = mathsat.api.MSAT_MAKE_ERROR_TERM();
            if (copyFromCache.containsKey(t)) {
                t2 = copyFromCache.get(t);
            } else {
                t2 = mathsat.api.msat_make_copy_from(env, t, msatEnv);
                copyFromCache.put(t, t2);
            }
            t = t2;
        }
        int group = mathsat.api.msat_create_itp_group(env);
        formulaToItpGroup.put(t, group);
        mathsat.api.msat_set_itp_group(env, group);
        mathsat.api.msat_assert_formula(env, t);
    }

    @Override
    public boolean isUnsat() {
        int res = mathsat.api.msat_solve(env);
        assert(res != mathsat.api.MSAT_UNKNOWN);
        return res == mathsat.api.MSAT_UNSAT;
    }

    @Override
    public SymbolicFormula getInterpolant(Vector<SymbolicFormula> formulasOfA) {
        int[] groupsOfA = new int[formulasOfA.size()];
        for (int i = 0; i < groupsOfA.length; ++i) {
            long t =
                ((MathsatSymbolicFormula)formulasOfA.elementAt(i)).getTerm();
            if (!useSharedEnv) {
                assert(copyFromCache.containsKey(t));
                t = copyFromCache.get(t);
            }
            assert(formulaToItpGroup.containsKey(t));
            int group = formulaToItpGroup.get(t);
            groupsOfA[i] = group;
        }
        long itp = mathsat.api.msat_get_interpolant(env, groupsOfA);
        assert(!mathsat.api.MSAT_ERROR_TERM(itp));
        if (!useSharedEnv) {
            itp = mathsat.api.msat_make_copy_from(msatEnv, itp, env);
        }

        return new MathsatSymbolicFormula(itp);
    }

    @Override
    public void reset() {
        mathsat.api.msat_destroy_env(env);
        env = 0;
        formulaToItpGroup.clear();
        copyFromCache.clear();
    }

}
