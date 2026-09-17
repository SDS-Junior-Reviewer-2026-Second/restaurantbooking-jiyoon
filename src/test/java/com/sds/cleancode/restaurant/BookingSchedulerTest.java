package com.sds.cleancode.restaurant;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

public class BookingSchedulerTest {

    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    public static final LocalDateTime ON_THE_HOUR = LocalDateTime.parse("2021/03/26 09:00", FORMAT);
    public static final LocalDateTime NOT_ON_THE_HOUR = LocalDateTime.parse("2021/03/26 09:05", FORMAT);
    public static final Customer CUSTOMER = new Customer("Fake name", "010-1234-5678");
    public static final Customer customerWithMail = new Customer("Fake Name", "010-1234-5678", "test@test.com");
    public static final int UNDER_CAPACITY = 1;
    public static final int CAPACITY_PER_HOUR = 3;

    BookingScheduler bookingScheduler;
    TestableSmsSender testableSmsSender = new TestableSmsSender();
    TestableMailSender testableMailSender = new TestableMailSender();

    public BookingSchedulerTest() {
        bookingScheduler = new BookingScheduler(CAPACITY_PER_HOUR);
    }

    @BeforeEach
    void setUp() {
        bookingScheduler.setSmsSender(testableSmsSender);
        bookingScheduler.setMailSender(testableMailSender);
    }

    @Test
    public void 예약은_정시에만_가능하다_정시가_아닌경우_예약불가() {
        //arrange
        Schedule schedule = new Schedule(NOT_ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER);
        //act
        assertThatThrownBy(() -> {
            bookingScheduler.addSchedule(schedule);
        }).isInstanceOf(RuntimeException.class);
    //assert
    //expected runtime exception
    }

    @Test
    public void 예약은_정시에만_가능하다_정시인_경우_예약가능() {
    //arrange
        Schedule schedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER);
    //act
        bookingScheduler.addSchedule(schedule);
    //assert
        assertThat(bookingScheduler.hasSchedule(schedule)).isEqualTo(true);
    }

    @Test
    public void 시간대별_인원제한이_있다_같은_시간대에_Capacity_초과할_경우_예외발생() {
        //arrange
        Schedule schedule = new Schedule(ON_THE_HOUR, CAPACITY_PER_HOUR, CUSTOMER);
        bookingScheduler.addSchedule(schedule);
        //act
        try {
            Schedule newSchedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER);
            bookingScheduler.addSchedule(newSchedule);
            fail();
        }
        catch (RuntimeException e) {
            //assert
            assertThat(e.getMessage()).isEqualTo("Number of people is over restaurant capacity per hour");
        }
    }

    @Test
    public void 시간대별_인원제한이_있다_같은_시간대가_다르면_Capacity_차있어도_스케줄_추가_성공() {
        //arrange
        Schedule schedule = new Schedule(ON_THE_HOUR, CAPACITY_PER_HOUR, CUSTOMER);
        bookingScheduler.addSchedule(schedule);
        //act
        LocalDateTime differentHour = ON_THE_HOUR.plusHours(1);
        Schedule newSchedule = new Schedule(differentHour, UNDER_CAPACITY, CUSTOMER);
        bookingScheduler.addSchedule(newSchedule);
        //assert
        assertThat(bookingScheduler.hasSchedule(schedule)).isEqualTo(true);
    }

    @Test
    public void 예약완료시_SMS는_무조건_발송() {
        //arrange
        Schedule schedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER);
        //act
        bookingScheduler.addSchedule(schedule);
        //assert
        assertThat(testableSmsSender.isSendMethodIsCalled()).isEqualTo(true);
    }

    @Test
    public void 이메일이_없는_경우에는_이메일_미발송() {
        //arrange
        Schedule schedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER);
        //act
        bookingScheduler.addSchedule(schedule);
        //assert
        assertThat(testableMailSender.getCountSendMailMethodIsCalled()).isEqualTo(0);
    }

    @Test
    public void 이메일이_있는_경우에는_이메일_발송() {
        //arrange
        Schedule schedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, customerWithMail);
        //act
        bookingScheduler.addSchedule(schedule);
        //assert
        assertThat(testableMailSender.getCountSendMailMethodIsCalled()).isEqualTo(1);
    }

    @Test
    public void 현재날짜가_일요일인_경우_예약불가_예외처리() {
        //arrange
        bookingScheduler = new SundayBookingScheduler(CAPACITY_PER_HOUR);
        //act
        try {
            Schedule newSchedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER);
            bookingScheduler.addSchedule(newSchedule);
            fail();
        }
        catch (RuntimeException e) {
            //assert
            assertThat(e.getMessage()).isEqualTo("Booking system is not available on sunday");
        }
    }

    @Test
    public void 현재날짜가_일요일이_아닌경우_예약가능() {
        //arrange
        bookingScheduler = new MondayBookingScheduler(CAPACITY_PER_HOUR);
        //act
        Schedule newSchedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER);
        bookingScheduler.addSchedule(newSchedule);
        //assert
        assertThat(bookingScheduler.hasSchedule(newSchedule)).isEqualTo(true);
    }
}
