package org.sosy_lab.cpachecker.plugin.eclipse;

import java.util.List;

import org.sosy_lab.cpachecker.core.CPAcheckerResult;

public interface ITaskListener {
	void tasksStarted(int taskCount);
	void selectTask(Task toSelect);
	void tasksChanged(List<Task> changed);
	void tasksFinished();
	void taskStarted(Task id);
	/**
	 * Result might be null, if the task could not be executed properly!
	 * @param id
	 * @param results
	 */
	void taskFinished(Task id, CPAcheckerResult resultsOrNull);
	void taskHasPreRunError(Task t, String errorMessage);
	void tasksChanged();
	
	static class DefaultImplementation implements ITaskListener {
		@Override
		public void taskFinished(Task id, CPAcheckerResult resultsOrNull) {}
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
		@Override
		public void selectTask(Task toSelect) {}
	}

	
}
