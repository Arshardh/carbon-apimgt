package org.wso2.carbon.apimgt.rest.api.store;

import org.wso2.carbon.apimgt.rest.api.store.dto.*;
import org.wso2.carbon.apimgt.rest.api.store.TiersApiService;
import org.wso2.carbon.apimgt.rest.api.store.factories.TiersApiServiceFactory;

import io.swagger.annotations.ApiParam;

import org.wso2.carbon.apimgt.rest.api.store.dto.TierListDTO;
import org.wso2.carbon.apimgt.rest.api.store.dto.ErrorDTO;
import org.wso2.carbon.apimgt.rest.api.store.dto.TierDTO;

import java.util.List;

import java.io.InputStream;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import javax.ws.rs.core.Response;
import javax.ws.rs.*;

@Path("/tiers")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@io.swagger.annotations.Api(value = "/tiers", description = "the tiers API")
public class TiersApi  {

   private final TiersApiService delegate = TiersApiServiceFactory.getTiersApi();

    @GET
    @Path("/{tierLevel}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "", notes = "Get available tiers", response = TierListDTO.class, responseContainer = "List")
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK. \nList of tiers returned."),
        
        @io.swagger.annotations.ApiResponse(code = 304, message = "Not Modified. \nEmpty body because the client has already the latest version of the requested resource."),
        
        @io.swagger.annotations.ApiResponse(code = 406, message = "Not Acceptable. \nThe requested media type is not supported") })

    public Response tiersTierLevelGet(@ApiParam(value = "List API or Application type tiers.",required=true, allowableValues="{values=[api, application]}" ) @PathParam("tierLevel") String tierLevel,
    @ApiParam(value = "Maximum size of resource array to return.", defaultValue="25") @QueryParam("limit") Integer limit,
    @ApiParam(value = "Starting point within the complete list of items qualified.", defaultValue="0") @QueryParam("offset") Integer offset,
    @ApiParam(value = "For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be \n  retirieved from."  )@HeaderParam("X-WSO2-Tenant") String xWSO2Tenant,
    @ApiParam(value = "Media types acceptable for the response. Default is JSON."  , defaultValue="JSON")@HeaderParam("Accept") String accept,
    @ApiParam(value = "Validator for conditional requests; based on the ETag of the formerly retrieved\nvariant of the resourec."  )@HeaderParam("If-None-Match") String ifNoneMatch)
    {
    return delegate.tiersTierLevelGet(tierLevel,limit,offset,xWSO2Tenant,accept,ifNoneMatch);
    }
    @GET
    @Path("/{tierLevel}/{tierName}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "", notes = "Get tier details", response = TierDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "OK. \nTier returned"),
        
        @io.swagger.annotations.ApiResponse(code = 304, message = "Not Modified. \nEmpty body because the client has already the latest version of the requested resource."),
        
        @io.swagger.annotations.ApiResponse(code = 404, message = "Not Found. \nRequested Tier does not exist."),
        
        @io.swagger.annotations.ApiResponse(code = 406, message = "Not Acceptable. \nThe requested media type is not supported.") })

    public Response tiersTierLevelTierNameGet(@ApiParam(value = "Tier name",required=true ) @PathParam("tierName") String tierName,
    @ApiParam(value = "List API or Application type tiers.",required=true, allowableValues="{values=[api, application]}" ) @PathParam("tierLevel") String tierLevel,
    @ApiParam(value = "For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be \n  retirieved from."  )@HeaderParam("X-WSO2-Tenant") String xWSO2Tenant,
    @ApiParam(value = "Media types acceptable for the response. Default is JSON."  , defaultValue="JSON")@HeaderParam("Accept") String accept,
    @ApiParam(value = "Validator for conditional requests; based on the ETag of the formerly retrieved\nvariant of the resourec."  )@HeaderParam("If-None-Match") String ifNoneMatch,
    @ApiParam(value = "Validator for conditional requests; based on Last Modified header of the \nformerly retrieved variant of the resource."  )@HeaderParam("If-Modified-Since") String ifModifiedSince)
    {
    return delegate.tiersTierLevelTierNameGet(tierName,tierLevel,xWSO2Tenant,accept,ifNoneMatch,ifModifiedSince);
    }
}

