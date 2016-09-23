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
                Map<String, String> interfaceMapper = new HashMap<>();
                TSResult tSResult;
                try {
                    final StyledDocument bDoc = (StyledDocument) document;
                    String content = bDoc.getText(0, caretOffset); // that is the max text we need...maybe even less
                    Matcher m = TSMatcher.find(SearchPattern.NG_CONTROLLER, content);
                    tSResult = getFilterValue(bDoc, caretOffset);
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
                if (filter.indexOf(".") > 0) {
                    int dotFound = filter.lastIndexOf(".");
                    startOffset += dotFound + 1;
                    String filters[] = filter.split("\\.");

                    TSInterface sInterface = interfacesFound.get(filters[0]);
                    if (sInterface != null) {
                        setPossibleCompletion(sInterface, filters, tSResult, sInterface, startOffset, caretOffset, completionResultSet);
                    } else {
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
                                            setPossibleCompletion(tsi, filters, tSResult, interfaces, startOffset, caretOffset, completionResultSet);
                                        } 
                                   }
                                } else {
                                    // todo
                                }
                            }
                        }
                    }
                } else {
                    for (TSInterface interfaces : interfacesFound.values()) {
                        for (String propVal : interfaces.getObjectProperties().keySet()) {
                            if (propVal.contains(filter)) {
                                tSResult.setRealInterfaceName(interfaces.getObjectProperties().get(propVal));
                                TypescriptHTMLItem item = new TypescriptHTMLItem(propVal, startOffset, caretOffset, tSResult, analyzer);
                                completionResultSet.addItem(item);
                            }
                        }
                    }
                }
                completionResultSet.finish();
            }

            private void setPossibleCompletion(TSInterface tsi, String[] filters, TSResult tSResult, TSInterface interfaces, int startOffset, int caretOffset, CompletionResultSet completionResultSet) {
                for (String value : tsi.getObjectProperties().keySet()) {
                    if (filters.length < 2) {
                        tSResult.setRealInterfaceName(interfaces.getName());
                        TypescriptHTMLItem item = new TypescriptHTMLItem(value, startOffset, caretOffset, tSResult, analyzer);
                        completionResultSet.addItem(item);
                    } else if (value.toLowerCase().startsWith(filters[1])) {
                        tSResult.setRealInterfaceName(interfaces.getName());
                        TypescriptHTMLItem item = new TypescriptHTMLItem(value, startOffset, caretOffset, tSResult, analyzer);
                        completionResultSet.addItem(item);
                    }
                }
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
                return new TSResult(ngRepeat.group(3), ngRepeat.group(2), true);
            } else if (lineToCheck.contains("{{")) {
                int firstBrackets = lineToCheck.indexOf("{{");
                while (firstBrackets > 0 && firstBrackets < (offset - start)) {
                    filter = lineToCheck.substring(firstBrackets + 2);
                    System.out.println("filter:" + filter);
                    firstBrackets = lineToCheck.indexOf("{{", firstBrackets + 1);
                }
                return new TSResult(filter, filter, false); // TODO check if array
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
