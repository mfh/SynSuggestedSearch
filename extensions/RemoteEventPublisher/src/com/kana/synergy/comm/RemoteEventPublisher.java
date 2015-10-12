package com.kana.synergy.comm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;

import org.apache.log4j.Logger;

import com.gtnet.common.session.SessionData;
import com.gtnet.j2ee.comms.mux.jms.JMSTopicPublisher;
import com.gtnet.systemProperties.SystemProperties;
import com.gtnet.pk.ecmas.common.IEcmascriptInterpreter;
import com.gtnet.pk.ecmas.extensions.GtEvaluator;

import FESI.Data.ESString;
import FESI.Data.ESValue;

public class RemoteEventPublisher {
	private static final Logger remoteEventLogger = Logger.getLogger(RemoteEventPublisher.class);
	
	private static final Boolean isRemoteTopicEnabled = SystemProperties.getInstance().getValueAsBoolean("remoteEventTopic.enabled");
	
	private JMSTopicPublisher publisher;
	private IEcmascriptInterpreter interpreter;
	private Map<String, Object> eventData;
	
	public RemoteEventPublisher() {
		remoteEventLogger.info("Instantiate remote event publisher.");
		this.interpreter = (IEcmascriptInterpreter) SessionData.getSessionDataProperty(SessionData.EVALUATOR);
		if (this.interpreter == null) {
			this.interpreter = new GtEvaluator();
		} else {
			remoteEventLogger.info("Got interpreter from session data!");
		}
	}
	
	public void init() {
		if (isRemoteTopicEnabled) {
			remoteEventLogger.info("Remote event topic is enabled.");
			try {
				this.publisher = new JMSTopicPublisher();
				this.publisher.initialise();
			} catch (Exception e) {
				remoteEventLogger.error("Error occured while initialising JMSTopicPublisher" + e.getMessage());
				this.publisher = null;
			}
		}
		if (this.publisher == null) {
			remoteEventLogger.info("Remote topic publisher is not available.");
		}
		
		this.eventData = new HashMap<String, Object>();
	}
	
	public void stop() {
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		this.cleanup();
	}

	private void cleanup() {
		if (this.publisher != null) {
			try {
				this.publisher.tearDown();
			} catch (JMSException e) {
				remoteEventLogger.error("Error occured while tearing down JMSTopicPublisher, error message " + e.getMessage());
			}
			this.publisher = null;
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void postRemoteTopicEvent(String topicPath, String eventPath, String gtTargetSessionId) {
		if (this.publisher == null) {
			remoteEventLogger.info("Remote topic publisher is not available, ignoring post request for event: " + eventPath);
			return;
		}
		
		try {
			Map propertyMap = new HashMap<String, Object>();
			propertyMap.put("GT_REMOTE_TOPIC_PATH", topicPath);
			propertyMap.put("GT_EVENT_PATH", eventPath);

			String targetSessionID = "";
			if ((gtTargetSessionId != null) && (!gtTargetSessionId.equals(""))) {
				targetSessionID = gtTargetSessionId;
			}
			propertyMap.put("GT_TARGET_SESSION_ID", targetSessionID);
			
			remoteEventLogger.info("Sending remote event [" + eventPath + "] to session [" + gtTargetSessionId + "] for topic [" + topicPath + "]");

			this.publisher.sendMessage((HashMap) this.eventData, propertyMap);
		} catch (JMSException e) {
			remoteEventLogger.error("Error occured while publishing Remote Event Topic , error message " + e.getMessage());
		} catch (Exception e) {
			remoteEventLogger.error(e);
		} finally {
			this.cleanup();
		}
	}
	
	public void addEventData(String str, String valStr) {
		if (this.eventData == null) {
			this.eventData = new HashMap<String, Object>();
		}
		
		if (str != null && !str.isEmpty()) {
			ESValue val = new ESString(valStr);
			this.eventData.put(str, val);
			remoteEventLogger.info("Added key: " + str + ", value: " + eventData.get(str).toString());
		}
	}
}