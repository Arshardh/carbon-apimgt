/*
 *
 *   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.apimgt.impl.utils;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.util.threadpool.ThreadFactory;
import org.apache.axis2.util.threadpool.ThreadPool;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.APIStatus;
import org.wso2.carbon.apimgt.api.model.CORSConfiguration;
import org.wso2.carbon.apimgt.api.model.Documentation;
import org.wso2.carbon.apimgt.api.model.DocumentationType;
import org.wso2.carbon.apimgt.api.model.KeyManager;
import org.wso2.carbon.apimgt.api.model.KeyManagerConfiguration;
import org.wso2.carbon.apimgt.api.model.Provider;
import org.wso2.carbon.apimgt.api.model.Scope;
import org.wso2.carbon.apimgt.api.model.Tier;
import org.wso2.carbon.apimgt.api.model.URITemplate;
import org.wso2.carbon.apimgt.api.model.policy.PolicyConstants;
import org.wso2.carbon.apimgt.api.model.policy.QuotaPolicy;
import org.wso2.carbon.apimgt.api.model.policy.RequestCountLimit;
import org.wso2.carbon.apimgt.api.model.policy.SubscriptionPolicy;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIMRegistryServiceImpl;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationServiceImpl;
import org.wso2.carbon.apimgt.impl.ServiceReferenceHolderMockCreator;
import org.wso2.carbon.apimgt.impl.TestUtils;
import org.wso2.carbon.apimgt.impl.clients.ApplicationManagementServiceClient;
import org.wso2.carbon.apimgt.impl.clients.OAuthAdminClient;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.dto.APIKeyValidationInfoDTO;
import org.wso2.carbon.apimgt.impl.dto.Environment;
import org.wso2.carbon.apimgt.impl.dto.ThrottleProperties;
import org.wso2.carbon.apimgt.impl.factory.KeyManagerHolder;
import org.wso2.carbon.apimgt.impl.internal.APIManagerComponent;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.keymgt.client.SubscriberKeyMgtClient;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.governance.api.common.dataobjects.GovernanceArtifact;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceArtifactConfiguration;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.Tag;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.realm.RegistryAuthorizationManager;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Matchers.eq;
import static org.wso2.carbon.apimgt.impl.utils.APIUtil.DISABLE_ROLE_VALIDATION_AT_SCOPE_CREATION;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {LogFactory.class, ServiceReferenceHolder.class, SSLSocketFactory.class, CarbonUtils.class,
        GovernanceUtils.class, AuthorizationManager.class, MultitenantUtils.class, GenericArtifactManager.class,
        APIUtil.class, KeyManagerHolder.class, SubscriberKeyMgtClient.class, ApplicationManagementServiceClient
        .class, OAuthAdminClient.class, ApiMgtDAO.class, AXIOMUtil.class, OAuthServerConfiguration.class,
        RegistryUtils.class, RegistryAuthorizationManager.class, RegistryContext.class, PrivilegedCarbonContext.class,
        APIManagerComponent.class, TenantAxisUtils.class })
@PowerMockIgnore("javax.net.ssl.*")
public class APIUtilTest {

    @Test
    public void testGetAPINamefromRESTAPI() throws Exception {
        String restAPI = "admin--map";
        String apiName = APIUtil.getAPINamefromRESTAPI(restAPI);

        Assert.assertEquals(apiName, "map");
    }

    @Test
    public void testGetAPIProviderFromRESTAPI() throws Exception {
        String restAPI = "admin--map";
        String providerName = APIUtil.getAPIProviderFromRESTAPI(restAPI, null);

        Assert.assertEquals(providerName, "admin@carbon.super");

        restAPI = "user@test.com--map";
        providerName = APIUtil.getAPIProviderFromRESTAPI(restAPI, "test.com");
        Assert.assertEquals(providerName, "user@test.com");

        restAPI = "user-AT-test.com--map";
        providerName = APIUtil.getAPIProviderFromRESTAPI(restAPI, "test.com");
        Assert.assertEquals(providerName, "user@test.com");

    }

    @Test
    public void testGetHttpClient() throws Exception {
        Log log = Mockito.mock(Log.class);
        PowerMockito.mockStatic(LogFactory.class);
        Mockito.when(LogFactory.getLog(Mockito.any(Class.class))).thenReturn(log);

        SSLSocketFactory socketFactory = Mockito.mock(SSLSocketFactory.class);
        PowerMockito.mockStatic(SSLSocketFactory.class);
        Mockito.when(SSLSocketFactory.getSocketFactory()).thenReturn(socketFactory);

        ServiceReferenceHolderMockCreator holderMockCreator = new ServiceReferenceHolderMockCreator(1);
        ServiceReferenceHolderMockCreator.initContextService();

        HttpClient client = APIUtil.getHttpClient(3244, "http");

        Assert.assertNotNull(client);
        Scheme scheme = client.getConnectionManager().getSchemeRegistry().get("http");
        Assert.assertEquals(3244, scheme.getDefaultPort());

        client = APIUtil.getHttpClient(3244, "https");
        Assert.assertNotNull(client);
        scheme = client.getConnectionManager().getSchemeRegistry().get("https");
        Assert.assertEquals(3244, scheme.getDefaultPort());

        client = APIUtil.getHttpClient(-1, "http");
        Assert.assertNotNull(client);
        scheme = client.getConnectionManager().getSchemeRegistry().get("http");
        Assert.assertEquals(80, scheme.getDefaultPort());

        client = APIUtil.getHttpClient(-1, "https");
        Assert.assertNotNull(client);
        scheme = client.getConnectionManager().getSchemeRegistry().get("https");
        Assert.assertEquals(443, scheme.getDefaultPort());
    }

    @Test
    public void testGetHttpClientIgnoreHostNameVerify() throws Exception {
        Log log = Mockito.mock(Log.class);
        PowerMockito.mockStatic(LogFactory.class);
        Mockito.when(LogFactory.getLog(Mockito.any(Class.class))).thenReturn(log);

        SSLSocketFactory socketFactory = Mockito.mock(SSLSocketFactory.class);
        PowerMockito.mockStatic(SSLSocketFactory.class);
        Mockito.when(SSLSocketFactory.getSocketFactory()).thenReturn(socketFactory);

        ServiceReferenceHolderMockCreator holderMockCreator = new ServiceReferenceHolderMockCreator(1);
        ServiceReferenceHolderMockCreator.initContextService();

        System.setProperty("org.wso2.ignoreHostnameVerification", "true");
        HttpClient client = APIUtil.getHttpClient(3244, "https");

        Assert.assertNotNull(client);
    }

    /*
    @Test
    public void testGetHttpClientSSLVerifyClient() throws Exception {
        System.setProperty("carbon.home", "");

        Log log = Mockito.mock(Log.class);
        PowerMockito.mockStatic(LogFactory.class);
        Mockito.when(LogFactory.getLog(Mockito.any(Class.class))).thenReturn(log);

        SSLSocketFactory socketFactory = Mockito.mock(SSLSocketFactory.class);
        PowerMockito.mockStatic(SSLSocketFactory.class);
        Mockito.when(SSLSocketFactory.getSocketFactory()).thenReturn(socketFactory);

        ServiceReferenceHolderMockCreator holderMockCreator = new ServiceReferenceHolderMockCreator(1);
        ServiceReferenceHolderMockCreator.initContextService();

        TransportInDescription transportInDescription = holderMockCreator.getConfigurationContextServiceMockCreator().
                getContextMockCreator().getConfigurationMockCreator().getTransportInDescription();

        Parameter sslVerifyClient = Mockito.mock(Parameter.class);
        Mockito.when(transportInDescription.getParameter(APIConstants.SSL_VERIFY_CLIENT)).thenReturn(sslVerifyClient);
        Mockito.when(sslVerifyClient.getValue()).thenReturn(APIConstants.SSL_VERIFY_CLIENT_STATUS_REQUIRE);

        System.setProperty("org.wso2.ignoreHostnameVerification", "true");

        File keyStore = new File(Thread.currentThread().getContextClassLoader().
                getResource("wso2carbon.jks").getFile());

        ServerConfiguration serverConfiguration = Mockito.mock(ServerConfiguration.class);
        PowerMockito.mockStatic(CarbonUtils.class);
        Mockito.when(CarbonUtils.getServerConfiguration()).thenReturn(serverConfiguration);

        Mockito.when(serverConfiguration.getFirstProperty("Security.KeyStore.Location")).
                thenReturn(keyStore.getAbsolutePath());
        Mockito.when(serverConfiguration.getFirstProperty("Security.KeyStore.Password")).
                thenReturn("wso2carbon");

        InputStream inputStream = new FileInputStream(keyStore.getAbsolutePath());
        KeyStore keystore = KeyStore.getInstance("JKS");
        char[] pwd = "wso2carbon".toCharArray();
        keystore.load(inputStream, pwd);
        SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(keystore).useSSL().build();
        SSLContext.setDefault(sslcontext);

        HttpClient client = APIUtil.getHttpClient(3244, "https");

        Assert.assertNotNull(client);
    }
    */

    @Test
    public void testIsValidURL() throws Exception {
        String validURL = "http://fsdfsfd.sda";

        Assert.assertTrue(APIUtil.isValidURL(validURL));

        String invalidURL = "sadafvsdfwef";

        Assert.assertFalse(APIUtil.isValidURL(invalidURL));
        Assert.assertFalse(APIUtil.isValidURL(null));
    }

    @Test
    public void testgGetUserNameWithTenantSuffix() throws Exception {
        String plainUserName = "john";

        String userNameWithTenantSuffix = APIUtil.getUserNameWithTenantSuffix(plainUserName);

        Assert.assertEquals("john@carbon.super", userNameWithTenantSuffix);

        String userNameWithDomain = "john@smith.com";

        userNameWithTenantSuffix = APIUtil.getUserNameWithTenantSuffix(userNameWithDomain);

        Assert.assertEquals("john@smith.com", userNameWithTenantSuffix);
    }

    @Test
    public void testGetRESTAPIScopesFromConfig() throws Exception {
        File siteConfFile = new File(Thread.currentThread().getContextClassLoader().
                getResource("tenant-conf.json").getFile());

        String tenantConfValue = FileUtils.readFileToString(siteConfFile);

        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(tenantConfValue);
        JSONObject restapiScopes = (JSONObject) json.get("RESTAPIScopes");

        Map<String, String> expectedScopes = new HashMap<String, String>();
        JSONArray scopes = (JSONArray) restapiScopes.get("Scope");

        for (Object scopeObj : scopes) {
            JSONObject scope = (JSONObject) scopeObj;
            String name = (String) scope.get("Name");
            String roles = (String) scope.get("Roles");
            expectedScopes.put(name, roles);
        }

        Map<String, String> restapiScopesFromConfig = APIUtil.getRESTAPIScopesFromConfig(restapiScopes);

        Assert.assertEquals(expectedScopes, restapiScopesFromConfig);
    }

    @Test
    public void testIsSandboxEndpointsExists() throws Exception {
        API api = Mockito.mock(API.class);

        JSONObject sandboxEndpoints = new JSONObject();
        sandboxEndpoints.put("url", "https:\\/\\/localhost:9443\\/am\\/sample\\/pizzashack\\/v1\\/api\\/");
        sandboxEndpoints.put("config", null);

        JSONObject root = new JSONObject();
        root.put("sandbox_endpoints", sandboxEndpoints);
        root.put("endpoint_type", "http");

        Mockito.when(api.getEndpointConfig()).thenReturn(root.toJSONString());

        Assert.assertTrue("Cannot find sandbox endpoint", APIUtil.isSandboxEndpointsExists(api));
    }

    @Test
    public void testIsSandboxEndpointsNotExists() throws Exception {
        API api = Mockito.mock(API.class);

        JSONObject productionEndpoints = new JSONObject();
        productionEndpoints.put("url", "https:\\/\\/localhost:9443\\/am\\/sample\\/pizzashack\\/v1\\/api\\/");
        productionEndpoints.put("config", null);

        JSONObject root = new JSONObject();
        root.put("production_endpoints", productionEndpoints);
        root.put("endpoint_type", "http");

        Mockito.when(api.getEndpointConfig()).thenReturn(root.toJSONString());

        Assert.assertFalse("Unexpected sandbox endpoint found", APIUtil.isSandboxEndpointsExists(api));
    }

    @Test
    public void testIsProductionEndpointsExists() throws Exception {
        API api = Mockito.mock(API.class);

        JSONObject productionEndpoints = new JSONObject();
        productionEndpoints.put("url", "https:\\/\\/localhost:9443\\/am\\/sample\\/pizzashack\\/v1\\/api\\/");
        productionEndpoints.put("config", null);

        JSONObject root = new JSONObject();
        root.put("production_endpoints", productionEndpoints);
        root.put("endpoint_type", "http");

        Mockito.when(api.getEndpointConfig()).thenReturn(root.toJSONString());

        Assert.assertTrue("Cannot find production endpoint", APIUtil.isProductionEndpointsExists(api));
    }

    @Test
    public void testIsProductionEndpointsNotExists() throws Exception {
        API api = Mockito.mock(API.class);

        JSONObject sandboxEndpoints = new JSONObject();
        sandboxEndpoints.put("url", "https:\\/\\/localhost:9443\\/am\\/sample\\/pizzashack\\/v1\\/api\\/");
        sandboxEndpoints.put("config", null);

        JSONObject root = new JSONObject();
        root.put("sandbox_endpoints", sandboxEndpoints);
        root.put("endpoint_type", "http");

        Mockito.when(api.getEndpointConfig()).thenReturn(root.toJSONString());

        Assert.assertFalse("Unexpected production endpoint found", APIUtil.isProductionEndpointsExists(api));
    }

    @Test
    public void testIsProductionSandboxEndpointsExists() throws Exception {
        API api = Mockito.mock(API.class);

        JSONObject productionEndpoints = new JSONObject();
        productionEndpoints.put("url", "https:\\/\\/localhost:9443\\/am\\/sample\\/pizzashack\\/v1\\/api\\/");
        productionEndpoints.put("config", null);

        JSONObject sandboxEndpoints = new JSONObject();
        sandboxEndpoints.put("url", "https:\\/\\/localhost:9443\\/am\\/sample\\/pizzashack\\/v1\\/api\\/");
        sandboxEndpoints.put("config", null);

        JSONObject root = new JSONObject();
        root.put("production_endpoints", productionEndpoints);
        root.put("sandbox_endpoints", sandboxEndpoints);
        root.put("endpoint_type", "http");

        Mockito.when(api.getEndpointConfig()).thenReturn(root.toJSONString());

        Assert.assertTrue("Cannot find production endpoint", APIUtil.isProductionEndpointsExists(api));
        Assert.assertTrue("Cannot find sandbox endpoint", APIUtil.isSandboxEndpointsExists(api));
    }

    @Test
    public void testIsProductionEndpointsInvalidJSON() throws Exception {
        Log log = Mockito.mock(Log.class);
        PowerMockito.mockStatic(LogFactory.class);
        Mockito.when(LogFactory.getLog(Mockito.any(Class.class))).thenReturn(log);

        API api = Mockito.mock(API.class);

        Mockito.when(api.getEndpointConfig()).thenReturn("</SomeXML>");

        Assert.assertFalse("Unexpected production endpoint found", APIUtil.isProductionEndpointsExists(api));

        JSONObject productionEndpoints = new JSONObject();
        productionEndpoints.put("url", "https:\\/\\/localhost:9443\\/am\\/sample\\/pizzashack\\/v1\\/api\\/");
        productionEndpoints.put("config", null);
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(productionEndpoints);

        Mockito.when(api.getEndpointConfig()).thenReturn(jsonArray.toJSONString());

        Assert.assertFalse("Unexpected production endpoint found", APIUtil.isProductionEndpointsExists(api));
    }

    @Test
    public void testIsSandboxEndpointsInvalidJSON() throws Exception {
        Log log = Mockito.mock(Log.class);
        PowerMockito.mockStatic(LogFactory.class);
        Mockito.when(LogFactory.getLog(Mockito.any(Class.class))).thenReturn(log);

        API api = Mockito.mock(API.class);

        Mockito.when(api.getEndpointConfig()).thenReturn("</SomeXML>");

        Assert.assertFalse("Unexpected sandbox endpoint found", APIUtil.isSandboxEndpointsExists(api));

        JSONObject sandboxEndpoints = new JSONObject();
        sandboxEndpoints.put("url", "https:\\/\\/localhost:9443\\/am\\/sample\\/pizzashack\\/v1\\/api\\/");
        sandboxEndpoints.put("config", null);
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(sandboxEndpoints);

        Mockito.when(api.getEndpointConfig()).thenReturn(jsonArray.toJSONString());

        Assert.assertFalse("Unexpected sandbox endpoint found", APIUtil.isSandboxEndpointsExists(api));
    }

    @Test
    public void testGetAPIInformation() throws Exception {
        GovernanceArtifact artifact = Mockito.mock(GovernanceArtifact.class);
        Registry registry = Mockito.mock(Registry.class);
        Resource resource = Mockito.mock(Resource.class);

        API expectedAPI = getUniqueAPI();

        String artifactPath = "";
        PowerMockito.mockStatic(GovernanceUtils.class);
        Mockito.when(GovernanceUtils.getArtifactPath(registry, expectedAPI.getUUID())).thenReturn(artifactPath);
        Mockito.when(registry.get(artifactPath)).thenReturn(resource);
        Mockito.when(resource.getLastModified()).thenReturn(expectedAPI.getLastUpdated());

        DateFormat df = new SimpleDateFormat("E MMM dd HH:mm:ss zzz yyyy");
        Date createdTime = df.parse(expectedAPI.getCreatedTime());
        Mockito.when(resource.getCreatedTime()).thenReturn(createdTime);

        ServiceReferenceHolderMockCreator holderMockCreator = new ServiceReferenceHolderMockCreator(1);
        APIManagerConfiguration apimConfiguration = holderMockCreator.getConfigurationServiceMockCreator().
                getConfigurationMockCreator().getMock();

        CORSConfiguration corsConfiguration = expectedAPI.getCorsConfiguration();

        Mockito.when(apimConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_HEADERS)).
                thenReturn(corsConfiguration.getAccessControlAllowHeaders().toString());
        Mockito.when(apimConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_METHODS)).
                thenReturn(corsConfiguration.getAccessControlAllowMethods().toString());
        Mockito.when(apimConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_ORIGIN)).
                thenReturn(corsConfiguration.getAccessControlAllowOrigins().toString());


        Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER)).
                thenReturn(expectedAPI.getId().getProviderName());
        Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_NAME)).
                thenReturn(expectedAPI.getId().getApiName());
        Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_VERSION)).
                thenReturn(expectedAPI.getId().getVersion());
        Mockito.when(artifact.getId()).thenReturn(expectedAPI.getUUID());

        API api = APIUtil.getAPIInformation(artifact, registry);

        Assert.assertEquals(expectedAPI.getId(), api.getId());
        Assert.assertEquals(expectedAPI.getUUID(), api.getUUID());

        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_PROVIDER);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_NAME);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_VERSION);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_THUMBNAIL_URL);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_STATUS);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_CONTEXT);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_VISIBILITY);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_VISIBLE_ROLES);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_VISIBLE_TENANTS);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_TRANSPORTS);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_INSEQUENCE);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_OUTSEQUENCE);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_FAULTSEQUENCE);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_DESCRIPTION);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_REDIRECT_URL);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_BUSS_OWNER);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_OWNER);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_ADVERTISE_ONLY);
        Mockito.verify(artifact, Mockito.atLeastOnce()).getAttribute(APIConstants.API_OVERVIEW_ENVIRONMENTS);
    }


    @Test
    public void testGetMediationSequenceUuidInSequence() throws Exception {
        APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry registry = Mockito.mock(UserRegistry.class);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(1))).thenReturn(registry);

        Collection collection = Mockito.mock(Collection.class);
        String path = APIConstants.API_CUSTOM_SEQUENCE_LOCATION + File.separator + APIConstants.API_CUSTOM_SEQUENCE_TYPE_IN;
        Mockito.when(registry.get(eq(path))).thenReturn(collection);

        String[] childPaths = {"test"};
        Mockito.when(collection.getChildren()).thenReturn(childPaths);

        String expectedUUID = UUID.randomUUID().toString();

        InputStream sampleSequence = new FileInputStream(Thread.currentThread().getContextClassLoader().
                getResource("sampleSequence.xml").getFile());

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(registry.get(eq("test"))).thenReturn(resource);
        Mockito.when(resource.getContentStream()).thenReturn(sampleSequence);
        Mockito.when(resource.getUUID()).thenReturn(expectedUUID);


        String actualUUID = APIUtil.getMediationSequenceUuid("sample", 1, "in", apiIdentifier);

        Assert.assertEquals(expectedUUID, actualUUID);
        sampleSequence.close();
    }

    @Test
    public void testGetMediationSequenceUuidOutSequence() throws Exception {
        APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry registry = Mockito.mock(UserRegistry.class);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(1))).thenReturn(registry);

        Collection collection = Mockito.mock(Collection.class);
        String path = APIConstants.API_CUSTOM_SEQUENCE_LOCATION + File.separator + APIConstants.API_CUSTOM_SEQUENCE_TYPE_OUT;
        Mockito.when(registry.get(eq(path))).thenReturn(collection);

        String[] childPaths = {"test"};
        Mockito.when(collection.getChildren()).thenReturn(childPaths);

        String expectedUUID = UUID.randomUUID().toString();

        InputStream sampleSequence = new FileInputStream(Thread.currentThread().getContextClassLoader().
                getResource("sampleSequence.xml").getFile());

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(registry.get(eq("test"))).thenReturn(resource);
        Mockito.when(resource.getContentStream()).thenReturn(sampleSequence);
        Mockito.when(resource.getUUID()).thenReturn(expectedUUID);


        String actualUUID = APIUtil.getMediationSequenceUuid("sample", 1, "out", apiIdentifier);

        Assert.assertEquals(expectedUUID, actualUUID);
        sampleSequence.close();
    }

    @Test
    public void testGetMediationSequenceUuidFaultSequence() throws Exception {
        APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry registry = Mockito.mock(UserRegistry.class);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(1))).thenReturn(registry);

        Collection collection = Mockito.mock(Collection.class);
        String path = APIConstants.API_CUSTOM_SEQUENCE_LOCATION + File.separator + APIConstants.API_CUSTOM_SEQUENCE_TYPE_FAULT;
        Mockito.when(registry.get(eq(path))).thenReturn(collection);

        String[] childPaths = {"test"};
        Mockito.when(collection.getChildren()).thenReturn(childPaths);

        String expectedUUID = UUID.randomUUID().toString();

        InputStream sampleSequence = new FileInputStream(Thread.currentThread().getContextClassLoader().
                getResource("sampleSequence.xml").getFile());

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(registry.get(eq("test"))).thenReturn(resource);
        Mockito.when(resource.getContentStream()).thenReturn(sampleSequence);
        Mockito.when(resource.getUUID()).thenReturn(expectedUUID);


        String actualUUID = APIUtil.getMediationSequenceUuid("sample", 1, "fault", apiIdentifier);

        Assert.assertEquals(expectedUUID, actualUUID);
        sampleSequence.close();
    }


    @Test
    public void testGetMediationSequenceUuidCustomSequence() throws Exception {
        APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry registry = Mockito.mock(UserRegistry.class);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(1))).thenReturn(registry);

        Collection collection = Mockito.mock(Collection.class);
        String artifactPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getApiName() + RegistryConstants.PATH_SEPARATOR + apiIdentifier.getVersion();
        String path = artifactPath + RegistryConstants.PATH_SEPARATOR + "custom" + RegistryConstants.PATH_SEPARATOR;

        Mockito.when(registry.get(eq(path))).thenReturn(collection);

        String[] childPaths = {"test"};
        Mockito.when(collection.getChildren()).thenReturn(childPaths);

        String expectedUUID = UUID.randomUUID().toString();

        InputStream sampleSequence = new FileInputStream(Thread.currentThread().getContextClassLoader().
                getResource("sampleSequence.xml").getFile());

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(registry.get(eq("test"))).thenReturn(resource);
        Mockito.when(resource.getContentStream()).thenReturn(sampleSequence);
        Mockito.when(resource.getUUID()).thenReturn(expectedUUID);


        String actualUUID = APIUtil.getMediationSequenceUuid("sample", 1, "custom", apiIdentifier);

        Assert.assertEquals(expectedUUID, actualUUID);
        sampleSequence.close();
    }


    @Test
    public void testGetMediationSequenceUuidCustomSequenceNotFound() throws Exception {
        APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry registry = Mockito.mock(UserRegistry.class);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(1))).thenReturn(registry);

        Collection collection = Mockito.mock(Collection.class);
        String artifactPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getApiName() + RegistryConstants.PATH_SEPARATOR + apiIdentifier.getVersion();
        String path = artifactPath + RegistryConstants.PATH_SEPARATOR + "custom" + RegistryConstants.PATH_SEPARATOR;

        Mockito.when(registry.get(eq(path))).thenReturn(null, collection);

        String[] childPaths = {"test"};
        Mockito.when(collection.getChildren()).thenReturn(childPaths);

        String expectedUUID = UUID.randomUUID().toString();

        InputStream sampleSequence = new FileInputStream(Thread.currentThread().getContextClassLoader().
                getResource("sampleSequence.xml").getFile());

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(registry.get(eq("test"))).thenReturn(resource);
        Mockito.when(resource.getContentStream()).thenReturn(sampleSequence);
        Mockito.when(resource.getUUID()).thenReturn(expectedUUID);


        String actualUUID = APIUtil.getMediationSequenceUuid("sample", 1, "custom", apiIdentifier);

        Assert.assertEquals(expectedUUID, actualUUID);
        sampleSequence.close();
    }

    @Test
    public void testIsPerAPISequence() throws Exception {
        APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry registry = Mockito.mock(UserRegistry.class);

        String artifactPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getApiName() + RegistryConstants.PATH_SEPARATOR + apiIdentifier.getVersion();
        String path = artifactPath + RegistryConstants.PATH_SEPARATOR + "in" + RegistryConstants.PATH_SEPARATOR;

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(1))).thenReturn(registry);
        Mockito.when(registry.resourceExists(eq(path))).thenReturn(true);

        Collection collection = Mockito.mock(Collection.class);
        Mockito.when(registry.get(eq(path))).thenReturn(collection);

        String[] childPaths = {"test"};
        Mockito.when(collection.getChildren()).thenReturn(childPaths);

        InputStream sampleSequence = new FileInputStream(Thread.currentThread().getContextClassLoader().
                        getResource("sampleSequence.xml").getFile());
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(registry.get(eq("test"))).thenReturn(resource);
        Mockito.when(resource.getContentStream()).thenReturn(sampleSequence);

        boolean isPerAPiSequence = APIUtil.isPerAPISequence("sample", 1, apiIdentifier, "in");

        Assert.assertTrue(isPerAPiSequence);
    }

    @Test
    public void testIsPerAPISequenceResourceMissing() throws Exception {
        APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry registry = Mockito.mock(UserRegistry.class);

        String artifactPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getApiName() + RegistryConstants.PATH_SEPARATOR + apiIdentifier.getVersion();
        String path = artifactPath + RegistryConstants.PATH_SEPARATOR + "in" + RegistryConstants.PATH_SEPARATOR;

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(1))).thenReturn(registry);
        Mockito.when(registry.resourceExists(eq(path))).thenReturn(false);

        boolean isPerAPiSequence = APIUtil.isPerAPISequence("sample", 1, apiIdentifier, "in");

        Assert.assertFalse(isPerAPiSequence);
    }

    @Test
    public void testIsPerAPISequenceSequenceMissing() throws Exception {
        APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry registry = Mockito.mock(UserRegistry.class);

        String artifactPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getApiName() + RegistryConstants.PATH_SEPARATOR + apiIdentifier.getVersion();
        String path = artifactPath + RegistryConstants.PATH_SEPARATOR + "in" + RegistryConstants.PATH_SEPARATOR;

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(1))).thenReturn(registry);
        Mockito.when(registry.resourceExists(eq(path))).thenReturn(true);
        Mockito.when(registry.get(eq(path))).thenReturn(null);

        boolean isPerAPiSequence = APIUtil.isPerAPISequence("sample", 1, apiIdentifier, "in");

        Assert.assertFalse(isPerAPiSequence);
    }

    @Test
    public void testIsPerAPISequenceNoPathsInCollection() throws Exception {
        APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry registry = Mockito.mock(UserRegistry.class);

        String artifactPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getApiName() + RegistryConstants.PATH_SEPARATOR + apiIdentifier.getVersion();
        String path = artifactPath + RegistryConstants.PATH_SEPARATOR + "in" + RegistryConstants.PATH_SEPARATOR;

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(1))).thenReturn(registry);
        Mockito.when(registry.resourceExists(eq(path))).thenReturn(false);

        Collection collection = Mockito.mock(Collection.class);
        Mockito.when(registry.get(eq(path))).thenReturn(collection);

        String[] childPaths = {};
        Mockito.when(collection.getChildren()).thenReturn(childPaths);

        boolean isPerAPiSequence = APIUtil.isPerAPISequence("sample", 1, apiIdentifier, "in");

        Assert.assertFalse(isPerAPiSequence);
    }


    @Test
    public void testGetCustomInSequence() throws Exception {
        APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry registry = Mockito.mock(UserRegistry.class);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(1))).thenReturn(registry);

        Collection collection = Mockito.mock(Collection.class);
        String artifactPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getApiName() + RegistryConstants.PATH_SEPARATOR + apiIdentifier.getVersion();
        String path = artifactPath + RegistryConstants.PATH_SEPARATOR + "in" + RegistryConstants.PATH_SEPARATOR;

        Mockito.when(registry.get(eq(path))).thenReturn(collection);

        String[] childPaths = {"test"};
        Mockito.when(collection.getChildren()).thenReturn(childPaths);

        InputStream sampleSequence = new FileInputStream(Thread.currentThread().getContextClassLoader().
                getResource("sampleSequence.xml").getFile());

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(registry.get(eq("test"))).thenReturn(resource);
        Mockito.when(resource.getContentStream()).thenReturn(sampleSequence);


        OMElement customSequence = APIUtil.getCustomSequence("sample", 1, "in", apiIdentifier);

        Assert.assertNotNull(customSequence);
        sampleSequence.close();
    }

    @Test
    public void testGetCustomOutSequence() throws Exception {
        APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry registry = Mockito.mock(UserRegistry.class);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(1))).thenReturn(registry);

        Collection collection = Mockito.mock(Collection.class);
        String artifactPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getApiName() + RegistryConstants.PATH_SEPARATOR + apiIdentifier.getVersion();
        String path = artifactPath + RegistryConstants.PATH_SEPARATOR + "out" + RegistryConstants.PATH_SEPARATOR;

        Mockito.when(registry.get(eq(path))).thenReturn(collection);

        String[] childPaths = {"test"};
        Mockito.when(collection.getChildren()).thenReturn(childPaths);

        InputStream sampleSequence = new FileInputStream(Thread.currentThread().getContextClassLoader().
                getResource("sampleSequence.xml").getFile());

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(registry.get(eq("test"))).thenReturn(resource);
        Mockito.when(resource.getContentStream()).thenReturn(sampleSequence);


        OMElement customSequence = APIUtil.getCustomSequence("sample", 1, "out", apiIdentifier);

        Assert.assertNotNull(customSequence);
        sampleSequence.close();
    }

    @Test
    public void testGetCustomFaultSequence() throws Exception {
        APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry registry = Mockito.mock(UserRegistry.class);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(1))).thenReturn(registry);

        Collection collection = Mockito.mock(Collection.class);
        String artifactPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getApiName() + RegistryConstants.PATH_SEPARATOR + apiIdentifier.getVersion();
        String path = artifactPath + RegistryConstants.PATH_SEPARATOR + "fault" + RegistryConstants.PATH_SEPARATOR;

        Mockito.when(registry.get(eq(path))).thenReturn(collection);

        String[] childPaths = {"test"};
        Mockito.when(collection.getChildren()).thenReturn(childPaths);

        InputStream sampleSequence = new FileInputStream(Thread.currentThread().getContextClassLoader().
                getResource("sampleSequence.xml").getFile());

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(registry.get(eq("test"))).thenReturn(resource);
        Mockito.when(resource.getContentStream()).thenReturn(sampleSequence);


        OMElement customSequence = APIUtil.getCustomSequence("sample", 1, "fault", apiIdentifier);

        Assert.assertNotNull(customSequence);
        sampleSequence.close();
    }

    @Test
    public void testGetCustomSequenceNotFound() throws Exception {
        APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry registry = Mockito.mock(UserRegistry.class);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(1))).thenReturn(registry);

        Collection collection = Mockito.mock(Collection.class);
        String artifactPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getApiName() + RegistryConstants.PATH_SEPARATOR + apiIdentifier.getVersion();
        String path = artifactPath + RegistryConstants.PATH_SEPARATOR + "custom" + RegistryConstants.PATH_SEPARATOR;

        Mockito.when(registry.get(eq(path))).thenReturn(null, collection);

        String[] childPaths = {"test"};
        Mockito.when(collection.getChildren()).thenReturn(childPaths);

        String expectedUUID = UUID.randomUUID().toString();

        InputStream sampleSequence = new FileInputStream(Thread.currentThread().getContextClassLoader().
                getResource("sampleSequence.xml").getFile());

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(registry.get(eq("test"))).thenReturn(resource);
        Mockito.when(resource.getContentStream()).thenReturn(sampleSequence);
        Mockito.when(resource.getUUID()).thenReturn(expectedUUID);


        OMElement customSequence = APIUtil.getCustomSequence("sample", 1, "custom", apiIdentifier);

        Assert.assertNotNull(customSequence);
        sampleSequence.close();
    }

    @Test
    public void testGetCustomSequenceNull() throws Exception {
        APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry registry = Mockito.mock(UserRegistry.class);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(1))).thenReturn(registry);

        Collection collection = Mockito.mock(Collection.class);
        String artifactPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                apiIdentifier.getApiName() + RegistryConstants.PATH_SEPARATOR + apiIdentifier.getVersion();
        String path = artifactPath + RegistryConstants.PATH_SEPARATOR + "custom" + RegistryConstants.PATH_SEPARATOR;

        Mockito.when(registry.get(eq(path))).thenReturn(null, null);

        String[] childPaths = {"test"};
        Mockito.when(collection.getChildren()).thenReturn(childPaths);

        String expectedUUID = UUID.randomUUID().toString();

        InputStream sampleSequence = new FileInputStream(Thread.currentThread().getContextClassLoader().
                getResource("sampleSequence.xml").getFile());

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(registry.get(eq("test"))).thenReturn(resource);
        Mockito.when(resource.getContentStream()).thenReturn(sampleSequence);
        Mockito.when(resource.getUUID()).thenReturn(expectedUUID);


        OMElement customSequence = APIUtil.getCustomSequence("sample", 1, "custom", apiIdentifier);

        Assert.assertNull(customSequence);
        sampleSequence.close();
    }

    @Test
    public void testCreateSwaggerJSONContent() throws Exception {
        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        APIManagerConfigurationService apiManagerConfigurationService = Mockito.mock(APIManagerConfigurationService.class);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Environment environment = Mockito.mock(Environment.class);
        Map<String, Environment> environmentMap = new HashMap<String, Environment>();
        environmentMap.put("Production", environment);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService()).thenReturn(apiManagerConfigurationService);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        Mockito.when(apiManagerConfiguration.getApiGatewayEnvironments()).thenReturn(environmentMap);
        Mockito.when(environment.getApiGatewayEndpoint()).thenReturn("");

        String swaggerJSONContent = APIUtil.createSwaggerJSONContent(getUniqueAPI());

        Assert.assertNotNull(swaggerJSONContent);
    }

    @Test
    public void testIsRoleNameExist() throws Exception {
        String userName = "John";
        String roleName = "developer";

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RealmService realmService = Mockito.mock(RealmService.class);
        TenantManager tenantManager = Mockito.mock(TenantManager.class);
        UserRealm userRealm = Mockito.mock(UserRealm.class);
        UserStoreManager userStoreManager = Mockito.mock(UserStoreManager.class);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
        Mockito.when(realmService.getTenantUserRealm(Mockito.anyInt())).thenReturn(userRealm);
        Mockito.when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        Mockito.when(userStoreManager.isExistingRole(roleName)).thenReturn(true);

        Assert.assertTrue(APIUtil.isRoleNameExist(userName, roleName));
    }

    @Test
    public void testIsRoleNameNotExist() throws Exception {
        String userName = "John";
        String roleName = "developer";

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RealmService realmService = Mockito.mock(RealmService.class);
        TenantManager tenantManager = Mockito.mock(TenantManager.class);
        UserRealm userRealm = Mockito.mock(UserRealm.class);
        UserStoreManager userStoreManager = Mockito.mock(UserStoreManager.class);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
        Mockito.when(realmService.getTenantUserRealm(Mockito.anyInt())).thenReturn(userRealm);
        Mockito.when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        Mockito.when(userStoreManager.isExistingRole(roleName)).thenReturn(false);

        Assert.assertFalse(APIUtil.isRoleNameExist(userName, roleName));
    }

    @Test
    public void testIsRoleNameExistDisableRoleValidation() throws Exception {
        String userName = "John";
        String roleName = "developer";

        System.setProperty(DISABLE_ROLE_VALIDATION_AT_SCOPE_CREATION, "true");

        Assert.assertTrue(APIUtil.isRoleNameExist(userName, roleName));

        Assert.assertTrue(APIUtil.isRoleNameExist(userName, null));

        Assert.assertTrue(APIUtil.isRoleNameExist(userName, ""));
    }

    @Test
    public void testGetRoleNamesSuperTenant() throws Exception {
        String userName = "John";

        String[] roleNames = {"role1", "role2"};

        AuthorizationManager authorizationManager = Mockito.mock(AuthorizationManager.class);

        PowerMockito.mockStatic(MultitenantUtils.class);
        PowerMockito.mockStatic(AuthorizationManager.class);
        Mockito.when(MultitenantUtils.getTenantDomain(userName)).
                thenReturn(org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        Mockito.when(AuthorizationManager.getInstance()).thenReturn(authorizationManager);
        Mockito.when(authorizationManager.getRoleNames()).thenReturn(roleNames);


        Assert.assertEquals(roleNames, APIUtil.getRoleNames(userName));
    }

    @Test
    public void testCreateAPIArtifactContent() throws Exception {
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(MultitenantConstants
                    .SUPER_TENANT_DOMAIN_NAME);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            GenericArtifact genericArtifact = Mockito.mock(GenericArtifact.class);
            API api = getUniqueAPI();
            Mockito.when(genericArtifact.getAttributeKeys()).thenReturn(new String[] {"URITemplate"}).thenThrow
                    (GovernanceException.class);

            APIUtil.createAPIArtifactContent(genericArtifact, api);
            Assert.assertTrue(true);
            APIUtil.createAPIArtifactContent(genericArtifact, api);
            Assert.fail();
        } catch (APIManagementException ex) {
            Assert.assertTrue(ex.getMessage().contains("Failed to create API for :"));
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

    }

    @Test
    public void testGetDocumentation() throws GovernanceException, APIManagementException {
        PowerMockito.mockStatic(CarbonUtils.class);
        ServerConfiguration serverConfiguration = Mockito.mock(ServerConfiguration.class);
        Mockito.when(serverConfiguration.getFirstProperty("WebContextRoot")).thenReturn("/abc").thenReturn("/");
        PowerMockito.when(CarbonUtils.getServerConfiguration()).thenReturn(serverConfiguration);
        GenericArtifact genericArtifact = Mockito.mock(GenericArtifact.class);
        Mockito.when(genericArtifact.getAttribute(APIConstants.DOC_TYPE)).thenReturn(DocumentationType.HOWTO.getType
                ()).thenReturn(DocumentationType.PUBLIC_FORUM.getType()).thenReturn(DocumentationType.SUPPORT_FORUM
                .getType()).thenReturn(DocumentationType.API_MESSAGE_FORMAT.getType()).thenReturn(DocumentationType
                .SAMPLES.getType()).thenReturn(DocumentationType.OTHER.getType());
        Mockito.when(genericArtifact.getAttribute(APIConstants.DOC_NAME)).thenReturn("Docname");
        Mockito.when(genericArtifact.getAttribute(APIConstants.DOC_VISIBILITY)).thenReturn(null).thenReturn
                (Documentation.DocumentVisibility.API_LEVEL.name()).thenReturn(Documentation.DocumentVisibility
                .PRIVATE.name()).thenReturn(Documentation.DocumentVisibility.OWNER_ONLY.name());
        Mockito.when(genericArtifact.getAttribute(APIConstants.DOC_SOURCE_TYPE)).thenReturn(Documentation
                .DocumentSourceType.URL.name()).thenReturn(Documentation.DocumentSourceType.FILE.name());
        Mockito.when(genericArtifact.getAttribute(APIConstants.DOC_SOURCE_URL)).thenReturn("https://localhost");
        Mockito.when(genericArtifact.getAttribute(APIConstants.DOC_FILE_PATH)).thenReturn("file://abc");
        Mockito.when(genericArtifact.getAttribute(APIConstants.DOC_OTHER_TYPE_NAME)).thenReturn("abc");
        APIUtil.getDocumentation(genericArtifact);
        APIUtil.getDocumentation(genericArtifact);
        APIUtil.getDocumentation(genericArtifact);
        APIUtil.getDocumentation(genericArtifact);
        APIUtil.getDocumentation(genericArtifact);
        APIUtil.getDocumentation(genericArtifact);

    }

    @Test
    public void testGetDocumentationByDocCreator() throws Exception {
        PowerMockito.mockStatic(CarbonUtils.class);
        ServerConfiguration serverConfiguration = Mockito.mock(ServerConfiguration.class);
        Mockito.when(serverConfiguration.getFirstProperty("WebContextRoot")).thenReturn("/abc").thenReturn("/");
        PowerMockito.when(CarbonUtils.getServerConfiguration()).thenReturn(serverConfiguration);
        GenericArtifact genericArtifact = Mockito.mock(GenericArtifact.class);
        Mockito.when(genericArtifact.getAttribute(APIConstants.DOC_TYPE)).thenReturn(DocumentationType.HOWTO.getType
                ()).thenReturn(DocumentationType.PUBLIC_FORUM.getType()).thenReturn(DocumentationType.SUPPORT_FORUM
                .getType()).thenReturn(DocumentationType.API_MESSAGE_FORMAT.getType()).thenReturn(DocumentationType
                .SAMPLES.getType()).thenReturn(DocumentationType.OTHER.getType());
        Mockito.when(genericArtifact.getAttribute(APIConstants.DOC_NAME)).thenReturn("Docname");
        Mockito.when(genericArtifact.getAttribute(APIConstants.DOC_VISIBILITY)).thenReturn(null).thenReturn
                (Documentation.DocumentVisibility.API_LEVEL.name()).thenReturn(Documentation.DocumentVisibility
                .PRIVATE.name()).thenReturn(Documentation.DocumentVisibility.OWNER_ONLY.name());
        Mockito.when(genericArtifact.getAttribute(APIConstants.DOC_SOURCE_TYPE)).thenReturn(Documentation
                .DocumentSourceType.URL.name()).thenReturn(Documentation.DocumentSourceType.FILE.name());
        Mockito.when(genericArtifact.getAttribute(APIConstants.DOC_SOURCE_URL)).thenReturn("https://localhost");
        Mockito.when(genericArtifact.getAttribute(APIConstants.DOC_FILE_PATH)).thenReturn("file://abc");
        Mockito.when(genericArtifact.getAttribute(APIConstants.DOC_OTHER_TYPE_NAME)).thenReturn("abc");
        APIUtil.getDocumentation(genericArtifact, "admin");
        APIUtil.getDocumentation(genericArtifact, "admin");
        APIUtil.getDocumentation(genericArtifact, "admin");
        APIUtil.getDocumentation(genericArtifact, "admin");
        APIUtil.getDocumentation(genericArtifact, "admin");
        APIUtil.getDocumentation(genericArtifact, "admin");
        APIUtil.getDocumentation(genericArtifact, "admin@wso2.com");
    }

    @Test
    public void testCreateDocArtifactContent() throws GovernanceException, APIManagementException {
        API api = getUniqueAPI();
        PowerMockito.mockStatic(CarbonUtils.class);
        ServerConfiguration serverConfiguration = Mockito.mock(ServerConfiguration.class);
        Mockito.when(serverConfiguration.getFirstProperty("WebContextRoot")).thenReturn("/abc").thenReturn("/");
        PowerMockito.when(CarbonUtils.getServerConfiguration()).thenReturn(serverConfiguration);
        GenericArtifact genericArtifact = Mockito.mock(GenericArtifact.class);
        Documentation documentation = new Documentation(DocumentationType.HOWTO, "this is a doc");
        documentation.setSourceType(Documentation.DocumentSourceType.FILE);
        documentation.setCreatedDate(new Date(System.currentTimeMillis()));
        documentation.setSummary("abcde");
        documentation.setVisibility(Documentation.DocumentVisibility.API_LEVEL);
        documentation.setSourceUrl("/abcd/def");
        documentation.setOtherTypeName("aa");
        APIUtil.createDocArtifactContent(genericArtifact, api.getId(), documentation);
        documentation.setSourceType(Documentation.DocumentSourceType.INLINE);
        APIUtil.createDocArtifactContent(genericArtifact, api.getId(), documentation);
        documentation.setSourceType(Documentation.DocumentSourceType.URL);
        APIUtil.createDocArtifactContent(genericArtifact, api.getId(), documentation);

        try {
            documentation.setSourceType(Documentation.DocumentSourceType.URL);
            Mockito.doThrow(GovernanceException.class).when(genericArtifact).setAttribute(APIConstants
                    .DOC_SOURCE_URL, documentation.getSourceUrl());
            APIUtil.createDocArtifactContent(genericArtifact, api.getId(), documentation);
            Assert.fail();
        } catch (APIManagementException ex) {
            Assert.assertTrue(ex.getMessage().contains("Failed to create doc artifact content from :"));
        }
    }

    @Test
    public void testGetArtifactManager()  {
        PowerMockito.mockStatic(GenericArtifactManager.class);
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        Registry registry = Mockito.mock(UserRegistry.class);
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(MultitenantConstants
                    .SUPER_TENANT_DOMAIN_NAME);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            PowerMockito.mockStatic(GovernanceUtils.class);
            PowerMockito.doNothing().when(GovernanceUtils.class, "loadGovernanceArtifacts",(UserRegistry)registry);
            Mockito.when(GovernanceUtils.findGovernanceArtifactConfiguration(APIConstants.API_KEY, registry))
                    .thenReturn(Mockito.mock(GovernanceArtifactConfiguration.class)).thenReturn(null).thenThrow
                    (RegistryException.class);
            GenericArtifactManager genericArtifactManager = Mockito.mock(GenericArtifactManager.class);
            PowerMockito.whenNew(GenericArtifactManager.class).withArguments(registry, APIConstants.API_KEY)
                    .thenReturn(genericArtifactManager);
            GenericArtifactManager retrievedGenericArtifactManager = APIUtil.getArtifactManager(registry,
                    APIConstants.API_KEY);
            Assert.assertEquals(genericArtifactManager, retrievedGenericArtifactManager);
            APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
            APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
            Assert.fail();
        } catch (APIManagementException ex) {
            Assert.assertTrue(ex.getMessage().contains("Failed to initialize GenericArtifactManager"));
        } catch (org.wso2.carbon.registry.core.exceptions.RegistryException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

    }

    @Test
    public void testGetgetKeyManagementClient() throws Exception {
        PowerMockito.mockStatic(KeyManagerHolder.class);
        KeyManagerConfiguration keyManagerConfiguration = Mockito.mock(KeyManagerConfiguration.class);
        KeyManager keyManagr = Mockito.mock(KeyManager.class);
        PowerMockito.when(KeyManagerHolder.getKeyManagerInstance()).thenReturn(keyManagr);
        Mockito.when(keyManagr.getKeyManagerConfiguration()).thenReturn(keyManagerConfiguration);
        Mockito.when(keyManagerConfiguration.getParameter(APIConstants.AUTHSERVER_URL)).thenReturn
                ("https://localhost").thenReturn(null).thenReturn("https://localhost").thenReturn("https://localhost");
        Mockito.when(keyManagerConfiguration.getParameter(APIConstants.KEY_MANAGER_USERNAME)).thenReturn("admin")
                .thenReturn(null).thenReturn("admin").thenReturn(null).thenReturn("admin");
        Mockito.when(keyManagerConfiguration.getParameter(APIConstants.KEY_MANAGER_PASSWORD)).thenReturn("admin")
                .thenReturn("admin").thenReturn(null).thenReturn(null).thenReturn("admin");
        PowerMockito.mockStatic(SubscriberKeyMgtClient.class);
        SubscriberKeyMgtClient subscriberKeyMgtClient = Mockito.mock(SubscriberKeyMgtClient.class);
        PowerMockito.whenNew(SubscriberKeyMgtClient.class).withArguments(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString()).thenReturn(subscriberKeyMgtClient).thenThrow(Exception.class);

        APIUtil.getKeyManagementClient();
        try{
            APIUtil.getKeyManagementClient();
            Assert.fail();
        }catch (APIManagementException ex){
            Assert.assertTrue(ex.getMessage().contains("API key manager URL unspecified"));
        }
        try{
            APIUtil.getKeyManagementClient();
            Assert.fail();
        }catch (APIManagementException ex){
            Assert.assertTrue(ex.getMessage().contains("Authentication credentials for API key manager unspecified"));
        }
        try{
            APIUtil.getKeyManagementClient();
            Assert.fail();
        }catch (APIManagementException ex){
            Assert.assertTrue(ex.getMessage().contains("Authentication credentials for API key manager unspecified"));
        }
        try{
            APIUtil.getKeyManagementClient();
            Assert.fail();
        }catch (APIManagementException ex){
            Assert.assertTrue(ex.getMessage().contains("Error while initializing the subscriber key management client"));
        }

    }
    @Test
    public void testGetApplicationManagementServiceClient() throws Exception {
        PowerMockito.mockStatic(ApplicationManagementServiceClient.class);
        ApplicationManagementServiceClient applicationManagementServiceClient = Mockito.mock
                (ApplicationManagementServiceClient.class);
        PowerMockito.whenNew(ApplicationManagementServiceClient.class).withNoArguments().thenReturn
                (applicationManagementServiceClient).thenThrow(Exception.class);
        APIUtil.getApplicationManagementServiceClient();
        Assert.assertTrue(true);
        try{
            APIUtil.getApplicationManagementServiceClient();
            Assert.fail();
        }catch (APIManagementException ex){
            Assert.assertTrue(ex.getMessage().contains("Error while initializing the Application Management Service " +
                    "client"));
        }
    }
    @Test
    public void testGetOAuthAdminClient() throws Exception {
        PowerMockito.mockStatic(OAuthAdminClient.class);
        OAuthAdminClient oAuthAdminClient = Mockito.mock(OAuthAdminClient.class);
        PowerMockito.whenNew(OAuthAdminClient.class).withNoArguments().thenReturn(oAuthAdminClient).thenThrow
                (Exception.class);
        APIUtil.getOauthAdminClient();
        Assert.assertTrue(true);
        try{
            APIUtil.getOauthAdminClient();
            Assert.fail();
        }catch (APIManagementException ex){
            Assert.assertTrue(ex.getMessage().contains("Error while initializing the OAuth admin client"));
        }
    }

    @Test
    public void testGetRoleNamesNonSuperTenant() throws Exception {
        String userName = "John";

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RealmService realmService = Mockito.mock(RealmService.class);
        TenantManager tenantManager = Mockito.mock(TenantManager.class);
        UserRealm userRealm = Mockito.mock(UserRealm.class);
        UserStoreManager userStoreManager = Mockito.mock(UserStoreManager.class);

        String[] roleNames = {"role1", "role2"};

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.mockStatic(MultitenantUtils.class);
        Mockito.when(MultitenantUtils.getTenantDomain(userName)).
                thenReturn("test.com");
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
        Mockito.when(realmService.getTenantUserRealm(Mockito.anyInt())).thenReturn(userRealm);
        Mockito.when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        Mockito.when(userStoreManager.getRoleNames()).thenReturn(roleNames);

        Assert.assertEquals(roleNames, APIUtil.getRoleNames(userName));
    }


    @Test
    public void testGetAPI() throws Exception {
        API expectedAPI = getUniqueAPI();

        final String provider = expectedAPI.getId().getProviderName();
        final String tenantDomain = org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        final int tenantId = -1234;

        GovernanceArtifact artifact = Mockito.mock(GovernanceArtifact.class);
        Registry registry = Mockito.mock(Registry.class);
        ApiMgtDAO apiMgtDAO = Mockito.mock(ApiMgtDAO.class);
        Resource resource = Mockito.mock(Resource.class);
        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RealmService realmService = Mockito.mock(RealmService.class);
        TenantManager tenantManager = Mockito.mock(TenantManager.class);
        APIManagerConfigurationService apiManagerConfigurationService = Mockito.mock(APIManagerConfigurationService.class);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        ThrottleProperties throttleProperties = Mockito.mock(ThrottleProperties.class);
        SubscriptionPolicy policy = Mockito.mock(SubscriptionPolicy.class);
        SubscriptionPolicy[] policies = new SubscriptionPolicy[]{policy};
        QuotaPolicy quotaPolicy = Mockito.mock(QuotaPolicy.class);
        RequestCountLimit limit = Mockito.mock(RequestCountLimit.class);

        PowerMockito.mockStatic(ApiMgtDAO.class);
        PowerMockito.mockStatic(MultitenantUtils.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ApiMgtDAO.getInstance()).thenReturn(apiMgtDAO);
        Mockito.when(apiMgtDAO.getAPIID(Mockito.any(APIIdentifier.class), eq((Connection) null))).thenReturn(123);
        Mockito.when(artifact.getId()).thenReturn("");
        Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER)).thenReturn(provider);
        Mockito.when(MultitenantUtils.getTenantDomain(provider)).
                thenReturn(tenantDomain);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
        Mockito.when(tenantManager.getTenantId(tenantDomain)).thenReturn(tenantId);

        String artifactPath = "";
        PowerMockito.mockStatic(GovernanceUtils.class);
        Mockito.when(GovernanceUtils.getArtifactPath(registry, "")).thenReturn(artifactPath);
        Mockito.when(registry.get(artifactPath)).thenReturn(resource);
        Mockito.when(resource.getLastModified()).thenReturn(expectedAPI.getLastUpdated());
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService()).thenReturn(apiManagerConfigurationService);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        Mockito.when(apiManagerConfiguration.getThrottleProperties()).thenReturn(throttleProperties);
        Mockito.when(throttleProperties.isEnabled()).thenReturn(true);
        Mockito.when(apiMgtDAO.getSubscriptionPolicies(tenantId)).thenReturn(policies);
        Mockito.when(policy.getDefaultQuotaPolicy()).thenReturn(quotaPolicy);
        Mockito.when(quotaPolicy.getLimit()).thenReturn(limit);
        Mockito.when(registry.getTags(artifactPath)).thenReturn(getTagsFromSet(expectedAPI.getTags()));

        HashMap<String, String> urlPatterns = getURLTemplatePattern(expectedAPI.getUriTemplates());
        Mockito.when(apiMgtDAO.getURITemplatesPerAPIAsString(Mockito.any(APIIdentifier.class))).thenReturn(urlPatterns);

        CORSConfiguration corsConfiguration = expectedAPI.getCorsConfiguration();

        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_HEADERS)).
                thenReturn(corsConfiguration.getAccessControlAllowHeaders().toString());
        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_METHODS)).
                thenReturn(corsConfiguration.getAccessControlAllowMethods().toString());
        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_ORIGIN)).
                thenReturn(corsConfiguration.getAccessControlAllowOrigins().toString());

        API api = APIUtil.getAPI(artifact, registry);

        Assert.assertNotNull(api);
    }

    @Test
    public void testGetAPIForPublishing() throws Exception {
        API expectedAPI = getUniqueAPI();

        final String provider = expectedAPI.getId().getProviderName();
        final String tenantDomain = org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        final int tenantId = -1234;

        GovernanceArtifact artifact = Mockito.mock(GovernanceArtifact.class);
        Registry registry = Mockito.mock(Registry.class);
        ApiMgtDAO apiMgtDAO = Mockito.mock(ApiMgtDAO.class);
        Resource resource = Mockito.mock(Resource.class);
        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RealmService realmService = Mockito.mock(RealmService.class);
        TenantManager tenantManager = Mockito.mock(TenantManager.class);
        APIManagerConfigurationService apiManagerConfigurationService = Mockito.mock(APIManagerConfigurationService.class);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        ThrottleProperties throttleProperties = Mockito.mock(ThrottleProperties.class);
        SubscriptionPolicy policy = Mockito.mock(SubscriptionPolicy.class);
        SubscriptionPolicy[] policies = new SubscriptionPolicy[]{policy};
        QuotaPolicy quotaPolicy = Mockito.mock(QuotaPolicy.class);
        RequestCountLimit limit = Mockito.mock(RequestCountLimit.class);

        PowerMockito.mockStatic(ApiMgtDAO.class);
        PowerMockito.mockStatic(GovernanceUtils.class);
        PowerMockito.mockStatic(MultitenantUtils.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);

        Mockito.when(ApiMgtDAO.getInstance()).thenReturn(apiMgtDAO);
        Mockito.when(apiMgtDAO.getAPIID(Mockito.any(APIIdentifier.class), eq((Connection) null))).thenReturn(123);
        Mockito.when(artifact.getId()).thenReturn("");
        Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER)).thenReturn(provider);
        Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_CACHE_TIMEOUT)).thenReturn("15");
        Mockito.when(MultitenantUtils.getTenantDomain(provider)).thenReturn(tenantDomain);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
        Mockito.when(tenantManager.getTenantId(tenantDomain)).thenReturn(tenantId);

        String artifactPath = "";
        Mockito.when(GovernanceUtils.getArtifactPath(registry, "")).thenReturn(artifactPath);
        Mockito.when(registry.get(artifactPath)).thenReturn(resource);
        Mockito.when(resource.getLastModified()).thenReturn(expectedAPI.getLastUpdated());
        Mockito.when(resource.getCreatedTime()).thenReturn(expectedAPI.getLastUpdated());
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService()).thenReturn(apiManagerConfigurationService);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        Mockito.when(apiManagerConfiguration.getThrottleProperties()).thenReturn(throttleProperties);
        Mockito.when(throttleProperties.isEnabled()).thenReturn(true);
        Mockito.when(apiMgtDAO.getSubscriptionPolicies(tenantId)).thenReturn(policies);
        Mockito.when(policy.getDefaultQuotaPolicy()).thenReturn(quotaPolicy);
        Mockito.when(quotaPolicy.getLimit()).thenReturn(limit);
        Mockito.when(registry.getTags(artifactPath)).thenReturn(getTagsFromSet(expectedAPI.getTags()));

        HashMap<String, String> urlPatterns = getURLTemplatePattern(expectedAPI.getUriTemplates());
        Mockito.when(apiMgtDAO.getURITemplatesPerAPIAsString(Mockito.any(APIIdentifier.class))).thenReturn(urlPatterns);

        CORSConfiguration corsConfiguration = expectedAPI.getCorsConfiguration();

        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_HEADERS)).
                thenReturn(corsConfiguration.getAccessControlAllowHeaders().toString());
        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_METHODS)).
                thenReturn(corsConfiguration.getAccessControlAllowMethods().toString());
        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_ORIGIN)).
                thenReturn(corsConfiguration.getAccessControlAllowOrigins().toString());

        API api = APIUtil.getAPIForPublishing(artifact, registry);

        Assert.assertNotNull(api);
    }

    @Test
    public void testGetAPIWithGovernanceArtifact() throws Exception {
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(MultitenantConstants
                    .SUPER_TENANT_DOMAIN_NAME);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            API expectedAPI = getUniqueAPI();

            final String provider = expectedAPI.getId().getProviderName();
            final String tenantDomain = org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;

            final int tenantId = -1234;

            System.setProperty("carbon.home", "");

            File siteConfFile = new File(Thread.currentThread().getContextClassLoader().
                    getResource("tenant-conf.json").getFile());

            String tenantConfValue = FileUtils.readFileToString(siteConfFile);

            GovernanceArtifact artifact = Mockito.mock(GovernanceArtifact.class);
            Registry registry = Mockito.mock(Registry.class);
            ApiMgtDAO apiMgtDAO = Mockito.mock(ApiMgtDAO.class);
            Resource resource = Mockito.mock(Resource.class);
            ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
            RealmService realmService = Mockito.mock(RealmService.class);
            TenantManager tenantManager = Mockito.mock(TenantManager.class);
            APIManagerConfigurationService apiManagerConfigurationService = Mockito.mock(APIManagerConfigurationService.class);
            APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
            ThrottleProperties throttleProperties = Mockito.mock(ThrottleProperties.class);
            SubscriptionPolicy policy = Mockito.mock(SubscriptionPolicy.class);
            SubscriptionPolicy[] policies = new SubscriptionPolicy[] {policy};
            QuotaPolicy quotaPolicy = Mockito.mock(QuotaPolicy.class);
            RequestCountLimit limit = Mockito.mock(RequestCountLimit.class);
            PrivilegedCarbonContext carbonContext = Mockito.mock(PrivilegedCarbonContext.class);
            RegistryService registryService = Mockito.mock(RegistryService.class);
            UserRegistry userRegistry = Mockito.mock(UserRegistry.class);

            PowerMockito.mockStatic(ApiMgtDAO.class);
            PowerMockito.mockStatic(GovernanceUtils.class);
            PowerMockito.mockStatic(MultitenantUtils.class);
            PowerMockito.mockStatic(ServiceReferenceHolder.class);

            Mockito.when(ApiMgtDAO.getInstance()).thenReturn(apiMgtDAO);
            Mockito.when(apiMgtDAO.getAPIID(Mockito.any(APIIdentifier.class), eq((Connection) null))).thenReturn(123);
            Mockito.when(apiMgtDAO.getPolicyNames(PolicyConstants.POLICY_LEVEL_SUB, provider)).thenReturn(new String[] {"Unlimited"});
            Mockito.when(artifact.getId()).thenReturn("");
            Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER)).thenReturn(provider);
            Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_CACHE_TIMEOUT)).thenReturn("15");
            Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_TIER)).thenReturn("Unlimited");
            Mockito.when(MultitenantUtils.getTenantDomain(provider)).thenReturn(tenantDomain);
            Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
            Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
            Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
            Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
            Mockito.when(tenantManager.getTenantId(tenantDomain)).thenReturn(tenantId);
            Mockito.when(registryService.getConfigSystemRegistry(tenantId)).thenReturn(userRegistry);
            Mockito.when(userRegistry.resourceExists(APIConstants.API_TENANT_CONF_LOCATION)).thenReturn(true);
            Mockito.when(userRegistry.get(APIConstants.API_TENANT_CONF_LOCATION)).thenReturn(resource);

            String artifactPath = "";
            Mockito.when(GovernanceUtils.getArtifactPath(registry, "")).thenReturn(artifactPath);
            Mockito.when(registry.get(artifactPath)).thenReturn(resource);
            Mockito.when(resource.getLastModified()).thenReturn(expectedAPI.getLastUpdated());
            Mockito.when(resource.getCreatedTime()).thenReturn(expectedAPI.getLastUpdated());
            Mockito.when(resource.getContent()).thenReturn(tenantConfValue.getBytes());
            Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService()).thenReturn(apiManagerConfigurationService);
            Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
            Mockito.when(apiManagerConfiguration.getThrottleProperties()).thenReturn(throttleProperties);
            Mockito.when(throttleProperties.isEnabled()).thenReturn(true);
            Mockito.when(apiMgtDAO.getSubscriptionPolicies(tenantId)).thenReturn(policies);
            Mockito.when(policy.getDefaultQuotaPolicy()).thenReturn(quotaPolicy);
            Mockito.when(quotaPolicy.getLimit()).thenReturn(limit);
            Mockito.when(registry.getTags(artifactPath)).thenReturn(getTagsFromSet(expectedAPI.getTags()));

            ArrayList<URITemplate> urlList = getURLTemplateList(expectedAPI.getUriTemplates());
            Mockito.when(apiMgtDAO.getAllURITemplates(Mockito.anyString(), Mockito.anyString())).thenReturn(urlList);

            CORSConfiguration corsConfiguration = expectedAPI.getCorsConfiguration();

            Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_HEADERS)).
                    thenReturn(corsConfiguration.getAccessControlAllowHeaders().toString());
            Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_METHODS)).
                    thenReturn(corsConfiguration.getAccessControlAllowMethods().toString());
            Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_ORIGIN)).
                    thenReturn(corsConfiguration.getAccessControlAllowOrigins().toString());


            API api = APIUtil.getAPI(artifact);

            Assert.assertNotNull(api);
        }finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    @Test
    public void testGetAPIWithGovernanceArtifactAdvancedThrottlingDisabled() throws Exception {
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(MultitenantConstants
                    .SUPER_TENANT_DOMAIN_NAME);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        API expectedAPI = getUniqueAPI();

        final String provider = expectedAPI.getId().getProviderName();
        final String tenantDomain = org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        final int tenantId = -1234;

        System.setProperty("carbon.home", "");

        File siteConfFile = new File(Thread.currentThread().getContextClassLoader().
                getResource("tenant-conf.json").getFile());

        String tenantConfValue = FileUtils.readFileToString(siteConfFile);

        GovernanceArtifact artifact = Mockito.mock(GovernanceArtifact.class);
        Registry registry = Mockito.mock(Registry.class);
        ApiMgtDAO apiMgtDAO = Mockito.mock(ApiMgtDAO.class);
        Resource resource = Mockito.mock(Resource.class);
        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RealmService realmService = Mockito.mock(RealmService.class);
        TenantManager tenantManager = Mockito.mock(TenantManager.class);
        APIManagerConfigurationService apiManagerConfigurationService = Mockito.mock(APIManagerConfigurationService.class);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        ThrottleProperties throttleProperties = Mockito.mock(ThrottleProperties.class);
        SubscriptionPolicy policy = Mockito.mock(SubscriptionPolicy.class);
        SubscriptionPolicy[] policies = new SubscriptionPolicy[]{policy};
        QuotaPolicy quotaPolicy = Mockito.mock(QuotaPolicy.class);
        RequestCountLimit limit = Mockito.mock(RequestCountLimit.class);
            RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry userRegistry = Mockito.mock(UserRegistry.class);

        PowerMockito.mockStatic(ApiMgtDAO.class);
        PowerMockito.mockStatic(GovernanceUtils.class);
        PowerMockito.mockStatic(MultitenantUtils.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ApiMgtDAO.getInstance()).thenReturn(apiMgtDAO);
        Mockito.when(apiMgtDAO.getAPIID(Mockito.any(APIIdentifier.class), eq((Connection) null))).thenReturn(123);
        Mockito.when(apiMgtDAO.getPolicyNames(PolicyConstants.POLICY_LEVEL_SUB, provider)).thenReturn(new String[]{"Unlimited"});
        Mockito.when(artifact.getId()).thenReturn("");
        Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER)).thenReturn(provider);
        Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_CACHE_TIMEOUT)).thenReturn("15");
        Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_TIER)).thenReturn("Unlimited");
        Mockito.when(MultitenantUtils.getTenantDomain(provider)).thenReturn(tenantDomain);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
        Mockito.when(tenantManager.getTenantId(tenantDomain)).thenReturn(tenantId);
        Mockito.when(registryService.getConfigSystemRegistry(tenantId)).thenReturn(userRegistry);
        Mockito.when(userRegistry.resourceExists(APIConstants.API_TENANT_CONF_LOCATION)).thenReturn(true);
        Mockito.when(userRegistry.get(APIConstants.API_TENANT_CONF_LOCATION)).thenReturn(resource);

        String artifactPath = "";
        Mockito.when(GovernanceUtils.getArtifactPath(registry, "")).thenReturn(artifactPath);
        Mockito.when(registry.get(artifactPath)).thenReturn(resource);
        Mockito.when(resource.getLastModified()).thenReturn(expectedAPI.getLastUpdated());
        Mockito.when(resource.getCreatedTime()).thenReturn(expectedAPI.getLastUpdated());
        Mockito.when(resource.getContent()).thenReturn(tenantConfValue.getBytes());
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService()).thenReturn(apiManagerConfigurationService);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        Mockito.when(apiManagerConfiguration.getThrottleProperties()).thenReturn(throttleProperties);
        Mockito.when(throttleProperties.isEnabled()).thenReturn(false);
        Mockito.when(apiMgtDAO.getSubscriptionPolicies(tenantId)).thenReturn(policies);
        Mockito.when(policy.getDefaultQuotaPolicy()).thenReturn(quotaPolicy);
        Mockito.when(quotaPolicy.getLimit()).thenReturn(limit);
        Mockito.when(registry.getTags(artifactPath)).thenReturn(getTagsFromSet(expectedAPI.getTags()));

        ArrayList<URITemplate> urlList = getURLTemplateList(expectedAPI.getUriTemplates());
        Mockito.when(apiMgtDAO.getAllURITemplates(Mockito.anyString(), Mockito.anyString())).thenReturn(urlList);

        CORSConfiguration corsConfiguration = expectedAPI.getCorsConfiguration();

        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_HEADERS)).
                thenReturn(corsConfiguration.getAccessControlAllowHeaders().toString());
        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_METHODS)).
                thenReturn(corsConfiguration.getAccessControlAllowMethods().toString());
        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_ORIGIN)).
                thenReturn(corsConfiguration.getAccessControlAllowOrigins().toString());

        API api = APIUtil.getAPI(artifact);

        Assert.assertNotNull(api);
    }finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    private API getUniqueAPI() {
        APIIdentifier apiIdentifier = new APIIdentifier(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
        API api = new API(apiIdentifier);
        api.setStatus(APIStatus.CREATED);
        api.setContext(UUID.randomUUID().toString());

        Set<String> environments = new HashSet<String>();
        environments.add(UUID.randomUUID().toString());

        URITemplate uriTemplate = new URITemplate();
        uriTemplate.setAuthType("None");
        uriTemplate.setHTTPVerb("GET");
        uriTemplate.setThrottlingTier("Unlimited");
        uriTemplate.setUriTemplate("/*");
        Set<URITemplate> uriTemplates = new HashSet<URITemplate>();
        uriTemplates.add(uriTemplate);

        uriTemplate = new URITemplate();
        uriTemplate.setAuthType("None");
        uriTemplate.setHTTPVerb("GET");
        uriTemplate.setThrottlingTier("Unlimited");
        uriTemplate.setUriTemplate("/get");
        uriTemplates.add(uriTemplate);

        uriTemplate = new URITemplate();
        uriTemplate.setAuthType("None");
        uriTemplate.setHTTPVerb("POST");
        uriTemplate.setThrottlingTier("Unlimited");
        uriTemplate.setUriTemplate("/*");
        uriTemplates.add(uriTemplate);

        uriTemplate = new URITemplate();
        uriTemplate.setAuthType("None");
        uriTemplate.setHTTPVerb("POST");
        uriTemplate.setThrottlingTier("Unlimited");
        uriTemplate.setUriTemplate("/post");
        uriTemplates.add(uriTemplate);

        uriTemplate = new URITemplate();
        uriTemplate.setAuthType("None");
        uriTemplate.setHTTPVerb("DELETE");
        uriTemplate.setThrottlingTier("Unlimited");
        uriTemplate.setUriTemplate("/*");
        uriTemplates.add(uriTemplate);

        uriTemplate = new URITemplate();
        uriTemplate.setAuthType("None");
        uriTemplate.setHTTPVerb("PUT");
        uriTemplate.setThrottlingTier("Unlimited");
        uriTemplate.setUriTemplate("/*");
        uriTemplates.add(uriTemplate);

        uriTemplate = new URITemplate();
        uriTemplate.setAuthType("None");
        uriTemplate.setHTTPVerb("PUT");
        uriTemplate.setThrottlingTier("Unlimited");
        uriTemplate.setUriTemplate("/put");
        uriTemplates.add(uriTemplate);

        api.setUriTemplates(uriTemplates);

        api.setEnvironments(environments);
        api.setUUID(UUID.randomUUID().toString());
        api.setThumbnailUrl(UUID.randomUUID().toString());
        api.setVisibility(UUID.randomUUID().toString());
        api.setVisibleRoles(UUID.randomUUID().toString());
        api.setVisibleTenants(UUID.randomUUID().toString());
        api.setTransports(UUID.randomUUID().toString());
        api.setInSequence(UUID.randomUUID().toString());
        api.setOutSequence(UUID.randomUUID().toString());
        api.setFaultSequence(UUID.randomUUID().toString());
        api.setDescription(UUID.randomUUID().toString());
        api.setRedirectURL(UUID.randomUUID().toString());
        api.setBusinessOwner(UUID.randomUUID().toString());
        api.setApiOwner(UUID.randomUUID().toString());
        api.setAdvertiseOnly(true);

        CORSConfiguration corsConfiguration = new CORSConfiguration(true, Arrays.asList("*"),
                true, Arrays.asList("*"), Arrays.asList("*"));

        api.setCorsConfiguration(corsConfiguration);
        api.setLastUpdated(new Date());
        api.setCreatedTime(new Date().toString());

        Set<Tier> tierSet = new HashSet<Tier>();
        tierSet.add(new Tier("Unlimited"));
        tierSet.add(new Tier("Gold"));
        api.addAvailableTiers(tierSet);
        Set<String> tags = new HashSet<String>();
        tags.add("stuff");
        api.addTags(tags);

        return api;
    }

    private Tag[] getTagsFromSet(Set<String> tagSet) {
        String[] tagNames = tagSet.toArray(new String[tagSet.size()]);

        Tag[] tags = new Tag[tagNames.length];

        for (int i = 0; i < tagNames.length; i++) {
            Tag tag = new Tag();
            tag.setTagName(tagNames[i]);
            tags[i] = tag;
        }

        return tags;
    }

    private HashMap<String, String> getURLTemplatePattern(Set<URITemplate> uriTemplates) {
        HashMap<String, String> pattern = new HashMap<String, String>();

        for (URITemplate uriTemplate : uriTemplates) {
            String key = uriTemplate.getUriTemplate() + "::" + uriTemplate.getHTTPVerb() + "::" +
                    uriTemplate.getAuthType() + "::" + uriTemplate.getThrottlingTier();
            pattern.put(key, uriTemplate.getHTTPVerb());
        }

        return pattern;
    }

    private ArrayList<URITemplate> getURLTemplateList(Set<URITemplate> uriTemplates) {
        ArrayList<URITemplate> list = new ArrayList<URITemplate>();
        list.addAll(uriTemplates);

        return list;

    }

    private Resource getMockedResource() throws Exception {
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        UserRegistry registry = Mockito.mock(UserRegistry.class);
        Mockito.when(registryService.getGovernanceSystemRegistry(5443)).thenReturn(registry);
        Mockito.when(registry.resourceExists(APIConstants.API_TIER_LOCATION)).thenReturn(true);

        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resource.getContent()).thenReturn("wsdl".getBytes());
        Mockito.when(registry.get(APIConstants.API_TIER_LOCATION)).thenReturn(resource);
        return resource;

    }
    @Test(expected = APIManagementException.class)
    public void testDeleteTiersTierWhenTierNotExists() throws Exception {
        Resource resource = getMockedResource();
        PowerMockito.mockStatic(AXIOMUtil.class);
        String content = new String((byte[]) resource.getContent(), Charset.defaultCharset());
        OMElement element = Mockito.mock(OMElement.class);
        PowerMockito.when(AXIOMUtil.stringToOM(content)).thenReturn(element);
        Mockito.when(element.getFirstChildWithName(APIConstants.ASSERTION_ELEMENT)).thenReturn(element);

        List<OMElement> list = new ArrayList<OMElement>();
        list.add(Mockito.mock(OMElement.class));
        Mockito.when(element.getChildrenWithName(APIConstants.POLICY_ELEMENT)).thenReturn(list.iterator());
        Tier tier = Mockito.mock(Tier.class);
        APIUtil.deleteTier(tier, 5443);
        Mockito.verify(resource, Mockito.times(0)).setContent(Matchers.anyString());
    }

    @Test
    public void testDeleteTiersTierWhenTierExists() throws Exception {
        Resource resource = getMockedResource();
        Tier tier = Mockito.mock(Tier.class);
        Mockito.when(tier.getName()).thenReturn("GOLD");

        PowerMockito.mockStatic(AXIOMUtil.class);
        String content = new String((byte[]) resource.getContent(), Charset.defaultCharset());
        OMElement element = Mockito.mock(OMElement.class);
        PowerMockito.when(AXIOMUtil.stringToOM(content)).thenReturn(element);
        Mockito.when(element.getFirstChildWithName(APIConstants.ASSERTION_ELEMENT)).thenReturn(element);

        List<OMElement> list = new ArrayList<OMElement>();
        OMElement omElement1 = Mockito.mock(OMElement.class);
        list.add(omElement1);
        OMElement omElement2 = Mockito.mock(OMElement.class);
        Mockito.when(omElement1.getFirstChildWithName(APIConstants.THROTTLE_ID_ELEMENT)).thenReturn(omElement2);
        Mockito.when(omElement2.getText()).thenReturn("GOLD");
        Mockito.when(element.getChildrenWithName(APIConstants.POLICY_ELEMENT)).thenReturn(list.iterator());
        APIUtil.deleteTier(tier, 5443);
        Mockito.verify(resource, Mockito.times(1)).setContent(Matchers.anyString());
    }

    @Test(expected = APIManagementException.class)
    public void testDeleteTiersTierWhenXMLStreamException() throws Exception {
        getMockedResource();
        Tier tier = Mockito.mock(Tier.class);
        Mockito.when(tier.getName()).thenReturn("GOLD");

        PowerMockito.mockStatic(AXIOMUtil.class);
        PowerMockito.when(AXIOMUtil.stringToOM(Matchers.anyString())).thenThrow(new XMLStreamException());
        APIUtil.deleteTier(tier, 5443);
    }

    @Test(expected = APIManagementException.class)
    public void testDeleteTiersTierWhenRegistryException() throws Exception {
        Resource resource = getMockedResource();
        Tier tier = Mockito.mock(Tier.class);
        Mockito.when(tier.getName()).thenReturn("GOLD");

        PowerMockito.mockStatic(AXIOMUtil.class);
        String content = new String((byte[]) resource.getContent(), Charset.defaultCharset());
        OMElement element = Mockito.mock(OMElement.class);
        PowerMockito.when(AXIOMUtil.stringToOM(content)).thenReturn(element);
        Mockito.when(element.getFirstChildWithName(APIConstants.ASSERTION_ELEMENT)).thenReturn(element);

        List<OMElement> list = new ArrayList<OMElement>();
        OMElement omElement1 = Mockito.mock(OMElement.class);
        list.add(omElement1);
        OMElement omElement2 = Mockito.mock(OMElement.class);
        Mockito.when(omElement1.getFirstChildWithName(APIConstants.THROTTLE_ID_ELEMENT)).thenReturn(omElement2);
        Mockito.when(omElement2.getText()).thenReturn("GOLD");
        Mockito.when(element.getChildrenWithName(APIConstants.POLICY_ELEMENT)).thenReturn(list.iterator());
        Mockito.doThrow(new org.wso2.carbon.registry.core.exceptions.RegistryException("")).when(resource).setContent(Matchers.anyString());
        APIUtil.deleteTier(tier, 5443);
    }

    @Test
    public void testGetTierDisplayNameWhenUnlimited() throws APIManagementException {
        String result = APIUtil.getTierDisplayName(5443, "Unlimited");
        Assert.assertEquals("Unlimited", result );
    }

    @Test
    public void testGetTierDisplayNameWhenDisplayNameNull() throws Exception {
        Resource resource = getMockedResource();
        Tier tier = Mockito.mock(Tier.class);
        Mockito.when(tier.getName()).thenReturn("GOLD");

        PowerMockito.mockStatic(AXIOMUtil.class);
        String content = new String((byte[]) resource.getContent(), Charset.defaultCharset());
        OMElement element = Mockito.mock(OMElement.class);
        PowerMockito.when(AXIOMUtil.stringToOM(content)).thenReturn(element);
        Mockito.when(element.getFirstChildWithName(APIConstants.ASSERTION_ELEMENT)).thenReturn(element);

        List<OMElement> list = new ArrayList<OMElement>();
        OMElement omElement1 = Mockito.mock(OMElement.class);
        list.add(omElement1);
        OMElement omElement2 = Mockito.mock(OMElement.class);
        Mockito.when(omElement1.getFirstChildWithName(APIConstants.THROTTLE_ID_ELEMENT)).thenReturn(omElement2);
        Mockito.when(omElement2.getText()).thenReturn("Gold");
        Mockito.when(element.getChildrenWithName(APIConstants.POLICY_ELEMENT)).thenReturn(list.iterator());
        String result = APIUtil.getTierDisplayName(5443, "Gold");
        Assert.assertEquals("Gold", result);
    }

    @Test
    public void testGetTierDisplayNameWithDisplayName() throws Exception {
        Resource resource = getMockedResource();
        Tier tier = Mockito.mock(Tier.class);
        Mockito.when(tier.getName()).thenReturn("GOLD");

        PowerMockito.mockStatic(AXIOMUtil.class);
        String content = new String((byte[]) resource.getContent(), Charset.defaultCharset());
        OMElement element = Mockito.mock(OMElement.class);
        PowerMockito.when(AXIOMUtil.stringToOM(content)).thenReturn(element);
        Mockito.when(element.getFirstChildWithName(APIConstants.ASSERTION_ELEMENT)).thenReturn(element);

        List<OMElement> list = new ArrayList<OMElement>();
        OMElement omElement1 = Mockito.mock(OMElement.class);
        list.add(omElement1);
        OMElement omElement2 = Mockito.mock(OMElement.class);
        Mockito.when(omElement1.getFirstChildWithName(APIConstants.THROTTLE_ID_ELEMENT)).thenReturn(omElement2);
        Mockito.when(omElement2.getText()).thenReturn("Gold");
        Mockito.when(omElement2.getAttribute(APIConstants.THROTTLE_ID_DISPLAY_NAME_ELEMENT)).thenReturn(Mockito.mock(
                OMAttribute.class));
        Mockito.when(omElement2.getAttributeValue(APIConstants.THROTTLE_ID_DISPLAY_NAME_ELEMENT)).thenReturn("Gold");
        Mockito.when(element.getChildrenWithName(APIConstants.POLICY_ELEMENT)).thenReturn(list.iterator());
        String result = APIUtil.getTierDisplayName(5443, "Gold");
        Assert.assertEquals("Gold", result);
    }

    @Test(expected = APIManagementException.class)
    public void testGetTierDisplayNameXMLStreamException() throws Exception {
        getMockedResource();
        Tier tier = Mockito.mock(Tier.class);
        Mockito.when(tier.getName()).thenReturn("GOLD");

        PowerMockito.mockStatic(AXIOMUtil.class);
        PowerMockito.when(AXIOMUtil.stringToOM(Matchers.anyString())).thenThrow(new XMLStreamException());
        APIUtil.getTierDisplayName(5443, "Gold");
    }

    @Test(expected = APIManagementException.class)
    public void testGetTierDisplayNameRegistryException() throws Exception {
        Tier tier = Mockito.mock(Tier.class);
        Mockito.when(tier.getName()).thenReturn("GOLD");
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        UserRegistry registry = Mockito.mock(UserRegistry.class);
        Mockito.when(registryService.getGovernanceSystemRegistry(5443)).thenReturn(registry);
        Mockito.when(registry.resourceExists(APIConstants.API_TIER_LOCATION)).thenReturn(true);

        Mockito.doThrow(new org.wso2.carbon.registry.core.exceptions.RegistryException("")).when(registry).get(Matchers.anyString());
        APIUtil.getTierDisplayName(5443, "Gold");
    }

    @Test
    public void testGetProvider() throws GovernanceException, APIManagementException {
        String providerName = "John";
        String providerDescription = "This is the description that goes under this provider object";
        String providerEmail = "email@provider.email.com";

        GenericArtifact genericArtifact = Mockito.mock(GenericArtifact.class);
        Mockito.when(genericArtifact.getAttribute(APIConstants.PROVIDER_OVERVIEW_NAME)).thenReturn(providerName);
        Mockito.when(genericArtifact.getAttribute(APIConstants.PROVIDER_OVERVIEW_DESCRIPTION)).thenReturn(providerDescription);
        Mockito.when(genericArtifact.getAttribute(APIConstants.PROVIDER_OVERVIEW_EMAIL)).thenReturn(providerEmail);

        Provider provider = APIUtil.getProvider(genericArtifact);
        Assert.assertEquals(provider.getName(), providerName);
        Assert.assertEquals(provider.getDescription(), providerDescription);
        Assert.assertEquals(provider.getEmail(), providerEmail);
    }

    @Test
    public void testGetProviderException() throws GovernanceException, APIManagementException {
        String exceptionMessage = "Failed to get provider ";

        GenericArtifact genericArtifact = Mockito.mock(GenericArtifact.class);
        Mockito.when(genericArtifact.getAttribute(APIConstants.PROVIDER_OVERVIEW_NAME)).thenThrow(new GovernanceException());

        try {
            APIUtil.getProvider(genericArtifact);
            Assert.fail();
        } catch (APIManagementException e ) {
            Assert.assertEquals(e.getMessage(), exceptionMessage);
        }
    }

    @Test
    public void testGetScopeByScopeKey() throws APIManagementException, UserStoreException {
        String scopeKey = "api_view";
        String provider = "john-AT-abc.com";
        final String tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        final int tenantId = -1234;
        PowerMockito.mockStatic(MultitenantUtils.class);
        Mockito.when(MultitenantUtils.getTenantDomain(Mockito.anyString())).
                thenReturn(org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RealmService realmService = Mockito.mock(RealmService.class);
        TenantManager tenantManager = Mockito.mock(TenantManager.class);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
        Mockito.when(tenantManager.getTenantId(tenantDomain)).thenReturn(tenantId);

        ApiMgtDAO apiMgtDAO = Mockito.mock(ApiMgtDAO.class);
        PowerMockito.mockStatic(ApiMgtDAO.class);
        Mockito.when(ApiMgtDAO.getInstance()).thenReturn(apiMgtDAO);

        Set<Scope> scopes = new LinkedHashSet<Scope>();
        Scope scope1 = new Scope();
        scope1.setId(1);
        scope1.setKey("api_view");
        scope1.setName("api_view");
        scope1.setDescription("Scope related to api view");
        scope1.setRoles("role1,role2");

        Scope scope2 = new Scope();
        scope2.setId(1);
        scope2.setKey("api_view");
        scope2.setName("api_view");
        scope2.setDescription("Scope related to api view");
        scope2.setRoles("role1,role2");

        scopes.add(scope1);
        scopes.add(scope2);


        Mockito.when(ApiMgtDAO.getInstance().getAPIScopesByScopeKey(scopeKey, tenantId)).thenReturn(scopes);
        Set<Scope> returnedScopes = APIUtil.getScopeByScopeKey(scopeKey, provider);
        Assert.assertEquals(returnedScopes.size(), 2);
    }

    @Test
    public void testGetScopeByScopeKeyException() throws APIManagementException, UserStoreException {
        String scopeKey = "api_view";
        String provider = "john-AT-abc.com";
        String expectedException = "Error while retrieving Scopes";
        final String tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        PowerMockito.mockStatic(MultitenantUtils.class);
        Mockito.when(MultitenantUtils.getTenantDomain(Mockito.anyString())).
                thenReturn(org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RealmService realmService = Mockito.mock(RealmService.class);
        TenantManager tenantManager = Mockito.mock(TenantManager.class);

        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
        Mockito.when(tenantManager.getTenantId(tenantDomain)).thenThrow(new UserStoreException());

        try {
            APIUtil.getScopeByScopeKey(scopeKey, provider);
            Assert.fail();
        } catch (APIManagementException e) {
            Assert.assertEquals(e.getMessage(), expectedException);
        }
    }

    @Test
    public void testGetAPIWithAPIIdentifier() throws Exception {
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(MultitenantConstants
                    .SUPER_TENANT_DOMAIN_NAME);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            API expectedAPI = getUniqueAPI();

            final String provider = expectedAPI.getId().getProviderName();
            final String tenantDomain = org.wso2.carbon.utils.multitenancy.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;

            final int tenantId = -1234;

            System.setProperty("carbon.home", "");

            File siteConfFile = new File(Thread.currentThread().getContextClassLoader().
                    getResource("tenant-conf.json").getFile());

            String tenantConfValue = FileUtils.readFileToString(siteConfFile);

            GovernanceArtifact artifact = Mockito.mock(GovernanceArtifact.class);
            Registry registry = Mockito.mock(Registry.class);
            ApiMgtDAO apiMgtDAO = Mockito.mock(ApiMgtDAO.class);
            Resource resource = Mockito.mock(Resource.class);
            ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
            RealmService realmService = Mockito.mock(RealmService.class);
            TenantManager tenantManager = Mockito.mock(TenantManager.class);
            APIManagerConfigurationService apiManagerConfigurationService = Mockito.mock(APIManagerConfigurationService.class);
            APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
            ThrottleProperties throttleProperties = Mockito.mock(ThrottleProperties.class);
            SubscriptionPolicy policy = Mockito.mock(SubscriptionPolicy.class);
            SubscriptionPolicy[] policies = new SubscriptionPolicy[] {policy};
            QuotaPolicy quotaPolicy = Mockito.mock(QuotaPolicy.class);
            RequestCountLimit limit = Mockito.mock(RequestCountLimit.class);
            RegistryService registryService = Mockito.mock(RegistryService.class);

            PowerMockito.mockStatic(ApiMgtDAO.class);
            PowerMockito.mockStatic(GovernanceUtils.class);
            PowerMockito.mockStatic(MultitenantUtils.class);
            PowerMockito.mockStatic(ServiceReferenceHolder.class);

            Mockito.when(ApiMgtDAO.getInstance()).thenReturn(apiMgtDAO);
            Mockito.when(apiMgtDAO.getAPIID(Mockito.any(APIIdentifier.class), eq((Connection) null))).thenReturn(123);
            Mockito.when(apiMgtDAO.getPolicyNames(PolicyConstants.POLICY_LEVEL_SUB, provider)).thenReturn(new String[] {"Unlimited"});
            Mockito.when(artifact.getId()).thenReturn("");
            Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER)).thenReturn(provider);
            Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_CACHE_TIMEOUT)).thenReturn("15");
            Mockito.when(artifact.getAttribute(APIConstants.API_OVERVIEW_TIER)).thenReturn("Unlimited");
            Mockito.when(MultitenantUtils.getTenantDomain(provider)).thenReturn(tenantDomain);
            Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
            Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
            Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
            Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
            Mockito.when(tenantManager.getTenantId(tenantDomain)).thenReturn(tenantId);

            String artifactPath = "";
            Mockito.when(GovernanceUtils.getArtifactPath(registry, "")).thenReturn(artifactPath);
            Mockito.when(registry.get(artifactPath)).thenReturn(resource);
            Mockito.when(resource.getLastModified()).thenReturn(expectedAPI.getLastUpdated());
            Mockito.when(resource.getCreatedTime()).thenReturn(expectedAPI.getLastUpdated());
            Mockito.when(resource.getContent()).thenReturn(tenantConfValue.getBytes());
            Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService()).thenReturn(apiManagerConfigurationService);
            Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
            Mockito.when(apiManagerConfiguration.getThrottleProperties()).thenReturn(throttleProperties);
            Mockito.when(throttleProperties.isEnabled()).thenReturn(true);
            Mockito.when(apiMgtDAO.getSubscriptionPolicies(tenantId)).thenReturn(policies);
            Mockito.when(policy.getDefaultQuotaPolicy()).thenReturn(quotaPolicy);
            Mockito.when(quotaPolicy.getLimit()).thenReturn(limit);
            Mockito.when(registry.getTags(artifactPath)).thenReturn(getTagsFromSet(expectedAPI.getTags()));

            ArrayList<URITemplate> urlList = getURLTemplateList(expectedAPI.getUriTemplates());
            Mockito.when(apiMgtDAO.getAllURITemplates(Mockito.anyString(), Mockito.anyString())).thenReturn(urlList);

            CORSConfiguration corsConfiguration = expectedAPI.getCorsConfiguration();

            Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_HEADERS)).
                    thenReturn(corsConfiguration.getAccessControlAllowHeaders().toString());
            Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_METHODS)).
                    thenReturn(corsConfiguration.getAccessControlAllowMethods().toString());
            Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_ORIGIN)).
                    thenReturn(corsConfiguration.getAccessControlAllowOrigins().toString());

            APIIdentifier apiIdentifier = Mockito.mock(APIIdentifier.class);
            API api = APIUtil.getAPI(artifact, registry, apiIdentifier, "");

            Assert.assertNotNull(api);
            Assert.assertNotNull(api.getId());
            Assert.assertEquals(api.getUriTemplates().size(), 7);
            Assert.assertEquals(api.getTags().size(),1);
        }finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    @Test
    public void testGetAvailableKeyStoreTables() throws APIManagementException {

        String domainString = "A:abc.com,B:pqr.com,C:xya.com";
        OAuthServerConfiguration oAuthServerConfiguration = Mockito.mock(OAuthServerConfiguration.class);
        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        Mockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oAuthServerConfiguration);
        Mockito.when(OAuthServerConfiguration.getInstance().getAccessTokenPartitioningDomains()).thenReturn(domainString);

        String[] keyStoreTables = APIUtil.getAvailableKeyStoreTables();

        Assert.assertEquals(keyStoreTables[0],"IDN_OAUTH2_ACCESS_TOKEN_C");
        Assert.assertEquals(keyStoreTables[1],"IDN_OAUTH2_ACCESS_TOKEN_A");
        Assert.assertEquals(keyStoreTables[2],"IDN_OAUTH2_ACCESS_TOKEN_B");
    }

    @Test
    public void testCheckAccessTokenPartitioningEnabled(){
        OAuthServerConfiguration oAuthServerConfiguration = Mockito.mock(OAuthServerConfiguration.class);
        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        Mockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oAuthServerConfiguration);
        Mockito.when(OAuthServerConfiguration.getInstance().isAccessTokenPartitioningEnabled()).thenReturn(true);
        Assert.assertTrue(APIUtil.checkAccessTokenPartitioningEnabled());
    }

    @Test
    public void testCheckUserNameAssertionEnabled(){
        OAuthServerConfiguration oAuthServerConfiguration = Mockito.mock(OAuthServerConfiguration.class);
        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        Mockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oAuthServerConfiguration);
        Mockito.when(OAuthServerConfiguration.getInstance().isUserNameAssertionEnabled()).thenReturn(true);
        Assert.assertTrue(APIUtil.checkUserNameAssertionEnabled());
    }
    @Test
    public void testLoadloadTenantAPIRXT() throws Exception {
        PowerMockito.mockStatic(RegistryUtils.class);
        PowerMockito.when(RegistryUtils.getRelativePathToOriginal(Mockito.anyString(), Mockito.anyString()))
                .thenReturn("abc/def");
        RegistryService registryService = Mockito.mock(RegistryService.class);
        ServiceReferenceHolder.getInstance().setRegistryService(registryService);
        UserRegistry userRegistry = Mockito.mock(UserRegistry.class);
        PowerMockito.mockStatic(CarbonUtils.class);
        URL url = Thread.currentThread().getContextClassLoader().getResource("api-manager.xml");
        PowerMockito.when(CarbonUtils.getCarbonHome()).thenReturn(url.getPath().split("/api-manager.xml")[0]);
        PowerMockito.mockStatic(RegistryAuthorizationManager.class);
        RegistryAuthorizationManager authorizationManager = Mockito.mock(RegistryAuthorizationManager.class);
        PowerMockito.whenNew(RegistryAuthorizationManager.class).withArguments(Mockito.any(UserRealm.class))
                .thenReturn(authorizationManager);
        org.wso2.carbon.user.api.AuthorizationManager authManager = Mockito.mock(org.wso2.carbon.user.api
                .AuthorizationManager.class);
        RealmService realmService = Mockito.mock(RealmService.class);
        UserRealm userRealm = Mockito.mock(UserRealm.class);
        Resource resource = Mockito.mock(Resource.class);
        ServiceReferenceHolder.getInstance().setRealmService(realmService);
        Mockito.when(realmService.getTenantUserRealm(-1234)).thenReturn(userRealm);
        Mockito.when(userRealm.getAuthorizationManager()).thenReturn(authManager);
        Mockito.when(userRegistry.resourceExists(Mockito.anyString())).thenReturn(false).thenReturn(true).thenReturn
                (false);
        Mockito.when(registryService.getGovernanceSystemRegistry(-1234)).thenReturn(userRegistry).thenThrow
                (RegistryException.class).thenReturn(userRegistry).thenReturn(userRegistry).thenReturn(userRegistry);
        Mockito.doNothing().doNothing().doThrow(UserStoreException.class).doNothing().when(authManager).authorizeRole
                (Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.when(userRegistry.newResource()).thenReturn(resource).thenReturn(resource).thenThrow
                (RegistryException.class);
        APIUtil.loadloadTenantAPIRXT("carbon.super",-1234);
        try {
            APIUtil.loadloadTenantAPIRXT("carbon.super",-1234);
            Assert.fail();
        }catch (APIManagementException ex){
            Assert.assertTrue(ex.getMessage().contains("Error when create registry instance "));
        }
        APIUtil.loadloadTenantAPIRXT("carbon.super",-1234);
        try {
            APIUtil.loadloadTenantAPIRXT("carbon.super",-1234);
            Assert.fail();
        }catch (APIManagementException ex){
            Assert.assertTrue(ex.getMessage().contains("Error while adding role permissions to API"));
        }
        try {
            APIUtil.loadloadTenantAPIRXT("carbon.super",-1234);
            Assert.fail();
        }catch (APIManagementException ex){
            Assert.assertTrue(ex.getMessage().contains("Failed to add rxt to registry "));
        }
    }
    @Test
    public void testSetDomainNameToUppercase(){
        String username = "wso2/admin";
        Assert.assertEquals(APIUtil.setDomainNameToUppercase(username),"WSO2/admin");
    }
    @Test
    public void testCreateRoles() throws UserStoreException {
        RealmService realmService = Mockito.mock(RealmService.class);
        ServiceReferenceHolder.getInstance().setRealmService(realmService);
        org.wso2.carbon.user.core.UserRealm userRealm = Mockito.mock(org.wso2.carbon.user.core.UserRealm.class);
        Mockito.when(realmService.getBootstrapRealm()).thenReturn(userRealm);
        org.wso2.carbon.user.api.UserRealm tenantRealm = Mockito.mock(org.wso2.carbon.user.api.UserRealm.class);
        Mockito.when(realmService.getTenantUserRealm(1)).thenReturn(tenantRealm);
        Mockito.when(realmService.getTenantUserRealm(-1234)).thenReturn(tenantRealm);
        org.wso2.carbon.user.core.UserStoreManager userStoreManager = Mockito.mock(org.wso2.carbon.user.core
                .UserStoreManager.class);
        Mockito.when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        Mockito.when(tenantRealm.getUserStoreManager()).thenReturn(userStoreManager);
        Mockito.when(userStoreManager.isExistingRole(Mockito.anyString())).thenReturn(false);
        RealmConfiguration realmConfiguration = Mockito.mock(RealmConfiguration.class);
        Mockito.when(tenantRealm.getRealmConfiguration()).thenReturn(realmConfiguration);
        Mockito.when(realmConfiguration.getAdminUserName()).thenReturn("admin");
        Mockito.doNothing().doNothing().doNothing().doNothing().doThrow(UserStoreException.class).when
                (userStoreManager).addRole(Mockito.anyString(), Mockito.any(String[].class), Mockito.any(Permission[]
                .class));
        try {
            APIUtil.createSubscriberRole("role1",-1234);
            Assert.assertTrue(true);
        } catch (APIManagementException e) {
            e.printStackTrace();
        }
        try {
            APIUtil.createPublisherRole("role1",-1234);
            Assert.assertTrue(true);
        } catch (APIManagementException e) {
            e.printStackTrace();
        }
        try {
            APIUtil.createCreatorRole("role1",-1234);
            Assert.assertTrue(true);
        } catch (APIManagementException e) {
            e.printStackTrace();
        }
        try {
            APIUtil.createCreatorRole("role1",1);
            Assert.assertTrue(true);
        } catch (APIManagementException e) {
            e.printStackTrace();
        }
        try {
            APIUtil.createCreatorRole("role1",1);
            Assert.fail();
        } catch (APIManagementException e) {
          Assert.assertTrue(e.getMessage().contains("Error while creating role: "));
        }


    }

    @Test
    public void testGetAccessTokenStoreTableFromUserId() throws APIManagementException {

        String domainString = "A:foo.com,B:pqr.com,C:xya.com";
        OAuthServerConfiguration oAuthServerConfiguration = Mockito.mock(OAuthServerConfiguration.class);
        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        Mockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oAuthServerConfiguration);
        Mockito.when(OAuthServerConfiguration.getInstance().getAccessTokenPartitioningDomains()).thenReturn(domainString);

        String accessToken = APIUtil.getAccessTokenStoreTableFromUserId("foo.com/admin");
        Assert.assertEquals(accessToken, "IDN_OAUTH2_ACCESS_TOKEN_A");
    }

    @Test
    public void testGetAccessTokenStoreTableFromUserIdWithoutDomain() throws APIManagementException {

        String domainString = "A:abc.com,B:pqr.com,C:xya.com";
        OAuthServerConfiguration oAuthServerConfiguration = Mockito.mock(OAuthServerConfiguration.class);
        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        Mockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oAuthServerConfiguration);
        Mockito.when(OAuthServerConfiguration.getInstance().getAccessTokenPartitioningDomains()).thenReturn(domainString);

        String accessToken = APIUtil.getAccessTokenStoreTableFromUserId("foo.com/admin");
        Assert.assertEquals(accessToken, "IDN_OAUTH2_ACCESS_TOKEN");
    }

    @Test
    public void testGetAccessTokenStoreTableFromAccessToken() throws APIManagementException {
        String apiKey = "Vkc0OVpscldTaDZpVkdmMnpyWmZBa1VrY2RnYTpQVk5fMkFfcndWWU1fejF6S19wemZycnBWQmdh";
        String accessToken = APIUtil.getAccessTokenStoreTableFromAccessToken(apiKey);
        Assert.assertEquals(accessToken, "IDN_OAUTH2_ACCESS_TOKEN");
    }

    @Test
    public void testIsAccessTokenExpiredTrue() {
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = Mockito.mock(APIKeyValidationInfoDTO.class);
        long issuedTime = 160000;
        long validTime = 180000;
        Mockito.when(apiKeyValidationInfoDTO.getValidityPeriod()).thenReturn(validTime);
        Mockito.when(apiKeyValidationInfoDTO.getIssuedTime()).thenReturn(issuedTime);

        long timeStamp = 1508995946;
        OAuthServerConfiguration oAuthServerConfiguration = Mockito.mock(OAuthServerConfiguration.class);
        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        Mockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oAuthServerConfiguration);
        Mockito.when(OAuthServerConfiguration.getInstance().getTimeStampSkewInSeconds()).thenReturn(timeStamp);

        boolean isExpired = APIUtil.isAccessTokenExpired(apiKeyValidationInfoDTO);
        Assert.assertEquals(isExpired, true);
    }

    @Test
    public void testIsAccessTokenExpiredFalse() {
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = Mockito.mock(APIKeyValidationInfoDTO.class);
        long issuedTime = 160000;
        Mockito.when(apiKeyValidationInfoDTO.getValidityPeriod()).thenReturn(Long.MAX_VALUE);
        Mockito.when(apiKeyValidationInfoDTO.getIssuedTime()).thenReturn(issuedTime);

        long timeStamp = 1508995946;
        OAuthServerConfiguration oAuthServerConfiguration = Mockito.mock(OAuthServerConfiguration.class);
        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        Mockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oAuthServerConfiguration);
        Mockito.when(OAuthServerConfiguration.getInstance().getTimeStampSkewInSeconds()).thenReturn(timeStamp);

        boolean isExpired = APIUtil.isAccessTokenExpired(apiKeyValidationInfoDTO);
        Assert.assertEquals(isExpired, false);
    }

    @Test
    public void testReplaceEmailDomain(){
        String input = "abc@abc.com";
        String emailStringExpected = "abc-AT-abc.com";
        String emailString = APIUtil.replaceEmailDomain(input);
        Assert.assertEquals(emailString, emailStringExpected);
    }

    @Test
    public void testReplaceEmailDomainForNullInputs(){
        String input = null;
        String emailString = APIUtil.replaceEmailDomain(input);
        Assert.assertNull(emailString);
    }

    @Test
    public void testReplaceEmailDomainInvalidInputs(){
        String input = "abc.com";
        String emailStringExpected = "abc.com";
        String emailString = APIUtil.replaceEmailDomain(input);
        Assert.assertEquals(emailString, emailStringExpected);
    }

    @Test
    public void testReplaceEmailDomainBack(){
        String input = "abc-AT-abc.com";
        String emailStringExpected = "abc@abc.com";
        String emailString = APIUtil.replaceEmailDomainBack(input);
        Assert.assertEquals(emailString, emailStringExpected);
    }

    @Test
    public void testReplaceEmailDomainBackForNullInputs(){
        String input = null;
        String emailString = APIUtil.replaceEmailDomainBack(input);
        Assert.assertNull(emailString);
    }

    @Test
    public void testReplaceEmailDomainBackInvalidInputs(){
        String input = "abc.com";
        String emailStringExpected = "abc.com";
        String emailString = APIUtil.replaceEmailDomainBack(input);
        Assert.assertEquals(emailString, emailStringExpected);
    }

    @Test
    public void testCopyResourcePermissions() throws UserStoreException {
        try {
            RegistryContext registryContext = Mockito.mock(RegistryContext.class);
            PowerMockito.mockStatic(RegistryContext.class);
            Mockito.when(RegistryContext.getBaseInstance()).thenReturn(registryContext);

            ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
            RealmService realmService = Mockito.mock(RealmService.class);
            TenantManager tenantManager = Mockito.mock(TenantManager.class);
            UserRealm userRealm = Mockito.mock(UserRealm.class);
            org.wso2.carbon.user.api.AuthorizationManager authorizationManager = Mockito
                    .mock(org.wso2.carbon.user.api.AuthorizationManager.class);

            PowerMockito.mockStatic(ServiceReferenceHolder.class);
            Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
            Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
            Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
            Mockito.when(realmService.getTenantUserRealm(Mockito.anyInt())).thenReturn(userRealm);
            Mockito.when(userRealm.getAuthorizationManager()).thenReturn(authorizationManager);
            Mockito.when(ServiceReferenceHolder.getInstance().getRealmService().getTenantManager()
                    .getTenantId(Mockito.anyString())).thenReturn(-1234);

            String[] allowedRoles = new String[] { "test3", "test4", "test5" };

            Mockito.when(authorizationManager.getAllowedRolesForResource(
                    "/_system/governance/_system/governance/apimgt/applicationdata/sourcepath", ActionConstants.GET))
                    .thenReturn(allowedRoles);

            String username = "admin";
            String sourceArtifactPath = "/_system/governance/apimgt/applicationdata/sourcepath";
            String targetArtifactPath = "/_system/governance/apimgt/applicationdata/targetpath";
            APIUtil.copyResourcePermissions(username, sourceArtifactPath, targetArtifactPath);

        } catch (APIManagementException e) {
            Assert.fail();
        }
    }

    @Test
    public void testSetResourcePermissionsForSuperTenant() throws Exception {
        String username = "admin";
        String visibility = "restricted";
        String[] roles = new String[] { "test3", "internal/everyone" };
        String artifactPath = "/_system/governance/apimgt/applicationdata/sourcepath";
        String resourcePath = "/_system/governance/_system/governance/apimgt/applicationdata/sourcepath";
        RegistryContext registryContext = Mockito.mock(RegistryContext.class);
        PowerMockito.mockStatic(RegistryContext.class);
        Mockito.when(RegistryContext.getBaseInstance()).thenReturn(registryContext);

        RegistryAuthorizationManager registryAuthorizationManager = Mockito.mock(RegistryAuthorizationManager.class);
        PowerMockito.whenNew(RegistryAuthorizationManager.class).withAnyArguments()
                .thenReturn(registryAuthorizationManager);

        Mockito.doNothing().
                when(registryAuthorizationManager).authorizeRole("test3", resourcePath, ActionConstants.GET);
        Mockito.doNothing().
                when(registryAuthorizationManager).authorizeRole("test4", resourcePath, ActionConstants.GET);
        Mockito.doNothing().
                when(registryAuthorizationManager)
                .authorizeRole(APIConstants.EVERYONE_ROLE, resourcePath, ActionConstants.GET);
        Mockito.doNothing().
                when(registryAuthorizationManager)
                .authorizeRole(APIConstants.EVERYONE_ROLE, resourcePath, ActionConstants.GET);
        Mockito.doNothing().
                when(registryAuthorizationManager)
                .authorizeRole(APIConstants.ANONYMOUS_ROLE, resourcePath, ActionConstants.GET);

        APIUtil.setResourcePermissions(username, visibility, roles, artifactPath);
        roles = new String[] { "test4" };
        APIUtil.setResourcePermissions(username, visibility, roles, artifactPath);

        visibility = "private";
        APIUtil.setResourcePermissions(username, visibility, roles, artifactPath);
        visibility = "OWNER_ONLY";
        APIUtil.setResourcePermissions(username, visibility, roles, artifactPath);
        roles = null;
        visibility = "OWNER_ONLY";
        APIUtil.setResourcePermissions(username, visibility, roles, artifactPath);
        visibility = "none";
        APIUtil.setResourcePermissions(username, visibility, roles, artifactPath);

        String tenantDomain = "abc.com";

        PowerMockito.mockStatic(MultitenantUtils.class);
        Mockito.when(MultitenantUtils.getTenantDomain(Mockito.anyString())).thenReturn(tenantDomain);
        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RealmService realmService = Mockito.mock(RealmService.class);
        TenantManager tenantManager = Mockito.mock(TenantManager.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
        Mockito.when(tenantManager.getTenantId(tenantDomain)).thenReturn(1);

        org.wso2.carbon.user.api.AuthorizationManager authorizationManager = Mockito
                .mock(org.wso2.carbon.user.api.AuthorizationManager.class);
        UserRealm userRealm = Mockito.mock(UserRealm.class);
        Mockito.when(realmService.getTenantUserRealm(1)).thenReturn(userRealm);
        Mockito.when(userRealm.getAuthorizationManager()).thenReturn(authorizationManager);

        Mockito.doNothing().
                when(authorizationManager).authorizeRole("test3", resourcePath, ActionConstants.GET);
        Mockito.doNothing().
                when(authorizationManager).authorizeRole("test4", resourcePath, ActionConstants.GET);
        Mockito.doNothing().
                when(authorizationManager).authorizeRole(APIConstants.EVERYONE_ROLE, resourcePath, ActionConstants.GET);
        Mockito.doNothing().
                when(authorizationManager).authorizeRole(APIConstants.EVERYONE_ROLE, resourcePath, ActionConstants.GET);
        Mockito.doNothing().
                when(authorizationManager)
                .authorizeRole(APIConstants.ANONYMOUS_ROLE, resourcePath, ActionConstants.GET);
        visibility = "restricted";
        roles = new String[] { "" };
        APIUtil.setResourcePermissions(username, visibility, roles, artifactPath);
        roles = new String[] { "test3", "internal/everyone" };
        APIUtil.setResourcePermissions(username, visibility, roles, artifactPath);
        roles = new String[] { "test3" };
        APIUtil.setResourcePermissions(username, visibility, roles, artifactPath);

        visibility = "private";
        APIUtil.setResourcePermissions(username, visibility, roles, artifactPath);
        visibility = "OWNER_ONLY";
        APIUtil.setResourcePermissions(username, visibility, roles, artifactPath);
        roles = null;
        visibility = "OWNER_ONLY";
        APIUtil.setResourcePermissions(username, visibility, roles, artifactPath);
        visibility = "none";
        APIUtil.setResourcePermissions(username, visibility, roles, artifactPath);

    }

    @Test(expected = APIManagementException.class)
    public void testCheckPermissionUserNameNull() throws Exception {
        APIUtil.checkPermission(null, "create");
    }

    @Test
    public void testCheckPermissionUserName() throws Exception {
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        APIManagerConfigurationService apiManagerConfigurationService = Mockito
                .mock(APIManagerConfigurationService.class);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);

        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.API_STORE_DISABLE_PERMISSION_CHECK))
                .thenReturn("true");
        //permission check disabled scenario
        APIUtil.checkPermission("john", "create");

        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.API_STORE_DISABLE_PERMISSION_CHECK))
                .thenReturn("false");
        PowerMockito.mockStatic(MultitenantUtils.class);
        PowerMockito.when(MultitenantUtils.getTenantDomain("john")).thenReturn("foo.com");
        System.setProperty("carbon.home", "");
        PrivilegedCarbonContext privilegedCarbonContext = Mockito.mock(PrivilegedCarbonContext.class);
        PowerMockito.mockStatic(PrivilegedCarbonContext.class);
        PowerMockito.when(PrivilegedCarbonContext.getThreadLocalCarbonContext()).thenReturn(privilegedCarbonContext);

        RealmService realmService = Mockito.mock(RealmService.class);
        TenantManager tenantManager = Mockito.mock(TenantManager.class);

        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
        Mockito.when(tenantManager.getTenantId("foo.com")).thenReturn(5443);
        UserRealm userRealm = Mockito.mock(UserRealm.class);
        Mockito.when(realmService.getTenantUserRealm(5443)).thenReturn(userRealm);
        org.wso2.carbon.user.api.AuthorizationManager authorizationManager = Mockito
                .mock(org.wso2.carbon.user.api.AuthorizationManager.class);
        Mockito.when(userRealm.getAuthorizationManager()).thenReturn(authorizationManager);
        Mockito.when(authorizationManager.isUserAuthorized(MultitenantUtils.getTenantAwareUsername("john"), "create",
                CarbonConstants.UI_PERMISSION_ACTION)).thenReturn(true);
        //permission check enabled scenario
        APIUtil.checkPermission("john", "create");

        PowerMockito.when(MultitenantUtils.getTenantDomain("john")).thenReturn("carbon.super");
        Mockito.when(tenantManager.getTenantId("carbon.super")).thenReturn(5443);
        Mockito.when(realmService.getTenantUserRealm(5443)).thenReturn(Mockito.mock(org.wso2.carbon.user.core.UserRealm.class));
        PowerMockito.mockStatic(AuthorizationManager.class);
        AuthorizationManager authorizationManager1 = Mockito.mock(AuthorizationManager.class);
        PowerMockito.when(AuthorizationManager.getInstance()).thenReturn(authorizationManager1);
        try {
            APIUtil.checkPermission("john", "create");//not authorized scenario
            Assert.fail(); // if exception not thrown test should fail
        } catch (APIManagementException e) {

        }
    }

    @Test(expected = APIManagementException.class)
    public void testCheckPermissionUserNameUserStoreException() throws Exception {
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        APIManagerConfigurationService apiManagerConfigurationService = Mockito
                .mock(APIManagerConfigurationService.class);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.API_STORE_DISABLE_PERMISSION_CHECK))
                .thenReturn("false");

        PowerMockito.mockStatic(MultitenantUtils.class);
        PowerMockito.when(MultitenantUtils.getTenantDomain("john")).thenReturn("foo.com");
        System.setProperty("carbon.home", "");
        PrivilegedCarbonContext privilegedCarbonContext = Mockito.mock(PrivilegedCarbonContext.class);
        PowerMockito.mockStatic(PrivilegedCarbonContext.class);
        PowerMockito.when(PrivilegedCarbonContext.getThreadLocalCarbonContext()).thenReturn(privilegedCarbonContext);

        RealmService realmService = Mockito.mock(RealmService.class);
        TenantManager tenantManager = Mockito.mock(TenantManager.class);

        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
        Mockito.when(tenantManager.getTenantId("foo.com")).thenReturn(5443);
        UserRealm userRealm = Mockito.mock(UserRealm.class);
        Mockito.when(realmService.getTenantUserRealm(5443)).thenReturn(userRealm);
        org.wso2.carbon.user.api.AuthorizationManager authorizationManager = Mockito
                .mock(org.wso2.carbon.user.api.AuthorizationManager.class);
        Mockito.when(userRealm.getAuthorizationManager()).thenReturn(authorizationManager);
        Mockito.when(
                authorizationManager.isUserAuthorized(Matchers.anyString(), Matchers.anyString(), Matchers.anyString()))
                .thenThrow(new org.wso2.carbon.user.core.UserStoreException());
        APIUtil.checkPermission("john", "create");
    }

    @Test
    public void testIsPermissionCheckDisabled() throws Exception {
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        APIManagerConfigurationService apiManagerConfigurationService = Mockito.mock(APIManagerConfigurationService.class);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService()).thenReturn(apiManagerConfigurationService);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);

        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.API_STORE_DISABLE_PERMISSION_CHECK)).thenReturn("true");
        Assert.assertTrue(APIUtil.isPermissionCheckDisabled());

        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.API_STORE_DISABLE_PERMISSION_CHECK)).thenReturn(null);
        Assert.assertFalse(APIUtil.isPermissionCheckDisabled());
    }

    @Test(expected = APIManagementException.class)
    public void testHasPermissionAnonymousUser() throws APIManagementException {
        APIUtil.hasPermission(null, "create");
    }

    @Test
    public void testHasPermissionPermissionDisabled() throws APIManagementException {
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        APIManagerConfigurationService apiManagerConfigurationService = Mockito
                .mock(APIManagerConfigurationService.class);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);

        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.API_STORE_DISABLE_PERMISSION_CHECK))
                .thenReturn("true");
        boolean result = APIUtil.hasPermission("john", "create");
        Assert.assertTrue(result);
    }

    @Test
    public void testHasPermissionPermissionEnabled() throws Exception {
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        APIManagerConfigurationService apiManagerConfigurationService = Mockito
                .mock(APIManagerConfigurationService.class);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);

        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.API_STORE_DISABLE_PERMISSION_CHECK))
                .thenReturn("false");
        PowerMockito.mockStatic(MultitenantUtils.class);
        PowerMockito.when(MultitenantUtils.getTenantDomain("john")).thenReturn("foo.com");
        System.setProperty("carbon.home", "");
        PrivilegedCarbonContext privilegedCarbonContext = Mockito.mock(PrivilegedCarbonContext.class);
        PowerMockito.mockStatic(PrivilegedCarbonContext.class);
        PowerMockito.when(PrivilegedCarbonContext.getThreadLocalCarbonContext()).thenReturn(privilegedCarbonContext);

        RealmService realmService = Mockito.mock(RealmService.class);
        TenantManager tenantManager = Mockito.mock(TenantManager.class);
        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(realmService.getTenantManager()).thenReturn(tenantManager);
        Mockito.when(tenantManager.getTenantId("foo.com")).thenReturn(5443);
        UserRealm userRealm = Mockito.mock(UserRealm.class);
        Mockito.when(realmService.getTenantUserRealm(5443)).thenReturn(userRealm);
        org.wso2.carbon.user.api.AuthorizationManager authorizationManager = Mockito
                .mock(org.wso2.carbon.user.api.AuthorizationManager.class);
        Mockito.when(authorizationManager.isUserAuthorized(MultitenantUtils.getTenantAwareUsername("john"), "create",
                CarbonConstants.UI_PERMISSION_ACTION)).thenReturn(true);
        Mockito.when(userRealm.getAuthorizationManager()).thenReturn(authorizationManager);
        boolean result = APIUtil.hasPermission("john", "create"); // tenant case
        Assert.assertTrue(result);
    }

    @Test
    public void testLoadTenantAPIPolicy()
            throws APIManagementException, org.wso2.carbon.registry.core.exceptions.RegistryException {

        String tenant = "carbon.super";
        int tenantId = -1234;

        PowerMockito.mockStatic(CarbonUtils.class);
        Mockito.when(CarbonUtils.getCarbonHome())
                .thenReturn(APIUtilTest.class.getResource("/").getPath().replaceAll("/$", ""));

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry userRegistry = Mockito.mock(UserRegistry.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(-1234))).thenReturn(userRegistry);

        org.wso2.carbon.registry.core.Resource resource = Mockito.mock(org.wso2.carbon.registry.core.Resource.class);
        Mockito.when(userRegistry.newResource()).thenReturn(resource);

        Mockito.when(userRegistry.put(APIConstants.API_TIER_LOCATION, resource))
                .thenReturn(APIConstants.API_TIER_LOCATION);

        Mockito.when(userRegistry.resourceExists(APIConstants.RES_TIER_LOCATION)).thenReturn(true);
        APIUtil.loadTenantAPIPolicy(tenant, tenantId);
    }

    @Test
    public void testLoadTenantAPIPolicyException() throws Exception {

        String tenant = "carbon.super";
        int tenantId = -1234;

        PowerMockito.mockStatic(CarbonUtils.class);
        Mockito.when(CarbonUtils.getCarbonHome())
                .thenReturn(APIUtilTest.class.getResource("/").getPath().replaceAll("/$", ""));

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry userRegistry = Mockito.mock(UserRegistry.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(-1234))).thenReturn(userRegistry);

        org.wso2.carbon.registry.core.Resource resource = Mockito.mock(org.wso2.carbon.registry.core.Resource.class);
        Mockito.when(userRegistry.newResource()).thenReturn(resource);

        Mockito.when(userRegistry.put(APIConstants.API_TIER_LOCATION, resource))
                .thenReturn(APIConstants.API_TIER_LOCATION);

        Mockito.when(userRegistry.resourceExists(APIConstants.RES_TIER_LOCATION)).thenReturn(true);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(-1234)))
                .thenThrow(new org.wso2.carbon.registry.core.exceptions.RegistryException(""));
        String expectedString = "Error while saving policy information to the registry";
        try {
            APIUtil.loadTenantAPIPolicy(tenant, tenantId);
            Assert.fail();
        } catch (APIManagementException e) {
            Assert.assertEquals(e.getMessage(), expectedString);
        }
    }

    @Test
    public void testLoadTenantExternalStoreConfigWhenResourceExists() throws APIManagementException, RegistryException {
        int tenantID = -1234;

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry userRegistry = Mockito.mock(UserRegistry.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(-1234))).thenReturn(userRegistry);

        Mockito.when(userRegistry.resourceExists(APIConstants.EXTERNAL_API_STORES_LOCATION)).thenReturn(true);
        APIUtil.loadTenantExternalStoreConfig(tenantID);
    }

    @Test
    public void testLoadTenantExternalStoreConfig()
            throws APIManagementException, RegistryException, FileNotFoundException, UserStoreException {
        int tenantID = -1234;

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        RegistryService registryService = Mockito.mock(RegistryService.class);
        UserRegistry userRegistry = Mockito.mock(UserRegistry.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        Mockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getRegistryService()).thenReturn(registryService);
        Mockito.when(registryService.getGovernanceSystemRegistry(eq(tenantID))).thenReturn(userRegistry);

        Mockito.when(userRegistry.resourceExists(APIConstants.EXTERNAL_API_STORES_LOCATION)).thenReturn(false);

        String defaultExternalAPIStoresXMLPath = "externalstores/default-external-api-stores.xml";

        InputStream resourceStream = new FileInputStream(
                APIUtilTest.class.getResource("/").getPath() + defaultExternalAPIStoresXMLPath);

        PowerMockito.mockStatic(APIManagerComponent.class);
        PowerMockito.when(APIManagerComponent.class.getResourceAsStream("/" + defaultExternalAPIStoresXMLPath))
                .thenReturn(resourceStream);

        org.wso2.carbon.registry.core.Resource resource = Mockito.mock(org.wso2.carbon.registry.core.Resource.class);
        Mockito.when(userRegistry.newResource()).thenReturn(resource);

        Mockito.when(userRegistry.put(APIConstants.EXTERNAL_API_STORES_LOCATION, resource))
                .thenReturn(APIConstants.EXTERNAL_API_STORES_LOCATION);

        PowerMockito.mockStatic(RegistryUtils.class);
        String resourcePath = "/_system/governance/apimgt/externalstores/external-api-stores.xml";
        RegistryUtils registryUtils = Mockito.mock(RegistryUtils.class);

        RegistryContext registryContext = Mockito.mock(RegistryContext.class);
        PowerMockito.mockStatic(RegistryContext.class);
        Mockito.when(RegistryContext.getBaseInstance()).thenReturn(registryContext);
        PowerMockito.when(registryUtils.getAbsolutePath(registryContext, resourcePath)).thenReturn(resourcePath);

        RealmService realmService = Mockito.mock(RealmService.class);
        UserRealm userRealm = Mockito.mock(UserRealm.class);
        org.wso2.carbon.user.api.AuthorizationManager authorizationManager = Mockito
                .mock(org.wso2.carbon.user.api.AuthorizationManager.class);

        Mockito.when(serviceReferenceHolder.getRealmService()).thenReturn(realmService);
        Mockito.when(realmService.getTenantUserRealm(Mockito.anyInt())).thenReturn(userRealm);
        Mockito.when(userRealm.getAuthorizationManager()).thenReturn(authorizationManager);

        Mockito.doNothing().when(authorizationManager)
                .denyRole(APIConstants.EVERYONE_ROLE, resourcePath, ActionConstants.GET);
        APIUtil.loadTenantExternalStoreConfig(tenantID);

        Mockito.when(userRegistry.resourceExists(APIConstants.EXTERNAL_API_STORES_LOCATION)).thenReturn(true);
        APIUtil.loadTenantExternalStoreConfig(tenantID);
    }



    @Test
    public void testRemoveAnySymbolFromUriTempate() {
        String uriTemplate = "/pizzashack/delivery/*";
        Assert.assertEquals(APIUtil.removeAnySymbolFromUriTemplate(uriTemplate), "/pizzashack/delivery");
    }

    @Test
    public void testGetAverageRatingByAPIIdentifier() throws APIManagementException {
        ApiMgtDAO apiMgtDAO = TestUtils.getApiMgtDAO();
        APIIdentifier apiIdentifier = new APIIdentifier("admin", "weatherAPI", "v1.0.0");
        Mockito.when(apiMgtDAO.getAverageRating(apiIdentifier)).thenReturn(4.9f);
        try {
            Assert.assertEquals(APIUtil.getAverageRating(apiIdentifier), 4.9, 1e-1);
        } catch (APIManagementException e) {
            Assert.fail("Unexpected APIManagementException occurred while retrieving API's average rating");
        }
    }

    @Test
    public void testGetAverageRatingByAPIID() throws APIManagementException {
        ApiMgtDAO apiMgtDAO = TestUtils.getApiMgtDAO();
        Mockito.when(apiMgtDAO.getAverageRating(1)).thenReturn(4.9f);
        try {
            Assert.assertEquals(APIUtil.getAverageRating(1), 4.9, 1e-1);
        } catch (APIManagementException e) {
            Assert.fail("Unexpected APIManagementException occurred while retrieving API's average rating");
        }
    }

    @Test
    public void testGetAllTenantsWithSuperTenant() throws UserStoreException {
        TenantManager tenantManager = TestUtils.getTenantManager();
        Tenant tenant = new Tenant();
        tenant.setDomain("wso2.com");
        tenant.setId(1);
        tenant.setAdminName("admin");
        tenant.setAdminPassword("admin");
        Tenant[] tenants = {tenant};
        Mockito.when(tenantManager.getAllTenants()).thenReturn(tenants);

        try {
            List<Tenant> tenantList = APIUtil.getAllTenantsWithSuperTenant();
            Assert.assertEquals(tenantList.size(), 2);
        } catch (UserStoreException e) {
            Assert.fail("Unexpected APIManagementException occurred while retrieving all available tenants");
        }
    }

    @Test
    public void testIsLoggedInUserAuthorizedToRevokeToken() {

        //Test authorized super tenant user in super tenant domain
        Assert.assertTrue(APIUtil.isLoggedInUserAuthorizedToRevokeToken("testUser@carbon.super", "testUser@carbon" +
                ".super"));

        //Test unauthorized tenant user in super tenant domain
        Assert.assertFalse(APIUtil.isLoggedInUserAuthorizedToRevokeToken("testUser@wso2.com",
                "testUser@carbon.super"));

        //Test authorized tenant user in tenant domain
        Assert.assertTrue(APIUtil.isLoggedInUserAuthorizedToRevokeToken("testUser@wso2.com", "testUser@wso2.com"));

        //Test unauthorized tenant user in different tenant domain
        Assert.assertFalse(APIUtil.isLoggedInUserAuthorizedToRevokeToken("testUser@hr.com",
                "testUser@wso2.com"));
    }

    @Test
    public void testGetApplicationId() throws APIManagementException {
        ApiMgtDAO apiMgtDAO = TestUtils.getApiMgtDAO();
        String appName = "DefaultApplication";
        String userID = "admin";
        Mockito.when(apiMgtDAO.getApplicationId(appName, userID)).thenReturn(1);

        try {
            Assert.assertEquals(APIUtil.getApplicationId(appName, userID), 1);
        } catch (APIManagementException e) {
            Assert.fail("Unexpected APIManagementException occurred while retrieving application ID by name and user");
        }
    }

    @Test
    public void testIsAPIManagementEnabled() {
        PowerMockito.mockStatic(CarbonUtils.class);
        ServerConfiguration serverConfiguration = Mockito.mock(ServerConfiguration.class);
        Mockito.when(serverConfiguration.getFirstProperty("APIManagement.Enabled")).thenReturn("true");
        PowerMockito.when(CarbonUtils.getServerConfiguration()).thenReturn(serverConfiguration);
        Assert.assertTrue(APIUtil.isAPIManagementEnabled());
    }

    @Test
    public void testIsLoadAPIContextsAtStartup() {
        PowerMockito.mockStatic(CarbonUtils.class);
        ServerConfiguration serverConfiguration = Mockito.mock(ServerConfiguration.class);
        Mockito.when(serverConfiguration.getFirstProperty("APIManagement.LoadAPIContextsInServerStartup"))
                .thenReturn("true");
        PowerMockito.when(CarbonUtils.getServerConfiguration()).thenReturn(serverConfiguration);
        Assert.assertTrue(APIUtil.isLoadAPIContextsAtStartup());
    }

    @Test
    public void testIsAllowDisplayMultipleVersionsWhenConfiguredInTenantLevel() throws Exception {
        String tenantDomain = "wso2.com";
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        APIMRegistryServiceImpl apimRegistryService = Mockito.mock(APIMRegistryServiceImpl.class);
        PowerMockito.whenNew(APIMRegistryServiceImpl.class).withNoArguments().thenReturn(apimRegistryService);
        File siteConfFile = new File(Thread.currentThread().getContextClassLoader().
                getResource("tenant-conf.json").getFile());
        String tenantConfValue = FileUtils.readFileToString(siteConfFile);
        PowerMockito.when(apimRegistryService.getConfigRegistryResourceContent(tenantDomain, APIConstants
                .API_TENANT_CONF_LOCATION)).thenReturn(tenantConfValue);

        try {
            Assert.assertTrue(APIUtil.isAllowDisplayMultipleVersions());
        } catch (APIManagementException e) {
            Assert.fail("Unexpected APIManagementException occurred while checking whether 'DisplayMultipleVersions' " +
                    "is enabled in tenant level");
        }
    }

    @Test
    public void testIsAllowDisplayMultipleVersionsUserStoreExceptionWhenConfiguredInTenantLevel() throws Exception {

        String tenantDomain = "wso2.com";
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        APIMRegistryServiceImpl apimRegistryService = Mockito.mock(APIMRegistryServiceImpl.class);
        PowerMockito.whenNew(APIMRegistryServiceImpl.class).withNoArguments().thenReturn(apimRegistryService);
        PowerMockito.when(apimRegistryService.getConfigRegistryResourceContent(tenantDomain, APIConstants
                .API_TENANT_CONF_LOCATION)).thenThrow(new UserStoreException("UserStoreException thrown when " +
                "tenant-config.json"));

        try {
            APIUtil.isAllowDisplayMultipleVersions();
            Assert.fail("Expected APIManagementException has not been thrown");
        } catch (APIManagementException e) {
            Assert.assertEquals(e.getMessage(), "UserStoreException thrown when getting tenant-config.json");
        }
    }

    @Test
    public void testIsAllowDisplayMultipleVersionsRegistryExceptionWhenConfiguredInTenantLevel() throws Exception {

        String tenantDomain = "wso2.com";
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        APIMRegistryServiceImpl apimRegistryService = Mockito.mock(APIMRegistryServiceImpl.class);
        PowerMockito.whenNew(APIMRegistryServiceImpl.class).withNoArguments().thenReturn(apimRegistryService);
        PowerMockito.when(apimRegistryService.getConfigRegistryResourceContent(tenantDomain, APIConstants
                .API_TENANT_CONF_LOCATION)).thenThrow(new org.wso2.carbon.registry.core.exceptions.RegistryException
                ("RegistryException thrown when getting tenant-config.json"));

        try {
            APIUtil.isAllowDisplayMultipleVersions();
            Assert.fail("Expected APIManagementException has not been thrown");
        } catch (APIManagementException e) {
            Assert.assertEquals(e.getMessage(), "RegistryException thrown when getting tenant-config.json");
        }
    }

    @Test
    public void testIsAllowDisplayMultipleVersionsParseExceptionWhenConfiguredInTenantLevel() throws Exception {

        String tenantDomain = "wso2.com";
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        APIMRegistryServiceImpl apimRegistryService = Mockito.mock(APIMRegistryServiceImpl.class);
        PowerMockito.whenNew(APIMRegistryServiceImpl.class).withNoArguments().thenReturn(apimRegistryService);
        PowerMockito.when(apimRegistryService.getConfigRegistryResourceContent(tenantDomain, APIConstants
                .API_TENANT_CONF_LOCATION)).thenReturn("{\"invalid\"}");

        try {
            APIUtil.isAllowDisplayMultipleVersions();
            Assert.fail("Expected APIManagementException has not been thrown");
        } catch (APIManagementException e) {
            Assert.assertEquals(e.getMessage(), "ParseException thrown when parsing the tenant-config.json content");
        }
    }

    @Test
    public void testIsAllowDisplayMultipleVersionsWhenConfiguredInGlobally() throws Exception {
        String tenantDomain = "wso2.com";
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        APIMRegistryServiceImpl apimRegistryService = Mockito.mock(APIMRegistryServiceImpl.class);
        PowerMockito.whenNew(APIMRegistryServiceImpl.class).withNoArguments().thenReturn(apimRegistryService);
        PowerMockito.when(apimRegistryService.getConfigRegistryResourceContent(tenantDomain, APIConstants
                .API_TENANT_CONF_LOCATION)).thenReturn(null);
        ServiceReferenceHolder serviceReferenceHolder = TestUtils.getServiceReferenceHolder();
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        APIManagerConfigurationService apiManagerConfigurationService = new APIManagerConfigurationServiceImpl
                (apiManagerConfiguration);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService()).thenReturn
                (apiManagerConfigurationService);

        //When configuration is not found in api-manager.xml
        try {
            Assert.assertFalse(APIUtil.isAllowDisplayMultipleVersions());
        } catch (APIManagementException e) {
            Assert.fail("Unexpected APIManagementException occurred while checking whether 'DisplayMultipleVersions' " +
                    "is enabled in tenant level");
        }

        //When configuration is found in api-manager.xml
        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.API_STORE_DISPLAY_MULTIPLE_VERSIONS))
                .thenReturn("true");
        try {
            Assert.assertTrue(APIUtil.isAllowDisplayMultipleVersions());
        } catch (APIManagementException e) {
            Assert.fail("Unexpected APIManagementException occurred while checking whether 'DisplayMultipleVersions' " +
                    "is enabled in tenant level");
        }
    }

    @Test
    public void testIsAllowDisplayAPIsWithMultipleStatusWhenConfiguredInTenantLevel() throws Exception {
        String tenantDomain = "wso2.com";
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        APIMRegistryServiceImpl apimRegistryService = Mockito.mock(APIMRegistryServiceImpl.class);
        PowerMockito.whenNew(APIMRegistryServiceImpl.class).withNoArguments().thenReturn(apimRegistryService);
        File siteConfFile = new File(Thread.currentThread().getContextClassLoader().
                getResource("tenant-conf.json").getFile());
        String tenantConfValue = FileUtils.readFileToString(siteConfFile);
        PowerMockito.when(apimRegistryService.getConfigRegistryResourceContent(tenantDomain, APIConstants
                .API_TENANT_CONF_LOCATION)).thenReturn(tenantConfValue);

        try {
            Assert.assertTrue(APIUtil.isAllowDisplayAPIsWithMultipleStatus());
        } catch (APIManagementException e) {
            Assert.fail("Unexpected APIManagementException occurred while checking whether 'DisplayAllAPIs' " +
                    "is enabled in tenant level");
        }
    }


    @Test
    public void testIsAllowDisplayAPIsWithMultipleStatusUserStoreExceptionWhenConfiguredInTenantLevel() throws
            Exception {

        String tenantDomain = "wso2.com";
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        APIMRegistryServiceImpl apimRegistryService = Mockito.mock(APIMRegistryServiceImpl.class);
        PowerMockito.whenNew(APIMRegistryServiceImpl.class).withNoArguments().thenReturn(apimRegistryService);
        PowerMockito.when(apimRegistryService.getConfigRegistryResourceContent(tenantDomain, APIConstants
                .API_TENANT_CONF_LOCATION)).thenThrow(new UserStoreException("UserStoreException thrown when getting " +
                "tenant-config.json"));

        try {
            APIUtil.isAllowDisplayAPIsWithMultipleStatus();
            Assert.fail("Expected APIManagementException has not been thrown");
        } catch (APIManagementException e) {
            Assert.assertEquals(e.getMessage(), "UserStoreException thrown when getting tenant-config.json");
        }
    }

    @Test
    public void testIsAllowDisplayAPIsWithMultipleStatusRegistryExceptionWhenConfiguredInTenantLevel() throws
            Exception {

        String tenantDomain = "wso2.com";
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        APIMRegistryServiceImpl apimRegistryService = Mockito.mock(APIMRegistryServiceImpl.class);
        PowerMockito.whenNew(APIMRegistryServiceImpl.class).withNoArguments().thenReturn(apimRegistryService);
        PowerMockito.when(apimRegistryService.getConfigRegistryResourceContent(tenantDomain, APIConstants
                .API_TENANT_CONF_LOCATION)).thenThrow(new org.wso2.carbon.registry.core.exceptions.RegistryException
                ("RegistryException thrown when getting tenant-config.json"));

        try {
            APIUtil.isAllowDisplayAPIsWithMultipleStatus();
            Assert.fail("Expected APIManagementException has not been thrown");
        } catch (APIManagementException e) {
            Assert.assertEquals(e.getMessage(), "RegistryException thrown when getting tenant-config.json");
        }
    }

    @Test
    public void testIsAllowDisplayAPIsWithMultipleStatusParseExceptionWhenConfiguredInTenantLevel() throws Exception {

        String tenantDomain = "wso2.com";
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        APIMRegistryServiceImpl apimRegistryService = Mockito.mock(APIMRegistryServiceImpl.class);
        PowerMockito.whenNew(APIMRegistryServiceImpl.class).withNoArguments().thenReturn(apimRegistryService);
        PowerMockito.when(apimRegistryService.getConfigRegistryResourceContent(tenantDomain, APIConstants
                .API_TENANT_CONF_LOCATION)).thenReturn("{\"invalid\"}");

        try {
            APIUtil.isAllowDisplayAPIsWithMultipleStatus();
            Assert.fail("Expected APIManagementException has not been thrown");
        } catch (APIManagementException e) {
            Assert.assertEquals(e.getMessage(), "ParseException thrown when parsing the tenant-config.json content");
        }
    }

    @Test
    public void testIsAllowDisplayAPIsWithMultipleStatusWhenConfiguredInGlobally() throws Exception {
        String tenantDomain = "wso2.com";
        System.setProperty("carbon.home", APIUtilTest.class.getResource("/").getFile());
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
        APIMRegistryServiceImpl apimRegistryService = Mockito.mock(APIMRegistryServiceImpl.class);
        PowerMockito.whenNew(APIMRegistryServiceImpl.class).withNoArguments().thenReturn(apimRegistryService);
        PowerMockito.when(apimRegistryService.getConfigRegistryResourceContent(tenantDomain, APIConstants
                .API_TENANT_CONF_LOCATION)).thenReturn(null);
        ServiceReferenceHolder serviceReferenceHolder = TestUtils.getServiceReferenceHolder();
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        APIManagerConfigurationService apiManagerConfigurationService = new APIManagerConfigurationServiceImpl
                (apiManagerConfiguration);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService()).thenReturn
                (apiManagerConfigurationService);

        //When configuration is not found in api-manager.xml
        try {
            Assert.assertFalse(APIUtil.isAllowDisplayAPIsWithMultipleStatus());
        } catch (APIManagementException e) {
            Assert.fail("Unexpected APIManagementException occurred while checking whether 'DisplayMultipleVersions' " +
                    "is enabled in tenant level");
        }

        //When configuration is found in api-manager.xml
        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.API_STORE_DISPLAY_ALL_APIS))
                .thenReturn("true");
        try {
            Assert.assertTrue(APIUtil.isAllowDisplayAPIsWithMultipleStatus());
        } catch (APIManagementException e) {
            Assert.fail("Unexpected APIManagementException occurred while checking whether 'DisplayMultipleVersions' " +
                    "is enabled in tenant level");
        }
    }

    @Test
    public void testIsAPIGatewayKeyCacheEnabled() {
        ServiceReferenceHolder serviceReferenceHolder = TestUtils.getServiceReferenceHolder();
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        APIManagerConfigurationService apiManagerConfigurationService = new APIManagerConfigurationServiceImpl
                (apiManagerConfiguration);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService()).thenReturn
                (apiManagerConfigurationService);
        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.GATEWAY_TOKEN_CACHE_ENABLED)).thenReturn
                ("true");
        Assert.assertTrue(APIUtil.isAPIGatewayKeyCacheEnabled());
    }

    @Test
    public void testIsValidWSDLURL() {
        String sampleHttpsURL = "https://mocked.test.wso2.org/sampleService?wsdl";
        String sampleHttpURL = "http://mocked.test.wso2.org/sampleService?wsdl";
        String sampleFileURL = "file:///home/wso2wsas/repository/mockedFile.wsdl";
        String sampleRegistryURL = "/registry/path/to/wsdl";
        String sampleInvalidURL = "invalid_https://mocked.test.wso2.org/sampleService?wsdl";

        Assert.assertTrue(APIUtil.isValidWSDLURL(sampleHttpsURL, true));
        Assert.assertEquals(false, APIUtil.isValidWSDLURL(sampleInvalidURL, false));
        Assert.assertTrue(APIUtil.isValidWSDLURL(sampleHttpURL, true));
        Assert.assertTrue(APIUtil.isValidWSDLURL(sampleFileURL, true));
        Assert.assertTrue(APIUtil.isValidWSDLURL(sampleRegistryURL, true));
        Assert.assertEquals(false, APIUtil.isValidWSDLURL(sampleInvalidURL, true));
        Assert.assertEquals(false, APIUtil.isValidWSDLURL(null, false));
    }

    @Test
    public void testLoadTenantConfig() throws Exception {
        String tenantDomain = "sample.com";

        ConfigurationContextService mockedConfigurationContextService = Mockito.mock(ConfigurationContextService.class);
        ConfigurationContext mockedConfigurationContext = Mockito.mock(ConfigurationContext.class);
        ThreadFactory mockedThreadFactory = new ThreadPool();
        Mockito.when(mockedConfigurationContext.getThreadPool()).thenReturn(mockedThreadFactory);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.when(ServiceReferenceHolder.getContextService()).thenReturn(mockedConfigurationContextService);
        Mockito.when(mockedConfigurationContextService.getServerConfigContext()).thenReturn(mockedConfigurationContext);
        APIUtil.loadTenantConfig(tenantDomain);
    }

    @Test
    public void testLoadTenantConfigBlockingMode() throws Exception {
        String tenantDomain = "sample.com";

        ConfigurationContextService mockedConfigurationContextService = Mockito.mock(ConfigurationContextService.class);
        ConfigurationContext mockedConfigurationContext = Mockito.mock(ConfigurationContext.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.when(ServiceReferenceHolder.getContextService()).thenReturn(mockedConfigurationContextService);
        Mockito.when(mockedConfigurationContextService.getServerConfigContext()).thenReturn(mockedConfigurationContext);
        APIUtil.loadTenantConfigBlockingMode(tenantDomain);

        PowerMockito.mockStatic(TenantAxisUtils.class);
        AxisConfiguration mockedAxisConfiguration = Mockito.mock(AxisConfiguration.class);
        PowerMockito.when(TenantAxisUtils.getTenantAxisConfiguration(tenantDomain, mockedConfigurationContext))
                .thenReturn(mockedAxisConfiguration);
        APIUtil.loadTenantConfigBlockingMode(tenantDomain);
        PowerMockito.verifyStatic(TenantAxisUtils.class, Mockito.atLeastOnce());
    }

    @Test
    public void testExtractCustomerKeyFromAuthHeader() throws Exception {
        Map sampleHeadersMap = new HashMap();
        String extractedCustomerKeyFromAuthHeader = APIUtil.extractCustomerKeyFromAuthHeader(sampleHeadersMap);
        Assert.assertNull(extractedCustomerKeyFromAuthHeader);
        String bearerToken = UUID.randomUUID().toString();
        String lowerCaseHeader = "oauth realm=\"Example\",\n" + "    Bearer \"" + bearerToken + "\",\n"
                + "    oauth_token=\"ad180jjd733klru7\",\n" + "    oauth_signature_method=\"HMAC-SHA1\",\n"
                + "    oauth_signature=\"wOJIO9A2W5mFwDgiDvZbTSMK%2FPY%3D\",\n" + "    oauth_timestamp=\"137131200\",\n"
                + "    oauth_nonce=\"4572616e48616d6d65724c61686176\",\n" + "    oauth_version=\"1.0\"";
        String upperCaseHeader = "OAuth realm=\"Example\",\n" + "    oauth_consumer_key=\"0685bd9184jfhq22\",\n"
                + "    oauth_token=\"ad180jjd733klru7\",\n" + "    oauth_signature_method=\"HMAC-SHA1\",\n"
                + "    oauth_signature=\"wOJIO9A2W5mFwDgiDvZbTSMK%2FPY%3D\",\n" + "    oauth_timestamp=\"137131200\",\n"
                + "    oauth_nonce=\"4572616e48616d6d65724c61686176\",\n" + "    oauth_version=\"1.0\"";
        sampleHeadersMap.put(HttpHeaders.AUTHORIZATION, lowerCaseHeader);
        extractedCustomerKeyFromAuthHeader = APIUtil.extractCustomerKeyFromAuthHeader(sampleHeadersMap);
        Assert.assertEquals(bearerToken, extractedCustomerKeyFromAuthHeader);
        sampleHeadersMap.put(HttpHeaders.AUTHORIZATION, upperCaseHeader);
        extractedCustomerKeyFromAuthHeader = APIUtil.extractCustomerKeyFromAuthHeader(sampleHeadersMap);
        // Should Return `NULL` because `Bearer` attribute is not present
        Assert.assertNull(extractedCustomerKeyFromAuthHeader);
    }
}
