/*
 * Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.gateway.handlers;

import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.synapse.AbstractSynapseHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.wso2.carbon.apimgt.gateway.APIMgtGatewayConstants;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLStreamException;

public class LogsHandler extends AbstractSynapseHandler {
    private static final Log log = LogFactory.getLog("timing");
    private String apiName = null;
    private String apiCTX = null;
    private String apiMethod = null;
    private String apiTo = null;
    private long requestSize = 0;
    private String apiElectedRsrc = null;
    private String apiRestReqFullPath = null;
    private String apiResponseSC = null;
    private String apiMsgUUID = null;
    private String apiRsrcCacheKey = null;
    private String applicationName = null;
    private String apiConsumerKey = null;

    private boolean isEnabled() {
        boolean enabled = false;
        String config = System.getProperty("enableCorrelationLogs");
        if (config != null && !config.equals("")) {
            enabled = Boolean.parseBoolean(config);
        }
        return enabled;
    }

    public boolean handleRequestInFlow(MessageContext messageContext) {
        if (isEnabled()) {
            try {
                apiTo = LogUtils.getTo(messageContext);
                return true;
            } catch (Exception e) {
                log.error("Cannot publish request event. " + e.getMessage(), e);
            }
            return false;
        }
        return true;
    }

    public boolean handleRequestOutFlow(MessageContext messageContext) {
        if (isEnabled()) {
            try {
                requestSize = buildRequestMessage(messageContext);
                Map headers = LogUtils.getTransportHeaders(messageContext);
                Set<String> key = headers.keySet();

                String authHeader = LogUtils.getAuthorizationHeader(headers);
                String orgIdHeader = LogUtils.getOrganizationIdHeader(headers);
                String SrcIdHeader = LogUtils.getSourceIdHeader(headers);
                String applIdHeader = LogUtils.getApplicationIdHeader(headers);
                String uuIdHeader = LogUtils.getUuidHeader(headers);
                messageContext.setProperty("AUTH_HEADER", authHeader);
                messageContext.setProperty("ORG_ID_HEADER", orgIdHeader);
                messageContext.setProperty("SRC_ID_HEADER", SrcIdHeader);
                messageContext.setProperty("APP_ID_HEADER", applIdHeader);
                messageContext.setProperty("UUID_HEADER", uuIdHeader);
                apiName = LogUtils.getAPIName(messageContext);
                apiCTX = LogUtils.getAPICtx(messageContext);
                apiMethod = LogUtils.getRestMethod(messageContext);
                // apiTo = LogUtils.getTo(messageContext);
                apiElectedRsrc = LogUtils.getElectedResource(messageContext);
                apiRestReqFullPath = LogUtils.getRestReqFullPath(messageContext);
                apiMsgUUID = (String) messageContext.getMessageID();
                apiRsrcCacheKey = LogUtils.getResourceCacheKey(messageContext);
                return true;
            } catch (Exception e) {
                log.error("Cannot publish request event. " + e.getMessage(), e);
            }
            return false;
        }
        return true;
    }

    public boolean handleResponseInFlow(MessageContext messageContext) {
        if (isEnabled()) {
            // default API would have the property LoggedResponse as true.
            String defaultAPI = (String) messageContext.getProperty("DefaultAPI");
            messageContext.setProperty("END_POINT_FLAG", "true");
            if ("true".equals(defaultAPI)) {
                return true;
            } else {
                try {
                    long responseTime = getResponseTime(messageContext);
                    long beTotalLatency = getBackendLatency(messageContext);
                    long responseSize = buildResponseMessage(messageContext);
                    apiResponseSC = LogUtils.getRestHttpResponseStatusCode(messageContext);
                    applicationName = LogUtils.getApplicationName(messageContext);
                    apiConsumerKey = LogUtils.getConsumerKey(messageContext);
                    String authHeader = (String) messageContext.getProperty("AUTH_HEADER");
                    String orgIdHeader = (String) messageContext.getProperty("ORG_ID_HEADER");
                    String SrcIdHeader = (String) messageContext.getProperty("SRC_ID_HEADER");
                    String applIdHeader = (String) messageContext.getProperty("APP_ID_HEADER");
                    String uuIdHeader = (String) messageContext.getProperty("UUID_HEADER");
//				log.info("Appl & Consumer RES-IN --- "+applicationName+"  "+apiConsumerKey);
                    /*messageContext.setProperty("END_POINT_FLAG", "true");*/
                    /*
                     * 1. Back-end Latency in milliseconds
                     * 2. API Name
                     * 3. API URI (HTTP method, Context+version+resource path)
                     * 4. API resource path
                     * 5. Authorization
                     * 6. Org-Id
                     * 7. Source-Id
                     * 8. Application-Id
                     * 9. UUID
                     * 10. Request Size in bytes
                     * 11. Response Size in bytes
                     * 12. Response Time in milliseconds
                     * 13. HTTP Response Status Code
                     */
                    log.info(beTotalLatency + "|HTTP|" + apiName + "|" + apiMethod + "|" + apiCTX + apiElectedRsrc + "|"
                            + apiTo + "|" + authHeader + "|" + orgIdHeader + "|" + SrcIdHeader
                            + "|" + applIdHeader + "|" + uuIdHeader + "|" + requestSize
                            + "|" + responseSize + "|" + apiResponseSC + "|"
                            + applicationName + "|" + apiConsumerKey + "|" + responseTime);
                    return true;
                } catch (Exception e) {
                    log.error("Cannot publish response event. " + e.getMessage(), e);
                }
            }
            return false;
        }
        return true;
    }

    public boolean handleResponseOutFlow(MessageContext messageContext) {
        if (isEnabled()) {
            String ep_flag = (String) messageContext.getProperty("END_POINT_FLAG");
            if (!("true").equals(ep_flag)) {
                apiResponseSC = LogUtils.getRestHttpResponseStatusCode(messageContext);
                try {
                    Map headers = LogUtils.getTransportHeaders(messageContext);
                    Set<String> key = headers.keySet();

                    String authHeader = LogUtils.getAuthorizationHeader(headers);
                    String orgIdHeader = LogUtils.getOrganizationIdHeader(headers);
                    String SrcIdHeader = LogUtils.getSourceIdHeader(headers);
                    String applIdHeader = LogUtils.getApplicationIdHeader(headers);
                    String uuIdHeader = LogUtils.getUuidHeader(headers);
                    apiName = LogUtils.getAPIName(messageContext);
                    apiCTX = LogUtils.getAPICtx(messageContext);
                    apiMethod = LogUtils.getRestMethod(messageContext);
                    apiTo = LogUtils.getTo(messageContext);
                    apiElectedRsrc = LogUtils.getElectedResource(messageContext);
                    apiRestReqFullPath = LogUtils.getRestReqFullPath(messageContext);
                    apiMsgUUID = (String) messageContext.getMessageID();
                    applicationName = LogUtils.getApplicationName(messageContext);
                    apiConsumerKey = LogUtils.getConsumerKey(messageContext);
                    log.info("" + "|HTTP|" + apiName + "|" + apiMethod + "|" + apiCTX + apiElectedRsrc + "|"
                            + apiTo + "|" + authHeader + "|" + orgIdHeader + "|" + SrcIdHeader
                            + "|" + applIdHeader + "|" + uuIdHeader + "|" + requestSize
                            + "|" + "" + "|" + apiResponseSC + "|"
                            + applicationName + "|" + apiConsumerKey + "|" + "");
                    return true;
                } catch (Exception e) {
                    log.error("Cannot publish request event. " + e.getMessage(), e);
                }
            } else {
                // log.info("Request sent to Backend.");
                return true;
            }
        }
        return true;
    }

    /*
     * getBackendLatency
     */
    private long getBackendLatency(org.apache.synapse.MessageContext messageContext) {
        long beTotalLatency = 0;
        long beStartTime = 0;
        long beEndTime = 0;
        long executionStartTime = 0;
        try {
            if (messageContext.getProperty(APIMgtGatewayConstants.BACKEND_REQUEST_END_TIME) == null) {
                if (messageContext.getProperty(APIMgtGatewayConstants.BACKEND_REQUEST_START_TIME) != null) {
                    executionStartTime = Long.parseLong(
                            (String) messageContext.getProperty(APIMgtGatewayConstants.BACKEND_REQUEST_START_TIME));
                }
                messageContext.setProperty(APIMgtGatewayConstants.BACKEND_LATENCY,
                        System.currentTimeMillis() - executionStartTime);
                messageContext.setProperty(APIMgtGatewayConstants.BACKEND_REQUEST_END_TIME, System.currentTimeMillis());
            }
            if (messageContext.getProperty(APIMgtGatewayConstants.BACKEND_REQUEST_START_TIME) != null) {
                beStartTime = Long.parseLong(
                        (String) messageContext.getProperty(APIMgtGatewayConstants.BACKEND_REQUEST_START_TIME));
            }
            if (messageContext.getProperty(APIMgtGatewayConstants.BACKEND_REQUEST_END_TIME) != null) {
                beEndTime = (Long) messageContext.getProperty(APIMgtGatewayConstants.BACKEND_REQUEST_END_TIME);
            }

            beTotalLatency = beEndTime - beStartTime;

        } catch (Exception e) {
            log.error("Error getBackendLatency -  " + e.getMessage(), e);
        }
        return beTotalLatency;
    }

    /*
     * getResponseTime
     */
    private long getResponseTime(org.apache.synapse.MessageContext messageContext) {
        long responseTime = 0;
        try {
            long rtStartTime = 0;
            if (messageContext.getProperty(APIMgtGatewayConstants.REQUEST_EXECUTION_START_TIME) != null) {
                Object objRtStartTime = messageContext.getProperty(APIMgtGatewayConstants.REQUEST_EXECUTION_START_TIME);
                rtStartTime = (objRtStartTime == null ? 0 : Long.parseLong((String) objRtStartTime));
            }
            responseTime = System.currentTimeMillis() - rtStartTime;
        } catch (Exception e) {
            log.error("Error getResponseTime -  " + e.getMessage(), e);
        }
        return responseTime;
    }

    /*
     * private long calculateMessageSize(org.apache.synapse.MessageContext
     * messageContext) { long requestSize = 0;
     * org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext)
     * messageContext) .getAxis2MessageContext(); try {
     * RelayUtils.buildMessage(axis2MC); } catch (IOException ex) { // In case of an
     * exception, it won't be propagated up,and set response size to 0
     * log.error("Error occurred while building the message to" +
     * " calculate the request body size", ex); } catch (XMLStreamException ex) {
     * log.
     * error("Error occurred while building the message to calculate the request" +
     * " body size", ex); }
     * if(axis2MC.getProperty(Constants.Configuration.CONTENT_TYPE).equals(
     * "application/json")){ InputStream jsonPaylodStream = (InputStream)
     * ((Axis2MessageContext) messageContext) .getAxis2MessageContext().getProperty(
     * "org.apache.synapse.commons.json.JsonInputStream") }else{ // retrive payload
     * from SoapPayload then do the calulation SOAPEnvelope env =
     * messageContext.getEnvelope(); if (env != null) { SOAPBody soapbody =
     * env.getBody(); if (soapbody != null) { byte[] size =
     * soapbody.toString().getBytes(Charset.defaultCharset()); requestSize =
     * size.length; } } return requestSize;
     *
     * }
     */

    private long buildRequestMessage(org.apache.synapse.MessageContext messageContext) {
        long requestSize = 0;
        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext();
        Map headers = (Map) axis2MC.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        String contentLength = (String) headers.get(HttpHeaders.CONTENT_LENGTH);
        if (contentLength != null) {
            requestSize = Integer.parseInt(contentLength);
        } else { // When chunking is enabled
            try {
                RelayUtils.buildMessage(axis2MC);
            } catch (IOException ex) {
                // In case of an exception, it won't be propagated up,and set response size to 0
                log.error("Error occurred while building the message to" + " calculate the request body size", ex);
            } catch (XMLStreamException ex) {
                log.error("Error occurred while building the message to calculate the request" + " body size", ex);
            }
            SOAPEnvelope env = messageContext.getEnvelope();
            if (env != null) {
                SOAPBody soapbody = env.getBody();
                if (soapbody != null) {
                    byte[] size = soapbody.toString().getBytes(Charset.defaultCharset());
                    requestSize = size.length;
                }

            }
        }
        return requestSize;
    }

    private long buildResponseMessage(org.apache.synapse.MessageContext messageContext) {
        long responseSize = 0;
        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext();
        Map headers = (Map) axis2MC.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        String contentLength = (String) headers.get(HttpHeaders.CONTENT_LENGTH);
        if (contentLength != null) {
            responseSize = Integer.parseInt(contentLength);
        } else { // When chunking is enabled
            try {
                RelayUtils.buildMessage(axis2MC);
            } catch (IOException ex) {
                // In case of an exception, it won't be propagated up,and set response size to 0
                log.error("Error occurred while building the message to" + " calculate the response body size", ex);
            } catch (XMLStreamException ex) {
                log.error("Error occurred while building the message to calculate the response" + " body size", ex);
            }
        }
        SOAPEnvelope env = messageContext.getEnvelope();
        if (env != null) {
            SOAPBody soapbody = env.getBody();
            if (soapbody != null) {
                byte[] size = soapbody.toString().getBytes(Charset.defaultCharset());
                responseSize = size.length;
            }

        }
        return responseSize;

    }
}