package org.sosy_lab.cpachecker.core.algorithm.concurrent.message.completion;

import org.sosy_lab.cpachecker.core.algorithm.concurrent.Scheduler.MessageProcessingVisitor;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.Message;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ErrorOrigin;

public class ErrorReachedProgramEntryMessage implements Message {
  private final ErrorOrigin origin;
  
  public ErrorReachedProgramEntryMessage(final ErrorOrigin pOrigin) {
    origin = pOrigin;
  }
  
  @Override
  public void accept(MessageProcessingVisitor visitor) {
    visitor.visit(this);
  }
  
  public ErrorOrigin getOrigin() {
    return origin;
  }
}
