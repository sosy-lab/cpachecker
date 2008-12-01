package symbpredabstraction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Stack;
import java.util.Vector;

import logging.LazyLogger;

import cmdline.CPAMain;


/**
 * Parses a file in msat format to extract a list of predicates. The file
 * should contain a formula that is a conjunction of
 * PREDICATE_NAME <-> PREDICATE_DEF
 * where PREDICATE_NAME is a Boolean variable, and PREDICATE_DEF is an atom
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class MathsatPredicateParser {
    private MathsatSymbPredAbsFormulaManager mgr;
    private BDDMathsatAbstractFormulaManager amgr;

    public MathsatPredicateParser(MathsatSymbPredAbsFormulaManager mgr,
                                  BDDMathsatAbstractFormulaManager amgr) {
        this.mgr = mgr;
        this.amgr = amgr;
    }

    public Collection<Predicate> parsePredicates(InputStream in) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuffer data = new StringBuffer();
            String line = r.readLine();
            while (line != null) {
                data.append(line);
                data.append("\n");
                line = r.readLine();
            }
            long msatEnv = mgr.getMsatEnv();
			long formula;
			if(CPAMain.cpaConfig.getProperty("predicates.format").equals("foci")){
				formula = mathsat.api.msat_from_foci(msatEnv, data.toString());
			}
			else if(CPAMain.cpaConfig.getProperty("predicates.format").equals("smt")){
				formula = mathsat.api.msat_from_smt(msatEnv, data.toString());
			}
			else if(CPAMain.cpaConfig.getProperty("predicates.format").equals("msat")){
				formula = mathsat.api.msat_from_msat(msatEnv, data.toString());
			}
			else{
				return null;
			}            if (mathsat.api.MSAT_ERROR_TERM(formula)) {
                return null;
            }
            return parsePredicates(formula);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Collection<Predicate> parsePredicates(long formula) {
        Collection<Predicate> ret = new Vector<Predicate>();
        Stack<Long> toProcess = new Stack<Long>();

        LazyLogger.log(LazyLogger.DEBUG_3,
                       "FORMULA IS: ", new MathsatSymbolicFormula(formula));

        // We *ASSUME* that in the original msat file the formula is a
        // conjunction of (name <-> def) for each predicate. Since mathsat
        // internally translates iffs in a conjunction of implications, which
        // are in turn translated into ORs, here we look only for ORs in which
        // one of the children is a boolean variable
        toProcess.push(formula);
        while (!toProcess.empty()) {
            long t = toProcess.pop();
            if (mathsat.api.msat_term_is_and(t) != 0) {
                for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
                    toProcess.push(mathsat.api.msat_term_get_arg(t, i));
                }
            } else {
                assert(mathsat.api.msat_term_is_or(t) != 0);
                long var = mathsat.api.msat_term_get_arg(t, 0);
                long def = mathsat.api.msat_term_get_arg(t, 1);
                if (mathsat.api.msat_term_is_boolean_var(var) == 0) {
                    long tmp = var;
                    var = def;
                    def = tmp;
                }
                if (mathsat.api.msat_term_is_boolean_var(var) != 0) {
                    assert(mathsat.api.msat_term_is_not(def) != 0);
                    def = mathsat.api.msat_term_get_arg(def, 0);
                    ret.add(amgr.makePredicate(var, def));

                    LazyLogger.log(LazyLogger.DEBUG_1,
                                   "ADDED PREDICATE, name: ",
                                   new MathsatSymbolicFormula(var),
                                   ", atom: ",
                                   new MathsatSymbolicFormula(def));
                }
            }
        }
        return ret;
    }
}
