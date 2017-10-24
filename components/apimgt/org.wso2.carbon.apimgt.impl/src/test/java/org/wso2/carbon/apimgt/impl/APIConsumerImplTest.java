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

package org.wso2.carbon.apimgt.impl;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.Scope;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceReferenceHolder.class})
public class APIConsumerImplTest extends TestCase {

    private static final Log log = LogFactory.getLog(APIConsumerImplTest.class);

    public void testReadMonetizationConfigAnnonymously() {
        APIMRegistryService apimRegistryService = Mockito.mock(APIMRegistryService.class);

        String json = "{\n  EnableMonetization : true\n }";

        try {
            when(apimRegistryService.getConfigRegistryResourceContent("", "")).thenReturn(json);
            /* TODO: Need to mock out ApimgtDAO and usage of registry else where in order to test this
            APIConsumer apiConsumer = new UserAwareAPIConsumer("__wso2.am.anon__", apimRegistryService);

            boolean isEnabled = apiConsumer.isMonetizationEnabled("carbon.super");

            assertTrue("Expected true but returned " + isEnabled, isEnabled);

        } catch (APIManagementException e) {
            e.printStackTrace();
        */} catch (UserStoreException e) {
            e.printStackTrace();
        } catch (RegistryException e) {
            e.printStackTrace();
        }
    }

    /**
     * This test case is to test the URIs generated for tag thumbnails when Tag wise listing is enabled in store page.
     */
    public void testTagThumbnailURLGeneration() {
        // Check the URL for super tenant
        String tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        String thumbnailPath = "/apimgt/applicationdata/tags/wso2-group/thumbnail.png";
        String finalURL = APIUtil.getRegistryResourcePathForUI(APIConstants.
                        RegistryResourceTypesForUI.TAG_THUMBNAIL, tenantDomain,
                thumbnailPath);
        log.info("##### Generated Tag Thumbnail URL > " + finalURL);
        Assert.assertEquals("/registry/resource/_system/governance" + thumbnailPath, finalURL);

        // Check the URL for other tenants
        tenantDomain = "apimanager3155.com";
        finalURL = APIUtil.getRegistryResourcePathForUI(APIConstants.
                        RegistryResourceTypesForUI.TAG_THUMBNAIL, tenantDomain,
                thumbnailPath);
        log.info("##### Generated Tag Thumbnail URL > " + finalURL);
        Assert.assertEquals("/t/" + tenantDomain + "/registry/resource/_system/governance" + thumbnailPath, finalURL);
    }

    public void testGetAuthorizedScopeString() throws APIManagementException {
        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);
        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        APIManagerConfigurationService apiManagerConfigurationService = new APIManagerConfigurationServiceImpl(apiManagerConfiguration);
        PowerMockito.when(serviceReferenceHolder.getAPIManagerConfigurationService()).thenReturn
                (apiManagerConfigurationService);
        APIConsumerImpl apiConsumer = new APIConsumerImpl();

        //Test when duplicate scopes are in authorized scope list
        List<Scope> authorizedScopes = new ArrayList<Scope>();
        //Populate three identical scopes
        Scope scope1 = new Scope();
        scope1.setId(1);
        scope1.setKey("scope1");
        scope1.setName("scope1");
        scope1.setRoles("role1");
        Scope scope2 = new Scope();
        scope2.setId(1);
        scope2.setKey("scope1");
        scope2.setName("scope1");
        scope2.setRoles("role1");
        scope2.setRoles("role1");
        Scope scope3 = new Scope();
        scope3.setId(1);
        scope3.setKey("scope1");
        scope3.setName("scope1");
        scope3.setRoles("role1");
        authorizedScopes.add(scope1);
        authorizedScopes.add(scope2);
        authorizedScopes.add(scope3);
        Assert.assertEquals(apiConsumer.getAuthorizedScopeString(authorizedScopes), "scope1 ");

        //Test when empty set of authorized scope are in list
        List<Scope> emptyScopeList = new ArrayList<Scope>();
        //When authorized scopes are null, default scope should be returned
        Assert.assertEquals(apiConsumer.getAuthorizedScopeString(emptyScopeList), "default");

        //Test when null authorized scope list is provided
        List<Scope> nullScopeList = new ArrayList<Scope>();
        //When authorized scopes are null, default scope should be returned
        Assert.assertEquals(apiConsumer.getAuthorizedScopeString(nullScopeList), "default");
    }
}
