/*
 *  Copyright WSO2 Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.apimgt.gateway.handlers.security;


import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.gateway.APIMgtGatewayConstants;
import org.wso2.carbon.apimgt.gateway.handlers.security.keys.APIKeyDataStore;
import org.wso2.carbon.apimgt.gateway.handlers.security.keys.APIKeyValidatorClientPool;
import org.wso2.carbon.apimgt.gateway.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.dto.APIKeyValidationInfoDTO;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.caching.impl.Util;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = { Caching.class, Util.class, CacheManager.class, ServiceReferenceHolder.class,
        ServerConfiguration.class, APIKeyValidatorClientPool.class })
public class TestAPIKeyValidator {   //extends APIKeyValidator

    /*
 * Test method fpr getKeyValidationInfo()
 * */
    @Test
    public void testGetKeyValidationInfo() throws Exception {
        String context = "/";
        String apiKey = "abc";
        String apiVersion = "1.0";
        String authenticationScheme = "";
        String clientDomain = "abc.com";
        String matchingResource = "/menu";
        String httpVerb = "get";
        boolean defaultVersionInvoked = true;
        AxisConfiguration axisConfiguration = new AxisConfiguration();

        PowerMockito.mockStatic(Caching.class);
        PowerMockito.mockStatic(CacheManager.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.mockStatic(ServerConfiguration.class);
        PowerMockito.mockStatic(APIKeyValidatorClientPool.class);
        PowerMockito.mockStatic(LogFactory.class);

        Cache cache = Mockito.mock(Cache.class);
        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = Mockito.mock(APIKeyValidationInfoDTO.class);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        ServerConfiguration serverConfiguration = Mockito.mock(ServerConfiguration.class);
        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        APIKeyValidatorClientPool apiKeyValidatorClientPool = Mockito.mock(APIKeyValidatorClientPool.class);
        apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Cache tokenCache = Mockito.mock(Cache.class);
        Cache keyCache = Mockito.mock(Cache.class);
        Cache resourceCache = Mockito.mock(Cache.class);
        APIKeyDataStore apiKeyDataStore = Mockito.mock(APIKeyDataStore.class);
        Cache invalidTokenCache = Mockito.mock(Cache.class);

        PowerMockito.when(Caching.getCacheManager(APIConstants.API_MANAGER_CACHE_MANAGER)).thenReturn(cacheManager);
        Mockito.when(cacheManager.getCache(Mockito.anyString())).thenReturn(cache);

        PowerMockito.when(APIKeyValidatorClientPool.getInstance()).thenReturn(apiKeyValidatorClientPool);
        PowerMockito.when(ServerConfiguration.getInstance()).thenReturn(serverConfiguration);
        PowerMockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);
        cacheManager.getCache(APIConstants.GATEWAY_KEY_CACHE_NAME);
        Mockito.when(tokenCache.get(Mockito.anyString())).thenReturn(null);


        APIKeyValidator apiKeyValidator = getAPIKeyValidator(axisConfiguration, invalidTokenCache,
                tokenCache, keyCache, resourceCache, apiKeyDataStore, MultitenantConstants
                        .SUPER_TENANT_DOMAIN_NAME, apiKeyValidationInfoDTO);
        Mockito.when(tokenCache.get(Mockito.anyString())).thenReturn(null);
        Mockito.when(invalidTokenCache.get(Mockito.anyString())).thenReturn(null);
        Mockito.when(apiKeyDataStore.getAPIKeyData(context, apiVersion, apiKey, authenticationScheme,
                clientDomain, matchingResource, httpVerb)).thenReturn(apiKeyValidationInfoDTO);
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO1 = apiKeyValidator.getKeyValidationInfo(context, apiKey,
                apiVersion, authenticationScheme, clientDomain,
                matchingResource, httpVerb, defaultVersionInvoked);
        /*If the system property CLIENT_DOMAIN_TOKEN_CACHING_ENABLED is not set api should not included in gatewaycache
        to meet backword compatibility*/
        Assert.assertFalse(apiKeyValidator.getGatewayKeyCache().containsKey(apiKey));

    }

    private APIKeyValidator getAPIKeyValidator(AxisConfiguration axisConfig, final Cache invalidTokenCache, final Cache
            tokenCache, final Cache keyCache, final Cache resourceCache, final APIKeyDataStore apiKeyDataStore,
                                               final String tenantDomain, final APIKeyValidationInfoDTO apiKeyValidationInfoDTO) {
        APIKeyValidator apiKeyValidator = new APIKeyValidator(axisConfig) {

            @Override
            protected Cache getGatewayKeyCache() {
                return keyCache;
            }


            @Override
            protected Cache getGatewayTokenCache() {
                return tokenCache;
            }

            @Override
            protected Cache getResourceCache() {
                return resourceCache;
            }

            @Override
            protected APIKeyValidationInfoDTO doGetKeyValidationInfo(String context, String apiVersion, String
                    apiKey, String authenticationScheme, String clientDomain, String matchingResource, String
                                                                             httpVerb) throws APISecurityException {
                return apiKeyValidationInfoDTO;
            }

            @Override
            public boolean isAPIKeyValidationEnabled() {
                return true;
            }

            @Override
            public void cleanup() {
            }

            @Override
            public boolean isAPIResourceValidationEnabled() {
                return true;
            }
        };
        apiKeyValidator.dataStore = apiKeyDataStore;
        return apiKeyValidator;
    }
    /*
    private int counter = 0;
    private Map<String,APIKeyValidationInfoDTO> userInfo = new HashMap<String, APIKeyValidationInfoDTO>();

    public TestAPIKeyValidator() {
        super(new AxisConfiguration());
    }

    @Override
    protected Cache initCache() {
        return new SimpleCache();
    }

    @Override
    protected APIKeyValidationInfoDTO doGetKeyValidationInfo(String context, String apiVersion, 
                                                             String apiKey) throws APISecurityException {
        counter++;
        String key = getKey(context, apiVersion, apiKey);
        if (userInfo.containsKey(key)) {
            return userInfo.get(key);
        }
        APIKeyValidationInfoDTO info = new APIKeyValidationInfoDTO();
        info.setAuthorized(false);
        return info;
    }

    public void addUserInfo(String context, String apiVersion, 
                            String apiKey, APIKeyValidationInfoDTO info) {
        String key = getKey(context, apiVersion, apiKey);
        userInfo.put(key, info);
    }

    private String getKey(String context, String apiVersion, String apiKey) {
        return "{" + context + ":" + apiVersion + ":" + apiKey + "}";
    }

    public int getCounter() {
        return counter;
    }
    
    private static class SimpleCache implements Cache {
        
        private Map<Object,Object> map = new LRUCache<Object, Object>(APISecurityConstants.DEFAULT_MAX_INVALID_KEYS);
        
        public boolean containsKey(Object o) {
            return map.containsKey(o);
        }

        public boolean containsValue(Object o) {
            return map.containsValue(o);
        }

        public Set entrySet() {
            return map.entrySet();
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public Set keySet() {
            return map.keySet();
        }

        public void putAll(Map map) {
            map.putAll(map);
        }

        public int size() {
            return map.size();
        }

        public Collection values() {
            return map.values();
        }

        public Object get(Object o) {
            return map.get(o);
        }

        public Map getAll(Collection collection) throws CacheException {
            return null;
        }

        public void load(Object o) throws CacheException {

        }

        public void loadAll(Collection collection) throws CacheException {

        }

        public Object peek(Object o) {
            return map.get(o);
        }

        public Object put(Object o, Object o1) {
            return map.put(o, o1);
        }

        public CacheEntry getCacheEntry(Object o) {
            return null;
        }

        public CacheStatistics getCacheStatistics() {
            return null;
        }

        public Object remove(Object o) {
            return map.remove(o);
        }

        public void clear() {
            map.clear();
        }

        public void evict() {

        }

        public void addListener(CacheListener cacheListener) {

        }

        public void removeListener(CacheListener cacheListener) {

        }
    }
    */
}
