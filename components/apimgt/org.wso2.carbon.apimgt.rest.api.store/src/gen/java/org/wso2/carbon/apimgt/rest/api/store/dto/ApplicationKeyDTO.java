package org.wso2.carbon.apimgt.rest.api.store.dto;

import org.wso2.carbon.apimgt.rest.api.store.dto.TokenDTO;
import java.util.*;

import io.swagger.annotations.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;



@ApiModel(description = "")
public class ApplicationKeyDTO  {
  
  
  
  private String consumerKey = null;
  
  
  private String consumerSecret = null;
  
  
  private List<String> supportedGrantTypes = new ArrayList<String>();
  
  
  private String keyState = null;
  
  public enum KeyTypeEnum {
     PRODUCTION,  SANDBOX, 
  };
  
  private KeyTypeEnum keyType = null;
  
  
  private TokenDTO token = null;

  
  /**
   * The consumer key associated with the application and identifying the client
   **/
  @ApiModelProperty(value = "The consumer key associated with the application and identifying the client")
  @JsonProperty("consumerKey")
  public String getConsumerKey() {
    return consumerKey;
  }
  public void setConsumerKey(String consumerKey) {
    this.consumerKey = consumerKey;
  }

  
  /**
   * The client secret that is used to authenticate the client with the authentication server
   **/
  @ApiModelProperty(value = "The client secret that is used to authenticate the client with the authentication server")
  @JsonProperty("consumerSecret")
  public String getConsumerSecret() {
    return consumerSecret;
  }
  public void setConsumerSecret(String consumerSecret) {
    this.consumerSecret = consumerSecret;
  }

  
  /**
   * The grant types that are supported by the application
   **/
  @ApiModelProperty(value = "The grant types that are supported by the application")
  @JsonProperty("supportedGrantTypes")
  public List<String> getSupportedGrantTypes() {
    return supportedGrantTypes;
  }
  public void setSupportedGrantTypes(List<String> supportedGrantTypes) {
    this.supportedGrantTypes = supportedGrantTypes;
  }

  
  /**
   * Describes the state of the key generation.
   **/
  @ApiModelProperty(value = "Describes the state of the key generation.")
  @JsonProperty("keyState")
  public String getKeyState() {
    return keyState;
  }
  public void setKeyState(String keyState) {
    this.keyState = keyState;
  }

  
  /**
   * Describes to which endpoint the key belongs
   **/
  @ApiModelProperty(value = "Describes to which endpoint the key belongs")
  @JsonProperty("keyType")
  public KeyTypeEnum getKeyType() {
    return keyType;
  }
  public void setKeyType(KeyTypeEnum keyType) {
    this.keyType = keyType;
  }

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("token")
  public TokenDTO getToken() {
    return token;
  }
  public void setToken(TokenDTO token) {
    this.token = token;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApplicationKeyDTO {\n");
    
    sb.append("  consumerKey: ").append(consumerKey).append("\n");
    sb.append("  consumerSecret: ").append(consumerSecret).append("\n");
    sb.append("  supportedGrantTypes: ").append(supportedGrantTypes).append("\n");
    sb.append("  keyState: ").append(keyState).append("\n");
    sb.append("  keyType: ").append(keyType).append("\n");
    sb.append("  token: ").append(token).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
