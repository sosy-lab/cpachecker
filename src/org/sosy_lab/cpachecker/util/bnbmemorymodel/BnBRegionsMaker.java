/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.bnbmemorymodel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.*;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CTypeUtils;


public class BnBRegionsMaker {

  private Set<BnBRegion> regions = new HashSet<>();
  private static final String GLOBAL = "global";
  private final LogManager logger;
  private final Timer regionCreationTime = new Timer();
  private ComplexTypeFieldStatistics ctfs;

  public BnBRegionsMaker(LogManager logger) {
    this.logger = logger;
  }

  /**
   * Determines whether or not the field is in global region
   * @param parent - element's base
   * @param memberType
   * @param name - name of element
   * @return true if not global, else otherwise
   */
  public boolean notInGlobalRegion(final CType parent, final CType memberType, final String name){

    if (regions.isEmpty()){
      return false;
    }

    BnBRegion toCheck = new BnBRegionImpl(CTypeUtils.simplifyType(memberType),
                                          CTypeUtils.simplifyType(parent), name);
    return regions.contains(toCheck);
  }

  /**
   * Constructs regions using information about field usages in CFA
   * @param cfa - program CFA
   */
  public void makeRegions(final CFA cfa) throws BnBException{
    ctfs = new ComplexTypeFieldStatistics(logger);
    ctfs.findFieldsInCFA(cfa);

    regionCreationTime.start();

    Map<CType, HashMap<CType, HashSet<String>>> usedFields = ctfs.getUsedFields();
    Map<CType, HashMap<CType, HashSet<String>>> refdFields = ctfs.getRefdFields();
    Map<CType, HashSet<String>> sub;

    // removing all fields present in both maps
    for (CType basicType : refdFields.keySet()){
      if (usedFields.containsKey(basicType)){
        sub = usedFields.get(basicType);
        for (CType structType : sub.keySet()){
          Set<String> set = refdFields.get(basicType).get(structType);

          if (set != null){
            usedFields.get(basicType).get(structType).removeAll(set);
          }

        }
      }
    }

    // filling regions
    for (CType basicType : usedFields.keySet()){
      for (CType structType : usedFields.get(basicType).keySet()){

        Set<String> set = usedFields.get(basicType).get(structType);
        if (!set.isEmpty()) {
          for (String name : set){
            regions.add(new BnBRegionImpl(CTypeUtils.simplifyType(basicType),
                                          CTypeUtils.simplifyType(structType), name));
          }
        }
      }
    }

    regionCreationTime.stop();
  }

  @Override
  public String toString() {
    String result = "Regions information\n\n";

    result += "Time for region creation:    " + regionCreationTime + "\n\n";

    result += "Total number of regions:     " + regions.size() + "\n\n";

    if (!regions.isEmpty()) {
      int i = 0;
      for (BnBRegion reg : regions) {
        result += "Number: " + (i++) + '\n';
        result += reg.toString() + '\n';
      }
    } else {
      result += "Empty regions\n";
    }
    return result;
  }

  /**
   * Returns string representation of the region information and of the used/referenced fields
   * information
   * @return statistics string representation
   */
  public String statsToString(){
    String result = "\n-----------------------------------------------------\n";
    result += toString();
    result += "\n-----------------------------------------------------\n";
    result += ctfs.toString();

    return result;
  }

  /**
   * Constructs new UF name with consideration of the region
   * @param ufName - UF name to use with this CType
   * @param region - null if global or parent_name + " " + field_name
   * @return new UF name for the CType with region information
   */
  public static String getNewUfName(final String ufName, final String region){
    String result = ufName + "_";
    if (region != null){
      result += region.replace(' ', '_');
    } else {
      result += GLOBAL;
    }
    return result;
  }

}
