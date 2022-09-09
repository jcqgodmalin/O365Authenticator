/**
 * 
Steps to use this class:
 * 1. Register O365Authenticator as application in Azure AD. 
 * 		REFERENCE: https://docs.microsoft.com/en-us/azure/active-directory/develop/quickstart-register-app
 * 2. Get Tenant ID, Application Client ID and Client Secret.
 * 3. Add Scope in App's API Permission:
 * 		Microsoft API:
 * 			Microsoft Graph:
 * 				offline_access, POP.AccessAsUser
 * 		APIs my organization uses:
 * 			Office 365 Exchange Online:
 * 				POP.AccessAsApp -Need to be granted by a Global Administrator
 * 3. Using an account with Global Administrator Role in Azure AD Tenant, Grant this app an access
 *    to the mailbox using powershell.
 *    	REFERENCE: https://github.com/jcqgodmalin/grant-o365-access-with-powershell/blob/main/README.md
 * 
 * Dependencies:
 * 
 * In order for this class to run properly, please make sure to include the ff. dependencies into your
 * application's POM.XML
 * 
 * <dependency>
 * 		<groupId>org.apache.httpcomponents.client5</groupId>
 *		<artifactId>httpclient5</artifactId>
 *		<version>5.1.3</version>
 * </dependency>
 * <dependency>
 * 		<groupId>commons-net</groupId>
 *		<artifactId>commons-net</artifactId>
 *		<version>3.8.0</version>
 * </dependency>
 * 
 */



package com.jic.lib;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.pop3.POP3Reply;
import org.apache.commons.net.pop3.POP3SClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.jic.test.jictest.impl.JicTestAuth;

public class O365Authenticator {
	
	private static final Logger LOG = LoggerFactory
			.getLogger(O365Authenticator.class);
	
	private String tenantId;
	private String clientId;
	private String clientsecret;
	
	private POP3SClient client;

	public O365Authenticator(String tenantId, String clientId, String clientsecret) {
		super();
		this.tenantId = tenantId;
		this.clientId = clientId;
		this.clientsecret = clientsecret;
	}

	public String getToken() {
		
		HttpPost httpPost = new HttpPost("https://login.microsoftonline.com/" 
		+ this.tenantId + "/oauth2/v2.0/token");
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		   
		params.add( new BasicNameValuePair("scope", "https://outlook.office365.com/.default") );
		params.add( new BasicNameValuePair( "grant_type"   , "client_credentials" ) );
		params.add( new BasicNameValuePair( "client_id"    , this.clientId ) );
		params.add( new BasicNameValuePair( "client_secret", this.clientsecret ) );
		   
	   try ( CloseableHttpClient httpclient = HttpClients.createDefault() ) {
		   
	      httpPost.setEntity(new UrlEncodedFormEntity(params,StandardCharsets.UTF_8));
	      
	      LOG.info("Getting access token...");
	      
	      return httpclient.execute( httpPost, new HttpClientResponseHandler<>() {
	
			@Override
			public String handleResponse(ClassicHttpResponse response) throws ClientProtocolException, HttpException, IOException {
				ObjectMapper mapper = new ObjectMapper();
	            TypeFactory f = mapper.getTypeFactory();
	            JavaType t = f.constructMapType( LinkedHashMap.class, f.constructType( String.class ),  f.constructType( Object.class ) );
	            Map<String,Object> map = mapper.readValue(response.getEntity().getContent(), t );
	            LOG.info("Access Token Received!");
	            return String.valueOf( map.get( "access_token" ) );
			}
	    	  
	      });
	   
	   } catch ( IOException ex ) {
		   
	      LOG.info("OOPS! Error encountered: " + ex.getMessage());
		  throw new IllegalStateException( "Unable to get token." );
	   
	   }
	}

	public POP3SClient getClient() {
	
		
		this.client = new POP3SClient("TLS", true) {

			@Override
			public boolean login (String mailbox, String token) throws IOException {
				
				int serverResponse;
		
				LOG.info("Checking state...");
				
				if (getState() != AUTHORIZATION_STATE) {
					
					LOG.info("Could not proceed to login. Error: Not in AUTHORIZATION_STATE");
					return false;
					
				} else {
					
					LOG.info("SUCCESS: in AUTHORIZATION_STATE");
					
				}
				
				LOG.info("Sending AUTH XOAUTH2 COMMAND TO POP3 SERVER...");
				serverResponse = sendCommand( "AUTH", "XOAUTH2" );
				LOG.info("SERVER RESPONSE: " + serverResponse);
				
				LOG.info("Encoding username/email and access token to SASL XOAUTH2 format");
				String toEncode = "user=" + mailbox + (char)0x01 + "auth=Bearer " + token + (char)0x01 + (char)0x01; 
				String encoded = Base64.getEncoder().encodeToString(toEncode.getBytes(StandardCharsets.UTF_8.toString()));
				LOG.info("Encoding complete.");
				
				try {
					
					LOG.info("Sending encoded credentials to the server...");
					serverResponse = sendCommand(encoded);
					
				}catch(Exception e) {
					
					LOG.info(e.getMessage());
					return false;
					
				}
				
				if (serverResponse != POP3Reply.OK) {
					
					LOG.info("Error logging in! The server response is: " + serverResponse);
					return false;
					
				}else {
					
					LOG.info("Success logging in! The server response is: " + serverResponse);
					setState(TRANSACTION_STATE);
					return true;
					
				}
				
			}
			
		};
		
		LOG.info("Setting Trust Manager...");
		this.client.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
		
		LOG.info("Adding ProtocolCommandListener...");
		this.client.addProtocolCommandListener( new ProtocolCommandListener() {
			   @Override
			   public void protocolCommandSent( ProtocolCommandEvent protocolCommandEvent ) {
			      if ( LOG.isDebugEnabled() ) {
			         LOG.debug( "Command Sent: " + protocolCommandEvent.getCommand() );
			      }
			   }
		
			   @Override
			   public void protocolReplyReceived( ProtocolCommandEvent protocolCommandEvent ) {
			      if ( LOG.isDebugEnabled() ) {
			         LOG.debug( "Reply received: " + protocolCommandEvent.getReplyCode() + ":" + protocolCommandEvent.getMessage() );
			      }
			   }
			});
		
		return client;
	
	}
	
}
