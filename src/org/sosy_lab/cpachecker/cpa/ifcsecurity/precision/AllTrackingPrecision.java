/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.precision;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.BottomPolicy;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.ConglomeratePolicy;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.Edge;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.PolicyAlgebra;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.PredefinedPolicies;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.SecurityClasses;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.ImmediateChecksParser;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.InitialMapParser;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.SetUtil;

@Options(prefix="cpa.ifcsecurity")
public class AllTrackingPrecision extends AbstractRefinableDependencyPrecision implements DepPrecision,Serializable {
  //TODO Make Immutable except for Dependency Exceptions (CEGAR)

  private static final long serialVersionUID = 6168063322071826399L;

  @Option(secure=true, name="scMappingFile", toUppercase=false, description="which betamapfile to use for PolicyEnforcementCPA")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private transient Path scMappingFile=Paths.get("betamap.conf");

  @Option(secure=true, name="defaultsc", toUppercase=true, description="which default SecurityClass to use for PolicyEnforcementCPA")
  private String defaultSCname="LOW";

  @Option(secure=true, name="policy", toUppercase=true, description="which policy to use for PolicyEnforcementCPA")
  private String policyname="HILO";

  @Option(secure=true, name="immediatechecksfile", toUppercase=false, description="which immediatechecksfile to use for PolicyEnforcementCPA")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private transient Path immediatechecksfile=Paths.get("immediatechecks.conf");

  private Map<Variable, SecurityClasses> scmap;
  private SecurityClasses defaultSC;
  private List<SecurityClasses> trackingSC;
  private List<SecurityClasses> trackableSC;
  private ConglomeratePolicy<SecurityClasses> policy;
  private SortedSet<Edge<SecurityClasses>> nset;
  private SortedSet<Variable> alwaysViolation;


  @Override
  public SortedSet<Variable> getAlwaysViolation() {
    return alwaysViolation;
  }

  public Map<Variable, SecurityClasses> getScmap() {
    return scmap;
  }

  public SecurityClasses getDefaultSC() {
    return defaultSC;
  }

  public List<SecurityClasses> getTrackingSC() {
    return trackingSC;
  }

  public List<SecurityClasses> getTrackableSC() {
    return trackableSC;
  }

  public ConglomeratePolicy<SecurityClasses> getPolicy() {
    return policy;
  }

  @Override
  public SortedSet<Edge<SecurityClasses>> getNset() {
    return nset;
  }




  public AllTrackingPrecision(LogManager pLogger, String pScMappingFile,String pPolicyname, String pDefaultSCname, String pImmediatechecksfile){
    this.scMappingFile=Paths.get(pScMappingFile);
    this.defaultSCname=pDefaultSCname;
    this.policyname=pPolicyname;
    this.immediatechecksfile=Paths.get(pImmediatechecksfile);
    construction(pLogger);
  }

  private void construction(LogManager pLogger){
    try {
      Field f;
      //Get Policy
      f=PredefinedPolicies.class.getField(policyname);
      policy=(ConglomeratePolicy<SecurityClasses>)(f.get(null));
      //Trackable
      nset=computeNPol();
      //Trackable
      trackingSC=computeLeft();
      trackableSC=null;

      //Initial Systemmap & History Map
      InitialMapParser imapp=new InitialMapParser(pLogger,scMappingFile);
          scmap = imapp.getInitialMap();

      //Attacker-Level
      f=PredefinedPolicies.class.getField(defaultSCname);
      defaultSC = (SecurityClasses)(f.get(null));

      //Immediate CheckSet
      ImmediateChecksParser imcp=new ImmediateChecksParser(pLogger,immediatechecksfile);
      alwaysViolation = imcp.getSet();

        } catch (NoSuchFieldException e) {
          pLogger.log(Level.WARNING, e);
        } catch (SecurityException e) {
          pLogger.log(Level.WARNING, e);
        } catch (IllegalArgumentException e) {
          pLogger.log(Level.WARNING, e);
        }catch (IllegalAccessException e) {
          pLogger.log(Level.WARNING, e);
        }

  }



  public AllTrackingPrecision(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    pConfig.inject(this);
    construction(pLogger);
  }

  @Override
  public boolean isViolable(Variable pVar){
    SecurityClasses sc;
    if(scmap.containsKey(pVar)){
      sc=scmap.get(pVar);
    }
    else{
      sc=defaultSC;
    }
    return trackingSC.contains(sc);
  }

  @Override
  public boolean isTrackingNecessary(Variable pVar, SortedSet<Variable> pVars){
    return true;
  }

  @Override
  public boolean isTracked(Variable pVar) {
    return true;
  }

  @Override
  public SecurityClasses getSC(Variable pVar){
    SecurityClasses sc;
    if(scmap.containsKey(pVar)){
      sc=scmap.get(pVar);
    }
    else{
      sc=defaultSC;
    }
    return sc;
  }

  PolicyAlgebra alg = new PolicyAlgebra();

  private SortedSet<Edge<SecurityClasses>> computeNPol() {
    ConglomeratePolicy toppolicy = new BottomPolicy<SecurityClasses>(alg.getDomain(policy));
    SortedSet<Edge<SecurityClasses>> nset =
        SetUtil.setminus(toppolicy.getEdges(), policy.getEdges());
    return nset;
  }

  private List<SecurityClasses> computeLeft(){
    List<SecurityClasses> result=new ArrayList<>();
    SC: for(SecurityClasses sc: (SortedSet<SecurityClasses>) alg.getDomain(policy)){
      for(Edge<SecurityClasses> vioedge: nset){
        if(vioedge.getFrom().equals(sc)){
          result.add(sc);
          continue SC;
        }
      }
    }
    return result;
  }

  private List<SecurityClasses> computeRight(){
    SortedSet<SecurityClasses> result=new TreeSet<>();
      for(Edge<SecurityClasses> vioedge: nset){
        SecurityClasses sc=vioedge.getFrom();
        SortedSet<SecurityClasses> scSet=vioedge.getTo();
        //TODO CHECK IS AGGREGATION POLICY
        for(SecurityClasses sc2:scSet){
          if(!(result.contains(sc2))){
            SortedSet<SecurityClasses> tmp=new TreeSet<>();
            tmp.add(sc2);
            SortedSet<SecurityClasses> scSet2=SetUtil.setminus(scSet, tmp);
            Edge<SecurityClasses> testedge=new Edge<SecurityClasses>(sc, scSet2);
            if(!(nset.contains(testedge))){
              result.add(sc2);
            }
          }
        }
      }
    return new ArrayList<SecurityClasses>(result);
  }


  @Override
  public String toString() {
    return ""+"("+trackableSC+","+refinementInfo+")";
  }


}
