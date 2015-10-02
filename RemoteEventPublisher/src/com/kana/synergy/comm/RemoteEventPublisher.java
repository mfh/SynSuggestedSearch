package com.kana.synergy.comm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;

import com.gtnet.common.session.SessionData;
import com.gtnet.common.util.logging.ShipsLog;
import com.gtnet.j2ee.comms.mux.jms.JMSTopicPublisher;
import com.gtnet.systemProperties.SystemProperties;
import com.gtnet.pk.ecmas.common.IEcmascriptInterpreter;
import com.gtnet.pk.ecmas.extensions.GtEvaluator;

import FESI.Data.ESString;
import FESI.Data.ESValue;

public class RemoteEventPublisher {
	
	private JMSTopicPublisher publisher;
	private IEcmascriptInterpreter interpreter;
	private Map<String, Object> eventData;
	
	public RemoteEventPublisher() {
		ShipsLog.out.asInfo(">>>> instantiate interpreter");
		this.interpreter = (IEcmascriptInterpreter) SessionData.getSessionDataProperty(SessionData.EVALUATOR);
		if (this.interpreter == null) {
			this.interpreter = new GtEvaluator();
		} else {
			ShipsLog.out.asInfo(">>>> Got interpreter from session data!");
		}
	}
	
	public void init() {
		if ((SystemProperties.getInstance().getValueAsBoolean("remoteEventTopic.enabled"))) {
			ShipsLog.out.asInfo(">>>> Remote topics are enabled.");
			try {
				this.publisher = new JMSTopicPublisher();
				this.publisher.initialise();
			} catch (Exception e) {
				ShipsLog.out.asError("!!!! Error occured while initialising JMSTopicPublisher"
								+ e.getMessage());
				this.publisher = null;
			}
		}
		if (this.publisher == null) {
			ShipsLog.out.asInfo(">>>> Remote topics are not available.");
		}
		
		this.eventData = new HashMap<String, Object>();
	}
	
	public void stop() {
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.cleanup();
	}

	private void cleanup() {
		if (this.publisher != null) {
			try {
				this.publisher.tearDown();
			} catch (JMSException e) {
				ShipsLog.kernel.asError("!!!! Error occured while tearing down JMSTopicPublisher, error message "
								+ e.getMessage());
			}
			this.publisher = null;
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void postRemoteTopicEvent(String topicPath, String eventPath, String gtTargetSessionId) {
		if (this.publisher == null) {
			ShipsLog.out.asInfo("Remote topics are not available, ignoring post request for event: " + eventPath);
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
			
			ShipsLog.out.asInfo("KEY", new Object[] { eventPath, topicPath, gtTargetSessionId },
							"Sending remote event [{0}] to session [{2}] for topic [{1}]");

			this.publisher.sendMessage((HashMap) this.eventData, propertyMap);
		} catch (JMSException e) {
			ShipsLog.out.asError("Error occured while publishing Remote Event Topic , error message " + e.getMessage());
		} catch (Exception e) {
			ShipsLog.out.asError(e);
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
			ShipsLog.out.asInfo(">>>> added key: " + str + ", value: " + eventData.get(str).toString());
		}
	}

}
