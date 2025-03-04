package org.orgaprop.test7.security.auth.models;

import org.json.JSONObject;

public class LoginDataModels {
	public static class UsageStats {
		private int dailyActiveUsers;
		private long averageSessionDuration;
		private double failureRate;

		public UsageStats() {
		}

		public void setDailyActiveUsers(int users) {
			this.dailyActiveUsers = users;
		}

		public void setAverageSessionDuration(long duration) {
			this.averageSessionDuration = duration;
		}

		public void setFailureRate(double rate) {
			this.failureRate = rate;
		}
	}

	public static class CachedResponse {
		private final JSONObject response;
		private final long expirationTime;

		public CachedResponse(JSONObject response, long expirationTime) {
			this.response = response;
			this.expirationTime = expirationTime;
		}

		public boolean isValid() {
			return System.currentTimeMillis() < expirationTime;
		}
	}

	public static class LoginAttempt {
		private final String username;
		private final long timestamp;
		private final boolean success;

		public LoginAttempt(String username, long timestamp, boolean success) {
			this.username = username;
			this.timestamp = timestamp;
			this.success = success;
		}
	}
}
