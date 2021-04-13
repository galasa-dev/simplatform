/*
 * Licensed Materials - Property of IBM
 * 
 * (c) Copyright IBM Corp. 2019.
 */
package dev.galasa.eclipse.ui.wizards.simbank;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

public class ExampleMavenSimbankWizardPage extends WizardPage implements Listener {

    private Text            prefixNameField;
    private Label           testProjectName;
    private Label           managerProjectName;

    private static String[] invalidCharacters = { "!", "@", "£", "$", "^", "&", "*", };

    public ExampleMavenSimbankWizardPage(String pageName) {
        super(pageName);

        setTitle("Example projects for the SimBank tutorial");
        setDescription("Select a prefix that will be used to create the example projects.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setFont(parent.getFont());

        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite prefixGroup = new Composite(composite, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        prefixGroup.setLayout(layout);
        prefixGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label prefixLabel = new Label(prefixGroup, SWT.NONE);
        prefixLabel.setText("New project prefix:");
        prefixLabel.setFont(parent.getFont());

        // new project name entry field
        prefixNameField = new Text(prefixGroup, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        prefixNameField.setLayoutData(data);
        prefixNameField.setFont(parent.getFont());

        prefixNameField.setText("dev.galasa.simbank");
        prefixNameField.addListener(SWT.Modify, this);

        Composite resultGroup = new Composite(composite, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 1;
        resultGroup.setLayout(layout);
        resultGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label resultLabel = new Label(resultGroup, SWT.NONE);
        resultLabel.setText("Will create the following projects in your workspace:-");
        resultLabel.setFont(parent.getFont());

        Composite projectsGroup = new Composite(composite, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 2;
        projectsGroup.setLayout(layout);
        projectsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label testLabel = new Label(projectsGroup, SWT.NONE);
        testLabel.setText("Tests project:");
        testLabel.setFont(parent.getFont());

        testProjectName = new Label(projectsGroup, SWT.NONE);
        testProjectName.setFont(parent.getFont());

        Label managerLabel = new Label(projectsGroup, SWT.NONE);
        managerLabel.setText("Manager project:");
        managerLabel.setFont(parent.getFont());

        managerProjectName = new Label(projectsGroup, SWT.NONE);
        managerProjectName.setFont(parent.getFont());

        setProjectNames();

        setErrorMessage(null);
        setMessage(null);

        setControl(composite);

        setPageComplete(validatePage());
    }

    private void setProjectNames() {
        String prefix = prefixNameField.getText();
        prefix = prefix.trim();

        testProjectName.setText(prefix + ".tests");
        managerProjectName.setText(prefix + ".manager");
    }

    @Override
    public void handleEvent(Event event) {
        setProjectNames();
        setPageComplete(validatePage());
    }

    private boolean validatePage() {
        String prefix = prefixNameField.getText().trim();
        if (prefix.isEmpty()) {
            setErrorMessage("A project prefix is required");
            setMessage(null);
            return false;
        }
        for (int a = 0; a < invalidCharacters.length; a++) {
            if (prefix.contains(invalidCharacters[a])) {
                setErrorMessage("Project prefix contains invalid characters");
                setMessage(null);
                return false;
            }
        }

        String testProjectName = prefix + ".tests";
        String managerProjectName = prefix + ".manager";

        IProject testProject = ResourcesPlugin.getWorkspace().getRoot().getProject(testProjectName);
        if (testProject.exists()) {
            setErrorMessage("The resulting test project name already exists in this workspace");
            setMessage(null);
            return false;
        }

        IProject managerProject = ResourcesPlugin.getWorkspace().getRoot().getProject(managerProjectName);
        if (managerProject.exists()) {
            setErrorMessage("The resulting manager project name already exists in this workspace");
            setMessage(null);
            return false;
        }

        setErrorMessage(null);
        setMessage(null);
        return true;
    }

    protected String getPrefix() {
        return this.prefixNameField.getText();
    }

}
