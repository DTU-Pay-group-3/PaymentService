package main;

import dtu.ws.fastmoney.AccountInfo;
import dtu.ws.fastmoney.BankService;
import dtu.ws.fastmoney.BankServiceService;
import jakarta.json.Json;
import messaging.implementations.RabbitMqQueue;

import javax.swing.text.html.parser.Entity;
import java.util.ArrayList;

public class StartUp {
	public static void main(String[] args) throws Exception {

		 BankService bank = new BankServiceService().getBankServicePort();
	ArrayList<AccountInfo> acc=new ArrayList<>(bank.getAccounts());
		System.out.println(acc.size());
		//new StartUp().startUp();
	}

	private void startUp() throws Exception {

		var mq = new RabbitMqQueue("rabbitMq");
		System.out.println("connected to Mq");
		System.out.println(mq.toString());
		new PaymentService(mq);
	}
}
