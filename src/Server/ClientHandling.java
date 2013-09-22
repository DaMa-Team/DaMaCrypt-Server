/*
 Copyright (C) 2013  Marcel Hollerbach, Daniel Haß

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import Client.Protocol.ChatSessionUser;
import Client.Protocol.Protocol;
import Client.Protocol.Types.ChatKeyOffer;
import Client.Protocol.Types.Common;
import Client.Protocol.Types.Message;
import Client.UserHandling.ChatSession;
import Client.UserHandling.User;

/**
 * This class manages the Client - Server / Server - Client communication.
 * 
 * @author Marcel Hollerbach
 * 
 */
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

	/**
	 * Will sent a ChatInvition to the client side.
	 * 
	 * @param index
	 * @throws IOException
	 */
	public void chatInvitation(Integer index) throws IOException {
		chats.add(index);
		protocol.writeChatInvite(Common
				.chatSessionToChatInvite(getChatSession(index)));
	}

	/**
	 * Getter of the ID
	 * 
	 * @return This ID.
	 */
	public long getClientId() {
		return user.getClientId();
	}

	/**
	 * This will "write" a ChatSession to the client side.
	 * 
	 * Actually this is just writing the partner Cry to the client side.
	 * 
	 * To this time we are just having one to one Chats, there are no more than
	 * two crys. The own one and the partner cry, the own one we are calculating
	 * on the client side, we don't have to write this one.
	 * 
	 * @param s
	 * @throws IOException
	 */
	public void writeChatSession(ChatSession s) throws IOException {
		protocol.writeChatKeyOfferToClient(new ChatKeyOffer((int) s.getId(), s
				.getPartner(getClientId()).getCry()));
	}

	/**
	 * This will remove all the ChatSessions with the ID id from the
	 * ClientHandling ChatSession list.
	 * 
	 * @param id
	 */
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

	/**
	 * Will write the OnlineList to the ClientSide.
	 * 
	 * @throws IOException
	 */
	public void sentOnlineList() throws IOException {
		ArrayList<User> arrlist = new ArrayList<User>();
		for (ClientHandling client : server.getOpenhandlers()) {
			arrlist.add(client.user);
		}
		protocol.writeOnlineList(arrlist);
	}

	/**
	 * Will write a Message to the Client Side.
	 * 
	 * @param message
	 * @throws IOException
	 */
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

	/**
	 * Short notation of the server.getChatSession(id) function.
	 * 
	 * @param index
	 * @return
	 */
	private ChatSession getChatSession(int index) {
		return server.getChatSession(index);
	}

	/**
	 * Will return the ID and name as User.
	 * 
	 * @return
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Will return the Client Synonym
	 * 
	 * @return
	 */
	public String getClientSynonym() {
		return user.getName();
	}

	/**
	 * Getter of the Socket.
	 * 
	 * @return
	 */
	public Socket getSocket() {
		return client;
	}
}
