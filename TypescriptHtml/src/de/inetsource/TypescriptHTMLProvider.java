package de.inetsource;

import de.inetsource.search.SearchPattern;
import de.inetsource.search.TSAnalyzer;
import de.inetsource.search.TSMatcher;
import de.inetsource.search.TSResult;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;

/**
 *
 * @author geekdivers
 */
@MimeRegistration(mimeType = "text/html", service = CompletionProvider.class)
public class TypescriptHTMLProvider implements CompletionProvider {

    TSAnalyzer analyzer;

    public TypescriptHTMLProvider() {
        analyzer = new TSAnalyzer();
    }

    @Override
    public CompletionTask createTask(int queryType, JTextComponent jtc) {
        if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE) {
            return null;
        }

        return new AsyncCompletionTask(new AsyncCompletionQuery() {

            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int caretOffset) {
                int startOffset;
                String filter;
                Map<String, TSInterface> interfacesFound = new HashMap<>();
                try {
                    final StyledDocument bDoc = (StyledDocument) document;
                    TSResult tSResult = getFilterValue(bDoc, caretOffset);
                    String content = bDoc.getText(0, caretOffset); // that is the max text we need...maybe even less
                    Matcher m = TSMatcher.find(SearchPattern.NG_CONTROLLER, content);
                    if (m != null && tSResult != null) {
                        filter = tSResult.getFilterResult();
                        String ngControllerName = m.group(2);
                        startOffset = caretOffset - filter.length();
                        interfacesFound.putAll(analyzer.findInterfacesFromController(ngControllerName));
                    } else {
                        completionResultSet.finish();
                        return;
                    }
                } catch (BadLocationException ex) {
                    completionResultSet.finish();
                    return;
                }
                // Iterate through the available locales
                // and assign each country display name
                // to a CompletionResultSet:
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
                                        Matcher m = TSMatcher.find(SearchPattern.TYPE_IS_ARRAY, interfaces.getObjectProperties().get(propVal));
                                        if (m != null) {
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
                completionResultSet.finish();
            }
        }, jtc);
    }

    @Override
    public int getAutoQueryTypes(JTextComponent jtc, String string
    ) {
        return 0;
    }

    static TSResult getFilterValue(StyledDocument doc, int offset) {
        try {
            Element lineElement = doc.getParagraphElement(offset);
            int start = lineElement.getStartOffset();
            String filter = null;
            String lineToCheck = doc.getText(start, offset - start);
            Matcher ngRepeat = TSMatcher.find(SearchPattern.NG_REPEAT, lineToCheck);
            if (ngRepeat != null) {
                return new TSResult(ngRepeat.group(3), true);
            } else if (lineToCheck.contains("{{")) {
                int firstBrackets = lineToCheck.indexOf("{{");
                while (firstBrackets > 0 && firstBrackets < (offset - start)) {
                    filter = lineToCheck.substring(firstBrackets + 2);
                    System.out.println("filter:" + filter);
                    firstBrackets = lineToCheck.indexOf("{{", firstBrackets + 1);
                }
                return new TSResult(filter, false); // TODO check if array
            }

        } catch (BadLocationException ex) {
        }
        return null;
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
