package org.sosy_lab.cpachecker.plugin.eclipse.popup.actions;

import org.eclipse.jface.action.Action;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;
import org.sosy_lab.cpachecker.plugin.eclipse.TasksIO;

public class SaveTasksAction extends Action {
	
	public SaveTasksAction() {
		super();
		this.setText("save all tasks");
	}

	@Override
	public void run() {
		TasksIO.saveTasks(CPAclipse.getPlugin().getTasks());
		CPAclipse.getPlugin().fireTasksChanged();
	}
}
