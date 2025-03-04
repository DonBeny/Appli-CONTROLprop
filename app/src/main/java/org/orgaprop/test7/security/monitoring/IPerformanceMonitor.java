package org.orgaprop.test7.security.monitoring;

import androidx.annotation.NonNull;

public interface IPerformanceMonitor {
	void startOperation(@NonNull String operation);

	void endOperation(@NonNull String operation);

	long getAverageOperationTime(@NonNull String operation);

	void resetMetrics();
}
