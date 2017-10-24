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
package org.wso2.carbon.apimgt.rest.api.util.interceptors;

import org.apache.commons.logging.LogFactory;
import org.apache.cxf.message.Message;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.api.APIConsumer;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.Subscriber;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.rest.api.util.utils.RestApiUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = { RestApiUtil.class, APIUtil.class, LogFactory.class })
public class PostAuthenticationInterceptorTest {

    @Test
    public void testHandleMessage() throws APIManagementException {
        PostAuthenticationInterceptor postAuthenticationInterceptor = new PostAuthenticationInterceptor();
        Message message = Mockito.mock(Message.class);

        PowerMockito.mockStatic(RestApiUtil.class);
        PowerMockito.mockStatic(APIUtil.class);
        PowerMockito.mockStatic(LogFactory.class);
        PowerMockito.when(RestApiUtil.getLoggedInUsername()).thenReturn("john");
        APIConsumer apiConsumer = Mockito.mock(APIConsumer.class);
        PowerMockito.when(RestApiUtil.getLoggedInUserConsumer()).thenReturn(apiConsumer);
        Mockito.when(apiConsumer.getSubscriber("john")).thenReturn(null);

        postAuthenticationInterceptor.handleMessage(message);
        Mockito.verify(apiConsumer, Mockito.times(1))
                .addSubscriber(Matchers.any(Subscriber.class), Matchers.anyString());

    }
}
