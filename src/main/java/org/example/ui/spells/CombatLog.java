package org.example.ui.spells;

import org.example.ui.common.AppStyle;
import org.example.ui.common.TactileStyle;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class CombatLog extends JPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_LOG_LENGTH = 30000, RETAINED_LOG_LENGTH = 24000;
    private final JTextArea log = AppStyle.text("");

    CombatLog() {
        super(new BorderLayout());
        setOpaque(false);
        JPanel card = AppStyle.card(new BorderLayout(0, 10));
        JPanel title = AppStyle.section(new BorderLayout());
        title.add(TactileStyle.heading("Combat Roll Log", TactileStyle.BLUE), BorderLayout.WEST);
        JPanel logActions = AppStyle.section(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        logActions.add(AppStyle.button("Clear", this::clear));
        title.add(logActions, BorderLayout.EAST);
        card.add(title, BorderLayout.NORTH);
        log.setRows(9);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        clear();
        JScrollPane scroll = new JScrollPane(log);
        AppStyle.scrollPane(scroll);
        scroll.setPreferredSize(new Dimension(360, 190));
        card.add(scroll, BorderLayout.CENTER);
        add(card);
    }

    void record(String event) {
        String entry = "[" + TIME.format(LocalTime.now()) + "] " + event;
        if (log.getText().startsWith("No actions recorded")) {
            log.setText("");
        }
        log.append((log.getText().isBlank() ? "" : "\n\n") + entry);
        if (log.getDocument().getLength() > MAX_LOG_LENGTH) {
            log.setText(
                    log.getText().substring(log.getDocument().getLength() - RETAINED_LOG_LENGTH));
        }
        log.setCaretPosition(log.getDocument().getLength());
    }

    String text() {
        return log.getText();
    }

    void clear() {
        log.setText("");
    }
}
