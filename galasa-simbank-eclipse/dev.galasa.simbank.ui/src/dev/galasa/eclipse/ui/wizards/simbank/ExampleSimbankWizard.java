package dev.galasa.eclipse.ui.wizards.simbank;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;

import dev.galasa.eclipse.Activator;

public class ExampleSimbankWizard extends Wizard implements INewWizard {
	
	public ExampleSimbankWizard() {
		setWindowTitle("Import Galasa SimBank example projects");
	}

	@Override
	public void init(IWorkbench arg0, IStructuredSelection arg1) {
		
	}

	@Override
	public boolean performFinish() {
		String prefix = ((ExampleSimbankWizardPage)getPage("prefix")).getPrefix().trim();
		if (prefix.isEmpty()) {
			return false;
		}
		
		ExampleSimbankOperation runnable = new ExampleSimbankOperation(prefix);
		IRunnableWithProgress op = new WorkspaceModifyDelegatingOperation(runnable);
		try {
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			Activator.log(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	@Override
	public void addPages() {
		addPage(new ExampleSimbankWizardPage("prefix"));
	}

}
