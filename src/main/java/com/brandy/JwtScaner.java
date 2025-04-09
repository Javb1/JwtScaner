package com.brandy;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import com.brandy.ui.JWTAuditorTab;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class JwtScaner implements BurpExtension {
    private MontoyaApi api;
    private static final String EXTENSION_NAME = "JWT Scanner";
    private static final String VERSION = "1.0.0";

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        
        // 设置扩展名称
        api.extension().setName(EXTENSION_NAME);
        
        // 输出加载成功信息
        api.logging().logToOutput("==================================================");
        api.logging().logToOutput("JWT Scanner 加载成功!");
        api.logging().logToOutput("版本: " + VERSION);
        api.logging().logToOutput("作者: Brandy");
        api.logging().logToOutput("功能: JWT令牌爆破工具");
        api.logging().logToOutput("==================================================");
        
        // 创建并注册UI标签页
        JWTAuditorTab tab = new JWTAuditorTab(api);
        api.userInterface().registerSuiteTab("JWT Scanner", tab);
        
        // 输出UI注册成功信息
        api.logging().logToOutput("JWT Scanner UI标签页注册成功!");
    }
}
