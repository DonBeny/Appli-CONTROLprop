package org.orgaprop.test7.security.events;

import java.util.ArrayList;
import java.util.List;

public class SecurityEventManager {
	private static final List<SecurityEventListener> listeners = new ArrayList<>();

	public interface SecurityEventListener {
		void onSecurityEvent(SecurityEvent event);
	}

	public static class SecurityEvent {
		public final String type;
		public final String message;
		public final long timestamp;

		public SecurityEvent(String type, String message) {
			this.type = type;
			this.message = message;
			this.timestamp = System.currentTimeMillis();
		}
	}

	public static void addListener(SecurityEventListener listener) {
		listeners.add(listener);
	}

	public static void removeListener(SecurityEventListener listener) {
		listeners.remove(listener);
	}

	public static void dispatchEvent(SecurityEvent event) {
		for (SecurityEventListener listener : listeners) {
			listener.onSecurityEvent(event);
		}
	}
}
