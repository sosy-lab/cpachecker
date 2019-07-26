/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.combetoutputgenerator;

import com.google.common.collect.FluentIterable;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.ControlDependencyTrackerState;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.DependencyTrackerCPA;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.DependencyTrackerState;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.combetoutputgenerator.lib.values.JsonArray;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.combetoutputgenerator.lib.values.JsonObj;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.combetoutputgenerator.lib.values.StringValue;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.ConglomeratePolicy;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.PredefinedPolicies;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.SecurityClasses;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.AllTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.DependencyPrecision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.ImplicitDependencyPrecision;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options(prefix = "cpa.ifcsecurity")
public class StatisticFile implements Statistics {

  @Option(secure = true, description = "target file to hold the exported precision")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path targetFile = Paths.get("stats.txt");

  private final DependencyTrackerCPA cpa;
  private final CFA cfa;
  private DependencyPrecision precision;
  private Configuration config;

  public StatisticFile(DependencyTrackerCPA pCpa, Configuration pConfig, CFA pCfa,
      DependencyPrecision pPrecision) throws InvalidConfigurationException {
    this.cpa = pCpa;
    this.cfa = pCfa;
    this.precision = pPrecision;
    this.config = pConfig;
    pConfig.inject(this, StatisticFile.class);
  }

  private boolean onlyLast = false;

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {

    //Analysis
    if (targetFile != null) {
      writeFile(pReached,pResult);
    }

  }

  public void writeFile(UnmodifiableReachedSet pReached,Result pResult) {


    List<AbstractState> targets =
        FluentIterable.from(pReached).filter(AbstractStates.IS_TARGET_STATE).toList();

    StatisiticInfo result=new StatisiticInfo();

    /* File-Name */
    List<Path> fileName = cfa.getFileNames();
    result.testcase=(fileName.get(0).getFileName().toString());

    /* Policy */
    ConglomeratePolicy<SecurityClasses> pol = null;
    if (precision instanceof AllTrackingPrecision) {
      pol = ((AllTrackingPrecision) precision).getPolicy();
    }
    if (precision instanceof ImplicitDependencyPrecision) {
      pol = ((ImplicitDependencyPrecision) precision).getPolicy();
    }
    if(pol.equals(PredefinedPolicies.HILOANY)){
      result.policy = ("LHI");
    }
    else
    if(pol.equals(PredefinedPolicies.CHINESEWALL)){
      result.policy = ("CW");
    }
    else{
      result.policy = (pol.toString());
    }

    /* Mapping */
    result.mapping=(fileName.get(0).getFileName().toString());

    try (Writer writer = MoreFiles.openOutputFile(targetFile, Charset.defaultCharset())) {
      writer.write(result.toString());
    } catch (IOException e) {
      cpa.getLogger().logUserException(Level.WARNING, e,
          "Could not write dep-analysis to json-file");
    }

  }

  private void mainAnalysisToJson(UnmodifiableReachedSet pReached, JsonObj topContainerR,
      Set<String> vars) {
    JsonArray usecasesR = new JsonArray();

    JsonObj analysis = new JsonObj();

    /* 2. 0. Tool-Information */
    JsonObj toolInfo=new JsonObj();
    toolInfo.add(new StringValue("toolName"), new StringValue("CPAchecker"));
    toolInfo.add(new StringValue("configuration"), new StringValue(config.toString()));

    analysis.add(new StringValue("Tool-Configuration"), toolInfo);

    /* 2. 1. Analysis Specific Information */
    JsonObj analysisSpecificInfo = new JsonObj();

    /* 2. 1. 1. Policy */
    StringValue policyR = null;
    ConglomeratePolicy<SecurityClasses> pol = null;
    if (precision instanceof AllTrackingPrecision) {
      pol = ((AllTrackingPrecision) precision).getPolicy();
    }
    if (precision instanceof ImplicitDependencyPrecision) {
      pol = ((ImplicitDependencyPrecision) precision).getPolicy();
    }
    policyR = new StringValue(pol.toString());

    analysisSpecificInfo.add(new StringValue("Policy"), policyR);

    /* 2. 1. 2 SC-Mapping */
    JsonObj scMapping = new JsonObj();

    for (String varStr : vars) {
      Variable var = new Variable(varStr);
      SecurityClasses sc = null;
      if (precision instanceof AllTrackingPrecision) {
        sc = ((AllTrackingPrecision) precision).getSC(var);
      }
      if (precision instanceof ImplicitDependencyPrecision) {
        sc = ((ImplicitDependencyPrecision) precision).getSC(var);
      }
      scMapping.add(new StringValue(varStr), new StringValue(sc.toString()));
    }

    policyR = new StringValue(pol.toString());


    analysisSpecificInfo.add(new StringValue("Mapping"), scMapping);

    analysis.add(new StringValue("Analysis-Specific"), analysisSpecificInfo);
    /* 2. 2. Uses Analyses */
    JsonArray usedAnalysesInfo = new JsonArray();
    usedAnalysesInfo.add(new StringValue("CFA"));


    ARGState argState = (ARGState) pReached.getFirstState();
    CompositeState compositState = (CompositeState) argState.getWrappedState();
    for (AbstractState aState : compositState.getWrappedStates()) {
      if (aState instanceof ControlDependencyTrackerState) {
        usedAnalysesInfo.add(new StringValue("Control-Dependency-Analysis"));
      }
      if (aState instanceof DependencyTrackerState) {
        usedAnalysesInfo.add(new StringValue("Dependency-Analysis"));
      }
      if (aState instanceof PointerState) {
        usedAnalysesInfo.add(new StringValue("Pointer-Analysis"));
      }
    }
    analysis.add(new StringValue("Used-Auxilliaries"), usedAnalysesInfo);

    /* 2. 3. Result */
    JsonObj secResult = new JsonObj();



    analysis.add(new StringValue("Violations"), secResult);

    usecasesR.add(analysis);
    topContainerR.add(new StringValue("Use-Cases"), usecasesR);
  }

  private void auxillaryAnalysesToJson(UnmodifiableReachedSet pReached, JsonObj topContainerR) {
    JsonObj usedAnalyses = new JsonObj();

    /* CFA */
    JsonObj cfaPreComputation = new JsonObj();

    writeCFAToJson(cfaPreComputation);

    usedAnalyses.add(new StringValue("CFA"), cfaPreComputation);

    /* Dep */
    JsonObj depAnalysis = new JsonObj();

    if (onlyLast) {
      writeDependencyTrackerToJson(depAnalysis, pReached.getLastState());
    } else {
      for (AbstractState reachNode : pReached) {
        writeDependencyTrackerToJson(depAnalysis, reachNode);
      }
    }

    usedAnalyses.add(new StringValue("Dep-Analysis"), depAnalysis);


    /* Control Dep */
    JsonObj controldepAnalysis = new JsonObj();

    if (onlyLast) {
      writeControlDependencyTrackerToJson(controldepAnalysis, pReached.getLastState());
    } else {
      for (AbstractState reachNode : pReached) {
        writeControlDependencyTrackerToJson(controldepAnalysis, reachNode);
      }
    }

    usedAnalyses.add(new StringValue("Control-Dep-Analysis"), controldepAnalysis);


    topContainerR.add(new StringValue("Auxilliary-Analyses"), usedAnalyses);
  }


  private void writeDependencyTrackerToJson(JsonObj analysis, AbstractState reachNode) {
    ARGState argState = (ARGState) reachNode;
    CompositeState compositState = (CompositeState) argState.getWrappedState();
    DependencyTrackerState depState = null;
    CFANode node = null;
    for (AbstractState aState : compositState.getWrappedStates()) {
      if (aState instanceof LocationState) {
        LocationState locState = (LocationState) aState;
        node = locState.getLocationNode();
      }
      if (aState instanceof DependencyTrackerState) {
        depState = (DependencyTrackerState) aState;
      }
    }
    JsonObj depJSon = depState.toJson();
    if(depJSon!=null){
      analysis.add(new StringValue(node.toString()), depJSon);
    }
  }

  private void writeControlDependencyTrackerToJson(JsonObj analysis, AbstractState reachNode) {
    ARGState argState = (ARGState) reachNode;
    CompositeState compositState = (CompositeState) argState.getWrappedState();
    ControlDependencyTrackerState condepState = null;
    CFANode node = null;
    for (AbstractState aState : compositState.getWrappedStates()) {
      if (aState instanceof LocationState) {
        LocationState locState = (LocationState) aState;
        node = locState.getLocationNode();
      }
      if (aState instanceof ControlDependencyTrackerState) {
        condepState = (ControlDependencyTrackerState) aState;
      }
    }
    StringValue condepJSon =condepState.toJson();
    if(condepJSon!=null){
      analysis.add(new StringValue(node.toString()), condepJSon);
    }
  }

  private void writeCFAToJson(JsonObj analysis){
   JsonArray cfaJson=new JsonArray();
   Collection<CFANode> nodes = cfa.getAllNodes();
   for(CFANode source: nodes){
     for(int i=0;i<source.getNumLeavingEdges();i++){

       CFAEdge edge=source.getLeavingEdge(i);
       CFANode target=edge.getSuccessor();
       JsonObj cfaEdge=new JsonObj();
       cfaEdge.add(new StringValue("Source"),new StringValue(source.toString()));
       cfaEdge.add(new StringValue("Label"),new StringValue(edge.getCode().toString()));
       cfaEdge.add(new StringValue("Target"),new StringValue(target.toString()));
       cfaJson.add(cfaEdge);
     }
   }
   analysis.add(new StringValue("Edges"), cfaJson);
  }

  private void writeCDtoJson(){
    //TODO
  }

  @Override
  public @Nullable String getName() {
    return "DependencyTrackerCPA";
  }


}
