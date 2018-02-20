/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.ConglomeratePolicy;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.PredefinedPolicies;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.SecurityClasses;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.ImmediateChecksParser;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.InitialMapParser;

/**
 * CPA for enforcing a Security Policy
 */
@Options(prefix="cpa.ifcsecurity")
public class PolicyEnforcementCPA implements ConfigurableProgramAnalysis {
  /*
   * Default Security Enforcer: Explicit Flow without Dependency (Access Control)
   *
   * Can Use: DependancyTracker (Explicit Flow with HWM)
   * Can Use: ControlDependencyTracker
   */
  @SuppressWarnings("unused")
  private LogManager logger;

  private AbstractDomain domain;

  private PolicyEnforcementRelation<SecurityClasses> transfer;

  @SuppressWarnings("unused")
  private CFA cfa;

  @Option(secure=true, name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="which merge operator to use for PolicyEnforcementCPA")
  private String mergeType = "JOIN";

  @Option(secure=true, name="stop", toUppercase=true, values={"SEP", "JOIN"},
      description="which stop operator to use for PolicyEnforcementCPA")
  private String stopType = "SEP";

  private StopOperator stop;
  private MergeOperator merge;

  @Option(secure=true, name="policy", toUppercase=true, description="which policy to use for PolicyEnforcementCPA")
  private String policyname="HILO";

  @Option(secure=true, name="scMappingFile", toUppercase=false, description="which betamapfile to use for PolicyEnforcementCPA")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path scMappingFile=Paths.get("betamap.conf");

  @Option(secure=true, name="immediatechecksfile", toUppercase=false, description="which immediatechecksfile to use for PolicyEnforcementCPA")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path immediatechecksfile=Paths.get("immediatechecks.conf");

  @Option(secure=true, name="defaultsc", toUppercase=true, description="which default SecurityClass to use for PolicyEnforcementCPA")
  private String defaultSC="LOW";

  @Option(secure=true, name="statestocheck", toUppercase=true, description="which states shall be checked")
  private int statestocheck=0;

  @Option(secure=true, description="defines security classes of entities")

  private Path initialmapfile = null;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PolicyEnforcementCPA.class);
  }

  @SuppressWarnings("unchecked")
  private PolicyEnforcementCPA(LogManager pLogger, Configuration pConfig, ShutdownNotifier pShutdownNotifier, CFA pCfa) throws InvalidConfigurationException {
    pConfig.inject(this);
    this.logger = pLogger;
    this.cfa=pCfa;

    domain = DelegateAbstractDomain.<PolicyEnforcementState<SecurityClasses>>getInstance();
    transfer = new PolicyEnforcementRelation<>(logger, pShutdownNotifier,statestocheck, pConfig);

    if (stopType.equals("SEP")) {
      stop = new StopSepOperator(domain);
    } else if (mergeType.equals("JOIN")) {
      stop = new StopJoinOperator(domain);
    }
    if (mergeType.equals("SEP")) {
      merge = MergeSepOperator.getInstance();
    } else if (mergeType.equals("JOIN")) {
      merge = new MergeJoinOperator(domain);
    }
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transfer;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return merge;
  }

  @Override
  public StopOperator getStopOperator() {
    return stop;
  }

  @SuppressWarnings("unchecked")
  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    PolicyEnforcementState<SecurityClasses> initialstate=new PolicyEnforcementState<>();



      try {
        Field f;
        //Get Policy
        f=PredefinedPolicies.class.getField(policyname);
        ConglomeratePolicy<SecurityClasses> policy=(ConglomeratePolicy<SecurityClasses>)(f.get(null));
        initialstate.setPolicy(policy);

        //Initial Systemmap & History Map
        InitialMapParser imapp=new InitialMapParser(logger,scMappingFile);
        Map<Variable, SecurityClasses> map = imapp.getInitialMap();

        Map<Variable, SecurityClasses> scmap=new TreeMap<>();
        Map<Variable, SortedSet<SecurityClasses>> cscmap=new TreeMap<>();
        for(Entry<Variable, SecurityClasses> entry:map.entrySet()){
          Variable key=entry.getKey();
          SecurityClasses sc=entry.getValue();
          scmap.put(key, sc);
          SortedSet<SecurityClasses> his=new TreeSet<>();
          his.add(sc);
          cscmap.put(key,his);
        }
        initialstate.setAllowedsecurityclassmapping(scmap);
        initialstate.setContentsecurityclasslevels(cscmap);

        //Immediate CheckSet
        ImmediateChecksParser imcp=new ImmediateChecksParser(logger,immediatechecksfile);
        initialstate.setImmediatecheck(imcp.getSet());

        //Attacker-Level
        f=PredefinedPolicies.class.getField(defaultSC);
        initialstate.setDefaultlevel((SecurityClasses)(f.get(null)));
      } catch (NoSuchFieldException e) {
        logger.log(Level.WARNING, e);
      } catch (SecurityException e) {
        logger.log(Level.WARNING, e);
      } catch (IllegalArgumentException e) {
        logger.log(Level.WARNING, e);
      } catch (IllegalAccessException e) {
        logger.log(Level.WARNING, e);
      }

    return  initialstate;
  }
}
