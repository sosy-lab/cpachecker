package org.sosy_lab.cpachecker.fllesh.fql.backend.targetgraph;

import org.jgrapht.graph.MaskFunctor;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.BasicBlockEntry;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Column;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Complement;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Compose;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.ConditionEdge;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.ConditionGraph;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.DecisionEdge;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.EnclosingScopes;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Expression;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.File;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Filter;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.FilterVisitor;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Function;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.FunctionCall;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.FunctionCalls;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.FunctionEntry;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.FunctionExit;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Identity;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Intersection;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Label;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Line;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Predication;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.RegularExpression;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.SetMinus;
import org.sosy_lab.cpachecker.fllesh.fql2.ast.filter.Union;

public class FilterEvaluator {
  
  private static FilterEvaluationCache mCache = new FilterEvaluationCache();
  private Visitor mVisitor;
  
  public FilterEvaluator(TargetGraph pTargetGraph) {
    mVisitor = new Visitor(pTargetGraph);
  }
  
  public TargetGraph evaluate(Filter pFilter) {
    return pFilter.accept(mVisitor);
  }
  
  private class Visitor implements FilterVisitor<TargetGraph> {
    
    private TargetGraph mTargetGraph;
    
    public Visitor(TargetGraph pTargetGraph) {
      mTargetGraph = pTargetGraph;
    }
    
    @Override
    public TargetGraph visit(Identity pIdentity) {
      return mTargetGraph;
    }

    @Override
    public TargetGraph visit(File pFileFilter) {
      if (mCache.isCached(mTargetGraph, pFileFilter)) {
        return mCache.get(mTargetGraph, pFileFilter);
      }
      
      throw new UnsupportedOperationException();
    }

    @Override
    public TargetGraph visit(BasicBlockEntry pBasicBlockEntry) {
      if (mCache.isCached(mTargetGraph, pBasicBlockEntry)) {
        return mCache.get(mTargetGraph, pBasicBlockEntry);
      }
      
      throw new UnsupportedOperationException();
    }

    @Override
    public TargetGraph visit(ConditionEdge pConditionEdge) {
      if (mCache.isCached(mTargetGraph, pConditionEdge)) {
        return mCache.get(mTargetGraph, pConditionEdge);
      }
      
      throw new UnsupportedOperationException();
    }

    @Override
    public TargetGraph visit(ConditionGraph pConditionGraph) {
      if (mCache.isCached(mTargetGraph, pConditionGraph)) {
        return mCache.get(mTargetGraph, pConditionGraph);
      }
      
      throw new UnsupportedOperationException();
    }

    @Override
    public TargetGraph visit(DecisionEdge pDecisionEdge) {
      if (mCache.isCached(mTargetGraph, pDecisionEdge)) {
        return mCache.get(mTargetGraph, pDecisionEdge);
      }
      
      throw new UnsupportedOperationException();
    }

    @Override
    public TargetGraph visit(Line pLine) {
      if (mCache.isCached(mTargetGraph, pLine)) {
        return mCache.get(mTargetGraph, pLine);
      }
      
      MaskFunctor<Node, Edge> lMaskFunctor = new LineNumberMaskFunctor(pLine.getLine());

      TargetGraph lResultGraph = TargetGraph.applyStandardEdgeBasedFilter(mTargetGraph, lMaskFunctor);

      mCache.add(mTargetGraph, pLine, lResultGraph);

      return lResultGraph;
    }

    @Override
    public TargetGraph visit(FunctionCalls pCalls) {
      if (mCache.isCached(mTargetGraph, pCalls)) {
        return mCache.get(mTargetGraph, pCalls);
      }
      
      TargetGraph lResultGraph = TargetGraph.applyStandardEdgeBasedFilter(mTargetGraph, FunctionCallsMaskFunctor.getInstance());

      mCache.add(mTargetGraph, pCalls, lResultGraph);

      return  lResultGraph;
    }

    @Override
    public TargetGraph visit(Column pColumn) {
      if (mCache.isCached(mTargetGraph, pColumn)) {
        return mCache.get(mTargetGraph, pColumn);
      }
      
      throw new UnsupportedOperationException();
    }

    @Override
    public TargetGraph visit(Function pFunc) {
      if (mCache.isCached(mTargetGraph, pFunc)) {
        return mCache.get(mTargetGraph, pFunc);
      }
      
      TargetGraph lResultGraph = TargetGraph.applyFunctionNameFilter(mTargetGraph, pFunc.getFunctionName());

      mCache.add(mTargetGraph, pFunc, lResultGraph);
      
      return lResultGraph;
    }

    @Override
    public TargetGraph visit(FunctionCall pCall) {
      if (mCache.isCached(mTargetGraph, pCall)) {
        return mCache.get(mTargetGraph, pCall);
      }
      
      MaskFunctor<Node, Edge> lMaskFunctor = new FunctionCallMaskFunctor(pCall.getFunctionName());

      TargetGraph lResultGraph = TargetGraph.applyStandardEdgeBasedFilter(mTargetGraph, lMaskFunctor);

      mCache.add(mTargetGraph, pCall, lResultGraph);
      
      return lResultGraph;
    }

    @Override
    public TargetGraph visit(FunctionEntry pEntry) {
      if (mCache.isCached(mTargetGraph, pEntry)) {
        return mCache.get(mTargetGraph, pEntry);
      }
      
      MaskFunctor<Node, Edge> lMaskFunctor = new FunctionEntryMaskFunctor(pEntry.getFunctionName());

      TargetGraph lResultGraph = TargetGraph.applyStandardEdgeBasedFilter(mTargetGraph, lMaskFunctor);

      mCache.add(mTargetGraph, pEntry, lResultGraph);

      return lResultGraph;
    }

    @Override
    public TargetGraph visit(FunctionExit pExit) {
      if (mCache.isCached(mTargetGraph, pExit)) {
        return mCache.get(mTargetGraph, pExit);
      }
      
      throw new UnsupportedOperationException();
    }

    @Override
    public TargetGraph visit(Label pLabel) {
      if (mCache.isCached(mTargetGraph, pLabel)) {
        return mCache.get(mTargetGraph, pLabel);
      }
      
      throw new UnsupportedOperationException();
    }

    @Override
    public TargetGraph visit(Expression pExpression) {
      if (mCache.isCached(mTargetGraph, pExpression)) {
        return mCache.get(mTargetGraph, pExpression);
      }
      
      throw new UnsupportedOperationException();
    }

    @Override
    public TargetGraph visit(RegularExpression pRegularExpression) {
      if (mCache.isCached(mTargetGraph, pRegularExpression)) {
        return mCache.get(mTargetGraph, pRegularExpression);
      }
      
      throw new UnsupportedOperationException();
    }

    @Override
    public TargetGraph visit(Complement pComplement) {
      if (mCache.isCached(mTargetGraph, pComplement)) {
        return mCache.get(mTargetGraph, pComplement);
      }
      
      TargetGraph lFilteredGraph = pComplement.getFilter().accept(this);
      TargetGraph lResultGraph = TargetGraph.applyMinusFilter(mTargetGraph, lFilteredGraph);

      mCache.add(mTargetGraph, pComplement, lResultGraph);
      
      return lResultGraph;
    }

    @Override
    public TargetGraph visit(Union pUnion) {
      if (mCache.isCached(mTargetGraph, pUnion)) {
        return mCache.get(mTargetGraph, pUnion);
      }
      
      TargetGraph lFirstGraph = pUnion.getFirstFilter().accept(this);
      TargetGraph lSecondGraph = pUnion.getSecondFilter().accept(this);
      
      TargetGraph lResultGraph = TargetGraph.applyUnionFilter(lFirstGraph, lSecondGraph);
      
      mCache.add(mTargetGraph, pUnion, lResultGraph);
      
      return lResultGraph;
    }

    @Override
    public TargetGraph visit(Compose pCompose) {
      if (mCache.isCached(mTargetGraph, pCompose)) {
        return mCache.get(mTargetGraph, pCompose);
      }
      
      TargetGraph lFirstGraph = pCompose.getFilterAppliedFirst().accept(this);
      Visitor lTmpVisitor = new Visitor(lFirstGraph);
      TargetGraph lResultGraph = pCompose.getFilterAppliedSecond().accept(lTmpVisitor);
      
      mCache.add(mTargetGraph, pCompose, lResultGraph);
      
      return lResultGraph;
    }

    @Override
    public TargetGraph visit(Intersection pIntersection) {
      if (mCache.isCached(mTargetGraph, pIntersection)) {
        return mCache.get(mTargetGraph, pIntersection);
      }
      
      TargetGraph lFirstGraph = pIntersection.getFirstFilter().accept(this);
      TargetGraph lSecondGraph = pIntersection.getSecondFilter().accept(this);
      
      TargetGraph lResultGraph = TargetGraph.applyIntersectionFilter(lFirstGraph, lSecondGraph);

      mCache.add(mTargetGraph, pIntersection, lResultGraph);

      return lResultGraph;
    }

    @Override
    public TargetGraph visit(SetMinus pSetMinus) {
      if (mCache.isCached(mTargetGraph, pSetMinus)) {
        return mCache.get(mTargetGraph, pSetMinus);
      }

      TargetGraph lFirstGraph = pSetMinus.getFirstFilter().accept(this);
      TargetGraph lSecondGraph = pSetMinus.getSecondFilter().accept(this);
      TargetGraph lResultGraph = TargetGraph.applyMinusFilter(lFirstGraph, lSecondGraph);
      
      mCache.add(mTargetGraph, pSetMinus, lResultGraph);

      return lResultGraph;
    }

    @Override
    public TargetGraph visit(EnclosingScopes pEnclosingScopes) {
      if (mCache.isCached(mTargetGraph, pEnclosingScopes)) {
        return mCache.get(mTargetGraph, pEnclosingScopes);
      }
      
      throw new UnsupportedOperationException();
    }

    @Override
    public TargetGraph visit(Predication pPredication) {
      if (mCache.isCached(mTargetGraph, pPredication)) {
        return mCache.get(mTargetGraph, pPredication);
      }
      
      throw new UnsupportedOperationException();
    }
    
  }
  
}
