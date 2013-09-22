package Server;

import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import Client.UserHandling.ChatSession;
import Client.UserHandling.User;

public class DaCryServer extends Thread implements Runnable {
	private ServerSocket _server;
	// private int openhandler_counter;
	private IDGenerater handlerID_generator;
	private ArrayList<ClientHandling> openhandlers;
	// private int openChats_counter;
	private IDGenerater chatID_generator;
	private ArrayList<ChatSession> openChats;

	public DaCryServer(ServerSocket server) {
		_server = server;
		openhandlers = new ArrayList<ClientHandling>();
		openChats = new ArrayList<ChatSession>();
		chatID_generator = new IDGenerater();
		handlerID_generator = new IDGenerater();
	}

	@Override
	public void run() {
		try {
			while (!isInterrupted()) {
				Socket clientsocket = _server.accept();

				System.out.println("New Client, IP:"
						+ clientsocket.getInetAddress());

				int id = handlerID_generator.getNewID();

				ClientHandling clienthandler = new ClientHandling(clientsocket,
						this, id);

				addClient(clienthandler);

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addClient(ClientHandling clienthandler) {
		openhandlers.add(clienthandler);
		clienthandler.start();
	}

	public void removeClient(ClientHandling clientHandling) {
		handlerID_generator.releaseID((int) clientHandling.getClientId());
		openhandlers.remove(clientHandling);
		clientHandling.interrupt();
		System.out.println(clientHandling.getSocket().getInetAddress()
				+ " quit");
		ArrayList<ChatSession> removal = new ArrayList<ChatSession>();
		for (ChatSession session : openChats) {
			if (session.getInitiator().getUser().getClientId() == clientHandling
					.getClientId()
					|| session.getPartner().getUser().getClientId() == clientHandling
							.getClientId())
				removal.add(session);

		}
		for (ChatSession chatSession : removal) {
			checkRemove(chatSession.getId());
			removeChatSession((int) chatSession.getId());
		}
		try {
			clientHandling.getSocket().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		nameUpdated();
	}

	private void checkRemove(long id) {
		for (ClientHandling clienthandler : openhandlers) {
			clienthandler.checkForRemoval(id);
		}
	}

	public void nameUpdated() {
		for (ClientHandling clienthandler : openhandlers) {
			try {
				clienthandler.sentOnlineList();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public ArrayList<ClientHandling> getOpenhandlers() {
		return openhandlers;
	}

	public ChatSession getChatSession(int id) {
		for (ChatSession session : openChats) {
			if (id == session.getId()) {
				return session;
			}
		}
		return null;
	}

	public void removeChatSession(int id) {
		ChatSession searched = getChatSession(id);
		if (searched != null) {
			chatID_generator.releaseID(id);
			openChats.remove(searched);
		}
	}

	public void cryCheck(int chatsessionindex) throws IOException {
		ChatSession s = getChatSession(chatsessionindex);
		if (s.getPartner().getCry() != null
				&& s.getInitiator().getCry() != null) {
			getClient(s.getInitiator().getUser().getClientId())
					.writeChatSession(s);
			getClient(s.getPartner().getUser().getClientId()).writeChatSession(
					s);
		}
	}

	public int registerChatSession(ChatSession s) {
		openChats.add(s);
		return chatID_generator.getNewID();
	}

	public ClientHandling getClient(long id) {
		for (ClientHandling client : openhandlers) {
			if (client.getClientId() == id) {
				return client;
			}
		}
		return null;
	}

	public ClientHandling getClientByNickName(String nickname) {
		for (ClientHandling client : openhandlers) {
			if (client.getClientSynonym().equals(nickname)) {
				return client;
			}
		}
		return null;
	}
}
