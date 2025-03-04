package org.orgaprop.test7.security.diagnostic.persistence;

import androidx.annotation.NonNull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.LocalDateTime;

import org.orgaprop.test7.security.diagnostic.DiagnosticResult;

public class DiagnosticRepository {
	private static final DiagnosticRepository INSTANCE = new DiagnosticRepository();
	private final List<DiagnosticRecord> records = new CopyOnWriteArrayList<>();

	public static DiagnosticRepository getInstance() {
		return INSTANCE;
	}

	public void saveDiagnostic(@NonNull DiagnosticResult result) {
		DiagnosticRecord record = new DiagnosticRecord(
				LocalDateTime.now(),
				result,
				Thread.currentThread().getName());
		records.add(record);
	}

	public void saveAll(List<DiagnosticRecord> records) {
		if (records == null || records.isEmpty())
			return;
		this.records.addAll(records);
		pruneOldRecords();
	}

	public List<DiagnosticRecord> getRecordsByCategory(SecurityCategory category) {
		return records.stream()
				.filter(r -> r.hasIssuesInCategory(category))
				.collect(Collectors.toList());
	}
}
