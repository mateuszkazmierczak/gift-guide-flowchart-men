package com.sample;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

public class GiftGuideGUI {
    private KieSession kSession;
    private JFrame frame;
    private JPanel panel;
    private JLabel questionLabel;
    private ButtonGroup optionsGroup;
    private List<AbstractButton> optionButtons;
    private JButton nextButton;
    private String currentQuestionId;
    private boolean isMultiSelect;

    public GiftGuideGUI() {
        // Initialize Drools
        KieServices ks = KieServices.Factory.get();
        KieContainer kContainer = ks.getKieClasspathContainer();
        kSession = kContainer.newKieSession("ksession-rules");

        // Initialize GUI
        frame = new JFrame("Gift Guide");
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        questionLabel = new JLabel();
        panel.add(questionLabel);
        optionButtons = new ArrayList<>();
        optionsGroup = new ButtonGroup();
        nextButton = new JButton("Next");
        
        frame.add(panel);
        frame.setSize(500, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Start
        kSession.fireAllRules();
        showNext();

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAnswer();
            }
        });
        panel.add(nextButton);
        frame.setVisible(true);
    }

    private void showNext() {
        // Remove old options from panel
        for (AbstractButton btn : optionButtons) {
            panel.remove(btn);
        }
        optionButtons.clear();
        optionsGroup = new ButtonGroup();
        currentQuestionId = null;
        isMultiSelect = false;
        
        // Move nextButton to the end of the panel
        panel.remove(nextButton);


        // Check for Recommendation
        Object recommendation = getFactBySimpleName("Recommendation");
        if (recommendation != null) {
            try {
                String gift = (String) recommendation.getClass().getMethod("getGift").invoke(recommendation);
                String reason = (String) recommendation.getClass().getMethod("getReason").invoke(recommendation);
                questionLabel.setText("<html>Recommended gift: " + gift + "<br>Reason: " + reason + "</html>");
                nextButton.setEnabled(false);
            } catch (Exception ex) {
                questionLabel.setText("Error displaying recommendation");
            }
            panel.add(nextButton);
            frame.revalidate();
            frame.repaint();
            return;
        }

        // Find next Question
        Object question = getFactBySimpleName("Question");
        if (question != null) {
            try {
                // Use reflection to get question properties
                Method getId = question.getClass().getMethod("getId");
                Method getText = question.getClass().getMethod("getText");
                Method getOptions = question.getClass().getMethod("getOptions");
                Method getMultiSelect = question.getClass().getMethod("isMultiSelect");

                currentQuestionId = (String) getId.invoke(question);
                String text = (String) getText.invoke(question);
                String options = (String) getOptions.invoke(question);
                isMultiSelect = (boolean) getMultiSelect.invoke(question);

                questionLabel.setText("<html>" + text + "</html>");

                for (String opt : options.split("\\|")) {
                    if (isMultiSelect) {
                        JCheckBox btn = new JCheckBox(opt);
                        optionButtons.add(btn);
                        panel.add(btn);
                    } else {
                        JRadioButton btn = new JRadioButton(opt);
                        optionsGroup.add(btn);
                        optionButtons.add(btn);
                        panel.add(btn);
                    }
                }
                nextButton.setEnabled(true);

            } catch (Exception ex) {
                questionLabel.setText("Error displaying question: " + ex.getMessage());
            }
        } else {
            questionLabel.setText("No more questions or final recommendation found.");
            nextButton.setEnabled(false);
        }
        
        panel.add(nextButton);
        frame.revalidate();
        frame.repaint();
    }

    private void handleAnswer() {
        String selectedValue = null;

        if (isMultiSelect) {
            selectedValue = optionButtons.stream()
                    .filter(AbstractButton::isSelected)
                    .map(AbstractButton::getText)
                    .collect(Collectors.joining(","));
        } else {
            for (Enumeration<AbstractButton> buttons = optionsGroup.getElements(); buttons.hasMoreElements();) {
                AbstractButton button = buttons.nextElement();
                if (button.isSelected()) {
                    selectedValue = button.getText();
                    break;
                }
            }
        }

        if (selectedValue != null && !selectedValue.isEmpty() && currentQuestionId != null) {
            try {
                // Retract the old question before inserting the answer
                Object oldQuestion = getFactBySimpleName("Question");
                if (oldQuestion != null) {
                     kSession.delete(kSession.getFactHandle(oldQuestion));
                }

                Object answer = createAnswer(currentQuestionId, selectedValue);
                kSession.insert(answer);
                kSession.fireAllRules();
            } catch (Exception ex) {
                questionLabel.setText("Error submitting answer: " + ex.getMessage());
            }
        }
        showNext();
    }

    private Object getFactBySimpleName(String simpleName) {
        for (Object fact : kSession.getObjects()) {
            if (fact.getClass().getSimpleName().equals(simpleName)) {
                return fact;
            }
        }
        return null;
    }

    private Object createAnswer(String questionId, String value) throws Exception {
        // Using reflection to create an Answer fact, as required
        Class<?> answerClass = kSession.getKieBase().getFactType("com.sample.rules", "Answer").getFactClass();
        Object answer = answerClass.getDeclaredConstructor().newInstance();
        answerClass.getMethod("setQuestionId", String.class).invoke(answer, questionId);
        answerClass.getMethod("setValue", String.class).invoke(answer, value);
        return answer;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GiftGuideGUI::new);
    }
}
