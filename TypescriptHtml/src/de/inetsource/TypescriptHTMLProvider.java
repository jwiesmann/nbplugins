package de.inetsource;

import de.inetsource.search.SearchPattern;
import de.inetsource.search.TSAnalyzer;
import de.inetsource.search.TSMatcher;
import de.inetsource.search.TSResult;
import de.inetsource.search.TSResultType;
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

                TSResult tSResult;
                try {                    
                    boolean controllerAsSyntax = false;
                    final StyledDocument bDoc = (StyledDocument) document;
                    String content = bDoc.getText(0, caretOffset);
                    Matcher m = TSMatcher.find(SearchPattern.NG_CONTROLLER, content);
                    if (m==null) {
                        m = TSMatcher.find(SearchPattern.NG_CONTROLLER_AS_SYNTAX, content);
                        controllerAsSyntax = true;
                    }
                    tSResult = getFilterValue(bDoc, caretOffset);
                    if (m != null && tSResult != null) {
                        filter = tSResult.getFilterResult();
                        String ngControllerName = m.group(2);
                        startOffset = caretOffset - filter.length();
                        interfacesFound.putAll(analyzer.findInterfacesFromController(ngControllerName, content, controllerAsSyntax));
                        interfacesFound.putAll(analyzer.getDynamicInterfaces());
                        System.out.println("interfaces found:" + interfacesFound);
                    } else {
                        completionResultSet.finish();
                        return;
                    }
                } catch (BadLocationException ex) {
                    completionResultSet.finish();
                    return;
                }
                System.out.println("fitering: " + filter);
                Map<String, TypescriptHTMLItem> itemsToAdd = new HashMap<>();
                for (TSInterface usedInterface : interfacesFound.values()) {
                    for (String childInterface : usedInterface.getChildren().keySet()) {
                        if (childInterface.startsWith(filter) && !filter.contains(".")) {
                            System.out.println("checking:" + childInterface + usedInterface.getChildren().get(childInterface).isArray());
                            if ((TSResultType.ARRAY.equals(tSResult.getType()) && usedInterface.getChildren().get(childInterface).isArray())
                                    || (!TSResultType.ARRAY.equals(tSResult.getType()) && !usedInterface.getChildren().get(childInterface).isArray())) {
                                TypescriptHTMLItem item = new TypescriptHTMLItem(childInterface, startOffset, caretOffset);
                                itemsToAdd.put(childInterface, item);
                            }
                        } else if (filter.startsWith(childInterface) && filter.contains(".")) {
                            String interfaceAssumptions[] = filter.split("\\.");
                            TSInterface lastOne = usedInterface.getChildren().get(interfaceAssumptions[0]);
                            if (lastOne != null) {
                                startOffset += lastOne.getName().length() + 1;
                                int iterationSize = interfaceAssumptions.length;
                                if (!filter.endsWith(".")) {
                                    iterationSize -= 1;
                                }
                                for (int i = 1; i < iterationSize; i++) {
                                    if (lastOne.getChildren() != null) {
                                        lastOne = lastOne.getChildren().get(interfaceAssumptions[i]);
                                        if (lastOne != null) {
                                            startOffset += lastOne.getName().length() + 1;
                                        }
                                    }
                                }
                                if (lastOne != null) {
                                    Map<String, String> propsOrFunction = lastOne.getObjectProperties();
                                    if (TSResultType.FUNCTION.equals(tSResult.getType())) {
                                        propsOrFunction = lastOne.getObjectFunctions();
                                    }
                                    for (String value : propsOrFunction.keySet()) {
                                        if (value.toLowerCase().startsWith(interfaceAssumptions[interfaceAssumptions.length - 1])
                                                || interfaceAssumptions.length == iterationSize) { // just after the dot
                                            if ((TSResultType.ARRAY.equals(tSResult.getType()) && lastOne.isArray())
                                                    || (!TSResultType.ARRAY.equals(tSResult.getType()) && !lastOne.isArray())) {
                                                TypescriptHTMLItem item = new TypescriptHTMLItem(value, startOffset, caretOffset);
                                                itemsToAdd.put(value, item);                                                
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // there is a little bug in case we define the interfaces in the class .. it might finds them twice...
                if (!itemsToAdd.isEmpty()) {
                    completionResultSet.addAllItems(itemsToAdd.values());
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
            String lineToCheck = doc.getText(start, offset - start);
            Matcher ngRepeat = TSMatcher.find(SearchPattern.NG_REPEAT, lineToCheck);
            Matcher ngModel = TSMatcher.find(SearchPattern.NG_MODEL, lineToCheck);
            Matcher ngClick = TSMatcher.find(SearchPattern.NG_CLICK, lineToCheck);
            if (ngRepeat != null) {
                if (ngRepeat.end() < lineToCheck.indexOf("{{")) {
                    return checkForBrackets(lineToCheck, offset, start);
                } else {
                    return new TSResult(ngRepeat.group(3), ngRepeat.group(2), TSResultType.ARRAY);
                }
            } else if (lineToCheck.contains("{{")) {
                return checkForBrackets(lineToCheck, offset, start);
            } else if (ngModel != null) {
                return new TSResult(ngModel.group(2), ngModel.group(2), TSResultType.PROPERTY);
            } else if (ngClick != null) {
                return new TSResult(ngClick.group(2), ngClick.group(2), TSResultType.FUNCTION);
            }
        } catch (BadLocationException ex) {
        }
        return null;
    }

    public static TSResult checkForBrackets(String lineToCheck, int offset, int start) {
        String filter = null;
        int firstBrackets = lineToCheck.indexOf("{{");
        while (firstBrackets > 0 && firstBrackets < (offset - start)) {
            filter = lineToCheck.substring(firstBrackets + 2);
            firstBrackets = lineToCheck.indexOf("{{", firstBrackets + 1);
        }
        return new TSResult(filter, filter, TSResultType.PROPERTY);
    }

}
