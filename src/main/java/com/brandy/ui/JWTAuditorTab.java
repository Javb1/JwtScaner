package com.brandy.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.UserInterface;
import com.brandy.core.attack.AttackListener;
import com.brandy.core.attack.JWTAttackEngine;
import com.brandy.core.model.AttackConfig;
import com.brandy.core.utils.JWTUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.nio.file.Paths;
import java.util.Date;

public class JWTAuditorTab extends JPanel implements AttackListener {
    private final MontoyaApi api;
    private JTextField jwtField;
    private JComboBox<String> algorithmCombo;
    private JButton loadDictButton;
    private JTextArea resultArea;
    private JProgressBar progressBar;
    private JButton startButton;
    private JLabel dictPathLabel;
    private JButton stopButton; // 新增
    private JScrollPane resultScroll; // 新增

    //新增secretArea
    private JTextArea secretArea;
    private JScrollPane secretScroll;
//    //新增区域复制按钮
//    private JPanel secretPanel;

    // 在resultArea下方添加日志区域
    private JTextArea logArea;
    private JWTAttackEngine attackEngine;
    
    // 添加内置字典选项
    private JCheckBox useBuiltinDictCheckbox;
    private boolean useBuiltinDictionary = true; // 默认使用内置字典


    public JWTAuditorTab(MontoyaApi api) {
        this.api = api;
        initComponents();
        initLayout();
    }

    private void initComponents() {
        // 输入组件
        jwtField = new JTextField(40);
        jwtField.setToolTipText("Paste JWT token here (header.payload.signature)");

        algorithmCombo = new JComboBox<>(new String[]{"HS256", "HS384", "HS512", "RS256", "RS384", "RS512"});
        algorithmCombo.setSelectedIndex(0);

        // 字典加载组件
        loadDictButton = new JButton("Load Dictionary");
        dictPathLabel = new JLabel("No dictionary selected");
        dictPathLabel.setForeground(Color.GRAY);
        
        // 添加内置字典选项
        useBuiltinDictCheckbox = new JCheckBox("Use Built-in Dictionary", true);
        useBuiltinDictCheckbox.addActionListener(e -> {
            useBuiltinDictionary = useBuiltinDictCheckbox.isSelected();
            loadDictButton.setEnabled(!useBuiltinDictionary);
            dictPathLabel.setEnabled(!useBuiltinDictionary);
            if (useBuiltinDictionary) {
                dictPathLabel.setText("Using built-in dictionary");
                dictPathLabel.setForeground(Color.GRAY);
            }
        });

        // 操作按钮
        startButton = new JButton("Start Attack");
        stopButton = new JButton("Stop");

        // 结果展示
        resultArea = new JTextArea(15, 60);
        resultArea.setEditable(false);
        resultScroll = new JScrollPane(resultArea);
        resultScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        //日志区域
        logArea = new JTextArea(5, 60);
        logArea.setEditable(false);



        // 初始化密钥展示区
        secretArea = new JTextArea(3, 60); // 较小的高度用于密钥显示
        secretArea.setCaretPosition(secretArea.getDocument().getLength());//自动滚动优化
        //// 当爆破失败时显示红色边框
        //secretArea.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        //// 成功时恢复
        //secretArea.setBorder(BorderFactory.createEmptyBorder());
        secretArea.setEditable(false);
        secretArea.setLineWrap(true);
        secretScroll = new JScrollPane(secretArea);
        secretScroll.setBorder(BorderFactory.createTitledBorder("Cracked Secret"));

        // 进度条
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        // 事件绑定
        loadDictButton.addActionListener(this::handleLoadDict);
        startButton.addActionListener(this::handleStartAttack);
        stopButton.addActionListener(this::handleStop);
    }



    private void initLayout() {
        setLayout(new MigLayout("wrap, fillx, insets 10", "[][grow]"));

        // 第一行：JWT输入
        add(new JLabel("JWT Token:"), "gapright 10");
        add(jwtField, "growx, span");

        // 第二行：算法选择
        add(new JLabel("Algorithm:"), "gapright 10");
        add(algorithmCombo, "split 3, width 100::");

        // 第三行：字典选择
        add(new JLabel("Dictionary:"), "newline, gapright 10");
        add(useBuiltinDictCheckbox, "split 3");
        add(loadDictButton, "width 120!");
        add(dictPathLabel, "gapleft 10, wrap");

        // 第四行：控制按钮
        add(startButton, "split 2, gapright 10, width 100!");
        add(stopButton, "width 100!, wrap");

        // 结果区域
        add(new JLabel("Results:"), "newline, gaptop 10");
        add(resultScroll, "grow, span, h 200::");

        //日志区域
        add(new JLabel("AttackLogs:"), "newline, gaptop 10");
        add(new JScrollPane(logArea), "newline, grow, span, h 80::");

        // 新增密钥展示区
        add(new JLabel("Found Secret:"), "newline, gaptop 10");
        add(secretScroll, "grow, span, h 60::");

//        //secret区域复制按钮
//        add(new JLabel("Found Secret:"), "newline, gaptop 10");
//        add(secretPanel, "grow, span, h 60::");

        // 进度条
        add(progressBar, "newline, growx, gaptop 5, span");
    }

    // 事件处理方法（需补充业务逻辑）
    private void handleLoadDict(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            dictPathLabel.setText(chooser.getSelectedFile().getAbsolutePath());
            dictPathLabel.setForeground(Color.BLACK);
            useBuiltinDictionary = false;
            useBuiltinDictCheckbox.setSelected(false);
        }
    }

    private void handleStartAttack(ActionEvent e) {
//        resultArea.setText(""); // 清空结果
//        secretArea.setText(""); //清空secretKey
//        progressBar.setValue(0);
//        progressBar.setVisible(true);
//        startButton.setEnabled(false);
        if (attackEngine != null) {
            attackEngine.reset(); // 重置引擎状态
        }
        try {
            // 添加输入校验
            if (jwtField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "JWT Token 不能为空", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            AttackConfig config;
            if (useBuiltinDictionary) {
                // 使用内置字典
                config = new AttackConfig(
                        jwtField.getText(),
                        (String) algorithmCombo.getSelectedItem(),
                        10
                );
            } else {
                // 使用用户选择的字典
                if (dictPathLabel.getText().equals("No dictionary selected") || 
                    dictPathLabel.getText().equals("Using built-in dictionary")) {
                    JOptionPane.showMessageDialog(this, "请选择字典文件", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                config = new AttackConfig(
                        jwtField.getText(),
                        (String) algorithmCombo.getSelectedItem(),
                        Paths.get(dictPathLabel.getText()),
                        10
                );
            }

            attackEngine = new JWTAttackEngine(config, this);
            attackEngine.start();

            // 重置UI状态
            SwingUtilities.invokeLater(() -> {
                resultArea.setText(JWTUtils.parseAndFormat(jwtField.getText()));
                secretArea.setText("");
                secretArea.setBackground(Color.WHITE);
                logArea.setText("");
                progressBar.setValue(0);
                progressBar.setVisible(true);
                startButton.setEnabled(false);
                stopButton.setEnabled(true); // 启用停止按钮
            });

        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                        "启动失败: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE
                );
                ex.printStackTrace();
            });
        }
        // TODO: 启动爆破线程

    }

    private void handleStop(ActionEvent e) {
//        // TODO: 停止爆破逻辑
//        startButton.setEnabled(true);
//        progressBar.setVisible(false);
        if (attackEngine != null) {
            attackEngine.stop();
        }
        startButton.setEnabled(true);
        progressBar.setVisible(false);
    }

    // 实现AttackListener接口方法
    @Override
    public void onSecretFound(String originalKey) {
        SwingUtilities.invokeLater(() -> {
            secretArea.setText(originalKey); // 显示原始密钥
            secretArea.setBackground(new Color(220, 255, 220));
        });
    }



    // 可在结果区域添加带颜色标记的输出
    public void appendResult(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            resultArea.setForeground(color);
            resultArea.append(text + "\n");
        });
    }

    // 更新进度条线程安全方法
    public void updateProgress(int progress) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(progress);
            if (progress >= 100) {
                progressBar.setVisible(false);
            }
        });
    }


    //记录日志过程
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[+] " + new Date() + " " + message + "\n");
        });
    }



    @Override
    public void onProgressUpdate(int progress) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
    }

    @Override
    public void onLogMessage(String message) {
        log(message);

    }

    @Override
    public void onAttackComplete() {
        SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(true);
            progressBar.setVisible(false);
        });
    }

    @Override
    public void onAttackFailed(String reason) {
        SwingUtilities.invokeLater(() -> {
            secretArea.setBackground(new Color(255, 220, 220));
            JOptionPane.showMessageDialog(this, reason, "Attack Failed", JOptionPane.ERROR_MESSAGE);
        });
    }
}