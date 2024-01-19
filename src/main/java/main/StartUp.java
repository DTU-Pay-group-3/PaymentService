package main;

import messaging.implementations.RabbitMqQueue;

public class StartUp {
	/*Author Marian s233481 */
	public static void main(String[] args) throws Exception {
		new StartUp().startUp();
	}

	/*Author Marian s233481 */
	private void startUp() throws Exception {
		var mq = new RabbitMqQueue("rabbitMq");
		new PaymentService(mq);
	}
}
