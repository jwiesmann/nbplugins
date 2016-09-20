package de.inetsource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import org.apache.commons.io.FileUtils;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
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

    Map<String, TSInterface> allInterfaces = new HashMap<>();
    String currentController = null;
    WatchDir tsWatchService = null;

    @Override
    public CompletionTask createTask(int queryType, JTextComponent jtc
    ) {
        if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE) {
            return null;
        }

        return new AsyncCompletionTask(new AsyncCompletionQuery() {

            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int caretOffset) {
                Date startDate = new Date();
                String filter = null;
                int startOffset = caretOffset - 1;
                Map<String, TSInterface> interfacesFound = new HashMap<>();
                try {
                    final StyledDocument bDoc = (StyledDocument) document;

                    String content = bDoc.getText(0, caretOffset); // that is the max text we need...maybe even less
                    String ngControllerPatternString = "(ng-controller=\")([a-zA-Z]+)\"";
                    Pattern ngControllerPattern = Pattern.compile(ngControllerPatternString);
                    Matcher m = ngControllerPattern.matcher(content);
                    if (m.find()) {
                        String ngControllerName = m.group(2);
                        System.out.println("found controller:" + ngControllerName + " in "
                                + (new Date().getTime() - startDate.getTime()) + " ms");
                        interfacesFound.putAll(findDefinitions(ngControllerName, document));
                        System.out.println(
                                "found definitions: in " + (new Date().getTime() - startDate.getTime()) + " ms");
                        System.out.println("found:" + interfacesFound.size());
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

                // Iterate through the available locales
                // and assign each country display name
                // to a CompletionResultSet:
                filter = filter.substring(2); // cut {{
                startOffset += 2; // add the two {{ again
                if (filter != null && filter.length() > 2) {
                    if (filter.indexOf(".") > 0) {
                        int dotFound = filter.lastIndexOf(".");
                        startOffset += dotFound + 1;
                        String filters[] = filter.split("\\.");
                        for (TSInterface interfaces : interfacesFound.values()) {
                            // now that gets a bit ugly...
                            for (String propVal : interfaces.getObjectProperties().keySet()) {
                                if (filters.length <= 2) {
                                    if (propVal.equals(filters[0])) {
                                        TSInterface tsi = interfacesFound.get(interfaces.getObjectProperties().get(propVal));
                                        if (tsi == null) {
                                            String searchArrayString = "([\\S]+)(\\[[0-9]{0,}\\])";
                                            Pattern sasPattern = Pattern.compile(searchArrayString);
                                            Matcher m = sasPattern.matcher(interfaces.getObjectProperties().get(propVal));
                                            if (m.find()) {
                                                tsi = interfacesFound.get(m.group(1));
                                                startOffset += m.group(2).length();
                                            }

                                        }
                                        if (tsi != null) {
                                            for (String value : tsi.getObjectProperties().keySet()) {
                                                if (filters.length < 2) {
                                                    TypescriptHTMLItem item = new TypescriptHTMLItem(value, startOffset, caretOffset);
                                                    completionResultSet.addItem(item);
                                                } else if (value.toLowerCase().startsWith(filters[1])) {
                                                    TypescriptHTMLItem item = new TypescriptHTMLItem(value, startOffset, caretOffset);
                                                    completionResultSet.addItem(item);
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    //todo
                                }
                            }
                        }
                    } else {
                        for (TSInterface interfaces : interfacesFound.values()) {
                            for (String propVal : interfaces.getObjectProperties().keySet()) {
                                System.out.println(propVal);
                                if (propVal.contains(filter)) {
                                    TypescriptHTMLItem item = new TypescriptHTMLItem(propVal, startOffset, caretOffset);
                                    completionResultSet.addItem(item);
                                }
                            }
                        }
                    }

                }
                completionResultSet.finish();
            }
        }, jtc);
    }

    @Override
    public int getAutoQueryTypes(JTextComponent jtc, String string
    ) {
        return 0;
    }

    private Map<String, TSInterface> findDefinitions(String ngControllerName, Document doc) {
        Project thisProject = lookupProject();
        return findTypescriptFiles(thisProject, ngControllerName);
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
    private Map<String, TSInterface> findTypescriptFiles(Project p, String ngControllerName) {
        Map<String, TSInterface> foundSuggestions = new HashMap<>();
        // not sure why, but we need to look up one dir higher...
        File projectDir;
        if (p.getProjectDirectory().getParent() != null) {
            projectDir = FileUtil.toFile(p.getProjectDirectory().getParent());
        } else {
            projectDir = FileUtil.toFile(p.getProjectDirectory());
        }
        if (projectDir != null && projectDir.isDirectory()) {

            if (tsWatchService == null) {
                try {
                    tsWatchService = new WatchDir(projectDir.toPath(), true);
                    tsWatchService.processEvents();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (allInterfaces.isEmpty()) {
                Collection<File> foundFiles2 = FileUtils.listFiles(projectDir, new SuffixFileFilter(".ts"),
                        TrueFileFilter.INSTANCE);

                for (File ff : foundFiles2) {
                    if (!ff.getAbsolutePath().contains("node_modules")) {

                        try {
                            String content = FileUtils.readFileToString(ff);
                            if (content.contains(ngControllerName)) {
                                currentController = content;
                            }
                            System.out.println("creating interface for:" + ff.getAbsolutePath());
                            allInterfaces.putAll(createAllInterfaces(content));

                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
                // start watching the files...

            }

            if (currentController != null) {
                Pattern interfacePattern = Pattern.compile(": [a-zA-Z\\.]+");
                Matcher interfacePatternMatcher = interfacePattern.matcher(currentController);
                while (interfacePatternMatcher.find()) {
                    String foundInterfaceName = currentController.substring(interfacePatternMatcher.start() + 2, interfacePatternMatcher.end());
                    TSInterface found = allInterfaces.get(foundInterfaceName);
                    if (found != null) {
                        System.out.println("FOUND: " + found.getName() + " >>>" + found.getObjectFunctions() + " >>> " + found.getObjectProperties());
                        foundSuggestions.put(foundInterfaceName, found);
                        if (found.isExtendsInterface() && allInterfaces.containsKey(found.getExtendsInterfaceName())) {
                            foundSuggestions.put(found.getExtendsInterfaceName(), allInterfaces.get(found.getExtendsInterfaceName()));
                            TSInterface extenededInterface = allInterfaces.get(found.getExtendsInterfaceName());
                            System.out.println("FOUND Extension: " + found.getExtendsInterfaceName());
                            found.getObjectFunctions().putAll(extenededInterface.getObjectFunctions());
                            found.getObjectProperties().putAll(extenededInterface.getObjectProperties());
                        }
                    }
                }

                System.out.println("interfaces created:" + allInterfaces.size());
            }

        }
        return foundSuggestions;
    }

    private static Map<String, TSInterface> createAllInterfaces(String text) {

        String searchNamePatternString = "(interface )([\\S]+)";
        String searchExtendsPatternString = "(extends )([\\S]+)( \\{)";
        String simplePropOrFunction = "(\\S+): ([a-zA-Z}\\[\\]]+);";
        Pattern simplePropOrFunctionPattern = Pattern.compile(simplePropOrFunction);
        Pattern searchNamePatter = Pattern.compile(searchNamePatternString);
        Pattern searchExtendsName = Pattern.compile(searchExtendsPatternString);

        long start = System.currentTimeMillis();
        Map<String, TSInterface> result = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            String line = reader.readLine();
            int bracketsCount = 0;
            boolean insideInterface = false;
            TSInterface tsi = null;
            while (line != null) {

                if (insideInterface) {
                    Matcher simpleProp = simplePropOrFunctionPattern.matcher(line);
                    if (simpleProp.find() && tsi != null) {
                        if (simpleProp.group(1).contains("(")) {
                            tsi.getObjectFunctions().put(simpleProp.group(1), simpleProp.group(2));
                        } else {
                            tsi.getObjectProperties().put(simpleProp.group(1), simpleProp.group(2));
                        }
                    }
                    insideInterface = isInterfaceDone(line, bracketsCount, insideInterface);
                    if (!insideInterface) {
                        result.put(tsi.getName(), tsi);
                    }

                }

                Matcher m = searchNamePatter.matcher(line);
                if (!insideInterface && m.find()) {
                    boolean extendsInterface = line.contains("extends");
                    tsi = new TSInterface(extendsInterface, m.group(2));
                    if (extendsInterface) {
                        Matcher extendsInterfaceNameMatcher = searchExtendsName.matcher(line);
                        if (extendsInterfaceNameMatcher.find()) {
                            String extendsInterfaceName = extendsInterfaceNameMatcher.group(2);
                            if (extendsInterfaceName.contains(".")) {
                                extendsInterfaceName = extendsInterfaceName.substring(extendsInterfaceName.lastIndexOf(".") + 1);
                            }
                            tsi.setExtendsInterfaceName(extendsInterfaceName);
                        }
                    }
                    insideInterface = true;
                }
                line = reader.readLine();
            }
        } catch (IOException exc) {
            // quit
        }
        System.out.printf(result.size() + " ==> in Reader: %d%n", System.currentTimeMillis() - start);
        return result;
    }

    public static boolean isInterfaceDone(String line, int bracketsCount, boolean insideInterface) {
        if (line.contains("{")) {
            bracketsCount += StringUtils.countMatches(line, "{");
        }
        if (line.contains("}") && bracketsCount == 0) {
            insideInterface = false;
        } else if (line.contains("}")) {
            bracketsCount -= StringUtils.countMatches(line, "}");
        }
        return insideInterface;
    }

    static int getRowFirstNonWhite(StyledDocument doc, int offset) throws BadLocationException {
        Element lineElement = doc.getParagraphElement(offset);
        int start = lineElement.getStartOffset();
        while (start + 1 < lineElement.getEndOffset()) {
            try {
                if (doc.getText(start, 1).charAt(0) != '{') {
                    break;
                }
            } catch (BadLocationException ex) {
                throw (BadLocationException) new BadLocationException(
                        "calling getText(" + start + ", " + (start + 1) + ") on doc of length: " + doc.getLength(),
                        start).initCause(ex);
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
