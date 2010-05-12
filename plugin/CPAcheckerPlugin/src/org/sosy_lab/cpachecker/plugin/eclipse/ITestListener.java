package org.sosy_lab.cpachecker.plugin.eclipse;

import org.sosy_lab.cpachecker.plugin.eclipse.TaskRunner.Task;

public interface ITestListener {
	void tasksStarted(int taskCount);
	void tasksFinished();
	void taskStarted(Task id);
	void taskFinished(Task id, boolean succeded);
	void tasksChanged();
}
