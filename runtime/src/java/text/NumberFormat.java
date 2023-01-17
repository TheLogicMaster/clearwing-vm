package java.text;

import java.math.BigInteger;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NumberFormat extends Format  {

    public String format(Object number) {
        if (number instanceof Long || number instanceof Integer || number instanceof Short || number instanceof Byte || number instanceof AtomicInteger
                || number instanceof AtomicLong || (number instanceof BigInteger && ((BigInteger)number).bitLength() < 64)) {
            return format(((Number)number).longValue());
        } else if (number instanceof Number) {
            return format(((Number)number).doubleValue());
        } else {
            throw new IllegalArgumentException("Cannot format given Object as a Number");
        }
    }

    public Object parseObject(String source) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public String format(long number) {
        return Long.toString(number);
    }

    public String format(double number) {
        return Double.toString(number);
    }

    public static NumberFormat getInstance(Locale inLocale) {
        return new NumberFormat();
    }
}
