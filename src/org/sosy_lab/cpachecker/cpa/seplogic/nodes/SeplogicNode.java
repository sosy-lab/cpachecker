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
package org.sosy_lab.cpachecker.cpa.seplogic.nodes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.parboiled.trees.ImmutableTreeNode;


public abstract class SeplogicNode extends ImmutableTreeNode<SeplogicNode> {
  public String getInternalRepr() {
    Class<? extends SeplogicNode> c = this.getClass();
    StringBuffer buf = new StringBuffer(c.getSimpleName() + "(");

    Field f[] = c.getDeclaredFields();
    for (Field field : f) {
      buf.append(field.getName());
      buf.append("=(");
      try {
        Object obj = field.get(this);
        String repr;
        if (obj instanceof SeplogicNode)
          repr = ((SeplogicNode) obj).getInternalRepr();
        else
          repr = obj.toString();
        buf.append(repr);
      } catch (IllegalArgumentException e) {
        buf.append("[IllegalArgumentException]");
      } catch (IllegalAccessException e) {
        buf.append("[IllegalAccessException]");
      }
      buf.append("), ");
    }
    buf.append(")");

    return buf.toString();
  }

  public abstract SeplogicNode accept(NodeVisitor pVisitor);

  public static interface NodeVisitor {
    SeplogicNode visitNode(BooleanValue pNode);
    SeplogicNode visitNode(Disjunction pNode);
    SeplogicNode visitNode(Empty pNode);
    SeplogicNode visitNode(Equality pNode);
    SeplogicNode visitNode(Inequality pNode);
    SeplogicNode visitNode(PurePredicate pNode);
    SeplogicNode visitNode(SeparatingConjunction pNode);
    SeplogicNode visitNode(SpatialPredicate pNode);
    SeplogicNode visitNode(OpArgument pNode);
    SeplogicNode visitNode(StringArgument pNode);
    SeplogicNode visitNode(VarArgument pNode);
    SeplogicNode visitNode(Variable pNode);
  }

  public static class IntegerEqualityArgumentExtractingNodeVisitor implements NodeVisitor {
    private Map<String, String> eqs = new HashMap<String, String>();
    private Map<String, Long> values = new HashMap<String, Long>();
    private String varName;

    public IntegerEqualityArgumentExtractingNodeVisitor(String pVarName) {
      super();
      varName = pVarName;
    }

    public Long getArg() {
      String currentVarname = varName;
      while (eqs.containsKey(currentVarname) && !values.containsKey(currentVarname))
        currentVarname = eqs.get(currentVarname);
      return values.get(currentVarname);
    }

    @Override
    public SeplogicNode visitNode(Variable pNode) {
      return null;
    }

    @Override
    public SeplogicNode visitNode(VarArgument pNode) {
      return null;
    }

    @Override
    public SeplogicNode visitNode(StringArgument pNode) {
      return null;
    }

    @Override
    public SeplogicNode visitNode(OpArgument pNode) {
      return null;
    }

    @Override
    public SeplogicNode visitNode(SpatialPredicate pNode) {
      return null;
    }

    @Override
    public SeplogicNode visitNode(SeparatingConjunction pNode) {
      pNode.f1.accept(this);
      pNode.f2.accept(this);
      return null;
    }

    @Override
    public SeplogicNode visitNode(PurePredicate pNode) {
      return null;
    }

    @Override
    public SeplogicNode visitNode(Inequality pNode) {
      return null;
    }

    @Override
    public SeplogicNode visitNode(Equality pNode) {
      Argument varArg = null, opArg = null;
      if (pNode.a1 instanceof VarArgument && pNode.a2 instanceof VarArgument) {
        eqs.put(((VarArgument) pNode.a1).var.name, ((VarArgument) pNode.a2).var.name);
        eqs.put(((VarArgument) pNode.a2).var.name, ((VarArgument) pNode.a1).var.name);
        return null;
      }
      if (pNode.a1 instanceof VarArgument) {
        varArg = pNode.a1;
        if (pNode.a2 instanceof OpArgument && ((OpArgument)pNode.a2).op.equals("numeric_const")) {
          opArg = pNode.a2;
        }
      } else if (pNode.a2 instanceof VarArgument) {
        varArg = pNode.a2;
        if (pNode.a1 instanceof OpArgument && ((OpArgument)pNode.a1).op.equals("numeric_const")) {
          opArg = pNode.a1;
        }
      }
      if (varArg == null || opArg == null)
        return null;
      assert(!values.containsKey(((VarArgument)varArg).var.name));
      values.put(((VarArgument)varArg).var.name, Long.valueOf(((StringArgument) ((OpArgument) opArg).args.get(0)).str));
      return null;
    }

    @Override
    public SeplogicNode visitNode(Empty pNode) {
      return null;
    }

    @Override
    public SeplogicNode visitNode(Disjunction pNode) {
      pNode.f1.accept(this);
      pNode.f2.accept(this);
      return null;
    }

    @Override
    public SeplogicNode visitNode(BooleanValue pNode) {
      return null;
    }
  }

  public static class RenamingNodeVisitor implements NodeVisitor {

    private String target;
    private String replacement;

    public RenamingNodeVisitor(String pTarget, String pReplacement) {
      target = pTarget;
      replacement = pReplacement;
    }

    @Override
    public SeplogicNode visitNode(BooleanValue pNode) {
      return pNode;
    }

    @Override
    public SeplogicNode visitNode(Disjunction pNode) {
      return new Disjunction((Formula) pNode.f1.accept(this), (Formula) pNode.f2.accept(this));
    }

    @Override
    public SeplogicNode visitNode(Empty pNode) {
      return pNode;
    }

    @Override
    public SeplogicNode visitNode(Equality pNode) {
      return new Equality((Argument) pNode.a1.accept(this), (Argument) pNode.a2.accept(this));
    }

    @Override
    public SeplogicNode visitNode(Inequality pNode) {
      return new Inequality((Argument) pNode.a1.accept(this), (Argument) pNode.a2.accept(this));
    }

    @Override
    public SeplogicNode visitNode(PurePredicate pNode) {
      List<Argument> newArgs = new ArrayList<Argument>();
      for (Argument arg : pNode.args) {
        newArgs.add((Argument) arg.accept(this));
      }
      return new PurePredicate(pNode.op, newArgs);
    }

    @Override
    public SeplogicNode visitNode(SeparatingConjunction pNode) {
      return new SeparatingConjunction((Formula) pNode.f1.accept(this), (Formula) pNode.f2.accept(this));
    }

    @Override
    public SeplogicNode visitNode(SpatialPredicate pNode) {
      List<Argument> newArgs = new ArrayList<Argument>();
      for (Argument arg : pNode.args) {
        newArgs.add((Argument) arg.accept(this));
      }
      return new SpatialPredicate(pNode.op, newArgs);
    }

    @Override
    public SeplogicNode visitNode(OpArgument pNode) {
      List<Argument> newArgs = new ArrayList<Argument>();
      for (Argument arg : pNode.args) {
        newArgs.add((Argument) arg.accept(this));
      }
      return new OpArgument(pNode.op, newArgs);
    }

    @Override
    public SeplogicNode visitNode(StringArgument pNode) {
      return pNode;
    }

    @Override
    public SeplogicNode visitNode(VarArgument pNode) {
      return new VarArgument((Variable) pNode.var.accept(this));
    }

    @Override
    public SeplogicNode visitNode(Variable pNode) {
      if (pNode.name.equals(target))
        return new Variable(replacement, pNode.isAny);
      return pNode;
    }

  }
}
