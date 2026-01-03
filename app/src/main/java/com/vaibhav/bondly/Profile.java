package com.vaibhav.bondly;

import android.os.Parcel;
import android.os.Parcelable;

public class Profile implements Parcelable {
    private String uid;
    private String name;
    private String phone;        // ✅ YOUR FIELD - KEPT
    private String photoUrl;
    private String gender;
    private String bio;
    private boolean isLikedByMe = false;
    private int likesCount = 0;

    public Profile() {}

    public Profile(String uid, String name, String phone, String photoUrl, String gender, String bio) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.photoUrl = photoUrl;
        this.gender = gender;
        this.bio = bio;
    }

    // 🔥 PARCELABLE CONSTRUCTOR (NEW)
    protected Profile(Parcel in) {
        uid = in.readString();
        name = in.readString();
        phone = in.readString();
        photoUrl = in.readString();
        gender = in.readString();
        bio = in.readString();
        isLikedByMe = in.readByte() != 0;
        likesCount = in.readInt();
    }

    // 🔥 PARCELABLE CREATOR (NEW)
    public static final Creator<Profile> CREATOR = new Creator<Profile>() {
        @Override
        public Profile createFromParcel(Parcel in) {
            return new Profile(in);
        }

        @Override
        public Profile[] newArray(int size) {
            return new Profile[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(name);
        dest.writeString(phone);
        dest.writeString(photoUrl);
        dest.writeString(gender);
        dest.writeString(bio);
        dest.writeByte((byte) (isLikedByMe ? 1 : 0));
        dest.writeInt(likesCount);
    }

    // 🔥 ALL YOUR EXISTING GETTERS/SETTERS (UNCHANGED)
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

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public boolean isLikedByMe() { return isLikedByMe; }
    public void setLikedByMe(boolean likedByMe) { this.isLikedByMe = likedByMe; }
    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
}
