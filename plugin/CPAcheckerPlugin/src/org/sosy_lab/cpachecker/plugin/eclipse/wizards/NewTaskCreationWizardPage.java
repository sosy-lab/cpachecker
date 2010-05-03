package org.sosy_lab.cpachecker.plugin.eclipse.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NewTaskCreationWizardPage extends WizardPage {
	Text sourceText;
	Text configText;
	
	NewTaskCreationWizard parent;
	private Composite composite;
	protected NewTaskCreationWizardPage(String pageName, NewTaskCreationWizard newTaskWizard) {
		super(pageName);
		parent = newTaskWizard;
	}

	@Override
	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		
		Label configLabel = new Label(composite, SWT.NONE);
		configLabel.setText("Config File:");
		
		configText = new Text(composite, SWT.BORDER);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		configText.setLayoutData(gridData);
		configText.setText("some_properties.properties");
		
		Label sourceLabel = new Label(composite, SWT.NONE);
		sourceLabel.setText("Source File:");
		gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		sourceLabel.setLayoutData(gridData);
		
		sourceText = new Text(composite, SWT.BORDER | SWT.WRAP | SWT.MULTI);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessVerticalSpace = true;
		sourceText.setLayoutData(gridData);
		sourceText.setText("some_source_file.c");
		
		super.setControl(composite);
	}
	
	String getConfigText() {
		return configText.getText();
	}
	
	String getSourceText() {
		return sourceText.getText();
	}
}
