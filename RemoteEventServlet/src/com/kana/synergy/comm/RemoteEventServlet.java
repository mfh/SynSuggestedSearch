package com.kana.synergy.comm;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.gtnet.common.util.logging.ShipsLog;

public class RemoteEventServlet extends HttpServlet {
	private static final long serialVersionUID = -89183843199648870L;
	
	private static final String EVT_SEARCH_TAG = "searchTag";
	private static final String EVT_USERNAME = "username";
	private static final String EVT_SESSIONID = "sessionID";
	
	private static final String EVENT_TOPIC = "SynergyAddKnowSearch.Implementation.SuggestedContent.Events.RemoteSearchTopic";
	private static final String EVENT_STR = "SynergyAddKnowSearch.Implementation.SuggestedContent.Events.RemoteSearchEvent";
	private static final String datasourceContext = "java:/jdbc/kanaDS";
	
	private static final String GET_USER_QUERY = "SELECT b.USERNAME FROM CE_AGENT a, FU_USER b, CE_EXTERNAL_SECURITY_DETAILS c WHERE a.USER_ID = b.ID AND b.id = c.AGENT_ID AND c.USERNAME = ?";
	private static final String GET_SESSION_QUERY = "SELECT TOP 1 SESSION_ID FROM FD_AGENT_SESSION_AUDIT WHERE USERNAME = ? AND END_TIME IS NULL ORDER BY start_time DESC;";
	
	private String searchTag;
	private String username;
	private String sessionId;
	
	public RemoteEventServlet() {}
	public void init() {}
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ShipsLog.out.asInfo("remote-event-servlet: Initialized.");
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ShipsLog.out.asInfo("remote-event-servlet: do something...");
		//parameters
		String extUsername = request.getParameter(EVT_USERNAME);
		searchTag = request.getParameter(EVT_SEARCH_TAG);
		
		ShipsLog.out.asInfo("remote-event-servlet: extUsername = " + extUsername);
		ShipsLog.out.asInfo("remote-event-servlet: searchTag = " + searchTag);
		
		if (searchTag != null && !searchTag.isEmpty()) {
			try {
				//get username and session id
				if (extUsername != null) {
					getDetails(extUsername);
				}
				
				ShipsLog.out.asInfo("remote-event-servlet: username = " + this.username);
				ShipsLog.out.asInfo("remote-event-servlet: sessionId = " + this.sessionId);

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//post remote event
			RemoteEventPublisher rep = new RemoteEventPublisher();
			rep.init();
			
			rep.addEventData(EVT_SEARCH_TAG, searchTag);
			
			if (username != null) {
				rep.addEventData(EVT_USERNAME, username);
			}
			
			if (sessionId != null) {
				rep.addEventData(EVT_SESSIONID, sessionId);
			}
			
			ShipsLog.out.asInfo("remote-event-servlet: posting remote event...");
			rep.postRemoteTopicEvent(EVENT_TOPIC, EVENT_STR, sessionId);
		} else {
			ShipsLog.out.asInfo("remote-event-servlet: No searchTag! No event!");
		}
		
		PrintWriter out = response.getWriter();
	    
		//set 200 OK
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		out.println("OK");
		
		out.flush();
	    out.close();
	}
	
	private Connection getConnection() {
		Connection con = null;
		try {
			Context ic = new javax.naming.InitialContext();
			DataSource ds = (javax.sql.DataSource) ic.lookup(datasourceContext);
			if (ds != null) { 
				con = ds.getConnection();
			} else {
			    ShipsLog.out.asInfo("remote-event-servlet: Failed to lookup datasource.");
			}
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return con;
	}
	
	private void getDetails(String extUsername) throws SQLException {
		Connection conn = getConnection();
		try {
			username = getUsername(conn, extUsername);
			sessionId = getSessionId(conn, username);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			conn.close();
		}
	}
	
	private String getUsername(Connection conn, String extUsername) throws SQLException {
		PreparedStatement stmt = null;
	    String username = null;
	    ResultSet rs = null;
		try {
	        stmt = conn.prepareStatement(GET_USER_QUERY);
	        stmt.setString(1, extUsername);
	        ShipsLog.out.asInfo("remote-event-servlet: get username: " + stmt.toString());
	        
	        rs = stmt.executeQuery();
	        while (rs.next()) {
	            username = rs.getString("USERNAME");
	            ShipsLog.out.asInfo("remote-event-servlet: found username: " + username + " for ext username: " + extUsername);
	        }
	    } catch (SQLException e ) {
	    	e.printStackTrace();
	    } finally {
	    	if (stmt != null ) { stmt.close(); }
	    	if (rs != null) { rs.close(); }
	    }
		
		if (username == null || username.isEmpty()) {
			username = extUsername;
		}
		
		return username;
	}
	
	private String getSessionId(Connection conn, String username) throws SQLException {
		PreparedStatement stmt = null;
	    String sessionId = null;
	    ResultSet rs = null;
		try {
			stmt = conn.prepareStatement(GET_SESSION_QUERY);
			stmt.setString(1, username);
			ShipsLog.out.asInfo("remote-event-servlet: get session id: " + stmt.toString());
			
	        rs = stmt.executeQuery();
	        while (rs.next()) {
	            sessionId = rs.getString("SESSION_ID");
	            ShipsLog.out.asInfo("remote-event-servlet: found sessionId: " + sessionId + " for username: " + username);
	        }
	    } catch (SQLException e ) {
	    	e.printStackTrace();
	    } finally {
	    	if (stmt != null ) { stmt.close(); }
	    	if (rs != null) { rs.close(); }
	    }
		
		return sessionId;
	}
	
	public String getServletInfo() {
	    return "Remote Event Servlet";
    }
}
