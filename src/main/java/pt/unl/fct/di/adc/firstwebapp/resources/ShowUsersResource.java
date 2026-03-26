package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.*;
import java.util.logging.Logger;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import pt.unl.fct.di.adc.firstwebapp.util.*;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;

import com.google.gson.Gson;

@Path("/showusers")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ShowUsersResource {

    private static final Logger LOG = Logger.getLogger(ShowUsersResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");

    private final Gson g = new Gson();

    public ShowUsersResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response showUsers(ShowUsersRequest request) {
        LOG.fine("ShowUsers attempt");


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


        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("User")
                .build();
        QueryResults<Entity> results = datastore.run(query);

        List<Map<String, String>> users = new ArrayList<>();
        results.forEachRemaining(user -> {
            Map<String, String> userMap = new HashMap<>();
            String username = user.getKey().getName();
            userMap.put("username", username);
            userMap.put("role",     user.getString("user_role"));
            users.add(userMap);
        });

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("users", users);

        return Response.ok(g.toJson(new RestResponse("success", dataMap))).build();
    }
}