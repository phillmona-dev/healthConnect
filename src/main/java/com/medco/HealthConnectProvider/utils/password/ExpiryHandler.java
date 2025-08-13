package com.medco.HealthConnectProvider.utils.password;

import java.util.Calendar;
import java.util.Date;

public class ExpiryHandler {

    public static Date calculateExpiryDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MINUTE, 2);
        return cal.getTime();
    }

}
