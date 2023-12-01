class User {
    String username;
    String password;
    String userId;
    boolean isAdmin;

    public User(String username, String userId, String password, boolean isAdmin) {
        this.username = username;
        this.password = password;
        this.userId = userId;
        this.isAdmin = isAdmin;
    }
}
