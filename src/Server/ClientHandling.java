package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;

import Client.Protocol.ChatMessage;
import Client.Protocol.ChatSessionUser;
import Client.Protocol.Protocol;
import Client.Protocol.Types.ChatInvite;
import Client.Protocol.Types.ChatKeyOffer;
import Client.Protocol.Types.Common;
import Client.Protocol.Types.Message;
import Client.UserHandling.ChatSession;
import Client.UserHandling.User;

public class ClientHandling extends Thread implements Runnable {
	private User user;

	private Socket client;
	private DaCryServer server;

	private ArrayList<Integer> chats;

	private Protocol protocol;

	private DataInputStream in;
	private DataOutputStream out;

	public ClientHandling(Socket client, DaCryServer server, int id) {
		user = new User("<Not set>", id);
		this.client = client;
		this.server = server;

		try {
			out = new DataOutputStream(client.getOutputStream());
			in = new DataInputStream(client.getInputStream());
			protocol = new Protocol(in, out);
		} catch (IOException e) {
			System.out
					.println(client.getInetAddress()
							+ ": Opening Streams went wrong ... this will end up in a drop :o");
			System.out
					.println("The reason for that could be a nmap of the port ... Nothing very very worse ..");
			e.printStackTrace();
		}

		chats = new ArrayList<Integer>();

	}

	public void chatInvitation(Integer index) throws IOException {
		chats.add(index);
		ChatSession s = getChatSession(index);
		// System.out.println("Writing chat: "
		// + s.getPartner().getUser().getClientId() + " "
		// + s.getInitiator().getUser().getClientId());
		protocol.writeChatInvite(Common
				.chatSessionToChatInvite(getChatSession(index)));
	}

	public long getClientId() {
		return user.getClientId();
	}

	public void writeChatSession(ChatSession s) throws IOException {
		protocol.writeChatKeyOfferToClient(new ChatKeyOffer((int) s.getId(), s
				.getPartner(getClientId()).getCry()));
	}

	public void checkForRemoval(long id) {
		for (int i = 0; i < chats.size(); i++) {
			if (chats.get(i) == id) {
				chats.remove(i);
				i--;
			}
		}
		for (Integer ids : chats) {
			if (id == ids) {
				chats.remove(ids);
			}
		}
	}

	public void sentOnlineList() throws IOException {
		ArrayList<User> arrlist = new ArrayList<User>();
		for (ClientHandling client : server.getOpenhandlers()) {
			arrlist.add(client.user);
		}
		protocol.writeOnlineList(arrlist);
	}

	public void deliverMessage(Message message) throws IOException {
		protocol.writeMessageToClient(message);
	}

	@Override
	public void run() {
		try {
			byte m = -1;
			while (!isInterrupted()) {
				Thread.sleep(100);
				// m = (Message) in.readObject();
				if (in.readByte() == Protocol.DACRY_SERVER_COMM_OPEN) {
					m = in.readByte();
					switch (m) {

					case Protocol.CMD_GET_ONLINE_USERS:

						sentOnlineList();

						break;
					case Protocol.CMD_REGISTER:
						protocol.writeID((int) getClientId());
						break;
					case Protocol.CMD_SET_NAME: {
						user.setName(protocol.readNamerequest().getName());

						System.out.println(client.getInetAddress()
								+ " got Name " + user.getName());

						server.nameUpdated();
					}
						break;
					case Protocol.CMD_UNREGISTER: {
						server.removeClient(this);
					}
						break;

					case Protocol.CMD_SETUP_CHAT: {
						int id = protocol.readChatopen().getChatpartner();

						ClientHandling chatpartner = server.getClient(id);
						ChatSessionUser init = new ChatSessionUser(getUser(),
								null);
						ChatSessionUser guest = new ChatSessionUser(
								chatpartner.getUser(), null);
						ChatSession session = ChatSession.generateChatSession(
								-1, init, guest);
						int chatSessionID = server.registerChatSession(session);
						// System.out.println("Init, Gust: "
						// + init.getUser().getClientId() + " "
						// + guest.getUser().getClientId()
						// + " with chatSessionID :" + session.getId());
						session.setId(chatSessionID);

						chats.add(chatSessionID);

						chatpartner.chatInvitation(chatSessionID);
						chatInvitation(chatSessionID);
					}
						break;
					case Protocol.CMD_SETUP_CHAT_2:
						ChatKeyOffer offer = protocol.readKeyOffer();
						ChatSession s = server.getChatSession(offer
								.getChatsessionid());
						s.getMe(getClientId()).setCry(offer.getKey());
						server.cryCheck(offer.getChatsessionid());
						break;
					case Protocol.CMD_SEND_MESSAGE:
						Message message = protocol.readMessage();
						ChatSession chatsession = server.getChatSession(message
								.getChatsessionid());
						if (chatsession == null)
							System.out.println("Chatpartner not found !");
						else
							server.getClient(
									chatsession.getPartner(getClientId())
											.getUser().getClientId())
									.deliverMessage(message);
						break;
					}
				}
			}
		} catch (Exception e) {
			System.out.println(client.getInetAddress() + ": Client Drop!");
			e.printStackTrace();
			server.removeClient(this);
		}

	}

	private ChatSession getChatSession(int index) {
		return server.getChatSession(index);
	}

	public User getUser() {
		return user;
	}

	public String getClientSynonym() {
		return user.getName();
	}

	public Socket getSocket() {
		return client;
	}
}
