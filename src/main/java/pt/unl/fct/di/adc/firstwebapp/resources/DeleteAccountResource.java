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
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;

import com.google.gson.Gson;

@Path("/deleteaccount")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class DeleteAccountResource {

    private static final Logger LOG = Logger.getLogger(DeleteAccountResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");

    private final Gson g = new Gson();

    public DeleteAccountResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteAccount(DeleteAccountRequest request) {
        LOG.fine("DeleteAccount attempt");


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


        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = userKeyFactory.newKey(request.input.username);
            Entity user = txn.get(userKey);

            if (user == null) {
                txn.rollback();
                return Response.ok(g.toJson(new RestResponse(ErrorCodes.USER_NOT_FOUND, ErrorCodes.USER_NOT_FOUND_MSG)))
                        .build();
            }


            Query<Entity> tokenQuery = Query.newEntityQueryBuilder()
                    .setKind("Token")
                    .build();
            QueryResults<Entity> tokens = txn.run(tokenQuery);
            tokens.forEachRemaining(token -> {
                if (token.getString("username").equals(request.input.username)) {
                    txn.delete(token.getKey());
                }
            });

            txn.delete(userKey);
            txn.commit();

            LOG.info("Account deleted: " + request.input.username);

            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("message", "Account deleted successfully");

            return Response.ok(g.toJson(new RestResponse("success", dataMap))).build();

        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Error deleting account: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(g.toJson(new RestResponse(ErrorCodes.INTERNAL_ERROR, ErrorCodes.INTERNAL_ERROR_MSG)))
                    .build();
        } finally {
            if (txn.isActive()) txn.rollback();
        }
    }
}