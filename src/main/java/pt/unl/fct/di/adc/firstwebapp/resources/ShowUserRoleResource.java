package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.*;
import java.util.logging.Logger;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.util.*;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;

import com.google.gson.Gson;

@Path("/showuserrole")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowUserRoleResource {

    private static final Logger LOG = Logger.getLogger(ShowUserRoleResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");

    private final Gson g = new Gson();

    public ShowUserRoleResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showUserRole(ShowUserRoleRequest request) {
        LOG.fine("ShowUserRole attempt");

        
        if (request.input == null || request.input.username == null || request.input.username.isBlank()) {
            return Response.ok(g.toJson(new RestResponse(ErrorCodes.INVALID_INPUT, ErrorCodes.INVALID_INPUT_MSG)))
                    .build();
        }


        if (request.token == null || request.token.tokenID == null || request.token.tokenID.isBlank()) {
            return Response.ok(g.toJson(new RestResponse(ErrorCodes.INVALID_TOKEN, ErrorCodes.INVALID_TOKEN_MSG)))
                    .build();
        }

        Key tokenKey = tokenKeyFactory.newKey(request.token.tokenID);
        Entity tokenEntity = datastore.get(tokenKey);

        if (tokenEntity == null) {
            return Response.ok(g.toJson(new RestResponse(ErrorCodes.INVALID_TOKEN, ErrorCodes.INVALID_TOKEN_MSG)))
                    .build();
        }

        long expiresAt = tokenEntity.getLong("expiresAt");
        if (System.currentTimeMillis() > expiresAt) {
            return Response.ok(g.toJson(new RestResponse(ErrorCodes.TOKEN_EXPIRED, ErrorCodes.TOKEN_EXPIRED_MSG)))
                    .build();
        }


        String role = tokenEntity.getString("role");
        if (!role.equals("ADMIN") && !role.equals("BOFFICER")) {
            return Response.ok(g.toJson(new RestResponse(ErrorCodes.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED_MSG)))
                    .build();
        }


        Key userKey = userKeyFactory.newKey(request.input.username);
        Entity user = datastore.get(userKey);

        if (user == null) {
            return Response.ok(g.toJson(new RestResponse(ErrorCodes.USER_NOT_FOUND, ErrorCodes.USER_NOT_FOUND_MSG)))
                    .build();
        }

        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("username", request.input.username);
        dataMap.put("role", user.getString("user_role"));

        return Response.ok(g.toJson(new RestResponse("success", dataMap))).build();
    }
}