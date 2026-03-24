package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.Produces;

import com.google.gson.Gson;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.DatastoreOptions;

import pt.unl.fct.di.adc.firstwebapp.util.CreateAccountRequest;
import pt.unl.fct.di.adc.firstwebapp.util.ErrorCodes;
import pt.unl.fct.di.adc.firstwebapp.util.RestResponse;
import java.util.HashMap;
import java.util.Map;



@Path("/createaccount")
@Produces(MediaType.APPLICATION_JSON)
public class CreateAccountResource {

    private static final Logger LOG = Logger.getLogger(CreateAccountResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private final Gson g = new Gson();


    public CreateAccountResource() {}	// Default constructor, nothing to do

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAccount(CreateAccountRequest request) {
        LOG.fine("Attempt to register user: " + request.input.username);

        if(!request.input.validRegistration())
            return Response.status(Response.Status.BAD_REQUEST).entity(g.toJson(new RestResponse(ErrorCodes.INVALID_INPUT, ErrorCodes.INVALID_INPUT_MSG)))
                    .build();

        Transaction txn = null;
        try {
            txn = datastore.newTransaction();
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(request.input.username);
            Entity user = txn.get(userKey);

            if(user != null) {
                txn.rollback();
                return Response.status(Response.Status.CONFLICT).entity(g.toJson(new RestResponse(ErrorCodes.USER_ALREADY_EXISTS, ErrorCodes.USER_ALREADY_EXISTS_MSG)))
                        .build();
            }
            else {
                user = Entity.newBuilder(userKey)
                        .set("user_pwd", DigestUtils.sha512Hex(request.input.password))
                        .set("user_phone",   request.input.phone)
                        .set("user_address", request.input.address)
                        .set("user_role",    request.input.role)
                        .set("user_creation_time", Timestamp.now())
                        .build();
                txn.put(user);
                txn.commit();
                LOG.info("User registered " + request.input.username);

                Map<String, String> responseData = new HashMap<>();
                responseData.put("username", request.input.username);
                responseData.put("role", request.input.role);

                return Response.ok(g.toJson(new RestResponse("success", responseData))).build();
            }
        } catch (Exception e) {
            LOG.severe("Error registering user: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(g.toJson(
                            new RestResponse(ErrorCodes.INTERNAL_ERROR, ErrorCodes.INTERNAL_ERROR_MSG)
                    ))
                    .build();
        }
        finally {
            if (txn != null && txn.isActive()) txn.rollback();
        }
    }
}
