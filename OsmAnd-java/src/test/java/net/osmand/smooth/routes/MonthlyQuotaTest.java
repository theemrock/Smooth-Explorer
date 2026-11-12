package net.osmand.smooth.routes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

public class MonthlyQuotaTest {

	private static class MemStore implements MonthlyQuota.Store {
		String month;
		int count;

		public String getMonth() {
			return month;
		}

		public int getCount() {
			return count;
		}

		public void save(String m, int c) {
			month = m;
			count = c;
		}
	}

	private static long at(int year, int month, int day) {
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(year, month - 1, day, 12, 0);
		return c.getTimeInMillis();
	}

	@Test
	public void stopsAtLimit() {
		MonthlyQuota q = new MonthlyQuota(new MemStore(), 3);
		long t = at(2026, 11, 10);
		assertTrue(q.tryAcquire(t));
		assertTrue(q.tryAcquire(t));
		assertTrue(q.tryAcquire(t));
		assertFalse(q.tryAcquire(t));
		assertEquals(3, q.used(t));
	}

	@Test
	public void resetsWhenMonthChanges() {
		MonthlyQuota q = new MonthlyQuota(new MemStore(), 2);
		q.tryAcquire(at(2026, 11, 30));
		q.tryAcquire(at(2026, 11, 30));
		assertFalse(q.tryAcquire(at(2026, 11, 30)));
		assertEquals(0, q.used(at(2026, 12, 1)));
		assertTrue(q.tryAcquire(at(2026, 12, 1)));
		assertEquals(1, q.used(at(2026, 12, 1)));
	}

	@Test
	public void sameMonthNextYearAlsoResets() {
		MonthlyQuota q = new MonthlyQuota(new MemStore(), 1);
		assertTrue(q.tryAcquire(at(2026, 11, 1)));
		assertTrue(q.tryAcquire(at(2027, 11, 1)));
	}
}
