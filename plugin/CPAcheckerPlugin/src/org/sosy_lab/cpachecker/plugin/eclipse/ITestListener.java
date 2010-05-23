package org.sosy_lab.cpachecker.plugin.eclipse;

import java.util.List;

import org.sosy_lab.cpachecker.core.CPAcheckerResult;

public interface ITestListener {
	void tasksStarted(int taskCount);
	void tasksChanged(List<Task> changed);
	void tasksFinished();
	void taskStarted(Task id);
	void taskFinished(Task id, CPAcheckerResult results);
	void taskHasPreRunError(Task t, String errorMessage);
	void tasksChanged();
	
	static class DefaultImplementation implements ITestListener {
		@Override
		public void taskFinished(Task id, CPAcheckerResult results) {}
		@Override
		public void taskHasPreRunError(Task t, String errorMessage) {}
		@Override
		public void taskStarted(Task id) {}
		@Override
		public void tasksChanged(List<Task> changed) {}
		@Override
		public void tasksFinished() {}
		@Override
		public void tasksStarted(int taskCount) {}
		@Override
		public void tasksChanged() {}
	}

	
}
