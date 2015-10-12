package com.kana.synergy.comm;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.gtnet.systemProperties.SystemProperties;

public class SuggestedSearchServlet extends HttpServlet {
	private static final Logger remoteEventLogger = Logger.getLogger(SuggestedSearchServlet.class);
	
	private static final long serialVersionUID = 89183843199648870L;
	
	private static final String dataSource = SystemProperties.getInstance().getValue("suggestedSearchService.dataSourceContext");
	private static final String eventTopic = SystemProperties.getInstance().getValue("suggestedSearchService.eventTopic");
	private static final String eventName = SystemProperties.getInstance().getValue("suggestedSearchService.eventName");
	
	private static final String SEARCHTAG = "searchTag";
	private static final String SEARCHTERM = "searchTerm";
	private static final String USERNAME = "username";
	private static final String SESSIONID = "sessionID";
	
	private static final String LOOKUP_QUERY = "SELECT TOP 1 a.USERNAME as username, a.SESSION_ID as sessionID FROM GTCC_AGENT_SESSION a, FU_USER b, CE_AGENT c, EVA_ENTITY_PROPERTY d WHERE a.USERNAME = b.USERNAME AND b.ID = c.USER_ID AND c.ID = d.ENTITY_ID AND d.PROPERTY_VALUE = ? ORDER BY a.ID DESC";
	
	private static RemoteEventPublisher publisher;
	
	private String searchTag;
	private String searchTerm;
	private String externalUsername;
	private String internalUsername;
	private String internalSessionID;
	
	public void init(ServletConfig config) throws ServletException {
		remoteEventLogger.info("Initialize remote event servlet.");
		super.init(config);
		
		publisher = new RemoteEventPublisher();
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		remoteEventLogger.info("Receive a request...");
		
		this.searchTag = request.getParameter(SEARCHTAG);
		if (this.searchTag == null) this.searchTag = "";
		
		this.searchTerm = request.getParameter(SEARCHTERM);
		if (this.searchTerm == null) this.searchTerm = "";
		
		this.externalUsername = request.getParameter(USERNAME);
		if (this.externalUsername == null) this.externalUsername = "";
		
		remoteEventLogger.info("Got query parameters: searchTag = \"" + this.searchTag + "\" and searchTerm = \"" + this.searchTerm + "\" for external username = \"" + this.externalUsername + "\"");
		
		if (!this.externalUsername.isEmpty() && !this.searchTag.isEmpty()) {
			try {
				Map<String, String> resultMap = getUsernameAndSessionID(this.externalUsername);
				this.internalUsername = resultMap.get(USERNAME);
				this.internalSessionID = resultMap.get(SESSIONID);
				
				if (!this.internalUsername.isEmpty() && !this.internalSessionID.isEmpty()) {
					remoteEventLogger.info("Posting a remote event to event publisher for this request.");
					publisher.init();
					publisher.addEventData(SEARCHTAG, this.searchTag);
					publisher.addEventData(SEARCHTERM, this.searchTerm);
					publisher.addEventData(USERNAME, this.internalUsername);
					publisher.addEventData(SESSIONID, this.internalSessionID);
					publisher.postRemoteTopicEvent(eventTopic, eventName, this.internalSessionID);
				} else {
					remoteEventLogger.info("Not posting a remote event because target internal username or sessionID is not found.");
				}
			} catch (NamingException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			remoteEventLogger.error("Not posting a remote event because mandatory username or searchTag parameter's value is empty!");
		}
		
		//set response
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		
		PrintWriter out = response.getWriter();
		out.println(this.getServletInfo());
		out.flush();
	    out.close();
	}
	
	private static Map<String, String> getUsernameAndSessionID(String externalUsername) throws NamingException, SQLException {
		Map<String, String> resultMap = new HashMap<String, String>();
		String username = null;
		String sessionID = null;
		
		Connection conn = getConnection();
		PreparedStatement stmt = null;
	    ResultSet rs = null;
		try {
	        stmt = conn.prepareStatement(LOOKUP_QUERY);
	        stmt.setString(1, externalUsername);
	        rs = stmt.executeQuery();
	        
	        while (rs.next()) {
	            username = rs.getString(USERNAME);
	            resultMap.put(USERNAME, username);
	            sessionID = rs.getString(SESSIONID);
	            resultMap.put(SESSIONID, sessionID);
	        }
	        
	        if (username == null) resultMap.put(USERNAME, "");
	        if (sessionID == null) resultMap.put(SESSIONID, "");
	        
	        if ((username != null && !username.isEmpty()) && (sessionID != null && !sessionID.isEmpty())) {
	        	remoteEventLogger.info("Found target: internal username = \"" + username + "\" and sessionID = \"" + sessionID + "\" for external username: \"" + externalUsername + "\"");
	        }
	    } catch (SQLException e ) {
	    	e.printStackTrace();
	    } finally {
	    	if (stmt != null ) stmt.close();
	    	if (rs != null) rs.close();
	    	conn.close();
	    }
		
		return resultMap;
	}

	private static Connection getConnection() throws NamingException, SQLException {
		Connection conn = null;
		try {
			Context ic = new javax.naming.InitialContext();
			DataSource ds = (javax.sql.DataSource) ic.lookup(dataSource);
			if (ds != null) { 
				conn = ds.getConnection();
			} else {
			    remoteEventLogger.info("Failed to lookup datasource.");
			}
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return conn;
	}

	public String getServletInfo() {
	    return "Suggested Search Servlet";
    }
}