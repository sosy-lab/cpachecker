package cpaplugin.cpa.cpas.itpabs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Stack;
import java.util.Vector;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.cpas.symbpredabs.ConcreteTraceNoInfo;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.UnrecognizedCFAEdgeException;
import cpaplugin.cpa.cpas.symbpredabs.logging.LazyLogger;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatSymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.MathsatSymbolicFormulaManager;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;

/**
 * An ItpCounterexampleRefiner is an object that is used to perform refinement
 * in interpolation-based lazy abstraction. It is also used for "forced
 * coverings" (see the CAV'06 paper by McMillan on "Lazy Abstraction with
 * Interpolants" for details)
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpCounterexampleRefiner {
    
    // some statistics. All times are in milliseconds
    public class Stats {
        public long abstractionMathsatTime = 0;
        public long abstractionMaxMathsatTime = 0;
        public long abstractionBddTime = 0;
        public long abstractionMaxBddTime = 0;
        public int numCallsAbstraction = 0;
        public long cexAnalysisTime = 0;
        public long cexAnalysisMaxTime = 0;
        public int numCallsCexAnalysis = 0;
        public long abstractionMathsatSolveTime = 0;
        public long abstractionMaxMathsatSolveTime = 0;
        public long cexAnalysisMathsatTime = 0;
        public long cexAnalysisMaxMathsatTime = 0;
        public int abstractionNumMathsatQueries = 0;
        
        public long forceCoverTime = 0;
        public long forceCoverMaxTime = 0;
        public long forceCoverMathsatTime = 0;
        public long forceCoverMaxMathsatTime = 0;
    }
    protected Stats stats;
    
    protected int cexDumpNum = 0;
    protected boolean dumpCexQueries;
    
    public ItpCounterexampleRefiner() {
        super();
        stats = new Stats();
        dumpCexQueries = CPAMain.cpaConfig.getBooleanValue(
                "cpas.itpabs.mathsat.dumpRefinementQueries");
    }
    
    public Stats getStats() { return stats; }

    /**
     * counterexample analysis and abstraction refinement, using interpolants
     * directly
     */
    public ItpCounterexampleTraceInfo buildCounterexampleTrace(
            SymbolicFormulaManager mgr,
            Deque<ItpAbstractElement> abstractTrace) {
        assert(abstractTrace.size() > 1);
        
//        mathsat.api.msat_set_verbosity(1);
        long startTime = System.currentTimeMillis();
        stats.numCallsCexAnalysis++;
        
        // create the DAG formula corresponding to the abstract trace. We create
        // n formulas, one per interpolation group
        SSAMap ssa = new SSAMap();        
        MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;
        
        Vector<SymbolicFormula> f = new Vector<SymbolicFormula>();
        
        LazyLogger.log(LazyLogger.DEBUG_1, "\nBUILDING COUNTEREXAMPLE TRACE\n");
        LazyLogger.log(LazyLogger.DEBUG_1, "ABSTRACT TRACE: ", abstractTrace);
        
        Object[] abstarr = abstractTrace.toArray();
        ItpAbstractElement cur = (ItpAbstractElement)abstarr[0];
        
        boolean theoryCombinationNeeded = false;
        
        MathsatSymbolicFormula bitwiseAxioms = 
            (MathsatSymbolicFormula)mmgr.makeTrue();
        
        for (int i = 1; i < abstarr.length; ++i) {
            ItpAbstractElement e = (ItpAbstractElement)abstarr[i];
            CFAEdge found = null;
            for (int j = 0; j < e.getLocation().getNumEnteringEdges(); ++j) {
                CFAEdge edge = e.getLocation().getEnteringEdge(j);
                if (edge.getPredecessor().equals(cur.getLocation())) {
                    found = edge;
                    break;
                }
            }
            assert(found != null);
            Pair<SymbolicFormula, SSAMap> p = null;
            try {
                p = mmgr.makeAnd(mmgr.makeTrue(), found, ssa, false, false);
            } catch (UnrecognizedCFAEdgeException e1) {
                e1.printStackTrace();
                System.exit(1);
            }
            
            SSAMap newssa = null;
            SymbolicFormula fm = null;
            if (false) {//i != 1) {
                LazyLogger.log(LazyLogger.DEBUG_3, "SHIFTING: ", p.getFirst(),
                        " WITH SSA: ", ssa);
                p = mmgr.shift(p.getFirst(), ssa);
                newssa = p.getSecond();
                LazyLogger.log(LazyLogger.DEBUG_3, "RESULT: ", p.getFirst(),
                               " SSA: ", newssa);
                newssa.update(ssa);
                fm = p.getFirst();
            } else {
                fm = mmgr.replaceAssignments(
                        (MathsatSymbolicFormula)p.getFirst());
                LazyLogger.log(LazyLogger.DEBUG_3, "INITIAL: ", fm,
                               " SSA: ", p.getSecond());
                newssa = p.getSecond();
            }
            boolean hasUf = mmgr.hasUninterpretedFunctions(
                    (MathsatSymbolicFormula)fm);
            theoryCombinationNeeded |= hasUf;
            f.add(fm);
            ssa = newssa;
            cur = e;
            
            if (hasUf && CPAMain.cpaConfig.getBooleanValue(
                    "cpas.symbpredabs.useBitwiseAxioms")) {
                MathsatSymbolicFormula a = mmgr.getBitwiseAxioms(
                        (MathsatSymbolicFormula)p.getFirst());
                bitwiseAxioms = (MathsatSymbolicFormula)mmgr.makeAnd(
                        bitwiseAxioms, a);
            }
        }
        
        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.useBitwiseAxioms")) {
            LazyLogger.log(LazyLogger.DEBUG_3, "ADDING BITWISE AXIOMS TO THE ",
                    "LAST GROUP: ", bitwiseAxioms);
            f.setElementAt(mmgr.makeAnd(f.elementAt(f.size()-1), bitwiseAxioms),
                    f.size()-1);
        }
        
        LazyLogger.log(LazyLogger.DEBUG_3,
                       "Checking feasibility of abstract trace");
        
        // now f is the DAG formula which is satisfiable iff there is a 
        // concrete counterexample
        //
        // create a working environment
        long env = mathsat.api.msat_create_env();
        long msatEnv = mmgr.getMsatEnv();
        long[] terms = new long[f.size()];
        for (int i = 0; i < terms.length; ++i) {
            terms[i] = mathsat.api.msat_make_copy_from(
                        env, ((MathsatSymbolicFormula)f.elementAt(i)).getTerm(), 
                        msatEnv);
        }
        // initialize the env and enable interpolation
        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_UF);
        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LRA);
        if (theoryCombinationNeeded) {
            mathsat.api.msat_set_theory_combination(env, 
                    mathsat.api.MSAT_COMB_DTC);
        } else if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useIntegers")) {
            int ok = mathsat.api.msat_set_option(env, "split_eq", "true");
            assert(ok == 0);
        }
//        int ok = mathsat.api.msat_set_option(env, "toplevelprop", "2");
//        assert(ok == 0);
        
        mathsat.api.msat_init_interpolation(env);        
        
        // for each term, create an interpolation group
        int[] groups = new int[terms.length];
        for (int i = 0; i < groups.length; ++i) {
            groups[i] = mathsat.api.msat_create_itp_group(env);
        }
        // then, assert the formulas
        long res = mathsat.api.MSAT_UNKNOWN;

        boolean shortestTrace = CPAMain.cpaConfig.getBooleanValue(
            "cpas.symbpredabs.shortestCexTrace");
        
        ++cexDumpNum;
        long msatSolveTimeStart = System.currentTimeMillis();
        for (int i = 0; i < terms.length; ++i) {
            mathsat.api.msat_set_itp_group(env, groups[i]);
            mathsat.api.msat_assert_formula(env, terms[i]);
            
            dumpMsat(String.format("cex_%02d.%02d.msat", cexDumpNum, i),
                     env, terms[i]);

            LazyLogger.log(LazyLogger.DEBUG_1,
                           "Asserting formula: ", 
                           new MathsatSymbolicFormula(terms[i]),
                           " in group: ", groups[i]);

            if (shortestTrace && mathsat.api.msat_term_is_true(terms[i]) == 0) {
                res = mathsat.api.msat_solve(env);
                assert(res != mathsat.api.MSAT_UNKNOWN);
                if (res == mathsat.api.MSAT_UNSAT) {
                    break;
                }
            }
        }
        // and check satisfiability
        if (!shortestTrace) {
            res = mathsat.api.msat_solve(env);
        }
        long msatSolveTimeEnd = System.currentTimeMillis();
        
        assert(res != mathsat.api.MSAT_UNKNOWN);
        
        ItpCounterexampleTraceInfo info = null;
        
        if (res == mathsat.api.MSAT_UNSAT) {
            // the counterexample is spurious. Extract the predicates from
            // the interpolants
            info = new ItpCounterexampleTraceInfo(true);            
//            UpdateablePredicateMap pmap = new UpdateablePredicateMap();
//            info.setPredicateMap(pmap);
            // how to partition the trace into (A, B) depends on whether
            // there are function calls involved or not: in general, A
            // is the trace from the entry point of the current function
            // to the current point, and B is everything else. To implement
            // this, we keep track of which function we are currently in.
            Stack<Integer> entryPoints = new Stack<Integer>();
            entryPoints.push(0);
            for (int i = 1; i < groups.length; ++i) {
                int start_of_a = entryPoints.peek();
                if (!CPAMain.cpaConfig.getBooleanValue(
                       "cpas.symbpredabs.refinement.addWellScopedPredicates")) {
                    // if we don't want "well-scoped" predicates, we always
                    // cut from the beginning
                    start_of_a = 0;
                }
                		
                int[] groups_of_a = new int[i-start_of_a];
                for (int j = 0; j < groups_of_a.length; ++j) {
                    groups_of_a[j] = groups[j+start_of_a];
                }
                long itp = mathsat.api.msat_get_interpolant(env, groups_of_a);
                assert(!mathsat.api.MSAT_ERROR_TERM(itp));
                
                if (CPACheckerLogger.getLevel() <= 
                    LazyLogger.DEBUG_1.intValue()) {
                    StringBuffer buf = new StringBuffer();
                    for (int g : groups_of_a) {
                        buf.append(g);
                        buf.append(" ");
                    }
                    LazyLogger.log(LazyLogger.DEBUG_1, "groups_of_a: ", buf);
                }
                LazyLogger.log(LazyLogger.DEBUG_1,
                               "Got interpolant(", i, "): ",
                               new MathsatSymbolicFormula(itp));
                
                long itpc = mathsat.api.msat_make_copy_from(msatEnv, itp, env);
                ItpAbstractElement s1 = 
                    (ItpAbstractElement)abstarr[i];
                info.setFormulaForRefinement(
                        s1, mmgr.uninstantiate(
                                new MathsatSymbolicFormula(itpc)));
                
                // If we are entering or exiting a function, update the stack 
                // of entry points
                ItpAbstractElement e = (ItpAbstractElement)abstarr[i];
                if (isFunctionEntry(e)) {
                    LazyLogger.log(LazyLogger.DEBUG_3,
                            "Pushing entry point, function: ",
                            e.getLocation().getFunctionName());
                    entryPoints.push(i);
                } 
                if (isFunctionExit(e)) {
                    LazyLogger.log(LazyLogger.DEBUG_3,
                            "Popping entry point, returning from function: ",
                            e.getLocation().getFunctionName());
                    entryPoints.pop();
                }                
            }
            info.setFormulaForRefinement((ItpAbstractElement)abstarr[0], 
                    mmgr.makeTrue());
            info.setFormulaForRefinement(
                    (ItpAbstractElement)abstarr[abstarr.length-1], 
                    mmgr.makeFalse());            
        } else {
            // this is a real bug, notify the user
            info = new ItpCounterexampleTraceInfo(false);
            info.setConcreteTrace(new ConcreteTraceNoInfo());
            // TODO - reconstruct counterexample
            // For now, we dump the asserted formula to a user-specified file
            String cexPath = CPAMain.cpaConfig.getProperty(
                    "cpas.symbpredabs.refinement.msatCexPath");
            if (cexPath != null) {
                long t = mathsat.api.msat_make_true(env);
                for (int i = 0; i < terms.length; ++i) {
                    t = mathsat.api.msat_make_and(env, t, terms[i]);
                }
                String msatRepr = mathsat.api.msat_to_msat(env, t);
                try {
                    PrintWriter pw = new PrintWriter(new File(cexPath));
                    pw.println(msatRepr);
                    pw.close();
                } catch (FileNotFoundException e) {
                    LazyLogger.log(CustomLogLevel.INFO, 
                            "Failed to save msat Counterexample to file: ",
                            cexPath);
                }
            }
        }
        
        mathsat.api.msat_destroy_env(env);
        
//        mathsat.api.msat_set_verbosity(0);
        
        // update stats
        long endTime = System.currentTimeMillis();
        long totTime = endTime - startTime;
        stats.cexAnalysisTime += totTime;
        stats.cexAnalysisMaxTime = Math.max(totTime, stats.cexAnalysisMaxTime);
        long msatSolveTime = msatSolveTimeEnd - msatSolveTimeStart;
        stats.cexAnalysisMathsatTime += msatSolveTime;
        stats.cexAnalysisMaxMathsatTime = 
            Math.max(msatSolveTime, stats.cexAnalysisMaxMathsatTime);
        
        return info;
    }

    private boolean isFunctionExit(ItpAbstractElement e) {
        return false; // TODO
//        CFANode inner = e.getLocation();
//        return (inner.getNumLeavingEdges() == 1 && 
//                inner.getLeavingEdge(0) instanceof ReturnEdge);
    }

    private boolean isFunctionEntry(ItpAbstractElement e) {
        return false; // TODO
//        CFANode inner = e.getLocation();
//        return (inner.getNumEnteringEdges() > 0 &&
//                inner.getEnteringEdge(0).getPredecessor() instanceof 
//                FunctionDefinitionNode);
    }

    /**
     * Forced Covering checks (see McMillan's CAV'06 paper)
     */ 
    public ItpCounterexampleTraceInfo forceCover(
            SymbolicFormulaManager mgr,
            ItpAbstractElement x,
            Deque<ItpAbstractElement> path, ItpAbstractElement w) {
        assert(path.size() > 1);
        
//      mathsat.api.msat_set_verbosity(1);
      long startTime = System.currentTimeMillis();
      //stats.numCallsCexAnalysis++;
      
      // create the DAG formula corresponding to the abstract trace. We create
      // n formulas, one per interpolation group
      SSAMap ssa = new SSAMap();        
      MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;
      
      Vector<SymbolicFormula> f = new Vector<SymbolicFormula>();
      
      LazyLogger.log(LazyLogger.DEBUG_1, "\nCHECKING FORCED COVERAGE\n");
      LazyLogger.log(LazyLogger.DEBUG_1, "PATH: ", path);
      
      Object[] abstarr = path.toArray();
      ItpAbstractElement cur = (ItpAbstractElement)abstarr[0];
      
      boolean theoryCombinationNeeded = false;
      
      MathsatSymbolicFormula bitwiseAxioms = 
          (MathsatSymbolicFormula)mmgr.makeTrue();
      
      SymbolicFormula statex = mmgr.instantiate(x.getAbstraction(), null);
      f.add(statex);
      
      for (int i = 1; i < abstarr.length; ++i) {
          ItpAbstractElement e = (ItpAbstractElement)abstarr[i];
          CFAEdge found = null;
          for (int j = 0; j < e.getLocation().getNumEnteringEdges(); ++j) {
              CFAEdge edge = e.getLocation().getEnteringEdge(j);
              if (edge.getPredecessor().equals(cur.getLocation())) {
                  found = edge;
                  break;
              }
          }
          assert(found != null);
          Pair<SymbolicFormula, SSAMap> p = null;
          try {
              p = mmgr.makeAnd(mmgr.makeTrue(), found, ssa, false, false);
          } catch (UnrecognizedCFAEdgeException e1) {
              e1.printStackTrace();
              System.exit(1);
          }
          
          SSAMap newssa = null;
          SymbolicFormula fm = null;
          if (false) {//i != 1) {
              LazyLogger.log(LazyLogger.DEBUG_3, "SHIFTING: ", p.getFirst(),
                      " WITH SSA: ", ssa);
              p = mmgr.shift(p.getFirst(), ssa);
              newssa = p.getSecond();
              LazyLogger.log(LazyLogger.DEBUG_3, "RESULT: ", p.getFirst(),
                             " SSA: ", newssa);
              newssa.update(ssa);
              fm = p.getFirst();
          } else {
              fm = mmgr.replaceAssignments(
                      (MathsatSymbolicFormula)p.getFirst());
              LazyLogger.log(LazyLogger.DEBUG_3, "INITIAL: ", fm,
                             " SSA: ", p.getSecond());
              newssa = p.getSecond();
          }
          boolean hasUf = mmgr.hasUninterpretedFunctions(
                  (MathsatSymbolicFormula)fm);
          theoryCombinationNeeded |= hasUf;
          f.add(fm);
          ssa = newssa;
          cur = e;
          
          if (hasUf && CPAMain.cpaConfig.getBooleanValue(
                  "cpas.symbpredabs.useBitwiseAxioms")) {
              MathsatSymbolicFormula a = mmgr.getBitwiseAxioms(
                      (MathsatSymbolicFormula)p.getFirst());
              bitwiseAxioms = (MathsatSymbolicFormula)mmgr.makeAnd(
                      bitwiseAxioms, a);
          }
      }
      
      long msatEnv = mmgr.getMsatEnv();
      
      SymbolicFormula statew = mmgr.instantiate(w.getAbstraction(),  ssa);
      statew = new MathsatSymbolicFormula(mathsat.api.msat_make_not(msatEnv, 
              ((MathsatSymbolicFormula)statew).getTerm()));
      f.add(statew);
      
      if (CPAMain.cpaConfig.getBooleanValue(
              "cpas.symbpredabs.useBitwiseAxioms")) {
          LazyLogger.log(LazyLogger.DEBUG_3, "ADDING BITWISE AXIOMS TO THE ",
                  "LAST GROUP: ", bitwiseAxioms);
          f.setElementAt(mmgr.makeAnd(f.elementAt(f.size()-1), bitwiseAxioms),
                  f.size()-1);
      }
      
      LazyLogger.log(LazyLogger.DEBUG_3,
                     "Checking feasibility of abstract trace");
      
      // now f is the DAG formula which is satisfiable iff there is a 
      // concrete counterexample
      //
      // create a working environment
      long env = mathsat.api.msat_create_env();
      long[] terms = new long[f.size()];
      for (int i = 0; i < terms.length; ++i) {
          terms[i] = mathsat.api.msat_make_copy_from(
                      env, ((MathsatSymbolicFormula)f.elementAt(i)).getTerm(), 
                      msatEnv);
      }
      // initialize the env and enable interpolation
      mathsat.api.msat_add_theory(env, mathsat.api.MSAT_UF);
      mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LRA);
      if (theoryCombinationNeeded) {
          mathsat.api.msat_set_theory_combination(env, 
                  mathsat.api.MSAT_COMB_DTC);
      } else if (CPAMain.cpaConfig.getBooleanValue(
              "cpas.symbpredabs.mathsat.useIntegers")) {
          int ok = mathsat.api.msat_set_option(env, "split_eq", "true");
          assert(ok == 0);
      }
      int ok = mathsat.api.msat_set_option(env, "toplevelprop", "2");
      assert(ok == 0);
      
      mathsat.api.msat_init_interpolation(env);        
      
      // for each term, create an interpolation group
      int[] groups = new int[terms.length];
      for (int i = 0; i < groups.length; ++i) {
          groups[i] = mathsat.api.msat_create_itp_group(env);
      }
      // then, assert the formulas
      long res = mathsat.api.MSAT_UNKNOWN;

      long msatSolveTimeStart = System.currentTimeMillis();
      for (int i = 0; i < terms.length; ++i) {
          mathsat.api.msat_set_itp_group(env, groups[i]);
          mathsat.api.msat_assert_formula(env, terms[i]);

          LazyLogger.log(LazyLogger.DEBUG_1,
                         "Asserting formula: ", 
                         new MathsatSymbolicFormula(terms[i]),
                         " in group: ", groups[i]);
      }
      // and check satisfiability
      res = mathsat.api.msat_solve(env);
      long msatSolveTimeEnd = System.currentTimeMillis();
      
      assert(res != mathsat.api.MSAT_UNKNOWN);
      
      ItpCounterexampleTraceInfo info = null;
      
      if (res == mathsat.api.MSAT_UNSAT) {
          // the forced coverage check is successful. 
          // Extract the predicates from the interpolants
          info = new ItpCounterexampleTraceInfo(true);            
          for (int k = 0; k < abstarr.length; ++k) {
              int i = k+1;
              int start_of_a = 0;
                              
              int[] groups_of_a = new int[i-start_of_a];
              for (int j = 0; j < groups_of_a.length; ++j) {
                  groups_of_a[j] = groups[j+start_of_a];
              }
              long itp = mathsat.api.msat_get_interpolant(env, groups_of_a);
              assert(!mathsat.api.MSAT_ERROR_TERM(itp));
              
              if (CPACheckerLogger.getLevel() <= 
                  LazyLogger.DEBUG_1.intValue()) {
                  StringBuffer buf = new StringBuffer();
                  for (int g : groups_of_a) {
                      buf.append(g);
                      buf.append(" ");
                  }
                  LazyLogger.log(LazyLogger.DEBUG_1, "groups_of_a: ", buf);
              }
              LazyLogger.log(LazyLogger.DEBUG_1,
                             "Got interpolant(", i, "): ",
                             new MathsatSymbolicFormula(itp));
              
              long itpc = mathsat.api.msat_make_copy_from(msatEnv, itp, env);
              ItpAbstractElement s1 = 
                  (ItpAbstractElement)abstarr[k];
              info.setFormulaForRefinement(
                      s1, mmgr.uninstantiate(
                              new MathsatSymbolicFormula(itpc)));              
          }
      } else {
          // this is a real bug, notify the user
          info = new ItpCounterexampleTraceInfo(false);
      }
      
      mathsat.api.msat_destroy_env(env);
      
//      mathsat.api.msat_set_verbosity(0);
      
      // update stats
      long endTime = System.currentTimeMillis();
      long totTime = endTime - startTime;
      stats.forceCoverTime += totTime;
      stats.forceCoverMaxTime = Math.max(totTime, stats.forceCoverMaxTime);
      long msatSolveTime = msatSolveTimeEnd - msatSolveTimeStart;
      stats.forceCoverMathsatTime += msatSolveTime;
      stats.forceCoverMaxMathsatTime = 
          Math.max(msatSolveTime, stats.forceCoverMaxMathsatTime);
      
      return info;
    }

    // used to dump interpolation queries to file for debugging
    protected void dumpMsat(String filename, long env, long term) {
        if (dumpCexQueries) {
            try {
                PrintWriter out = new PrintWriter(new File(filename));
                String repr = mathsat.api.msat_to_msat(env, term);
                out.println(repr);
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                assert(false);
            }
        }
    }

}
