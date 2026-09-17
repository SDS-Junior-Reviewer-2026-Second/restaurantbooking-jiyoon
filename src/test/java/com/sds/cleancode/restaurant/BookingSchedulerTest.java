package com.sds.cleancode.restaurant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingSchedulerTest {
    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    public static final LocalDateTime ON_THE_HOUR = LocalDateTime.parse("2021/03/26 09:00", FORMAT);
    public static final LocalDateTime NOT_ON_THE_HOUR = LocalDateTime.parse("2021/03/26 09:05", FORMAT);
    public static final int UNDER_CAPACITY = 1;
    public static final int CAPACITY_PER_HOUR = 3;

    @Mock
    public Customer CUSTOMER;

    @Mock(answer = Answers.RETURNS_MOCKS)
    public Customer CUSTOMER_WITH_MAIL;

    @Spy
    BookingScheduler bookingScheduler;

    @Mock
    public SmsSender smsSender;

    @Mock
    public MailSender mailSender;

    public BookingSchedulerTest(){
        bookingScheduler = new BookingScheduler(CAPACITY_PER_HOUR);
    }

    @BeforeEach
    void setUp(){
        lenient().doReturn(ON_THE_HOUR).when(bookingScheduler).getNow();
        bookingScheduler.setSmsSender(smsSender);
        bookingScheduler.setMailSender(mailSender);
    }

    @Test
    public void 예약은_정시에만_가능하다_정시가_아닌경우_예약불가() {
        Schedule schedule = new Schedule(NOT_ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER);

        assertThatThrownBy(() -> {
            bookingScheduler.addSchedule(schedule);
        }).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void 예약은_정시에만_가능하다_정시인_경우_예약가능() {
        Schedule schedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER);

        bookingScheduler.addSchedule(schedule);

        assertThat(bookingScheduler.hasSchedule(schedule)).isEqualTo(true);
    }

    @Test
    public void 시간대별_인원제한이_있다_같은_시간대에_Capacity_초과할_경우_예외발생() {

        Schedule schedule = new Schedule(ON_THE_HOUR, CAPACITY_PER_HOUR, CUSTOMER);
        bookingScheduler.addSchedule(schedule);

        try {
            Schedule newSchedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER);
            bookingScheduler.addSchedule(newSchedule);
            fail();
        }
        catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Number of people is over restaurant capacity per hour");
        }
    }

    @Test
    public void 시간대별_인원제한이_있다_같은_시간대가_다르면_Capacity_차있어도_스케쥴_추가_성공() {

        Schedule schedule = new Schedule(ON_THE_HOUR, CAPACITY_PER_HOUR, CUSTOMER);
        bookingScheduler.addSchedule(schedule);

        LocalDateTime differentHour = ON_THE_HOUR.plusHours(1);
        Schedule newSchedule = new Schedule(differentHour, UNDER_CAPACITY, CUSTOMER);
        bookingScheduler.addSchedule(newSchedule);

        assertThat(bookingScheduler.hasSchedule(schedule)).isEqualTo(true);
    }

    @Test
    public void 예약완료시_SMS는_무조건_발송() {

        Schedule schedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER);

        bookingScheduler.addSchedule(schedule);

        verify(smsSender, times(1)).send(schedule);
    }

    @Test
    public void 이메일이_없는_경우에는_이메일_미발송() {

        Schedule schedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER);

        bookingScheduler.addSchedule(schedule);

        verify(mailSender, times(0)).sendMail(schedule);
    }

    @Test
    public void 이메일이_있는_경우에는_이메일_발송() {
        Schedule schedule = new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER_WITH_MAIL);

        bookingScheduler.addSchedule(schedule);

        verify(mailSender, times(1)).sendMail(schedule);
    }

    @Test
    public void 현재날짜가_일요일인_경우_예약불가_예외처리() {
        LocalDateTime sunday =
                LocalDateTime.parse("2021/03/28 17:00", FORMAT);

        doReturn(sunday).when(bookingScheduler).getNow();

        Schedule schedule =
                new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER);

        assertThatThrownBy(() -> bookingScheduler.addSchedule(schedule))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Booking system is not available on sunday");

        assertThat(bookingScheduler.hasSchedule(schedule)).isFalse();
    }

    @Test
    public void 현재날짜가_일요일이_아닌경우_예약가능() {
        LocalDateTime monday =
                LocalDateTime.parse("2024/06/03 17:00", FORMAT);

        doReturn(monday).when(bookingScheduler).getNow();

        Schedule schedule =
                new Schedule(ON_THE_HOUR, UNDER_CAPACITY, CUSTOMER);

        bookingScheduler.addSchedule(schedule);

        assertThat(bookingScheduler.hasSchedule(schedule)).isTrue();
    }
}