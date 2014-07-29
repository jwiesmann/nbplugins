/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.inetsource;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.text.StyledDocument;
import org.netbeans.modules.editor.NbEditorDocument;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.Repository;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Refactoring",
        id = "de.inetsource.CreateProperty")
@ActionRegistration(
        iconBase = "de/inetsource/1406316195_Chat.png",
        displayName = "#CTL_CreateProperty")
@ActionReferences({ @ActionReference(path = "Editors/text/xhtml/Popup", position = 400) })
@Messages("CTL_CreateProperty=Create Property")
public final class CreateProperty implements ActionListener {

    private final EditorCookie context;

    public CreateProperty(EditorCookie context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {

        try {
            StyledDocument doc = context.openDocument();
            if (context != null) {
                JEditorPane[] panes = context.getOpenedPanes();
                if (panes.length > 0) {
                    NbEditorDocument document = (NbEditorDocument) context.getDocument();
                    System.out.println(document.getDocumentProperties());

                    int selStart = panes[0].getSelectionStart();
                    int selEnd = panes[0].getSelectionEnd();
                    if (selStart > -1 && selEnd > selStart) {
                        String selectedText = panes[0].getDocument().getText(selStart, selEnd - selStart);
                        FileObject root = Repository.getDefault().getDefaultFileSystem().getRoot();
                        System.out.println(root);
                        File f = new File(
                                "D:\\dev\\CRM_DEV\\crm-esp\\comp\\trunk\\vms\\vms-ui\\src\\main\\resources\\com\\qvc\\vms\\vms-ui-messages.properties");
                        FileObject fileObject = FileUtil.toFileObject(f);
                        DataObject dobj = null;
                        try {
                            dobj = DataObject.find(fileObject);

                        } catch (DataObjectNotFoundException ex) {
                            ex.printStackTrace();
                        }
                        if (dobj != null) {
                            LineCookie lc = dobj.getLookup().lookup(LineCookie.class);
                            if (lc == null) {/* cannot do it */
                                return;
                            }
                            Line l = lc.getLineSet().getOriginal(1);
                            l.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
                        }
                        // new DataEditorSupport.open(fileObject);
                        // fileObject

                    } else {
                        JOptionPane.showMessageDialog(null, "Nothing selected..");
                    }
                }
            }
        } catch (Exception ex) {
        }
    }
}
