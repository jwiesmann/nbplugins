package com.geekdivers;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import org.apache.commons.io.FileUtils;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

/**
 *
 * @author geekdivers
 */
@MimeRegistration(mimeType = "text/html", service = CompletionProvider.class)
public class TypescriptHTMLProvider implements CompletionProvider {

    @Override
    public CompletionTask createTask(int queryType, JTextComponent jtc) {
        if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE) {
            return null;
        }

        return new AsyncCompletionTask(new AsyncCompletionQuery() {

            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int caretOffset) {
                Date startDate = new Date();
                String filter = null;
                int startOffset = caretOffset - 1;

                try {
                    final StyledDocument bDoc = (StyledDocument) document;

                    String content = bDoc.getText(0, caretOffset); // that is the max text we need...maybe even less
                    String ngControllerPatternString = "(ng-controller=\")([a-zA-Z]+)\"";
                    Pattern ngControllerPattern = Pattern.compile(ngControllerPatternString);
                    Matcher m = ngControllerPattern.matcher(content);
                    if (m.find()) {
                        String ngControllerName = m.group(2);
                        System.out.println("found controller:" + ngControllerName + " in " + (new Date().getTime() - startDate.getTime()) + " ms");
                        findDefinitions(ngControllerName, document);
                        System.out.println("found definitions: in " + (new Date().getTime() - startDate.getTime()) + " ms");
                    } else {
                        completionResultSet.finish();
                        return;
                    }

                    final int lineStartOffset = getRowFirstNonWhite(bDoc, caretOffset);
                    final char[] line = bDoc.getText(lineStartOffset, caretOffset - lineStartOffset).toCharArray();
                    final int whiteOffset = indexOfWhite(line);
                    filter = new String(line, whiteOffset + 1, line.length - whiteOffset - 1);
                    if (whiteOffset > 0) {
                        startOffset = lineStartOffset + whiteOffset + 1;
                    } else {
                        startOffset = lineStartOffset;
                    }
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }

                //Iterate through the available locales
                //and assign each country display name
                //to a CompletionResultSet:
                Locale[] locales = Locale.getAvailableLocales();
                for (int i = 0; i < locales.length; i++) {
                    final Locale locale = locales[i];
                    final String country = locale.getDisplayCountry();
                    //Here we test whether the country starts with the filter defined above:
                    if (!country.equals("") && country.startsWith(filter)) {
                        //Here we include the start offset, so that we'll be able to figure out
                        //the number of characters that we'll need to remove:
                        completionResultSet.addItem(new TypescriptHTMLItem(country, startOffset, caretOffset));
                    }
                }
                completionResultSet.finish();

            }

        }, jtc);
    }

    @Override
    public int getAutoQueryTypes(JTextComponent jtc, String string) {
        return 0;
    }

    private void findDefinitions(String ngControllerName, Document doc) {
        Project thisProject = lookupProject();
        findTypescriptFiles(thisProject,ngControllerName);
    }

    private Project lookupProject() {

        Lookup genlokup = Utilities.actionsGlobalContext();
        Project proj = genlokup.lookup(Project.class);

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
    private List<FileObject> findTypescriptFiles(Project p, String ngControllerName) {
        List<FileObject> files = new ArrayList<FileObject>();
        // not sure why, but we need to look up one dir higher...
        File projectDir = null;
        if (p.getProjectDirectory().getParent() != null) {
            projectDir = FileUtil.toFile(p.getProjectDirectory().getParent());
        } else {
            projectDir = FileUtil.toFile(p.getProjectDirectory());
        }
        if (projectDir != null && projectDir.isDirectory()) {
            Collection<File> foundFiles2 = FileUtils.listFiles(projectDir, new SuffixFileFilter(".ts"), TrueFileFilter.INSTANCE);
            for (File ff : foundFiles2) {
                if (!ff.getAbsolutePath().contains("node_modules")) {

                    try {
                        String content = FileUtils.readFileToString(ff);
                        if (content.contains(ngControllerName)) {
                            System.out.println(content);
                        }
                        
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }

        }
        return files;
    }

    static int getRowFirstNonWhite(StyledDocument doc, int offset)
            throws BadLocationException {
        Element lineElement = doc.getParagraphElement(offset);
        int start = lineElement.getStartOffset();
        while (start + 1 < lineElement.getEndOffset()) {
            try {
                if (doc.getText(start, 1).charAt(0) != ' ') {
                    break;
                }
            } catch (BadLocationException ex) {
                throw (BadLocationException) new BadLocationException(
                        "calling getText(" + start + ", " + (start + 1)
                        + ") on doc of length: " + doc.getLength(), start
                ).initCause(ex);
            }
            start++;
        }
        return start;
    }

    static int indexOfWhite(char[] line) {
        int i = line.length;
        while (--i > -1) {
            final char c = line[i];
            if (Character.isWhitespace(c)) {
                return i;
            }
        }
        return -1;
    }
}
