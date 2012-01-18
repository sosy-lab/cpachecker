package org.sosy_lab.cpachecker.plugin.eclipse.editors.propertieseditor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.sosy_lab.cpachecker.plugin.eclipse.CPAclipse;

public class PropertiesTableEditor extends EditorPart {

	private Composite parent;
	private TableViewer propertiesTableViewer;
	private IFileEditorInput input;
	private ToolItem newButton;
	private ToolItem insertButton;
	private ToolItem delButton;
	private Color commentColor = null;
	
	private boolean dirty = false;
	
	private List<Property> data = new ArrayList<Property>();
	
	@Override
	public void init(IEditorSite pSite, IEditorInput pInput)
			throws PartInitException {
		this.input = (IFileEditorInput)pInput.getAdapter(IFileEditorInput.class);
		setInput(pInput);		
		// would be shown in a label below the tab-bar
		//setContentDescription("Tabular Editor for PropertyFiles");
		setSite(pSite);
	}

	@Override
	public void createPartControl(final Composite pParent) {
		this.parent = pParent;
		commentColor = new Color(parent.getDisplay(), new RGB(128, 0, 0));
		pParent.setData(this);
	
	    GridLayout gridLayout = new GridLayout ();
	    pParent.setLayout (gridLayout);
	    
		createTableViewer(pParent);
	    createToolBar(pParent);
	    loadFromFile();
	
	}

	private void createTableViewer(final Composite pParent) {
		this.propertiesTableViewer = new TableViewer(pParent, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		
		TableViewerColumn keyColumn = new TableViewerColumn(propertiesTableViewer, SWT.BORDER);
		keyColumn.getColumn().setText("Key");
		TableViewerColumn valueColumn = new TableViewerColumn(propertiesTableViewer, SWT.BORDER);
		valueColumn.getColumn().setText("Value");
		TableViewerColumn descColumn = new TableViewerColumn(propertiesTableViewer, SWT.BORDER);
		descColumn.getColumn().setText("Description");
		propertiesTableViewer.setContentProvider(new ArrayContentProvider());
		propertiesTableViewer.setLabelProvider(new PropertyTableLabelProvider());
		
		//propertiesTableViewer.setCellEditors(new CellEditor[] {new TextCellEditor(propertiesTableViewer.getTable())});
		
		keyColumn.setEditingSupport(new PropertyTableEditingSupport(propertiesTableViewer, 0));
		valueColumn.setEditingSupport(new PropertyTableEditingSupport(propertiesTableViewer, 1));
		
		GridData data = new GridData (SWT.FILL, SWT.FILL, true, true);
		propertiesTableViewer.getTable().setLayoutData (data);
	    propertiesTableViewer.getTable().setHeaderVisible(true);
	    propertiesTableViewer.getTable().setLinesVisible(true);
	    
	    // key column
	    ColumnLayoutData keyColumnLayout = new ColumnWeightData(40, true);
	    // value column
	    ColumnLayoutData valueColumnLayout = new ColumnWeightData(40, true);
	    ColumnLayoutData descColumnLayout = new ColumnWeightData(20, true);
	
	    // set columns in Table layout
	    
	    TableLayout layout = new TableLayout();
	    layout.addColumnData(keyColumnLayout);
	    layout.addColumnData(valueColumnLayout);
	    layout.addColumnData(descColumnLayout);
	    propertiesTableViewer.getTable().setLayout(layout);
	    propertiesTableViewer.getTable().layout();
	    
	    createToolTip(propertiesTableViewer, pParent);
	}
	private void createToolTip(final TableViewer tv, final Composite pParent) {
		// copied from: http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet125.java?view=co
		final Display display = pParent.getDisplay();
		final Shell shell = pParent.getShell();
		// Disable native tooltip
		tv.getTable().setToolTipText ("");
		
		// Implement a "fake" tooltip
		final Listener labelListener = new Listener () {
			public void handleEvent (Event event) {
				Label label = (Label)event.widget;
				Shell shell = label.getShell ();
				switch (event.type) {
					case SWT.MouseDown:
						Event e = new Event ();
						e.item = (TableItem) label.getData ("_TABLEITEM");
						// Assuming table is single select, set the selection as if
						// the mouse down event went through to the table
						if (e.item == null) {
							shell.dispose();
							tv.getTable().setFocus();
						} else {
							tv.getTable().setSelection (new TableItem [] {(TableItem) e.item});
							tv.getTable().notifyListeners (SWT.Selection, e);
							shell.dispose ();
							tv.getTable().setFocus();
						}
						break;
					case SWT.MouseExit:
						shell.dispose ();
						break;
				}
			}
		};
		
		Listener tableListener = new Listener () {
			Shell tip = null;
			Label label = null;
			public void handleEvent (Event event) {
				switch (event.type) {
					case SWT.Dispose:
					case SWT.KeyDown:
					case SWT.MouseMove: {
						if (tip == null) break;
						tip.dispose ();
						tip = null;
						label = null;
						break;
					}
					case SWT.MouseHover: {
						// find the property for the point
						Property prop = null;
						ViewerCell cell = tv.getCell(new Point (event.x, event.y));						
						if (cell != null) {
							Object o = cell.getElement();
							if (o instanceof Property) {
								prop = (Property) o;
							}
						}
							
						if (prop != null) {
							if (tip != null  && !tip.isDisposed ()) tip.dispose ();
							tip = new Shell (shell, SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
							tip.setBackground (display.getSystemColor (SWT.COLOR_INFO_BACKGROUND));
							FillLayout layout = new FillLayout ();
							layout.marginWidth = 2;
							tip.setLayout (layout);
							label = new Label (tip, SWT.NONE);
							label.setForeground (display.getSystemColor (SWT.COLOR_INFO_FOREGROUND));
							label.setBackground (display.getSystemColor (SWT.COLOR_INFO_BACKGROUND));
							//label.setData ("_TABLEITEM", item);
							
							label.setText (prop.getToolTip());
							label.addListener (SWT.MouseExit, labelListener);
							label.addListener (SWT.MouseDown, labelListener);
							Point size = tip.computeSize (SWT.DEFAULT, SWT.DEFAULT);
							Rectangle rect = cell.getBounds();
							Point pt = tv.getTable().toDisplay (rect.x, rect.y);
							tip.setBounds (pt.x, pt.y, size.x, size.y);
							tip.setVisible (true);
						}
					}
				}
			}
		};
		tv.getTable().addListener (SWT.Dispose, tableListener);
		tv.getTable().addListener (SWT.KeyDown, tableListener);
		tv.getTable().addListener (SWT.MouseMove, tableListener);
		tv.getTable().addListener (SWT.MouseHover, tableListener);
	}

	private void createToolBar(final Composite pParent) {
		GridData data;
		ToolBar bar = new ToolBar (pParent, SWT.NULL);
	    data = new GridData ();
	    //data.widthHint = 105;
	    data.horizontalAlignment = SWT.LEFT;
	    data.verticalAlignment = SWT.BOTTOM;
	    bar.setLayoutData(data);

	    newButton = new ToolItem(bar, SWT.PUSH);
	    newButton.setText("Append Line");
	    
	    newButton.addSelectionListener(new SelectionAdapter() {
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		PropertiesTableEditor.this.data.add(new Property("newKey", "newValue"));
	    		PropertiesTableEditor.this.refresh();
	    		super.widgetSelected(e);
	    	}
		});
	    insertButton = new ToolItem (bar, SWT.PUSH);
	    insertButton.setText("insert new Line");
	    insertButton.addSelectionListener(new SelectionAdapter() {
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
		    	if (propertiesTableViewer.getSelection() instanceof IStructuredSelection) {
	    			IStructuredSelection selection = (IStructuredSelection) propertiesTableViewer.getSelection();
	    			Object first = selection.getFirstElement();
	    			int index = PropertiesTableEditor.this.data.indexOf(first);
	    			if (index < 0) {
	    				index = PropertiesTableEditor.this.data.size();
	    			}
	    			PropertiesTableEditor.this.data.add(index, new Property("",""));
	    			PropertiesTableEditor.this.refresh();
	    		}
	    		super.widgetSelected(e);
	    	}
		});
	    delButton = new ToolItem (bar, SWT.PUSH);
	    delButton.setText("Remove Line");
	    delButton.addSelectionListener(new SelectionAdapter() {
			@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		if (propertiesTableViewer.getSelection() instanceof IStructuredSelection) {
	    			IStructuredSelection selection = (IStructuredSelection) propertiesTableViewer.getSelection();
	    			for (Object selectedElement : selection.toList()) {
	    				if (selectedElement instanceof Property) {
	    					PropertiesTableEditor.this.data.remove(selectedElement);
	    				}
	    			}
	    			PropertiesTableEditor.this.refresh();
	    		}
	    		super.widgetSelected(e);
	    	}
		});
	}

	/** Stores the properties of this class in  the file, sets dirty to false.
	 * @param ioFile
	 * @throws IOException 
	 * @throws IOException
	 */
	private void storeToFile(File ioFile) throws IOException {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(ioFile));
			for (Property p : data) {
				writer.write(p.getFileRepresentation());
				writer.newLine();
			}
			/*
			Properties prop = new Properties();
			for (Property p : data) {
				prop.setProperty(p.getKey(), p.getValue());
			}
			prop.store(new FileWriter(ioFile), "Stored by the PropertiesEditor of CPAclipse");
			*/
			this.dirty = false;
			firePropertyChange(PROP_DIRTY);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	private void loadFromFile() {
		File ioFile = this.input.getFile().getLocation().toFile();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(ioFile));
			String line = null;
			while ((line = reader.readLine()) != null ) {
				line = line.trim();
				while (line.endsWith("\\")) {
					//line.substring(0, line.length()-1);//strip "\"
					line = line + "\n";
					String nextline = reader.readLine(); 
					// no endless loop because 1 char has been removed
					if (nextline != null) line = line + nextline;
				}
				// line holds one "locical" line now
				//do something with unicode normalization here (use java.text.Normalizer ?)
				// not necessary, the symbols we use here should be entered in normal form
				
				if (line.equals("")) {
					data.add(new Property("", ""));
				} else if (line.startsWith("!")) {
					data.add(new Property(line, ""));
				} else if (line.startsWith("#")) {
					data.add(new Property(line, ""));
				} else {
					if (line.contains("=")) {
						String[] parts = line.split("=", 2);
						assert parts.length == 2;
						data.add(new Property(parts[0], parts[1]));
					} else if (line.contains(":")) {
						String[] parts = line.split(":", 2);
						assert parts.length == 2;
						data.add(new Property(parts[0], parts[1]));
					} else {
						String[] parts = line.split(" ", 2);
						assert parts.length == 2;
						data.add(new Property(parts[0], parts[1]));
					}
				}
			}
		} catch (FileNotFoundException e) {
			CPAclipse.logError(e);
			return;
		} catch (IOException e) {
			CPAclipse.logError(e);
			return;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}
		
		/*Properties prop = new Properties();
		try {
			prop.load(new FileReader(ioFile));
			for (Entry<Object, Object> e : prop.entrySet()) {
				this.data.add(new Property(e.getKey().toString(), e.getValue().toString()));
			}
		} catch (FileNotFoundException e) {
			CPAclipse.logError(e);
		} catch (IOException e) {
			CPAclipse.logError(e);
		}
		*/
		refresh();
	}

	@Override
	public void doSave(IProgressMonitor pMonitor) {
		try {
			storeToFile(this.input.getFile().getLocation().toFile());
			this.input.getFile().refreshLocal(IResource.DEPTH_ZERO, pMonitor);
		} catch (IOException e) {
			pMonitor.setCanceled(true);
			CPAclipse.logError("Could not store Properties", e);
		} catch (CoreException e) {
			CPAclipse.logError("Could not store Properties", e);
		}
	}
	
	@Override
	public void doSaveAs() {
		// copied & modified from org.eclipse.ui.texteditor.AbstractDecoratedTextEditor.performSaveAs(IProgressMonitor)
		Shell shell= getSite().getShell();
		final IEditorInput input= getEditorInput();
		final IEditorInput newInput;

		if (input instanceof IURIEditorInput && !(input instanceof IFileEditorInput)) {
			FileDialog dialog= new FileDialog(shell, SWT.SAVE);
			IPath oldPath= URIUtil.toPath(((IURIEditorInput)input).getURI());
			if (oldPath != null) {
				dialog.setFileName(oldPath.lastSegment());
				dialog.setFilterPath(oldPath.toOSString());
			}

			String path= dialog.open();
			if (path == null) {
				return;
			}

			// Check whether file exists and if so, confirm overwrite
			final File localFile= new File(path);
			if (localFile.exists()) {
		        MessageDialog overwriteDialog= new MessageDialog(
		        		shell,
		        		"Overwrite?",
		        		null,
		        		"Overwrite the existing file?",
		        		MessageDialog.WARNING,
		        		new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
		        		1); // 'No' is the default
				if (overwriteDialog.open() != Window.OK) {
					return;
				}
			}
			try {
				this.storeToFile(localFile);
			} catch (IOException e) {
				CPAclipse.logError("Could not store Properties", e);
			}

		} else {
			SaveAsDialog dialog= new SaveAsDialog(shell);

			IFile original= (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
			if (original != null)
				dialog.setOriginalFile(original);
			dialog.create();
			if (original == null || !original.exists()) {
				String message= "The original file has been deleted or is not accessible";
				dialog.setErrorMessage(null);
				dialog.setMessage(message, IMessageProvider.WARNING);
			}
			if (dialog.open() == Window.CANCEL) {
				return;
			}
			IPath filePath= dialog.getResult();
			if (filePath == null) {
				return;
			}
			IWorkspace workspace= ResourcesPlugin.getWorkspace();
			final IFile file= workspace.getRoot().getFile(filePath);
			newInput= new FileEditorInput(file);

			try {
		        org.eclipse.ui.actions.WorkspaceModifyOperation operation = new org.eclipse.ui.actions.WorkspaceModifyOperation() {
		        	@Override
		            public void execute(IProgressMonitor pm) throws CoreException {
		        		try {
							storeToFile(file.getLocation().toFile());
							file.refreshLocal(IResource.DEPTH_ZERO, pm);
		        		} catch (IOException e) {
		    				CPAclipse.logError("Could not store Properties", e);
		        		}
		            }
		        };
	            new org.eclipse.jface.dialogs.ProgressMonitorDialog(getSite().getShell()).run(false, false, operation);
			} catch (InvocationTargetException e) {
				CPAclipse.logError("Could not store Properties", e);
			} catch (InterruptedException e) {
				CPAclipse.logError("Could not store Properties", e);
			}
			this.setInput(newInput);
		}
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		this.setPartName(input.getName());
		if (parent != null && !parent.isDisposed())
			parent.update();
		firePropertyChange(PROP_DIRTY);
	}
	@Override
	public boolean isDirty() {
		return dirty;
	}

	private void refresh() {
		this.propertiesTableViewer.setInput(this.data);
		//this.propertiesTableViewer.refresh(true);
	}
	@Override
	public void dispose() {
		if (commentColor != null) {
			this.commentColor.dispose();
		}
	}
	@Override
	public void setFocus() {
		this.propertiesTableViewer.getTable().setFocus();
	}
	
	private class PropertyTableEditingSupport extends EditingSupport {
		int column;
		private TextCellEditor freeTextEditor;

		private PropertyTableEditingSupport(ColumnViewer viewer, int pColumn) {
			super(viewer);
			freeTextEditor = new TextCellEditor(((TableViewer) viewer).getTable());
			this.column = pColumn;
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (element == null || value == null) {
				return;
			}
			if (element instanceof Property && value instanceof String) {
				Property p = (Property)element;
				String str = (String) value;
				if (column == 0) {
					if (p.getKey().equals(value)) {
						return;
					} else {
						p.setKey(str);
					}
				} else if (column == 1) {
					if (p.getValue().equals(value)) {
						return;
					} else {
						p.setValue(str);
					}
				} else {
					return;
				}
				PropertiesTableEditor.this.dirty = true;
				PropertiesTableEditor.this.firePropertyChange(PROP_DIRTY);
			} else {
				CPAclipse.logInfo(this.getClass().getName() + ".setValue() : could not set value " + value.getClass() + " in Object " + element.getClass());
			}
			getViewer().update(element, null);
		}

		@Override
		protected Object getValue(Object element) {
			if (element instanceof Property) {
				if (column == 0) {
					return ((Property)element).getKey();
				} else if (column == 1) {
					return ((Property)element).getValue();
				}
			}
			return element.toString();
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			/*if (element instanceof Property && column == 1) {
				final Property prop = (Property) element;
				String[] values = prop.getPossibleValues();
				boolean free = false;
				for (int i = 0; i < values.length; i++) {
					if (values[i].equals("")) {
						free = true;
						break;
					}
				}
				if (! free) {
					freeTextEditor.setValidator(new CellValidator(values));
				}
			}*/
			return freeTextEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
	}
/*	class CellValidator implements ICellEditorValidator {					
		String[] possibleValues;
		public CellValidator(String[] possibleValues) {
			super();
			this.possibleValues = possibleValues;
		}

		@Override
		public String isValid(Object value) {
			for (int i = 0; i < possibleValues.length; i++) {
				if (possibleValues[i].equals(value)) {
					return null; // valid
				}
			}
			// failure
			String message = "Valid Values: \n";
			for (int i = 0; i < possibleValues.length; i++) {
				if (possibleValues[i].equals(value)) {
					message = message + possibleValues[i];
				}
			}
			MessageDialog dialog = new MessageDialog(
					PropertiesTableEditor.this.parent.getShell(), 
					"Could not insert the value", 
					null, 
					message, 
					SWT.ICON_WARNING,
					new String[] {"OK"}, 0);
			dialog.setBlockOnOpen(false);
			dialog.open();
			return message;
		}
	}*/

	private class PropertyTableLabelProvider implements ITableLabelProvider, ITableColorProvider {
		List<ILabelProviderListener> listeners = new ArrayList<ILabelProviderListener>(4);

		@Override
		public void removeListener(ILabelProviderListener listener) {
			listeners.remove(listener);
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
			listeners.add(listener);
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (columnIndex == 0) {
				if (element instanceof Property) {
					return ((Property)element).getKey();
				} else {
					return element.toString();
				}
			} else if (columnIndex == 1) {
				if (element instanceof Property) {
					return ((Property)element).getValue();
				} else {
					return element.toString();
				}
			} else {
				if (element instanceof Property) {
					return ((Property)element).getDescription();
				} else {
					return element.toString();
				}
			}
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			
			if (element instanceof Property) {
				if (((Property)element).isComment()) {
					return PropertiesTableEditor.this.commentColor;
				}
			}
			return null;
		}
	}
	
}
