package de.inetsource;

import de.inetsource.search.TSAnalyzer;
import de.inetsource.search.TSResult;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;

/**
 *
 * @author geekdivers
 */
public class TypescriptHTMLItem implements CompletionItem {

    private String text;
    private static ImageIcon fieldIcon = new ImageIcon(ImageUtilities.loadImage("de/inetsource/1474226385_Image.png"));
    private static Color fieldColor = Color.decode("0x0000B2");
    private int caretOffset;
    private final int dotOffset;
    private final TSResult tsResult;
    private final TSAnalyzer tsAnalyzer;

    public TypescriptHTMLItem(String text, int dotOffset, int caretOffset, TSResult tSResult, TSAnalyzer tsAnalyzer) {
        this.text = text;
        this.dotOffset = dotOffset;
        this.caretOffset = caretOffset;
        this.tsResult = tSResult;
        this.tsAnalyzer = tsAnalyzer;
    }

    @Override
    public void defaultAction(JTextComponent component) {
        try {
            StyledDocument doc = (StyledDocument) component.getDocument();
            doc.remove(dotOffset, caretOffset - dotOffset);
            doc.insertString(dotOffset, text, null);
            if (!tsResult.getFilterResult().equals(tsResult.getVariableUsed())) {
                tsAnalyzer.copyInterface(tsResult.getRealInterfaceName(), tsResult.getVariableUsed());
            }
            Completion.get().hideAll();
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void processKeyEvent(KeyEvent ke) {
    }

    @Override
    public int getPreferredWidth(Graphics graphics, Font font) {
        return CompletionUtilities.getPreferredWidth(text, null, graphics, font);
    }

    @Override
    public void render(Graphics g, Font defaultFont, Color defaultColor,
            Color backgroundColor, int width, int height, boolean selected) {
        CompletionUtilities.renderHtml(fieldIcon, text, null, g, defaultFont,
                (selected ? Color.white : fieldColor), width, height, selected);
    }

    @Override
    public CompletionTask createDocumentationTask() {
        return null;
    }

    @Override
    public CompletionTask createToolTipTask() {
        return null;
    }

    @Override
    public boolean instantSubstitution(JTextComponent jtc) {
        return false;
    }

    @Override
    public int getSortPriority() {
        return 0;
    }

    @Override
    public CharSequence getSortText() {
        return text;
    }

    @Override
    public CharSequence getInsertPrefix() {
        return text;
    }

}