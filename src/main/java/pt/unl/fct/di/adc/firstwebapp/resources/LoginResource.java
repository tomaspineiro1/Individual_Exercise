package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;

import jakarta.servlet.http.HttpServletRequest;

import pt.unl.fct.di.adc.firstwebapp.util.*;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;

import com.google.gson.Gson;


@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	/** 
	 * Logger Object
	 */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");

	private final Gson g = new Gson();
	
	public LoginResource() {} // Nothing to be done here


	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response doLogin(LoginRequestData request) {
		LOG.fine("Login attempt: " + request.input.username);

		if (request.input == null ||
				request.input.username == null || request.input.username.isBlank() ||
				request.input.password == null || request.input.password.isBlank()) {
			return Response.ok(g.toJson(new RestResponse(ErrorCodes.INVALID_CREDENTIALS, ErrorCodes.INVALID_CREDENTIALS_MSG)))
					.build();
		}


		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = userKeyFactory.newKey(request.input.username);
			Entity user = txn.get(userKey);
			if (user == null) {
				txn.rollback();
				// Username does not exist
				LOG.warning("User not found: " + request.input.username);
				return Response.ok(g.toJson(new RestResponse(ErrorCodes.USER_NOT_FOUND, ErrorCodes.USER_NOT_FOUND_MSG)))
						.build();
			}


			String hashedPWD = user.getString("user_pwd");
			if (!hashedPWD.equals(DigestUtils.sha512Hex(request.input.password))) {
				txn.rollback();
				LOG.warning("Wrong password for: " + request.input.username);
				return Response.ok(g.toJson(new RestResponse(ErrorCodes.INVALID_CREDENTIALS, ErrorCodes.INVALID_CREDENTIALS_MSG)))
						.build();
			}

			String role = user.getString("user_role");
			AuthToken token = new AuthToken(request.input.username, role);

			Key tokenKey = tokenKeyFactory.newKey(token.tokenID);
			Entity tokenEntity = Entity.newBuilder(tokenKey)
					.set("tokenId",   token.tokenID)
					.set("username",  token.username)
					.set("role",      token.role)
					.set("issuedAt",  token.issuedAt)
					.set("expiresAt", token.expiresAt)
					.build();

			txn.put(tokenEntity);
			txn.commit();

			LOG.info("Login successful: " + request.input.username);

			Map<String, Object> dataMap = new HashMap<>();
			dataMap.put("token", token);

			return Response.ok(g.toJson(new RestResponse("success", dataMap))).build();

		} catch (Exception e) {
			txn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(g.toJson(new RestResponse(ErrorCodes.INTERNAL_ERROR, ErrorCodes.INTERNAL_ERROR_MSG)))
					.build();
		} finally {
			if (txn.isActive()) txn.rollback();
		}
	}




}