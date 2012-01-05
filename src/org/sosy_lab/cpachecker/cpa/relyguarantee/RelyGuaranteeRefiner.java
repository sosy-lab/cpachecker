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

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Lists.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTUtils;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;


@Options(prefix="cpa.relyguarantee")
public class RelyGuaranteeRefiner{

  @Option(description="Print debugging info?")
  private boolean debug=true;

  @Option(name="refinement.addPredicatesGlobally",
      description="refinement will add all discovered predicates "
        + "to all the locations in the abstract trace")
        private boolean addPredicatesGlobally = false;

  @Option(name="refinement.addPredicatesGlobally",
      description="refinement will add all discovered predicates "
        + "to all the locations in the abstract trace")
        private boolean addEnvPredicatesGlobally = true;

  @Option(name="errorPath.export",
      description="export one satisfying assignment for the error path")
      private boolean exportErrorPath = true;

  @Option(name="errorPath.file", type=Option.Type.OUTPUT_FILE,
      description="export one satisfying assignment for the error path")
      private File exportFile = new File("ErrorPathAssignment.txt");

  @Option(name="refinement.msatCexFile", type=Option.Type.OUTPUT_FILE,
      description="where to dump the counterexample formula in case the error location is reached")
      private File dumpCexFile = new File("counterexample.msat");

  @Option(name="refinement.restartAnalysis",
      description="restart analysis after refinement")
      private boolean restartAnalysis = false;

  @Option(description="List of variables global to multiple threads")
  protected String[] globalVariables = {};



  public abstract class  RelyGuaranteeRefinerStatistics implements Statistics {

    protected Timer totalTimer          = new Timer();
    public    Timer interpolationTimer  = new Timer();
    public    Timer formulaTimer        = new Timer();
    protected Timer   restartingTimer     = new Timer();

    public int formulaNo                = 0;
    public int unsatChecks              = 0;
    public int maxPredicatesPerLoc      = 0;

    @Override
    public String getName() {
      return "RG refinement statistics";
    }

  }
  /**
   * Information about a restarting refinement.
   */
  public class RelyGuaranteeRefinerRestartingStatistics extends RelyGuaranteeRefinerStatistics {

    public void printStatistics(PrintStream out, Result pResult,ReachedSet pReached){
      out.println("Interpolation fomulas:           " + formulaNo);
      out.println("Unsat checks:                    " + unsatChecks);
      out.println("Max. predicates per location     " + maxPredicatesPerLoc);
      out.println();
      out.println("Time on constructing formulas:   " + formulaTimer);
      out.println("Total time on interpolation:     " + interpolationTimer+" (max: "+interpolationTimer.printMaxTime()+")");
      out.println("Time on restarting analysis:     " + restartingTimer);
      out.println("Total time on refinement:        " + totalTimer);
    }
  }

  /**
   * Information about a lazy refinement.
   */
  public class RelyGuaranteeRefinerLazyStatistics extends RelyGuaranteeRefinerStatistics {

    public void printStatistics(PrintStream out, Result pResult,ReachedSet pReached){
      out.println("Interpolation fomulas:           " + formulaNo);
      out.println("Unsat checks:                    " + unsatChecks);
      out.println();
      out.println("Time on constructing formulas:   " + formulaTimer);
      out.println("Total time on interpolation:     " + interpolationTimer+" (max: "+interpolationTimer.printMaxTime()+")");
      out.println("Time on restarting analysis:     " + restartingTimer);
      out.println("Total time on refinement:        " + totalTimer);
    }
  }


  private RelyGuaranteeRefinerStatistics stats;
  private RelyGuaranteeRefinementManager manager;
  private final ARTCPA[] artCpas;

  private RelyGuaranteeEnvironment rgEnvironment;
  private static RelyGuaranteeRefiner rgRefiner;

  /**
   * Singleton instance of RelyGuaranteeRefiner.
   * @param cpas
   * @param rgEnvironment
   * @param pConfig
   * @return
   * @throws InvalidConfigurationException
   */
  public static RelyGuaranteeRefiner getInstance(final ConfigurableProgramAnalysis[] cpas, RelyGuaranteeEnvironment rgEnvironment, Configuration pConfig) throws InvalidConfigurationException {
    if (rgRefiner == null){
      rgRefiner = new RelyGuaranteeRefiner(cpas, rgEnvironment, pConfig);
    }
    return rgRefiner;
  }

  public RelyGuaranteeRefiner(final ConfigurableProgramAnalysis[] cpas, RelyGuaranteeEnvironment rgEnvironment, Configuration pConfig) throws InvalidConfigurationException{
    pConfig.inject(this, RelyGuaranteeRefiner.class);
    artCpas = new ARTCPA[cpas.length];
    for (int i=0; i<cpas.length; i++){
      if (cpas[i] instanceof ARTCPA) {
        artCpas[i] = (ARTCPA) cpas[i];
      } else {
        throw new InvalidConfigurationException("ART CPA needed for refinement");
      }
    }

    this.rgEnvironment = rgEnvironment;

    RelyGuaranteeCPA rgCPA = artCpas[0].retrieveWrappedCpa(RelyGuaranteeCPA.class);
    if (rgCPA != null){
      manager = rgCPA.getRelyGuaranteeManager();
      //rgCPA.getConfiguration().inject(this, RelyGuaranteeRefiner.class);
    } else {
      throw new InvalidConfigurationException("RelyGuaranteeCPA needed for refinement");
    }

  }


  protected Path computePath(ARTElement pLastElement, ReachedSet pReached) throws InterruptedException, CPAException {
    return ARTUtils.getOnePathTo(pLastElement);
  }

  protected List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> transformPath(Path pPath) throws CPATransferException {
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> result = Lists.newArrayList();

    for (ARTElement ae : skip(transform(pPath, Pair.<ARTElement>getProjectionToFirst()), 1)) {
      RelyGuaranteeAbstractElement pe = extractElementByType(ae, RelyGuaranteeAbstractElement.class);
      if (pe instanceof AbstractionElement) {
        CFANode loc = AbstractElements.extractLocation(ae);
        result.add(Triple.of(ae, loc, pe));
      }
    }

    //assert pPath.getLast().getFirst() == result.get(result.size()-1).getFirst();
    return result;
  }

  /**
   *
   * @param pRelyGuaranteeEnvironment
   * @param pReachedSets
   * @param pErrorThr
   * @return
   * @throws CPAException
   * @throws InterruptedException
   */
  public boolean performRefinment(ReachedSet[] reachedSets, RelyGuaranteeEnvironment environment, int errorThr) throws InterruptedException, CPAException {

    // use statistics appropriate to refinement method: lazy or restarting
    if (restartAnalysis){
      stats = new RelyGuaranteeRefinerRestartingStatistics();
    } else {
      stats = new RelyGuaranteeRefinerLazyStatistics();
    }

    stats.totalTimer.start();

    int threadNo = reachedSets.length;


    //assert checkART(reachedSets[errorThr]);
    assert reachedSets[errorThr].getLastElement() instanceof ARTElement;
    ARTElement targetElement = (ARTElement) reachedSets[errorThr].getLastElement();
    assert (targetElement).isTarget();

    ARTReachedSet[] artReachedSets = new ARTReachedSet[threadNo];
    for (int i=0; i<threadNo; i++){
      artReachedSets[i] = new ARTReachedSet(reachedSets[i], artCpas[i]);
    }
    System.out.println();
    System.out.println("\t\t\t ----- Interpolation -----");
    CounterexampleTraceInfo mCounterexampleTraceInfo = manager.buildRgCounterexampleTrace(targetElement, reachedSets, errorThr, stats);

    // if error is spurious refine
    if (mCounterexampleTraceInfo.isSpurious()) {
      Multimap<Integer, Pair<ARTElement, RelyGuaranteePrecision>> refinementResult;
      if (restartAnalysis){
        System.out.println();
        System.out.println("\t\t\t ----- Restarting analysis -----");
        stats.restartingTimer.start();
        refinementResult = restartingRefinement(reachedSets, mCounterexampleTraceInfo);
      } else {
        System.out.println();
        System.out.println("\t\t\t ----- Lazy abstraction -----");
        refinementResult = lazyRefinement(reachedSets, mCounterexampleTraceInfo, errorThr);
      }


      /*System.out.println("Wait lists before refinement");
      for (int i=0; i<reachedSets.length; i++){
        System.out.println("For thread "+i);
        for (AbstractElement element : reachedSets[i].getWaitlist()){
          ARTElement artElement = (ARTElement) element;
          System.out.println("\t - id:"+artElement.getElementId());
        }
      }*/

      // drop subtrees and change precision
      for(int tid : refinementResult.keySet()){
        for(Pair<ARTElement, RelyGuaranteePrecision> pair : refinementResult.get(tid)){
          ARTElement root = pair.getFirst();
          // drop cut-off node in every thread
          RelyGuaranteePrecision precision = pair.getSecond();
          Set<ARTElement> parents = new HashSet<ARTElement>(root.getParents());


          // TODO why does it take so long?
          artReachedSets[tid].removeSubtree(root, precision);

          if (debug){
            System.out.println();
            for (ARTElement parent : parents){
              RelyGuaranteePrecision prec = Precisions.extractPrecisionByType(artReachedSets[tid].getPrecision(parent), RelyGuaranteePrecision.class);
              System.out.println("Precision for thread "+tid+":");
              System.out.println("\t-ART local " + prec.getPredicateMap());
              System.out.println("\t-ART global " + prec.getGlobalPredicates());
              System.out.println("\t-Env local "  +  rgEnvironment.getEnvPrecision()[tid]);
              System.out.println("\t-Env global " + rgEnvironment.getEnvGlobalPrecision()[tid]);
            }
          }
        }
      }
      if (restartAnalysis){
        System.out.println();
        System.out.println("\t\t\t --- Dropping all env transitions ---");
        environment.resetEnvironment();
        stats.restartingTimer.stop();

        environment.resetEnvironment();
        for (int i=0; i<reachedSets.length;i++){
          assert reachedSets[i].getReached().size()==1;
        }
      } else {

        /* System.out.println("Wait lists after refinement");
        for (int i=0; i<reachedSets.length; i++){
          System.out.println("For thread "+i);
          for (AbstractElement element : reachedSets[i].getWaitlist()){
            ARTElement artElement = (ARTElement) element;
            System.out.println("\t - id:"+artElement.getElementId());
          }
        }*/

        // kill the env transitions that were generated in the drop ARTs
        // if they killed transitions covered some other transitions, then make them valid again
        System.out.println("\t\t\t --- Processing env transitions ---");

        if (debug){
          environment.printUnprocessedTransitions();
        }


        environment.killEnvironmetalEdges(refinementResult.keySet(), artReachedSets, refinementResult);
        // process the remaining environmental transition
        environment.processEnvTransitions(errorThr);
        /* System.out.println();
        System.out.println("Wait lists after processing environment");
        for (int i=0; i<reachedSets.length; i++){
          System.out.println("For thread "+i);
          for (AbstractElement element : reachedSets[i].getWaitlist()){
            ARTElement artElement = (ARTElement) element;
            System.out.println("\t - id:"+artElement.getElementId());
          }

        }*/

      }

      stats.totalTimer.stop();
      return true;
    } else {

      // a real error
      stats.totalTimer.stop();
      return false;
    }
  }


  /**
   * For each thread adds new interpolants to the initial element.
   * @param reachedSets
   * @param info
   * @param errorThr
   * @return
   */
  private Multimap<Integer, Pair<ARTElement, RelyGuaranteePrecision>> restartingRefinement(ReachedSet[] reachedSets, CounterexampleTraceInfo info) {

    Multimap<Integer, Pair<ARTElement, RelyGuaranteePrecision>> refinementMap = HashMultimap.create();
    // multimap : thread no -> (ART element)
    Multimap<Integer, ARTElement> artMap = HashMultimap.create();

    boolean newPredicates = false;

    if (debug){
      System.out.println("New predicates:");
    }

    // add env. predicates to precision
    for (AbstractElement aElement : info.getEnvPredicatesForRefinmentKeys()){
      assert aElement instanceof ARTElement;
      ARTElement artElement = (ARTElement) aElement;
      RelyGuaranteeAbstractElement rgElement = AbstractElements.extractElementByType(artElement, RelyGuaranteeAbstractElement.class);
      int tid = rgElement.getTid();
      CFANode loc = AbstractElements.extractLocation(artElement);
      Collection<AbstractionPredicate> preds = info.getEnvPredicatesForRefinement(aElement);

      if (!this.addEnvPredicatesGlobally){
        SetMultimap<CFANode, AbstractionPredicate> tPrec = this.rgEnvironment.getEnvPrecision()[tid];
        if (!tPrec.get(loc).containsAll(preds)){
          newPredicates = true;
          Collection<AbstractionPredicate> cNewPreds = new HashSet<AbstractionPredicate>(preds);
          cNewPreds.removeAll(tPrec.get(loc));
          if (debug){
            System.out.println("\t- env: "+loc+" -> "+cNewPreds);
          }
        }
        tPrec.putAll(loc, preds);
      } else {
        Set<AbstractionPredicate> tPrec = this.rgEnvironment.getEnvGlobalPrecision()[tid];
        if (!tPrec.containsAll(preds)){
          newPredicates = true;
          Collection<AbstractionPredicate> cNewPreds = new HashSet<AbstractionPredicate>(preds);
          cNewPreds.removeAll(tPrec);
          if (debug){
            System.out.println("\t- env: "+loc+" -> "+cNewPreds);
          }
        }
        tPrec.addAll(preds);
      }

    if (!this.rgEnvironment.getEnvPrecision()[tid].isEmpty()){
      System.out.println();
    }
    }


    // group interpolation elements  by threads
    for (AbstractElement aElement : info.getPredicatesForRefinmentKeys()){
      //Collection<AbstractionPredicate> newpreds = info.getPredicatesForRefinement(aElement);
      assert aElement instanceof ARTElement;
      ARTElement artElement = (ARTElement) aElement;
      RelyGuaranteeAbstractElement rgElement = AbstractElements.extractElementByType(artElement, RelyGuaranteeAbstractElement.class);
      int tid = rgElement.getTid();
      if (!info.getPredicatesForRefinement(aElement).isEmpty()){
        artMap.put(tid, artElement);
      }
    }


    // for every thread sum up interpolants and add it to the initial element
    for (int tid=0 ; tid < reachedSets.length; tid++){
      ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();
      // add the precision of the initial element
      ARTElement inital  = (ARTElement) reachedSets[tid].getFirstElement();
      Precision prec = reachedSets[tid].getPrecision(inital);
      RelyGuaranteePrecision rgPrecision = Precisions.extractPrecisionByType(prec, RelyGuaranteePrecision.class);
      SetMultimap<CFANode, AbstractionPredicate> oldPreds = rgPrecision.getPredicateMap();

      pmapBuilder.putAll(oldPreds);

      for (ARTElement artElement : artMap.get(tid)){
        // add the new interpolants

        Collection<AbstractionPredicate> newpreds = info.getPredicatesForRefinement(artElement);
        CFANode loc = AbstractElements.extractLocation(artElement);
        if (this.addPredicatesGlobally){
          Set<AbstractionPredicate> gpreds = rgPrecision.getGlobalPredicates();
          Set<AbstractionPredicate> ngpreds = new HashSet<AbstractionPredicate>(gpreds);
          ngpreds.addAll(newpreds);
          rgPrecision.setGlobalPredicates(ImmutableSet.copyOf(ngpreds));
        } else {
          pmapBuilder.putAll(loc, newpreds);
        }

        if (!oldPreds.get(loc).containsAll(newpreds)){
          newPredicates = true;
          Collection<AbstractionPredicate> cNewPreds = new HashSet<AbstractionPredicate>(newpreds);
          cNewPreds.removeAll(oldPreds.get(loc));
          if (debug){
            System.out.println("\t- ART: "+loc+" -> "+cNewPreds);
          }
        }
      }

      ImmutableSetMultimap<CFANode, AbstractionPredicate> newPredMap = pmapBuilder.build();
      RelyGuaranteePrecision newPrecision = new RelyGuaranteePrecision(newPredMap, rgPrecision.getGlobalPredicates());

      // for statistics check the number of predicates per location
      for (CFANode node : newPredMap.keySet()){
        stats.maxPredicatesPerLoc = Math.max(stats.maxPredicatesPerLoc, newPredMap.get(node).size());
      }

      for (ARTElement initChild : inital.getChildren()){
        refinementMap.put(tid, Pair.of(initChild, newPrecision));
      }

    }
    assert newPredicates;

    return refinementMap;
  }

  /**
   * Returns multimap mapping: thread id -> cut-off element, new precision
   */
  private Multimap<Integer, Pair<ARTElement, RelyGuaranteePrecision>> lazyRefinement(ReachedSet[] reachedSets, CounterexampleTraceInfo info, int errorThr) throws CPAException {
    // multimap : thread no -> (ART element)
    Multimap<Integer, ARTElement> artMap = HashMultimap.create();

    // group interpolation elements  by threads
    for (AbstractElement aElement : info.getPredicatesForRefinmentKeys()){
      //Collection<AbstractionPredicate> newpreds = info.getPredicatesForRefinement(aElement);
      assert aElement instanceof ARTElement;
      ARTElement artElement = (ARTElement) aElement;
      RelyGuaranteeAbstractElement rgElement = AbstractElements.extractElementByType(artElement, RelyGuaranteeAbstractElement.class);
      int tid = rgElement.getTid();
      artMap.put(tid, artElement);
    }

    Multimap<Integer, Pair<ARTElement, RelyGuaranteePrecision>> refinementMap = HashMultimap.create();

    // for every thread get cut-off elements and their new precision
    for (int tid : artMap.keySet()){
      Collection<ARTElement> artElements = artMap.get(tid);
      List<ARTNode> nodes = new Vector<ARTNode>(artElements.size());

      // find reachability relation between ART elements of the thread
      for (ARTElement artElem : artElements){
        ARTNode node = new ARTNode(artElem);
        nodes.add(node);
      }

      System.out.println();
      System.out.println("Looking for cut-off nodes.");

      List<ARTNode> covered = new Vector<ARTNode>(artElements.size());
      for (ARTNode nodeA : nodes){
        CFANode loc = AbstractElements.extractLocation(nodeA.getArtElement());
        System.out.println("Location "+loc);
        Precision prec = reachedSets[tid].getPrecision(nodeA.getArtElement());
        RelyGuaranteePrecision rgPrec = Precisions.extractPrecisionByType(prec, RelyGuaranteePrecision.class);
        SetMultimap<CFANode, AbstractionPredicate> oldPredmap = rgPrec.getPredicateMap();
        Set<AbstractionPredicate> oldPreds = oldPredmap.get(loc);
        System.out.println("Old preds "+oldPreds);

        Collection<AbstractionPredicate> newPreds = info.getPredicatesForRefinement(nodeA.getArtElement());
        System.out.println("New preds"+newPreds);
        // skip nodes that don't have interpolants
        if (!newPreds.isEmpty()){
          for  (ARTNode nodeB : nodes){
            if (nodeA != nodeB && !covered.contains(nodeB)){
              if (belongsToProperSubtree(nodeA.getArtElement(),nodeB.getArtElement())){
                System.out.println("ART element id:"+nodeA+" is above id:"+nodeB);
                nodeA.addChild(nodeB);
                covered.add(nodeB);
              }
            }
          }
        }
      }



      // ART element unreachable by other elements are the cut-off node
      Vector<ARTNode> cutoffNodes = new Vector<ARTNode>();
      for (ARTNode node : nodes){
        CFANode loc = AbstractElements.extractLocation(node.getArtElement());
        Precision prec = reachedSets[tid].getPrecision(node.getArtElement());
        RelyGuaranteePrecision rgPrec = Precisions.extractPrecisionByType(prec, RelyGuaranteePrecision.class);
        SetMultimap<CFANode, AbstractionPredicate> oldPredmap = rgPrec.getPredicateMap();
        Collection<AbstractionPredicate> newPreds = info.getPredicatesForRefinement(node.getArtElement());
        /*Set<AbstractionPredicate> oldPreds = oldPredmap.get(loc);
        Collection<AbstractionPredicate> newPreds = info.getPredicatesForRefinement(node.getArtElement());*/
        if (node.getParent() == null && !newPreds.isEmpty()){
          cutoffNodes.add(node);
        }
      }

      System.out.println();
      if (cutoffNodes.isEmpty()){
        System.out.println("Cut-off nodes:\tnone");
      } else {
        System.out.println("Cut-off nodes:");
        for (ARTNode node : cutoffNodes){
          System.out.println("\t- id:"+node.getArtElement().getElementId());
        }
      }


      // correctness assertion
      if (debug){
        assertionCutoffNodes(tid, nodes, cutoffNodes, reachedSets, info);
      }

      // get new precision for the cutoff nodes;
      // precision of a cut-off node should include the precision of the elements below

      for (ARTNode coNode : cutoffNodes) {
        boolean newPredicate = false;
        ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();

        // in the error thread, add the precision of the error element
        // TODO necessary?
        /* if (tid == errorThr){
          AbstractElement targetElement = reachedSets[errorThr].getLastElement();
          Precision oldPrecision = reachedSets[tid].getPrecision(targetElement);
          RelyGuaranteePrecision oldRgPrecision = Precisions.extractPrecisionByType(oldPrecision, RelyGuaranteePrecision.class);
          pmapBuilder.putAll(oldRgPrecision.getPredicateMap());
        }*/


        // add precision of the cut-off node
        ARTElement artCoElement = coNode.getArtElement();
        Precision oldCoPrec = reachedSets[tid].getPrecision(artCoElement);
        RelyGuaranteePrecision oldCoRgPrec = Precisions.extractPrecisionByType(oldCoPrec, RelyGuaranteePrecision.class);
        SetMultimap<CFANode, AbstractionPredicate> oldCoPredmap = oldCoRgPrec.getPredicateMap();
        pmapBuilder.putAll(oldCoPredmap);
        // add the interpolant for the cut-off node
        Collection<AbstractionPredicate> newCoPreds = info.getPredicatesForRefinement(artCoElement);
        CFANode coLoc = AbstractElements.extractLocation(artCoElement);
        pmapBuilder.putAll(coLoc, newCoPreds);

        if (debug){
          if (!oldCoPredmap.get(coLoc).containsAll(newCoPreds)){
            newPredicate = true;
            System.out.println("New predicate is in "+newCoPreds+" for "+coLoc);
          }
        }

        // add old precisions and new predicates of  the nodes below the cut-off node
        for (ARTElement artElement : coNode.getARTProperSubtree()){
          // add the new interpolants
          Collection<AbstractionPredicate> newpreds = info.getPredicatesForRefinement(artElement);
          CFANode loc = AbstractElements.extractLocation(artElement);
          pmapBuilder.putAll(loc, newpreds);

          if (debug){
            if (!oldCoPredmap.get(loc).containsAll(newpreds)){
              newPredicate = true;
              System.out.println("New predicate is in "+newpreds+" for "+loc);
            }
          }

          // TODO need it?
          Precision oldPrecision = reachedSets[tid].getPrecision(artElement);
          RelyGuaranteePrecision oldRgPrecision = Precisions.extractPrecisionByType(oldPrecision, RelyGuaranteePrecision.class);
          pmapBuilder.putAll(oldRgPrecision.getPredicateMap());
        }

        RelyGuaranteePrecision newPrecision = new RelyGuaranteePrecision(pmapBuilder.build(), new HashSet<AbstractionPredicate>());
        refinementMap.put(tid, Pair.of(artCoElement, newPrecision));

        if (debug){
          //assert newPredicate;
          System.out.println();
          System.out.println("Thread "+tid+": cut-off node id:"+coNode.getArtElement().getElementId()+" precision "+newPrecision);
        }
      }
    }

    return refinementMap;
  }

  /**
   * Checks if cut-off nodes are correct for thread tid.
   * @param pTid
   * @param pNodes
   * @param pCutoffNodes
   * @param pReachedSets
   * @param pInfo
   */
  private void assertionCutoffNodes(int tid, List<ARTNode> nodes, Vector<ARTNode> cutoffNodes, ReachedSet[] reachedSets, CounterexampleTraceInfo info) {


    // check correctnes of cutoffNodes
    for (ARTNode node : nodes){
      CFANode loc = AbstractElements.extractLocation(node.getArtElement());
      Precision prec = reachedSets[tid].getPrecision(node.getArtElement());
      RelyGuaranteePrecision rgPrec = Precisions.extractPrecisionByType(prec, RelyGuaranteePrecision.class);
      SetMultimap<CFANode, AbstractionPredicate> oldPredmap = rgPrec.getPredicateMap();
      Set<AbstractionPredicate> oldPreds = oldPredmap.get(loc);
      Collection<AbstractionPredicate> newPreds = info.getPredicatesForRefinement(node.getArtElement());
      // cut-off has some new predicates


      if (cutoffNodes.contains(node)){
        //assert (!oldPreds.containsAll(newPreds));
      } else {
        // node is independent if it has no predictates and no cut-off node can reach it
        boolean indi=false;
        if (oldPreds.containsAll(newPreds)){
          indi = true;
          for (ARTNode cutNode : cutoffNodes){
            if (belongsToProperSubtree(cutNode.getArtElement(),node.getArtElement())){
              indi = false;
              break;
            }
          }
        }

        // node belongs to some cut-off node
        boolean belongs=false;
        for (ARTNode cutNode : cutoffNodes){
          if (cutNode.getARTProperSubtree().contains(node.getArtElement())){
            belongs = true;
            break;
          }
        }
        if ((indi && belongs) || (!indi && !belongs)){
          System.out.println();
        }
        assert (indi || belongs) && (!indi || !belongs);
      }
    }
  }

  /**
   * Returns true if artElement1 lies on the path from the root to artElement2
   * @param pArtElement
   * @param pArtElement2
   * @return
   */
  private boolean belongsToProperSubtree(ARTElement artElement1, ARTElement artElement2) {

    Path cfaPath = ARTUtils.getOnePathTo(artElement2);
    List<Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement>> path = null;
    try {
      path = transformPath(cfaPath);
    } catch (CPATransferException e) {
      e.printStackTrace();
    }
    // find the highest occurence of artElement1 on the path
    boolean occursOnPath = false;
    for (Triple<ARTElement, CFANode, RelyGuaranteeAbstractElement> triple: path){
      if (triple.getFirst().equals(artElement1)){
        occursOnPath = true;
        break;
      }
      else if  (triple.getFirst().equals(artElement2)){
        break;
      }

    }
    return occursOnPath;
  }

  public void printStatitics(){
    System.out.println();
    System.out.println("RG refinement statistics:");
    if (stats != null){
      stats.printStatistics(System.out, null, null);
    }
  }
}

/**
 * Represents reachability relation between elements in an ART
 */
class ARTNode{

  private ARTElement  artElement;
  private ARTNode parent;
  private Collection<ARTNode> children;

  public ARTNode(ARTElement  artElement){
    this.artElement = artElement;
    this.parent = null;
    this.children = new HashSet<ARTNode>();
  }

  public ARTElement getArtElement() {
    return artElement;
  }

  public ARTNode getParent() {
    return parent;
  }

  public Collection<ARTNode> getChildren() {
    return children;
  }

  public void setParent(ARTNode pParent) {
    parent = pParent;
  }

  public void addChild(ARTNode child) {
    child.setParent(this);
    children.add(child);
  }

  public Set<ARTElement> getARTProperSubtree() {
    Set<ARTElement> reached = new HashSet<ARTElement>();
    for (ARTNode child : children){
      reached.add(child.artElement);
      reached.addAll(child.getARTProperSubtree());
    }
    return reached;
  }

  public String toString() {
    return ""+artElement.getElementId();
  }



}
