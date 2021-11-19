package com.shepa.filestorage;

/**
 * Сообщение аутентикации
 * Передает на сервер учетные данные и возвращает на клиент результат
 */
public class AuthMessage extends ExchangeMessage{

    private final String login;
    private final String password;
    private String userDir;
    private boolean loginSuccessful;

    public AuthMessage(String login, String password, ExchangeMessageTypes type) {
        super(type);
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getUserDir() {
        return userDir;
    }

    public void setLoginSuccessful(String userDir) {
        if (userDir != null) {
            this.userDir = userDir;
            loginSuccessful = true;
        }
    }

    public boolean isLoginSuccessful() {
        return loginSuccessful;
    }

}
