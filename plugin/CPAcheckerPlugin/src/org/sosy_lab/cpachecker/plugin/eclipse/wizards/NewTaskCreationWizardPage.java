package org.sosy_lab.cpachecker.plugin.eclipse.wizards;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NewTaskCreationWizardPage extends WizardPage {
	private Text sourceTextControl;
	private Text configTextControl;
	private Text nameTextControl;
	
	NewTaskCreationWizard parent;
	private Composite composite;
	private ITranslationUnit sourceFile;
	private IFile configFile;

	protected NewTaskCreationWizardPage(String pageName, NewTaskCreationWizard newTaskWizard) {
		super(pageName);
		parent = newTaskWizard;
	}

	@Override
	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		
		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText("Task Name:");
		
		nameTextControl = new Text(composite, SWT.BORDER);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		nameTextControl.setLayoutData(gridData);
		nameTextControl.setText("default name");
		
		Label configLabel = new Label(composite, SWT.NONE);
		configLabel.setText("Config File:");
		
		configTextControl = new Text(composite, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		configTextControl.setLayoutData(gridData);
		configTextControl.setText("TestProjekt/src/setuid.properties");
		
		Label sourceLabel = new Label(composite, SWT.NONE);
		sourceLabel.setText("Source File:");
		gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		sourceLabel.setLayoutData(gridData);
		
		sourceTextControl = new Text(composite, SWT.BORDER | SWT.WRAP | SWT.MULTI);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessVerticalSpace = true;
		sourceTextControl.setLayoutData(gridData);
		sourceTextControl.setText("src/simple_setuid_test.c");
		sourceTextControl.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				NewTaskCreationWizardPage.this.obtainSourceFile(sourceTextControl.getText());
				
			}
			@Override
			public void focusGained(FocusEvent e) {
				// nothing
			}
		});
		configTextControl.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				NewTaskCreationWizardPage.this.obtainConfigFile(configTextControl.getText());
				
			}
			@Override
			public void focusGained(FocusEvent e) {
				// nothing
			}
		});
		super.setControl(composite);
	}
	
	IFile getConfigFile() {
		if (configFile == null) {
			obtainConfigFile(this.configTextControl.getText());
		}
		return configFile;
	}
	
	private void setITranslationUnit(ITranslationUnit source) {
		this.sourceFile = source;
	}
	
	public ITranslationUnit getITranslationUnit() {
		if (this.sourceFile == null) {
			obtainSourceFile(this.sourceTextControl.getText());
		}
		return this.sourceFile;
	}

	public String getTaskName() {
		return nameTextControl.getText();
	}
	private boolean obtainConfigFile(String configFilePath) {
		IWorkspaceRoot fWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IResource member = fWorkspaceRoot.findMember(configFilePath);
		if (member == null || !member.exists() || !(member.getType() == IResource.FILE)) {
			this.setErrorMessage("Failed to locate the file " + configFilePath);
			return false;
		} else {
			IFile file = (IFile)member;
			this.configFile = file;
			this.setMessage("ConfigFile is now: " + member.getFullPath().toPortableString());
			return true;
		}
	}
	
	
	private boolean obtainSourceFile(String sourceFilePath) {
		IWorkspaceRoot fWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
	    ICModel fInput = CoreModel.create(fWorkspaceRoot);
	    
		ITranslationUnit tu = null;
		try {
			ICProject[] cProjects = fInput.getCProjects();
			IProject[] projectsToLookIn = new IProject[cProjects.length];
			for (int i = 0; i < cProjects.length; i++) {
				projectsToLookIn[i] = cProjects[i].getProject();
			}
			
			IFile ifile = projectsToLookIn[0].getFile(sourceFilePath);
			tu = CoreModelUtil.findTranslationUnit(ifile);
		} catch (CModelException e) {
			this.setErrorMessage("Exception in the CModel: " + e.getLocalizedMessage());
			return false;
		} catch (Exception e) {
			this.setErrorMessage("Failed to locate the file " + sourceFilePath);
			return false;
		}
		if (tu == null) {
			this.setErrorMessage("Could not find the c source file " + sourceFilePath);
			return false;
		} else {
			this.setITranslationUnit(tu);
			this.setErrorMessage(null);
			this.setMessage("Source is " + tu.getPath() + " in Project " + tu.getCProject().getElementName());
			return true;
		}
		
	}
}
