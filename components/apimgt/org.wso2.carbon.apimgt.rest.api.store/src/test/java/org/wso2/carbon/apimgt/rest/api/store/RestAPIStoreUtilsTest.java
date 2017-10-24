/*
* Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.wso2.carbon.apimgt.rest.api.store;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.api.model.Application;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.rest.api.store.utils.RestAPIStoreUtils;
import org.wso2.carbon.context.CarbonContext;

import static org.junit.Assert.assertEquals;
import static org.wso2.carbon.base.CarbonBaseConstants.CARBON_HOME;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CarbonContext.class, ServiceReferenceHolder.class})
public class RestAPIStoreUtilsTest {

    @Test
    public void isUserAccessAllowedForApplication() {
        Application application = new Application(2);
        application.getSubscriber().setName("admin");
        System.setProperty(CARBON_HOME, "");
        CarbonContext carbonContext = Mockito.mock(CarbonContext.class);
        PowerMockito.mockStatic(CarbonContext.class);

        PowerMockito.when(CarbonContext.getThreadLocalCarbonContext()).thenReturn(carbonContext);
        Mockito.when(carbonContext.getUsername()).thenReturn("admin");
        assertEquals(true, RestAPIStoreUtils.isUserAccessAllowedForApplication(application));
    }

    @Test public void isUserAccessAllowedForApplicationForDifferentUsername() {
        Application application = new Application(2);
        application.getSubscriber().setName("Admin");
        System.setProperty(CARBON_HOME, "");
        CarbonContext carbonContext = Mockito.mock(CarbonContext.class);
        PowerMockito.mockStatic(CarbonContext.class);

        PowerMockito.when(CarbonContext.getThreadLocalCarbonContext()).thenReturn(carbonContext);
        Mockito.when(carbonContext.getUsername()).thenReturn("admin");

        ServiceReferenceHolder serviceReferenceHolder = Mockito.mock(ServiceReferenceHolder.class);
        PowerMockito.mockStatic(ServiceReferenceHolder.class);
        PowerMockito.when(ServiceReferenceHolder.getInstance()).thenReturn(serviceReferenceHolder);

        APIManagerConfigurationService apiManagerConfigurationService = Mockito
                .mock(APIManagerConfigurationService.class);
        Mockito.when(serviceReferenceHolder.getAPIManagerConfigurationService())
                .thenReturn(apiManagerConfigurationService);

        APIManagerConfiguration apiManagerConfiguration = Mockito.mock(APIManagerConfiguration.class);
        Mockito.when(apiManagerConfigurationService.getAPIManagerConfiguration()).thenReturn(apiManagerConfiguration);

        Mockito.when(apiManagerConfiguration.getFirstProperty(APIConstants.API_STORE_FORCE_CI_COMPARISIONS))
                .thenReturn("true");

        assertEquals(true, RestAPIStoreUtils.isUserAccessAllowedForApplication(application));
    }
}
