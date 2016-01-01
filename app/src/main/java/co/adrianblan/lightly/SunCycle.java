package co.adrianblan.lightly;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * A class which models the information for a sun cycle
 */
public class SunCycle {

    private float sunPositionHorizontal; // Position [0, 1] in x axis that the sun is at
    private float cycleOffsetHorizontal; // Position [0, 1] in x axis that the cycle should be offset
    private float twilightPositionVertical; // Position [0, 1] in y axis that the twilight is at

    float sunrisePosition; // Position [0, 1] in x axis that the sunrise is at
    float sunsetPosition; // Position [0, 1] in x axis that the sunset is set at

    public SunCycle (Date current, SunriseSunsetData sunriseSunsetData) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss aa", Locale.US);
        //simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date sunrise = simpleDateFormat.parse(sunriseSunsetData.getCivilTwilightBegin());
        Date sunset = simpleDateFormat.parse(sunriseSunsetData.getCivilTwilightEnd());

        initializeSunCycle(sunrise, sunset);
        updateSunPosition(current);
    }

    public SunCycle (Date current, Date sunrise, Date sunset) {
        initializeSunCycle(sunrise, sunset);
        updateSunPosition(current);
    }

    /**
     * Initializes a sun cycle, given the Dates of sunrise and sunset.
     * Sunset and sunrise are assumed to be during the same day.
     */
    private void initializeSunCycle(Date sunrise, Date sunset) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(sunrise);

        // Convert the dates to [0, 1] positions
        sunrisePosition = getScaledTime(sunrise);
        sunsetPosition = getScaledTime(sunset);

        // The position where the sun is at it's highest
        float solarNoonHorizontalPosition = sunrisePosition +  (sunsetPosition - sunrisePosition) / 2f;

        // If the solar noon is at 0.25f, we count that as zero offset
        cycleOffsetHorizontal = ((solarNoonHorizontalPosition - 0.25f) + 1f) % 1f;

        // Calculate at what scaled height the twilight is at
        twilightPositionVertical = (float) getScaledRadian(
                (Math.sin(sunrisePosition * Constants.tau + cycleOffsetHorizontal * Constants.tau) +
                        Math.sin(sunsetPosition * Constants.tau + cycleOffsetHorizontal * Constants.tau)) / 2);
    }

    /** Calculates the position of the sun for the current time, given the initialized sun cycle */
    public void updateSunPosition(Date current) {
        sunPositionHorizontal = getScaledTime(current);
    }

    /** Scales a Date [0, 1] according to how far it is in the current date */
    private float getScaledTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        // Scale according to hours and minutes
        float scaledTime = calendar.get(Calendar.HOUR_OF_DAY) / 24f;
        scaledTime += calendar.get(Calendar.MINUTE) / (60f * 24f);

        return scaledTime;
    }

    /** Takes an angle in radians, and converts it to an abs value with bounds [0, 1] */
    private double getScaledRadian(double radian) {
        return ((radian + Constants.tau) % Constants.tau) / Constants.tau;
    }

    public float getSunPositionHorizontal() {
        return sunPositionHorizontal;
    }

    public float getCycleOffsetHorizontal() {
        return cycleOffsetHorizontal;
    }

    public float getTwilightPositionVertical() {
        return twilightPositionVertical;
    }

    public float getSunrisePosition() {
        return sunrisePosition;
    }

    public float getSunsetPosition() {
        return sunsetPosition;
    }

    /** Takes a position [0, 1] and converts it to a string of the time (HH:MM) */
    public static String getTimeFromPosition(float position) {
        int hours = (int)(position * 24 / 1);
        int minutes = (int)((position * 24 % 1) * 60);
        return  hours + ":" + minutes;
    }
}