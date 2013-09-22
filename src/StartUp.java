import java.io.IOException;
import java.net.ServerSocket;
import java.util.Scanner;

import Server.DaCryServer;

public class StartUp {
	public static void main(String[] args) {

		System.out.println("Welcome to DaCry ... :D");
		System.out.print("Init Server Socket on Port 6666 :");

		try {
			ServerSocket serversocket = new ServerSocket(6666);
			DaCryServer server = new DaCryServer(serversocket);
			server.start();
		} catch (IOException e) {
			e.printStackTrace();

			System.out.println("Failed !");

			System.exit(0);
		}
		System.out.println(" Done !");
		System.out.println("Going to working mode !");
		Scanner sc = new Scanner(System.in);
		String str = "";
		while (!str.equals("exit")) {
			str = sc.nextLine();
		}
		sc.close();
		System.exit(0);
	}
}
