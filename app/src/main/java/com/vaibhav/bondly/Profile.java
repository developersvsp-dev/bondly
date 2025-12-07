package com.vaibhav.bondly;

public class Profile {
    private String uid;
    private String name;
    private String phone;
    private String photoUrl;
    private String gender;
    private String bio;  // 🔥 ADDED BIO

    public Profile() {}

    public Profile(String uid, String name, String phone, String photoUrl, String gender, String bio) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.photoUrl = photoUrl;
        this.gender = gender;
        this.bio = bio;
    }

    // ALL GETTERS & SETTERS
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBio() { return bio; }      // 🔥 BIO GETTER
    public void setBio(String bio) { this.bio = bio; }  // 🔥 BIO SETTER
}
