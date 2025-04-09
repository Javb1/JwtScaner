package com.brandy.core.attack;

import com.brandy.core.model.AttackConfig;
import com.brandy.core.model.KeyCandidate;
import com.brandy.core.utils.DictLoader;
import com.brandy.core.utils.JWTUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.Timer;
import java.util.TimerTask;


public class JWTAttackEngine {
    private final AttackConfig config;
    private final AttackListener listener;
    private ExecutorService executor;
    private volatile boolean isStopped;
    private ExecutorService workerPool;

    public JWTAttackEngine(AttackConfig config, AttackListener listener) {
        this.config = config;
        this.listener = listener;
    }

    public void start() {
        // 使用线程池替代裸线程
        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                doAttack();
            } finally {
                executor.shutdown();
            }
        });
    }

    public void stop() {
        isStopped = true;
        if (executor != null) {
            executor.shutdownNow();
        }
        if (workerPool != null) {
            workerPool.shutdownNow();
        }
    }
    //重置方法
    public void reset() {
        this.isStopped = false;
        this.executor = null;
        this.workerPool = null;
    }
    private void doAttack() {
        // 添加一个标志，表示是否找到了正确的密钥
        AtomicInteger secretFound = new AtomicInteger(0);
        
        try {
            // 1. 解析 JWT
            final String[] jwtParts = JWTUtils.parseJWT(config.getJwtToken());
            final String unsignedToken = jwtParts[0] + "." + jwtParts[1];
            final String targetSignature = jwtParts[2];

            // 2. 加载字典（捕获IO异常）
            List<KeyCandidate> candidates;

            try {
                if (config.isUseBuiltinDictionary()) {
                    // 使用内置字典
                    try {
                        candidates = DictLoader.loadBuiltinDictionary()
                                .collect(Collectors.toList());
                        listener.onLogMessage("成功加载内置字典，密钥总数: " + candidates.size());
                    } catch (IOException e) {
                        // 详细记录异常信息
                        listener.onLogMessage("加载内置字典失败: " + e.getMessage());
                        e.printStackTrace();
                        throw e;
                    }
                } else {
                    // 使用用户选择的字典
                    try {
                        candidates = DictLoader.loadDictionary(config.getDictionaryPath())
                                .collect(Collectors.toList());
                        listener.onLogMessage("成功加载字典，密钥总数: " + candidates.size());
                    } catch (IOException e) {
                        // 详细记录异常信息
                        listener.onLogMessage("加载用户字典失败: " + e.getMessage());
                        e.printStackTrace();
                        throw e;
                    }
                }
                listener.onLogMessage("目标签名: " + targetSignature);
            } catch (IOException e) {
                listener.onAttackFailed("字典加载失败: " + e.getMessage());
                return;
            }

            // 3. 初始化线程池 - 根据字典大小动态调整线程数
            int optimalThreadCount = Math.min(config.getThreadCount(), 
                    Math.max(1, Runtime.getRuntime().availableProcessors() * 2));
            listener.onLogMessage("使用线程数: " + optimalThreadCount);
            workerPool = Executors.newFixedThreadPool(optimalThreadCount);
            
            AtomicInteger processedCount = new AtomicInteger(0);
            AtomicInteger completedTasks = new AtomicInteger(0);
            AtomicInteger totalTasks = new AtomicInteger(0);
            
            // 添加进度更新定时器，定期更新进度
            int updateInterval = Math.max(1, candidates.size() / 100); // 每1%更新一次
            AtomicInteger lastUpdateCount = new AtomicInteger(0);
            
            // 创建进度更新定时器
            Timer progressTimer = new Timer();
            progressTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    int currentCount = processedCount.get();
                    int lastCount = lastUpdateCount.get();
                    
                    if (currentCount > lastCount) {
                        int progress = (int) ((currentCount * 100.0) / candidates.size());
                        listener.onProgressUpdate(progress);
                        lastUpdateCount.set(currentCount);
                    }
                }
            }, 1000, 1000); // 每秒更新一次

            // 4. 分批提交任务，避免一次性提交过多任务导致内存压力
            int batchSize = 1000; // 每批处理1000个密钥
            int totalCandidates = candidates.size();
            
            for (int i = 0; i < totalCandidates; i += batchSize) {
                if (isStopped) break;
                
                int endIndex = Math.min(i + batchSize, totalCandidates);
                List<KeyCandidate> batch = candidates.subList(i, endIndex);
                
                // 提交这一批任务
                for (KeyCandidate candidate : batch) {
                    if (isStopped) break;
                    
                    final KeyCandidate finalCandidate = candidate;
                    totalTasks.incrementAndGet();
                    
                    workerPool.submit(() -> {
                        try {
                            if (isStopped) return;
                            
                            // 减少日志输出频率，避免日志过多
                            if (processedCount.get() % 100 == 0) {
                                listener.onLogMessage("正在测试密钥: " + finalCandidate.getOriginalKey()
                                        + " (编码形式: " + finalCandidate.getEncodedKey() + ")");
                            }
                            
                            boolean isValid = verifySignature(
                                    unsignedToken,
                                    targetSignature,
                                    finalCandidate.getEncodedKey()
                            );

                            // 更新进度
                            processedCount.incrementAndGet();

                            if (isValid) {
                                // 设置找到密钥的标志
                                secretFound.set(1);
                                // 更新进度到100%
                                listener.onProgressUpdate(100);
                                // 通知找到密钥
                                listener.onSecretFound(finalCandidate.getOriginalKey());
                                // 显示弹窗提示
                                showSecretFoundDialog(finalCandidate.getOriginalKey());
                                // 恢复startButton状态
                                resetStartButton();
                                stop();
                            }
                        } catch (Exception e) {
                            // 减少异常日志输出频率
                            if (completedTasks.get() % 1000 == 0) {
                                listener.onLogMessage("任务执行异常: " + e.getMessage());
                            }
                        } finally {
                            completedTasks.incrementAndGet();
                        }
                    });
                }
                
                // 等待当前批次完成，避免内存压力过大
                if (!isStopped && i + batchSize < totalCandidates) {
                    // 短暂等待，让已提交的任务有机会执行
                    Thread.sleep(100);
                }
            }

            // 5. 等待线程池结束或超时
            workerPool.shutdown();
            
            // 取消进度更新定时器
            progressTimer.cancel();
            
            // 设置更合理的超时时间，例如30秒
            boolean terminated = workerPool.awaitTermination(30, TimeUnit.SECONDS);
            
            if (!terminated) {
                listener.onLogMessage("攻击超时，强制停止");
                workerPool.shutdownNow();
            }
            
            // 检查是否找到了密钥
            if (secretFound.get() == 1) {
                // 如果找到了密钥，直接返回，不执行后续代码
                return;
            }
            
            // 检查是否所有任务都已完成
            if (completedTasks.get() == totalTasks.get()) {
                listener.onAttackComplete();
                // 密钥未找到，显示提示
                showSecretNotFoundDialog();
                // 恢复startButton状态
                resetStartButton();
            } else {
                listener.onLogMessage("攻击未完全完成，已完成: " + completedTasks.get() + "/" + totalTasks.get());
                // 恢复startButton状态
                resetStartButton();
            }

        } catch (Exception e) {
            // 只有在没有找到密钥的情况下才执行异常处理
            if (secretFound.get() == 0) {
                // 详细记录异常信息
                listener.onLogMessage("攻击异常: " + e.getMessage());
                e.printStackTrace();
                listener.onAttackFailed("攻击异常: " + e.getMessage());
                // 恢复startButton状态
                resetStartButton();
            }
        } finally {
            if (workerPool != null && !workerPool.isShutdown()) {
                workerPool.shutdownNow();
            }
        }
    }

    private String getJavaAlgorithm(String jwtAlgorithm) {
        return switch (jwtAlgorithm) {
            case "HS256" -> "HmacSHA256";
            case "HS384" -> "HmacSHA384";
            case "HS512" -> "HmacSHA512";
            case "RS256" -> "SHA256withRSA";
            case "RS384" -> "SHA384withRSA";
            case "RS512" -> "SHA512withRSA";
            // 添加其他算法支持
            default -> throw new IllegalArgumentException("Unsupported algorithm: " + jwtAlgorithm);
        };
    }
    private byte[] processKeyEncoding(String encodedKey, String algorithm) {
        // 直接使用预先生成的编码密钥字节
        return encodedKey.getBytes(StandardCharsets.UTF_8);
    }

//    private boolean isHex(String str) {
//        // 允许偶数长度并忽略大小写
//        return str.length() % 2 == 0 && str.matches("^[0-9a-fA-F]*$");
//    }

    private boolean verifySignature(String unsignedToken, String targetSig, String key) {
        try {
            String javaAlgo = getJavaAlgorithm(config.getAlgorithm());
            Mac mac = Mac.getInstance(javaAlgo);
            // 修正后的密钥处理
            byte[] keyBytes = processKeyEncoding(key, config.getAlgorithm());
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, javaAlgo);

            mac.init(secretKey);
            byte[] calculatedSig = mac.doFinal(unsignedToken.getBytes());

            String encodedSig = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(calculatedSig);

            return encodedSig.equals(targetSig);
        } catch (Exception e) {
            listener.onLogMessage("Error verifying ["+key+"]: "+e.getMessage());
            return false;
        }
    }
    
    /**
     * 显示找到密钥的弹窗
     * @param secret 找到的密钥
     */
    private void showSecretFoundDialog(String secret) {
        // 使用SwingUtilities确保在EDT线程中执行UI操作
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JOptionPane.showMessageDialog(
                null,
                "恭喜！已找到正确的密钥：\n" + secret,
                "密钥找到",
                javax.swing.JOptionPane.INFORMATION_MESSAGE
            );
        });
    }
    
    /**
     * 显示未找到密钥的弹窗
     */
    private void showSecretNotFoundDialog() {
        // 使用SwingUtilities确保在EDT线程中执行UI操作
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JOptionPane.showMessageDialog(
                null,
                "很遗憾，未找到正确的密钥。\n请尝试使用其他字典或调整攻击参数。",
                "密钥未找到",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
        });
    }
    
    /**
     * 恢复startButton状态
     */
    private void resetStartButton() {
        // 使用SwingUtilities确保在EDT线程中执行UI操作
        javax.swing.SwingUtilities.invokeLater(() -> {
            // 通知监听器恢复startButton状态
            listener.onAttackComplete();
        });
    }
}
