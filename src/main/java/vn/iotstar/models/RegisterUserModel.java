package vn.iotstar.models;

public class RegisterUserModel {
    private String email;
    private String password;
    private String fullName;
    private String images;

    public RegisterUserModel() {
    }

    public RegisterUserModel(String email, String password, String fullName, String images) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.images = images;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }
}
