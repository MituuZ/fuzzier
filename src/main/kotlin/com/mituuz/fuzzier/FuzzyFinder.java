package com.mituuz.fuzzier;

import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBList;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;

public class FuzzyFinder extends JPanel {
    private EditorTextField previewPane;
    private JBList<String> fileList;
    private EditorTextField searchField;
    private JPanel fuzzyPanel;
    private JSplitPane splitPane;

    public EditorTextField getPreviewPane() {
        return previewPane;
    }

    public JBList<String> getFileList() {
        return fileList;
    }

    public EditorTextField getSearchField() {
        return searchField;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    private void createUIComponents() {
        this.setLayout(new BorderLayout());
        fuzzyPanel = new JPanel();
        this.add(fuzzyPanel);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        fuzzyPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane = new JSplitPane();
        fuzzyPanel.add(splitPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane.setLeftComponent(panel1);
        searchField = new EditorTextField();
        searchField.setText("");
        panel1.add(searchField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, -1), null, 0, false));
        fileList = new JBList();
        fileList.setSelectionMode(0);
        scrollPane1.setViewportView(fileList);
        previewPane = new EditorTextField();
        previewPane.setText("");
        previewPane.setViewer(true);
        splitPane.setRightComponent(previewPane);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return fuzzyPanel;
    }

}
