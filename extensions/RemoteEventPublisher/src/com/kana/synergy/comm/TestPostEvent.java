package com.kana.synergy.comm;

public class TestPostEvent {
	private static final String EVENT_TOPIC = "Sandpit.RemoteSearch.RemoteSearchTopic";
	private static final String EVENT_STR = "Sandpit.RemoteSearch.RemoteSearchEvent";
	
	public static void main(String[] args) {
		RemoteEventPublisher rep = new RemoteEventPublisher();
		rep.init();
		rep.addEventData("searchTag", "test");
		rep.postRemoteTopicEvent(EVENT_TOPIC, EVENT_STR, null);
	}
}