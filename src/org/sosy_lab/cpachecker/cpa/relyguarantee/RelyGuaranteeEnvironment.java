/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.bdd.BDDRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatTheoremProver;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

/**
 * Stores information about environmental edges.
 */
@Options(prefix="cpa.relyguarantee")
public class RelyGuaranteeEnvironment {

  @Option(description="Print debugging info?")
  private boolean debug=true;

  @Option(name="symbolicCoverageCheck",description="Use a theorem prover to remove covered environemtal transitions" +
  " if false perform only a syntatic check for equivalence")
  private boolean checkEnvTransitionCoverage = true;

  @Option(description="Abstract environmental transitions using their own predicates:"
      + "0 - don't abstract, 1 - abstract filter, 2 - abstract filter and operation.")
  private int abstractEnvTransitions = 2;

  /**
   * Statiscs for processing env transitions transitions.
   */
  public class RelyGuaranteeEnvironmentProcessStatistics implements Statistics {

    private Timer totalTimer    = new Timer();
    private Timer semanticTimer = new Timer();

    private int unprocessed       = 0;
    private int removedSyntactic  = 0;
    private int coverChecks       = 0;
    private int oldCovered        = 0;
    private int newCovered        = 0;
    private int newValid          = 0;
    private int allValid           = 0;

    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {

      out.println("Transitions generated:                 " + unprocessed);
      out.println("Transitions removed by syntatic check: " + removedSyntactic);
      out.println("No of coverage checks (non-debug):     " + coverChecks);
      out.println("New transitions that are covered:      " + newCovered);
      out.println("Old transitions that become covered:   " + oldCovered);
      out.println("New valid transitions by thread:       " + newValid  );
      out.println("All valid transitions by thread:       " + allValid  );
      out.println();
      out.println("Time for coverage check:               " + semanticTimer);
      out.println("Total time for processing:             " + totalTimer);
    }

    @Override
    public String getName() {
      return "Enviornmetal transitions processing statistics";
    }

  }

  // Statitics about processing env. transitions
  private RelyGuaranteeEnvironmentProcessStatistics processStats;

  // number of threads
  private int threadNo;
  // map from ART elements to unprocessed env transitions
  private final Vector<RelyGuaranteeEnvironmentalTransition> unprocessedTransitions;
  // all rely guarantee transitions  generated by thread i that do not have to be processed again
  // for thread i envTransProcessedBeforeFromThread[i] is a multimap : source ART element -> generated env transitions
  //private final Multimap<ARTElement, RelyGuaranteeEnvironmentalTransition>[] envTransProcessedBeforeFromThread;
  // valid rely env edges generated by thread i
  private final Vector<RelyGuaranteeCFAEdgeTemplate>[] validEnvEdgesFromThread;
  // rely guarantee edges valid for thread i, that haven't been applied yet on the CFA
  // these edges should belong to some  validEnvEdgesFromThread[j], where j != i
  // this data structure is used for termination check
  private final Vector<RelyGuaranteeCFAEdgeTemplate>[] unappliedEnvEdgesForThread;
  // rely guarantee edges generated by thread i, that are covered by more general edges in appliedEnvEdgesFromThread[i]
  // the edges in appliedEnvEdgesForThread[i] cannot be in coveredEnvEdgesFromThread[i]
  private final Set<RelyGuaranteeCFAEdgeTemplate>[] coveredEnvEdgesFromThread;
  // envPrecision[i] are predicates for generating env. edges from thread i.
  private final SetMultimap<CFANode, AbstractionPredicate>[] envPrecision;
  // envGlobalPrecision[i] contains global env. predicates for thread i
  private final Set<AbstractionPredicate>[] envGlobalPrecision;
  // information about variables in threads
  private final RelyGuaranteeVariables variables;

  // Managers
  private final PathFormulaManager pfManager;
  private final MathsatFormulaManager fManager;
  private final MathsatTheoremProver tProver;
  private final PredicateAbstractionManager paManager;
  private final RegionManager rManager;
  private final AbstractionManagerImpl absManager;


  public RelyGuaranteeEnvironment(int threadNo, RelyGuaranteeVariables vars, Configuration config, LogManager logger) throws InvalidConfigurationException{
    // TODO add option for caching
    config.inject(this, RelyGuaranteeEnvironment.class);

    this.threadNo = threadNo;
    this.unprocessedTransitions = new Vector<RelyGuaranteeEnvironmentalTransition>();
    this.validEnvEdgesFromThread = new Vector[threadNo];
    this.unappliedEnvEdgesForThread = new Vector[threadNo];
    this.coveredEnvEdgesFromThread = new HashSet[threadNo];
    this.envPrecision = new SetMultimap[threadNo];
    this.envGlobalPrecision = new HashSet[threadNo];
    this.variables = vars;

    // set up managers
    MathsatFormulaManager msatFormulaManager;
    this.rManager = BDDRegionManager.getInstance();
    msatFormulaManager = MathsatFormulaManager.getInstance(config, logger);
    this.fManager = msatFormulaManager;
    this.tProver = MathsatTheoremProver.getInstance(msatFormulaManager);
    PathFormulaManager pfMgr  = PathFormulaManagerImpl.getInstance(msatFormulaManager, config, logger);
    pfMgr = CachingPathFormulaManager.getInstance(pfMgr);
    this.pfManager = pfMgr;
    RegionManager rManager = BDDRegionManager.getInstance();
    this.paManager = PredicateAbstractionManager.getInstance(rManager, fManager, pfMgr, tProver, config, logger);
    this.absManager = AbstractionManagerImpl.getInstance(rManager, msatFormulaManager, pfManager, config, logger);


    for (int i=0; i< threadNo; i++){
      //envTransProcessedBeforeFromThread[i] = HashMultimap.<ARTElement, RelyGuaranteeEnvironmentalTransition>create();
      this.validEnvEdgesFromThread[i] = new Vector<RelyGuaranteeCFAEdgeTemplate>();
      this.unappliedEnvEdgesForThread[i] = new Vector<RelyGuaranteeCFAEdgeTemplate>();
      this.coveredEnvEdgesFromThread[i] = new HashSet<RelyGuaranteeCFAEdgeTemplate>();
      this.envPrecision[i] = HashMultimap.create();
      this.envGlobalPrecision[i] = new HashSet<AbstractionPredicate>();
    }

    // test
/*
    for (int i=0; i<threadNo; i++){
      for (String var : vars.globalVars){
        Formula vh = this.fManager.makeVariable(PathFormula.NEXTVAL_SYMBOL + var + PathFormula.PRIME_SYMBOL + i);
        Formula v =  this.fManager.makeVariable(var + PathFormula.PRIME_SYMBOL + i);
        Formula atom = this.fManager.makeEqual(vh, v);
        AbstractionPredicate pred = absManager.makePredicate(atom);
        envGlobalPrecision[i].add(pred);
      }
    }*/
  }



  public int getThreadNo() {
    return threadNo;
  }

  public void setThreadNo(int pThreadNo) {
    threadNo = pThreadNo;
  }

  public RelyGuaranteeVariables getVariables() {
    return variables;
  }

  public PathFormulaManager getPfManager() {
    return pfManager;
  }

  public MathsatFormulaManager getfManager() {
    return fManager;
  }

  public MathsatTheoremProver gettProver() {
    return tProver;
  }

  public PredicateAbstractionManager getPaManager() {
    return paManager;
  }



  public RegionManager getrManager() {
    return rManager;
  }



  public AbstractionManagerImpl getAbsManager() {
    return absManager;
  }



  /**
   * Add new environmental transitions for processing.
   * @param pAElement
   * @param pNewEnvTransitions
   */
  public void addEnvTransitions(Vector<RelyGuaranteeEnvironmentalTransition> newTransitions) {
    if (!newTransitions.isEmpty()){
      unprocessedTransitions.addAll(newTransitions);
    }
  }

  public void addEnvTransition(RelyGuaranteeEnvironmentalTransition newTransition) {
    unprocessedTransitions.add(newTransition);
  }

  public void clearUnappliedEnvEdgesForThread(int i) {
    unappliedEnvEdgesForThread[i].removeAllElements();
  }

  public List<RelyGuaranteeEnvironmentalTransition> getUnprocessedTransitions(){
    return unprocessedTransitions;
  }

  public void addNewEnvEgde(int j, RelyGuaranteeCFAEdgeTemplate edge) {
    unappliedEnvEdgesForThread[j].add(edge);
  }


  public  List<RelyGuaranteeCFAEdgeTemplate> getValidEnvEdgesFromThread(int i) {
    return Collections.unmodifiableList(validEnvEdgesFromThread[i]);
  }

  public  List<RelyGuaranteeCFAEdgeTemplate> getUnappliedEnvEdgesForThread(int i) {
    return Collections.unmodifiableList(unappliedEnvEdgesForThread[i]);
  }

  public void printUnprocessedTransitions() {
    printTransitions("Enviornmental transitions generated:", unprocessedTransitions);
  }

  /**
   * Convert environmental transitions from thread i into relevant CFA edges.
   * Remove edges that are useless, have been applied before or are covered.
   * @param i   thread that generate the transitions
   */
  public void processEnvTransitions(int i) {
    processStats = new RelyGuaranteeEnvironmentProcessStatistics();
    processStats.totalTimer.start();
    processStats.unprocessed = unprocessedTransitions.size();

    syntacticCoverageCheck(i);
    // generate CFA edges from env transitions
    Vector<RelyGuaranteeCFAEdgeTemplate> rgEdges = new Vector<RelyGuaranteeCFAEdgeTemplate>();
    for (RelyGuaranteeEnvironmentalTransition  et: unprocessedTransitions){
      //assert envTransProcessedBeforeFromThread[i].containsValue(et);

      int tid = et.getSourceThread();
      assert tid == i;

      // apply the env operation on env pf
     /* PathFormula opPf = null;
      try {
        opPf = pfManager.makePureAnd(et.getPathFormula(), et.getEdge(), tid);
      } catch (CPATransferException e) {
        // TODO better handling
        e.printStackTrace();
      }

      // find the variable changed by the operation
      SSAMap envSsa = et.getPathFormula().getSsa();
      SSAMap opSsa   = opPf.getSsa();
      String opVar   = null;

      for (String var : opSsa.allVariables()){
        if (opSsa.getIndex(var) > envSsa.getIndex(var)){
          Pair<String, Integer> data = PathFormula.getPrimeData(var);
          opVar = data.getFirst();
          break;
        }
      }

      assert opVar != null;*/

      // find the last abstraction element


      //ARTElement nextARTAbstractionElement = findNextAbstractionARTElement(et.getSourceARTElement());
      //assert nextARTAbstractionElement != null;

      RelyGuaranteeCFAEdgeTemplate rgEdge = generateEnvTransition(et);
      rgEdges.add(rgEdge);
    }
    unprocessedTransitions.clear();


    if (checkEnvTransitionCoverage) {
      processStats.semanticTimer.start();
      Pair<Vector<RelyGuaranteeCFAEdgeTemplate>, Vector<RelyGuaranteeCFAEdgeTemplate>> pair = semanticCoverageCheck(rgEdges, i);
      processStats.oldCovered = pair.getSecond().size();
      processStats.semanticTimer.stop();
      if (debug){
        printEdges("New env. edges that are not covered:",pair.getFirst());
        printEdges("Old env. edges that are covered by some new ones:",pair.getSecond());
      }

      rgEdges = pair.getFirst();
      processStats.newValid = rgEdges.size();
      // remove valid edges that have been covered
      validEnvEdgesFromThread[i].removeAll(pair.getSecond());
      // unapplied edges may also become covered
      for (int j=0; j<threadNo; j++){
        if (j != i){
          unappliedEnvEdgesForThread[j].removeAll(pair.getSecond());
        }
      }
    }
    // add the edges after filtering to the set of valid edges by thread i
    // add them to the set of unapplied edges for other threads
    validEnvEdgesFromThread[i].addAll(rgEdges);
    processStats.allValid = validEnvEdgesFromThread[i].size();
    distributeAsUnapplied(rgEdges, i);

    if (debug){
      printEdges("All valid env. edges from thread "+i+" after filtering", validEnvEdgesFromThread[i]);
      assertion();
    }

    processStats.totalTimer.stop();
  }

  /**
   * Generates a template for a env. transition. The transition may be abstracted.
   * @param et
   * @return
   */
  private RelyGuaranteeCFAEdgeTemplate generateEnvTransition(RelyGuaranteeEnvironmentalTransition et) {
    if (et.getSourceARTElement().getElementId() == 4315){
      System.out.println();
    }

    ARTElement lastARTAbstractionElement = findLastAbstractionARTElement(et.getSourceARTElement());
    assert lastARTAbstractionElement != null;

    if (this.abstractEnvTransitions == 2){

      int sourceTid = et.getSourceThread();
      PathFormula oldPf = et.getPathFormula();
      AbstractionFormula oldAbs = et.getAbstractionFormula();
      PathFormula newPf = null;

      // compute the sucessor's path formula
      try {
        newPf = pfManager.makeAnd(oldPf, et.getEdge(), sourceTid);
      } catch (CPATransferException e) {
        e.printStackTrace();
      }

      // increment indexes of variables global and local to this thread by 1, mimimal index is 2
      Set<String> vars = new HashSet<String>(variables.globalVars);
      vars.addAll(variables.localVars.get(sourceTid));
      SSAMapBuilder newSsaBldr = SSAMap.emptySSAMap().builder();
      SSAMap oldSsa = oldPf.getSsa();
      for (String var : vars){
        String pVar = var + PathFormula.PRIME_SYMBOL + sourceTid;
        int idx = Math.max(oldSsa.getIndex(pVar) + 1, 2);
        newSsaBldr.setIndex(pVar, idx);
      }
      SSAMap newSsa = newSsaBldr.build();

      // create a formula, where every index is increased - either by operation or by equivalence
      Pair<Pair<Formula, Formula>, SSAMap> equivs = pfManager.mergeRelyGuaranteeSSAMaps(newPf.getSsa(), newSsa, sourceTid);
      Formula newF = fManager.makeAnd(newPf.getFormula(), equivs.getFirst().getFirst());
      newPf = new PathFormula(newF, newSsa, newPf.getLength());

      // get the predicates for the transition
      CFANode loc = et.getEdge().getPredecessor();
      SetMultimap<CFANode, AbstractionPredicate> prec = envPrecision[sourceTid];
      Set<AbstractionPredicate> preds = new HashSet<AbstractionPredicate>(prec.get(loc));
      preds.addAll(envGlobalPrecision[sourceTid]);

      // abstract
      AbstractionFormula newAbs = paManager.buildNextValAbstraction(oldAbs, oldPf, newPf, preds);
      assert lastARTAbstractionElement != null;

      return new RelyGuaranteeAbstractCFAEdgeTemplate(newAbs, lastARTAbstractionElement, et);
    }
    else if (this.abstractEnvTransitions == 1){
      // abstract the conjuction of abstraction and path formula using set of predicates.
      int sourceTid = et.getSourceThread();
      CFANode loc = et.getEdge().getPredecessor();
      // preds is the set of env. predicates for the location plus the global predicates
      SetMultimap<CFANode, AbstractionPredicate> prec = envPrecision[sourceTid];
      Set<AbstractionPredicate> preds = new HashSet<AbstractionPredicate>(prec.get(loc));
      preds.addAll(envGlobalPrecision[sourceTid]);

      AbstractionFormula aFilter = paManager.buildAbstraction(et.getAbstractionFormula(), et.getPathFormula(), preds);
      return new RelyGuaranteeAbstractCFAEdgeTemplate(aFilter, lastARTAbstractionElement, et);
    } else {
      // don't abstract - the filer is conjuction of abstraction and path formula of  the generating elements
      PathFormula filter = pfManager.makeAnd(et.getPathFormula(), et.getAbstractionPathFormula());
      return new RelyGuaranteeCFAEdgeTemplate(filter, lastARTAbstractionElement, et);
    }
  }

  private void assertion(){
    for (int i=0; i<threadNo; i++){
      // valid
      for (RelyGuaranteeCFAEdgeTemplate et : validEnvEdgesFromThread[i]){
        //assert envTransProcessedBeforeFromThread[i].containsValue(et.getSourceEnvTransition());

        assert !coveredEnvEdgesFromThread[i].contains(et);
        assert !et.getSourceARTElement().isDestroyed();
        assert !et.getPathFormula().toString().contains("dummy");
        if (checkEnvTransitionCoverage){
          for (RelyGuaranteeCFAEdgeTemplate et2 : validEnvEdgesFromThread[i]){
            assert et == et2 || !isCovered(et, et2);
          }
        }

      }
      // covered
      for (RelyGuaranteeCFAEdgeTemplate et3 : coveredEnvEdgesFromThread[i]){
        //assert envTransProcessedBeforeFromThread[i].containsValue(et3.getSourceEnvTransition());
        assert !validEnvEdgesFromThread[i].contains(et3);
        assert !et3.getPathFormula().toString().contains("dummy");
        assert !et3.getSourceARTElement().isDestroyed();
        assert et3.getCoveredBy()!=null;
        assert et3.getCoveredBy().getCovers().contains(et3);
        assert validEnvEdgesFromThread[i].contains(et3.getCoveredBy()) || coveredEnvEdgesFromThread[i].contains(et3.getCoveredBy());
      }
      // processed before
      /*for (ARTElement artElement: envTransProcessedBeforeFromThread[i].keySet()){
        assert !artElement.isDestroyed();
        for (RelyGuaranteeEnvironmentalTransition et : envTransProcessedBeforeFromThread[i].get(artElement)){
          assert artElement == et.getSourceARTElement();
        }
      }*/

    }
    // unapplied
    for (int j=0; j<threadNo; j++){
      for (RelyGuaranteeCFAEdgeTemplate et: this.unappliedEnvEdgesForThread[j]){
        boolean isValid = false;
        for (int i=0; i<threadNo; i++){
          if (i!=j){
            if (validEnvEdgesFromThread[i].contains(et)){
              isValid = true;
            }
          }
        }
        assert isValid;
        assert !et.getPathFormula().toString().contains("dummy");
        assert !et.getSourceARTElement().isDestroyed();
      }
    }
    // unprocessed
    for ( RelyGuaranteeEnvironmentalTransition et: this.unprocessedTransitions){
      assert !et.getSourceARTElement().isDestroyed();
    }

  }
  /**
   * Print env. edges with a title.
   * @param string
   * @param rgEdges
   */
  private void printEdges(String string, List<RelyGuaranteeCFAEdgeTemplate> rgEdges) {
    System.out.println();
    if (rgEdges.isEmpty()){
      System.out.println(string+"\tnone");
    } else {
      System.out.println(string);
    }
    for (RelyGuaranteeCFAEdgeTemplate edge : rgEdges){
      System.out.println("\t-"+edge);
    }
  }

  /**
   * Print env. transitinos with a title.
   * @param string
   */
  private void printTransitions(String string, Collection<RelyGuaranteeEnvironmentalTransition> transitions) {
    System.out.println();
    if (transitions.isEmpty()){
      System.out.println(string+"\tnone");
    } else {
      System.out.println(string);
    }
    for (RelyGuaranteeEnvironmentalTransition tran : transitions){
      System.out.println("\t-"+tran);
    }
  }

  /**
   * Distributes env. edges as unapplied to threads other than i.
   * @param rgEdges
   * @param i
   */
  private void distributeAsUnapplied(List<RelyGuaranteeCFAEdgeTemplate> rgEdges, int i) {
    for (int j=0; j<threadNo; j++){
      if (j!=i){
        unappliedEnvEdgesForThread[j].addAll(rgEdges);
      }
    }
  }

  /**
   * Removes environmental transitions that have been produced before by thread i  or are obviously unnecessary.
   */
  private void syntacticCoverageCheck(int i) {
    Vector<RelyGuaranteeEnvironmentalTransition> toDelete = new Vector<RelyGuaranteeEnvironmentalTransition>();
    Vector<RelyGuaranteeEnvironmentalTransition> toPrint = new Vector<RelyGuaranteeEnvironmentalTransition>();


    for (RelyGuaranteeEnvironmentalTransition  et: unprocessedTransitions){
      Formula f = et.getPathFormula().getFormula();
      PathFormula af = et.getAbstractionPathFormula();
      PathFormula pf = et.getPathFormula();
      CFAEdge localEdge = et.getEdge();
      // don't generate transition with 'false' or transitions that assign to local variables
      if (f.isFalse() || af.getFormula().isFalse() || pf.getFormula().isFalse() || isLocalAssigment(localEdge)) {
        toDelete.add(et);
        if (debug){
          System.out.println("Removed (syn,false): "+et);
        }
      }
    }

    processStats.removedSyntactic = toDelete.size();
    unprocessedTransitions.removeAll(toDelete);

    if (debug){
      printTransitions("Env. transitions removed by syntactick check:", toPrint);
    }

  }



  /**
   * Return a pair of list.
   * The first list contains env. edges from newEdges that ARE NOT properly covered by any other edge in this list
   * or in validEnvEdgesFromThread[i].
   * The second list contains env. edges from validEnvEdgesFromThread[i] that ARE properly covered by some edge in newEdges.
   * @param rgEdges   set of new env edges generated by thread i
   * @param i         source thread
   */
  private Pair<Vector<RelyGuaranteeCFAEdgeTemplate>, Vector<RelyGuaranteeCFAEdgeTemplate>> semanticCoverageCheck(List<RelyGuaranteeCFAEdgeTemplate> newEdges, int i) {

    Vector<RelyGuaranteeCFAEdgeTemplate> toProcess = new Vector<RelyGuaranteeCFAEdgeTemplate>(newEdges);
    Vector<RelyGuaranteeCFAEdgeTemplate> toDelete = new Vector<RelyGuaranteeCFAEdgeTemplate>();


    // find the most general env. edges in toProcess. If edge2 is more general then edge1, but they are not equivalent,
    // then remember that edge2 covers edge1
    for (RelyGuaranteeCFAEdgeTemplate edge1 : toProcess){
      if (!toDelete.contains(edge1)){
        for (RelyGuaranteeCFAEdgeTemplate edge2 : toProcess){
          if (edge1 !=edge2 && !toDelete.contains(edge2)){
            processStats.coverChecks++;
            if (isCovered(edge1,edge2)){
              // edge1 => edge2
              toDelete.add(edge1);
              if (debug){
                System.out.println("Covered 0:\t"+edge1+" => "+edge2);
              }
              edge1.coveredBy(edge2);
              processStats.newCovered++;
              coveredEnvEdgesFromThread[i].add(edge1);
              assert coveredEnvEdgesFromThread[i].contains(edge1);
              break;
            }
          }
        }
      }
    }

    toProcess.removeAll(toDelete);

    Vector<RelyGuaranteeCFAEdgeTemplate> list1 = new Vector<RelyGuaranteeCFAEdgeTemplate>(toProcess);
    Vector<RelyGuaranteeCFAEdgeTemplate> list2 = new Vector<RelyGuaranteeCFAEdgeTemplate>();

    // sanity check on request
    if (debug){
      for (RelyGuaranteeCFAEdgeTemplate edge1 : newEdges){
        for (RelyGuaranteeCFAEdgeTemplate edge2 : toProcess){
          assert edge1 == edge2 || !isCovered(edge2,edge1)  ||  isCovered(edge2,edge1);
        }
      }
      for (RelyGuaranteeCFAEdgeTemplate edge1 : toProcess){
        for (RelyGuaranteeCFAEdgeTemplate edge2 : toProcess){
          assert edge1 == edge2 || !isCovered(edge2,edge1);
        }
      }
    }

    while (!toProcess.isEmpty()){
      RelyGuaranteeCFAEdgeTemplate newEdge = toProcess.remove(0);
      for (RelyGuaranteeCFAEdgeTemplate oldEdge : validEnvEdgesFromThread[i]){
        assert newEdge != oldEdge;
        processStats.coverChecks++;
        if (isCovered(newEdge, oldEdge)){
          // newEdge => oldEdge
          if (debug){
            System.out.println("Covered 1:\t"+newEdge+" => "+oldEdge);
          }
          newEdge.coveredBy(oldEdge);
          processStats.newCovered++;
          coveredEnvEdgesFromThread[i].add(newEdge);
          list1.remove(newEdge);
          break;
        } else if (isCovered(oldEdge, newEdge)){
          // oldEdge => newEdge, but not equivalent
          if (debug){
            System.out.println("Covered 2:\t"+oldEdge+" => "+newEdge);
          }
          oldEdge.coveredBy(newEdge);
          processStats.oldCovered++;
          coveredEnvEdgesFromThread[i].add(oldEdge);
          list2.add(oldEdge);
        }
      }
    }

    // sanity check on request
    if (debug){
      for (RelyGuaranteeCFAEdgeTemplate edge1  : list1){
        for (RelyGuaranteeCFAEdgeTemplate edge2  : newEdges){
          assert edge1 == edge2 || !isCovered(edge1,edge2) ||  isCovered(edge2,edge1);
        }
        for (RelyGuaranteeCFAEdgeTemplate edge2  : validEnvEdgesFromThread[i]){
          assert  !isCovered(edge1,edge2);
        }
      }
      for (RelyGuaranteeCFAEdgeTemplate edge1  : list2){
        boolean covered = false;
        assert validEnvEdgesFromThread[i].contains(edge1);
        for (RelyGuaranteeCFAEdgeTemplate edge2  : list1){
          covered = covered || isCovered(edge1, edge2);
        }
        assert covered;
      }
    }
    return Pair.of(list1, list2);
  }

  /**
   * Returns true if env1 => env2 is valid, sound but not complete if operation is not abstracted.
   */
  public boolean isCovered(RelyGuaranteeCFAEdgeTemplate env1, RelyGuaranteeCFAEdgeTemplate env2) {
    if (env1.equals(env2)){
      return true;
    }

    // if operation was unabstracted, then they must match
    if (this.abstractEnvTransitions == 0 || this.abstractEnvTransitions == 1){
      if (!env1.getLocalEdge().equals(env2.getLocalEdge())){
        return false;
      }
    }

    Formula f1= env1.getFilter().getFormula();
    Formula f2 = env2.getFilter().getFormula();

    if (f1.isFalse() || f2.isTrue()) {
      return true;
    }

    if (this.abstractEnvTransitions == 0){
      // check coverage by thm. prover


      Formula nImpl = fManager.makeAnd(f1, fManager.makeNot(f2));
      tProver.init();
      try {
        return tProver.isUnsat(nImpl);
      } finally {
        tProver.reset();
      }
    } else {
      // check coverage by BDDs
      assert env1.getType() == RelyGuaranteeCFAEdgeTemplate.RelyGuaranteeAbstractCFAEdgeTemplate;
      assert env2.getType() == RelyGuaranteeCFAEdgeTemplate.RelyGuaranteeAbstractCFAEdgeTemplate;
      RelyGuaranteeAbstractCFAEdgeTemplate aEnv1 = (RelyGuaranteeAbstractCFAEdgeTemplate) env1;
      RelyGuaranteeAbstractCFAEdgeTemplate aEnv2 = (RelyGuaranteeAbstractCFAEdgeTemplate) env2;
      Region r1 = aEnv1.getAbstractFilter().asRegion();
      Region r2 = aEnv2.getAbstractFilter().asRegion();

      return rManager.entails(r1, r2);
    }
  }

  /**
   * Returns true iff edge is an assignment to a non-global variable
   */
  private boolean isLocalAssigment(CFAEdge edge) {
    String var = getLhsVariable(edge);
    if (var == null || !variables.globalVars.contains(var)){
      return true;
    }
    return false;
  }

  /**
   * Get the variable in the lhs of an expression or return null
   */
  private String getLhsVariable(CFAEdge edge){
    IASTNode node = edge.getRawAST();
    if (node instanceof IASTExpressionAssignmentStatement) {
      IASTExpressionAssignmentStatement stmNode = (IASTExpressionAssignmentStatement) node;
      if (stmNode.getLeftHandSide() instanceof IASTIdExpression) {
        IASTIdExpression idExp = (IASTIdExpression) stmNode.getLeftHandSide();
        return new String(idExp.getName());
      }
    }
    return null;
  }

  /**
   * reachedElement elements has been merged into mergedElement, therefore adjust the source elements.
   * @param mergedElement
   * @param reachedElement
   */
  public void mergeSourceElements(ARTElement mergedElement, ARTElement reachedElement, int i) {
    assert reachedElement.isDestroyed();
    assert !mergedElement.isDestroyed();

    // valid
    for(RelyGuaranteeCFAEdgeTemplate rgEdge : validEnvEdgesFromThread[i]){
      ARTElement sourceARTElement = rgEdge.getSourceARTElement();
      if (sourceARTElement == reachedElement){
        rgEdge.setSourceARTElement(mergedElement);
        System.out.println("! Replaced id:"+reachedElement.getElementId()+" by id:"+mergedElement.getElementId()+" in a valid edge: "+rgEdge);
      }
    }

    // covered
    for(RelyGuaranteeCFAEdgeTemplate rgEdge : coveredEnvEdgesFromThread[i]){
      ARTElement sourceARTElement = rgEdge.getSourceARTElement();
      if (sourceARTElement == reachedElement){
        rgEdge.setSourceARTElement(mergedElement);
        System.out.println("! Replaced id:"+reachedElement.getElementId()+" by id:"+mergedElement.getElementId()+" in a valid edge: "+rgEdge);
      }
    }

    // unproccessed

    for (RelyGuaranteeEnvironmentalTransition et :  unprocessedTransitions){
      if (et.getSourceARTElement() == reachedElement){
        et.setSourceARTElement(mergedElement);
        System.out.println("! Replaced id:"+reachedElement.getElementId()+" by id:"+mergedElement.getElementId()+" in unprocesseds transition: "+et);
      }
    }

    if (debug){
      assertion();
    }

  }

  /**
   * Looks for env. edges generated in threads on the list and checks if their source elements have been destroyed.
   * Their  path formula is set to false and they are not valid any more. Transitions covered by them will be valid
   * again, unless they have also been  killed.
   * @param artReachedSets
   * @param refinementResult
   * @param tid
   */
  public void killEnvironmetalEdges(Iterable<Integer> tids, ARTReachedSet[] artReachedSets, Multimap<Integer, Pair<ARTElement, RelyGuaranteePrecision>> refinementResult) {
    System.out.println();
    System.out.println("\t\t\t ----- Killing environemtal edges ------");
    for (Integer tid : tids){
      // remove covered transitions that belong to the subtree
      killCovered(tid);
      // kill valid and processed env. edges
      killValid(tid);
    }
    // propagate drop trees
    Set<Integer> dropped = dropApplications(artReachedSets, refinementResult);
    while (!dropped.isEmpty()){
      for (Integer tid : dropped){
        // remove covered transitions that belong to the subtree
        killCovered(tid);
        // kill valid and processed env. edges
        killValid(tid);
      }
      dropped = dropApplications(artReachedSets, refinementResult);
    }

    // kill unapplied  env. edges
    killUnapplied();
    // drop unprocess transitions that were generated in the subtree
    removeUnprocessedTransitionsFromElement();



    if (debug){
      assertion();
    }

  }

  /**
   * For every covered env. edge from thread i, whose source element has been destroyed:
   * - remove them for the list
   * - push one level up env. edges that they cover
   * - make them false
   * @param pRoot
   */
  private void killCovered(int tid) {
    Vector<RelyGuaranteeCFAEdgeTemplate> toDelete = new Vector<RelyGuaranteeCFAEdgeTemplate>();
    for (RelyGuaranteeCFAEdgeTemplate rgEdge : coveredEnvEdgesFromThread[tid]){
      if (rgEdge.getSourceARTElement().isDestroyed()){
        // rgEdge belongs to a dropped subtree
        toDelete.add(rgEdge);
        rgEdge.killCoveredEdge();

        //makeRelyGuaranteeEnvEdgeFalse(rgEdge);
      }
    }
    if (debug){
      printEdges("Covered env. edge kill in thread "+tid+" :",toDelete);
    }

    coveredEnvEdgesFromThread[tid].removeAll(toDelete);

    for (RelyGuaranteeCFAEdgeTemplate rgEdge : coveredEnvEdgesFromThread[tid]){
      assert !rgEdge.getSourceARTElement().isDestroyed();
    }
  }

  /**
   * For every valid env. edge from thread i, whose source element has been destroyed:
   * - remove from the list
   * - make them false
   * - revert covered transitions
   * @param root
   * @param tid
   * @param artReachedSets
   */
  private void killValid(int tid) {
    Vector<RelyGuaranteeCFAEdgeTemplate> toDelete = new Vector<RelyGuaranteeCFAEdgeTemplate>();
    for (RelyGuaranteeCFAEdgeTemplate rgEdge : validEnvEdgesFromThread[tid]){
      if (rgEdge.getSourceARTElement().isDestroyed()){
        // rgEdge belongs to a dropped subtree
        toDelete.add(rgEdge);
        //makeRelyGuaranteeEnvEdgeFalse(rgEdge);
      }
    }

    if (debug){
      printEdges("Valid env. edge killed in thread "+tid+" :",toDelete);
    }

    validEnvEdgesFromThread[tid].removeAll(toDelete);
    // see if some transitions that were covered by rgEdge can become valid
    // they could also be covered by some other valid transition
    for (RelyGuaranteeCFAEdgeTemplate rgEdge : toDelete){
      List<RelyGuaranteeCFAEdgeTemplate> covered = new Vector<RelyGuaranteeCFAEdgeTemplate>(rgEdge.getCovers());

      rgEdge.killValidEdge();

      for (RelyGuaranteeCFAEdgeTemplate edge : covered){
        assert edge.getCoveredBy() == null;
      }

      Pair<Vector<RelyGuaranteeCFAEdgeTemplate>, Vector<RelyGuaranteeCFAEdgeTemplate>> pair = semanticCoverageCheck(covered, tid);
      validEnvEdgesFromThread[tid].addAll(pair.getFirst());
      validEnvEdgesFromThread[tid].removeAll(pair.getSecond());
      for (int i=0; i<threadNo; i++){
        if (i!=tid){
          this.unappliedEnvEdgesForThread[i].removeAll(pair.getSecond());
        }
      }
      coveredEnvEdgesFromThread[tid].removeAll(pair.getFirst());
      distributeAsUnapplied(pair.getFirst(), tid);
      if (!pair.getFirst().isEmpty()){
        System.out.println();
      }
      printEdges("Env edge uncovered after  "+rgEdge+":", pair.getFirst());
    }

    for (RelyGuaranteeCFAEdgeTemplate rgEdge : toDelete){
      assert !validEnvEdgesFromThread[tid].contains(toDelete);
    }
    for (RelyGuaranteeCFAEdgeTemplate rgEdge : validEnvEdgesFromThread[tid]){
      assert !rgEdge.getSourceARTElement().isDestroyed();
      assert !rgEdge.getPathFormula().toString().contains("dummy");
      assert !coveredEnvEdgesFromThread[tid].contains(rgEdge);
    }

  }

  /**
   * For every unapplied env. edge, whose source element has been destroyed:
   * - it removes it from the set
   * @param root
   * @param tid
   */
  private void killUnapplied() {
    Vector<RelyGuaranteeCFAEdgeTemplate> toDelete = new Vector<RelyGuaranteeCFAEdgeTemplate>();
    for(int j=0; j<threadNo; j++){
      for (RelyGuaranteeCFAEdgeTemplate rgEdge : unappliedEnvEdgesForThread[j]){
        if (rgEdge.getSourceARTElement().isDestroyed()){
          // rgEdge belongs to the dropped subtree
          toDelete.add(rgEdge);
        }
      }
      unappliedEnvEdgesForThread[j].removeAll(toDelete);
      if (debug){
        printEdges("Unapplied env. edge killed in thread "+j+" :",toDelete);
      }
      toDelete.clear();
    }
  }



  private Set<Integer> dropApplications(ARTReachedSet[] artReachedSets, Multimap<Integer, Pair<ARTElement, RelyGuaranteePrecision>> refinementResult) {
    //List<Pair<Integer, ARTElement>> toDrop = new Vector<Pair<Integer, ARTElement>>();
    Multimap<Integer, ARTElement> toDrop = HashMultimap.create();
    for (int i=0; i<threadNo; i++){
      UnmodifiableReachedSet reached = artReachedSets[i].asReachedSet();
      for (AbstractElement element : reached){
        ARTElement artElement = (ARTElement) element;
        RelyGuaranteeAbstractElement rgElement = AbstractElements.extractElementByType(artElement, RelyGuaranteeAbstractElement.class);
        if (rgElement instanceof AbstractionElement){
          CFAEdge edge = rgElement.getParentEdge();
          if (edge != null && edge.getEdgeType() == CFAEdgeType.RelyGuaranteeCFAEdge ){
            RelyGuaranteeCFAEdge rgEdge = (RelyGuaranteeCFAEdge) edge;
            ARTElement source = rgEdge.getSourceARTElement();
            if (source.isDestroyed()){
              /// drop it
              toDrop.put(i, artElement);
            }
          }
        }
      }
    }

    System.out.println();
    System.out.println("Removing subtree because of dead env. edges:");
    for (Integer tid : toDrop.keySet()){
      // add the precision after interpolation to the dropped elements - without the same refinement pattern between threads may repeat forever
      Collection<Pair<ARTElement, RelyGuaranteePrecision>> precisions = refinementResult.get(tid);
      assert precisions.size() <= 1;
      SetMultimap<CFANode, AbstractionPredicate> oldPreds = null;
      for (Pair<ARTElement, RelyGuaranteePrecision> pair : precisions){
        oldPreds = pair.getSecond().getPredicateMap();
      }

      System.out.println("Interpolation precision for thread "+tid+" is: "+oldPreds);

      for (ARTElement artElement : toDrop.get(tid)){
        if (!artElement.isDestroyed()){
          ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
          // add the interpolation precision, if any
          if (oldPreds != null){
            pmapBuilder.putAll(oldPreds);
          };

          Precision prec = artReachedSets[tid].getPrecision(artElement);
          RelyGuaranteePrecision rgPrec = Precisions.extractPrecisionByType(prec, RelyGuaranteePrecision.class);
          pmapBuilder.putAll(rgPrec.getPredicateMap());
          RelyGuaranteePrecision newRgPrec = new RelyGuaranteePrecision(pmapBuilder.build(), rgPrec.getGlobalPredicates());

          System.out.println("- ART element id:"+artElement.getElementId()+" in thread "+tid+" new prec: "+newRgPrec.getPredicateMap());
          artReachedSets[tid].removeSubtree(artElement, newRgPrec);

        }

      }
    }

    return toDrop.keySet();
  }


  /**
   * Removes rgEdge generated by thread i from the list of edges that do not have to be processed.
   * @param rgEdge
   * @param tid
   */
  /*private void removeFromProcessedBefore(RelyGuaranteeCFAEdgeTemplate rgEdge , int tid) {
    RelyGuaranteeEnvironmentalTransition et = rgEdge.getSourceEnvTransition();
    ARTElement artElement = rgEdge.getSourceARTElement();
    boolean changed = envTransProcessedBeforeFromThread[tid].remove(artElement, et);
    if (!changed){
      System.out.println("DEBUG: "+et+" from element id:"+et.getSourceARTElement().getElementId());
    }
    assert changed;
  }*/




/*
  private void makeRelyGuaranteeEnvEdgeFalse(RelyGuaranteeCFAEdgeTemplate rgEdge){
    //System.out.println("---> Made false "+rgEdge);
    PathFormula falsePathFormula = pfManager.makeFalsePathFormula();
    PathFormulaWrapper pfw = rgEdge.getPathFormulaWrapper();
    ARTElementWrapper aew = rgEdge.getSourceARTElementWrapper();
    assert !pfw.getPathFormula().getFormula().isFalse();
    assert aew.getArtElement() != null;
    pfw.setPathFormula(falsePathFormula);
    //aew.setARTElement(null);
  }/*

  /**
   * Removes all unprocessed environmental transitions that belong to subtree root at the specified element.
   * @param root
   */
  public void removeUnprocessedTransitionsFromElement() {

    List<RelyGuaranteeEnvironmentalTransition> toDelete = new Vector<RelyGuaranteeEnvironmentalTransition>();

    for ( RelyGuaranteeEnvironmentalTransition et: unprocessedTransitions ){
      if (et.getSourceARTElement().isDestroyed()){
        System.out.println("Killing unprocessed transitions at "+et.getSourceARTElement().getElementId());
        toDelete.add(et);
      }
    }
    unprocessedTransitions.removeAll(toDelete);
  }

  public void clearUnprocessedTransitions() {
    unprocessedTransitions.clear();
  }

  public void cleanEnvironment(int tid) {
    // TODO works only for two threads
    int other = tid==0 ? 1 : 0;
    unappliedEnvEdgesForThread[other].clear();
    validEnvEdgesFromThread[tid].clear();
    coveredEnvEdgesFromThread[tid].clear();
  }

  public void resetEnvironment() {
    for (int i=0; i<threadNo; i++){
      unappliedEnvEdgesForThread[i].clear();
      validEnvEdgesFromThread[i].clear();
      coveredEnvEdgesFromThread[i].clear();
    }
    unprocessedTransitions.clear();
  }

  public void printProcessingStatistics(){
    System.out.println();
    System.out.println("Processing statistics:");
    if (processStats != null){
      processStats.printStatistics(System.out, null, null);
    }
  }

  /**
   * Finds the next descendant in the ART that is a rely-guarantee abstraction.
   * Returns the argument if it is an abstraction.
   * @param element
   * @return
   */
  private ARTElement findNextAbstractionARTElement(ARTElement element) {

    ARTElement naARTElement = null;
    Deque<ARTElement> toProcess = new LinkedList<ARTElement>();
    Set<ARTElement> visisted = new HashSet<ARTElement>();
    toProcess.add(element);

    while (!toProcess.isEmpty()){
      ARTElement elem = toProcess.poll();
      visisted.add(elem);

      AbstractionElement aElement = AbstractElements.extractElementByType(elem, AbstractionElement.class);
      if (aElement != null){
        naARTElement = elem;
        break;
      }

      for (ARTElement parent : elem.getChildren()){
        if (!visisted.contains(parent)){
          toProcess.addLast(parent);
        }
      }
    }

    return naARTElement;
  }


  /**
   * Finds the last rely-guarantee abstraction element that is an ancestor of the argument.
   * @param pElement
   * @return
   */
  public static ARTElement findLastAbstractionARTElement(ARTElement pElement) {
    ARTElement laARTElement = null;
    Deque<ARTElement> toProcess = new LinkedList<ARTElement>();
    Set<ARTElement> visisted = new HashSet<ARTElement>();
    toProcess.add(pElement);

    while (!toProcess.isEmpty()){
      ARTElement element = toProcess.poll();
      visisted.add(element);

      AbstractionElement aElement = AbstractElements.extractElementByType(element, AbstractionElement.class);
      if (aElement != null){
        laARTElement = element;
        break;
      }

      for (ARTElement parent : element.getParents()){
        if (!visisted.contains(parent)){
          toProcess.addLast(parent);
        }
      }
    }

    return laARTElement;
  }



  public SetMultimap<CFANode, AbstractionPredicate>[] getEnvPrecision() {
    return envPrecision;
  }

  /**
   * Add predicates to environmental precision of some thread.
   * @param tid
   * @param loc
   * @param preds
   */
  public void addPredicatesToEnvPrecision(Integer tid, CFANode loc, Collection<AbstractionPredicate> preds){
    this.envPrecision[tid].putAll(loc, preds);
  }

  public Set<AbstractionPredicate>[] getEnvGlobalPrecision() {
    return envGlobalPrecision;
  }







}



