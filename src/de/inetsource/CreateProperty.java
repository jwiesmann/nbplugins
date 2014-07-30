package de.inetsource;

import de.inetsource.gui.PropertyEditor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
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
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

@ActionID(
        category = "Refactoring",
        id = "de.inetsource.CreateProperty")
@ActionRegistration(
        iconBase = "de/inetsource/1406316195_Chat.png",
        displayName = "#CTL_CreateProperty")
@ActionReferences({
        @ActionReference(path = "Menu/Refactoring", position = 0)
})
@Messages("CTL_CreateProperty=Create Property")
public final class CreateProperty implements ActionListener {

    private final EditorCookie context;

    public CreateProperty(EditorCookie context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (getFileFromEditor() != null) {
            Project curProject = lookupProject();
            if (curProject != null) {
                List<FileObject> propertyFiles = findPropertyFiles(curProject);
                if (propertyFiles.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "No Property file(s) found. Please create them upfront. Also add the word 'message' "
                            + "into your property files (like messages_de.properties or messages_en.properties", "Cannot Continue",
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    showFastPropertyEditor(propertyFiles);
                }
            } else {
                JOptionPane.showMessageDialog(null, "Could not locate current Project. Editing failed!");
            }
        } else {
            JOptionPane.showMessageDialog(null, "Please open a file first");
        }
    }

    private FileObject getFileFromEditor() {
        if (context.getDocument() != null && context.getDocument().getClass().equals(NbEditorDocument.class)) {
            NbEditorDocument doc = (NbEditorDocument) context.getDocument();
            Object sdp = doc.getProperty(NbEditorDocument.StreamDescriptionProperty);
            if (sdp instanceof FileObject) {
                return (FileObject) sdp;
            }
            if (sdp instanceof DataObject) {
                return ((DataObject) sdp).getPrimaryFile();
            }
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
                if (f.getName().toLowerCase().contains("message")) {
                    FileObject fo = FileUtil.toFileObject(f);
                    files.add(fo);
                }
            }
        }
        return files;
    }

    private void showFastPropertyEditor(List<FileObject> fileObjects) {
        System.out.println(getFileFromEditor().getMIMEType());
        JEditorPane[] panes = context.getOpenedPanes();

        String editorText = "";
        if (panes.length > 0) {
            editorText = panes[0].getText();
        }
        List<String> defaultValues = findOccurrences(editorText);

        PropertyEditor editor = new PropertyEditor();
        JTable propertyTable = editor.getPropertyTable();
        Object[] tableRows = new Object[fileObjects.size() + 1];
        tableRows[0] = "Key";
        for (int i = 1; i <= fileObjects.size(); i++) {
            tableRows[i] = fileObjects.get(i - 1).getName();
        }
        DefaultTableModel dtm = new DefaultTableModel(tableRows, defaultValues.size());
        int i = 0;
        for (String value : defaultValues) {
            String key = createKeyFromValue(value, fileObjects);
            dtm.setValueAt(key, i, 0);
            for (int j = 1; j <= fileObjects.size(); j++) {
                dtm.setValueAt(getValueFromProperty(fileObjects.get(j - 1), value, key), i, j);
            }
            i++;
        }
        propertyTable.setModel(dtm);

        editor.setVisible(true);
    }

    private List<String> findOccurrences(String editorText) {
        List<String> result = new ArrayList<String>();
        String[] patterns = new String[] {
                "(value=\")([a-zA-Z0-9 ]*)(\")",
                "(headerText=\")([a-zA-Z0-9 ]*)(\")",
                "(itemLabel=\")([a-zA-Z0-9 ]*)(\")",
                "(header=\")([a-zA-Z0-9 ]*)(\")",
                "(>)([a-zA-Z0-9 ]*)(<)" };
        for (String patternString : patterns) {
            Pattern pattern = Pattern.compile(patternString);
            Matcher matcher = pattern.matcher(editorText);
            while (matcher.find()) {
                String foundValue = matcher.group(2);
                if (!result.contains(foundValue) && StringUtils.trimToEmpty(foundValue).length()>0) {
                    result.add(foundValue);
                }
            }
        }
        return result;
    }

    private String createKeyFromValue(String value, List<FileObject> messageProperties) {
        String suggestedKey = "";
        if (value != null && value.length() >= 1) {
            Properties props = new Properties();
            try {
                props.load(messageProperties.get(0).getInputStream());
                Iterator<Object> iter = props.keySet().iterator();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    if (value.equals(props.get(key))) {
                        return key;
                    }
                }
            } catch (FileNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                try {
                    messageProperties.get(0).getInputStream().close();
                } catch (FileNotFoundException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            String[] keyVal = value.split(" ");
            for (String val : keyVal) {
                suggestedKey += val.toLowerCase() + ".";
            }
            return suggestedKey.substring(0, suggestedKey.length() - 1);
        }
        return suggestedKey;
    }

    private String getValueFromProperty(FileObject fileObject, String defaultValue, String key) {
        try {
            Properties p = new Properties();
            p.load(fileObject.getInputStream());
            return p.getProperty(key, defaultValue);
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            try {
                fileObject.getInputStream().close();
            } catch (FileNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return defaultValue;
    }

}
