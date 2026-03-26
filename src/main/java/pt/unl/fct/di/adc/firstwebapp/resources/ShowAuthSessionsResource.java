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
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;

import com.google.gson.Gson;

@Path("/showauthsessions")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowAuthSessionsResource {

    private static final Logger LOG = Logger.getLogger(ShowAuthSessionsResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");

    private final Gson g = new Gson();

    public ShowAuthSessionsResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showAuthSessions(ShowAuthSessionsRequest request) {
        LOG.fine("ShowAuthSessions attempt");

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
        if (!role.equals("ADMIN")) {
            return Response.ok(g.toJson(new RestResponse(ErrorCodes.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED_MSG)))
                    .build();
        }

        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("Token")
                .build();
        QueryResults<Entity> results = datastore.run(query);

        List<Map<String, Object>> sessions = new ArrayList<>();
        results.forEachRemaining(token -> {
            long tokenExpiresAt = token.getLong("expiresAt");
            if (System.currentTimeMillis() < tokenExpiresAt) {
                Map<String, Object> sessionMap = new HashMap<>();
                sessionMap.put("tokenId",   token.getString("tokenId"));
                sessionMap.put("username",  token.getString("username"));
                sessionMap.put("role",      token.getString("role"));
                sessionMap.put("expiresAt", tokenExpiresAt);
                sessions.add(sessionMap);
            }
        });

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("sessions", sessions);

        return Response.ok(g.toJson(new RestResponse("success", dataMap))).build();
    }
}