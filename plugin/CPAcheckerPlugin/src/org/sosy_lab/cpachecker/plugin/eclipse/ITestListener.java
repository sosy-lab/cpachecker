package org.sosy_lab.cpachecker.plugin.eclipse;

import org.sosy_lab.cpachecker.plugin.eclipse.TaskRunner.Task;

public interface ITestListener {
	void tasksStarted(int taskCount);
	void tasksFinished();
	void taskStarted(Task id);
	void taskFailed(Task id);
	void tasksChanged();
}
