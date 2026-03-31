package com.holodos.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "google_subject", nullable = false, unique = true)
    private String googleSubject;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String name;

    @Column
    private String picture;

    protected User() {}

    public User(String googleSubject, String email, String name, String picture) {
        this.googleSubject = googleSubject;
        this.email = email;
        this.name = name;
        this.picture = picture;
    }

    public String getGoogleSubject() { return googleSubject; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getPicture() { return picture; }

    public void update(String email, String name, String picture) {
        this.email = email;
        this.name = name;
        this.picture = picture;
    }
}
