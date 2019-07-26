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

import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.PredefinedPolicies;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.SecurityClasses;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.InitialMapParser;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix="cpa.ifcsecurity")
public class ExplicitDependencyPrecision extends AbstractRefinableDependencyPrecision {

  private Set<Variable> trackedVariables;

  @Option(secure=true, name="scMappingFile", toUppercase=false, description="which betamapfile to use for PolicyEnforcementCPA")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path scMappingFile=Paths.get("betamap.conf");

  @Option(secure=true, name="defaultsc", toUppercase=true, description="which default SecurityClass to use for PolicyEnforcementCPA")
  private String defaultSC="LOW";


  private Map<Variable, SecurityClasses> scmap;
  private SecurityClasses defaultsc;
  private List<SecurityClasses> trackableSC;

  public ExplicitDependencyPrecision(Configuration pConfig, LogManager pLogger, CFA cfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    try {
      Field f;

      //Initial Systemmap & History Map
      InitialMapParser imapp=new InitialMapParser(pLogger,scMappingFile);
      scmap = imapp.getInitialMap();

      //Attacker-Level
      f=PredefinedPolicies.class.getField(defaultSC);
      defaultsc = (SecurityClasses)(f.get(null));

      //Trackable
      trackableSC=new ArrayList<>();
      //TODO ADD CONFIGURATION OPTION
      trackableSC.add(PredefinedPolicies.HIGH);

    } catch (NoSuchFieldException e) {
      pLogger.log(Level.WARNING, e);
    } catch (SecurityException e) {
      pLogger.log(Level.WARNING, e);
    } catch (IllegalArgumentException e) {
      pLogger.log(Level.WARNING, e);
    }catch (IllegalAccessException e) {
      pLogger.log(Level.WARNING, e);
    }


    trackedVariables = new TreeSet<>();
    Pair<Set<MemoryLocation>, Map<FunctionEntryNode, Set<MemoryLocation>>> allVariablesList =
        ReachingDefUtils.getAllVariables(cfa.getMainFunction());
    Set<MemoryLocation> allVariables = new TreeSet<>(allVariablesList.getFirst());

    for (MemoryLocation memLoc : allVariables) {
      String varNode = memLoc.getIdentifier();
      Variable var = new Variable(varNode);
      SecurityClasses sc;
      if (scmap.containsKey(var)) {
        sc = scmap.get(var);
      } else {
        sc = defaultsc;
      }
      if (trackableSC == null || trackableSC.contains(sc)) {
        pLogger.log(Level.FINE, trackableSC);
        trackedVariables.add(var);
      }
    }
    trackedVariables = ImmutableSet.copyOf(trackedVariables);
    pLogger.log(Level.FINE, trackedVariables);
  }

  public Set<Variable> getTrackedVariables() {
    return trackedVariables;
  }


  @Override
  public boolean isTracked(Variable var) {
    return trackedVariables.contains(var);
  }



  @Override
  public String toString() {
    return trackedVariables.toString();
  }

}
