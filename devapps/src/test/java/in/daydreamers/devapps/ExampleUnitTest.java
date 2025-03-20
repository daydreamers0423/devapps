package in.daydreamers.devapps;

import org.junit.Test;

import static org.junit.Assert.*;

import android.util.Log;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(DevAppsTime.getCurrentTimeFromNTP());
        DecimalFormat mFormat= new DecimalFormat("00");
        System.out.println("Test:::"+ (calendar.get(Calendar.DAY_OF_MONTH)+"-"+ mFormat.format(calendar.get(Calendar.MONTH)+1)+"-"+ calendar.get(Calendar.YEAR)).toString());
    }
}