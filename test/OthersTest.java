import org.jgroups.Address;
import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OthersTest {

	@Test
	public void testParseDate() throws ParseException {
		String[] dateStrs = new String[]{"1/1/2016 9:00", "11/11/2016 10:00"};
		DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.ENGLISH);
		Date[] dates = new Date[dateStrs.length];
		for (int i = 0; i < dateStrs.length; i++) {
			dates[i] = dateFormat.parse(dateStrs[i]);
			System.out.println(dateFormat.format(dates[i]));
		}
	}

	@Test
	public void testCastNull() {
		Address add = (Address) null;
		Assert.assertEquals(add, null);
	}
}
