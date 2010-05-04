package org.sosy_lab.cpachecker.plugin.eclipse.popup.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAcheckerPlugin;
import org.sosy_lab.cpachecker.plugin.eclipse.TaskRunner.Task;

public class RunMultipleTasksAction extends Action {

	private Shell shell;
	private List<Task> toRun;
	
	public RunMultipleTasksAction(Shell shell, List<Task> toRun) {
		super();
		this.shell = shell;
		this.toRun = toRun;
		this.setText("run selected tasks");
	}

	@Override
	public void run() {
		for (Task t :toRun) {
			System.out.println("RunMultipleTasksAction.starting: " + t.getName());
		}
				
		CPAcheckerPlugin.runTasks(toRun);
	}
}
