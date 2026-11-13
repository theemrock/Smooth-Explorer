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
	public void formatDurationUsesMinutesAndSeconds() {
		assertEquals("0:00", CurrentStepRemainingWidget.formatDuration(0));
		assertEquals("0:55", CurrentStepRemainingWidget.formatDuration(55));
		assertEquals("1:30", CurrentStepRemainingWidget.formatDuration(90));
	}

	@Test
	public void formatDurationUsesHoursAndMinutesFromOneHour() {
		assertEquals("1:00", CurrentStepRemainingWidget.formatDuration(3600));
		assertEquals("1:02", CurrentStepRemainingWidget.formatDuration(3725));
	}

	@Test
	public void formatDurationClampsNegativeToZero() {
		assertEquals("0:00", CurrentStepRemainingWidget.formatDuration(-5));
	}
}
