package net.osmand.plus.views.mapwidgets.widgets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CurrentStepRemainingWidgetTest {

	@Test
	public void remainingTimeScalesWithLeftDistance() {
		assertEquals(30, CurrentStepRemainingWidget.remainingTimeSeconds(60, 1000, 500));
	}

	@Test
	public void zeroStepDistanceGivesZero() {
		assertEquals(0, CurrentStepRemainingWidget.remainingTimeSeconds(60, 0, 500));
	}

	@Test
	public void leftDistanceAboveStepDistanceGivesStepTime() {
		assertEquals(55, CurrentStepRemainingWidget.remainingTimeSeconds(55, 1490, 2000));
	}

	@Test
	public void zeroLeftDistanceGivesZero() {
		assertEquals(0, CurrentStepRemainingWidget.remainingTimeSeconds(55, 1490, 0));
	}

	@Test
	public void negativeInputsGiveZero() {
		assertEquals(0, CurrentStepRemainingWidget.remainingTimeSeconds(55, 1490, -100));
		assertEquals(0, CurrentStepRemainingWidget.remainingTimeSeconds(55, -1490, 100));
		assertEquals(0, CurrentStepRemainingWidget.remainingTimeSeconds(-55, 1490, 100));
	}

	@Test
	public void formatDurationShowsSecondsUpToOneMinute() {
		assertEquals("0sn", CurrentStepRemainingWidget.formatDuration(0));
		assertEquals("31sn", CurrentStepRemainingWidget.formatDuration(31));
		assertEquals("60sn", CurrentStepRemainingWidget.formatDuration(60));
	}

	@Test
	public void formatDurationShowsMinutesAboveOneMinute() {
		assertEquals("1dk", CurrentStepRemainingWidget.formatDuration(61));
		assertEquals("34dk", CurrentStepRemainingWidget.formatDuration(2054));
	}

	@Test
	public void formatDurationShowsHoursAndMinutesFromOneHour() {
		assertEquals("1S00dk", CurrentStepRemainingWidget.formatDuration(3600));
		assertEquals("12S34dk", CurrentStepRemainingWidget.formatDuration(12 * 3600 + 34 * 60 + 54));
	}

	@Test
	public void formatDurationClampsNegativeToZero() {
		assertEquals("0sn", CurrentStepRemainingWidget.formatDuration(-5));
	}
}
