package net.osmand.smooth.routes;

import java.util.Calendar;

/** Request counter that resets when the calendar month changes. */
public final class MonthlyQuota {

	public static final int DEFAULT_LIMIT = 1000;

	public interface Store {
		String getMonth();

		int getCount();

		void save(String month, int count);
	}

	private final Store store;
	private final int limit;

	public MonthlyQuota(Store store, int limit) {
		this.store = store;
		this.limit = limit;
	}

	public static String monthOf(long millis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millis);
		return c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1);
	}

	public synchronized int used(long now) {
		return monthOf(now).equals(store.getMonth()) ? store.getCount() : 0;
	}

	/** Counts one request. @return false (and counts nothing) when the limit is already reached. */
	public synchronized boolean tryAcquire(long now) {
		int used = used(now);
		if (used >= limit) {
			return false;
		}
		store.save(monthOf(now), used + 1);
		return true;
	}
}
