package de.inetsource;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.text.StyledDocument;
import org.apache.commons.io.FileUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.editor.NbEditorDocument;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

@ActionID(
        category = "Refactoring",
        id = "de.inetsource.CreateProperty")
@ActionRegistration(
        iconBase = "de/inetsource/1406316195_Chat.png",
        displayName = "#CTL_CreateProperty")
@ActionReferences({
        @ActionReference(path = "Editors/text/xhtml/Popup", position = 400) })
@Messages("CTL_CreateProperty=Create Property")
public final class CreateProperty implements ActionListener {

    private final EditorCookie context;

    public CreateProperty(EditorCookie context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        Project curProject = lookupProject();
        if (curProject != null) {
            try {
                StyledDocument doc = context.openDocument();
                if (context != null) {
                    JEditorPane[] panes = context.getOpenedPanes();
                    System.out.println(context.getDocument().getDefaultRootElement().getAttributes());
                    if (panes.length > 0) {
                        FileObject fob = getFileObject((NbEditorDocument) context.getDocument());
                        List<FileObject> propertyFiles = findPropertyFiles(curProject);
                        if (propertyFiles.isEmpty()) {
                            if (JOptionPane.showConfirmDialog(null, "No property file found, do you want to create them now? ") == JOptionPane.YES_OPTION) {
                                createPropertyFiles();
                            }
                        }

                        int selStart = panes[0].getSelectionStart();
                        int selEnd = panes[0].getSelectionEnd();
                        if (selStart > -1 && selEnd > selStart) {
                            TopComponent component;
                            String selectedText = panes[0].getDocument().getText(selStart, selEnd - selStart);

                        } else {
                            JOptionPane.showMessageDialog(null, "Nothing selected..");
                        }
                    }
                }
            } catch (Exception ex) {
            }
        } else {
            JOptionPane.showMessageDialog(null, "Could not locate current Project. Editing failed!");
        }
    }

    public static FileObject getFileObject(NbEditorDocument doc) {
        Object sdp = doc.getProperty(NbEditorDocument.StreamDescriptionProperty);
        if (sdp instanceof FileObject) {
            return (FileObject) sdp;
        }
        if (sdp instanceof DataObject) {
            return ((DataObject) sdp).getPrimaryFile();
        }
        return null;
    }

    private Project lookupProject() {
        Project p = TopComponent.getRegistry().getActivated().getLookup().lookup(Project.class);
        if (p == null) {
            DataObject dob = TopComponent.getRegistry().getActivated().getLookup().lookup(DataObject.class);
            if (dob != null) {
                FileObject fo = dob.getPrimaryFile();
                p = FileOwnerQuery.getOwner(fo);
                return p;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<FileObject> findPropertyFiles(Project p) {
        List<FileObject> files = new ArrayList<FileObject>();
        File projectDir = FileUtil.toFile(p.getProjectDirectory());
        if (projectDir != null && projectDir.isDirectory()) {
            Collection<File> foundFiles = FileUtils.listFiles(projectDir, new String[] { "properties" }, true);
            for (File f : foundFiles) {
                FileObject fo = FileUtil.toFileObject(f);
                files.add(fo);
            }
        }
        return files;
    }

    private void createPropertyFiles() {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
                                                                       // Tools | Templates.
    }

}
