package org.sosy_lab.cpachecker.plugin.eclipse;

import org.sosy_lab.cpachecker.plugin.eclipse.TaskRunner.Task;

public class Listener implements ITestListener {

	@Override
	public void taskFailed(Task id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void taskStarted(Task id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tasksFinished() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tasksStarted(int taskCount) {
		// TODO Auto-generated method stub
		System.out.println(taskCount + " Tasks started");
		
	}

	@Override
	public void tasksChanged() {
		// TODO Auto-generated method stub
		
	}

}
