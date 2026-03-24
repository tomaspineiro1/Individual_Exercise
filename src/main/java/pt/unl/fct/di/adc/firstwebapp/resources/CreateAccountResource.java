package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.DatastoreOptions;

import pt.unl.fct.di.adc.firstwebapp.util.CreateAccountRequest;
import pt.unl.fct.di.adc.firstwebapp.util.RestResponse;
import java.util.HashMap;
import java.util.Map;



@Path("/createaccount")
public class CreateAccountResource {

    private static final String INVALID_INPUT = "INVALID_INPUT";
    private static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private static final String INVALID_INPUT_MSG = "Missing or invalid fields";
    private static final String USER_ALREADY_EXISTS_MSG = "Error in creating an account because the username already exists";
    private static final String INTERNAL_ERROR_MSG = "Error registering user";

    private static final Logger LOG = Logger.getLogger(CreateAccountResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private final Gson g = new Gson();


    public CreateAccountResource() {}	// Default constructor, nothing to do

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAccount(CreateAccountRequest request) {
        LOG.fine("Attempt to register user: " + request.input.username);

        if(!request.input.validRegistration())
            return Response.status(Response.Status.BAD_REQUEST).entity(g.toJson(new RestResponse(INVALID_INPUT, INVALID_INPUT_MSG)))
                    .build();

        try {
            Transaction txn = datastore.newTransaction();
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(request.input.username);
            Entity user = txn.get(userKey);

            if(user != null) {
                txn.rollback();
                return Response.status(Response.Status.CONFLICT).entity(g.toJson(new RestResponse(USER_ALREADY_EXISTS, USER_ALREADY_EXISTS_MSG)))
                        .build();
            }
            else {
                user = Entity.newBuilder(userKey)
                        .set("user_pwd", DigestUtils.sha512Hex(request.input.password))
                        .set("user_email", request.input.email)
                        .set("user_phone",   request.input.phone)
                        .set("user_address", request.input.address)
                        .set("user_role",    request.input.role)
                        .set("user_creation_time", Timestamp.now())
                        .build();
                txn.put(user);
                txn.commit();
                LOG.info("User registered " + request.input.username);
                return Response.ok().build();
            }
        } catch (Exception e) {
            LOG.severe("Error registering user: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(g.toJson(
                            new RestResponse(INTERNAL_ERROR, INTERNAL_ERROR_MSG)
                    ))
                    .build();
        }
        finally {
            // No need to rollback here, as we only have one transaction and it will be automatically rolled back if not committed.
        }
    }
}
