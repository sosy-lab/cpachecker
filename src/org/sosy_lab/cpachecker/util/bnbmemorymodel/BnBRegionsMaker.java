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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.c.CType;


public class BnBRegionsMaker {

  private Set<BnBRegionImpl> regions = new HashSet<>();
  private static final String GLOBAL = " global";
  private final LogManager logger;

  public BnBRegionsMaker(LogManager logger) {
    this.logger = logger;
  }

  /**
   * Determines whether or not the field is in global region
   * @param parent - element's base
   * @param pMemberType
   * @param name - name of element
   * @return true if global, else otherwise
   */
  public boolean isInGlobalRegion(final CType parent, CType pMemberType, final String name){

    if (regions.isEmpty()){
      return true;
    }

    BnBRegion toCheck = new BnBRegionImpl(pMemberType, parent, name);
    if (regions.contains(toCheck)){
      return false;
    }

    return true;
  }

  /**
   * Gathers information about struct field usage and constructs regions
   * @param cfa - program CFA
   */
  public void makeRegions(final CFA cfa) throws BnBException{
    ComplexTypeFieldStatistics ctfs = new ComplexTypeFieldStatistics(logger);
    ctfs.findFieldsInCFA(cfa);

    Map<CType, HashMap<CType, HashSet<String>>> usedFields = ctfs.getUsedFields();
    Map<CType, HashMap<CType, HashSet<String>>> refdFields = ctfs.getRefdFields();
    Map<CType, HashSet<String>> sub;

    // remove all fields present in both maps
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

    // fill regions
    for (CType basicType : usedFields.keySet()){
      for (CType structType : usedFields.get(basicType).keySet()){

        Set<String> set = usedFields.get(basicType).get(structType);
        if (!set.isEmpty()) {
          for (String name : set){
            regions.add(new BnBRegionImpl(basicType, structType, name));
          }
        }
      }
    }
  }

  /**
   * Writes information about regions in the specified file
   * @param filename - desired filename
   */
  public void dumpRegions(final String filename){
    File dump = new File(filename);

    try{
      FileWriter writer = new FileWriter(dump);

      String result = toString();

      writer.write(result);
      writer.close();

    } catch (IOException e){
      logger.logException(Level.WARNING, e, "Exception while writing the regions statistics");
    }
  }

  @Override
  public String toString() {
    String result = "";

    if (!regions.isEmpty()) {
      int i = 0;
      for (BnBRegionImpl reg : regions) {
        result += "Number: " + (i++) + '\n';
        result += reg.toString() + '\n';
      }
    } else {
      result += "Empty regions\n";
    }
    return result;
  }

  public static String getGlobal(){
    return GLOBAL;
  }

  /**
   * Constructs new UF name with consideration of the region
   * @param ufName - UF name to use with this CType
   * @param region - null if global or parent_name + " " + field_name
   * @return new UF name for the CType with region information
   */
  public static String getNewUfName(final String ufName, String region){
    String result = ufName + "_";
    if (region != null){
      result += region.replace(' ', '_');
    } else {
      result += GLOBAL;
    }
    return result;
  }

}
