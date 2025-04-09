package com.brandy.core.attack;

public interface AttackListener {

    void onSecretFound(String secret);
    void onProgressUpdate(int progress);
    void onLogMessage(String message);
    void onAttackComplete();
    void onAttackFailed(String reason);
}
