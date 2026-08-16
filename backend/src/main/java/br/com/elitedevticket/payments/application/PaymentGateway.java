package br.com.elitedevticket.payments.application;

public interface PaymentGateway {
    PaymentGatewayResult process(PaymentGatewayCommand command);
}
