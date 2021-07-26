package com.shepa.filestorage.server;

import java.sql.*;

/**
 * В данном классе:
 * - выполняется подключение/создание базы данных пользователей
 * - содержатся методы по работе с базой данных пользователей
 */
public class DBConnector {

    private static Connection connection;
    private static Statement statement;

    public static void connect() throws ClassNotFoundException, SQLException {
        // Соединение с БД
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:file_storage_db.db");
        statement = connection.createStatement();

        // Создание таблицы пользователей
        createTableUsers();
    }

    /**
     * Выполняет создание таблицы "user" в случае ее отсутствия
     * @throws SQLException
     */
    private static void createTableUsers() throws SQLException {
        statement.execute("CREATE TABLE if not exists 'users' " +
                "('id' INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "'login' text, " +
                "'password' INT," +
                "'root_directory' text);");
    }

    /**
     * Проверяет наличие в базе данных пользователя с введенными учетными данными
     * @param login - имя пользователя
     * @param password - пароль
     * @return имя корневой папки,в случае наличия в БД пользователя с указанными учетными данными
     *         null в случае неверных учетных данных
     */
    public static String checkAuth(String login, String password) {
        String query = String.format("select root_directory, password from users where login = '%s'", login);
        try {
            ResultSet rs = statement.executeQuery(query);

            // Расчет хеша переданной строки-пароля и сравнение полученного значения со занчением в БД
            int myHash = password.hashCode();
            if(rs.next()){
                String root_directory = rs.getString(1);
                int dbHash = rs.getInt(2);
                if(myHash == dbHash){
                    return root_directory;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Добавляет в базу данных строку пользователя с указанными значениями логина/пароля
     * Определяет имя корневой папки (имя пользователя)
     * @param login - имя пользователя
     * @param password - пароль
     * @return количество измененных строк в случае корректного выполнения запроса
     *         0 в случае ошибки при работе с БД
     */
    public static int addUser(String login, String password) {
        try {
            String query = "INSERT INTO users (login, password, root_directory) VALUES (?, ?, ?);";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, login);
            ps.setInt(2, password.hashCode());
            ps.setString(3, login);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Закрытие соединения с БД
     */
    public static void disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
