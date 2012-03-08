/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.waitlist;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;

/**
 * Waitlist where elements are sorted using the provided comparator.
 */
public class ComparatorWaitlist implements Waitlist {

  private final TreeSet<AbstractElement> waitlist;

  public ComparatorWaitlist(Comparator<AbstractElement> comp){
    waitlist = new TreeSet(comp);
  }

  @Override
  public Iterator<AbstractElement> iterator() {
    return waitlist.iterator();
  }

  @Override
  public void add(AbstractElement pElement) {
    waitlist.add(pElement);
  }

  @Override
  public void clear() {
    waitlist.clear();
  }

  @Override
  public boolean contains(AbstractElement pElement) {
    return waitlist.contains(pElement);
  }

  @Override
  public boolean isEmpty() {
    return waitlist.isEmpty();
  }

  @Override
  public AbstractElement pop() {
    return waitlist.pollLast();
  }

  @Override
  public boolean remove(AbstractElement pElement) {
    return waitlist.remove(pElement);
  }

  @Override
  public int size() {
    return waitlist.size();
  }


  public enum ComparatorWaitlistFactory implements WaitlistFactory {
    ART_ID_MAX {
      @Override
      public Waitlist createWaitlistInstance() {
        return new ComparatorWaitlist(new ARTElementIdMax());
      }
    },

    ART_ID_MIN {

      @Override
      public Waitlist createWaitlistInstance() {
        return new ComparatorWaitlist(new ARTElementIdMin());
      }
    },

    MIN_ENVAPP_MIN_TOP {

      @Override
      public Waitlist createWaitlistInstance() {
        return new ComparatorWaitlist(new EnvAppMinTopMin());
      }
    },

    MIN_ENVAPP_MAX_TOP {

      @Override
      public Waitlist createWaitlistInstance() {
        return new ComparatorWaitlist(new EnvAppMinTopMax());
      }
    }


  }


  public static class ARTElementIdMax implements Comparator<AbstractElement>{

    @Override
    public int compare(AbstractElement elem1, AbstractElement elem2) {
      ARTElement aelem1 = (ARTElement) elem1;
      ARTElement aelem2 = (ARTElement) elem2;
      int id1 = aelem1.getElementId();
      int id2 = aelem2.getElementId();
      return id1 > id2 ? 1 : (id1 == id2 ? 0 : -1);
    }
  }

  public static class ARTElementIdMin implements Comparator<AbstractElement>{

    @Override
    public int compare(AbstractElement elem1, AbstractElement elem2) {
      ARTElement aelem1 = (ARTElement) elem1;
      ARTElement aelem2 = (ARTElement) elem2;
      int id1 = aelem1.getElementId();
      int id2 = aelem2.getElementId();
      return id2 > id1 ? 1 : (id1 == id2 ? 0 : -1);
    }
  }

  /**
   * Minimize env. applications and then minimizes topological numbers.
   */
  public static class EnvAppMinTopMin implements Comparator<AbstractElement>{

    @Override
    public int compare(AbstractElement elem1, AbstractElement elem2) {
      ARTElement aelem1 = (ARTElement) elem1;
      ARTElement aelem2 = (ARTElement) elem2;

      if (elem1 == elem2){
        return 0;
      }

      List<Pair<ARTElement, RGEnvTransition>> eapp1 = aelem1.getEnvApplied();
      List<Pair<ARTElement, RGEnvTransition>> eapp2 = aelem2.getEnvApplied();

      // compare the number of env. applications
      if (eapp1.size() < eapp2.size()){
        return 1;
      }

      if (eapp1.size() > eapp2.size()){
        return -1;
      }

      // compare applications recursivly
      for (int i=0; i<eapp1.size(); i++){
        Pair<ARTElement, RGEnvTransition> app1 = eapp1.get(i);
        Pair<ARTElement, RGEnvTransition> app2 = eapp2.get(i);

        // choose the one with lower topological application point
        int apptop1 = app1.getFirst().retrieveLocationElement().getLocationNode()
            .getTopologicalSortId();
        int apptop2 = app2.getFirst().retrieveLocationElement().getLocationNode()
            .getTopologicalSortId();

        if (apptop1 < apptop2){
          return 1;
        }

        if (apptop1 > apptop2){
          return -1;
        }

        // compare the sources of env. transitions
        ARTElement source1 = app1.getSecond().getSourceARTElement();
        ARTElement source2 = app2.getSecond().getSourceARTElement();

        int result = compare(source1, source2);
        if (result != 0){
          return result;
        }
      }


      // env. applications are equivalent, compare topological id
      int top1 = aelem1.retrieveLocationElement().getLocationNode().getTopologicalSortId();
      int top2 = aelem2.retrieveLocationElement().getLocationNode().getTopologicalSortId();

      if (top1 < top2){
        return 1;
      }

      if (top2 < top1){
        return -1;
      }

      // compre ART element ids
      int id1 = aelem1.getElementId();
      int id2 = aelem2.getElementId();

      return id1 > id2 ? 1 : (id1 < id2) ? -1 : 0;
    }
  }


  /**
   * Minimize env. applications and maximizes then topological numbers.
   */
  public static class EnvAppMinTopMax implements Comparator<AbstractElement>{

    @Override
    public int compare(AbstractElement elem1, AbstractElement elem2) {
      ARTElement aelem1 = (ARTElement) elem1;
      ARTElement aelem2 = (ARTElement) elem2;

      if (elem1 == elem2){
        return 0;
      }

      List<Pair<ARTElement, RGEnvTransition>> eapp1 = aelem1.getEnvApplied();
      List<Pair<ARTElement, RGEnvTransition>> eapp2 = aelem2.getEnvApplied();

      // compare the number of env. applications
      if (eapp1.size() < eapp2.size()){
        return 1;
      }

      if (eapp1.size() > eapp2.size()){
        return -1;
      }

      // compare applications recursivly
      for (int i=0; i<eapp1.size(); i++){
        Pair<ARTElement, RGEnvTransition> app1 = eapp1.get(i);
        Pair<ARTElement, RGEnvTransition> app2 = eapp2.get(i);

        // choose the one with higher topological application point
        int apptop1 = app1.getFirst().retrieveLocationElement().getLocationNode()
            .getTopologicalSortId();
        int apptop2 = app2.getFirst().retrieveLocationElement().getLocationNode()
            .getTopologicalSortId();

        if (apptop1 > apptop2){
          return 1;
        }

        if (apptop1 < apptop2){
          return -1;
        }

        // compare the sources of env. transitions
        ARTElement source1 = app1.getSecond().getSourceARTElement();
        ARTElement source2 = app2.getSecond().getSourceARTElement();

        int result = compare(source1, source2);
        if (result != 0){
          return result;
        }
      }


      // env. applications are equivalent, compare topological id
      int top1 = aelem1.retrieveLocationElement().getLocationNode().getTopologicalSortId();
      int top2 = aelem2.retrieveLocationElement().getLocationNode().getTopologicalSortId();

      if (top1 > top2){
        return 1;
      }

      if (top2 > top1){
        return -1;
      }

      // compre ART element ids
      int id1 = aelem1.getElementId();
      int id2 = aelem2.getElementId();

      return id1 > id2 ? 1 : (id1 < id2) ? -1 : 0;
    }
  }


}
