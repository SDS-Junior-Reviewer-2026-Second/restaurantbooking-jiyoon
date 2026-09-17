package com.sds.cleancode.restaurant;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MondayBookingScheduler extends BookingScheduler {

    public MondayBookingScheduler(int capacityPerHour) {
        super(capacityPerHour);
    }

    @Override
    public LocalDateTime getNow() {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
        return LocalDateTime.parse("2024/06/03 17:00", format);
    }
}
