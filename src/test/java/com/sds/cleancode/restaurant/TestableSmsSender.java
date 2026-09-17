package com.sds.cleancode.restaurant;

public class TestableSmsSender extends SmsSender {
    private boolean sendMethodIsCalled;
    @Override
    public void send(Schedule schedule) {
        System.out.println("테스트용 SmsSender class의 send메서드 실행됨");
        sendMethodIsCalled = true;
    }
    public boolean isSendMethodIsCalled() {
        return sendMethodIsCalled;
    }
}
