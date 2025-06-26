package org.bg3.forge;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import jakarta.inject.Inject;

@WebSocket(path = "/chatbot")
public class ChatBotWebSocket {

    private final ItemService bot;

    public ChatBotWebSocket(ItemService bot) {
        this.bot = bot;
    }

    @Inject
    ForgeAgent forgeAgent;

    @OnOpen
    public String onOpen() {
        return "<b>Hello, this is Baldur's Forge, how can I help you?</b>";
    }

    @OnTextMessage
    public String onMessage(String message) {
        if (true) {
            return forgeAgent.filter(message).toString();
        }
        List<Item> items = bot.query(message);
        String response = "";
        if (items.isEmpty()) {
            response = "I couldn't find any items that match your query.";
        } else {
            List<ForgeItem> forgeItems = items.stream().map(ForgeItem::toForgeItem).toList();
            String json = ForgeItem.toJson(forgeItems);
            response = forgeAgent.answer(message, json);
        }
        return response;
    }

}
