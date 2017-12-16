package wall_follower;

import lejos.nxt.*;

public class LeftHand {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		UltrasonicSensor ultra = new UltrasonicSensor(SensorPort.S1);
		int power = 70;
		int mode_forward = 1;
		int mode_backward = 2;
		int mode_stop = 3;
		int mode_float = 4;
		int distance;
		
		LCD.drawString("Distance %: ", 0, 0);
		LCD.drawString("Press LEFT", 0, 2);
		LCD.drawString("to start", 0, 3);
		
		while(!Button.LEFT.isDown()) {
			LCD.drawInt(ultra.getDistance(), 3, 9, 0);
		}
		
		LCD.drawString("Press ESCAPE", 0, 2);
		LCD.drawString("to stop ", 0, 3);
		
		while(!Button.ESCAPE.isDown()) {
			distance = ultra.getDistance();
			Thread.sleep(20);
			LCD.drawInt(distance, 3, 9, 0);
		
			if (distance < 25) {	//kanan
				MotorPort.A.controlMotor(power, mode_backward);
				MotorPort.B.controlMotor(power, mode_forward);
				LCD.drawString("kanan ", 0, 5);
			}
			else if (distance > 50) {	//kiri
				MotorPort.A.controlMotor(power*3/2, mode_forward);
				MotorPort.B.controlMotor(power, mode_forward);
				LCD.drawString("kiri ", 0, 5);
			}
			else {
				MotorPort.A.controlMotor(power, mode_forward);
				MotorPort.B.controlMotor(power, mode_forward);
				LCD.drawString("maju ", 0, 5);
			}
			Thread.sleep(10);
		}
		
		MotorPort.A.controlMotor(0, mode_float);
		MotorPort.B.controlMotor(0, mode_float);
		Thread.sleep(2000);
	}
}