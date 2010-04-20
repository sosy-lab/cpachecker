package fllesh.fql.backend.testgoals;

public interface TestGoal {
  
  public <T> T accept(TestGoalVisitor<T> pVisitor);
  
}
