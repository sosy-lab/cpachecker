/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.rcucpa.cpalias;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;

public class AliasState implements LatticeAbstractState<AliasState> {
  private Map<AbstractIdentifier, Set<AbstractIdentifier>> alias;
  private Map<AbstractIdentifier, Set<AbstractIdentifier>> pointsTo;
  private Set<AbstractIdentifier> rcu;

  AliasState(
      Map<AbstractIdentifier, Set<AbstractIdentifier>> palias,
      Map<AbstractIdentifier, Set<AbstractIdentifier>> ppointsTo,
      Set<AbstractIdentifier> prcu) {
    alias = palias;
    pointsTo = ppointsTo;
    rcu = prcu;
  }

  static void addToRCU(AliasState pResult, AbstractIdentifier pId, LogManager pLogger){
    Set<AbstractIdentifier> old = new HashSet<>(pResult.rcu);
    pResult.rcu.add(pId);
    if (!old.containsAll(pResult.rcu)) {
      Set<AbstractIdentifier> alias = pResult.alias.get(pId);
      pLogger.log(Level.ALL, "Added synonyms for <" + (pId == null ? "NULL" : pId.toString()) +
          "> which are: " + (alias == null? "NULL" : alias.toString()));
      if (alias != null && !alias.isEmpty()) {
        for (AbstractIdentifier ai : alias) {
          addToRCU(pResult, ai, pLogger);
        }
      }
      for (AbstractIdentifier key : pResult.alias.keySet()) {
        if (pResult.alias.get(key).contains(pId)) {
          addToRCU(pResult, key, pLogger);
        }
      }
    }
  }

  void addAlias(AbstractIdentifier key, AbstractIdentifier value, LogManager logger) {
    if (!this.alias.containsKey(key)) {
      this.alias.put(key, new HashSet<>());
    }
    logger.log(Level.ALL, value != null ? value.toString() + ' ' + value.getDereference() :
                          "Value NULL");
    if (value != null && value.getDereference() >= 0 ) {
      this.alias.get(key).add(value);
      logger.log(Level.ALL, "Added alias <" + (value == null? "NULL" : value.toString()) + "> for "
          + "key <" + key.toString() + ">");
    }
  }

  void addPointsTo(AbstractIdentifier key, AbstractIdentifier value, LogManager logger) {
    if (!this.pointsTo.containsKey(key)) {
      this.pointsTo.put(key, new HashSet<>());
    }
    if (value != null) {
      if (value.isPointer()) {
        if (this.pointsTo.containsKey(value)) {
          // p = q
          this.pointsTo.get(key).addAll(this.pointsTo.get(value));
          updateAlias(key);
        } else {
          // p = &a
          AbstractIdentifier buf = value;
          //buf.setDereference(buf.getDereference() + 1);
          logger.log(Level.ALL, "Here");
          this.pointsTo.get(key).add(buf);
          updateAlias(key);
        }

        logger.log(Level.ALL, "Added point-to <" + (value == null? "NULL" : value.toString()) + "> "
            + "for "
            + "key <" + key.toString() + ">");
      }
    }
  }

  private void updateAlias(AbstractIdentifier key) {
    Set<AbstractIdentifier> buf;
    for (AbstractIdentifier other: this.pointsTo.keySet()) {
      if (!other.equals(key)) {
        buf = new HashSet<>(this.pointsTo.get(other));
        buf.retainAll(this.pointsTo.get(key));
        if (!buf.isEmpty()) {
          this.alias.get(key).add(other);
          this.alias.get(other).add(key);
        }
      }
    }

    for (AbstractIdentifier other: this.alias.keySet()) {
      if (!other.equals(key)) {
        buf = new HashSet<>(this.alias.get(other));
        buf.retainAll(this.alias.get(key));
        if (!buf.isEmpty()) {
          this.alias.get(key).add(other);
          this.alias.get(other).add(key);
        }
      }
    }

    // pp1 -> [p1], pp2 -> [p2], p1 = [a], p2 = [a] ==> pp1 = [pp2], pp2 = [pp1]

  }

  void clearAlias(AbstractIdentifier key) {
    this.alias.get(key).clear();
  }

  void clearPointsTo(AbstractIdentifier key) {this.pointsTo.get(key).clear();}

  @Override
  public AliasState join(AliasState other) {
    Map<AbstractIdentifier, Set<AbstractIdentifier>> alias;
    Map<AbstractIdentifier, Set<AbstractIdentifier>> pointsTo;
    Set<AbstractIdentifier> rcu;

    alias = mergeMaps(this.alias, other.alias);
    pointsTo = mergeMaps(this.pointsTo, other.pointsTo);

    rcu = new HashSet<>(this.rcu);
    rcu.addAll(other.rcu);

    AliasState newState = new AliasState(alias, pointsTo, rcu);
    if (newState.equals(this)){
      return this;
    } else {
      return newState;
    }
  }

  private Map<AbstractIdentifier, Set<AbstractIdentifier>> mergeMaps(
      Map<AbstractIdentifier, Set<AbstractIdentifier>> one,
      Map<AbstractIdentifier, Set<AbstractIdentifier>> other) {

    Map<AbstractIdentifier, Set<AbstractIdentifier>> result = new HashMap<>();

    for (AbstractIdentifier id : one.keySet()) {
      result.put(id, one.get(id));
      result.get(id).addAll(other.get(id));
    }

    for (AbstractIdentifier id : other.keySet()) {
      if (!one.containsKey(id)) {
        result.put(id, other.get(id));
      }
    }

    return result;
  }

  @Override
  public boolean isLessOrEqual(AliasState other)
      throws CPAException, InterruptedException {
    boolean sameAlias = false;
    boolean samePointsTo = false;
    boolean sameRcu = false;

    if (this.alias.isEmpty() && other.alias.isEmpty()) {
      sameAlias = true;
    } else if (other.alias.keySet().containsAll(this.alias.keySet()) &&
        containsAll(other.alias, this.alias)) {
      sameAlias = true;
    }

    if (this.pointsTo.isEmpty() && other.pointsTo.isEmpty()) {
      samePointsTo = true;
    } else if (other.pointsTo.keySet().containsAll(this.pointsTo.keySet()) &&
        containsAll(other.pointsTo, this.pointsTo)){
      samePointsTo = true;
    }

    if (other.rcu.isEmpty() && this.rcu.isEmpty()) {
      sameRcu = true;
    } else if (other.rcu.containsAll(this.rcu)) {
      sameRcu = true;
    }

    return sameAlias && samePointsTo && sameRcu;
  }

  private boolean containsAll(
      Map<AbstractIdentifier, Set<AbstractIdentifier>> greater,
      Map<AbstractIdentifier, Set<AbstractIdentifier>> lesser) {
    for (AbstractIdentifier id : lesser.keySet()) {
      if (!greater.get(id).containsAll(lesser.get(id))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }

    AliasState that = (AliasState) pO;

    if (!alias.equals(that.alias)) {
      return false;
    }

    if (!pointsTo.equals(that.pointsTo)) {
      return false;
    }

    if (!rcu.equals(that.rcu)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = alias.hashCode();
    result = 31 * result + rcu.hashCode();
    return result;
  }

  public String getContents() {
    return "\nAlias: " + alias.toString()
        + "\nPoints-To: " + pointsTo.toString()
        + "\nRCU: " + rcu.toString();
  }

  @Override
  public String toString() {
    return getContents();
  }

  public Precision getPrecision() {
    return new AliasPrecision(rcu);
  }
}
