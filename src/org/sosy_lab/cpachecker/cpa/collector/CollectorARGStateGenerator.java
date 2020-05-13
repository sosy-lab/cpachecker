/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.collector;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class CollectorARGStateGenerator {
  private final LogManager logger;
  private final UnmodifiableReachedSet reachedSet;
  private Collection<ARGState> reachedcollectionARG = new ArrayList<>();
  private ImmutableList<AbstractState> theStorage;
  private myARGState myARGState1;
  private myARGState myARGState2;
  private myARGState myARGStatetransfer;
  private ARGState convertedARGStatetransfer;
  private ARGState currentARG;
  private ARGState convertedparenttransfer;
  private ARGState newarg;
  private ARGState newarg1;
  private ARGState newarg2;
  private LinkedHashMap<ARGState,ARGState> linkedparents = new LinkedHashMap<>();
  private ImmutableList anecstors;
  private LinkedHashMap<ARGState, Boolean> linkedDestroyer = new LinkedHashMap<>();
  private LinkedHashMap<ARGState, Boolean> linkedmergepartner = new LinkedHashMap<>();
  private boolean aftermerge = false;


  public CollectorARGStateGenerator(
      LogManager pLogger,
      UnmodifiableReachedSet pReachedSet) {

    logger = pLogger;
    reachedSet = pReachedSet;


    buildGraphData(reachedSet);
    //logger.log(Level.INFO, "sonja reachedset "+ reachedSet.toString());
  }

  /** Build ARG data for all ARG states in the reached set. */
  private void buildGraphData(UnmodifiableReachedSet reached) {

   /** for (AbstractState entry : reached.asCollection()) {

      //logger.log(Level.INFO, "sonja reachedCOLLECTION singleentry "+ reachedcollectionARG.toString());
      //logger.log(Level.INFO, "sonja ENTRY "+ entry.toString());
      anecstors = ((CollectorState) entry).getAncestor();
      Boolean merged = ((CollectorState) entry).ismerged();
      if (merged){
      //if (anecstors.size()>= 2){
        //logger.log(Level.INFO, "sonja es wurde gemerged!!!!!!!");
        //logger.log(Level.INFO, "sonja ENTRY "+ entry.toString());
        myARGState1 = ((CollectorState) entry).getTestmyARG();
        myARGState2 = ((CollectorState) entry).getTestmyARG2();
        if(myARGState1 !=null && myARGState2 !=null){
          ARGState convertedARGState1 = myARGState1.getARGState();
          ARGState convertedparent1 = myARGState1.getparentARGState();
          //logger.log(Level.INFO, "sonja converted 1 !!!! " + "\n"+ convertedARGState1.toString());
          //logger.log(Level.INFO, "sonja converted_Parent 1!!!! "+ "\n"+ convertedparent1.toString());
          ARGState convertedARGState2 = myARGState2.getARGState();
          ARGState convertedparent2 = myARGState2.getparentARGState();
          //logger.log(Level.INFO, "sonja converted 2 !!!! " + "\n"+ convertedARGState2.toString());
          //logger.log(Level.INFO, "sonja converted_Parent 2!!!! "+ "\n"+ convertedparent2.toString());


          AbstractState wrappedmyARG1 = ((CollectorState) entry).getTestmyARG().getwrappedState();
          AbstractState wrappedmyARG2 = ((CollectorState) entry).getTestmyARG2().getwrappedState();

         /** if(linkedparents.containsKey(convertedparent1))
          {
            ARGState current1 =linkedparents.get(convertedparent1);
            boolean destroyed1 = convertedARGState1.isDestroyed();
            newarg1 = new ARGState(wrappedmyARG1, current1);
            //logger.log(Level.INFO, "sonja NEWARG "+ newarg.toString());
            linkedparents.put(convertedARGState1, newarg1);
            linkedDestroyer.put(newarg1,destroyed1);
            linkedmergepartner.put(newarg1,true);
            reachedcollectionARG.add(newarg1);
            //logger.log(Level.INFO, "sonja DESTROYED "+ destroyed1);
          }
          if(linkedparents.containsKey(convertedparent2))
          {
            ARGState current2 =linkedparents.get(convertedparent2);
            boolean destroyed2 = convertedARGState2.isDestroyed();
            newarg2 = new ARGState(wrappedmyARG2, current2);
            //logger.log(Level.INFO, "sonja NEWARG "+ newarg.toString());
            linkedparents.put(convertedARGState2, newarg2);
            linkedDestroyer.put(newarg2,destroyed2);
            linkedmergepartner.put(newarg2,true);
            reachedcollectionARG.add(newarg2);
            //logger.log(Level.INFO, "sonja DESTROYED2 "+ convertedARGState2.getStateId() + destroyed2);
          }**/
       /**   ARGState mergedstate = ((CollectorState) entry).getARGState();
          //logger.log(Level.INFO, "sonja mergedstate "+ mergedstate.toString());
          if(linkedparents.containsKey(convertedparent1) && linkedparents.containsKey(convertedparent2)){
            AbstractState c = mergedstate.getWrappedState();
            ARGState parent2 =linkedparents.get(convertedparent2);
            ARGState parent1 =linkedparents.get(convertedparent1);

            //hier test anfang
            ARGState current1 =linkedparents.get(convertedparent1);
            ARGState current2 =linkedparents.get(convertedparent2);
            newarg1 = new ARGState(wrappedmyARG1, current1);
            newarg2 = new ARGState(wrappedmyARG2, current2);
            reachedcollectionARG.add(newarg1);
            reachedcollectionARG.add(newarg2);

            boolean destroyed1 = convertedARGState1.isDestroyed();
            boolean destroyed2 = convertedARGState2.isDestroyed();
            linkedparents.put(convertedARGState1, newarg1);
            linkedDestroyer.put(newarg1,destroyed1);
            linkedparents.put(convertedARGState2, newarg2);
            linkedDestroyer.put(newarg2,destroyed2);
            linkedmergepartner.put(newarg2,true);
            linkedmergepartner.put(newarg1,true);
            newarg = new ARGState(c, current2);
            newarg.addParent(current1);
            //hier test ende
            //newarg = new ARGState(c, parent2);
            //newarg.addParent(parent1);
            linkedmergepartner.put(newarg, false);
            linkedparents.put(mergedstate, newarg);
            //logger.log(Level.INFO, "sonja NewARg with parents "+ newarg.toString());
            reachedcollectionARG.add(newarg);
          }
        }

      }



      myARGStatetransfer = ((CollectorState) entry).getMyARGTransfer();

      if (myARGStatetransfer != null){
        //logger.log(Level.INFO, "sonja myARGTransfer "+ myARGStatetransfer.toDOTLabel());
        convertedARGStatetransfer = myARGStatetransfer.getARGState();
        convertedparenttransfer = myARGStatetransfer.getparentARGState();
        //logger.log(Level.INFO, "sonja convertedARGTransfer "+ convertedARGStatetransfer.toString());
        //logger.log(Level.INFO, "sonja convertedARGTransfer_Parent "+ convertedparenttransfer.toString());

        AbstractState wrappedmyARG = ((CollectorState) entry).getMyARGTransfer().getwrappedState();

        if(reachedcollectionARG.size() == 0){
          newarg = new ARGState(wrappedmyARG, null);
          newarg.markExpanded();
          linkedparents.put(convertedARGStatetransfer,newarg);
        }
        else{
          //logger.log(Level.INFO, "sonja linkedparents!!!!!! "+ "\n" + linkedparents.toString());
              if(linkedparents.containsKey(convertedparenttransfer))
              {
                //logger.log(Level.INFO, "sonja current in liste!!!!!! "+ "\n" + convertedparenttransfer);
                ARGState current =linkedparents.get(convertedparenttransfer);
                //logger.log(Level.INFO, "sonja current!!!!!! "+ "\n" + current);
                newarg = new ARGState(wrappedmyARG, current);
                newarg.markExpanded();
                linkedparents.put(convertedARGStatetransfer, newarg);
            }
            else {
              logger.log(Level.INFO, "sonja sollte hier nicht herkommen!!!!! ");
            }
        }
        //logger.log(Level.INFO, "sonja NEWARG "+ newarg.toString());
        reachedcollectionARG.add(newarg);
      }

  /**    theStorage = ((CollectorState) entry).getStorage();
      //logger.log(Level.INFO, "sonja theStorage "+ theStorage);
      if(theStorage != null){
        currentARG = ((CollectorState) entry).getARGState();
        //reachedcollectionARG.add(currentARG);
      }
    }**/

    //logger.log(Level.INFO, "sonja reachedcollection "+ reachedcollectionARG.toString());

  }

  public Collection<ARGState> getCollection() { return reachedcollectionARG; }
  public LinkedHashMap<ARGState, Boolean> getDestroyed() { return linkedDestroyer; }
  public LinkedHashMap<ARGState, Boolean> getLinkedmergepartner() { return linkedmergepartner; }
  public Boolean getAftermerge(){return aftermerge;}

}

