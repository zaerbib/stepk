package org.io.service.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.json.JsonObject;
import lombok.Getter;

@Getter
@DataObject(generateConverter = true, publicConverter = false)
public class User {

  private String username;
  private String password;
  private String email;
  private String city;
  private String deviceId;
  private String makePublic;

  public User(String username,
              String password,
              String email,
              String city,
              String deviceId,
              String makePublic) {
    this.username = username;
    this.password = password;
    this.email = email;
    this.city = city;
    this.deviceId = deviceId;
    this.makePublic = makePublic;
  }

  public User(JsonObject jsonObject) {
    UserConverter.fromJson(jsonObject, this);
  }

  public User(User other) {
    this.username = other.username;
    this.password = other.password;
    this.email = other.email;
    this.city = other.city;
    this.deviceId = other.deviceId;
    this.makePublic = other.makePublic;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    UserConverter.toJson(this, json);
    return json;
  }


  @Fluent
  public void setUsername(String username) {
    this.username = username;
  }


  @Fluent
  public void setPassword(String password) {
    this.password = password;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @Fluent
  public void setCity(String city) {
    this.city = city;
  }


  @Fluent
  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  @Fluent
  public void setMakePublic(String makePublic) {
    this.makePublic = makePublic;
  }
}
