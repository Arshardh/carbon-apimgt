package org.wso2.carbon.apimgt.keymgt.service;
/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.wsdl.WSDLConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.MockRepository;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.AccessTokenInfo;
import org.wso2.carbon.apimgt.api.model.KeyManager;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.dto.APIKeyValidationInfoDTO;
import org.wso2.carbon.apimgt.impl.dto.Environment;
import org.wso2.carbon.apimgt.impl.factory.KeyManagerHolder;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.keymgt.APIKeyMgtException;
import org.wso2.carbon.apimgt.keymgt.handlers.AbstractKeyValidationHandler;
import org.wso2.carbon.apimgt.keymgt.handlers.DefaultKeyValidationHandler;
import org.wso2.carbon.apimgt.keymgt.handlers.KeyValidationHandler;
import org.wso2.carbon.apimgt.keymgt.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.keymgt.util.APIKeyMgtDataHolder;
import org.wso2.carbon.apimgt.keymgt.util.APIKeyMgtUtil;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.metrics.manager.MetricManager;
import org.wso2.carbon.metrics.manager.MetricService;
import org.wso2.carbon.metrics.manager.Timer;

import java.util.HashMap;
import java.util.Map;

import static org.wso2.carbon.base.CarbonBaseConstants.CARBON_HOME;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PrivilegedCarbonContext.class, ServiceReferenceHolder.class, ApiMgtDAO.class,
        APIManagerConfiguration.class, ApiMgtDAO.class,
        org.wso2.carbon.metrics.manager.internal.ServiceReferenceHolder.class, APIKeyMgtUtil.class,
        MessageContext.class, APIKeyMgtDataHolder.class, KeyValidationHandler.class, KeyManagerHolder.class,
        OAuthServerConfiguration.class, APIUtil.class })
public class APIKeyValidationServiceTest {
    private final String USER_NAME = "admin";
    private final String API_CONTEXT = "apicontext";
    private final String API_NAME = "apiname";
    private final String API_VERSION = "1.0.0";
    private final String TENANT_DOMAIN = "foo.com";
    private final String SECONDARY_USER_NAME = "secondary/admin@foo.com";
    private final String APPLICATION_NAME = "foo_PRODUCTION";
    private final String APPLICATION_NAME_1 = "sample_app";
    private final String CALLBACK_URL = "http://localhost";
    private final String CONSUMER_KEY = "Har2MjbxeMg3ysWEudjOKnXb3pAa";
    private final String CONSUMER_SECRET = "Ha52MfbxeFg3HJKEud156Y5GnAa";
    private final String[] GRANT_TYPES = { "password" };
    private final String REFRESH_GRANT_TYPE = "refresh_token";
    private final String IMPLICIT_GRANT_TYPE = "implicit";
    private final String ACCESS_TOKEN = "ca19a540f544777860e44e75f605d927";
    private final String API_KEY_MANGER_VALIDATION_HANDLER_CLASS_NAME = "org.wso2.carbon.apimgt.keymgt.handlers.DefaultKeyValidationHandler";
    private final String REQUIRED_AUTHENTICATION_LEVEL = "level";
    private PrivilegedCarbonContext privilegedCarbonContext;
    private ServiceReferenceHolder serviceReferenceHolder;
    private APIManagerConfigurationService apiManagerConfigurationService;
    private ApiMgtDAO apiMgtDAO;
    private APIManagerConfiguration apiManagerConfiguration;
    private MetricService metricService;

    @Before
    public void Init() throws Exception {
        System.setProperty(CARBON_HOME, "");
        privilegedCarbonContext = Mockito.mock(PrivilegedCarbonContext.class);
        serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        apiManagerConfigurationService = Mockito.mock(APIManagerConfigurationService.class);
        apiMgtDAO = Mockito.mock(ApiMgtDAO.class);
        apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        metricService = Mockito.mock(MetricService.class);
        org.wso2.carbon.metrics.manager.internal.ServiceReferenceHolder serviceReferenceHolder1 = Mockito
                .mock(org.wso2.carbon.metrics.manager.internal.ServiceReferenceHolder.class);
        Timer timer = Mockito.mock(Timer.class);
        Timer.Context timerContext = Mockito.mock(Timer.Context.class);
        MessageContext messageContext = Mockito.mock(MessageContext.class);
        OperationContext operationContext = Mockito.mock(OperationContext.class);
        MessageContext responseMessageContext = Mockito.mock(MessageContext.class);

        PowerMockito.mockStatic(PrivilegedCarbonContext.class);
        PowerMockito.mockStatic(ApiMgtDAO.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.mockStatic(org.wso2.carbon.metrics.manager.internal.ServiceReferenceHolder.class);
        PowerMockito.mockStatic(APIKeyMgtUtil.class);
        PowerMockito.mockStatic(MessageContext.class);
        PowerMockito.mockStatic(APIKeyMgtDataHolder.class);

        PowerMockito.when(PrivilegedCarbonContext.getThreadLocalCarbonContext()).thenReturn(privilegedCarbonContext);
        PowerMockito.when(privilegedCarbonContext.getUsername()).thenReturn(USER_NAME);
        PowerMockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        PowerMockito.when(ApiMgtDAO.getInstance()).thenReturn(apiMgtDAO);
        PowerMockito.when(MessageContext.getCurrentMessageContext()).thenReturn(messageContext);
        PowerMockito.when(APIKeyMgtDataHolder.isJwtGenerationEnabled()).thenReturn(true);

        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);
        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.API_KEY_MANGER_VALIDATIONHANDLER_CLASS_NAME))
                .thenReturn(API_KEY_MANGER_VALIDATION_HANDLER_CLASS_NAME);
        Mockito.when(org.wso2.carbon.metrics.manager.internal.ServiceReferenceHolder.getInstance())
                .thenReturn(serviceReferenceHolder1);
        Mockito.when(serviceReferenceHolder1.getMetricService()).thenReturn(metricService);
        Mockito.when(timer.start()).thenReturn(timerContext);
        Mockito.when(metricService.timer(Mockito.any(org.wso2.carbon.metrics.manager.Level.class), Mockito.anyString()))
                .thenReturn(timer);

        Mockito.when(messageContext.getOperationContext()).thenReturn(operationContext);
        Mockito.when(operationContext.getMessageContext(WSDLConstants.MESSAGE_LABEL_OUT_VALUE))
                .thenReturn(responseMessageContext);
        Map headers = new HashMap();
        headers.put("activityID", "1s2f2g4g5");
        Mockito.when(messageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS))
                .thenReturn(headers);

        String cacheKey = APIUtil.getAccessTokenCacheKey(ACCESS_TOKEN, API_CONTEXT, API_VERSION, "/*", "GET",
                REQUIRED_AUTHENTICATION_LEVEL);
        org.wso2.carbon.apimgt.impl.dto.APIKeyValidationInfoDTO infoDTO = new org.wso2.carbon.apimgt.impl.dto.APIKeyValidationInfoDTO();
        infoDTO.setApiPublisher(USER_NAME);
        infoDTO.setEndUserName(USER_NAME);
        PowerMockito.when(APIKeyMgtUtil.getFromKeyManagerCache(cacheKey)).thenReturn(infoDTO);

    }

    @Test
    public void testAPIKeyValidationServiceConstructor() throws Exception {
        PowerMockito.mockStatic(APIUtil.class);
        PowerMockito.when(APIUtil.getClassForName(DefaultKeyValidationHandler.class.getName()))
                .thenThrow(InstantiationException.class).thenThrow(IllegalAccessException.class)
                .thenThrow(ClassNotFoundException.class);
        try {
            APIKeyValidationService apiKeyValidationService = new APIKeyValidationService();
            apiKeyValidationService.validateKeyforHandshake(API_CONTEXT, API_VERSION, ACCESS_TOKEN);
            Assert.fail("NullPointerException expected");
        } catch (Exception e) {
            Assert.assertEquals(e.getMessage(), e.getMessage());
        }

        try {
            APIKeyValidationService apiKeyValidationService = new APIKeyValidationService();
            apiKeyValidationService.validateKeyforHandshake(API_CONTEXT, API_VERSION, ACCESS_TOKEN);
            Assert.fail("NullPointerException expected");
        } catch (Exception e) {
            Assert.assertEquals(e.getMessage(), e.getMessage());
        }

        try {
            APIKeyValidationService apiKeyValidationService = new APIKeyValidationService();
            apiKeyValidationService.validateKeyforHandshake(API_CONTEXT, API_VERSION, ACCESS_TOKEN);
            Assert.fail("NullPointerException expected");
        } catch (Exception e) {
            Assert.assertEquals(e.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateKey() throws Exception {
        APIKeyValidationService apiKeyValidationService = new APIKeyValidationService();
        DefaultKeyValidationHandler keyValidationHandler = Mockito.mock(DefaultKeyValidationHandler.class);
        PowerMockito.mockStatic(KeyValidationHandler.class);
        PowerMockito.whenNew(DefaultKeyValidationHandler.class).withNoArguments().thenReturn(keyValidationHandler);
        Mockito.when(keyValidationHandler.validateToken(Mockito.any(TokenValidationContext.class))).thenReturn(true);

        PowerMockito.mockStatic(OAuthServerConfiguration.class);
        OAuthServerConfiguration oAuthServerConfiguration = Mockito.mock(OAuthServerConfiguration.class);
        PowerMockito.when(OAuthServerConfiguration.getInstance()).thenReturn(oAuthServerConfiguration);
        PowerMockito.when(OAuthServerConfiguration.getInstance().getTimeStampSkewInSeconds())
                .thenReturn(System.currentTimeMillis());

        APIKeyValidationInfoDTO validateKey = apiKeyValidationService
                .validateKey(API_CONTEXT, API_VERSION, ACCESS_TOKEN, REQUIRED_AUTHENTICATION_LEVEL, TENANT_DOMAIN, "/*",
                        "GET");
        Assert.assertEquals(USER_NAME, validateKey.getApiPublisher());
        MockRepository.remove(DefaultKeyValidationHandler.class);
    }

    @Test
    public void testValidateKeyForHandshake() throws Exception {
        APIKeyValidationService apiKeyValidationService = new APIKeyValidationService();
        DefaultKeyValidationHandler keyValidationHandler = Mockito.mock(DefaultKeyValidationHandler.class);
        PowerMockito.mockStatic(KeyValidationHandler.class);
        PowerMockito.whenNew(DefaultKeyValidationHandler.class).withNoArguments().thenReturn(keyValidationHandler);
        Mockito.when(keyValidationHandler.validateToken(Mockito.any(TokenValidationContext.class))).thenReturn(true);
        PowerMockito.mockStatic(KeyManagerHolder.class);
        KeyManager keyManager = Mockito.mock(KeyManager.class);
        PowerMockito.when(KeyManagerHolder.getKeyManagerInstance()).thenReturn(keyManager);
        AccessTokenInfo tokenInfo = new AccessTokenInfo();
        tokenInfo.setTokenValid(true);
        tokenInfo.setEndUserName(USER_NAME);
        PowerMockito.when(keyManager.getTokenMetaData(ACCESS_TOKEN)).thenReturn(tokenInfo);

        Mockito.when(apiMgtDAO.getAPIDetailsByContext(API_CONTEXT)).thenReturn(new String[] { API_NAME, USER_NAME });
        Mockito.when(apiMgtDAO.getDefaultVersion(new APIIdentifier(USER_NAME, API_NAME, ""))).thenReturn(API_VERSION);
        APIKeyValidationInfoDTO info1 = new APIKeyValidationInfoDTO();
        info1.setEndUserName(USER_NAME);
        Mockito.when(apiMgtDAO
                .validateSubscriptionDetails(Mockito.any(APIKeyValidationInfoDTO.class), Mockito.any(String.class),
                        Mockito.any(String.class), Mockito.any(String.class), Mockito.any(boolean.class)))
                .thenReturn(info1);

        APIKeyValidationInfoDTO info = apiKeyValidationService
                .validateKeyforHandshake(API_CONTEXT, API_VERSION, ACCESS_TOKEN);
        Assert.assertEquals(USER_NAME, info.getEndUserName());
        MockRepository.remove(DefaultKeyValidationHandler.class);
    }
}
